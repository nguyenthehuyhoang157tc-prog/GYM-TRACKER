package com.hoctin.gymtracker.data.research

import com.hoctin.gymtracker.data.ExerciseEntity
import com.hoctin.gymtracker.data.LoggedSetEntity
import com.hoctin.gymtracker.data.Readiness
import com.hoctin.gymtracker.data.SessionWithSets
import com.hoctin.gymtracker.data.WorkoutAttemptIdentity
import com.hoctin.gymtracker.data.WorkoutDayEntity
import com.hoctin.gymtracker.data.catalog.CanonicalExerciseKey
import com.hoctin.gymtracker.data.catalog.CatalogExercise
import com.hoctin.gymtracker.data.catalog.CatalogSeed
import com.hoctin.gymtracker.data.catalog.MuscleGroup
import com.hoctin.gymtracker.data.catalog.TrackingType
import com.hoctin.gymtracker.data.catalog.catalogSlugsByName
import com.hoctin.gymtracker.domain.ProgressiveOverloadEngine
import com.hoctin.gymtracker.domain.WorkoutGoalSelection
import com.hoctin.gymtracker.domain.muscleheat.MuscleRecoveryPipeline
import com.hoctin.gymtracker.domain.toWorkoutContext
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt
import org.json.JSONArray

object RecoveryResearchVersions {
    const val SUBJECT_ID = "local-device-subject"
    const val SOURCE = "PRE_FIRST_VALID_WORKING_SET"
    const val RECOVERY_MODEL = "MUSCLE_4B_BASELINE"
    const val MUSCLE_HEAT_MODEL = "MUSCLE_HEAT_1_STIMULUS_QUALITY"
    const val PROGRESSIVE_OVERLOAD_MODEL = "PROGRESSIVE_OVERLOAD_V3_11D"
    const val CONTRACT = "WEARABLE_1A_V1"
    const val RESEARCH_SCHEMA = 1
    const val ROOM_SCHEMA = 9
    const val FIRST_WORKING_SET_RULE = "TRACKING_AWARE_FIRST_WORKING_SET_V1"
    const val LOADED_SET_RULE = "MODAL_LOAD_TIE_HEAVIEST_V1"
    const val BODYWEIGHT_SET_RULE = "FIRST_POSITIVE_REP_SET_V1"
    const val METRIC_SET_RULE = "FIRST_TRACKING_VALID_SET_V1"
    const val EXPORT_SCHEMA = 1
}

enum class ResearchConsentState { GRANTED, REVOKED }
enum class ResearchCompleteness { COMPLETE, PARTIAL, NONE }
enum class ResearchOutcomeStatus { ACTIVE, COMPLETED, PARTIAL, ABANDONED, UNKNOWN_INTERRUPTED }
enum class ResearchConfidence { HIGH, MODERATE, LOW, NONE }

enum class ResearchReason {
    NO_VALID_TARGET_EXERCISE,
    NO_BASELINE_HISTORY,
    INSUFFICIENT_READINESS_INPUT,
    NO_PO_SUGGESTION,
    NO_VALID_FIRST_WORKING_SET,
    SESSION_INTERRUPTED,
    USER_CONFIRMED_DISCARD,
    OUTCOME_AMBIGUOUS,
    TRACKING_TYPE_UNSUPPORTED_FOR_METRIC,
    TIMESTAMP_INCONSISTENT,
    DUPLICATE_ATTEMPT_PREVENTED,
    LATE_EXISTING_VALID_DRAFT,
    NO_PRIMARY_MUSCLE_MAPPING,
    SUGGESTED_RIR_UNAVAILABLE,
}

fun interface ResearchIdFactory {
    fun newId(): String
}

val UuidResearchIdFactory = ResearchIdFactory { UUID.randomUUID().toString() }

data class RecoveryResearchFreezeRequest(
    val attempt: WorkoutAttemptIdentity,
    val day: WorkoutDayEntity,
    val targetExercises: List<ExerciseEntity>,
    val allPlanExercises: List<ExerciseEntity>,
    val goalSelection: WorkoutGoalSelection,
    /** Captured before any research input query begins. */
    val inputDataCutoffAtMillis: Long,
)

data class FirstWorkingSetSelection(
    val set: LoggedSetEntity,
    val confidence: ResearchConfidence,
    val ruleVersion: String,
)

data class SuggestedActualDelta(
    val loadDeltaKg: Double?,
    val repsDelta: Int?,
    val loadReduced: Boolean?,
    val repsUnexpectedlyUnderperformed: Boolean?,
)

fun suggestedActualDelta(
    achievedLoadKg: Double?,
    achievedReps: Int?,
    suggestedLoadKg: Double?,
    suggestedMinReps: Int?,
): SuggestedActualDelta = SuggestedActualDelta(
    loadDeltaKg = achievedLoadKg?.let { actual -> suggestedLoadKg?.let(actual::minus) },
    repsDelta = achievedReps?.let { actual -> suggestedMinReps?.let(actual::minus) },
    loadReduced = achievedLoadKg?.let { actual -> suggestedLoadKg?.let { actual + 0.01 < it } },
    repsUnexpectedlyUnderperformed = achievedReps?.let { actual -> suggestedMinReps?.let { actual < it } },
)

fun trackingTypeForEvaluation(code: String): TrackingType? =
    TrackingType.entries.firstOrNull { it.name == code }

/** Pure, versioned rule. It never guesses warm-up labels that AURON does not collect. */
fun selectFirstWorkingSet(
    sets: List<LoggedSetEntity>,
    trackingType: TrackingType,
): FirstWorkingSetSelection? {
    val ordered = sets.sortedWith(compareBy<LoggedSetEntity> { it.setNumber }.thenBy { it.id })
    return when (trackingType) {
        TrackingType.REPS_WEIGHT, TrackingType.SKILL_FORM -> {
            val valid = ordered.filter { it.reps > 0 && it.weight >= 0.0 }
            val modalLoad = valid.groupingBy { it.weight }.eachCount().entries
                .maxWithOrNull(compareBy<Map.Entry<Double, Int>> { it.value }.thenBy { it.key })?.key
                ?: return null
            valid.firstOrNull { it.weight == modalLoad }?.let {
                FirstWorkingSetSelection(it, ResearchConfidence.MODERATE, RecoveryResearchVersions.LOADED_SET_RULE)
            }
        }
        TrackingType.BODYWEIGHT_REPS -> ordered.firstOrNull { it.reps > 0 }?.let {
            FirstWorkingSetSelection(it, ResearchConfidence.MODERATE, RecoveryResearchVersions.BODYWEIGHT_SET_RULE)
        }
        else -> ordered.firstOrNull { set ->
            (!trackingType.showsDuration || (set.durationSeconds ?: 0) > 0) &&
                (!trackingType.showsDistance || (set.distanceMeters ?: 0.0) > 0.0) &&
                (!trackingType.showsLoad || set.weight >= 0.0)
        }?.let {
            FirstWorkingSetSelection(it, ResearchConfidence.LOW, RecoveryResearchVersions.METRIC_SET_RULE)
        }
    }
}

object RecoveryResearchRecordBuilder {
    fun build(
        request: RecoveryResearchFreezeRequest,
        sessionsAtOrBeforeCutoff: List<SessionWithSets>,
        catalog: List<CatalogExercise>,
        predictionGeneratedAtMillis: Long,
        idFactory: ResearchIdFactory = UuidResearchIdFactory,
    ): FrozenRecoveryResearchRecord {
        require(predictionGeneratedAtMillis >= request.inputDataCutoffAtMillis)
        require(request.attempt.createdAtMillis <= request.attempt.startedAtMillis)
        require(request.attempt.startedAtMillis <= request.inputDataCutoffAtMillis)
        require(sessionsAtOrBeforeCutoff.all { it.session.startedAtMillis <= request.inputDataCutoffAtMillis })

        val predictionId = idFactory.newId()
        val outcomeId = idFactory.newId()
        val catalogBySlug = catalog.associateBy { it.slug }
        val slugMap = catalogSlugsByName(catalog)
        fun keyFor(name: String) = CanonicalExerciseKey.resolve(name, slugMap)
        fun catalogFor(key: String): CatalogExercise? = key
            .takeIf { it.startsWith(CanonicalExerciseKey.CATALOG_PREFIX) }
            ?.removePrefix(CanonicalExerciseKey.CATALOG_PREFIX)
            ?.let(catalogBySlug::get)

        val allPlanNames = request.allPlanExercises.map { it.name }
        val snapshot = MuscleRecoveryPipeline.compute(
            sessions = sessionsAtOrBeforeCutoff,
            catalog = catalog,
            selectedPlanExerciseNames = request.targetExercises.map { it.name },
            allPlanExerciseNames = allPlanNames,
            nowMillis = request.inputDataCutoffAtMillis,
        )
        val resultByMuscle = snapshot.results.associateBy { it.muscleGroup }

        val groupRows = snapshot.groups.map { group ->
            RecoveryResearchPredictionGroupEntity(
                predictionId = predictionId,
                groupCode = group.group.name,
                readiness = group.recoveryScore,
                statusCode = group.status.name,
                trained = group.trained,
            )
        }
        val targetCatalog = request.targetExercises.mapNotNull { catalogFor(keyFor(it.name)) }
        val relevantMuscles = (snapshot.results.map { it.muscleGroup } +
            targetCatalog.flatMap { it.primaryMuscles + it.secondaryMuscles }).toSortedSet(compareBy { it.name })
        val muscleRows = relevantMuscles.map { muscle ->
            val result = resultByMuscle[muscle]
            RecoveryResearchPredictionMuscleEntity(
                predictionId = predictionId,
                muscleCode = muscle.name,
                readiness = result?.recoveryScore ?: 100,
                fatigueScore = result?.fatigueScore ?: 0.0,
                acuteResidual = result?.acuteResidual ?: 0.0,
                structuralResidual = result?.structuralResidual ?: 0.0,
                statusCode = result?.status?.name ?: "READY",
                colorSemanticCode = result?.colorToken?.name ?: "RECOVERY_READY",
                soreWindow = result?.soreWindow ?: false,
                grindCarried = result?.grindCarried ?: false,
                lastTrainedAtMillis = result?.lastTrainedAtMillis,
            )
        }

        val readinessBySession = sessionsAtOrBeforeCutoff.associate { session ->
            session.session.id to Readiness.fromName(session.session.readiness)
        }
        val setsWithTime = sessionsAtOrBeforeCutoff.flatMap { sws -> sws.sets.map { sws.session.startedAtMillis to it } }
        val siblingDownFlags = request.targetExercises.map { exercise ->
            val key = keyFor(exercise.name)
            val history = setsWithTime.filter { (_, set) ->
                set.canonicalExerciseKey == key ||
                    (set.canonicalExerciseKey.isNullOrBlank() &&
                        CanonicalExerciseKey.normalizeName(set.exerciseNameSnapshot) ==
                        CanonicalExerciseKey.normalizeName(exercise.name))
            }.sortedWith(compareByDescending<Pair<Long, LoggedSetEntity>> { it.first }.thenBy { it.second.setNumber })
                .map { it.second }
            ProgressiveOverloadEngine.latestSessionDownVsBaseline(exercise, history)
        }
        val workoutContext = request.goalSelection.toWorkoutContext(
            siblingsEvaluated = siblingDownFlags.count { it != null },
            siblingsDown = siblingDownFlags.count { it == true },
        )

        val targetRows = request.targetExercises.sortedWith(compareBy<ExerciseEntity> { it.sortOrder }.thenBy { it.id })
            .mapIndexed { index, exercise ->
                val key = keyFor(exercise.name)
                val catalogEntry = catalogFor(key)
                val historyPairs = setsWithTime.filter { (_, set) ->
                    set.canonicalExerciseKey == key ||
                        (set.canonicalExerciseKey.isNullOrBlank() &&
                            CanonicalExerciseKey.normalizeName(set.exerciseNameSnapshot) ==
                            CanonicalExerciseKey.normalizeName(exercise.name))
                }.sortedWith(compareByDescending<Pair<Long, LoggedSetEntity>> { it.first }.thenBy { it.second.setNumber })
                val history = historyPairs.map { it.second }
                val tracking = catalogEntry?.trackingType ?: TrackingType.REPS_WEIGHT
                val profile = catalogEntry?.let {
                    ProgressiveOverloadEngine.profileFromCatalog(
                        progressionTypeName = it.progressionType.name,
                        incrementKg = it.defaultIncrementKg,
                        sessionsRequiredBeforeIncrease = it.sessionsRequiredBeforeIncrease,
                        isBodyweight = it.isBodyweight,
                        isUnilateral = it.isUnilateral,
                        trackingTypeName = it.trackingType.name,
                    )
                }
                val latestReadiness = history.firstOrNull()?.sessionId?.let(readinessBySession::get)
                val historyReadiness = history.map { it.sessionId }.distinct().associateWith(readinessBySession::get)
                val reducedSuggestion = ProgressiveOverloadEngine.suggestion(
                    exercise, history, profile, latestReadiness, historyReadiness, workoutContext,
                )
                val typedSuggestion = if (tracking.isRepBased) {
                    ProgressiveOverloadEngine.progression(
                        exercise, history, profile, latestReadiness, historyReadiness, workoutContext,
                    )
                } else null
                val latestSessionId = history.firstOrNull()?.sessionId
                val latestSessionSets = latestSessionId?.let { id -> history.filter { it.sessionId == id } }.orEmpty()
                val reference = selectFirstWorkingSet(latestSessionSets, tracking)
                val primary = catalogEntry?.primaryMuscles.orEmpty()
                val secondary = catalogEntry?.secondaryMuscles.orEmpty()
                fun readiness(muscle: MuscleGroup) = resultByMuscle[muscle]?.recoveryScore ?: 100
                val abstention = when {
                    catalogEntry == null || primary.isEmpty() -> ResearchReason.NO_PRIMARY_MUSCLE_MAPPING
                    reference == null -> ResearchReason.NO_BASELINE_HISTORY
                    else -> null
                }
                val referenceStartedAt = latestSessionId?.let { id ->
                    sessionsAtOrBeforeCutoff.firstOrNull { it.session.id == id }?.session?.startedAtMillis
                }
                RecoveryResearchTargetExerciseEntity(
                    targetExerciseId = idFactory.newId(),
                    predictionId = predictionId,
                    targetOrdinal = index,
                    planExerciseIdSnapshot = exercise.id,
                    exerciseNameSnapshot = exercise.name,
                    canonicalExerciseKey = key,
                    trackingTypeCode = tracking.name,
                    targetSetsSnapshot = exercise.targetSets,
                    targetMinRepsSnapshot = exercise.targetMinReps,
                    targetMaxRepsSnapshot = exercise.targetMaxReps,
                    primaryMuscleCodesJson = namesJson(primary.map { it.name }),
                    secondaryMuscleCodesJson = namesJson(secondary.map { it.name }),
                    primaryReadinessMin = primary.minOfOrNull(::readiness),
                    primaryReadinessMean = primary.takeIf { it.isNotEmpty() }?.map(::readiness)?.average(),
                    secondaryReadinessMin = secondary.minOfOrNull(::readiness),
                    suggestedLoadKg = reducedSuggestion.suggestedWeight,
                    suggestedMinReps = exercise.targetMinReps.takeIf { tracking.isRepBased },
                    suggestedMaxReps = exercise.targetMaxReps.takeIf { tracking.isRepBased },
                    suggestedRir = null,
                    suggestionActionCode = typedSuggestion?.action?.name,
                    progressionTypeCode = typedSuggestion?.progressionType?.name ?: profile?.progressionType?.name,
                    suggestionStrategyCode = typedSuggestion?.plateauStrategy?.name,
                    suggestionTargetText = typedSuggestion?.targetText,
                    suggestionReasonText = typedSuggestion?.reason,
                    suggestionTitleText = reducedSuggestion.title,
                    suggestionDetailText = reducedSuggestion.detail,
                    suggestionConfidence = typedSuggestion?.confidence,
                    sessionQualityCode = typedSuggestion?.sessionQuality?.name,
                    coachInsightCode = typedSuggestion?.insight?.name,
                    referenceSessionId = reference?.set?.sessionId,
                    referenceSessionStartedAtMillis = referenceStartedAt,
                    referenceSetNumber = reference?.set?.setNumber,
                    referenceLoadKg = reference?.set?.weight,
                    referenceReps = reference?.set?.reps,
                    referenceRir = reference?.set?.rir,
                    referenceDurationSeconds = reference?.set?.durationSeconds,
                    referenceDistanceMeters = reference?.set?.distanceMeters,
                    referencePerformanceValue = reference?.set?.let { performanceValue(it, tracking) },
                    dataCompletenessCode = when {
                        abstention != null -> ResearchCompleteness.NONE.name
                        typedSuggestion == null || typedSuggestion.confidence < 60 -> ResearchCompleteness.PARTIAL.name
                        else -> ResearchCompleteness.COMPLETE.name
                    },
                    abstained = abstention != null,
                    abstentionReasonCode = abstention?.name,
                )
            }

        val inputSessions = sessionsAtOrBeforeCutoff.sortedWith(
            compareBy<SessionWithSets> { it.session.startedAtMillis }.thenBy { it.session.id },
        )
        val inputDigest = sha256(
            buildString {
                append("cutoff=").append(request.inputDataCutoffAtMillis).append('\n')
                append("goal=").append(request.goalSelection.name).append('\n')
                inputSessions.forEach { sws ->
                    append("session|").append(sws.session.id).append('|').append(sws.session.startedAtMillis)
                        .append('|').append(sws.session.readiness.orEmpty()).append('\n')
                    sws.sets.sortedWith(compareBy<LoggedSetEntity> { it.setNumber }.thenBy { it.id }).forEach { set ->
                        append("set|").append(set.id).append('|').append(set.canonicalExerciseKey.orEmpty())
                            .append('|').append(set.exerciseNameSnapshot).append('|').append(set.setNumber)
                            .append('|').append(set.weight).append('|').append(set.reps).append('|').append(set.rir)
                            .append('|').append(set.durationSeconds).append('|').append(set.distanceMeters).append('\n')
                    }
                }
            },
        )
        val predictionAbstention = ResearchReason.NO_VALID_TARGET_EXERCISE.takeIf { targetRows.isEmpty() }
        val predictionDraft = RecoveryResearchPredictionEntity(
            predictionId = predictionId,
            targetWorkoutAttemptId = request.attempt.attemptId,
            subjectId = RecoveryResearchVersions.SUBJECT_ID,
            sourceCode = RecoveryResearchVersions.SOURCE,
            workoutCreatedAtMillis = request.attempt.createdAtMillis,
            workoutStartedAtMillis = request.attempt.startedAtMillis,
            predictionGeneratedAtMillis = predictionGeneratedAtMillis,
            inputDataCutoffAtMillis = request.inputDataCutoffAtMillis,
            frozenAtMillis = predictionGeneratedAtMillis,
            recoveryModelVersion = RecoveryResearchVersions.RECOVERY_MODEL,
            muscleHeatModelVersion = RecoveryResearchVersions.MUSCLE_HEAT_MODEL,
            progressiveOverloadModelVersion = RecoveryResearchVersions.PROGRESSIVE_OVERLOAD_MODEL,
            researchContractVersion = RecoveryResearchVersions.CONTRACT,
            researchSchemaVersion = RecoveryResearchVersions.RESEARCH_SCHEMA,
            firstWorkingSetRuleVersion = RecoveryResearchVersions.FIRST_WORKING_SET_RULE,
            catalogContentVersion = CatalogSeed.CONTENT_VERSION,
            planDayIdSnapshot = request.day.id,
            planDayNameSnapshot = request.day.name,
            overallReadiness = snapshot.overallReadiness,
            overallStatusCode = snapshot.overallStatus.name,
            inputSessionIdsJson = idsJson(inputSessions.map { it.session.id }),
            inputSessionMaxTimestampMillis = inputSessions.maxOfOrNull { it.session.startedAtMillis },
            inputDigestSha256 = inputDigest,
            dataCompletenessCode = when {
                targetRows.isEmpty() -> ResearchCompleteness.NONE.name
                targetRows.any { it.abstained || it.dataCompletenessCode != ResearchCompleteness.COMPLETE.name } ->
                    ResearchCompleteness.PARTIAL.name
                else -> ResearchCompleteness.COMPLETE.name
            },
            abstained = predictionAbstention != null,
            abstentionReasonCode = predictionAbstention?.name,
            payloadSha256 = "",
        )
        val payloadHash = hashFrozenPayload(predictionDraft, groupRows, muscleRows, targetRows)
        return FrozenRecoveryResearchRecord(
            prediction = predictionDraft.copy(payloadSha256 = payloadHash),
            groups = groupRows,
            muscles = muscleRows,
            targetExercises = targetRows,
            initialOutcome = RecoveryResearchOutcomeEntity(
                outcomeId = outcomeId,
                predictionId = predictionId,
                targetWorkoutAttemptId = request.attempt.attemptId,
                sessionId = null,
                statusCode = ResearchOutcomeStatus.ACTIVE.name,
                statusReasonCode = null,
                workoutCompletedAtMillis = null,
                workoutAbandonedAtMillis = null,
                firstValidSetObservedAtMillis = null,
                firstWorkingSetLoggedAtMillis = null,
                outcomeCapturedAtMillis = null,
                dataCompletenessCode = ResearchCompleteness.NONE.name,
                validForEvaluation = false,
                exclusionReasonCode = null,
            ),
        )
    }

    fun hashFrozenPayload(
        prediction: RecoveryResearchPredictionEntity,
        groups: List<RecoveryResearchPredictionGroupEntity>,
        muscles: List<RecoveryResearchPredictionMuscleEntity>,
        targets: List<RecoveryResearchTargetExerciseEntity>,
    ): String = sha256(
        buildString {
            append(prediction.copy(payloadSha256 = "").toString()).append('\n')
            groups.sortedBy { it.groupCode }.forEach { append(it).append('\n') }
            muscles.sortedBy { it.muscleCode }.forEach { append(it).append('\n') }
            targets.sortedBy { it.targetOrdinal }.forEach { append(it).append('\n') }
        },
    )

    fun performanceValue(set: LoggedSetEntity, trackingType: TrackingType): Double? {
        return when {
            trackingType.isRepBased -> if (trackingType == TrackingType.BODYWEIGHT_REPS) set.reps.toDouble()
                else set.weight * set.reps
            trackingType.showsDistance && trackingType.showsDuration -> {
                val duration = set.durationSeconds?.takeIf { it > 0 } ?: return null
                set.distanceMeters?.takeIf { it > 0.0 }?.div(duration)
            }
            trackingType.showsDistance -> set.distanceMeters
            trackingType.showsDuration -> set.durationSeconds?.toDouble()
            else -> null
        }
    }
}

internal fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
    .digest(value.toByteArray(StandardCharsets.UTF_8))
    .joinToString("") { "%02x".format(Locale.US, it) }

internal fun namesJson(values: List<String>): String = JSONArray(values.sorted()).toString()
internal fun idsJson(values: List<Long>): String = JSONArray(values.sorted()).toString()

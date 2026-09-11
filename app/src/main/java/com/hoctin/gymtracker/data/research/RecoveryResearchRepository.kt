package com.hoctin.gymtracker.data.research

import androidx.room.withTransaction
import com.hoctin.gymtracker.data.AppDatabase
import com.hoctin.gymtracker.data.ExerciseLogInput
import com.hoctin.gymtracker.data.ExerciseEntity
import com.hoctin.gymtracker.data.GymDao
import com.hoctin.gymtracker.data.GymRepository
import com.hoctin.gymtracker.data.LoggedSetEntity
import com.hoctin.gymtracker.data.PersistedWorkoutSession
import com.hoctin.gymtracker.data.Readiness
import com.hoctin.gymtracker.data.WorkoutDayEntity
import com.hoctin.gymtracker.data.catalog.ExerciseCatalogRepository
import com.hoctin.gymtracker.data.catalog.TrackingType
import com.hoctin.gymtracker.domain.ProgressiveOverloadEngine
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class RecoveryResearchRepository(
    private val database: AppDatabase,
    private val dao: RecoveryResearchDao,
    private val gymDao: GymDao,
    private val gymRepository: GymRepository,
    private val catalogRepository: ExerciseCatalogRepository,
    private val clock: () -> Long = System::currentTimeMillis,
    private val idFactory: ResearchIdFactory = UuidResearchIdFactory,
) {
    fun observeConsent(): Flow<RecoveryResearchConsentEntity?> = dao.observeConsent()

    suspend fun isGranted(): Boolean =
        dao.getConsent()?.stateCode == ResearchConsentState.GRANTED.name

    suspend fun setConsent(granted: Boolean) = withContext(Dispatchers.IO) {
        val now = clock()
        database.withTransaction {
            val existing = dao.getConsent()
            if (existing == null) {
                dao.insertConsentIgnore(
                    RecoveryResearchConsentEntity(
                        subjectId = RecoveryResearchVersions.SUBJECT_ID,
                        stateCode = if (granted) ResearchConsentState.GRANTED.name else ResearchConsentState.REVOKED.name,
                        contractVersion = RecoveryResearchVersions.CONTRACT,
                        createdAtMillis = now,
                        grantedAtMillis = now.takeIf { granted },
                        revokedAtMillis = now.takeUnless { granted },
                    ),
                )
            } else {
                dao.updateConsentState(
                    subjectId = existing.subjectId,
                    stateCode = if (granted) ResearchConsentState.GRANTED.name else ResearchConsentState.REVOKED.name,
                    contractVersion = RecoveryResearchVersions.CONTRACT,
                    grantedAtMillis = if (granted) now else existing.grantedAtMillis,
                    revokedAtMillis = now.takeUnless { granted },
                )
            }
        }
    }

    /** Idempotent by the durable attempt id; a second caller gets the already-frozen row. */
    suspend fun freezePredictionIfEligible(
        request: RecoveryResearchFreezeRequest,
    ): RecoveryResearchPredictionEntity? = withContext(Dispatchers.IO) {
        dao.getPredictionByAttempt(request.attempt.attemptId)?.let { return@withContext it }
        if (!isGranted()) return@withContext null
        val sessions = gymDao.getSessionHistoryAtOrBefore(request.inputDataCutoffAtMillis)
        check(sessions.all { it.session.startedAtMillis <= request.inputDataCutoffAtMillis }) {
            "Prospective cutoff violation"
        }
        val catalog = catalogRepository.all()
        val generatedAt = clock().coerceAtLeast(request.inputDataCutoffAtMillis)
        val record = RecoveryResearchRecordBuilder.build(
            request = request,
            sessionsAtOrBeforeCutoff = sessions,
            catalog = catalog,
            predictionGeneratedAtMillis = generatedAt,
            idFactory = idFactory,
        )
        database.withTransaction {
            if (!isGranted()) null else dao.getOrInsertFrozen(record)
        }
    }

    suspend fun recordFirstValidSet(
        attemptId: String,
        planExerciseId: Long,
        canonicalExerciseKey: String,
        draftSetKey: String,
        draftSetOrdinal: Int,
        observedAtMillis: Long = clock(),
    ) = withContext(Dispatchers.IO) {
        if (!isGranted()) return@withContext
        val prediction = dao.getPredictionByAttempt(attemptId) ?: return@withContext
        if (observedAtMillis < prediction.inputDataCutoffAtMillis) return@withContext
        dao.insertSetObservationIgnore(
            RecoveryResearchSetObservationEntity(
                setObservationId = idFactory.newId(),
                predictionId = prediction.predictionId,
                targetWorkoutAttemptId = attemptId,
                planExerciseIdSnapshot = planExerciseId,
                canonicalExerciseKey = canonicalExerciseKey,
                draftSetKey = draftSetKey,
                draftSetOrdinal = draftSetOrdinal,
                firstValidAtMillis = observedAtMillis,
            ),
        )
    }

    /**
     * Production persistence is authoritative and commits first. Optional research outcome capture
     * follows in its own transaction, so a research failure can never roll back a saved workout.
     */
    suspend fun saveWorkoutAndCaptureOutcome(
        attemptId: String,
        day: WorkoutDayEntity,
        inputs: List<ExerciseLogInput>,
        date: LocalDate = LocalDate.now(),
        readiness: Readiness? = null,
        durationSeconds: Int? = null,
        completedAtMillis: Long = clock(),
    ): Long = withContext(Dispatchers.IO) {
        // The production save is authoritative and must never be rolled back by optional research.
        val persisted = database.withTransaction {
            gymRepository.persistWorkoutSession(day, inputs, date, readiness, durationSeconds)
        }
        runCatching {
            database.withTransaction {
                val prediction = dao.getPredictionByAttempt(attemptId)
                if (prediction != null && isGranted()) {
                    captureSavedOutcome(prediction, persisted, completedAtMillis)
                }
            }
        }
        persisted.sessionId
    }

    suspend fun abandonAttempt(
        attemptId: String,
        abandonedAtMillis: Long = clock(),
    ) = withContext(Dispatchers.IO) {
        database.withTransaction {
            val prediction = dao.getPredictionByAttempt(attemptId) ?: return@withTransaction
            val firstObserved = dao.firstValidSetObservedAt(prediction.predictionId)
            dao.transitionActiveOutcome(
                predictionId = prediction.predictionId,
                sessionId = null,
                statusCode = ResearchOutcomeStatus.ABANDONED.name,
                statusReasonCode = ResearchReason.USER_CONFIRMED_DISCARD.name,
                completedAtMillis = null,
                abandonedAtMillis = abandonedAtMillis,
                firstValidSetObservedAtMillis = firstObserved,
                firstWorkingSetLoggedAtMillis = firstObserved,
                outcomeCapturedAtMillis = abandonedAtMillis,
                dataCompletenessCode = ResearchCompleteness.PARTIAL.name,
                validForEvaluation = false,
                exclusionReasonCode = ResearchReason.SESSION_INTERRUPTED.name,
            )
        }
    }

    suspend fun deleteAllResearchData() = withContext(Dispatchers.IO) {
        database.withTransaction { dao.deleteAllResearchData() }
    }

    suspend fun exportDeterministicJson(): String = withContext(Dispatchers.IO) {
        database.withTransaction { RecoveryResearchExport.encode(dao) }
    }

    private suspend fun captureSavedOutcome(
        prediction: RecoveryResearchPredictionEntity,
        persisted: PersistedWorkoutSession,
        completedAtMillis: Long,
    ) {
        val outcome = dao.getOutcomeForPrediction(prediction.predictionId) ?: return
        if (outcome.statusCode != ResearchOutcomeStatus.ACTIVE.name) return
        val targets = dao.getTargetExercises(prediction.predictionId)
        val observations = dao.getSetObservations(prediction.predictionId)
        val observationByRow = observations.associateBy { it.planExerciseIdSnapshot to it.draftSetKey }
        val exerciseOutcomes = targets.map { target ->
            val tracking = trackingTypeForEvaluation(target.trackingTypeCode)
            val matchingSets = persisted.sets.filter { set ->
                set.canonicalExerciseKey == target.canonicalExerciseKey ||
                    (set.exerciseId != null && set.exerciseId == target.planExerciseIdSnapshot)
            }
            val selected = tracking?.let { selectFirstWorkingSet(matchingSets, it) }
            val observedAt = selected?.set?.id?.let { setId ->
                val rowKey = persisted.sourceDraftKeyBySetId[setId]
                rowKey?.let { observationByRow[target.planExerciseIdSnapshot to it]?.firstValidAtMillis }
            }
            val valid = !target.abstained && selected != null && observedAt != null
            val exclusion = when {
                tracking == null -> ResearchReason.TRACKING_TYPE_UNSUPPORTED_FOR_METRIC.name
                target.abstained -> target.abstentionReasonCode
                selected == null -> ResearchReason.NO_VALID_FIRST_WORKING_SET.name
                observedAt == null -> ResearchReason.OUTCOME_AMBIGUOUS.name
                else -> null
            }
            val actual = selected?.set
            val delta = suggestedActualDelta(
                achievedLoadKg = actual?.weight,
                achievedReps = actual?.reps,
                suggestedLoadKg = target.suggestedLoadKg,
                suggestedMinReps = target.suggestedMinReps,
            )
            val sessionQuality = if (tracking?.isRepBased == true && matchingSets.isNotEmpty()) {
                runCatching {
                    ProgressiveOverloadEngine.progression(
                        exercise = ExerciseEntity(
                            id = target.planExerciseIdSnapshot ?: 0,
                            dayId = prediction.planDayIdSnapshot ?: 0,
                            name = target.exerciseNameSnapshot,
                            targetSets = target.targetSetsSnapshot,
                            targetMinReps = target.targetMinRepsSnapshot,
                            targetMaxReps = target.targetMaxRepsSnapshot,
                            sortOrder = target.targetOrdinal,
                        ),
                        recentSets = matchingSets,
                    ).sessionQuality.name
                }.getOrNull()
            } else null
            RecoveryResearchExerciseOutcomeEntity(
                exerciseOutcomeId = idFactory.newId(),
                outcomeId = outcome.outcomeId,
                targetExerciseId = target.targetExerciseId,
                canonicalExerciseKey = target.canonicalExerciseKey,
                exerciseNameSnapshot = target.exerciseNameSnapshot,
                exerciseSequence = target.targetOrdinal,
                trackingTypeCode = target.trackingTypeCode,
                firstWorkingSetId = actual?.id,
                firstWorkingSetSetNumber = actual?.setNumber,
                firstWorkingSetRuleVersion = selected?.ruleVersion ?: RecoveryResearchVersions.FIRST_WORKING_SET_RULE,
                firstWorkingSetObservedAtMillis = observedAt,
                achievedLoadKg = actual?.weight,
                achievedReps = actual?.reps,
                achievedRir = actual?.rir,
                achievedDurationSeconds = actual?.durationSeconds,
                achievedDistanceMeters = actual?.distanceMeters,
                suggestedLoadDeltaKg = delta.loadDeltaKg,
                suggestedRepsDelta = delta.repsDelta,
                loadReduced = delta.loadReduced,
                repsUnexpectedlyUnderperformed = delta.repsUnexpectedlyUnderperformed,
                completedValidSets = matchingSets.size,
                targetSetsSnapshot = target.targetSetsSnapshot,
                exerciseCompleted = matchingSets.size >= target.targetSetsSnapshot,
                sessionQualityCode = sessionQuality,
                matchedPlannedTarget = matchingSets.isNotEmpty(),
                firstWorkingSetConfidenceCode = selected?.confidence?.name ?: ResearchConfidence.NONE.name,
                validForEvaluation = valid,
                exclusionReasonCode = exclusion,
                outcomeCapturedAtMillis = completedAtMillis,
            )
        }
        val evaluable = exerciseOutcomes.filter { it.validForEvaluation }
        val allPlannedCompleted = evaluable.isNotEmpty() &&
            exerciseOutcomes.filter { target ->
                targets.first { it.targetExerciseId == target.targetExerciseId }.abstained.not()
            }.all { it.exerciseCompleted }
        val status = if (allPlannedCompleted) ResearchOutcomeStatus.COMPLETED else ResearchOutcomeStatus.PARTIAL
        val firstObserved = exerciseOutcomes.mapNotNull { it.firstWorkingSetObservedAtMillis }.minOrNull()
            ?: observations.minOfOrNull { it.firstValidAtMillis }
        val validForEvaluation = evaluable.isNotEmpty()
        val exclusion = ResearchReason.NO_VALID_FIRST_WORKING_SET.name.takeUnless { validForEvaluation }
        val transitioned = dao.transitionActiveOutcome(
            predictionId = prediction.predictionId,
            sessionId = persisted.sessionId,
            statusCode = status.name,
            statusReasonCode = null,
            completedAtMillis = completedAtMillis,
            abandonedAtMillis = null,
            firstValidSetObservedAtMillis = observations.minOfOrNull { it.firstValidAtMillis },
            firstWorkingSetLoggedAtMillis = firstObserved,
            outcomeCapturedAtMillis = completedAtMillis,
            dataCompletenessCode = if (allPlannedCompleted) ResearchCompleteness.COMPLETE.name
            else ResearchCompleteness.PARTIAL.name,
            validForEvaluation = validForEvaluation,
            exclusionReasonCode = exclusion,
        )
        if (transitioned == 1 && exerciseOutcomes.isNotEmpty()) {
            dao.insertExerciseOutcomesIgnore(exerciseOutcomes)
        }
    }
}

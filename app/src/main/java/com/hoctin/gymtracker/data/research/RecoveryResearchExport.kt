package com.hoctin.gymtracker.data.research

import org.json.JSONObject

/** Separate, local-only and byte-deterministic research export. It is never part of normal backup. */
object RecoveryResearchExport {
    suspend fun encode(dao: RecoveryResearchDao): String {
        val root = obj(
            "contractVersion" to RecoveryResearchVersions.CONTRACT,
            "exportSchemaVersion" to RecoveryResearchVersions.EXPORT_SCHEMA,
            "roomSchemaVersion" to RecoveryResearchVersions.ROOM_SCHEMA,
            "consent" to dao.getAllConsent().map(::consent),
            "predictions" to dao.getAllPredictions().map(::prediction),
            "predictionGroups" to dao.getAllPredictionGroups().map(::group),
            "predictionMuscles" to dao.getAllPredictionMuscles().map(::muscle),
            "targetExercises" to dao.getAllTargetExercises().map(::target),
            "setObservations" to dao.getAllSetObservations().map(::observation),
            "outcomes" to dao.getAllOutcomes().map(::outcome),
            "exerciseOutcomes" to dao.getAllExerciseOutcomes().map(::exerciseOutcome),
        )
        return canonical(root) + "\n"
    }

    private fun consent(v: RecoveryResearchConsentEntity) = obj(
        "subjectId" to v.subjectId, "stateCode" to v.stateCode, "contractVersion" to v.contractVersion,
        "createdAtMillis" to v.createdAtMillis, "grantedAtMillis" to v.grantedAtMillis,
        "revokedAtMillis" to v.revokedAtMillis,
    )

    private fun prediction(v: RecoveryResearchPredictionEntity) = obj(
        "predictionId" to v.predictionId, "targetWorkoutAttemptId" to v.targetWorkoutAttemptId,
        "subjectId" to v.subjectId, "sourceCode" to v.sourceCode,
        "workoutCreatedAtMillis" to v.workoutCreatedAtMillis, "workoutStartedAtMillis" to v.workoutStartedAtMillis,
        "predictionGeneratedAtMillis" to v.predictionGeneratedAtMillis,
        "inputDataCutoffAtMillis" to v.inputDataCutoffAtMillis, "frozenAtMillis" to v.frozenAtMillis,
        "recoveryModelVersion" to v.recoveryModelVersion, "muscleHeatModelVersion" to v.muscleHeatModelVersion,
        "progressiveOverloadModelVersion" to v.progressiveOverloadModelVersion,
        "researchContractVersion" to v.researchContractVersion, "researchSchemaVersion" to v.researchSchemaVersion,
        "firstWorkingSetRuleVersion" to v.firstWorkingSetRuleVersion,
        "catalogContentVersion" to v.catalogContentVersion, "planDayIdSnapshot" to v.planDayIdSnapshot,
        "planDayNameSnapshot" to v.planDayNameSnapshot, "overallReadiness" to v.overallReadiness,
        "overallStatusCode" to v.overallStatusCode, "inputSessionIdsJson" to v.inputSessionIdsJson,
        "inputSessionMaxTimestampMillis" to v.inputSessionMaxTimestampMillis,
        "inputDigestSha256" to v.inputDigestSha256, "dataCompletenessCode" to v.dataCompletenessCode,
        "abstained" to v.abstained, "abstentionReasonCode" to v.abstentionReasonCode,
        "payloadSha256" to v.payloadSha256,
    )

    private fun group(v: RecoveryResearchPredictionGroupEntity) = obj(
        "predictionId" to v.predictionId, "groupCode" to v.groupCode, "readiness" to v.readiness,
        "statusCode" to v.statusCode, "trained" to v.trained,
    )

    private fun muscle(v: RecoveryResearchPredictionMuscleEntity) = obj(
        "predictionId" to v.predictionId, "muscleCode" to v.muscleCode, "readiness" to v.readiness,
        "fatigueScore" to v.fatigueScore, "acuteResidual" to v.acuteResidual,
        "structuralResidual" to v.structuralResidual, "statusCode" to v.statusCode,
        "colorSemanticCode" to v.colorSemanticCode, "soreWindow" to v.soreWindow,
        "grindCarried" to v.grindCarried, "lastTrainedAtMillis" to v.lastTrainedAtMillis,
    )

    private fun target(v: RecoveryResearchTargetExerciseEntity) = obj(
        "targetExerciseId" to v.targetExerciseId, "predictionId" to v.predictionId,
        "targetOrdinal" to v.targetOrdinal, "planExerciseIdSnapshot" to v.planExerciseIdSnapshot,
        "exerciseNameSnapshot" to v.exerciseNameSnapshot, "canonicalExerciseKey" to v.canonicalExerciseKey,
        "trackingTypeCode" to v.trackingTypeCode, "targetSetsSnapshot" to v.targetSetsSnapshot,
        "targetMinRepsSnapshot" to v.targetMinRepsSnapshot, "targetMaxRepsSnapshot" to v.targetMaxRepsSnapshot,
        "primaryMuscleCodesJson" to v.primaryMuscleCodesJson,
        "secondaryMuscleCodesJson" to v.secondaryMuscleCodesJson,
        "primaryReadinessMin" to v.primaryReadinessMin, "primaryReadinessMean" to v.primaryReadinessMean,
        "secondaryReadinessMin" to v.secondaryReadinessMin, "suggestedLoadKg" to v.suggestedLoadKg,
        "suggestedMinReps" to v.suggestedMinReps, "suggestedMaxReps" to v.suggestedMaxReps,
        "suggestedRir" to v.suggestedRir, "suggestionActionCode" to v.suggestionActionCode,
        "progressionTypeCode" to v.progressionTypeCode, "suggestionStrategyCode" to v.suggestionStrategyCode,
        "suggestionTargetText" to v.suggestionTargetText, "suggestionReasonText" to v.suggestionReasonText,
        "suggestionTitleText" to v.suggestionTitleText, "suggestionDetailText" to v.suggestionDetailText,
        "suggestionConfidence" to v.suggestionConfidence, "sessionQualityCode" to v.sessionQualityCode,
        "coachInsightCode" to v.coachInsightCode, "referenceSessionId" to v.referenceSessionId,
        "referenceSessionStartedAtMillis" to v.referenceSessionStartedAtMillis,
        "referenceSetNumber" to v.referenceSetNumber, "referenceLoadKg" to v.referenceLoadKg,
        "referenceReps" to v.referenceReps, "referenceRir" to v.referenceRir,
        "referenceDurationSeconds" to v.referenceDurationSeconds,
        "referenceDistanceMeters" to v.referenceDistanceMeters,
        "referencePerformanceValue" to v.referencePerformanceValue,
        "dataCompletenessCode" to v.dataCompletenessCode, "abstained" to v.abstained,
        "abstentionReasonCode" to v.abstentionReasonCode,
    )

    private fun observation(v: RecoveryResearchSetObservationEntity) = obj(
        "setObservationId" to v.setObservationId, "predictionId" to v.predictionId,
        "targetWorkoutAttemptId" to v.targetWorkoutAttemptId,
        "planExerciseIdSnapshot" to v.planExerciseIdSnapshot,
        "canonicalExerciseKey" to v.canonicalExerciseKey, "draftSetKey" to v.draftSetKey,
        "draftSetOrdinal" to v.draftSetOrdinal,
        "firstValidAtMillis" to v.firstValidAtMillis,
    )

    private fun outcome(v: RecoveryResearchOutcomeEntity) = obj(
        "outcomeId" to v.outcomeId, "predictionId" to v.predictionId,
        "targetWorkoutAttemptId" to v.targetWorkoutAttemptId, "sessionId" to v.sessionId,
        "statusCode" to v.statusCode, "statusReasonCode" to v.statusReasonCode,
        "workoutCompletedAtMillis" to v.workoutCompletedAtMillis,
        "workoutAbandonedAtMillis" to v.workoutAbandonedAtMillis,
        "firstValidSetObservedAtMillis" to v.firstValidSetObservedAtMillis,
        "firstWorkingSetLoggedAtMillis" to v.firstWorkingSetLoggedAtMillis,
        "outcomeCapturedAtMillis" to v.outcomeCapturedAtMillis,
        "dataCompletenessCode" to v.dataCompletenessCode, "validForEvaluation" to v.validForEvaluation,
        "exclusionReasonCode" to v.exclusionReasonCode,
    )

    private fun exerciseOutcome(v: RecoveryResearchExerciseOutcomeEntity) = obj(
        "exerciseOutcomeId" to v.exerciseOutcomeId, "outcomeId" to v.outcomeId,
        "targetExerciseId" to v.targetExerciseId, "canonicalExerciseKey" to v.canonicalExerciseKey,
        "exerciseNameSnapshot" to v.exerciseNameSnapshot, "exerciseSequence" to v.exerciseSequence,
        "trackingTypeCode" to v.trackingTypeCode, "firstWorkingSetId" to v.firstWorkingSetId,
        "firstWorkingSetSetNumber" to v.firstWorkingSetSetNumber,
        "firstWorkingSetRuleVersion" to v.firstWorkingSetRuleVersion,
        "firstWorkingSetObservedAtMillis" to v.firstWorkingSetObservedAtMillis,
        "achievedLoadKg" to v.achievedLoadKg, "achievedReps" to v.achievedReps,
        "achievedRir" to v.achievedRir, "achievedDurationSeconds" to v.achievedDurationSeconds,
        "achievedDistanceMeters" to v.achievedDistanceMeters,
        "suggestedLoadDeltaKg" to v.suggestedLoadDeltaKg, "suggestedRepsDelta" to v.suggestedRepsDelta,
        "loadReduced" to v.loadReduced, "repsUnexpectedlyUnderperformed" to v.repsUnexpectedlyUnderperformed,
        "completedValidSets" to v.completedValidSets, "targetSetsSnapshot" to v.targetSetsSnapshot,
        "exerciseCompleted" to v.exerciseCompleted, "sessionQualityCode" to v.sessionQualityCode,
        "matchedPlannedTarget" to v.matchedPlannedTarget,
        "firstWorkingSetConfidenceCode" to v.firstWorkingSetConfidenceCode,
        "validForEvaluation" to v.validForEvaluation, "exclusionReasonCode" to v.exclusionReasonCode,
        "outcomeCapturedAtMillis" to v.outcomeCapturedAtMillis,
    )

    private fun obj(vararg values: Pair<String, Any?>): Map<String, Any?> = sortedMapOf(*values)

    private fun canonical(value: Any?): String = when (value) {
        null -> "null"
        is String -> JSONObject.quote(value)
        is Boolean, is Byte, is Short, is Int, is Long -> value.toString()
        is Float -> require(value.isFinite()) { "Non-finite export number" }.let { value.toString() }
        is Double -> require(value.isFinite()) { "Non-finite export number" }.let { value.toString() }
        is Map<*, *> -> value.entries.sortedBy { it.key.toString() }.joinToString(",", "{", "}") {
            JSONObject.quote(it.key.toString()) + ":" + canonical(it.value)
        }
        is Iterable<*> -> value.joinToString(",", "[", "]") { canonical(it) }
        else -> error("Unsupported recovery research export value: ${value::class.java.name}")
    }
}

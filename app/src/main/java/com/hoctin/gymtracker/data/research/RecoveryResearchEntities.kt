package com.hoctin.gymtracker.data.research

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/** Explicit, local-only consent. Permission or opening Log Workout never implies this row is granted. */
@Entity(
    tableName = "recovery_research_consent",
    indices = [Index("stateCode")],
    primaryKeys = ["subjectId"],
)
data class RecoveryResearchConsentEntity(
    val subjectId: String,
    val stateCode: String,
    val contractVersion: String,
    val createdAtMillis: Long,
    val grantedAtMillis: Long?,
    val revokedAtMillis: Long?,
)

/**
 * One immutable MUSCLE-4B prediction per stable Log Workout attempt. Child prediction rows are also
 * immutable; mutable workout outcomes live in separate tables.
 */
@Entity(
    tableName = "recovery_research_predictions",
    foreignKeys = [
        ForeignKey(
            entity = RecoveryResearchConsentEntity::class,
            parentColumns = ["subjectId"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["targetWorkoutAttemptId"], unique = true),
        Index("subjectId"),
        Index("predictionGeneratedAtMillis"),
        Index("inputDataCutoffAtMillis"),
        Index(value = ["recoveryModelVersion", "predictionGeneratedAtMillis"]),
    ],
    primaryKeys = ["predictionId"],
)
data class RecoveryResearchPredictionEntity(
    val predictionId: String,
    val targetWorkoutAttemptId: String,
    val subjectId: String,
    val sourceCode: String,
    val workoutCreatedAtMillis: Long,
    val workoutStartedAtMillis: Long,
    val predictionGeneratedAtMillis: Long,
    val inputDataCutoffAtMillis: Long,
    val frozenAtMillis: Long,
    val recoveryModelVersion: String,
    val muscleHeatModelVersion: String,
    val progressiveOverloadModelVersion: String,
    val researchContractVersion: String,
    val researchSchemaVersion: Int,
    val firstWorkingSetRuleVersion: String,
    val catalogContentVersion: Int,
    val planDayIdSnapshot: Long?,
    val planDayNameSnapshot: String,
    val overallReadiness: Int,
    val overallStatusCode: String,
    val inputSessionIdsJson: String,
    val inputSessionMaxTimestampMillis: Long?,
    val inputDigestSha256: String,
    val dataCompletenessCode: String,
    val abstained: Boolean,
    val abstentionReasonCode: String?,
    val payloadSha256: String,
)

@Entity(
    tableName = "recovery_research_prediction_groups",
    foreignKeys = [
        ForeignKey(
            entity = RecoveryResearchPredictionEntity::class,
            parentColumns = ["predictionId"],
            childColumns = ["predictionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("groupCode")],
    primaryKeys = ["predictionId", "groupCode"],
)
data class RecoveryResearchPredictionGroupEntity(
    val predictionId: String,
    val groupCode: String,
    val readiness: Int,
    val statusCode: String,
    val trained: Boolean,
)

@Entity(
    tableName = "recovery_research_prediction_muscles",
    foreignKeys = [
        ForeignKey(
            entity = RecoveryResearchPredictionEntity::class,
            parentColumns = ["predictionId"],
            childColumns = ["predictionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("muscleCode"), Index("lastTrainedAtMillis")],
    primaryKeys = ["predictionId", "muscleCode"],
)
data class RecoveryResearchPredictionMuscleEntity(
    val predictionId: String,
    val muscleCode: String,
    val readiness: Int,
    val fatigueScore: Double,
    val acuteResidual: Double,
    val structuralResidual: Double,
    val statusCode: String,
    val colorSemanticCode: String,
    val soreWindow: Boolean,
    val grindCarried: Boolean,
    val lastTrainedAtMillis: Long?,
)

@Entity(
    tableName = "recovery_research_target_exercises",
    foreignKeys = [
        ForeignKey(
            entity = RecoveryResearchPredictionEntity::class,
            parentColumns = ["predictionId"],
            childColumns = ["predictionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["predictionId", "targetOrdinal"], unique = true),
        Index("canonicalExerciseKey"),
        Index("planExerciseIdSnapshot"),
        Index("referenceSessionId"),
    ],
    primaryKeys = ["targetExerciseId"],
)
data class RecoveryResearchTargetExerciseEntity(
    val targetExerciseId: String,
    val predictionId: String,
    val targetOrdinal: Int,
    val planExerciseIdSnapshot: Long?,
    val exerciseNameSnapshot: String,
    val canonicalExerciseKey: String,
    val trackingTypeCode: String,
    val targetSetsSnapshot: Int,
    val targetMinRepsSnapshot: Int,
    val targetMaxRepsSnapshot: Int,
    val primaryMuscleCodesJson: String,
    val secondaryMuscleCodesJson: String,
    val primaryReadinessMin: Int?,
    val primaryReadinessMean: Double?,
    val secondaryReadinessMin: Int?,
    val suggestedLoadKg: Double?,
    val suggestedMinReps: Int?,
    val suggestedMaxReps: Int?,
    val suggestedRir: Int?,
    val suggestionActionCode: String?,
    val progressionTypeCode: String?,
    val suggestionStrategyCode: String?,
    val suggestionTargetText: String?,
    val suggestionReasonText: String?,
    val suggestionTitleText: String?,
    val suggestionDetailText: String?,
    val suggestionConfidence: Int?,
    val sessionQualityCode: String?,
    val coachInsightCode: String?,
    val referenceSessionId: Long?,
    val referenceSessionStartedAtMillis: Long?,
    val referenceSetNumber: Int?,
    val referenceLoadKg: Double?,
    val referenceReps: Int?,
    val referenceRir: Int?,
    val referenceDurationSeconds: Int?,
    val referenceDistanceMeters: Double?,
    val referencePerformanceValue: Double?,
    val dataCompletenessCode: String,
    val abstained: Boolean,
    val abstentionReasonCode: String?,
)

/** First time a particular draft row became valid. INSERT IGNORE preserves the first observation. */
@Entity(
    tableName = "recovery_research_set_observations",
    foreignKeys = [
        ForeignKey(
            entity = RecoveryResearchPredictionEntity::class,
            parentColumns = ["predictionId"],
            childColumns = ["predictionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["predictionId", "planExerciseIdSnapshot", "draftSetKey"], unique = true),
        Index("targetWorkoutAttemptId"),
        Index("canonicalExerciseKey"),
        Index("firstValidAtMillis"),
    ],
    primaryKeys = ["setObservationId"],
)
data class RecoveryResearchSetObservationEntity(
    val setObservationId: String,
    val predictionId: String,
    val targetWorkoutAttemptId: String,
    val planExerciseIdSnapshot: Long,
    val canonicalExerciseKey: String,
    /** Stable persisted row identity; unlike an ordinal it survives skipped-row removal/reordering. */
    val draftSetKey: String,
    val draftSetOrdinal: Int,
    val firstValidAtMillis: Long,
)

/** Mutable lifecycle only. The frozen prediction graph is never updated when this row changes. */
@Entity(
    tableName = "recovery_research_outcomes",
    foreignKeys = [
        ForeignKey(
            entity = RecoveryResearchPredictionEntity::class,
            parentColumns = ["predictionId"],
            childColumns = ["predictionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["predictionId"], unique = true),
        Index(value = ["targetWorkoutAttemptId"], unique = true),
        Index(value = ["sessionId"], unique = true),
        Index("statusCode"),
        Index("outcomeCapturedAtMillis"),
    ],
    primaryKeys = ["outcomeId"],
)
data class RecoveryResearchOutcomeEntity(
    val outcomeId: String,
    val predictionId: String,
    val targetWorkoutAttemptId: String,
    val sessionId: Long?,
    val statusCode: String,
    val statusReasonCode: String?,
    val workoutCompletedAtMillis: Long?,
    val workoutAbandonedAtMillis: Long?,
    val firstValidSetObservedAtMillis: Long?,
    val firstWorkingSetLoggedAtMillis: Long?,
    val outcomeCapturedAtMillis: Long?,
    val dataCompletenessCode: String,
    val validForEvaluation: Boolean,
    val exclusionReasonCode: String?,
)

@Entity(
    tableName = "recovery_research_exercise_outcomes",
    foreignKeys = [
        ForeignKey(
            entity = RecoveryResearchOutcomeEntity::class,
            parentColumns = ["outcomeId"],
            childColumns = ["outcomeId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = RecoveryResearchTargetExerciseEntity::class,
            parentColumns = ["targetExerciseId"],
            childColumns = ["targetExerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("outcomeId"),
        Index("targetExerciseId"),
        Index("canonicalExerciseKey"),
        Index("firstWorkingSetId"),
    ],
    primaryKeys = ["exerciseOutcomeId"],
)
data class RecoveryResearchExerciseOutcomeEntity(
    val exerciseOutcomeId: String,
    val outcomeId: String,
    val targetExerciseId: String,
    val canonicalExerciseKey: String,
    val exerciseNameSnapshot: String,
    val exerciseSequence: Int,
    val trackingTypeCode: String,
    val firstWorkingSetId: Long?,
    val firstWorkingSetSetNumber: Int?,
    val firstWorkingSetRuleVersion: String,
    val firstWorkingSetObservedAtMillis: Long?,
    val achievedLoadKg: Double?,
    val achievedReps: Int?,
    val achievedRir: Int?,
    val achievedDurationSeconds: Int?,
    val achievedDistanceMeters: Double?,
    val suggestedLoadDeltaKg: Double?,
    val suggestedRepsDelta: Int?,
    val loadReduced: Boolean?,
    val repsUnexpectedlyUnderperformed: Boolean?,
    val completedValidSets: Int,
    val targetSetsSnapshot: Int,
    val exerciseCompleted: Boolean,
    val sessionQualityCode: String?,
    val matchedPlannedTarget: Boolean,
    val firstWorkingSetConfidenceCode: String,
    val validForEvaluation: Boolean,
    val exclusionReasonCode: String?,
    val outcomeCapturedAtMillis: Long,
)

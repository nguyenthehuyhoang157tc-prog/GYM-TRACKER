package com.hoctin.gymtracker.data.research

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

data class FrozenRecoveryResearchRecord(
    val prediction: RecoveryResearchPredictionEntity,
    val groups: List<RecoveryResearchPredictionGroupEntity>,
    val muscles: List<RecoveryResearchPredictionMuscleEntity>,
    val targetExercises: List<RecoveryResearchTargetExerciseEntity>,
    val initialOutcome: RecoveryResearchOutcomeEntity,
)

@Dao
abstract class RecoveryResearchDao {
    @Query("SELECT * FROM recovery_research_consent LIMIT 1")
    abstract fun observeConsent(): Flow<RecoveryResearchConsentEntity?>

    @Query("SELECT * FROM recovery_research_consent LIMIT 1")
    abstract suspend fun getConsent(): RecoveryResearchConsentEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertConsentIgnore(consent: RecoveryResearchConsentEntity): Long

    @Query(
        "UPDATE recovery_research_consent SET stateCode = :stateCode, contractVersion = :contractVersion, " +
            "grantedAtMillis = :grantedAtMillis, revokedAtMillis = :revokedAtMillis WHERE subjectId = :subjectId"
    )
    abstract suspend fun updateConsentState(
        subjectId: String,
        stateCode: String,
        contractVersion: String,
        grantedAtMillis: Long?,
        revokedAtMillis: Long?,
    ): Int

    @Query("SELECT * FROM recovery_research_predictions WHERE targetWorkoutAttemptId = :attemptId LIMIT 1")
    abstract suspend fun getPredictionByAttempt(attemptId: String): RecoveryResearchPredictionEntity?

    @Query("SELECT * FROM recovery_research_predictions WHERE predictionId = :predictionId LIMIT 1")
    abstract suspend fun getPrediction(predictionId: String): RecoveryResearchPredictionEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertPredictionIgnore(prediction: RecoveryResearchPredictionEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertPredictionGroups(groups: List<RecoveryResearchPredictionGroupEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertPredictionMuscles(muscles: List<RecoveryResearchPredictionMuscleEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertTargetExercises(targets: List<RecoveryResearchTargetExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertInitialOutcome(outcome: RecoveryResearchOutcomeEntity)

    /** Unique attempt key + INSERT IGNORE makes repeated/recomposed/concurrent calls return one row. */
    @Transaction
    open suspend fun getOrInsertFrozen(record: FrozenRecoveryResearchRecord): RecoveryResearchPredictionEntity {
        getPredictionByAttempt(record.prediction.targetWorkoutAttemptId)?.let { return it }
        val inserted = insertPredictionIgnore(record.prediction)
        if (inserted != -1L) {
            insertPredictionGroups(record.groups)
            insertPredictionMuscles(record.muscles)
            insertTargetExercises(record.targetExercises)
            insertInitialOutcome(record.initialOutcome)
        }
        return requireNotNull(getPredictionByAttempt(record.prediction.targetWorkoutAttemptId))
    }

    @Query(
        "SELECT * FROM recovery_research_prediction_groups WHERE predictionId = :predictionId " +
            "ORDER BY groupCode"
    )
    abstract suspend fun getPredictionGroups(predictionId: String): List<RecoveryResearchPredictionGroupEntity>

    @Query(
        "SELECT * FROM recovery_research_prediction_muscles WHERE predictionId = :predictionId " +
            "ORDER BY muscleCode"
    )
    abstract suspend fun getPredictionMuscles(predictionId: String): List<RecoveryResearchPredictionMuscleEntity>

    @Query(
        "SELECT * FROM recovery_research_target_exercises WHERE predictionId = :predictionId " +
            "ORDER BY targetOrdinal, targetExerciseId"
    )
    abstract suspend fun getTargetExercises(predictionId: String): List<RecoveryResearchTargetExerciseEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertSetObservationIgnore(observation: RecoveryResearchSetObservationEntity): Long

    @Query(
        "SELECT * FROM recovery_research_set_observations WHERE predictionId = :predictionId " +
            "ORDER BY firstValidAtMillis, planExerciseIdSnapshot, draftSetOrdinal"
    )
    abstract suspend fun getSetObservations(predictionId: String): List<RecoveryResearchSetObservationEntity>

    @Query(
        "SELECT * FROM recovery_research_set_observations WHERE predictionId = :predictionId " +
        "AND planExerciseIdSnapshot = :planExerciseId AND draftSetKey = :draftSetKey LIMIT 1"
    )
    abstract suspend fun getSetObservation(
        predictionId: String,
        planExerciseId: Long,
        draftSetKey: String,
    ): RecoveryResearchSetObservationEntity?

    @Query("SELECT MIN(firstValidAtMillis) FROM recovery_research_set_observations WHERE predictionId = :predictionId")
    abstract suspend fun firstValidSetObservedAt(predictionId: String): Long?

    @Query("SELECT * FROM recovery_research_outcomes WHERE predictionId = :predictionId LIMIT 1")
    abstract suspend fun getOutcomeForPrediction(predictionId: String): RecoveryResearchOutcomeEntity?

    @Query("SELECT * FROM recovery_research_outcomes WHERE targetWorkoutAttemptId = :attemptId LIMIT 1")
    abstract suspend fun getOutcomeByAttempt(attemptId: String): RecoveryResearchOutcomeEntity?

    /** Terminal transition is accepted only from ACTIVE. Repeating the same action is idempotent. */
    @Query(
        "UPDATE recovery_research_outcomes SET sessionId = :sessionId, statusCode = :statusCode, " +
            "statusReasonCode = :statusReasonCode, workoutCompletedAtMillis = :completedAtMillis, " +
            "workoutAbandonedAtMillis = :abandonedAtMillis, " +
            "firstValidSetObservedAtMillis = :firstValidSetObservedAtMillis, " +
            "firstWorkingSetLoggedAtMillis = :firstWorkingSetLoggedAtMillis, " +
            "outcomeCapturedAtMillis = :outcomeCapturedAtMillis, dataCompletenessCode = :dataCompletenessCode, " +
            "validForEvaluation = :validForEvaluation, exclusionReasonCode = :exclusionReasonCode " +
            "WHERE predictionId = :predictionId AND statusCode = 'ACTIVE'"
    )
    abstract suspend fun transitionActiveOutcome(
        predictionId: String,
        sessionId: Long?,
        statusCode: String,
        statusReasonCode: String?,
        completedAtMillis: Long?,
        abandonedAtMillis: Long?,
        firstValidSetObservedAtMillis: Long?,
        firstWorkingSetLoggedAtMillis: Long?,
        outcomeCapturedAtMillis: Long,
        dataCompletenessCode: String,
        validForEvaluation: Boolean,
        exclusionReasonCode: String?,
    ): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertExerciseOutcomesIgnore(outcomes: List<RecoveryResearchExerciseOutcomeEntity>): List<Long>

    @Query(
        "SELECT * FROM recovery_research_exercise_outcomes WHERE outcomeId = :outcomeId " +
            "ORDER BY exerciseSequence, exerciseOutcomeId"
    )
    abstract suspend fun getExerciseOutcomes(outcomeId: String): List<RecoveryResearchExerciseOutcomeEntity>

    // Deterministically ordered export reads. Research export never joins mutable production tables.
    @Query("SELECT * FROM recovery_research_consent ORDER BY subjectId")
    abstract suspend fun getAllConsent(): List<RecoveryResearchConsentEntity>

    @Query("SELECT * FROM recovery_research_predictions ORDER BY predictionGeneratedAtMillis, predictionId")
    abstract suspend fun getAllPredictions(): List<RecoveryResearchPredictionEntity>

    @Query("SELECT * FROM recovery_research_prediction_groups ORDER BY predictionId, groupCode")
    abstract suspend fun getAllPredictionGroups(): List<RecoveryResearchPredictionGroupEntity>

    @Query("SELECT * FROM recovery_research_prediction_muscles ORDER BY predictionId, muscleCode")
    abstract suspend fun getAllPredictionMuscles(): List<RecoveryResearchPredictionMuscleEntity>

    @Query("SELECT * FROM recovery_research_target_exercises ORDER BY predictionId, targetOrdinal, targetExerciseId")
    abstract suspend fun getAllTargetExercises(): List<RecoveryResearchTargetExerciseEntity>

    @Query(
        "SELECT * FROM recovery_research_set_observations " +
            "ORDER BY predictionId, firstValidAtMillis, planExerciseIdSnapshot, draftSetOrdinal"
    )
    abstract suspend fun getAllSetObservations(): List<RecoveryResearchSetObservationEntity>

    @Query("SELECT * FROM recovery_research_outcomes ORDER BY predictionId, outcomeId")
    abstract suspend fun getAllOutcomes(): List<RecoveryResearchOutcomeEntity>

    @Query("SELECT * FROM recovery_research_exercise_outcomes ORDER BY outcomeId, exerciseSequence, exerciseOutcomeId")
    abstract suspend fun getAllExerciseOutcomes(): List<RecoveryResearchExerciseOutcomeEntity>

    /** Explicit user deletion. Consent deletion cascades through the entire isolated research graph. */
    @Query("DELETE FROM recovery_research_consent")
    abstract suspend fun deleteAllResearchData(): Int
}

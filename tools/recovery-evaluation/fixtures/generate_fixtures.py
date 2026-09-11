#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any


TOOL_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(TOOL_ROOT / "src"))

from recovery_evaluation.evaluator import canonical_json  # noqa: E402
from recovery_evaluation.payload_hash import frozen_payload_sha256  # noqa: E402


READINESS_VALUES = [10, 35, 60, 80, 95]
EXERCISES = ["synthetic:bench", "synthetic:row", "synthetic:squat", "synthetic:rdl"]
MUSCLES = ["CHEST", "BACK", "QUADS", "HAMSTRINGS"]


def make_export(workouts: int, behavior: str, targets_per_workout: int = 2) -> dict[str, Any]:
    root: dict[str, Any] = {
        "contractVersion": "WEARABLE_1A_V1",
        "exportSchemaVersion": 1,
        "roomSchemaVersion": 9,
        "consent": [{
            "subjectId": "synthetic-subject",
            "stateCode": "GRANTED",
            "contractVersion": "WEARABLE_1A_V1",
            "createdAtMillis": 1_700_000_000_000,
            "grantedAtMillis": 1_700_000_000_000,
            "revokedAtMillis": None,
        }],
        "predictions": [],
        "predictionGroups": [],
        "predictionMuscles": [],
        "targetExercises": [],
        "setObservations": [],
        "outcomes": [],
        "exerciseOutcomes": [],
    }
    for workout_index in range(workouts):
        cutoff = 1_700_100_000_000 + workout_index * 86_400_000
        prediction_id = f"prediction-{workout_index:04d}"
        attempt_id = f"attempt-{workout_index:04d}"
        outcome_id = f"outcome-{workout_index:04d}"
        prediction = {
            "predictionId": prediction_id,
            "targetWorkoutAttemptId": attempt_id,
            "subjectId": "synthetic-subject",
            "sourceCode": "PRE_FIRST_VALID_WORKING_SET",
            "workoutCreatedAtMillis": cutoff - 120_000,
            "workoutStartedAtMillis": cutoff - 60_000,
            "predictionGeneratedAtMillis": cutoff + 10,
            "inputDataCutoffAtMillis": cutoff,
            "frozenAtMillis": cutoff + 10,
            "recoveryModelVersion": "MUSCLE_4B_BASELINE",
            "muscleHeatModelVersion": "MUSCLE_HEAT_1_STIMULUS_QUALITY",
            "progressiveOverloadModelVersion": "PROGRESSIVE_OVERLOAD_V3_11D",
            "researchContractVersion": "WEARABLE_1A_V1",
            "researchSchemaVersion": 1,
            "firstWorkingSetRuleVersion": "TRACKING_AWARE_FIRST_WORKING_SET_V1",
            "catalogContentVersion": 1,
            "planDayIdSnapshot": 1,
            "planDayNameSnapshot": "Synthetic",
            "overallReadiness": 60,
            "overallStatusCode": "RECOVERING",
            "inputSessionIdsJson": "[]",
            "inputSessionMaxTimestampMillis": cutoff - 86_400_000,
            "inputDigestSha256": "0" * 64,
            "dataCompletenessCode": "COMPLETE",
            "abstained": False,
            "abstentionReasonCode": None,
            "payloadSha256": "",
        }
        groups = [{
            "predictionId": prediction_id,
            "groupCode": "SYNTHETIC",
            "readiness": 60,
            "statusCode": "RECOVERING",
            "trained": True,
        }]
        muscles = []
        targets = []
        for target_index in range(targets_per_workout):
            global_index = workout_index * targets_per_workout + target_index
            exercise_index = global_index % len(EXERCISES)
            readiness = READINESS_VALUES[global_index % len(READINESS_VALUES)]
            canonical = EXERCISES[exercise_index]
            muscle = MUSCLES[exercise_index]
            target_id = f"target-{workout_index:04d}-{target_index}"
            target = {
                "targetExerciseId": target_id,
                "predictionId": prediction_id,
                "targetOrdinal": target_index,
                "planExerciseIdSnapshot": 100 + exercise_index,
                "exerciseNameSnapshot": f"Synthetic exercise {exercise_index}",
                "canonicalExerciseKey": canonical,
                "trackingTypeCode": "REPS_WEIGHT",
                "targetSetsSnapshot": 3,
                "targetMinRepsSnapshot": 8,
                "targetMaxRepsSnapshot": 12,
                "primaryMuscleCodesJson": json.dumps([muscle], separators=(",", ":")),
                "secondaryMuscleCodesJson": "[]",
                "primaryReadinessMin": readiness,
                "primaryReadinessMean": float(readiness),
                "secondaryReadinessMin": None,
                "suggestedLoadKg": 50.0,
                "suggestedMinReps": 8,
                "suggestedMaxReps": 12,
                "suggestedRir": None,
                "suggestionActionCode": "HOLD",
                "progressionTypeCode": "STANDARD",
                "suggestionStrategyCode": "NONE",
                "suggestionTargetText": "8-12 reps",
                "suggestionReasonText": "Synthetic fixture",
                "suggestionTitleText": "Hold",
                "suggestionDetailText": "Synthetic fixture",
                "suggestionConfidence": 75,
                "sessionQualityCode": "GOOD",
                "coachInsightCode": "NONE",
                "referenceSessionId": 10_000 + workout_index,
                "referenceSessionStartedAtMillis": cutoff - 172_800_000,
                "referenceSetNumber": 1,
                "referenceLoadKg": 50.0,
                "referenceReps": 8,
                "referenceRir": 2,
                "referenceDurationSeconds": None,
                "referenceDistanceMeters": None,
                "referencePerformanceValue": 400.0,
                "dataCompletenessCode": "COMPLETE",
                "abstained": False,
                "abstentionReasonCode": None,
            }
            targets.append(target)
            muscles.append({
                "predictionId": prediction_id,
                "muscleCode": muscle,
                "readiness": readiness,
                "fatigueScore": float(100 - readiness),
                "acuteResidual": float(100 - readiness) * 0.6,
                "structuralResidual": float(100 - readiness) * 0.4,
                "statusCode": status_for(readiness),
                "colorSemanticCode": status_for(readiness),
                "soreWindow": readiness < 50,
                "grindCarried": False,
                "lastTrainedAtMillis": cutoff - 172_800_000,
            })
            if behavior == "positive":
                attained = readiness >= 50
            elif behavior == "inverted":
                attained = readiness < 50
            else:
                attained = (global_index % 2) == 0
            achieved_reps = 10 if attained else 5
            root["setObservations"].append({
                "setObservationId": f"observation-{workout_index:04d}-{target_index}",
                "predictionId": prediction_id,
                "targetWorkoutAttemptId": attempt_id,
                "planExerciseIdSnapshot": 100 + exercise_index,
                "canonicalExerciseKey": canonical,
                "draftSetKey": f"draft-{workout_index:04d}-{target_index}",
                "draftSetOrdinal": 0,
                "firstValidAtMillis": cutoff + 1_000,
            })
            root["exerciseOutcomes"].append({
                "exerciseOutcomeId": f"exercise-outcome-{workout_index:04d}-{target_index}",
                "outcomeId": outcome_id,
                "targetExerciseId": target_id,
                "canonicalExerciseKey": canonical,
                "exerciseNameSnapshot": f"Synthetic exercise {exercise_index}",
                "exerciseSequence": target_index,
                "trackingTypeCode": "REPS_WEIGHT",
                "firstWorkingSetId": 100_000 + global_index,
                "firstWorkingSetSetNumber": 1,
                "firstWorkingSetRuleVersion": "MODAL_LOAD_TIE_HEAVIEST_V1",
                "firstWorkingSetObservedAtMillis": cutoff + 1_000,
                "achievedLoadKg": 50.0,
                "achievedReps": achieved_reps,
                "achievedRir": 2,
                "achievedDurationSeconds": None,
                "achievedDistanceMeters": None,
                "suggestedLoadDeltaKg": 0.0,
                "suggestedRepsDelta": achieved_reps - 8,
                "loadReduced": False,
                "repsUnexpectedlyUnderperformed": not attained,
                "completedValidSets": 3,
                "targetSetsSnapshot": 3,
                "exerciseCompleted": True,
                "sessionQualityCode": "GOOD",
                "matchedPlannedTarget": True,
                "firstWorkingSetConfidenceCode": "MODERATE",
                "validForEvaluation": True,
                "exclusionReasonCode": None,
                "outcomeCapturedAtMillis": cutoff + 7_200_000,
            })
        unique_muscles = {row["muscleCode"]: row for row in muscles}
        muscles = [unique_muscles[key] for key in sorted(unique_muscles)]
        prediction["payloadSha256"] = frozen_payload_sha256(prediction, groups, muscles, targets)
        root["predictions"].append(prediction)
        root["predictionGroups"].extend(groups)
        root["predictionMuscles"].extend(muscles)
        root["targetExercises"].extend(targets)
        root["outcomes"].append({
            "outcomeId": outcome_id,
            "predictionId": prediction_id,
            "targetWorkoutAttemptId": attempt_id,
            "sessionId": 20_000 + workout_index,
            "statusCode": "COMPLETED",
            "statusReasonCode": None,
            "workoutCompletedAtMillis": cutoff + 7_200_000,
            "workoutAbandonedAtMillis": None,
            "firstValidSetObservedAtMillis": cutoff + 1_000,
            "firstWorkingSetLoggedAtMillis": cutoff + 1_000,
            "outcomeCapturedAtMillis": cutoff + 7_200_000,
            "dataCompletenessCode": "COMPLETE",
            "validForEvaluation": True,
            "exclusionReasonCode": None,
        })
    for key in ["predictions", "predictionGroups", "predictionMuscles", "targetExercises", "setObservations", "outcomes", "exerciseOutcomes"]:
        root[key] = sorted(root[key], key=lambda row: tuple(str(value) for value in row.values()))
    return root


def status_for(readiness: int) -> str:
    if readiness < 22:
        return "OVERLOADED"
    if readiness < 50:
        return "SORE"
    if readiness < 72:
        return "RECOVERING"
    if readiness < 88:
        return "PRIMED"
    return "READY"


def write_fixture(output: Path, name: str, data: dict[str, Any]) -> None:
    output.mkdir(parents=True, exist_ok=True)
    (output / name).write_bytes(canonical_json(data))


def rehash_export(data: dict[str, Any]) -> None:
    for prediction in data["predictions"]:
        prediction_id = prediction["predictionId"]
        prediction["payloadSha256"] = frozen_payload_sha256(
            prediction,
            [row for row in data["predictionGroups"] if row["predictionId"] == prediction_id],
            [row for row in data["predictionMuscles"] if row["predictionId"] == prediction_id],
            [row for row in data["targetExercises"] if row["predictionId"] == prediction_id],
        )


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", default=str(TOOL_ROOT / "fixtures" / "generated"))
    arguments = parser.parse_args()
    output = Path(arguments.output)
    positive = make_export(130, "positive")
    no_signal = make_export(130, "none")
    inverted = make_export(130, "inverted")
    insufficient = make_export(10, "positive")
    corrupt = make_export(5, "positive")
    corrupt["predictions"][0]["overallReadiness"] = 99
    heavy_missingness = make_export(10, "positive")
    for target in heavy_missingness["targetExercises"]:
        target["suggestionConfidence"] = None
        target["referenceRir"] = None
        target["referencePerformanceValue"] = None
    rehash_export(heavy_missingness)
    first_set_ambiguity = make_export(5, "positive")
    first_set_ambiguity["exerciseOutcomes"][0]["achievedReps"] = None
    multiple_model_versions = make_export(5, "positive")
    multiple_model_versions["predictions"][0]["recoveryModelVersion"] = "SYNTHETIC_OTHER_MODEL"
    rehash_export(multiple_model_versions)
    leaky = make_export(5, "positive")
    leaky_prediction = {row["predictionId"]: row for row in leaky["predictions"]}
    leaky["outcomes"][0]["firstWorkingSetLoggedAtMillis"] = (
        leaky_prediction[leaky["outcomes"][0]["predictionId"]]["inputDataCutoffAtMillis"] - 1
    )
    write_fixture(output, "sufficient_signal.json", positive)
    write_fixture(output, "no_incremental_signal.json", no_signal)
    write_fixture(output, "inverted_signal.json", inverted)
    write_fixture(output, "insufficient_data.json", insufficient)
    write_fixture(output, "integrity_blocked.json", corrupt)
    write_fixture(output, "heavy_missingness.json", heavy_missingness)
    write_fixture(output, "first_set_ambiguity.json", first_set_ambiguity)
    write_fixture(output, "multiple_model_versions.json", multiple_model_versions)
    write_fixture(output, "future_leakage.json", leaky)
    print(output.resolve())
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

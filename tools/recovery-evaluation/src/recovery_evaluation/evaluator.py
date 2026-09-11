from __future__ import annotations

import hashlib
import json
import math
from collections import Counter, defaultdict
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

from . import EVALUATOR_VERSION
from .payload_hash import frozen_payload_sha256
from .protocol import LoadedProtocol
from .statistics import (
    FoldPreprocessor,
    RidgeLogistic,
    auroc,
    brier_score,
    calibration_summary,
    log_loss,
    moving_block_bootstrap,
    pr_auc,
    reliability_table,
)


class ExportError(ValueError):
    pass


ROOT_ARRAYS = [
    "consent",
    "predictions",
    "predictionGroups",
    "predictionMuscles",
    "targetExercises",
    "setObservations",
    "outcomes",
    "exerciseOutcomes",
]

ID_FIELDS = {
    "predictions": "predictionId",
    "targetExercises": "targetExerciseId",
    "setObservations": "setObservationId",
    "outcomes": "outcomeId",
    "exerciseOutcomes": "exerciseOutcomeId",
}

STATUS_ORDER = ["OVERLOADED", "SORE", "RECOVERING", "PRIMED", "READY"]
TERMINAL_OUTCOMES = {"COMPLETED", "PARTIAL", "ABANDONED", "UNKNOWN_INTERRUPTED"}
OUTCOME_STATUSES = TERMINAL_OUTCOMES | {"ACTIVE"}
CONFIDENCE_CODES = {"HIGH", "MODERATE", "LOW", "NONE"}
COMPLETENESS_CODES = {"COMPLETE", "PARTIAL", "NONE"}
RESEARCH_REASONS = {
    "NO_VALID_TARGET_EXERCISE", "NO_BASELINE_HISTORY", "INSUFFICIENT_READINESS_INPUT",
    "NO_PO_SUGGESTION", "NO_VALID_FIRST_WORKING_SET", "SESSION_INTERRUPTED",
    "USER_CONFIRMED_DISCARD", "OUTCOME_AMBIGUOUS", "TRACKING_TYPE_UNSUPPORTED_FOR_METRIC",
    "TIMESTAMP_INCONSISTENT", "DUPLICATE_ATTEMPT_PREVENTED", "LATE_EXISTING_VALID_DRAFT",
    "NO_PRIMARY_MUSCLE_MAPPING", "SUGGESTED_RIR_UNAVAILABLE",
}
TRACKING_TYPES = {
    "REPS_WEIGHT", "BODYWEIGHT_REPS", "SKILL_FORM", "TIME_ONLY", "DISTANCE_ONLY",
    "TIME_DISTANCE", "TIME_LOAD", "DISTANCE_LOAD", "TIME_DISTANCE_LOAD",
}


def canonical_json(value: Any) -> bytes:
    return (json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"), allow_nan=False) + "\n").encode("utf-8")


def _no_duplicate_keys(pairs: list[tuple[str, Any]]) -> dict[str, Any]:
    result: dict[str, Any] = {}
    for key, value in pairs:
        if key in result:
            raise ExportError(f"Duplicate JSON object key: {key}")
        result[key] = value
    return result


def load_export(path: str | Path) -> tuple[dict[str, Any], str]:
    raw = Path(path).read_bytes()
    digest = hashlib.sha256(raw).hexdigest()
    try:
        data = json.loads(raw, object_pairs_hook=_no_duplicate_keys)
    except (json.JSONDecodeError, UnicodeDecodeError, ExportError) as exc:
        raise ExportError(f"Malformed WEARABLE-1A JSON: {exc}") from exc
    if not isinstance(data, dict):
        raise ExportError("WEARABLE-1A export root must be an object")
    for name in ROOT_ARRAYS:
        if not isinstance(data.get(name), list):
            raise ExportError(f"Missing or non-array export field: {name}")
        if any(not isinstance(row, dict) for row in data[name]):
            raise ExportError(f"Export array contains a non-object row: {name}")
    return data, digest


def _index(rows: list[dict[str, Any]], key: str) -> dict[Any, dict[str, Any]]:
    return {row.get(key): row for row in rows}


def _safe_ratio(numerator: int, denominator: int) -> float | None:
    return numerator / denominator if denominator else None


def _sorted_counts(values: list[Any]) -> dict[str, int]:
    return dict(sorted(Counter(str(value) for value in values).items()))


@dataclass
class AuditContext:
    critical: list[dict[str, str]]
    warnings: list[dict[str, str]]
    invalid_prediction_ids: set[str]
    invalid_prediction_reasons: dict[str, str] = field(default_factory=dict)

    def add_critical(self, code: str, detail: str) -> None:
        item = {"code": code, "detail": detail}
        if item not in self.critical:
            self.critical.append(item)

    def add_warning(self, code: str, detail: str) -> None:
        item = {"code": code, "detail": detail}
        if item not in self.warnings:
            self.warnings.append(item)

    def invalidate_prediction(self, prediction_id: str, reason: str) -> None:
        self.invalid_prediction_ids.add(prediction_id)
        self.invalid_prediction_reasons.setdefault(prediction_id, reason)


def validate_export(data: dict[str, Any], protocol: dict[str, Any]) -> tuple[AuditContext, dict[str, Any]]:
    context = AuditContext([], [], set())
    gates = protocol["data_integrity_gates"]
    if data.get("exportSchemaVersion") not in gates["allowed_export_schema_versions"]:
        context.add_critical("EXPORT_SCHEMA_UNSUPPORTED", f"exportSchemaVersion={data.get('exportSchemaVersion')}")
    if data.get("contractVersion") not in gates["allowed_research_contract_versions"]:
        context.add_critical("CONTRACT_VERSION_UNSUPPORTED", f"contractVersion={data.get('contractVersion')}")
    if data.get("roomSchemaVersion") not in gates["allowed_room_schema_versions"]:
        context.add_critical("EXPORT_SCHEMA_UNSUPPORTED", f"roomSchemaVersion={data.get('roomSchemaVersion')}")

    for array_name, id_field in ID_FIELDS.items():
        ids = [row.get(id_field) for row in data[array_name]]
        missing = [index for index, value in enumerate(ids) if not isinstance(value, (str, int)) or value == ""]
        if missing:
            context.add_critical("BROKEN_INTERNAL_LINK", f"{array_name}.{id_field} missing at rows {missing[:5]}")
        duplicates = sorted(str(value) for value, count in Counter(ids).items() if count > 1)
        if duplicates:
            code = "DUPLICATE_PREDICTION_ID" if array_name == "predictions" else "DUPLICATE_ENTITY_ID"
            context.add_critical(code, f"{array_name}: {duplicates[:5]}")

    predictions = _index(data["predictions"], "predictionId")
    targets = _index(data["targetExercises"], "targetExerciseId")
    outcomes = _index(data["outcomes"], "outcomeId")
    attempts = [row.get("targetWorkoutAttemptId") for row in data["predictions"]]
    duplicate_attempts = sorted(str(value) for value, count in Counter(attempts).items() if count > 1)
    if duplicate_attempts:
        context.add_critical("DUPLICATE_WORKOUT_ATTEMPT_ID", str(duplicate_attempts[:5]))

    for consent in data["consent"]:
        if consent.get("stateCode") not in {"GRANTED", "REVOKED"}:
            context.add_critical("BROKEN_INTERNAL_LINK", f"Unknown consent state: {consent.get('stateCode')}")
        if consent.get("contractVersion") not in gates["allowed_research_contract_versions"]:
            context.add_critical("CONTRACT_VERSION_UNSUPPORTED", "Consent contract version mismatch")

    for array_name in ["predictions", "targetExercises", "outcomes"]:
        for row in data[array_name]:
            if row.get("dataCompletenessCode") not in COMPLETENESS_CODES:
                context.add_critical("BROKEN_INTERNAL_LINK", f"Unknown completeness in {array_name}: {row.get('dataCompletenessCode')}")
    for prediction in data["predictions"]:
        if prediction.get("overallStatusCode") not in STATUS_ORDER:
            context.add_critical("BROKEN_INTERNAL_LINK", f"Unknown overall readiness status: {prediction.get('overallStatusCode')}")
    for array_name, reason_fields in [
        ("predictions", ["abstentionReasonCode"]),
        ("targetExercises", ["abstentionReasonCode"]),
        ("outcomes", ["statusReasonCode", "exclusionReasonCode"]),
        ("exerciseOutcomes", ["exclusionReasonCode"]),
    ]:
        for row in data[array_name]:
            for reason_field in reason_fields:
                reason = row.get(reason_field)
                if reason is not None and reason not in RESEARCH_REASONS:
                    context.add_critical("BROKEN_INTERNAL_LINK", f"Unknown {reason_field} in {array_name}: {reason}")
    for array_name in ["predictions", "targetExercises"]:
        for row in data[array_name]:
            if bool(row.get("abstained")) != (row.get("abstentionReasonCode") is not None):
                context.add_critical("BROKEN_INTERNAL_LINK", f"Abstention/reason contradiction in {array_name}")
    for target in data["targetExercises"]:
        if target.get("suggestedRir") is not None:
            context.add_critical("CONTRACT_VERSION_UNSUPPORTED", "WEARABLE-1A V1 suggestedRir must remain unavailable/null")

    child_links = [
        ("predictionGroups", "predictionId", predictions),
        ("predictionMuscles", "predictionId", predictions),
        ("targetExercises", "predictionId", predictions),
        ("setObservations", "predictionId", predictions),
        ("outcomes", "predictionId", predictions),
        ("exerciseOutcomes", "outcomeId", outcomes),
        ("exerciseOutcomes", "targetExerciseId", targets),
    ]
    for array_name, key, parent in child_links:
        broken = [str(row.get(key)) for row in data[array_name] if row.get(key) not in parent]
        if broken:
            context.add_critical("BROKEN_INTERNAL_LINK", f"{array_name}.{key}: {broken[:5]}")

    outcome_by_prediction: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for outcome in data["outcomes"]:
        outcome_by_prediction[str(outcome.get("predictionId"))].append(outcome)
        prediction = predictions.get(outcome.get("predictionId"))
        if prediction and outcome.get("targetWorkoutAttemptId") != prediction.get("targetWorkoutAttemptId"):
            context.add_critical("BROKEN_INTERNAL_LINK", f"Outcome attempt mismatch: {outcome.get('outcomeId')}")
        if outcome.get("statusCode") not in OUTCOME_STATUSES:
            context.add_critical("BROKEN_INTERNAL_LINK", f"Unknown outcome status: {outcome.get('statusCode')}")
    if any(len(rows) > 1 for rows in outcome_by_prediction.values()):
        context.add_critical("DUPLICATE_ENTITY_ID", "More than one outcome links to one prediction")

    exercise_outcomes_by_target: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for row in data["exerciseOutcomes"]:
        exercise_outcomes_by_target[str(row.get("targetExerciseId"))].append(row)
        target = targets.get(row.get("targetExerciseId"))
        if target and (
            row.get("canonicalExerciseKey") != target.get("canonicalExerciseKey")
            or row.get("trackingTypeCode") != target.get("trackingTypeCode")
        ):
            context.add_critical("BROKEN_INTERNAL_LINK", f"Exercise outcome target mismatch: {row.get('exerciseOutcomeId')}")
        if row.get("firstWorkingSetConfidenceCode") not in CONFIDENCE_CODES:
            context.add_critical("BROKEN_INTERNAL_LINK", f"Unknown confidence: {row.get('firstWorkingSetConfidenceCode')}")
    if any(len(rows) > 1 for rows in exercise_outcomes_by_target.values()):
        context.add_critical("DUPLICATE_ENTITY_ID", "More than one exercise outcome links to one target")

    allowed_models = gates["allowed_model_versions"]
    for prediction in data["predictions"]:
        prediction_id = str(prediction.get("predictionId"))
        model_fields = ["recoveryModelVersion", "muscleHeatModelVersion", "progressiveOverloadModelVersion"]
        if any(prediction.get(field) not in allowed_models[field] for field in model_fields):
            context.add_critical("MODEL_VERSION_MISMATCH", f"predictionId={prediction_id}")
            context.invalidate_prediction(prediction_id, "MODEL_VERSION_MISMATCH")
        if prediction.get("researchContractVersion") not in gates["allowed_research_contract_versions"]:
            context.add_critical("CONTRACT_VERSION_UNSUPPORTED", f"predictionId={prediction_id}")
            context.invalidate_prediction(prediction_id, "CONTRACT_VERSION_UNSUPPORTED")
        if prediction.get("researchSchemaVersion") not in gates["allowed_research_schema_versions"]:
            context.add_critical("EXPORT_SCHEMA_UNSUPPORTED", f"predictionId={prediction_id}")
            context.invalidate_prediction(prediction_id, "EXPORT_SCHEMA_UNSUPPORTED")
        if prediction.get("firstWorkingSetRuleVersion") not in gates["allowed_first_working_set_contract_versions"]:
            context.add_critical("CONTRACT_VERSION_UNSUPPORTED", f"predictionId={prediction_id} first-working-set contract")
            context.invalidate_prediction(prediction_id, "CONTRACT_VERSION_UNSUPPORTED")
        try:
            created = int(prediction["workoutCreatedAtMillis"])
            started = int(prediction["workoutStartedAtMillis"])
            cutoff = int(prediction["inputDataCutoffAtMillis"])
            generated = int(prediction["predictionGeneratedAtMillis"])
            frozen = int(prediction["frozenAtMillis"])
            input_max = prediction.get("inputSessionMaxTimestampMillis")
            if not (created <= started <= cutoff <= generated and cutoff <= frozen):
                raise ValueError
            if input_max is not None and int(input_max) > cutoff:
                context.add_critical("INPUT_MAX_AFTER_CUTOFF", f"predictionId={prediction_id}")
                context.invalidate_prediction(prediction_id, "INVALID_INPUT_CUTOFF")
        except (KeyError, TypeError, ValueError):
            context.add_critical("TIMESTAMP_INCONSISTENT", f"predictionId={prediction_id}")
            context.invalidate_prediction(prediction_id, "TIMESTAMP_INCONSISTENT")

        children_groups = [row for row in data["predictionGroups"] if row.get("predictionId") == prediction.get("predictionId")]
        children_muscles = [row for row in data["predictionMuscles"] if row.get("predictionId") == prediction.get("predictionId")]
        children_targets = [row for row in data["targetExercises"] if row.get("predictionId") == prediction.get("predictionId")]
        try:
            expected_hash = frozen_payload_sha256(prediction, children_groups, children_muscles, children_targets)
        except (KeyError, TypeError, ValueError) as exc:
            expected_hash = None
            context.add_critical("PAYLOAD_HASH_INVALID", f"predictionId={prediction_id}: {exc}")
            context.invalidate_prediction(prediction_id, "PAYLOAD_HASH_INVALID")
        if expected_hash is not None and str(prediction.get("payloadSha256", "")).lower() != expected_hash:
            context.add_critical("PAYLOAD_HASH_INVALID", f"predictionId={prediction_id}")
            context.invalidate_prediction(prediction_id, "PAYLOAD_HASH_INVALID")

    for target in data["targetExercises"]:
        if target.get("trackingTypeCode") not in TRACKING_TYPES:
            context.add_warning("TRACKING_TYPE_UNSUPPORTED", f"targetExerciseId={target.get('targetExerciseId')}")
        reference_time = target.get("referenceSessionStartedAtMillis")
        prediction = predictions.get(target.get("predictionId"))
        if reference_time is not None and prediction and reference_time > prediction.get("inputDataCutoffAtMillis", -1):
            context.add_critical("TIMESTAMP_INCONSISTENT", f"Future reference on target {target.get('targetExerciseId')}")

    for outcome in data["outcomes"]:
        prediction = predictions.get(outcome.get("predictionId"))
        if not prediction:
            continue
        cutoff = prediction.get("inputDataCutoffAtMillis")
        for field in ["firstValidSetObservedAtMillis", "firstWorkingSetLoggedAtMillis", "outcomeCapturedAtMillis"]:
            value = outcome.get(field)
            if value is not None and cutoff is not None and value < cutoff:
                context.add_critical("TIMESTAMP_INCONSISTENT", f"{field} before cutoff on {outcome.get('outcomeId')}")
        terminal_time = outcome.get("workoutCompletedAtMillis") or outcome.get("workoutAbandonedAtMillis")
        if terminal_time is not None and terminal_time < prediction.get("workoutStartedAtMillis", terminal_time):
            context.add_critical("TIMESTAMP_INCONSISTENT", f"Terminal time before workout start on {outcome.get('outcomeId')}")

    outcome_count = len(data["outcomes"])
    active_count = sum(row.get("statusCode") == "ACTIVE" for row in data["outcomes"])
    active_proportion = _safe_ratio(active_count, outcome_count) or 0.0
    if active_proportion > gates["maximum_active_outcome_proportion"]:
        context.add_critical("EXCESSIVE_ACTIVE_OUTCOMES", f"proportion={active_proportion:.6f}")
    targets_without_outcome = sum(not outcome_by_prediction.get(str(row.get("predictionId"))) for row in data["targetExercises"])
    linkage_failure = _safe_ratio(targets_without_outcome, len(data["targetExercises"])) or 0.0
    if linkage_failure > gates["maximum_outcome_linkage_failure_proportion"]:
        context.add_critical("OUTCOME_LINKAGE_FAILURE", f"proportion={linkage_failure:.6f}")

    distributions = {
        "recovery_model_versions": _sorted_counts([row.get("recoveryModelVersion") for row in data["predictions"]]),
        "muscle_heat_model_versions": _sorted_counts([row.get("muscleHeatModelVersion") for row in data["predictions"]]),
        "progressive_overload_model_versions": _sorted_counts([row.get("progressiveOverloadModelVersion") for row in data["predictions"]]),
        "research_contract_versions": _sorted_counts([row.get("researchContractVersion") for row in data["predictions"]]),
    }
    context.critical.sort(key=lambda item: (item["code"], item["detail"]))
    context.warnings.sort(key=lambda item: (item["code"], item["detail"]))
    return context, distributions


def _parse_muscles(value: Any) -> list[str]:
    if not isinstance(value, str):
        return []
    try:
        parsed = json.loads(value)
    except json.JSONDecodeError:
        return []
    return sorted(str(item) for item in parsed) if isinstance(parsed, list) else []


def build_eligibility_ledger(data: dict[str, Any], context: AuditContext, protocol: dict[str, Any]) -> list[dict[str, Any]]:
    predictions = _index(data["predictions"], "predictionId")
    outcomes_by_prediction = {row.get("predictionId"): row for row in data["outcomes"]}
    exercise_by_target = {row.get("targetExerciseId"): row for row in data["exerciseOutcomes"]}
    duplicate_canonical: set[tuple[str, str]] = set()
    canonical_counts = Counter((str(row.get("predictionId")), str(row.get("canonicalExerciseKey"))) for row in data["targetExercises"])
    duplicate_canonical.update(key for key, count in canonical_counts.items() if count > 1)
    tolerance = float(protocol["target_attempt"]["load_tolerance_kg"])
    rows: list[dict[str, Any]] = []
    for target in data["targetExercises"]:
        prediction = predictions.get(target.get("predictionId"), {})
        outcome = outcomes_by_prediction.get(target.get("predictionId"))
        exercise = exercise_by_target.get(target.get("targetExerciseId"))
        state = "UNRESOLVED"
        reason = "OUTCOME_AMBIGUOUS"
        attempt = "TARGET_ATTEMPT_AMBIGUOUS"
        attained: int | None = None
        prediction_id = str(target.get("predictionId"))
        canonical = str(target.get("canonicalExerciseKey", ""))

        if prediction_id in context.invalid_prediction_ids:
            state = "EXCLUDED"
            reason = context.invalid_prediction_reasons.get(prediction_id, "OUTCOME_AMBIGUOUS")
        elif prediction.get("abstained"):
            state, reason = "ABSTAINED", str(prediction.get("abstentionReasonCode") or "PREDICTION_ABSTAINED")
        elif target.get("abstained"):
            state, reason = "ABSTAINED", str(target.get("abstentionReasonCode") or "TARGET_ABSTAINED")
        elif not canonical:
            state, reason = "EXCLUDED", "OUTCOME_AMBIGUOUS"
        elif not _parse_muscles(target.get("primaryMuscleCodesJson")) or target.get("primaryReadinessMin") is None:
            state, reason = "ABSTAINED", "NO_PRIMARY_MUSCLE_MAPPING"
        elif outcome is None:
            state, reason = "UNRESOLVED", "NO_LINKED_OUTCOME"
        elif outcome.get("statusCode") == "ACTIVE":
            state, reason = "UNRESOLVED", "ACTIVE_OUTCOME"
        elif outcome.get("statusCode") in {"ABANDONED", "UNKNOWN_INTERRUPTED"}:
            state, reason = "EXCLUDED", "ABANDONED_WITHOUT_EVALUABLE_EXERCISE"
        elif outcome.get("statusCode") not in {"COMPLETED", "PARTIAL"} or not outcome.get("validForEvaluation"):
            state, reason = "EXCLUDED", str(outcome.get("exclusionReasonCode") or "OUTCOME_AMBIGUOUS")
        elif exercise is None:
            state, reason = "UNRESOLVED", "OUTCOME_AMBIGUOUS"
        elif target.get("trackingTypeCode") != "REPS_WEIGHT":
            state, reason = "ELIGIBLE_SECONDARY_ONLY", "TRACKING_TYPE_UNSUPPORTED"
        elif exercise.get("firstWorkingSetConfidenceCode") not in {"MODERATE", "HIGH"}:
            state, reason = "ELIGIBLE_SECONDARY_ONLY", "FIRST_SET_LOW_CONFIDENCE"
        elif exercise.get("firstWorkingSetRuleVersion") != "MODAL_LOAD_TIE_HEAVIEST_V1":
            state, reason = "ELIGIBLE_SECONDARY_ONLY", "FIRST_SET_RULE_UNSUPPORTED"
        elif target.get("suggestedLoadKg") is None or target.get("suggestedMinReps") is None or target.get("suggestedMaxReps") is None:
            state, reason = "ABSTAINED", "NO_PO_SUGGESTION"
        elif (prediction_id, canonical) in duplicate_canonical:
            state, reason = "ABSTAINED", "TARGET_ATTEMPT_AMBIGUOUS"
        elif exercise.get("canonicalExerciseKey") != target.get("canonicalExerciseKey"):
            state, reason = "ABSTAINED", "TARGET_ATTEMPT_AMBIGUOUS"
        elif exercise.get("firstWorkingSetId") is None and not exercise.get("matchedPlannedTarget"):
            state, reason, attempt = "ABSTAINED", "TARGET_NOT_ATTEMPTED", "TARGET_NOT_ATTEMPTED"
        elif not exercise.get("matchedPlannedTarget") or not exercise.get("validForEvaluation"):
            state, reason = "ABSTAINED", "TARGET_ATTEMPT_AMBIGUOUS"
        elif exercise.get("achievedLoadKg") is None or exercise.get("achievedReps") is None:
            state, reason = "ABSTAINED", "TARGET_ATTEMPT_AMBIGUOUS"
        elif int(exercise.get("achievedReps")) <= 0:
            state, reason = "ABSTAINED", "TARGET_ATTEMPT_AMBIGUOUS"
        elif abs(float(exercise.get("achievedLoadKg")) - float(target.get("suggestedLoadKg"))) > tolerance:
            state, reason, attempt = "ABSTAINED", "TARGET_NOT_ATTEMPTED", "TARGET_NOT_ATTEMPTED"
        else:
            state, reason, attempt = "ELIGIBLE_PRIMARY", "ELIGIBLE_PRIMARY", "TARGET_ATTEMPTED"
            attained = int(int(exercise["achievedReps"]) >= int(target["suggestedMinReps"]))

        row = {
            "attained": attained,
            "attempt_classification": attempt,
            "canonical_exercise_key": canonical,
            "exercise_outcome_id": exercise.get("exerciseOutcomeId") if exercise else None,
            "final_state": state,
            "first_set_confidence": exercise.get("firstWorkingSetConfidenceCode") if exercise else None,
            "first_set_rule": exercise.get("firstWorkingSetRuleVersion") if exercise else None,
            "input_cutoff_at_millis": prediction.get("inputDataCutoffAtMillis"),
            "outcome_status": outcome.get("statusCode") if outcome else None,
            "prediction_id": prediction_id,
            "primary_muscle_codes": _parse_muscles(target.get("primaryMuscleCodesJson")),
            "primary_readiness_mean": target.get("primaryReadinessMean"),
            "primary_readiness_min": target.get("primaryReadinessMin"),
            "reason_code": reason,
            "secondary_readiness_min": target.get("secondaryReadinessMin"),
            "subject_id": prediction.get("subjectId"),
            "target_exercise_id": target.get("targetExerciseId"),
            "target_ordinal": target.get("targetOrdinal"),
            "tracking_type": target.get("trackingTypeCode"),
            "workout_attempt_id": prediction.get("targetWorkoutAttemptId"),
        }
        if state == "ELIGIBLE_PRIMARY":
            cutoff = int(prediction["inputDataCutoffAtMillis"])
            reference_time = target.get("referenceSessionStartedAtMillis")
            row.update({
                "achieved_load_kg": exercise.get("achievedLoadKg"),
                "achieved_reps": exercise.get("achievedReps"),
                "completed_valid_sets": exercise.get("completedValidSets"),
                "exercise_completed": exercise.get("exerciseCompleted"),
                "outcome_completed": outcome.get("statusCode") == "COMPLETED",
                "outcome_captured_at_millis": exercise.get("outcomeCapturedAtMillis"),
                "reference_load_kg": target.get("referenceLoadKg"),
                "reference_performance_value": target.get("referencePerformanceValue"),
                "reference_reps": target.get("referenceReps"),
                "reference_rir": target.get("referenceRir"),
                "reference_time_missing": reference_time is None,
                "suggested_load_kg": target.get("suggestedLoadKg"),
                "suggested_max_reps": target.get("suggestedMaxReps"),
                "suggested_min_reps": target.get("suggestedMinReps"),
                "suggestion_confidence": target.get("suggestionConfidence"),
                "target_sets_snapshot": target.get("targetSetsSnapshot"),
                "time_since_reference_hours": (cutoff - int(reference_time)) / 3_600_000.0 if reference_time is not None else None,
            })
        rows.append(row)
    rows.sort(key=lambda row: (
        row["input_cutoff_at_millis"] if row["input_cutoff_at_millis"] is not None else -1,
        str(row["workout_attempt_id"]), int(row["target_ordinal"] or 0), str(row["target_exercise_id"]),
    ))
    return rows


def _decorate_history(rows: list[dict[str, Any]]) -> list[dict[str, Any]]:
    history: dict[str, list[tuple[int, int]]] = defaultdict(list)
    decorated: list[dict[str, Any]] = []
    by_workout: dict[str, list[dict[str, Any]]] = defaultdict(list)
    workout_order: list[str] = []
    for row in rows:
        workout = str(row["workout_attempt_id"])
        if workout not in by_workout:
            workout_order.append(workout)
        by_workout[workout].append(row)
    for workout in workout_order:
        block = by_workout[workout]
        for row in block:
            copy = dict(row)
            cutoff = int(row["input_cutoff_at_millis"])
            available = [
                item for item in history.get(str(row["canonical_exercise_key"]), [])
                if item[0] <= cutoff
            ]
            previous = available[-1][1] if available else None
            copy["last_outcome"] = previous
            copy["last_outcome_missing"] = previous is None
            decorated.append(copy)
        for row in block:
            history[str(row["canonical_exercise_key"])].append(
                (int(row["outcome_captured_at_millis"]), int(row["attained"]))
            )
    return decorated


RAW_FEATURES = {
    "BASE_RATE_V1": [],
    "LAST_OUTCOME_V1": ["last_outcome"],
    "TIME_SINCE_TRAINED_V1": ["log_hours_since_reference"],
    "PO_HISTORY_ONLY_V1": [
        "suggested_load_kg", "suggested_min_reps", "suggested_max_reps", "target_sets_snapshot",
        "suggestion_confidence", "reference_load_kg", "reference_reps", "reference_rir",
        "reference_performance_value", "log_hours_since_reference", "last_outcome",
    ],
    "MUSCLE_4B_ONLY_V1": ["primary_readiness_min"],
    "PO_PLUS_MUSCLE_4B_V1": [
        "suggested_load_kg", "suggested_min_reps", "suggested_max_reps", "target_sets_snapshot",
        "suggestion_confidence", "reference_load_kg", "reference_reps", "reference_rir",
        "reference_performance_value", "log_hours_since_reference", "last_outcome", "primary_readiness_min",
    ],
}


def raw_features(row: dict[str, Any], model_id: str, readiness_key: str = "primary_readiness_min") -> list[float | None]:
    values = dict(row)
    hours = row.get("time_since_reference_hours")
    values["log_hours_since_reference"] = math.log1p(max(0.0, float(hours))) if hours is not None else None
    values["primary_readiness_min"] = row.get(readiness_key)
    return [float(values[name]) if values.get(name) is not None else None for name in RAW_FEATURES[model_id]]


def build_folds(rows: list[dict[str, Any]], minimum_history_blocks: int) -> list[dict[str, Any]]:
    by_workout: dict[str, list[dict[str, Any]]] = defaultdict(list)
    ordered_workouts: list[str] = []
    for row in rows:
        workout = str(row["workout_attempt_id"])
        if workout not in by_workout:
            ordered_workouts.append(workout)
        by_workout[workout].append(row)
    folds = []
    for index in range(1, len(ordered_workouts)):
        test_workout = ordered_workouts[index]
        test_rows = by_workout[test_workout]
        test_cutoff = min(int(row["input_cutoff_at_millis"]) for row in test_rows)
        train_workouts = [
            workout for workout in ordered_workouts[:index]
            if max(int(row["outcome_captured_at_millis"]) for row in by_workout[workout]) <= test_cutoff
        ]
        if len(train_workouts) < minimum_history_blocks:
            continue
        train_rows = [row for workout in train_workouts for row in by_workout[workout]]
        folds.append({
            "fold_id": len(folds) + 1,
            "train_rows": train_rows,
            "test_rows": test_rows,
            "train_workout_ids": train_workouts,
            "test_workout_ids": [test_workout],
        })
    return folds


def _predict_fold_model(model_id: str, train: list[dict[str, Any]], test: list[dict[str, Any]], readiness_key: str) -> list[float]:
    outcomes = [int(row["attained"]) for row in train]
    if model_id == "BASE_RATE_V1":
        probability = (sum(outcomes) + 1.0) / (len(outcomes) + 2.0)
        return [probability] * len(test)
    train_raw = [raw_features(row, model_id, readiness_key) for row in train]
    test_raw = [raw_features(row, model_id, readiness_key) for row in test]
    preprocessor = FoldPreprocessor().fit(train_raw)
    train_features = preprocessor.transform(train_raw)
    test_features = preprocessor.transform(test_raw)
    model = RidgeLogistic().fit(train_features, outcomes)
    return model.predict(test_features)


def rolling_origin(rows: list[dict[str, Any]], protocol: dict[str, Any], readiness_key: str = "primary_readiness_min") -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    decorated = _decorate_history(rows)
    folds = build_folds(decorated, int(protocol["folds"]["minimum_history_workout_blocks"]))
    prediction_rows: list[dict[str, Any]] = []
    manifest: list[dict[str, Any]] = []
    model_ids = [item["id"] for item in protocol["comparator_models"]]
    for fold in folds:
        train = fold["train_rows"]
        test = fold["test_rows"]
        model_predictions = {
            model_id: _predict_fold_model(model_id, train, test, readiness_key)
            for model_id in model_ids
        }
        for index, row in enumerate(test):
            prediction_rows.append({
                "attained": int(row["attained"]),
                "canonical_exercise_key": row["canonical_exercise_key"],
                "fold_id": fold["fold_id"],
                "primary_readiness_min": row[readiness_key],
                "primary_status": readiness_status(float(row[readiness_key])),
                "probabilities": {model_id: model_predictions[model_id][index] for model_id in model_ids},
                "target_exercise_id": row["target_exercise_id"],
                "workout_id": row["workout_attempt_id"],
            })
        train_max = max(int(row["input_cutoff_at_millis"]) for row in train)
        train_outcome_max = max(int(row["outcome_captured_at_millis"]) for row in train)
        test_min = min(int(row["input_cutoff_at_millis"]) for row in test)
        manifest.append({
            "fold_id": fold["fold_id"],
            "strictly_ordered": train_max <= test_min,
            "test_cutoff_at_millis": test_min,
            "test_row_ids": sorted(str(row["target_exercise_id"]) for row in test),
            "test_workout_ids": fold["test_workout_ids"],
            "train_max_cutoff_at_millis": train_max,
            "train_max_outcome_captured_at_millis": train_outcome_max,
            "train_outcomes_available_before_test": train_outcome_max <= test_min,
            "train_row_count": len(train),
            "train_workout_count": len(fold["train_workout_ids"]),
            "train_workout_ids": fold["train_workout_ids"],
            "workout_split": bool(set(fold["train_workout_ids"]) & set(fold["test_workout_ids"])),
        })
    return prediction_rows, manifest


def readiness_status(readiness: float) -> str:
    if readiness < 22:
        return "OVERLOADED"
    if readiness < 50:
        return "SORE"
    if readiness < 72:
        return "RECOVERING"
    if readiness < 88:
        return "PRIMED"
    return "READY"


def _model_metrics(rows: list[dict[str, Any]], model_id: str) -> dict[str, Any]:
    outcomes = [int(row["attained"]) for row in rows]
    probabilities = [float(row["probabilities"][model_id]) for row in rows]
    underperformance = [1 - outcome for outcome in outcomes]
    risk = [1.0 - probability for probability in probabilities]
    return {
        "auroc_underperformance": auroc(underperformance, risk),
        "brier_score": brier_score(outcomes, probabilities),
        "calibration": calibration_summary(outcomes, probabilities),
        "log_loss": log_loss(outcomes, probabilities),
        "pr_auc_underperformance": pr_auc(underperformance, risk),
        "reliability_table": reliability_table(outcomes, probabilities),
        "row_count": len(rows),
    }


def _status_summary(rows: list[dict[str, Any]], minimum_count: int) -> tuple[list[dict[str, Any]], bool]:
    summary = []
    qualifying_rates = []
    for status in STATUS_ORDER:
        members = [row for row in rows if row["primary_status"] == status]
        rate = sum(int(row["attained"]) for row in members) / len(members) if members else None
        summary.append({"status": status, "count": len(members), "attainment_rate": rate, "underperformance_rate": None if rate is None else 1.0 - rate})
        if len(members) >= minimum_count:
            qualifying_rates.append(rate)
    ordering = len(qualifying_rates) >= 3 and all(
        qualifying_rates[index + 1] + 0.1 >= qualifying_rates[index]
        for index in range(len(qualifying_rates) - 1)
    )
    return summary, ordering


def calculate_metrics(prediction_rows: list[dict[str, Any]], protocol: dict[str, Any], seed: int) -> dict[str, Any]:
    models = {model_id: _model_metrics(prediction_rows, model_id) for model_id in RAW_FEATURES}
    po = "PO_HISTORY_ONLY_V1"
    combined = "PO_PLUS_MUSCLE_4B_V1"
    incremental = moving_block_bootstrap(
        prediction_rows,
        lambda rows: (
            float(brier_score([int(row["attained"]) for row in rows], [float(row["probabilities"][po]) for row in rows]))
            - float(brier_score([int(row["attained"]) for row in rows], [float(row["probabilities"][combined]) for row in rows]))
        ) if rows else None,
        seed,
        int(protocol["bootstrap"]["replicates"]),
        int(protocol["bootstrap"]["workout_block_length"]),
    )
    readiness_auc = moving_block_bootstrap(
        prediction_rows,
        lambda rows: auroc([1 - int(row["attained"]) for row in rows], [100.0 - float(row["primary_readiness_min"]) for row in rows]),
        seed + 1,
        int(protocol["bootstrap"]["replicates"]),
        int(protocol["bootstrap"]["workout_block_length"]),
    )
    by_fold: dict[int, list[dict[str, Any]]] = defaultdict(list)
    for row in prediction_rows:
        by_fold[int(row["fold_id"])].append(row)
    fold_differences = []
    for fold_id in sorted(by_fold):
        fold_rows = by_fold[fold_id]
        outcomes = [int(row["attained"]) for row in fold_rows]
        po_brier = brier_score(outcomes, [float(row["probabilities"][po]) for row in fold_rows])
        combined_brier = brier_score(outcomes, [float(row["probabilities"][combined]) for row in fold_rows])
        fold_differences.append({"fold_id": fold_id, "brier_improvement": float(po_brier) - float(combined_brier)})
    improved = sum(item["brier_improvement"] > 0 for item in fold_differences)
    status_summary, status_ordering = _status_summary(
        prediction_rows, int(protocol["sample_size_gates"]["minimum_status_records_for_inferential_comparison"])
    )
    threshold = int(protocol["metrics"]["readiness_thresholds"]["not_ready_below"])
    false_ready = sum(int(row["attained"]) == 0 and float(row["primary_readiness_min"]) >= threshold for row in prediction_rows)
    ready = sum(float(row["primary_readiness_min"]) >= threshold for row in prediction_rows)
    false_not_ready = sum(int(row["attained"]) == 1 and float(row["primary_readiness_min"]) < threshold for row in prediction_rows)
    not_ready = sum(float(row["primary_readiness_min"]) < threshold for row in prediction_rows)
    by_exercise: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for row in prediction_rows:
        by_exercise[str(row["canonical_exercise_key"])].append(row)
    exercise_incremental = []
    for exercise in sorted(by_exercise):
        members = by_exercise[exercise]
        outcomes = [int(row["attained"]) for row in members]
        exercise_incremental.append({
            "canonical_exercise_key": exercise,
            "count": len(members),
            "brier_improvement": float(brier_score(outcomes, [float(row["probabilities"][po]) for row in members]))
            - float(brier_score(outcomes, [float(row["probabilities"][combined]) for row in members])),
        })
    return {
        "bootstrap": {
            "block_length": protocol["bootstrap"]["workout_block_length"],
            "method": protocol["bootstrap"]["one_subject_method"],
            "replicates": protocol["bootstrap"]["replicates"],
            "seed": seed,
        },
        "comparator_metrics": models,
        "exercise_incremental": exercise_incremental,
        "false_not_ready": {"count": false_not_ready, "rate_among_not_ready": _safe_ratio(false_not_ready, not_ready)},
        "false_ready": {"count": false_ready, "rate_among_ready": _safe_ratio(false_ready, ready)},
        "fold_improvement_proportion": _safe_ratio(improved, len(fold_differences)),
        "fold_metrics": fold_differences,
        "incremental_brier": incremental,
        "readiness_underperformance_auroc": readiness_auc,
        "status_ordering_passed": status_ordering,
        "status_summary": status_summary,
    }


def sample_gates(ledger: list[dict[str, Any]], protocol: dict[str, Any]) -> dict[str, Any]:
    primary = [row for row in ledger if row["final_state"] == "ELIGIBLE_PRIMARY"]
    blocks = {str(row["workout_attempt_id"]) for row in primary}
    successes = sum(int(row["attained"]) for row in primary)
    failures = len(primary) - successes
    total_targets = len(ledger)
    canonical_counts = Counter(str(row["canonical_exercise_key"]) for row in primary)
    status_counts = Counter(readiness_status(float(row["primary_readiness_min"])) for row in primary)
    thresholds = protocol["sample_size_gates"]
    potential_folds = len(build_folds(
        _decorate_history(primary), int(protocol["folds"]["minimum_history_workout_blocks"])
    )) if primary else 0
    checks = {
        "canonical_exercises_with_20_records": sum(count >= 20 for count in canonical_counts.values()) >= thresholds["minimum_canonical_exercises_with_20_records"],
        "eligible_exercise_outcomes": len(primary) >= thresholds["minimum_eligible_exercise_outcomes"],
        "eligible_workout_blocks": len(blocks) >= thresholds["minimum_eligible_workout_blocks"],
        "evaluable_coverage": (_safe_ratio(len(primary), total_targets) or 0.0) >= thresholds["evaluable_coverage_min"],
        "rolling_folds": potential_folds >= thresholds["minimum_rolling_folds"],
        "status_buckets": sum(count >= thresholds["minimum_status_records_for_inferential_comparison"] for count in status_counts.values()) >= 3,
        "target_attainment_successes": successes >= thresholds["minimum_target_attainment_successes"],
        "underperformance_events": failures >= thresholds["minimum_underperformance_events"],
    }
    return {
        "all_passed": all(checks.values()),
        "checks": dict(sorted(checks.items())),
        "observed": {
            "canonical_exercises_with_20_records": sum(count >= 20 for count in canonical_counts.values()),
            "eligible_exercise_outcomes": len(primary),
            "eligible_workout_blocks": len(blocks),
            "evaluable_coverage": _safe_ratio(len(primary), total_targets),
            "potential_rolling_folds": potential_folds,
            "status_buckets_with_minimum_records": sum(count >= thresholds["minimum_status_records_for_inferential_comparison"] for count in status_counts.values()),
            "target_attainment_successes": successes,
            "underperformance_events": failures,
        },
        "required": thresholds,
    }


def _mean(values: list[float]) -> float | None:
    return sum(values) / len(values) if values else None


def _group_outcomes(rows: list[dict[str, Any]], key_name: str) -> list[dict[str, Any]]:
    grouped: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for row in rows:
        values = row.get(key_name)
        if isinstance(values, list):
            keys = values or ["UNAVAILABLE"]
        else:
            keys = [str(values)]
        for key in keys:
            grouped[str(key)].append(row)
    return [
        {
            key_name: key,
            "count": len(grouped[key]),
            "attainment_rate": _mean([float(row["attained"]) for row in grouped[key]]),
        }
        for key in sorted(grouped)
    ]


def descriptive_secondary_outcomes(primary: list[dict[str, Any]]) -> dict[str, Any]:
    load_deltas = [float(row["achieved_load_kg"]) - float(row["suggested_load_kg"]) for row in primary]
    rep_deltas = [float(row["achieved_reps"]) - float(row["suggested_min_reps"]) for row in primary]
    performance_deltas = [
        float(row["achieved_load_kg"]) * float(row["achieved_reps"]) - float(row["reference_performance_value"])
        for row in primary if row.get("reference_performance_value") is not None
    ]
    rep_range = Counter(
        "BELOW" if int(row["achieved_reps"]) < int(row["suggested_min_reps"])
        else "ABOVE" if int(row["achieved_reps"]) > int(row["suggested_max_reps"])
        else "WITHIN"
        for row in primary
    )
    target_set_completion = [
        min(1.0, float(row["completed_valid_sets"]) / max(1.0, float(row["target_sets_snapshot"])))
        for row in primary
    ]
    status_rows = [dict(row, readiness_status=readiness_status(float(row["primary_readiness_min"]))) for row in primary]
    return {
        "actual_minus_suggested_load_kg": {"count": len(load_deltas), "mean": _mean(load_deltas)},
        "actual_minus_suggested_reps": {"count": len(rep_deltas), "mean": _mean(rep_deltas)},
        "completed_vs_partial": _group_outcomes(primary, "outcome_completed"),
        "exercise_completion_rate": _mean([1.0 if row["exercise_completed"] else 0.0 for row in primary]),
        "outcome_by_canonical_exercise": _group_outcomes(primary, "canonical_exercise_key"),
        "outcome_by_first_set_confidence": _group_outcomes(primary, "first_set_confidence"),
        "outcome_by_first_set_rule": _group_outcomes(primary, "first_set_rule"),
        "outcome_by_primary_muscle_code": _group_outcomes(primary, "primary_muscle_codes"),
        "outcome_by_readiness_status": _group_outcomes(status_rows, "readiness_status"),
        "outcome_by_tracking_type": _group_outcomes(primary, "tracking_type"),
        "performance_change_vs_frozen_reference": {"count": len(performance_deltas), "mean": _mean(performance_deltas)},
        "rep_range_counts": dict(sorted(rep_range.items())),
        "target_set_completion_rate_mean": _mean(target_set_completion),
        "time_since_frozen_reference_hours": {
            "count": sum(row.get("time_since_reference_hours") is not None for row in primary),
            "mean": _mean([float(row["time_since_reference_hours"]) for row in primary if row.get("time_since_reference_hours") is not None]),
        },
    }


def stable_loading_subset(primary: list[dict[str, Any]], tolerance: float = 0.01) -> list[dict[str, Any]]:
    history: dict[str, list[tuple[int, float]]] = defaultdict(list)
    result: list[dict[str, Any]] = []
    by_workout: dict[str, list[dict[str, Any]]] = defaultdict(list)
    order: list[str] = []
    for row in primary:
        workout = str(row["workout_attempt_id"])
        if workout not in by_workout:
            order.append(workout)
        by_workout[workout].append(row)
    for workout in order:
        block = by_workout[workout]
        for row in block:
            cutoff = int(row["input_cutoff_at_millis"])
            prior = [load for captured, load in history[str(row["canonical_exercise_key"])] if captured <= cutoff][-3:]
            suggested = float(row["suggested_load_kg"])
            if len(prior) == 3 and max(prior) - min(prior) <= tolerance and all(abs(load - suggested) <= tolerance for load in prior):
                result.append(row)
        for row in block:
            history[str(row["canonical_exercise_key"])].append(
                (int(row["outcome_captured_at_millis"]), float(row["achieved_load_kg"]))
            )
    return result


def conclusion_from(metrics: dict[str, Any] | None, gate_result: dict[str, Any], context: AuditContext, protocol: dict[str, Any]) -> str:
    if context.critical:
        return "DATA_INTEGRITY_BLOCKED"
    if not gate_result["all_passed"]:
        return "INSUFFICIENT_DATA"
    if metrics is None:
        return "INSUFFICIENT_DATA"
    support = protocol["conclusion_logic"]["baseline_supported"]
    incremental = metrics["incremental_brier"]
    readiness_auc = metrics["readiness_underperformance_auroc"]
    exercise_rows = [row for row in metrics["exercise_incremental"] if row["count"] >= 20]
    total = sum(row["count"] for row in metrics["exercise_incremental"])
    largest_share = max((row["count"] for row in metrics["exercise_incremental"]), default=0) / total if total else 1.0
    support_passed = all([
        incremental["point"] is not None and incremental["point"] >= support["combined_minus_po_brier_improvement_min"],
        incremental["lower"] is not None and incremental["lower"] > support["combined_minus_po_brier_improvement_ci_lower_gt"],
        metrics["fold_improvement_proportion"] is not None and metrics["fold_improvement_proportion"] >= support["fold_improvement_proportion_min"],
        readiness_auc["point"] is not None and readiness_auc["point"] >= support["readiness_underperformance_auroc_min"],
        readiness_auc["lower"] is not None and readiness_auc["lower"] > support["readiness_underperformance_auroc_ci_lower_gt"],
        metrics["status_ordering_passed"],
        largest_share <= support["largest_canonical_exercise_share_max"],
        len(exercise_rows) >= support["minimum_canonical_exercises_with_20_records"],
        sum(row["brier_improvement"] >= 0 for row in exercise_rows) >= support["minimum_exercises_with_nonnegative_incremental_brier"],
    ])
    if support_passed:
        return "BASELINE_SUPPORTED"
    promising = protocol["conclusion_logic"]["promising_not_validated"]
    if (
        incremental["point"] is not None
        and incremental["point"] >= promising["combined_minus_po_brier_improvement_min"]
        and readiness_auc["point"] is not None
        and readiness_auc["point"] >= promising["readiness_underperformance_auroc_min"]
    ):
        return "PROMISING_NOT_VALIDATED"
    return "NO_INCREMENTAL_SIGNAL"


def evaluate(data: dict[str, Any], input_sha256: str, protocol: LoadedProtocol, mode: str, seed: int) -> dict[str, Any]:
    if mode not in {"AUDIT_ONLY", "FULL_EVALUATION"}:
        raise ValueError("Mode must be AUDIT_ONLY or FULL_EVALUATION")
    context, distributions = validate_export(data, protocol.data)
    ledger = build_eligibility_ledger(data, context, protocol.data)
    state_counts = _sorted_counts([row["final_state"] for row in ledger])
    reason_counts = _sorted_counts([row["reason_code"] for row in ledger])
    attempt_counts = _sorted_counts([row["attempt_classification"] for row in ledger])
    primary = [row for row in ledger if row["final_state"] == "ELIGIBLE_PRIMARY"]
    gates = sample_gates(ledger, protocol.data)
    prediction_rows: list[dict[str, Any]] = []
    fold_manifest: list[dict[str, Any]] = []
    metrics: dict[str, Any] | None = None
    sensitivities: dict[str, Any] = {
        "modal_load_heuristic_removal": {
            "status": "UNAVAILABLE",
            "reason": "WEARABLE-1A exports no alternate supported loaded-set rule; removing the V1 modal rule removes every strict primary row.",
        },
        "primary_readiness_mean": {"status": "NOT_RUN"},
        "repeated_stable_loading_pattern": {"status": "NOT_RUN"},
        "secondary_readiness_min": {
            "available_count": sum(row.get("secondary_readiness_min") is not None for row in primary),
            "status": "DESCRIPTIVE_ONLY",
        },
        "attempted_plus_ambiguous": {
            "ambiguous_count": attempt_counts.get("TARGET_ATTEMPT_AMBIGUOUS", 0),
            "attempted_count": attempt_counts.get("TARGET_ATTEMPTED", 0),
            "status": "DESCRIPTIVE_ONLY",
        },
    }
    if mode == "FULL_EVALUATION" and not context.critical and gates["all_passed"]:
        prediction_rows, fold_manifest = rolling_origin(primary, protocol.data)
        if len(fold_manifest) >= protocol.data["sample_size_gates"]["minimum_rolling_folds"]:
            metrics = calculate_metrics(prediction_rows, protocol.data, seed)
            if all(row.get("primary_readiness_mean") is not None for row in primary):
                mean_rows, mean_folds = rolling_origin(primary, protocol.data, "primary_readiness_mean")
                sensitivities["primary_readiness_mean"] = {
                    "fold_count": len(mean_folds),
                    "metrics": calculate_metrics(mean_rows, protocol.data, seed + 10),
                    "status": "RUN",
                }
            stable = stable_loading_subset(primary, float(protocol.data["target_attempt"]["load_tolerance_kg"]))
            stable_gates = sample_gates(stable, protocol.data)
            if stable_gates["all_passed"]:
                stable_rows, stable_folds = rolling_origin(stable, protocol.data)
                sensitivities["repeated_stable_loading_pattern"] = {
                    "eligible_records": len(stable),
                    "fold_count": len(stable_folds),
                    "metrics": calculate_metrics(stable_rows, protocol.data, seed + 20),
                    "sample_gates": stable_gates,
                    "status": "RUN",
                }
            else:
                sensitivities["repeated_stable_loading_pattern"] = {
                    "eligible_records": len(stable),
                    "sample_gates": stable_gates,
                    "status": "INSUFFICIENT_DATA",
                }
    conclusion = conclusion_from(metrics, gates, context, protocol.data)
    audit = {
        "active_outcomes": sum(row.get("statusCode") == "ACTIVE" for row in data["outcomes"]),
        "attempt_classification_counts": attempt_counts,
        "capture_denominator_known": False,
        "consent_records": len(data["consent"]),
        "consent_state_distribution": _sorted_counts([row.get("stateCode") for row in data["consent"]]),
        "critical_integrity_findings": context.critical,
        "data_quality_verdict": "BLOCKED" if context.critical else "ACCEPTABLE_WITH_DECLARED_LIMITATIONS",
        "eligibility_state_counts": state_counts,
        "exercise_outcomes": len(data["exerciseOutcomes"]),
        "first_set_confidence_distribution": _sorted_counts([
            row.get("firstWorkingSetConfidenceCode") for row in data["exerciseOutcomes"]
        ]),
        "linked_outcomes": len(data["outcomes"]),
        "model_version_distribution": distributions,
        "outcome_status_distribution": _sorted_counts([row.get("statusCode") for row in data["outcomes"]]),
        "predictions": len(data["predictions"]),
        "raw_history_digest_reconstructable": False,
        "readiness_status_distribution": _sorted_counts([
            readiness_status(float(row["primary_readiness_min"])) for row in primary
        ]),
        "reason_counts": reason_counts,
        "set_observations": len(data["setObservations"]),
        "target_exercises": len(data["targetExercises"]),
        "tracking_type_distribution": _sorted_counts([row.get("trackingTypeCode") for row in data["targetExercises"]]),
        "warnings": context.warnings + [
            {"code": "CAPTURE_DENOMINATOR_UNKNOWN", "detail": "Export cannot enumerate workouts missed outside consent/capture."},
            {"code": "RAW_HISTORY_DIGEST_NOT_RECONSTRUCTABLE", "detail": "Export contains the frozen digest but not raw pre-cutoff history."},
        ],
    }
    report = {
        "audit": audit,
        "comparator_feature_sets": {key: value for key, value in sorted(RAW_FEATURES.items())},
        "conclusion": conclusion,
        "descriptive_secondary_outcomes": descriptive_secondary_outcomes(primary),
        "evaluator_version": EVALUATOR_VERSION,
        "fold_summary": {
            "all_strictly_ordered": all(row["strictly_ordered"] for row in fold_manifest),
            "fold_count": len(fold_manifest),
            "all_training_outcomes_available_before_test": all(
                row["train_outcomes_available_before_test"] for row in fold_manifest
            ),
            "workouts_split": any(row["workout_split"] for row in fold_manifest),
        },
        "input_sha256": input_sha256,
        "input_versions": {
            "contract_version": data["contractVersion"],
            "export_schema_version": data["exportSchemaVersion"],
            "room_schema_version": data["roomSchemaVersion"],
        },
        "metrics": metrics,
        "mode": mode,
        "protocol_hash": protocol.sha256,
        "protocol_version": protocol.data["protocol_version"],
        "random_seed": seed,
        "sample_gates": gates,
        "sensitivity_analyses": sensitivities,
        "wearable_modifier_active": False,
    }
    return {
        "evaluation_report": report,
        "eligibility_ledger": ledger,
        "fold_manifest": fold_manifest,
        "metrics": metrics or {"status": "NOT_RUN", "reason": conclusion},
        "prediction_rows": prediction_rows,
    }


def markdown_report(report: dict[str, Any]) -> str:
    audit = report["audit"]
    gates = report["sample_gates"]
    versions = report["input_versions"]
    lines = [
        "# WEARABLE-1B evaluation report",
        "",
        f"- Conclusion: `{report['conclusion']}`",
        f"- Mode: `{report['mode']}`",
        f"- Protocol: `{report['protocol_version']}`",
        f"- Protocol SHA-256: `{report['protocol_hash']}`",
        f"- Input SHA-256: `{report['input_sha256']}`",
        f"- Evaluator: `{report['evaluator_version']}`",
        f"- Input contract: `{versions['contract_version']}`",
        f"- Export schema: `{versions['export_schema_version']}`",
        f"- Room schema: `{versions['room_schema_version']}`",
        f"- Seed: `{report['random_seed']}`",
        f"- Wearable modifier active: `{str(report['wearable_modifier_active']).lower()}`",
        "",
        "## Audit",
        "",
        f"- Predictions: {audit['predictions']}",
        f"- Target exercises: {audit['target_exercises']}",
        f"- Consent records: {audit['consent_records']}",
        f"- Set observations: {audit['set_observations']}",
        f"- Linked outcomes: {audit['linked_outcomes']}",
        f"- ACTIVE outcomes: {audit['active_outcomes']}",
        f"- Consent states: `{json.dumps(audit['consent_state_distribution'], sort_keys=True)}`",
        f"- Outcome states: `{json.dumps(audit['outcome_status_distribution'], sort_keys=True)}`",
        f"- First-set confidence: `{json.dumps(audit['first_set_confidence_distribution'], sort_keys=True)}`",
        f"- Data-quality verdict: `{audit['data_quality_verdict']}`",
        f"- Capture denominator known: `{str(audit['capture_denominator_known']).lower()}`",
        f"- Raw history digest reconstructable: `{str(audit['raw_history_digest_reconstructable']).lower()}`",
        "",
        "## Eligibility",
        "",
    ]
    for key, value in audit["eligibility_state_counts"].items():
        lines.append(f"- {key}: {value}")
    lines.extend(["", "## Sample gates", ""])
    for key, passed in gates["checks"].items():
        lines.append(f"- {key}: `{'PASS' if passed else 'FAIL'}`")
    lines.extend(["", "## Rolling origin", ""])
    lines.append(f"- Fold count: {report['fold_summary']['fold_count']}")
    lines.append(f"- All train cutoffs precede test cutoffs: `{str(report['fold_summary']['all_strictly_ordered']).lower()}`")
    lines.append(f"- All training outcomes existed before each test cutoff: `{str(report['fold_summary']['all_training_outcomes_available_before_test']).lower()}`")
    lines.append(f"- Workouts split: `{str(report['fold_summary']['workouts_split']).lower()}`")
    if report["metrics"]:
        incremental = report["metrics"]["incremental_brier"]
        readiness_auc = report["metrics"]["readiness_underperformance_auroc"]
        lines.extend([
            "",
            "## Full-evaluation metrics",
            "",
            f"- Incremental Brier improvement: {incremental['point']:.6f}",
            f"- 95% workout-block bootstrap CI: [{incremental['lower']:.6f}, {incremental['upper']:.6f}]",
            f"- Readiness underperformance AUROC: {readiness_auc['point']:.6f}",
            f"- Fold improvement proportion: {report['metrics']['fold_improvement_proportion']:.6f}",
        ])
    lines.extend([
        "",
        "## Interpretation boundary",
        "",
        "Readiness is evaluated as an ordinal/model feature, never as `readiness / 100` probability. "
        "The export can validate frozen metadata and payload lineage, but cannot independently reconstruct "
        "the raw pre-cutoff history digest or the total uncaptured-workout denominator.",
        "",
    ])
    return "\n".join(lines)


def write_outputs(result: dict[str, Any], output_directory: str | Path) -> dict[str, str]:
    output = Path(output_directory)
    output.mkdir(parents=True, exist_ok=True)
    files = {
        "evaluation_report.json": result["evaluation_report"],
        "eligibility_ledger.json": result["eligibility_ledger"],
        "fold_manifest.json": result["fold_manifest"],
        "metrics.json": result["metrics"],
    }
    hashes: dict[str, str] = {}
    for name, value in files.items():
        raw = canonical_json(value)
        (output / name).write_bytes(raw)
        hashes[name] = hashlib.sha256(raw).hexdigest()
    markdown = markdown_report(result["evaluation_report"]).encode("utf-8")
    (output / "evaluation_report.md").write_bytes(markdown)
    hashes["evaluation_report.md"] = hashlib.sha256(markdown).hexdigest()
    return dict(sorted(hashes.items()))

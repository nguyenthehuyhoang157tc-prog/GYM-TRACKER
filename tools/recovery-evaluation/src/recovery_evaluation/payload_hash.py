from __future__ import annotations

import hashlib
import math
from decimal import Decimal
from typing import Any, Iterable


PREDICTION_FIELDS = [
    "predictionId", "targetWorkoutAttemptId", "subjectId", "sourceCode",
    "workoutCreatedAtMillis", "workoutStartedAtMillis", "predictionGeneratedAtMillis",
    "inputDataCutoffAtMillis", "frozenAtMillis", "recoveryModelVersion",
    "muscleHeatModelVersion", "progressiveOverloadModelVersion", "researchContractVersion",
    "researchSchemaVersion", "firstWorkingSetRuleVersion", "catalogContentVersion",
    "planDayIdSnapshot", "planDayNameSnapshot", "overallReadiness", "overallStatusCode",
    "inputSessionIdsJson", "inputSessionMaxTimestampMillis", "inputDigestSha256",
    "dataCompletenessCode", "abstained", "abstentionReasonCode", "payloadSha256",
]

GROUP_FIELDS = ["predictionId", "groupCode", "readiness", "statusCode", "trained"]

MUSCLE_FIELDS = [
    "predictionId", "muscleCode", "readiness", "fatigueScore", "acuteResidual",
    "structuralResidual", "statusCode", "colorSemanticCode", "soreWindow",
    "grindCarried", "lastTrainedAtMillis",
]

TARGET_FIELDS = [
    "targetExerciseId", "predictionId", "targetOrdinal", "planExerciseIdSnapshot",
    "exerciseNameSnapshot", "canonicalExerciseKey", "trackingTypeCode", "targetSetsSnapshot",
    "targetMinRepsSnapshot", "targetMaxRepsSnapshot", "primaryMuscleCodesJson",
    "secondaryMuscleCodesJson", "primaryReadinessMin", "primaryReadinessMean",
    "secondaryReadinessMin", "suggestedLoadKg", "suggestedMinReps", "suggestedMaxReps",
    "suggestedRir", "suggestionActionCode", "progressionTypeCode", "suggestionStrategyCode",
    "suggestionTargetText", "suggestionReasonText", "suggestionTitleText",
    "suggestionDetailText", "suggestionConfidence", "sessionQualityCode", "coachInsightCode",
    "referenceSessionId", "referenceSessionStartedAtMillis", "referenceSetNumber",
    "referenceLoadKg", "referenceReps", "referenceRir", "referenceDurationSeconds",
    "referenceDistanceMeters", "referencePerformanceValue", "dataCompletenessCode",
    "abstained", "abstentionReasonCode",
]


def _expand_scientific(value: str) -> str:
    mantissa, exponent_text = value.lower().split("e")
    exponent = int(exponent_text)
    sign = ""
    if mantissa.startswith("-"):
        sign, mantissa = "-", mantissa[1:]
    left, dot, right = mantissa.partition(".")
    digits = left + right
    decimal_index = len(left) + exponent
    if decimal_index <= 0:
        return sign + "0." + ("0" * -decimal_index) + digits
    if decimal_index >= len(digits):
        return sign + digits + ("0" * (decimal_index - len(digits))) + ".0"
    return sign + digits[:decimal_index] + "." + digits[decimal_index:]


def _to_scientific(value: str) -> str:
    sign = ""
    if value.startswith("-"):
        sign, value = "-", value[1:]
    left, dot, right = value.partition(".")
    if left != "0":
        digits = (left + right).rstrip("0")
        exponent = len(left) - 1
    else:
        leading = len(right) - len(right.lstrip("0"))
        digits = right[leading:].rstrip("0")
        exponent = -(leading + 1)
    if not digits:
        return sign + "0.0E0"
    tail = digits[1:] or "0"
    return f"{sign}{digits[0]}.{tail}E{exponent}"


def java_double_to_string(value: float) -> str:
    """Reproduce Java/Kotlin Double.toString notation for finite JSON doubles.

    CPython and modern JVMs both emit the shortest round-trippable decimal; this function
    normalizes the different fixed/scientific notation thresholds required by the legacy
    WEARABLE-1A data-class payload hash.
    """
    if not math.isfinite(value):
        raise ValueError("Non-finite payload number")
    if value == 0.0:
        return "-0.0" if math.copysign(1.0, value) < 0 else "0.0"
    rendered = repr(value)
    absolute = abs(value)
    java_scientific = absolute < 1e-3 or absolute >= 1e7
    python_scientific = "e" in rendered.lower()
    if java_scientific:
        if python_scientific:
            fixed = _expand_scientific(rendered)
            return _to_scientific(fixed)
        return _to_scientific(rendered)
    if python_scientific:
        return _expand_scientific(rendered)
    if "." not in rendered:
        return rendered + ".0"
    return rendered


def kotlin_value(value: Any) -> str:
    if value is None:
        return "null"
    if isinstance(value, bool):
        return "true" if value else "false"
    if isinstance(value, int):
        return str(value)
    if isinstance(value, float):
        return java_double_to_string(value)
    if isinstance(value, str):
        return value
    raise ValueError(f"Unsupported Kotlin payload value: {type(value).__name__}")


def data_class_string(class_name: str, row: dict[str, Any], fields: Iterable[str]) -> str:
    missing = [field for field in fields if field not in row]
    if missing:
        raise ValueError(f"Missing payload fields for {class_name}: {', '.join(missing)}")
    body = ", ".join(f"{field}={kotlin_value(row[field])}" for field in fields)
    return f"{class_name}({body})"


def frozen_payload_sha256(
    prediction: dict[str, Any],
    groups: list[dict[str, Any]],
    muscles: list[dict[str, Any]],
    targets: list[dict[str, Any]],
) -> str:
    prediction_for_hash = dict(prediction)
    prediction_for_hash["payloadSha256"] = ""
    lines = [data_class_string("RecoveryResearchPredictionEntity", prediction_for_hash, PREDICTION_FIELDS)]
    lines.extend(
        data_class_string("RecoveryResearchPredictionGroupEntity", row, GROUP_FIELDS)
        for row in sorted(groups, key=lambda row: row["groupCode"])
    )
    lines.extend(
        data_class_string("RecoveryResearchPredictionMuscleEntity", row, MUSCLE_FIELDS)
        for row in sorted(muscles, key=lambda row: row["muscleCode"])
    )
    lines.extend(
        data_class_string("RecoveryResearchTargetExerciseEntity", row, TARGET_FIELDS)
        for row in sorted(targets, key=lambda row: row["targetOrdinal"])
    )
    payload = "\n".join(lines) + "\n"
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()


from __future__ import annotations

import copy
import hashlib
import json
import sys
import tempfile
import unittest
from pathlib import Path


TOOL_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(TOOL_ROOT / "src"))
sys.path.insert(0, str(TOOL_ROOT / "fixtures"))

from generate_fixtures import make_export  # noqa: E402
from recovery_evaluation.evaluator import (  # noqa: E402
    AuditContext,
    RAW_FEATURES,
    ExportError,
    build_eligibility_ledger,
    build_folds,
    canonical_json,
    conclusion_from,
    evaluate,
    load_export,
    raw_features,
    rolling_origin,
    sample_gates,
    validate_export,
    write_outputs,
)
from recovery_evaluation.payload_hash import frozen_payload_sha256, java_double_to_string  # noqa: E402
from recovery_evaluation.protocol import LoadedProtocol, ProtocolError, load_protocol  # noqa: E402
from recovery_evaluation.statistics import (  # noqa: E402
    FoldPreprocessor,
    auroc,
    brier_score,
    calibration_summary,
    moving_block_bootstrap,
    pr_auc,
)


PROTOCOL_PATH = TOOL_ROOT / "protocol" / "WEARABLE_1B_EVALUATION_PROTOCOL_V1.json"


def protocol() -> LoadedProtocol:
    return load_protocol(PROTOCOL_PATH)


def rehash(data: dict) -> None:
    for prediction in data["predictions"]:
        prediction_id = prediction["predictionId"]
        prediction["payloadSha256"] = frozen_payload_sha256(
            prediction,
            [row for row in data["predictionGroups"] if row["predictionId"] == prediction_id],
            [row for row in data["predictionMuscles"] if row["predictionId"] == prediction_id],
            [row for row in data["targetExercises"] if row["predictionId"] == prediction_id],
        )


def audit_and_ledger(data: dict):
    loaded = protocol()
    context, _ = validate_export(data, loaded.data)
    return context, build_eligibility_ledger(data, context, loaded.data)


class ProtocolTests(unittest.TestCase):
    def test_protocol_parses_and_hash_is_frozen(self):
        loaded = protocol()
        self.assertEqual(loaded.sha256, "494ac18b8a92114091464ab1731776def680af40fa26e2cb27a4742748ea6998")
        self.assertEqual(loaded.data["protocol_version"], "WEARABLE_1B_EVALUATION_PROTOCOL_V1")

    def test_protocol_version_rejection(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "bad.json"
            data = copy.deepcopy(protocol().data)
            data["protocol_version"] = "V2"
            raw = canonical_json(data)
            path.write_bytes(raw)
            path.with_suffix(".sha256").write_text(hashlib.sha256(raw).hexdigest() + "  bad.json\n", encoding="utf-8")
            with self.assertRaises(ProtocolError):
                load_protocol(path)

    def test_protocol_hash_rejection(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "bad.json"
            path.write_bytes(PROTOCOL_PATH.read_bytes())
            path.with_suffix(".sha256").write_text("0" * 64 + "  bad.json\n", encoding="utf-8")
            with self.assertRaisesRegex(ProtocolError, "PROTOCOL_HASH_INVALID"):
                load_protocol(path)

    def test_readiness_is_not_declared_probability(self):
        self.assertFalse(protocol().data["calibration"]["readiness_divided_by_100_is_probability"])


class PayloadCodecTests(unittest.TestCase):
    def test_java_double_notation_thresholds(self):
        self.assertEqual(java_double_to_string(0.001), "0.001")
        self.assertEqual(java_double_to_string(0.0001), "1.0E-4")
        self.assertEqual(java_double_to_string(10_000_000.0), "1.0E7")
        self.assertEqual(java_double_to_string(1_000_000.0), "1000000.0")

    def test_valid_fixture_payload_hashes(self):
        data = make_export(2, "positive")
        context, _ = validate_export(data, protocol().data)
        self.assertFalse([item for item in context.critical if item["code"] == "PAYLOAD_HASH_INVALID"])


class InputValidationTests(unittest.TestCase):
    def test_valid_export(self):
        context, _ = validate_export(make_export(2, "positive"), protocol().data)
        self.assertFalse(context.critical)

    def test_malformed_json(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "bad.json"
            path.write_text("{bad", encoding="utf-8")
            with self.assertRaises(ExportError):
                load_export(path)

    def test_duplicate_json_key(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "bad.json"
            path.write_text('{"consent":[],"consent":[]}', encoding="utf-8")
            with self.assertRaises(ExportError):
                load_export(path)

    def test_unsupported_schema(self):
        data = make_export(1, "positive")
        data["exportSchemaVersion"] = 99
        context, _ = validate_export(data, protocol().data)
        self.assertIn("EXPORT_SCHEMA_UNSUPPORTED", {item["code"] for item in context.critical})

    def test_duplicate_prediction_ids(self):
        data = make_export(2, "positive")
        data["predictions"][1]["predictionId"] = data["predictions"][0]["predictionId"]
        context, _ = validate_export(data, protocol().data)
        self.assertIn("DUPLICATE_PREDICTION_ID", {item["code"] for item in context.critical})

    def test_broken_foreign_link(self):
        data = make_export(1, "positive")
        data["exerciseOutcomes"][0]["targetExerciseId"] = "missing"
        context, _ = validate_export(data, protocol().data)
        self.assertIn("BROKEN_INTERNAL_LINK", {item["code"] for item in context.critical})

    def test_invalid_payload_hash(self):
        data = make_export(1, "positive")
        data["predictions"][0]["overallReadiness"] += 1
        context, _ = validate_export(data, protocol().data)
        self.assertIn("PAYLOAD_HASH_INVALID", {item["code"] for item in context.critical})

    def test_future_timestamp_leakage(self):
        data = make_export(1, "positive")
        data["targetExercises"][0]["referenceSessionStartedAtMillis"] = data["predictions"][0]["inputDataCutoffAtMillis"] + 1
        rehash(data)
        context, _ = validate_export(data, protocol().data)
        self.assertIn("TIMESTAMP_INCONSISTENT", {item["code"] for item in context.critical})

    def test_invalid_cutoff_order(self):
        data = make_export(1, "positive")
        data["predictions"][0]["inputDataCutoffAtMillis"] -= 1_000_000
        rehash(data)
        context, _ = validate_export(data, protocol().data)
        self.assertIn("TIMESTAMP_INCONSISTENT", {item["code"] for item in context.critical})

    def test_mixed_model_versions(self):
        data = make_export(2, "positive")
        data["predictions"][1]["recoveryModelVersion"] = "OTHER"
        rehash(data)
        context, _ = validate_export(data, protocol().data)
        self.assertIn("MODEL_VERSION_MISMATCH", {item["code"] for item in context.critical})
        ledger = build_eligibility_ledger(data, context, protocol().data)
        affected = [row for row in ledger if row["prediction_id"] == data["predictions"][1]["predictionId"]]
        self.assertTrue(affected)
        self.assertTrue(all(row["reason_code"] == "MODEL_VERSION_MISMATCH" for row in affected))


class EligibilityTests(unittest.TestCase):
    def test_completed_target_is_eligible(self):
        _, ledger = audit_and_ledger(make_export(1, "positive", 1))
        self.assertEqual(ledger[0]["final_state"], "ELIGIBLE_PRIMARY")

    def test_partial_with_evaluable_exercise_is_eligible(self):
        data = make_export(1, "positive", 1)
        data["outcomes"][0]["statusCode"] = "PARTIAL"
        _, ledger = audit_and_ledger(data)
        self.assertEqual(ledger[0]["final_state"], "ELIGIBLE_PRIMARY")

    def test_abandoned_is_excluded(self):
        data = make_export(1, "positive", 1)
        data["outcomes"][0].update(statusCode="ABANDONED", validForEvaluation=False)
        _, ledger = audit_and_ledger(data)
        self.assertEqual(ledger[0]["reason_code"], "ABANDONED_WITHOUT_EVALUABLE_EXERCISE")

    def test_active_is_unresolved(self):
        data = make_export(1, "positive", 1)
        data["outcomes"][0]["statusCode"] = "ACTIVE"
        _, ledger = audit_and_ledger(data)
        self.assertEqual(ledger[0]["final_state"], "UNRESOLVED")
        self.assertEqual(ledger[0]["reason_code"], "ACTIVE_OUTCOME")

    def test_no_po_suggestion_abstains(self):
        data = make_export(1, "positive", 1)
        data["targetExercises"][0]["suggestedLoadKg"] = None
        rehash(data)
        _, ledger = audit_and_ledger(data)
        self.assertEqual(ledger[0]["reason_code"], "NO_PO_SUGGESTION")

    def test_low_confidence_is_secondary_only(self):
        data = make_export(1, "positive", 1)
        data["exerciseOutcomes"][0]["firstWorkingSetConfidenceCode"] = "LOW"
        _, ledger = audit_and_ledger(data)
        self.assertEqual(ledger[0]["final_state"], "ELIGIBLE_SECONDARY_ONLY")

    def test_unsupported_tracking_is_secondary_only(self):
        data = make_export(1, "positive", 1)
        data["targetExercises"][0]["trackingTypeCode"] = "BODYWEIGHT_REPS"
        data["exerciseOutcomes"][0]["trackingTypeCode"] = "BODYWEIGHT_REPS"
        rehash(data)
        _, ledger = audit_and_ledger(data)
        self.assertEqual(ledger[0]["reason_code"], "TRACKING_TYPE_UNSUPPORTED")

    def test_missing_muscle_mapping_abstains(self):
        data = make_export(1, "positive", 1)
        data["targetExercises"][0]["primaryMuscleCodesJson"] = "[]"
        data["targetExercises"][0]["primaryReadinessMin"] = None
        rehash(data)
        _, ledger = audit_and_ledger(data)
        self.assertEqual(ledger[0]["reason_code"], "NO_PRIMARY_MUSCLE_MAPPING")

    def test_exact_load_is_attempted(self):
        _, ledger = audit_and_ledger(make_export(1, "positive", 1))
        self.assertEqual(ledger[0]["attempt_classification"], "TARGET_ATTEMPTED")

    def test_materially_different_load_is_not_attempted(self):
        data = make_export(1, "positive", 1)
        data["exerciseOutcomes"][0]["achievedLoadKg"] = 47.5
        _, ledger = audit_and_ledger(data)
        self.assertEqual(ledger[0]["attempt_classification"], "TARGET_NOT_ATTEMPTED")

    def test_missing_actual_is_ambiguous(self):
        data = make_export(1, "positive", 1)
        data["exerciseOutcomes"][0]["achievedReps"] = None
        _, ledger = audit_and_ledger(data)
        self.assertEqual(ledger[0]["attempt_classification"], "TARGET_ATTEMPT_AMBIGUOUS")

    def test_minimum_primary_readiness_is_primary_mapping(self):
        data = make_export(1, "positive", 1)
        data["targetExercises"][0]["primaryReadinessMin"] = 25
        data["targetExercises"][0]["primaryReadinessMean"] = 80.0
        rehash(data)
        _, ledger = audit_and_ledger(data)
        self.assertEqual(ledger[0]["primary_readiness_min"], 25)
        self.assertEqual(ledger[0]["primary_readiness_mean"], 80.0)


class RollingOriginTests(unittest.TestCase):
    def test_expanding_folds_are_strict_and_workouts_not_split(self):
        data = make_export(35, "positive")
        _, ledger = audit_and_ledger(data)
        primary = [row for row in ledger if row["final_state"] == "ELIGIBLE_PRIMARY"]
        _, manifest = rolling_origin(primary, protocol().data)
        self.assertEqual(len(manifest), 5)
        self.assertTrue(all(row["strictly_ordered"] for row in manifest))
        self.assertTrue(all(row["train_outcomes_available_before_test"] for row in manifest))
        self.assertTrue(all(not row["workout_split"] for row in manifest))

    def test_unavailable_prior_outcome_never_enters_training(self):
        data = make_export(35, "positive", 1)
        delayed = data["exerciseOutcomes"][0]
        delayed["outcomeCapturedAtMillis"] = data["predictions"][-1]["inputDataCutoffAtMillis"] + 1
        _, ledger = audit_and_ledger(data)
        primary = [row for row in ledger if row["final_state"] == "ELIGIBLE_PRIMARY"]
        _, manifest = rolling_origin(primary, protocol().data)
        self.assertTrue(all(row["train_outcomes_available_before_test"] for row in manifest))

    def test_first_fold_uses_minimum_history_only(self):
        data = make_export(31, "positive", 1)
        _, ledger = audit_and_ledger(data)
        primary = [row for row in ledger if row["final_state"] == "ELIGIBLE_PRIMARY"]
        _, manifest = rolling_origin(primary, protocol().data)
        self.assertEqual(manifest[0]["train_workout_count"], 30)
        self.assertEqual(len(manifest[0]["test_workout_ids"]), 1)

    def test_fold_preprocessor_uses_training_mean(self):
        processor = FoldPreprocessor().fit([[1.0], [3.0]])
        self.assertEqual(processor.means, [2.0])
        self.assertGreater(processor.transform([[100.0]])[0][0], 90.0)

    def test_fold_manifest_is_deterministic(self):
        data = make_export(32, "positive")
        _, ledger = audit_and_ledger(data)
        primary = [row for row in ledger if row["final_state"] == "ELIGIBLE_PRIMARY"]
        self.assertEqual(rolling_origin(primary, protocol().data)[1], rolling_origin(primary, protocol().data)[1])


class ComparatorTests(unittest.TestCase):
    def test_combined_differs_from_po_only_by_readiness(self):
        po = RAW_FEATURES["PO_HISTORY_ONLY_V1"]
        combined = RAW_FEATURES["PO_PLUS_MUSCLE_4B_V1"]
        self.assertEqual(combined[:-1], po)
        self.assertEqual(combined[-1], "primary_readiness_min")

    def test_po_history_excludes_readiness(self):
        self.assertNotIn("primary_readiness_min", RAW_FEATURES["PO_HISTORY_ONLY_V1"])

    def test_muscle_only_excludes_po_history(self):
        self.assertEqual(RAW_FEATURES["MUSCLE_4B_ONLY_V1"], ["primary_readiness_min"])

    def test_time_since_uses_frozen_reference(self):
        data = make_export(1, "positive", 1)
        _, ledger = audit_and_ledger(data)
        features = raw_features(ledger[0] | {"last_outcome": None}, "TIME_SINCE_TRAINED_V1")
        self.assertAlmostEqual(features[0], __import__("math").log1p(48.0))


class MetricTests(unittest.TestCase):
    def test_auroc_known_fixture(self):
        self.assertEqual(auroc([0, 0, 1, 1], [0.1, 0.2, 0.8, 0.9]), 1.0)

    def test_pr_auc_known_fixture(self):
        self.assertEqual(pr_auc([0, 0, 1, 1], [0.1, 0.2, 0.8, 0.9]), 1.0)

    def test_calibration_requires_both_classes(self):
        self.assertEqual(calibration_summary([1, 1], [0.5, 0.6])["reason"], "INSUFFICIENT_CLASSES")

    def test_bootstrap_is_clustered_and_deterministic(self):
        rows = [
            {"workout_id": f"w{i // 2}", "value": float(i)} for i in range(20)
        ]
        metric = lambda sample: sum(float(row["value"]) for row in sample) / len(sample)
        first = moving_block_bootstrap(rows, metric, 7, 100, 2)
        second = moving_block_bootstrap(rows, metric, 7, 100, 2)
        self.assertEqual(first, second)

    def test_brier_known_fixture(self):
        self.assertAlmostEqual(brier_score([0, 1], [0.0, 1.0]), 0.0)


class ConclusionTests(unittest.TestCase):
    def setUp(self):
        self.loaded = protocol()
        self.passing = {"all_passed": True}
        self.context = AuditContext([], [], set())
        self.metrics = {
            "incremental_brier": {"point": 0.02, "lower": 0.005, "upper": 0.03},
            "readiness_underperformance_auroc": {"point": 0.7, "lower": 0.6, "upper": 0.8},
            "fold_improvement_proportion": 0.7,
            "status_ordering_passed": True,
            "exercise_incremental": [
                {"count": 40, "brier_improvement": 0.01},
                {"count": 40, "brier_improvement": 0.01},
                {"count": 40, "brier_improvement": -0.001},
            ],
        }

    def test_data_integrity_blocked(self):
        context = AuditContext([{"code": "PAYLOAD_HASH_INVALID", "detail": "x"}], [], set())
        self.assertEqual(conclusion_from(None, self.passing, context, self.loaded.data), "DATA_INTEGRITY_BLOCKED")

    def test_insufficient_data(self):
        self.assertEqual(conclusion_from(None, {"all_passed": False}, self.context, self.loaded.data), "INSUFFICIENT_DATA")

    def test_baseline_supported(self):
        self.assertEqual(conclusion_from(self.metrics, self.passing, self.context, self.loaded.data), "BASELINE_SUPPORTED")

    def test_promising_not_validated(self):
        metrics = copy.deepcopy(self.metrics)
        metrics["incremental_brier"]["lower"] = -0.01
        self.assertEqual(conclusion_from(metrics, self.passing, self.context, self.loaded.data), "PROMISING_NOT_VALIDATED")

    def test_no_incremental_signal(self):
        metrics = copy.deepcopy(self.metrics)
        metrics["incremental_brier"]["point"] = 0.0
        self.assertEqual(conclusion_from(metrics, self.passing, self.context, self.loaded.data), "NO_INCREMENTAL_SIGNAL")


class AuditReportContractTests(unittest.TestCase):
    def test_report_includes_wearable_1c_required_aggregates(self):
        data = make_export(2, "positive")
        result = evaluate(data, "input-sha", protocol(), "AUDIT_ONLY", 41701)
        report = result["evaluation_report"]
        audit = report["audit"]

        self.assertEqual(
            report["input_versions"],
            {
                "contract_version": data["contractVersion"],
                "export_schema_version": data["exportSchemaVersion"],
                "room_schema_version": data["roomSchemaVersion"],
            },
        )
        self.assertEqual(audit["consent_records"], len(data["consent"]))
        self.assertEqual(audit["set_observations"], len(data["setObservations"]))
        self.assertEqual(audit["consent_state_distribution"], {"GRANTED": 1})
        self.assertEqual(audit["outcome_status_distribution"], {"COMPLETED": 2})
        self.assertEqual(
            audit["first_set_confidence_distribution"],
            {"MODERATE": len(data["exerciseOutcomes"])},
        )


class DeterminismTests(unittest.TestCase):
    def test_same_audit_run_is_byte_identical(self):
        data = make_export(2, "positive")
        loaded = protocol()
        first = evaluate(copy.deepcopy(data), "same", loaded, "AUDIT_ONLY", 41701)
        second = evaluate(copy.deepcopy(data), "same", loaded, "AUDIT_ONLY", 41701)
        self.assertEqual(canonical_json(first["evaluation_report"]), canonical_json(second["evaluation_report"]))
        self.assertEqual(canonical_json(first["eligibility_ledger"]), canonical_json(second["eligibility_ledger"]))

    def test_input_array_order_does_not_change_semantic_output(self):
        data = make_export(2, "positive")
        reordered = copy.deepcopy(data)
        for key in ["predictions", "predictionGroups", "predictionMuscles", "targetExercises", "setObservations", "outcomes", "exerciseOutcomes"]:
            reordered[key].reverse()
        loaded = protocol()
        first = evaluate(data, "canonical-semantic-hash", loaded, "AUDIT_ONLY", 41701)
        second = evaluate(reordered, "canonical-semantic-hash", loaded, "AUDIT_ONLY", 41701)
        self.assertEqual(canonical_json(first["evaluation_report"]), canonical_json(second["evaluation_report"]))
        self.assertEqual(canonical_json(first["eligibility_ledger"]), canonical_json(second["eligibility_ledger"]))

    def test_writer_produces_identical_bytes(self):
        result = evaluate(make_export(2, "positive"), "same", protocol(), "AUDIT_ONLY", 41701)
        with tempfile.TemporaryDirectory() as first_dir, tempfile.TemporaryDirectory() as second_dir:
            first = write_outputs(result, first_dir)
            second = write_outputs(result, second_dir)
            self.assertEqual(first, second)


if __name__ == "__main__":
    unittest.main()

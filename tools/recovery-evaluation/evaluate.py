#!/usr/bin/env python3
from __future__ import annotations

import argparse
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parent
sys.path.insert(0, str(ROOT / "src"))

from recovery_evaluation.evaluator import ExportError, evaluate, load_export, write_outputs  # noqa: E402
from recovery_evaluation.protocol import ProtocolError, load_protocol  # noqa: E402


def parser() -> argparse.ArgumentParser:
    result = argparse.ArgumentParser(description="Offline deterministic WEARABLE-1B evaluator")
    result.add_argument("--input", required=True, help="Canonical WEARABLE-1A JSON export")
    result.add_argument("--mode", choices=["audit", "full"], default="audit")
    result.add_argument(
        "--protocol",
        default=str(ROOT / "protocol" / "WEARABLE_1B_EVALUATION_PROTOCOL_V1.json"),
        help="Frozen machine-readable protocol",
    )
    result.add_argument("--output", required=True, help="Output directory")
    result.add_argument("--seed", type=int, default=None, help="Deterministic bootstrap seed override")
    return result


def main(argv: list[str] | None = None) -> int:
    arguments = parser().parse_args(argv)
    try:
        protocol = load_protocol(arguments.protocol)
        data, input_sha256 = load_export(arguments.input)
        seed = arguments.seed if arguments.seed is not None else int(protocol.data["bootstrap"]["default_seed"])
        result = evaluate(data, input_sha256, protocol, arguments.mode.upper() + "_EVALUATION" if arguments.mode == "full" else "AUDIT_ONLY", seed)
        hashes = write_outputs(result, arguments.output)
    except (OSError, ExportError, ProtocolError, ValueError, KeyError) as exc:
        print(f"WEARABLE-1B evaluator error: {exc}", file=sys.stderr)
        return 2
    print(f"Conclusion: {result['evaluation_report']['conclusion']}")
    print(f"Protocol SHA-256: {protocol.sha256}")
    print(f"Input SHA-256: {input_sha256}")
    print(f"Output: {Path(arguments.output).resolve()}")
    for name, digest in hashes.items():
        print(f"{name}: {digest}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())


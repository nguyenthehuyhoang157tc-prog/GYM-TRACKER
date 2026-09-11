from __future__ import annotations

import hashlib
import json
from dataclasses import dataclass
from pathlib import Path
from typing import Any


class ProtocolError(ValueError):
    pass


@dataclass(frozen=True)
class LoadedProtocol:
    data: dict[str, Any]
    path: Path
    sha256: str


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def load_protocol(path: str | Path) -> LoadedProtocol:
    protocol_path = Path(path).resolve()
    raw = protocol_path.read_bytes()
    digest = sha256_bytes(raw)
    sidecar = protocol_path.with_suffix(".sha256")
    if not sidecar.exists():
        raise ProtocolError(f"Protocol hash sidecar is missing: {sidecar}")
    sidecar_parts = sidecar.read_text(encoding="utf-8").strip().split()
    if not sidecar_parts or sidecar_parts[0].lower() != digest:
        raise ProtocolError("PROTOCOL_HASH_INVALID")
    try:
        data = json.loads(raw)
    except json.JSONDecodeError as exc:
        raise ProtocolError(f"Malformed protocol JSON: {exc}") from exc
    if data.get("protocol_version") != "WEARABLE_1B_EVALUATION_PROTOCOL_V1":
        raise ProtocolError("Unsupported protocol version")
    if data.get("evaluator_version") != "WEARABLE_1B_EVALUATOR_V1":
        raise ProtocolError("Protocol/evaluator version mismatch")
    allowed = data.get("conclusion_logic", {}).get("allowed_codes")
    expected = [
        "DATA_INTEGRITY_BLOCKED",
        "INSUFFICIENT_DATA",
        "NO_INCREMENTAL_SIGNAL",
        "PROMISING_NOT_VALIDATED",
        "BASELINE_SUPPORTED",
    ]
    if allowed != expected:
        raise ProtocolError("Protocol conclusion codes do not match V1")
    return LoadedProtocol(data=data, path=protocol_path, sha256=digest)


"""
AUTO-GENERATED FILE - DO NOT EDIT
Generated from: lifecycle-diagnostics.schema.json
"""

from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime
from enum import Enum
from typing import Any, TypeAlias

from .session_events import (
    from_datetime,
    from_int,
    from_list,
    from_none,
    from_str,
    from_union,
    parse_enum,
    to_class,
    to_datetime,
    to_enum,
    to_int,
)


# Experimental: this enum is part of an experimental API and may change or be removed.
class RuntimeLifecycleDisposition(Enum):
    "How a settled lifecycle operation or progress record ended."
    # The work completed.
    SUCCEEDED = "succeeded"
    # The work ended without reaching its intended boundary.
    FAILED = "failed"
    # Cancellation ended the work.
    CANCELLED = "cancelled"
    # A deadline ended the work.
    TIMED_OUT = "timed_out"
    # Newer work replaced this work.
    SUPERSEDED = "superseded"
    # The owner stopped observing the work without settling it.
    ABANDONED = "abandoned"
# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class RuntimeLifecycleBlockerTiming:
    "Timing of one real dependency wait."
    blocking_since: datetime

    @staticmethod
    def from_dict(obj: Any) -> "RuntimeLifecycleBlockerTiming":
        assert isinstance(obj, dict)
        blocking_since = from_datetime(obj.get("blockingSince"))
        return RuntimeLifecycleBlockerTiming(
            blocking_since=blocking_since,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["blockingSince"] = to_datetime(self.blocking_since)
        return result
# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class RuntimeLifecycleBlocker:
    "Exact lifecycle operation awaited by a real dependency wait."
    current_phase: str
    current_step: str
    operation_id: str
    operation_revision: int
    requirement: str
    system: str
    timing: RuntimeLifecycleBlockerTiming
    operation_disposition: RuntimeLifecycleDisposition | None = None

    @staticmethod
    def from_dict(obj: Any) -> "RuntimeLifecycleBlocker":
        assert isinstance(obj, dict)
        current_phase = from_str(obj.get("currentPhase"))
        current_step = from_str(obj.get("currentStep"))
        operation_id = from_str(obj.get("operationId"))
        operation_revision = from_int(obj.get("operationRevision"))
        requirement = from_str(obj.get("requirement"))
        system = from_str(obj.get("system"))
        timing = RuntimeLifecycleBlockerTiming.from_dict(obj.get("timing"))
        operation_disposition = from_union([from_none, lambda x: parse_enum(RuntimeLifecycleDisposition, x)], obj.get("operationDisposition"))
        return RuntimeLifecycleBlocker(
            current_phase=current_phase,
            current_step=current_step,
            operation_id=operation_id,
            operation_revision=operation_revision,
            requirement=requirement,
            system=system,
            timing=timing,
            operation_disposition=operation_disposition,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["currentPhase"] = from_str(self.current_phase)
        result["currentStep"] = from_str(self.current_step)
        result["operationId"] = from_str(self.operation_id)
        result["operationRevision"] = to_int(self.operation_revision)
        result["requirement"] = from_str(self.requirement)
        result["system"] = from_str(self.system)
        result["timing"] = to_class(RuntimeLifecycleBlockerTiming, self.timing)
        if self.operation_disposition is not None:
            result["operationDisposition"] = from_union([from_none, lambda x: to_enum(RuntimeLifecycleDisposition, x)], self.operation_disposition)
        return result
# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class RuntimeLifecycleProgressTiming:
    "Fixed timestamps for one observed progress revision."
    phase_started_at: datetime
    started_at: datetime
    transitioned_at: datetime

    @staticmethod
    def from_dict(obj: Any) -> "RuntimeLifecycleProgressTiming":
        assert isinstance(obj, dict)
        phase_started_at = from_datetime(obj.get("phaseStartedAt"))
        started_at = from_datetime(obj.get("startedAt"))
        transitioned_at = from_datetime(obj.get("transitionedAt"))
        return RuntimeLifecycleProgressTiming(
            phase_started_at=phase_started_at,
            started_at=started_at,
            transitioned_at=transitioned_at,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["phaseStartedAt"] = to_datetime(self.phase_started_at)
        result["startedAt"] = to_datetime(self.started_at)
        result["transitionedAt"] = to_datetime(self.transitioned_at)
        return result
# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class RuntimeLifecycleProgressOrigin:
    "Immutable causal origin of a progress record with independent identity and settlement."
    kind: str
    progress_id: str

    @staticmethod
    def from_dict(obj: Any) -> "RuntimeLifecycleProgressOrigin":
        assert isinstance(obj, dict)
        kind = from_str(obj.get("kind"))
        progress_id = from_str(obj.get("progressId"))
        return RuntimeLifecycleProgressOrigin(
            kind=kind,
            progress_id=progress_id,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["kind"] = from_str(self.kind)
        result["progressId"] = from_str(self.progress_id)
        return result
# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class RuntimeLifecycleProgressStateData:
    "Experimental, observational-only state of runtime progress and the lifecycle operations that block it. Observation and delivery never control runtime or model behavior. Process death, diagnostic projection loss, transport loss, or serialization failure can prevent external delivery."
    blockers: list[RuntimeLifecycleBlocker]
    incarnation_id: str
    kind: str
    phase: str
    progress_id: str
    revision: int
    session_id: str
    timing: RuntimeLifecycleProgressTiming
    disposition: RuntimeLifecycleDisposition | None = None
    origin: RuntimeLifecycleProgressOrigin | None = None

    @staticmethod
    def from_dict(obj: Any) -> "RuntimeLifecycleProgressStateData":
        assert isinstance(obj, dict)
        blockers = from_list(lambda x: RuntimeLifecycleBlocker.from_dict(x), obj.get("blockers"))
        incarnation_id = from_str(obj.get("incarnationId"))
        kind = from_str(obj.get("kind"))
        phase = from_str(obj.get("phase"))
        progress_id = from_str(obj.get("progressId"))
        revision = from_int(obj.get("revision"))
        session_id = from_str(obj.get("sessionId"))
        timing = RuntimeLifecycleProgressTiming.from_dict(obj.get("timing"))
        disposition = from_union([from_none, lambda x: parse_enum(RuntimeLifecycleDisposition, x)], obj.get("disposition"))
        origin = from_union([from_none, lambda x: RuntimeLifecycleProgressOrigin.from_dict(x)], obj.get("origin"))
        return RuntimeLifecycleProgressStateData(
            blockers=blockers,
            incarnation_id=incarnation_id,
            kind=kind,
            phase=phase,
            progress_id=progress_id,
            revision=revision,
            session_id=session_id,
            timing=timing,
            disposition=disposition,
            origin=origin,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["blockers"] = from_list(lambda x: to_class(RuntimeLifecycleBlocker, x), self.blockers)
        result["incarnationId"] = from_str(self.incarnation_id)
        result["kind"] = from_str(self.kind)
        result["phase"] = from_str(self.phase)
        result["progressId"] = from_str(self.progress_id)
        result["revision"] = to_int(self.revision)
        result["sessionId"] = from_str(self.session_id)
        result["timing"] = to_class(RuntimeLifecycleProgressTiming, self.timing)
        if self.disposition is not None:
            result["disposition"] = from_union([from_none, lambda x: to_enum(RuntimeLifecycleDisposition, x)], self.disposition)
        if self.origin is not None:
            result["origin"] = from_union([from_none, lambda x: to_class(RuntimeLifecycleProgressOrigin, x)], self.origin)
        return result
# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class RuntimeLifecycleOperationTiming:
    "Fixed timestamps for one observed operation revision."
    phase_started_at: datetime
    started_at: datetime
    transitioned_at: datetime

    @staticmethod
    def from_dict(obj: Any) -> "RuntimeLifecycleOperationTiming":
        assert isinstance(obj, dict)
        phase_started_at = from_datetime(obj.get("phaseStartedAt"))
        started_at = from_datetime(obj.get("startedAt"))
        transitioned_at = from_datetime(obj.get("transitionedAt"))
        return RuntimeLifecycleOperationTiming(
            phase_started_at=phase_started_at,
            started_at=started_at,
            transitioned_at=transitioned_at,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["phaseStartedAt"] = to_datetime(self.phase_started_at)
        result["startedAt"] = to_datetime(self.started_at)
        result["transitionedAt"] = to_datetime(self.transitioned_at)
        return result
# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class RuntimeLifecycleOperation:
    "Observed state of one readiness or convergence operation."
    operation_id: str
    operation_revision: int
    phase: str
    step: str
    timing: RuntimeLifecycleOperationTiming
    trigger: str
    disposition: RuntimeLifecycleDisposition | None = None

    @staticmethod
    def from_dict(obj: Any) -> "RuntimeLifecycleOperation":
        assert isinstance(obj, dict)
        operation_id = from_str(obj.get("operationId"))
        operation_revision = from_int(obj.get("operationRevision"))
        phase = from_str(obj.get("phase"))
        step = from_str(obj.get("step"))
        timing = RuntimeLifecycleOperationTiming.from_dict(obj.get("timing"))
        trigger = from_str(obj.get("trigger"))
        disposition = from_union([from_none, lambda x: parse_enum(RuntimeLifecycleDisposition, x)], obj.get("disposition"))
        return RuntimeLifecycleOperation(
            operation_id=operation_id,
            operation_revision=operation_revision,
            phase=phase,
            step=step,
            timing=timing,
            trigger=trigger,
            disposition=disposition,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["operationId"] = from_str(self.operation_id)
        result["operationRevision"] = to_int(self.operation_revision)
        result["phase"] = from_str(self.phase)
        result["step"] = from_str(self.step)
        result["timing"] = to_class(RuntimeLifecycleOperationTiming, self.timing)
        result["trigger"] = from_str(self.trigger)
        if self.disposition is not None:
            result["disposition"] = from_union([from_none, lambda x: to_enum(RuntimeLifecycleDisposition, x)], self.disposition)
        return result
# Experimental: this type is part of an experimental API and may change or be removed.
@dataclass
class RuntimeLifecycleSystemStateData:
    "Experimental, observational-only state of one runtime system after an operation transition. Observation and delivery never control runtime work. Process death, diagnostic projection loss, transport loss, or serialization failure can prevent external delivery."
    active_operations: list[RuntimeLifecycleOperation]
    changed_operation: RuntimeLifecycleOperation
    incarnation_id: str
    session_id: str
    system: str
    system_revision: int
    condition: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> "RuntimeLifecycleSystemStateData":
        assert isinstance(obj, dict)
        active_operations = from_list(lambda x: RuntimeLifecycleOperation.from_dict(x), obj.get("activeOperations"))
        changed_operation = RuntimeLifecycleOperation.from_dict(obj.get("changedOperation"))
        incarnation_id = from_str(obj.get("incarnationId"))
        session_id = from_str(obj.get("sessionId"))
        system = from_str(obj.get("system"))
        system_revision = from_int(obj.get("systemRevision"))
        condition = from_union([from_none, lambda x: from_str(x)], obj.get("condition"))
        return RuntimeLifecycleSystemStateData(
            active_operations=active_operations,
            changed_operation=changed_operation,
            incarnation_id=incarnation_id,
            session_id=session_id,
            system=system,
            system_revision=system_revision,
            condition=condition,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["activeOperations"] = from_list(lambda x: to_class(RuntimeLifecycleOperation, x), self.active_operations)
        result["changedOperation"] = to_class(RuntimeLifecycleOperation, self.changed_operation)
        result["incarnationId"] = from_str(self.incarnation_id)
        result["sessionId"] = from_str(self.session_id)
        result["system"] = from_str(self.system)
        result["systemRevision"] = to_int(self.system_revision)
        if self.condition is not None:
            result["condition"] = from_union([from_none, lambda x: from_str(x)], self.condition)
        return result
class LifecycleDiagnosticEventType(Enum):
    RUNTIME_LIFECYCLE_SYSTEM_STATE = "runtime.lifecycle.system_state"
    RUNTIME_LIFECYCLE_PROGRESS_STATE = "runtime.lifecycle.progress_state"


@dataclass
class RuntimeLifecycleSystemStateDiagnosticEvent:
    data: RuntimeLifecycleSystemStateData
    timestamp: datetime
    type: LifecycleDiagnosticEventType

    @staticmethod
    def from_dict(obj: Any) -> RuntimeLifecycleSystemStateDiagnosticEvent:
        assert isinstance(obj, dict)
        return RuntimeLifecycleSystemStateDiagnosticEvent(
            data=RuntimeLifecycleSystemStateData.from_dict(obj.get("data")),
            timestamp=from_datetime(obj.get("timestamp")),
            type=LifecycleDiagnosticEventType(from_str(obj.get("type"))),
        )

    def to_dict(self) -> dict[str, Any]:
        return {
            "data": self.data.to_dict(),
            "timestamp": to_datetime(self.timestamp),
            "type": self.type.value,
        }


@dataclass
class RuntimeLifecycleProgressStateDiagnosticEvent:
    data: RuntimeLifecycleProgressStateData
    timestamp: datetime
    type: LifecycleDiagnosticEventType

    @staticmethod
    def from_dict(obj: Any) -> RuntimeLifecycleProgressStateDiagnosticEvent:
        assert isinstance(obj, dict)
        return RuntimeLifecycleProgressStateDiagnosticEvent(
            data=RuntimeLifecycleProgressStateData.from_dict(obj.get("data")),
            timestamp=from_datetime(obj.get("timestamp")),
            type=LifecycleDiagnosticEventType(from_str(obj.get("type"))),
        )

    def to_dict(self) -> dict[str, Any]:
        return {
            "data": self.data.to_dict(),
            "timestamp": to_datetime(self.timestamp),
            "type": self.type.value,
        }


LifecycleDiagnosticEvent: TypeAlias = (
    RuntimeLifecycleSystemStateDiagnosticEvent | RuntimeLifecycleProgressStateDiagnosticEvent
)


def lifecycle_diagnostic_event_from_dict(obj: Any) -> LifecycleDiagnosticEvent:
    assert isinstance(obj, dict)
    event_type = LifecycleDiagnosticEventType(from_str(obj.get("type")))
    match event_type:
        case LifecycleDiagnosticEventType.RUNTIME_LIFECYCLE_SYSTEM_STATE:
            return RuntimeLifecycleSystemStateDiagnosticEvent.from_dict(obj)
        case LifecycleDiagnosticEventType.RUNTIME_LIFECYCLE_PROGRESS_STATE:
            return RuntimeLifecycleProgressStateDiagnosticEvent.from_dict(obj)
    raise ValueError(f"Unknown lifecycle diagnostic event type: {event_type.value}")


def lifecycle_diagnostic_event_to_dict(event: LifecycleDiagnosticEvent) -> dict[str, Any]:
    return event.to_dict()


__all__ = [
    "LifecycleDiagnosticEvent",
    "LifecycleDiagnosticEventType",
    "RuntimeLifecycleBlocker",
    "RuntimeLifecycleBlockerTiming",
    "RuntimeLifecycleDisposition",
    "RuntimeLifecycleOperation",
    "RuntimeLifecycleOperationTiming",
    "RuntimeLifecycleProgressOrigin",
    "RuntimeLifecycleProgressStateData",
    "RuntimeLifecycleProgressTiming",
    "RuntimeLifecycleSystemStateData",
    "RuntimeLifecycleSystemStateDiagnosticEvent",
    "RuntimeLifecycleProgressStateDiagnosticEvent",
    "lifecycle_diagnostic_event_from_dict",
    "lifecycle_diagnostic_event_to_dict",
]

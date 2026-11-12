//! Auto-generated from lifecycle-diagnostics.schema.json — do not edit manually.

use serde::{Deserialize, Serialize};

use crate::types::SessionId;

/// How a settled lifecycle operation or progress record ended.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub(crate) enum RuntimeLifecycleDisposition {
    /// The work completed.
    #[serde(rename = "succeeded")]
    Succeeded,
    /// The work ended without reaching its intended boundary.
    #[serde(rename = "failed")]
    Failed,
    /// Cancellation ended the work.
    #[serde(rename = "cancelled")]
    Cancelled,
    /// A deadline ended the work.
    #[serde(rename = "timed_out")]
    TimedOut,
    /// Newer work replaced this work.
    #[serde(rename = "superseded")]
    Superseded,
    /// The owner stopped observing the work without settling it.
    #[serde(rename = "abandoned")]
    Abandoned,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}
/// Timing of one real dependency wait.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct RuntimeLifecycleBlockerTiming {
    /// ISO 8601 timestamp when this dependency began blocking progress.
    pub blocking_since: String,
}
/// Exact lifecycle operation awaited by a real dependency wait.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct RuntimeLifecycleBlocker {
    /// System-owned capability required by the progress owner.
    pub requirement: String,
    /// System that owns the awaited operation.
    pub system: String,
    /// Opaque awaited operation identifier.
    pub operation_id: String,
    /// Awaited operation revision observed by this blocker.
    pub operation_revision: i64,
    /// Current phase of the awaited operation.
    pub current_phase: String,
    /// Current production boundary of the awaited operation.
    pub current_step: String,
    /// How the awaited operation ended when the real wait remains active.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub operation_disposition: Option<RuntimeLifecycleDisposition>,
    /// Timing of this dependency wait.
    pub timing: RuntimeLifecycleBlockerTiming,
}
/// Fixed timestamps for one observed operation revision.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct RuntimeLifecycleOperationTiming {
    /// ISO 8601 timestamp when the operation began.
    pub started_at: String,
    /// ISO 8601 timestamp when the current phase began.
    pub phase_started_at: String,
    /// ISO 8601 timestamp when this operation revision was recorded.
    pub transitioned_at: String,
}
/// Observed state of one readiness or convergence operation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct RuntimeLifecycleOperation {
    /// Opaque operation identifier, unique within the lifecycle incarnation and system.
    pub operation_id: String,
    /// Orders transitions of this operation.
    pub operation_revision: i64,
    /// System-owned reason the operation started.
    pub trigger: String,
    /// System-owned lifecycle stage.
    pub phase: String,
    /// System-owned production boundary within the phase.
    pub step: String,
    /// How the operation ended. Absent while the operation remains active.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub disposition: Option<RuntimeLifecycleDisposition>,
    /// Timestamps recorded by the lifecycle service for this operation revision.
    pub timing: RuntimeLifecycleOperationTiming,
}
/// Immutable causal origin of a progress record with independent identity and settlement.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct RuntimeLifecycleProgressOrigin {
    /// Kind of the originating progress record.
    pub kind: String,
    /// Identifier of the originating progress record.
    pub progress_id: String,
}
/// Fixed timestamps for one observed progress revision.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct RuntimeLifecycleProgressTiming {
    /// ISO 8601 timestamp when the progress record began.
    pub started_at: String,
    /// ISO 8601 timestamp when the current phase began.
    pub phase_started_at: String,
    /// ISO 8601 timestamp when this progress revision was recorded.
    pub transitioned_at: String,
}
/// Experimental, observational-only state of runtime progress and the lifecycle operations that block it. Observation and delivery never control runtime or model behavior. Process death, diagnostic projection loss, transport loss, or serialization failure can prevent external delivery.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct RuntimeLifecycleProgressStateData {
    /// Session whose lifecycle owns this progress.
    pub session_id: SessionId,
    /// Opaque identity for one session registry generation.
    pub incarnation_id: String,
    /// Progress-owner-defined kind.
    pub kind: String,
    /// Opaque progress identifier, unique within the incarnation and kind.
    pub progress_id: String,
    /// Orders changes to this progress record.
    pub revision: i64,
    /// Progress-owner-defined phase.
    pub phase: String,
    /// How the progress record ended. Absent while it remains active.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub disposition: Option<RuntimeLifecycleDisposition>,
    /// Immutable causal origin, when another lifecycle progress record caused this one.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub origin: Option<RuntimeLifecycleProgressOrigin>,
    /// Timestamps recorded by the lifecycle service for this progress revision.
    pub timing: RuntimeLifecycleProgressTiming,
    /// Complete dependency blocker set after this progress change.
    pub blockers: Vec<RuntimeLifecycleBlocker>,
}
/// Experimental, observational-only state of one runtime system after an operation transition. Observation and delivery never control runtime work. Process death, diagnostic projection loss, transport loss, or serialization failure can prevent external delivery.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct RuntimeLifecycleSystemStateData {
    /// Session whose lifecycle owns this system.
    pub session_id: SessionId,
    /// Opaque identity for one session registry generation.
    pub incarnation_id: String,
    /// Stable subsystem identifier.
    pub system: String,
    /// Orders every observed change to this system in the incarnation.
    pub system_revision: i64,
    /// Authoritative condition retained after this change. Initial shared values are `current`, `disabled`, `degraded`, and `quiescent`; integrations can add bounded values as the contract matures.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub condition: Option<String>,
    /// Operation transition that produced this system revision.
    pub changed_operation: RuntimeLifecycleOperation,
    /// Complete active operation set after applying the change.
    pub active_operations: Vec<RuntimeLifecycleOperation>,
}
#[derive(Debug, Clone, Copy, Serialize, Deserialize)]
enum RuntimeLifecycleSystemStateDiagnosticEventType {
    #[serde(rename = "runtime.lifecycle.system_state")]
    Value,
}

/// Experimental runtime lifecycle diagnostic event.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct RuntimeLifecycleSystemStateDiagnosticEvent {
    #[serde(rename = "type")]
    event_type: RuntimeLifecycleSystemStateDiagnosticEventType,
    /// ISO 8601 timestamp when the runtime projected this event.
    pub timestamp: String,
    /// Lifecycle observation payload.
    pub data: RuntimeLifecycleSystemStateData,
}

#[derive(Debug, Clone, Copy, Serialize, Deserialize)]
enum RuntimeLifecycleProgressStateDiagnosticEventType {
    #[serde(rename = "runtime.lifecycle.progress_state")]
    Value,
}

/// Experimental runtime lifecycle diagnostic event.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct RuntimeLifecycleProgressStateDiagnosticEvent {
    #[serde(rename = "type")]
    event_type: RuntimeLifecycleProgressStateDiagnosticEventType,
    /// ISO 8601 timestamp when the runtime projected this event.
    pub timestamp: String,
    /// Lifecycle observation payload.
    pub data: RuntimeLifecycleProgressStateData,
}

/// Experimental lifecycle diagnostic event union.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub(crate) enum LifecycleDiagnosticEvent {
    SystemState(RuntimeLifecycleSystemStateDiagnosticEvent),
    ProgressState(RuntimeLifecycleProgressStateDiagnosticEvent),
}

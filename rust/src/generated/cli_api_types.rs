//! Auto-generated from api.schema.json — do not edit manually.

#![allow(clippy::large_enum_variant)]
#![allow(deprecated)]
#![allow(dead_code)]
#![allow(rustdoc::invalid_html_tags)]

use serde::{Deserialize, Serialize};

use crate::types::SessionId;

/// JSON-RPC method name constants.
pub mod rpc_methods {
    /// `cafeMetrics.start`
    pub const CAFEMETRICS_START: &str = "cafeMetrics.start";
    /// `cafeMetrics.resolveAdmission`
    pub const CAFEMETRICS_RESOLVEADMISSION: &str = "cafeMetrics.resolveAdmission";
    /// `cafeMetrics.recordResumeResult`
    pub const CAFEMETRICS_RECORDRESUMERESULT: &str = "cafeMetrics.recordResumeResult";
    /// `cafeMetrics.shutdown`
    pub const CAFEMETRICS_SHUTDOWN: &str = "cafeMetrics.shutdown";
    /// `session.canvasSource.close`
    pub const SESSION_CANVASSOURCE_CLOSE: &str = "session.canvasSource.close";
    /// `session.cafeMetrics.attach`
    pub const SESSION_CAFEMETRICS_ATTACH: &str = "session.cafeMetrics.attach";
    /// `session.cafeMetrics.detach`
    pub const SESSION_CAFEMETRICS_DETACH: &str = "session.cafeMetrics.detach";
}

/// Terminal result of one resume attempt.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CafeMetricsRecordResumeResultRequest {
    /// Terminal outcome.
    pub outcome: CafeMetricsResumeOutcome,
    /// Bounded result classification.
    pub reason: CafeMetricsResumeReason,
    /// Whether an event-log recovery projection was used.
    pub recovered: bool,
    /// Last phase reached.
    pub stage: CafeMetricsResumeStage,
}

/// Release metadata without credentials or links.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CafeMetricsRelease {
    /// Whether this is a draft.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub draft: Option<bool>,
    /// Whether this is a prerelease.
    pub prerelease: bool,
    /// Raw release tag.
    pub tag_name: String,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CafeMetricsResolveAdmissionRequestSuccess {
    pub kind: CafeMetricsResolveAdmissionRequestSuccessKind,
    /// Finite non-negative lookup duration in milliseconds.
    pub lookup_duration_ms: f64,
    /// Release metadata from the lookup shared with auto-update.
    pub releases: Vec<CafeMetricsRelease>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CafeMetricsResolveAdmissionRequestFailed {
    pub kind: CafeMetricsResolveAdmissionRequestFailedKind,
    /// Finite non-negative lookup duration in milliseconds.
    pub lookup_duration_ms: f64,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CafeMetricsResolveAdmissionResultApplied {
    pub kind: CafeMetricsResolveAdmissionResultAppliedKind,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CafeMetricsResolveAdmissionResultNotStarted {
    pub kind: CafeMetricsResolveAdmissionResultNotStartedKind,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CafeMetricsResolveAdmissionResultAlreadyResolved {
    pub kind: CafeMetricsResolveAdmissionResultAlreadyResolvedKind,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CafeMetricsResolveAdmissionResultShutdown {
    pub kind: CafeMetricsResolveAdmissionResultShutdownKind,
}

/// Versions and provider configuration for CAFE.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CafeMetricsStartRequest {
    /// Effective comparison version and appVersion tag.
    pub app_version: String,
    /// Raw package build version.
    pub build_version: String,
    /// Whether the host uses a custom model provider.
    pub custom_provider_configured: bool,
    /// Embedded package identity. Omission enables the unbundled development fallback.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub embedded_package_identity: Option<CafeMetricsEmbeddedPackageIdentity>,
}

/// CAFE startup result.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CafeMetricsStartResult {
    /// Refusal reason, absent on success.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reason: Option<CafeMetricsStartRefusal>,
    /// Whether this call started CAFE.
    pub started: bool,
}

/// Preliminary flush or final disposal.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CafeMetricsShutdownRequest {
    /// Send at most one batch without stopping recording. Omission means disposal.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub flush_only: Option<bool>,
}

/// Closes a canvas through its original owned or attached session namespace.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CanvasSourceCloseRequest {
    /// Target session identifier
    pub session_id: SessionId,
    /// Open canvas instance identifier
    pub instance_id: String,
    /// Exact authenticated local attachment identifier. Omit for the SDK connection's owned session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub attach_id: Option<i64>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionCafeMetricsAttachParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionCafeMetricsDetachParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Embedded package identity.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CafeMetricsEmbeddedPackageIdentity {
    /// Development package.
    #[serde(rename = "dev")]
    Dev,
    /// Release package.
    #[serde(rename = "release")]
    Release,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Terminal resume outcome.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CafeMetricsResumeOutcome {
    /// Resumed.
    #[serde(rename = "success")]
    Success,
    /// Recovered and resumed.
    #[serde(rename = "recovered")]
    Recovered,
    /// Resume failed.
    #[serde(rename = "failed")]
    Failed,
    /// No session found.
    #[serde(rename = "not_found")]
    NotFound,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Bounded resume reason, without error text.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CafeMetricsResumeReason {
    /// No detail.
    #[serde(rename = "none")]
    None,
    /// Recovered event log.
    #[serde(rename = "log_recovered")]
    LogRecovered,
    /// Corrupt event log.
    #[serde(rename = "log_corrupt")]
    LogCorrupt,
    /// Legacy format.
    #[serde(rename = "legacy_format")]
    LegacyFormat,
    /// Recovery I/O failed.
    #[serde(rename = "recovery_io")]
    RecoveryIo,
    /// Hydration failed.
    #[serde(rename = "hydration")]
    Hydration,
    /// Construction failed.
    #[serde(rename = "construction")]
    Construction,
    /// Queue restore failed.
    #[serde(rename = "queue")]
    Queue,
    /// Session not found.
    #[serde(rename = "not_found")]
    NotFound,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Last resume phase.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CafeMetricsResumeStage {
    /// Read the event log.
    #[serde(rename = "event_log")]
    EventLog,
    /// Construct the session.
    #[serde(rename = "construction")]
    Construction,
    /// Restore the queue.
    #[serde(rename = "queue_restore")]
    QueueRestore,
    /// Resume complete.
    #[serde(rename = "complete")]
    Complete,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CafeMetricsResolveAdmissionRequestSuccessKind {
    #[serde(rename = "success")]
    #[default]
    Success,
}

#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CafeMetricsResolveAdmissionRequestFailedKind {
    #[serde(rename = "failed")]
    #[default]
    Failed,
}

/// Immutable release lookup result.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum CafeMetricsResolveAdmissionRequest {
    Success(CafeMetricsResolveAdmissionRequestSuccess),
    Failed(CafeMetricsResolveAdmissionRequestFailed),
}

#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CafeMetricsResolveAdmissionResultAppliedKind {
    #[serde(rename = "applied")]
    #[default]
    Applied,
}

#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CafeMetricsResolveAdmissionResultNotStartedKind {
    #[serde(rename = "not-started")]
    #[default]
    NotStarted,
}

#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CafeMetricsResolveAdmissionResultAlreadyResolvedKind {
    #[serde(rename = "already-resolved")]
    #[default]
    AlreadyResolved,
}

#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CafeMetricsResolveAdmissionResultShutdownKind {
    #[serde(rename = "shutdown")]
    #[default]
    Shutdown,
}

/// Admission result.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum CafeMetricsResolveAdmissionResult {
    Applied(CafeMetricsResolveAdmissionResultApplied),
    NotStarted(CafeMetricsResolveAdmissionResultNotStarted),
    AlreadyResolved(CafeMetricsResolveAdmissionResultAlreadyResolved),
    Shutdown(CafeMetricsResolveAdmissionResultShutdown),
}

/// Reason startup was refused.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CafeMetricsStartRefusal {
    /// The process is offline.
    #[serde(rename = "offline")]
    Offline,
    /// Unsupported platform.
    #[serde(rename = "unsupportedPlatform")]
    UnsupportedPlatform,
    /// The process already started CAFE.
    #[serde(rename = "alreadyStarted")]
    AlreadyStarted,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

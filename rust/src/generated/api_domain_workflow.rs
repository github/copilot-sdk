//! Auto-generated from api.schema.json — do not edit manually.

#![allow(clippy::large_enum_variant)]
#![allow(deprecated)]
#![allow(dead_code)]
#![allow(unused_imports)]
#![allow(rustdoc::invalid_html_tags)]

use std::collections::HashMap;

pub(crate) use lifecycle_diagnostics::LifecycleDiagnosticEvent;
use serde::{Deserialize, Serialize};
pub use session_events::{
    AbortReason, AgentModelPolicy, AssistedApprovalAssessment, AutoTier, BlockedPermissionAction,
    ContextTier, IndexedSearchState, ManagedPermissionsContext, ManagedSettingsResolvedSource,
    McpOauthHttpResponse, McpOauthWWWAuthenticateParams, McpServerMetadata, McpServerSource,
    McpServerStatus, McpSkillOrigin, ModelChangeSource, ModelProviderKind, ModelProviderRef,
    OmittedBinaryOmittedReason, PermissionDecisionSource, PermissionMode, PermissionPromptRequest,
    PermissionRule, PolicyHelperCapabilities, PolicyHelperFailure, ProviderQuotaState,
    ReasoningSummary, RemediationAction, SessionLimitsConfig, SessionMode, ShutdownType,
    SkillSource, TaskBlocker, TaskCompletionOutcome, UsageGetMetricsResult,
    UserToolSessionApproval, Verbosity,
};

use super::sdk_api_shared::*;
use crate::generated::{lifecycle_diagnostics, session_events};
use crate::types::{RequestId, SessionEvent, SessionId};

/// Parameters for cooperatively aborting a workflow body.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkflowAbortRequest {
    /// Target session identifier
    pub session_id: SessionId,
    /// Workflow run identifier.
    pub run_id: String,
    /// Opaque token identifying the execution attempt to abort.
    pub execution_token: String,
}

/// Acknowledgement that a workflow request was accepted.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkflowAckResult {}

/// Options for one workflow-scoped subagent call.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkflowAgentOptions {
    /// Optional built-in or custom agent name whose definition configures the subagent.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub agent: Option<String>,
    /// Optional context tier override for the subagent.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context_tier: Option<ContextTier>,
    /// Optional label distinguishing otherwise identical memoized agent calls.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub label: Option<String>,
    /// Optional model identifier for the subagent.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model: Option<String>,
    /// Optional reasoning effort override for the subagent.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_effort: Option<String>,
    /// Optional JSON Schema for structured agent output.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub schema: Option<serde_json::Value>,
}

/// Parameters for one workflow-scoped subagent call.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkflowAgentRequest {
    /// Opaque token identifying the current workflow execution attempt.
    pub execution_token: String,
    /// Subagent execution options.
    pub opts: WorkflowAgentOptions,
    /// Prompt to send to the subagent.
    pub prompt: String,
    /// Workflow run identifier that owns the subagent.
    pub workflow_run_id: String,
}

/// Result of one workflow-scoped subagent call.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkflowAgentResult {
    /// Agent result, omitted when the agent produced no result.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub result: Option<serde_json::Value>,
}

/// Parameters for cancelling a workflow run.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkflowCancelRequest {
    /// Workflow run identifier.
    pub run_id: String,
}

/// Parameters sent to the owning extension to execute a workflow closure.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkflowExecuteRequest {
    /// Target session identifier
    pub session_id: SessionId,
    /// Registered workflow name.
    pub name: String,
    /// Workflow run identifier.
    pub run_id: String,
    /// Opaque token identifying this workflow execution attempt.
    pub execution_token: String,
    /// Workflow input value.
    pub args: serde_json::Value,
}

/// Result returned by an extension workflow closure.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkflowExecuteResult {
    /// Workflow result value.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub result: Option<serde_json::Value>,
}

/// Parameters for paging workflow progress.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkflowGetRunProgressRequest {
    /// Exclusive forward cursor.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub after_seq: Option<i64>,
    /// Exclusive backward cursor.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub before_seq: Option<i64>,
    /// Maximum records to return. Defaults to 200 and is capped at 500.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub limit: Option<i32>,
    /// Optional phase identifier used to scope records and cursors.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub phase_id: Option<String>,
    /// Workflow run identifier.
    pub run_id: String,
}

/// Parameters for retrieving a workflow run.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkflowGetRunRequest {
    /// Workflow run identifier.
    pub run_id: String,
}

/// Parameters for reading a workflow journal entry.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkflowJournalGetRequest {
    /// Opaque token identifying the current workflow execution attempt.
    pub execution_token: String,
    /// Namespaced journal key.
    pub key: String,
    /// Workflow run identifier.
    pub run_id: String,
}

/// Result of reading a workflow journal entry.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkflowJournalGetResult {
    /// Whether the journal contained the requested key.
    pub hit: bool,
    /// Cached JSON result. The hit field distinguishes a cached JSON null from a miss.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub result_json: Option<serde_json::Value>,
}

/// Parameters for storing a workflow journal entry.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkflowJournalPutRequest {
    /// Opaque token identifying the current workflow execution attempt.
    pub execution_token: String,
    /// Namespaced journal key.
    pub key: String,
    /// JSON result to memoize.
    pub result_json: serde_json::Value,
    /// Workflow run identifier.
    pub run_id: String,
}

/// Parameters for paging workflow runs.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkflowListRunsRequest {
    /// Exclusive forward cursor.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub after_seq: Option<i64>,
    /// Exclusive backward cursor.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub before_seq: Option<i64>,
    /// Maximum terminal runs to return. Defaults to 200 and is capped at 500.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub limit: Option<i32>,
}

/// A page of workflow runs in durable creation order.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkflowListRunsResult {
    /// Whether terminal runs newer than this page exist.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub has_more_newer: Option<bool>,
    /// Newest terminal-run cursor in this page, or null when the terminal window is empty.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub newest_seq: Option<i64>,
    /// Oldest terminal-run cursor in this page, or null when the terminal window is empty.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub oldest_seq: Option<i64>,
    /// Number of terminal runs older than this page.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub omitted_older: Option<i64>,
    /// Workflow run summaries in durable creation order.
    pub runs: Vec<WorkflowRunSummary>,
}

/// One ordered workflow progress line.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkflowLogLine {
    /// Progress line kind.
    pub kind: WorkflowLogLineKind,
    /// Monotonic sequence number within the workflow run.
    pub seq: i64,
    /// Progress text.
    pub text: String,
}

/// Parameters for recording workflow progress.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkflowLogRequest {
    /// Opaque token identifying the current workflow execution attempt.
    pub execution_token: String,
    /// Ordered progress lines to append.
    pub lines: Vec<WorkflowLogLine>,
    /// Workflow run identifier.
    pub run_id: String,
}

/// Parameters for an owned durable pause checkpoint.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkflowPauseCheckpointRequest {
    /// Opaque token identifying the execution attempt that reached the checkpoint.
    pub execution_token: String,
    /// Stable author-defined checkpoint key.
    pub key: String,
    /// Workflow run identifier.
    pub run_id: String,
}

///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkflowPauseCheckpointResult {
    /// Whether this execution attempt must pause or may continue.
    pub action: WorkflowPauseCheckpointAction,
}

/// Parameters for pausing a running workflow.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkflowPauseRequest {
    /// Workflow run identifier.
    pub run_id: String,
}

/// Wire-only per-invocation workflow resource ceiling overrides.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkflowRunLimits {
    /// Maximum AI credits consumed by workflow subagents and their descendants. The post-paid ceiling is soft: parallel turns can settle beyond it before the run stops.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_ai_credits: Option<f64>,
    /// Maximum number of workflow subagents that may run concurrently.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_concurrent_subagents: Option<i64>,
    /// Maximum total number of workflow subagents that may be admitted.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_total_subagents: Option<i64>,
    /// Maximum accumulated active-execution time in seconds. Active execution includes the entire extension body, subprocess waits, queued-agent waits, and sleeps; time between resumed attempts is not counted.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub timeout_seconds: Option<f64>,
}

/// Parameters for resuming a workflow run from its persisted identity.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkflowResumeRequest {
    /// Optional per-invocation resource ceiling overrides.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub limits: Option<WorkflowRunLimits>,
    /// Whether to emit workflow phase names to the session transcript.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub log_phase_names: Option<bool>,
    /// Whether to notify the originating session when the workflow completes.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub notify_on_complete: Option<bool>,
    /// Workflow run identifier.
    pub run_id: String,
}

/// Resolved persisted workflow identity and resumed run envelope.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkflowResumeResult {
    /// Terminal resumed run envelope.
    pub run: WorkflowRunResult,
    /// Persisted workflow name resolved for the resumed run.
    pub workflow_name: String,
}

/// Full workflow run observability detail.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkflowRunDetail {
    /// Epoch milliseconds when the current active segment started, or null while inactive.
    pub active_segment_started_at: Option<i64>,
    /// Durable identities and live statuses for direct workflow agents.
    pub agents: Vec<WorkflowAgentSummary>,
    /// Approved effective resource ceilings, or null until approved.
    pub approved: Option<WorkflowDeclaredLimits>,
    /// Whether the durable run state currently passes runtime resume eligibility checks.
    pub can_resume: bool,
    /// Epoch milliseconds when the run completed, or null while nonterminal.
    pub completed_at: Option<i64>,
    /// Durable resource consumption.
    pub consumed: WorkflowRunConsumed,
    /// Epoch milliseconds when the run was created.
    pub created_at: i64,
    /// Current phase identity, or null before any phase is entered.
    pub current_phase: Option<WorkflowCurrentPhase>,
    /// Resource ceilings declared by the workflow.
    pub declared_limits: WorkflowDeclaredLimits,
    /// Number of phases declared by the workflow.
    pub declared_phase_count: i64,
    /// Human-readable workflow description.
    pub description: String,
    /// Number of direct workflow agents currently live.
    pub live_agent_count: i64,
    /// Epoch milliseconds when this live-overlay snapshot was observed.
    pub observed_at: i64,
    /// Lifecycle and timing observations for each workflow phase.
    pub phases: Vec<WorkflowPhaseObservation>,
    /// Bidirectional page of durable workflow progress.
    pub progress: WorkflowProgressPage,
    /// Monotonic durable run revision.
    pub revision: i64,
    /// Workflow run identifier.
    pub run_id: String,
    /// Epoch milliseconds when execution first started, or null before start.
    pub started_at: Option<i64>,
    /// Current workflow run status.
    pub status: WorkflowRunStatus,
    /// Terminal run outcome, or null while nonterminal.
    pub terminal: Option<WorkflowRunTerminal>,
    /// Total direct workflow agents spawned across all attempts.
    pub total_spawned_agent_count: i64,
    /// Epoch milliseconds when the durable run was last updated.
    pub updated_at: i64,
    /// Registered workflow name.
    pub workflow_name: String,
}

/// Options controlling workflow invocation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkflowRunOptions {
    /// Per-invocation resource ceiling overrides.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub limits: Option<WorkflowRunLimits>,
    /// Whether to emit workflow phase names to the session transcript.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub log_phase_names: Option<bool>,
    /// Whether to notify the originating session when the workflow completes.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub notify_on_complete: Option<bool>,
    /// Run identifier whose journal and progress should seed this resumed run.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub resume_from_run_id: Option<String>,
}

/// Parameters for invoking a registered workflow.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkflowRunRequest {
    /// Workflow input value.
    pub args: serde_json::Value,
    /// Registered workflow name.
    pub name: String,
    /// Workflow invocation options.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub options: Option<WorkflowRunOptions>,
}

/// Internal parameters for resuming a workflow run from a tool.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct WorkflowToolResumeRequest {
    /// Optional per-invocation resource ceiling overrides.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub limits: Option<WorkflowRunLimits>,
    /// Workflow run identifier.
    pub run_id: String,
    /// Opaque identifier of the originating tool call.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_call_id: Option<String>,
}

/// Options for an internal tool-originated workflow invocation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct WorkflowToolRunOptions {
    /// Per-invocation resource ceiling overrides.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub limits: Option<WorkflowRunLimits>,
    /// Run identifier whose journal and progress should seed this resumed run.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub resume_from_run_id: Option<String>,
}

/// Internal parameters for invoking a registered workflow from a tool.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct WorkflowToolRunRequest {
    /// Workflow input value.
    pub args: serde_json::Value,
    /// Registered workflow name.
    pub name: String,
    /// Tool-originated workflow invocation options.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) options: Option<WorkflowToolRunOptions>,
    /// Opaque identifier of the originating tool call.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_call_id: Option<String>,
}

/// Acknowledgement that a workflow request was accepted.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkflowAbortResult {}

/// Execution-critical workflow storage operation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum WorkflowDurableOperation {
    /// Creating the durable run and declared phases.
    #[serde(rename = "createRun")]
    CreateRun,
    /// Persisting the transition to running.
    #[serde(rename = "markRunStarted")]
    MarkRunStarted,
    /// Persisting the terminal run envelope.
    #[serde(rename = "finishRun")]
    FinishRun,
    /// Persisting subagent admission accounting.
    #[serde(rename = "reserveAgent")]
    ReserveAgent,
    /// Rolling back an uncommitted subagent admission.
    #[serde(rename = "releaseAgent")]
    ReleaseAgent,
    /// Persisting an idempotent model-usage charge.
    #[serde(rename = "chargeCredit")]
    ChargeCredit,
    /// Persisting active execution time.
    #[serde(rename = "addElapsed")]
    AddElapsed,
    /// Reading the authoritative AI-credit total.
    #[serde(rename = "reconcileCreditTotal")]
    ReconcileCreditTotal,
    /// Reading a journal entry without treating storage failure as a cache miss.
    #[serde(rename = "journalGet")]
    JournalGet,
    /// Persisting a journal entry before reporting success.
    #[serde(rename = "journalPut")]
    JournalPut,
    /// Renewing the durable owner lease that proves this process still owns the run.
    #[serde(rename = "refreshLease")]
    RefreshLease,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Cumulative resource ceiling that stopped a workflow run.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum WorkflowRunFailureKind {
    /// The run admitted the approved maximum total number of subagents.
    #[serde(rename = "maxTotalSubagents")]
    MaxTotalSubagents,
    /// The run reached the approved accumulated active-execution time in seconds.
    #[serde(rename = "timeoutSeconds")]
    TimeoutSeconds,
    /// The run's settled subagent model usage exceeded the approved AI-credit ceiling, or no headroom remained for another subagent.
    #[serde(rename = "maxAiCredits")]
    MaxAiCredits,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

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

///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AcceptedEnqueueCommandResult {
    /// True because the command was accepted into the local execution queue.
    #[serde(
        deserialize_with = "AcceptedEnqueueCommandResult::deserialize_queued",
        serialize_with = "AcceptedEnqueueCommandResult::serialize_queued"
    )]
    pub queued: bool,
    /// Stable opaque ID of the queued command.
    pub queue_id: String,
}

impl AcceptedEnqueueCommandResult {
    fn deserialize_queued<'de, D>(deserializer: D) -> Result<bool, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let value = bool::deserialize(deserializer)?;
        if !value {
            return Err(serde::de::Error::custom("expected true"));
        }
        Ok(value)
    }

    fn serialize_queued<S>(value: &bool, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: serde::Serializer,
    {
        if !*value {
            return Err(serde::ser::Error::custom("expected true"));
        }
        value.serialize(serializer)
    }
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
pub struct UnsupportedEnqueueCommandResult {
    /// False because the target session does not support local command queueing (e.g. remote sessions).
    #[serde(
        deserialize_with = "UnsupportedEnqueueCommandResult::deserialize_queued",
        serialize_with = "UnsupportedEnqueueCommandResult::serialize_queued"
    )]
    pub queued: bool,
    /// Legacy null queue ID accepted for compatibility with older runtimes.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub queue_id: Option<serde_json::Value>,
}

impl UnsupportedEnqueueCommandResult {
    fn deserialize_queued<'de, D>(deserializer: D) -> Result<bool, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let value = bool::deserialize(deserializer)?;
        if value {
            return Err(serde::de::Error::custom("expected false"));
        }
        Ok(value)
    }

    fn serialize_queued<S>(value: &bool, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: serde::Serializer,
    {
        if *value {
            return Err(serde::ser::Error::custom("expected false"));
        }
        value.serialize(serializer)
    }
}

/// Parameters for aborting the current turn
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AbortRequest {
    /// Finite reason code describing why the current turn was aborted
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reason: Option<AbortReason>,
}

/// Result of aborting the current turn
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AbortResult {
    /// Error message if the abort failed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Whether the abort completed successfully
    pub success: bool,
}

/// Canonical directory where custom agents can be discovered or created, with scope, preference, and optional project path.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentDiscoveryPath {
    /// Absolute path of the search/create directory (may not exist on disk yet)
    pub path: String,
    /// Whether this is the canonical directory to create a new agent in its tier. At most one entry per tier is preferred.
    pub preferred_for_creation: bool,
    /// The input project path this directory was derived from (only for project scope)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub project_path: Option<String>,
    /// Which tier this directory belongs to
    pub scope: AgentDiscoveryPathScope,
}

/// Canonical locations where custom agents can be created so the runtime will recognize them.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentDiscoveryPathList {
    /// Canonical agent create/discovery directories, in priority order
    pub paths: Vec<AgentDiscoveryPath>,
}

/// The currently selected custom agent, or null when using the default agent.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentGetCurrentResult {
    /// Currently selected custom agent, or null if using the default agent
    pub agent: AgentInfo,
}

/// Agents available to the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentList {
    /// Available agents
    pub agents: Vec<AgentInfo>,
}

/// Controls whether built-in agents and authored prompt text are included.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentListRequest {
    /// When true, request the session's configured built-in agents alongside custom agents. Listing applies feature, context, inclusion, exclusion, and user-disabled-agent policy, but does not evaluate transient invocation requirements such as model availability. Built-in metadata may be omitted when the session cannot project it, such as a relay session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub include_built_in_agents: Option<bool>,
    /// When true, request authored base prompt text on each AgentInfo. Prompt text may be omitted when unavailable, such as for agents projected through a relay session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub include_prompt: Option<bool>,
}

/// A filesystem error, with its original platform code.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentRegistryError {
    /// The platform error code.
    pub code: String,
    /// The filesystem error detail.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub message: Option<String>,
    /// The affected path.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub path: Option<String>,
}

/// The resolved registry directory and any creation failure.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentRegistryEnsureDirResult {
    /// The creation failure, if any.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<AgentRegistryError>,
    /// The resolved directory, also present on failure.
    pub path: String,
}

/// Options for local agent discovery.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentRegistryListEntriesRequest {
    /// Explicit Copilot home, or absent to use the home directory.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub copilot_home: Option<String>,
    /// A publisher to exclude.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub exclude_pid: Option<i64>,
    /// The caller's home directory.
    pub home_dir: String,
    /// Override the current epoch time for deterministic discovery.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub now_ms: Option<f64>,
    /// Override process liveness checks.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub pid_alive_override: Option<bool>,
    /// Maximum heartbeat age in milliseconds.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub stale_ms: Option<f64>,
}

/// Full registry entry for the spawned child. Lets the controller call `handleLiveTargetSelected(entry)` directly without re-reading the registry (avoids a TOCTOU window).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentRegistryLiveTargetEntry {
    /// Kind of attention required when status === "attention". Meaningful only when status === "attention".
    #[serde(skip_serializing_if = "Option::is_none")]
    pub attention_kind: Option<AgentRegistryLiveTargetEntryAttentionKind>,
    /// Git branch of the session (when known)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub branch: Option<String>,
    /// Copilot CLI version that wrote the entry
    pub copilot_version: String,
    /// Working directory of the session (when known)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cwd: Option<String>,
    /// Bind host for the entry's JSON-RPC server
    pub host: String,
    /// Process kind tag for the registry entry
    pub kind: AgentRegistryLiveTargetEntryKind,
    /// Wall-clock milliseconds since the watcher last observed this entry (heartbeat freshness)
    pub last_seen_ms: i64,
    /// How the most recent turn ended (clean vs aborted). Lets the renderer distinguish done from done_cancelled.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub last_terminal_event: Option<AgentRegistryLiveTargetEntryLastTerminalEvent>,
    /// Model identifier currently selected for the session
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model: Option<String>,
    /// Operating-system pid of the process owning this entry
    pub pid: i64,
    /// TCP port the entry's JSON-RPC server is listening on
    pub port: i64,
    /// Registry entry schema version (1 = ui-server, 2 = managed-server)
    pub schema_version: i64,
    /// Session ID of the foreground session for this entry
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_id: Option<SessionId>,
    /// Friendly session name (when set)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_name: Option<String>,
    /// ISO 8601 timestamp captured at registration
    pub started_at: String,
    /// Coarse lifecycle status of the foreground session
    #[serde(skip_serializing_if = "Option::is_none")]
    pub status: Option<AgentRegistryLiveTargetEntryStatus>,
    /// Monotonic per-publisher revision counter incremented on every status update. Lets watchers detect transient flips.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub status_revision: Option<i64>,
    /// Connection token (null when the target is unauthenticated)
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) token: Option<String>,
}

/// Live agent metadata without connection credentials.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentRegistryListEntriesResult {
    /// The live local agents.
    pub entries: Vec<AgentRegistryLiveTargetEntry>,
    /// The discovery failure, if any.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<AgentRegistryError>,
}

/// The location of a local agent registry.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentRegistryLocationRequest {
    /// Explicit Copilot home, or absent to use the home directory.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub copilot_home: Option<String>,
    /// The caller's home directory.
    pub home_dir: String,
}

/// Canonical process-log discovery outcome; populated from spawnLiveTarget.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentRegistryLogCapture {
    /// Whether a canonical process log was discovered for this managed spawn
    pub enabled: bool,
    /// Why no canonical process log could be opened for this managed spawn (set only when enabled is false)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub open_error: Option<String>,
    /// Categorized reason no canonical process log could be opened (set only when enabled is false)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub open_error_reason: Option<AgentRegistryLogCaptureOpenErrorReason>,
    /// Absolute path to the managed spawn's process-`<timestamp>`-`<pid>`.log file (only set when enabled)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub path: Option<String>,
}

/// A local agent entry identified by its publisher.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentRegistryPidRequest {
    /// Explicit Copilot home, or absent to use the home directory.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub copilot_home: Option<String>,
    /// The caller's home directory.
    pub home_dir: String,
    /// The publisher's process identifier.
    pub pid: i64,
}

/// The outcome of removing a registry entry, with no success payload.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentRegistryRemoveEntryResult {
    /// The operation failure, if any.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<AgentRegistryError>,
}

/// `child_process.spawn` itself failed before the child entered the registry.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentRegistrySpawnError {
    /// Underlying errno code (e.g. ENOENT, EACCES) when available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub code: Option<String>,
    /// Discriminator: child_process.spawn itself failed
    pub kind: AgentRegistrySpawnErrorKind,
    /// Human-readable error message
    pub message: String,
}

/// Spawn succeeded but the child did not publish a matching managed-server entry within the timeout.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentRegistrySpawnRegistryTimeout {
    /// Process ID of the orphaned child (so the caller can offer 'kill the pid' guidance)
    pub child_pid: i64,
    /// Discriminator: spawn succeeded but child never registered
    pub kind: AgentRegistrySpawnRegistryTimeoutKind,
    /// Per-spawn log-capture outcome; populated from spawnLiveTarget.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub log_capture: Option<AgentRegistryLogCapture>,
}

/// Inputs to spawn a managed-server child via the controller's spawn delegate.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentRegistrySpawnRequest {
    /// Custom or built-in agent name (e.g. 'explore'). When omitted, the child uses its own default.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub agent_name: Option<String>,
    /// Working directory for the spawned child (must be an existing directory)
    pub cwd: String,
    /// Optional first user message. Forwarded to the caller (the CLI's spawn wrapper sends it post-attach via the standard LocalRpcSession.send path).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub initial_prompt: Option<String>,
    /// Model identifier to apply to the new session
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model: Option<String>,
    /// Friendly session name. Must satisfy validateSessionName: non-empty, no leading/trailing whitespace, <=100 chars, no control chars, no double quotes.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    /// Permission posture for the new session. 'yolo' requires the controller-local session to currently be in allow-all mode.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub permission_mode: Option<AgentRegistrySpawnPermissionMode>,
}

/// Managed-server child was spawned and registered successfully.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentRegistrySpawnSpawned {
    /// Full registry entry for the spawned child. Lets the controller call `handleLiveTargetSelected(entry)` directly without re-reading the registry (avoids a TOCTOU window).
    pub entry: AgentRegistryLiveTargetEntry,
    /// If the delegate attempted to send the initial prompt and failed, the categorized error message.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub initial_prompt_error: Option<String>,
    /// Whether the delegate already sent the initial prompt. Always omitted in the current wiring: the controller sends the prompt post-attach via the standard LocalRpcSession.send path.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub initial_prompt_sent: Option<bool>,
    /// Discriminator: managed-server child spawned successfully
    pub kind: AgentRegistrySpawnSpawnedKind,
    /// Per-spawn log-capture outcome; populated from spawnLiveTarget.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub log_capture: Option<AgentRegistryLogCapture>,
}

/// Synchronous pre-validation rejected the spawn request.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentRegistrySpawnValidationError {
    /// Which parameter field was invalid. Omitted when the rejection is not field-specific.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub field: Option<AgentRegistrySpawnValidationErrorField>,
    /// Discriminator: synchronous pre-validation rejected the request
    pub kind: AgentRegistrySpawnValidationErrorKind,
    /// Human-readable explanation; safe to surface in the UI banner. Never logged to unrestricted telemetry.
    pub message: String,
    /// Categorized reason for the rejection. Low-cardinality enum so telemetry can aggregate by reason without leaking raw paths or agent/model names.
    pub reason: AgentRegistrySpawnValidationErrorReason,
}

/// Custom agents available to the session after reloading definitions from disk.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentReloadResult {
    /// Reloaded custom agents
    pub agents: Vec<AgentInfo>,
}

/// The models a custom agent asks for, and the models actually available.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct AgentsCustomAgentInitialModelDecisionParams {
    /// The agent's declared `model:` entry, serialized. A single name or an ordered list of acceptable names.
    pub agent_models_json: String,
    /// The models available to this session, serialized in the shape the model list carries.
    pub available_models_json: String,
}

/// The model to switch to, and the warning to show when the agent's preference could not be met.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct AgentsCustomAgentInitialModelDecisionResult {
    /// The reasoning effort attached to the selected model preference. Absent when that preference does not specify an effort.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_effort: Option<String>,
    /// The first available model that matches the agent's preferences. Absent when none of the requested models is available.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub target_model: Option<String>,
    /// What to tell the user about an unmet preference. Absent when the preference was met. A warning with no `targetModel` means the agent's models are all unavailable.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub warning: Option<String>,
}

/// Optional project paths to include in agent discovery.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentsDiscoverRequest {
    /// When true, omit the host's agents (the user-level agent directory and all plugin agents), leaving only project and remote agents. For multitenant deployments.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub exclude_host_agents: Option<bool>,
    /// Optional list of project directory paths to scan for project-scoped agents. When omitted or empty, only user/plugin/remote-independent agents are returned (no project scan).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub project_paths: Option<Vec<String>>,
}

/// Extensible [`AgentsDiscoverRequest`], including inputs added after it was published.
///
/// Required inputs are [`AgentsDiscoverOptions::new`] arguments; optional inputs have fluent setters.
/// Input-only: it serialises to the flat wire request and is not deserialisable.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentsDiscoverOptions {
    #[serde(flatten)]
    legacy: AgentsDiscoverRequest,
    #[serde(skip_serializing_if = "Option::is_none")]
    agent_directories: Option<Vec<String>>,
}

impl AgentsDiscoverOptions {
    /// Creates options with the required inputs.
    pub fn new() -> Self {
        Self {
            legacy: AgentsDiscoverRequest {
                project_paths: None,
                exclude_host_agents: None,
            },
            agent_directories: None,
        }
    }

    /// Optional list of project directory paths to scan for project-scoped agents. When omitted or empty, only user/plugin/remote-independent agents are returned (no project scan).
    pub fn project_paths(mut self, value: Vec<String>) -> Self {
        self.legacy.project_paths = Some(value);
        self
    }

    /// When true, omit the host's agents (the user-level agent directory and all plugin agents), leaving only project and remote agents. For multitenant deployments.
    pub fn exclude_host_agents(mut self, value: bool) -> Self {
        self.legacy.exclude_host_agents = Some(value);
        self
    }

    /// Explicit discovery-only agent roots, separate from customAgents and filesystem grants. Available without ambient discovery, subject to managed/source disables. Relative paths resolve against the request's effective working directory when supplied. Null and omission are equivalent; an empty array contributes no explicit roots.
    pub fn agent_directories(mut self, value: Vec<String>) -> Self {
        self.agent_directories = Some(value);
        self
    }
}

impl Default for AgentsDiscoverOptions {
    fn default() -> Self {
        Self::new()
    }
}

/// Name of the custom agent to select for subsequent turns.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentSelectRequest {
    /// Name of the custom agent to select
    pub name: String,
}

/// The newly selected custom agent.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentSelectResult {
    /// The newly selected custom agent
    pub agent: AgentInfo,
}

/// An in-memory authored prompt override for an available agent.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentSetPromptRequest {
    /// Stable effective agent id. Plugin namespace separators are normalized.
    pub id: String,
    /// Replacement authored prompt. Empty text is valid.
    pub prompt: String,
}

/// The feature flags to evaluate shipped agents against.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct AgentsGetAvailableBuiltinsRequest {
    /// The surface asking, which gates agents that only apply to one client. Omit or pass null to apply no client filter.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context: Option<String>,
    /// Feature flag values keyed by name, evaluated with the runtime's truthiness rules. Omit or pass null for no flags.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub feature_flags: Option<HashMap<String, serde_json::Value>>,
    /// Flag overrides keyed by name. A null entry uses the corresponding base flag; false explicitly disables it. Omit or pass null for no overrides.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub overrides: Option<HashMap<String, serde_json::Value>>,
}

/// A shipped agent, named and described.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct BuiltinAgentSummary {
    /// One-line description of what the agent does.
    pub description: String,
    /// The agent name, as it appears in `getBuiltins`.
    pub name: String,
}

/// The shipped agents available under the requested flags.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct AgentsGetAvailableBuiltinsResult {
    /// Available shipped agents, in the runtime's own order.
    #[doc(hidden)]
    pub(crate) agents: Vec<BuiltinAgentSummary>,
}

/// The shipped agent whose definition to load.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct AgentsGetBuiltinDefinitionRequest {
    /// The agent name, which must be one of `getBuiltins`'s `yamlBasedNames`. A name outside that list is special-cased in code and has no definition, and is reported as an error rather than as an empty definition.
    pub name: String,
}

/// One shipped agent's definition.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct AgentsGetBuiltinDefinitionResult {
    /// The agent's definition, serialized as JSON. It carries the authored keys plus the runtime's projected `__nativeCustomAgent` view of the same agent. It is a string rather than an object because the runtime parses it with the agent schema's tolerant shape, which accepts keys this contract does not name.
    pub definition_json: String,
}

/// The shipped agent whose listing entry to load.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct AgentsGetBuiltinListingDefinitionRequest {
    /// The agent name, taken from `getAvailableBuiltins`. Unlike `getBuiltinDefinition`, the agent that `getBuiltins` reports as special-cased rather than YAML-based is answered here too, from its in-code definition.
    pub name: String,
}

/// One shipped agent, projected for a listing.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct AgentsGetBuiltinListingDefinitionResult {
    /// The agent projected as a custom agent, serialized as JSON. It is a string rather than an object for the same reason as `getBuiltinDefinition`: the runtime parses the underlying definition with the agent schema's tolerant shape, which accepts keys this contract does not name.
    pub definition_json: String,
}

/// The agents this runtime ships, named so a consumer can tell them apart from authored ones.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct AgentsGetBuiltinsResult {
    /// The subset of `names` a user is allowed to turn off. A shipped agent outside this list is always active and a client should not offer a toggle for it.
    pub disableable_names: Vec<String>,
    /// Every agent name this runtime ships.
    pub names: Vec<String>,
    /// The subset of `names` defined by a shipped YAML definition. The remainder are special-cased in code and have no definition to load.
    pub yaml_based_names: Vec<String>,
}

/// Optional project paths to include when enumerating agent discovery directories.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentsGetDiscoveryPathsRequest {
    /// When true, omit the host's user-level agent directory, leaving only project directories. For multitenant deployments (mirrors `discover`'s `excludeHostAgents`).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub exclude_host_agents: Option<bool>,
    /// Optional list of project directory paths. When omitted or empty, only the user-level directory is returned.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub project_paths: Option<Vec<String>>,
}

/// Extensible [`AgentsGetDiscoveryPathsRequest`], including inputs added after it was published.
///
/// Required inputs are [`AgentsGetDiscoveryPathsOptions::new`] arguments; optional inputs have fluent setters.
/// Input-only: it serialises to the flat wire request and is not deserialisable.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentsGetDiscoveryPathsOptions {
    #[serde(flatten)]
    legacy: AgentsGetDiscoveryPathsRequest,
    #[serde(skip_serializing_if = "Option::is_none")]
    agent_directories: Option<Vec<String>>,
}

impl AgentsGetDiscoveryPathsOptions {
    /// Creates options with the required inputs.
    pub fn new() -> Self {
        Self {
            legacy: AgentsGetDiscoveryPathsRequest {
                project_paths: None,
                exclude_host_agents: None,
            },
            agent_directories: None,
        }
    }

    /// Optional list of project directory paths. When omitted or empty, only the user-level directory is returned.
    pub fn project_paths(mut self, value: Vec<String>) -> Self {
        self.legacy.project_paths = Some(value);
        self
    }

    /// When true, omit the host's user-level agent directory, leaving only project directories. For multitenant deployments (mirrors `discover`'s `excludeHostAgents`).
    pub fn exclude_host_agents(mut self, value: bool) -> Self {
        self.legacy.exclude_host_agents = Some(value);
        self
    }

    /// Explicit discovery-only agent roots to enumerate, including configured missing directories. Relative paths resolve against the request's effective working directory when supplied. These are not filesystem grants or preferred creation targets. Null and omission are equivalent.
    pub fn agent_directories(mut self, value: Vec<String>) -> Self {
        self.agent_directories = Some(value);
        self
    }
}

impl Default for AgentsGetDiscoveryPathsOptions {
    fn default() -> Self {
        Self::new()
    }
}

/// Blob attachment with inline base64-encoded data
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AttachmentBlob {
    /// Internal: content-addressed id of the session.binary_asset event holding this attachment's model-facing bytes (e.g. "sha256:..."). Absent externally.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub asset_id: Option<String>,
    /// Internal: decoded byte length of the attachment's model-facing bytes. Absent externally.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub byte_length: Option<i64>,
    /// Base64-encoded content. Present on input and for external consumers; replaced by an internal `assetId` reference in persisted events when interned to a content-addressed asset.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub data: Option<String>,
    /// User-facing display name for the attachment
    #[serde(skip_serializing_if = "Option::is_none")]
    pub display_name: Option<String>,
    /// MIME type of the inline data
    pub mime_type: String,
    /// Internal: why model-facing bytes are absent from persistence. Absent externally.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub omitted_reason: Option<OmittedBinaryOmittedReason>,
    /// Attachment type discriminator
    pub r#type: AttachmentBlobType,
}

/// Directory attachment
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AttachmentDirectory {
    /// User-facing display name for the attachment
    pub display_name: String,
    /// Absolute directory path
    pub path: String,
    /// Frozen rendered line this attachment contributed to the `<tagged_files>` prompt block (e.g. "* /path (12 items)"). Captured at send time so resumed history reproduces the exact text the model saw, independent of later filesystem changes.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tagged_files_entry: Option<String>,
    /// Attachment type discriminator
    pub r#type: AttachmentDirectoryType,
}

/// Structured context contributed by an extension. Composer pills displayed in the host are forwarded back through session.send.attachments, then rendered into the model prompt as an <extension_context> XML block.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AttachmentExtensionContext {
    /// Provider-local canvas identifier when the push was bound to a canvas instance
    #[serde(skip_serializing_if = "Option::is_none")]
    pub canvas_id: Option<String>,
    /// ISO 8601 timestamp captured by the runtime when the push was accepted
    pub captured_at: String,
    /// Owning extension identifier. Runtime-derived from the caller's connection when produced via session.extensions.sendAttachmentsToMessage; preserved verbatim on subsequent transports.
    pub extension_id: String,
    /// Open canvas instance identifier when the push was bound to a canvas instance
    #[serde(skip_serializing_if = "Option::is_none")]
    pub instance_id: Option<String>,
    /// Caller-supplied JSON payload
    #[serde(skip_serializing_if = "Option::is_none")]
    pub payload: Option<serde_json::Value>,
    /// Human-readable composer pill label
    pub title: String,
    /// Attachment type discriminator
    pub r#type: AttachmentExtensionContextType,
}

/// Optional line range to scope the attachment to a specific section of the file
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AttachmentFileLineRange {
    /// End line number (1-based, inclusive)
    pub end: i64,
    /// Start line number (1-based)
    pub start: i64,
}

/// File attachment
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AttachmentFile {
    /// Internal: content-addressed id of the session.binary_asset event holding this attachment's model-facing bytes (e.g. "sha256:..."). Absent externally.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub asset_id: Option<String>,
    /// Internal: decoded byte length of the attachment's model-facing bytes. Absent externally.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub byte_length: Option<i64>,
    /// User-facing display name for the attachment
    pub display_name: String,
    /// Optional line range to scope the attachment to a specific section of the file
    #[serde(skip_serializing_if = "Option::is_none")]
    pub line_range: Option<AttachmentFileLineRange>,
    /// Internal: MIME type of the file's model-facing bytes (post-resize for images). Set when the file's bytes are interned to an asset. Absent externally.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mime_type: Option<String>,
    /// Internal: why model-facing bytes are absent from persistence. Absent externally.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub omitted_reason: Option<OmittedBinaryOmittedReason>,
    /// Absolute file path
    pub path: String,
    /// Frozen rendered line this attachment contributed to the `<tagged_files>` prompt block (e.g. "* /path (123 lines)"). Captured at send time so resumed history reproduces the exact text the model saw, independent of later filesystem changes. Present only for attachments routed to `<tagged_files>` (mutually exclusive with assetId, which marks bytes sent natively).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tagged_files_entry: Option<String>,
    /// Attachment type discriminator
    pub r#type: AttachmentFileType,
}

/// Pointer to a GitHub Actions job.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AttachmentGitHubActionsJob {
    /// Terminal conclusion of the job when finished (e.g., success, failure, cancelled). Absent for in-progress jobs.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub conclusion: Option<String>,
    /// Job id within the workflow run
    pub job_id: i64,
    /// Display name of the job
    pub job_name: String,
    /// Repository the workflow run belongs to
    pub repo: GitHubRepoRef,
    /// Attachment type discriminator
    pub r#type: AttachmentGitHubActionsJobType,
    /// URL to the job on GitHub
    pub url: String,
    /// Display name of the workflow the job ran in
    pub workflow_name: String,
}

/// Pointer to a GitHub commit.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AttachmentGitHubCommit {
    /// First line of the commit message
    pub message: String,
    /// Full commit SHA
    pub oid: String,
    /// Repository the commit belongs to
    pub repo: GitHubRepoRef,
    /// Attachment type discriminator
    pub r#type: AttachmentGitHubCommitType,
    /// URL to the commit on GitHub
    pub url: String,
}

/// Pointer to a file in a GitHub repository at a specific ref.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AttachmentGitHubFile {
    /// Repository-relative path to the file
    pub path: String,
    /// Git ref the file is read at (branch, tag, or commit SHA)
    pub r#ref: String,
    /// Repository the file lives in
    pub repo: GitHubRepoRef,
    /// Attachment type discriminator
    pub r#type: AttachmentGitHubFileType,
    /// URL to the file on GitHub
    pub url: String,
}

/// One side of a file diff (head or base)
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AttachmentGitHubFileDiffSide {
    /// Repository-relative path to the file
    pub path: String,
    /// Git ref (branch, tag, or commit SHA) the file is read at
    pub r#ref: String,
    /// Repository the file lives in
    pub repo: GitHubRepoRef,
}

/// Pointer to a single-file diff. At least one of `head` and `base` must be present.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AttachmentGitHubFileDiff {
    /// File location on the base side of the diff. Absent for additions.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub base: Option<AttachmentGitHubFileDiffSide>,
    /// File location on the head side of the diff. Absent for deletions.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub head: Option<AttachmentGitHubFileDiffSide>,
    /// Attachment type discriminator
    pub r#type: AttachmentGitHubFileDiffType,
    /// URL to the diff on GitHub (e.g., a commit, compare, or PR-file URL)
    pub url: String,
}

/// GitHub issue, pull request, or discussion reference
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AttachmentGitHubReference {
    /// Issue, pull request, or discussion number
    pub number: i64,
    /// Type of GitHub reference
    pub reference_type: AttachmentGitHubReferenceType,
    /// Current state of the referenced item (e.g., open, closed, merged)
    pub state: String,
    /// Title of the referenced item
    pub title: String,
    /// Attachment type discriminator
    pub r#type: AttachmentGitHubReferenceType,
    /// URL to the referenced item on GitHub
    pub url: String,
}

/// Pointer to a GitHub release.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AttachmentGitHubRelease {
    /// Human-readable release name
    pub name: String,
    /// Repository the release belongs to
    pub repo: GitHubRepoRef,
    /// Git tag the release is anchored to
    pub tag_name: String,
    /// Attachment type discriminator
    pub r#type: AttachmentGitHubReleaseType,
    /// URL to the release on GitHub
    pub url: String,
}

/// Pointer to a GitHub repository.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AttachmentGitHubRepository {
    /// Short description of the repository
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Git ref this attachment is anchored at (branch, tag, or commit). When absent the default branch is implied.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub r#ref: Option<String>,
    /// Repository pointer
    pub repo: GitHubRepoRef,
    /// Attachment type discriminator
    pub r#type: AttachmentGitHubRepositoryType,
    /// URL to the repository on GitHub
    pub url: String,
}

/// Pointer to a line range inside a file in a GitHub repository.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AttachmentGitHubSnippet {
    /// Line range the snippet covers
    pub line_range: AttachmentFileLineRange,
    /// Repository-relative path to the file
    pub path: String,
    /// Git ref the file is read at (branch, tag, or commit SHA)
    pub r#ref: String,
    /// Repository the file lives in
    pub repo: GitHubRepoRef,
    /// Attachment type discriminator
    pub r#type: AttachmentGitHubSnippetType,
    /// URL to the snippet on GitHub (with line anchor)
    pub url: String,
}

/// One side of a tree comparison (head or base)
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AttachmentGitHubTreeComparisonSide {
    /// Repository the revision belongs to
    pub repo: GitHubRepoRef,
    /// Git revision (branch, tag, or commit SHA)
    pub revision: String,
}

/// Pointer to a comparison between two git revisions.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AttachmentGitHubTreeComparison {
    /// Base side of the comparison
    pub base: AttachmentGitHubTreeComparisonSide,
    /// Head side of the comparison
    pub head: AttachmentGitHubTreeComparisonSide,
    /// Attachment type discriminator
    pub r#type: AttachmentGitHubTreeComparisonType,
    /// URL to the comparison on GitHub
    pub url: String,
}

/// Generic GitHub URL reference.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AttachmentGitHubUrl {
    /// Attachment type discriminator
    pub r#type: AttachmentGitHubUrlType,
    /// URL to the GitHub resource
    pub url: String,
}

/// End position of the selection
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AttachmentSelectionDetailsEnd {
    /// End character offset within the line (0-based)
    pub character: i64,
    /// End line number (0-based)
    pub line: i64,
}

/// Start position of the selection
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AttachmentSelectionDetailsStart {
    /// Start character offset within the line (0-based)
    pub character: i64,
    /// Start line number (0-based)
    pub line: i64,
}

/// Position range of the selection within the file
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AttachmentSelectionDetails {
    /// End position of the selection
    pub end: AttachmentSelectionDetailsEnd,
    /// Start position of the selection
    pub start: AttachmentSelectionDetailsStart,
}

/// Code selection attachment from an editor
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AttachmentSelection {
    /// User-facing display name for the selection
    pub display_name: String,
    /// Absolute path to the file containing the selection
    pub file_path: String,
    /// Position range of the selection within the file
    pub selection: AttachmentSelectionDetails,
    /// The selected text content
    pub text: String,
    /// Attachment type discriminator
    pub r#type: AttachmentSelectionType,
}

/// Credential-free identity metadata.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AuthIdentityMetadata {
    /// Identity host.
    pub host: String,
    /// User login.
    pub login: String,
    /// Authentication type.
    pub r#type: AuthInfoType,
}

/// Result of applying the ordinary session-idle Autopilot continuation decision.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AutopilotEvaluateIdleContinuationResult {
    /// Applied continuation outcome.
    pub outcome: AutopilotEvaluateIdleContinuationOutcome,
}

/// Canonical runtime state for the session's current autopilot objective.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AutopilotObjectiveGetStateResult {
    /// Current objective state, or `null` when the session has no objective.
    pub state: Option<AutopilotObjectiveState>,
}

/// Lists models eligible for local use before a chat session exists.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AzureMlCatalogRequest {
    /// Ask for the staff registry; the server also verifies its own authenticated identity.
    pub include_dev: bool,
    /// An explicit user refresh retries a cached sign-in failure.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub retry_mode: Option<AzureMlCatalogRetryMode>,
}

/// License published with an eligible Azure ML model.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AzureMlLicense {
    /// Bounded display label for the model license.
    pub name: String,
}

/// Validated local runtime requirements for an Azure ML model.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AzureMlProfile {
    /// Sanitized model-picker display label.
    pub display_name: String,
    /// Published download size, when available.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub download_bytes: Option<i64>,
    /// Published license, when available.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub license: Option<AzureMlLicense>,
    /// Maximum context window accepted by the model.
    pub max_context_window_tokens: i64,
    /// Maximum output tokens produced by the model.
    pub max_output_tokens: i64,
    /// Maximum input tokens accepted by the model.
    pub max_prompt_tokens: i64,
    /// Peak disk use during installation, when available.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub peak_disk_bytes: Option<i64>,
    /// Recommended graphics memory, when available.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub recommended_vram_bytes: Option<i64>,
    /// Validated local runtime configuration.
    pub serve_options: serde_json::Value,
    /// Whether the model supports tool calls.
    pub supports_tool_calls: bool,
}

/// Eligible Azure ML model version and its provider metadata.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AzureMlModel {
    /// Provider metadata retained for registration.
    pub metadata: serde_json::Value,
    /// Stable model-family name.
    pub name: String,
    /// Validated Windows ML requirements.
    pub profile: AzureMlProfile,
    /// Registry that owns this model version.
    pub registry: AzureMlRegistry,
    /// Exact published model version.
    pub version: String,
}

/// Redacted warning for a skipped model or registry.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AzureMlCatalogWarning {
    /// Safe reason without signed access details.
    pub kind: String,
    /// Model family, or `catalog` for registry-wide errors.
    pub model_name: String,
    /// Registry associated with the warning.
    pub registry: AzureMlRegistry,
    /// Version, if one was available.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub version: Option<String>,
}

/// Eligible models and redacted validation warnings.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AzureMlCatalogResult {
    /// Model versions suitable for local use.
    pub models: Vec<AzureMlModel>,
    /// Skipped model and catalog errors without sensitive access details.
    pub warnings: Vec<AzureMlCatalogWarning>,
}

/// Selects an inactive Azure ML version for cleanup.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AzureMlDiscardRequest {
    /// Version directory under this server's Azure ML library.
    pub destination: String,
    /// Optional connection-owned lease that spans the caller's complete family mutation.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub lease_id: Option<String>,
}

/// Selects an active connection-owned download.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AzureMlDownloadIdRequest {
    /// Identifier supplied when this connection started the download.
    pub request_id: RequestId,
}

/// Downloads a model version to its configured local library.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AzureMlDownloadRequest {
    /// Version directory under this server's Azure ML library.
    pub destination: String,
    /// Optional connection-owned lease that spans the caller's complete family mutation.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub lease_id: Option<String>,
    /// Exact model-family name.
    pub name: String,
    /// Registry that owns the model.
    pub registry: AzureMlRegistry,
    /// Unique request identifier for progress and explicit cancellation.
    pub request_id: RequestId,
    /// Exact published version.
    pub version: String,
}

/// Completed and verified Azure ML model files.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AzureMlDownloadResult {
    /// Installed companion file paths.
    pub extra_files: Vec<String>,
    /// Published manifest path.
    pub manifest_path: String,
    /// Maximum supported local concurrency.
    pub max_concurrent: i64,
    /// Installed GGUF path.
    pub primary_path: String,
    /// Bytes received over the network during this run, including discarded bytes.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub received_bytes: Option<i64>,
    /// Whether a partial from a prior run continued after the server accepted its range.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub resumed: Option<bool>,
    /// Whether all prior verified files were reused.
    pub reused: bool,
    /// Validated local runtime settings.
    pub serve_options: serde_json::Value,
    /// Exact bytes across the model version.
    pub total_bytes: i64,
    /// Transient transfer failures retried during this run.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub total_failures: Option<i64>,
}

/// Progress and retry facts for one active download.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AzureMlDownloadStatus {
    /// Bytes completed across all files.
    pub completed_bytes: i64,
    /// Configured retry limit, if the transfer is retrying.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_attempts: Option<i64>,
    /// Current transfer phase.
    pub phase: String,
    /// Next retry attempt, if the transfer is retrying.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub retry_attempt: Option<i64>,
    /// Planned delay before retry, if the transfer is retrying.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub retry_delay_ms: Option<i64>,
    /// Redacted failure reason, if the transfer is retrying.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub retry_reason: Option<String>,
    /// Exact bytes expected across all files.
    pub total_bytes: i64,
}

/// Opaque handle to a connection-owned family mutation interval.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AzureMlFamilyLeaseResult {
    /// Server-generated handle, valid only on the originating SDK connection.
    pub lease_id: String,
}

/// Releases a family mutation interval owned by this SDK connection.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AzureMlFamilyReleaseRequest {
    /// Server-generated handle from `azureMl.acquireFamily`.
    pub lease_id: String,
    /// Exact model-family name.
    pub name: String,
    /// Registry that owns the acquired family.
    pub registry: AzureMlRegistry,
}

/// Identifies a family in the server's Azure ML model library.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AzureMlFamilyRequest {
    /// Exact model-family name.
    pub name: String,
    /// Registry that owns the model family.
    pub registry: AzureMlRegistry,
}

/// Selects an Azure ML model family.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AzureMlLatestRequest {
    /// Exact model-family name.
    pub name: String,
    /// Registry that owns the model.
    pub registry: AzureMlRegistry,
}

/// Selects an immutable model version.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AzureMlResolveRequest {
    /// Exact model-family name.
    pub name: String,
    /// Registry that owns the model.
    pub registry: AzureMlRegistry,
    /// Exact published version.
    pub version: String,
}

/// Resolved artifact facts without signed download URLs.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AzureMlResolveResult {
    /// Other paths relative to the version directory.
    pub extra_files: Vec<String>,
    /// Maximum supported local concurrency.
    pub max_concurrent: i64,
    /// GGUF path relative to the version directory.
    pub primary_path: String,
    /// Validated local runtime settings.
    pub serve_options: serde_json::Value,
    /// Exact bytes across all files in this version.
    pub total_bytes: i64,
}

/// The running runtime's complete catalog of well-known built-in model IDs, including supported models and additional IDs with built-in metadata.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct BuiltInModelCatalog {
    /// Built-in model entries.
    pub models: Vec<BuiltInModelCatalogEntry>,
}

/// Per-field telemetry-safety policy for a built-in tool.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct BuiltinToolSafeTelemetryFields {
    /// Whether tool input names may be included in telemetry without obfuscation.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub inputs_names: Option<bool>,
    /// Whether the tool name may be included in telemetry without obfuscation.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<bool>,
}

/// Cancellation result for a user-requested shell command.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CancelUserRequestedShellCommandResult {
    /// Whether an in-flight execution was found and signalled to cancel
    pub cancelled: bool,
}

/// Agent Finder's verified materialisation of the plugin. Present only when the candidate carries a complete materialisation reference; the reference itself stays runtime-private.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogAgentPluginMaterializationSource {
    /// Discriminator: the plugin installs from Agent Finder's materialisation.
    #[serde(deserialize_with = "CatalogAgentPluginMaterializationSource::deserialize_kind")]
    pub kind: CatalogAgentPluginMaterializationSourceKind,
}

impl CatalogAgentPluginMaterializationSource {
    fn deserialize_kind<'de, D>(
        deserializer: D,
    ) -> Result<CatalogAgentPluginMaterializationSourceKind, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let value = String::deserialize(deserializer)?;
        if value != "materialization" {
            return Err(serde::de::Error::unknown_variant(
                &value,
                &["materialization"],
            ));
        }
        <CatalogAgentPluginMaterializationSourceKind>::deserialize(
            serde::de::value::StringDeserializer::<D::Error>::new(value),
        )
    }
}

/// A plugin marketplace named by verified catalogue install data, installed as `pluginName@name`.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogAgentPluginMarketplaceSource {
    /// Discriminator: the plugin installs from a marketplace.
    #[serde(deserialize_with = "CatalogAgentPluginMarketplaceSource::deserialize_kind")]
    pub kind: CatalogAgentPluginMarketplaceSourceKind,
    /// Marketplace name.
    pub name: String,
    /// Canonical lowercase GitHub owner/repository hosting the marketplace.
    pub repository: String,
}

impl CatalogAgentPluginMarketplaceSource {
    fn deserialize_kind<'de, D>(
        deserializer: D,
    ) -> Result<CatalogAgentPluginMarketplaceSourceKind, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let value = String::deserialize(deserializer)?;
        if value != "marketplace" {
            return Err(serde::de::Error::unknown_variant(&value, &["marketplace"]));
        }
        <CatalogAgentPluginMarketplaceSourceKind>::deserialize(
            serde::de::value::StringDeserializer::<D::Error>::new(value),
        )
    }
}

/// A GitHub repository holding the plugin, installed directly as `owner/repo[:path]` at the plan's commit, without a marketplace.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogAgentPluginRepositorySource {
    /// Discriminator: the plugin installs directly from a repository.
    #[serde(deserialize_with = "CatalogAgentPluginRepositorySource::deserialize_kind")]
    pub kind: CatalogAgentPluginRepositorySourceKind,
    /// Relative POSIX path of the plugin directory in the repository. Omitted when the plugin is at the repository root.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub path: Option<String>,
    /// Canonical lowercase GitHub owner/repository holding the plugin.
    pub repository: String,
}

impl CatalogAgentPluginRepositorySource {
    fn deserialize_kind<'de, D>(
        deserializer: D,
    ) -> Result<CatalogAgentPluginRepositorySourceKind, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let value = String::deserialize(deserializer)?;
        if value != "repository" {
            return Err(serde::de::Error::unknown_variant(&value, &["repository"]));
        }
        <CatalogAgentPluginRepositorySourceKind>::deserialize(
            serde::de::value::StringDeserializer::<D::Error>::new(value),
        )
    }
}

/// What installing an Agent Plugin would do, for the user to review and confirm. Provisional: the catalogue install data it is built from is not final.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogAgentPluginInstallPlan {
    /// Canvas IDs the plugin contributes. Non-empty exactly when components include `canvas`.
    pub canvas_ids: Vec<String>,
    /// Exact full commit ID to install, in lowercase hex. Always present when the plan has a repository source; a plan with a marketplace source carries at least one of version and commit.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub commit: Option<String>,
    /// Components the plugin contributes, in canonical order. Empty only in a materialisation-only plan, where the verified manifests list them in the plugins.planInstall review.
    pub components: Vec<CatalogAgentPluginComponent>,
    /// Connectors the plugin depends on.
    pub connector_dependencies: Vec<String>,
    /// Extension IDs the plugin contributes. Non-empty exactly when components include `extension`.
    pub extension_ids: Vec<String>,
    /// MCP servers the plugin depends on but does not bundle.
    pub mcp_dependencies: Vec<String>,
    /// The plugin's name, as named in its marketplace or manifest. Omitted from a materialisation-only plan, whose sources are only `materialization`: the verified manifests supply the name in the plugins.planInstall review.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub plugin_name: Option<String>,
    /// Every valid install source, in preference order: materialization, then marketplace, then repository. Each kind appears at most once.
    pub sources: Vec<CatalogAgentPluginInstallSource>,
    /// Exact plugin version to install. A plan with a marketplace source carries at least one of version and commit.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub version: Option<String>,
}

/// Where and when an Agent Plugin catalog reference was observed. Discovery provenance deliberately carries no descriptor URL, raw data, candidate handle, or content digest.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogAgentPluginCandidateProvenance {
    /// Host of the catalog authority that advertised the reference, without path, query, or credentials. Inert untrusted data.
    pub authority: String,
    /// Media type naming the plugin's format.
    pub media_type: CatalogAgentPluginMediaType,
    /// ISO 8601 timestamp at which the runtime observed the catalog reference. This is not a retrieval or validation timestamp.
    pub observed_at: String,
}

/// Syntactically validated GitHub repository provenance declared by catalog metadata. This is a source claim rather than proof that the descriptor URL resolves to the repository.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogPluginRepositorySource {
    /// Case-preserving safe relative POSIX path derived from metadata.repoPath. Absent when the catalog declared sourceSet without repoPath, meaning the plugin is at the repository root; only callers that require agent-plugin-confirmed-installation receive such a source.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub path: Option<String>,
    /// Canonical lowercase owner/repository name derived from metadata.sourceSet.
    pub repository: String,
}

/// An inert Agent Plugin catalog result. Its canonical catalog identity, declared version, repository source claim, and explicit compatibility tags are safe to correlate, while its descriptor, URL, raw data, materialisation reference, and installed-plugin state remain runtime-private. A handle is minted only under agent-plugin-confirmed-installation, for a candidate with a complete materialisation reference.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogAgentPluginCandidate {
    /// Category slugs reported by the feed, such as `build` or `review`, without repeats. Values are passed through, so clients must tolerate slugs they do not recognise. Inert untrusted text. Omitted when the feed reports none.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub categories: Option<Vec<String>>,
    /// Explicit validated compatibility tags, in canonical order. An empty list means the source declared no recognised compatibility; clients must not infer compatibility from other fields. `canvas-only` requires both `canvas` and `github-copilot`.
    pub compatibility_tags: Vec<CatalogAgentPluginCompatibilityTag>,
    /// Identifiers of the connectors the resource works with, as reported by the feed, without repeats. Inert untrusted text. Omitted when the feed reports none.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub connector_ids: Option<Vec<String>>,
    /// Description taken verbatim from the card. Inert untrusted text.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Display name taken verbatim from the card. Inert untrusted text.
    pub display_name: String,
    /// Opaque, runtime-instance scoped, TTL-bound, single-use handle for planning this plugin's installation through plugins.planInstall. Present only when the caller requires the agent-plugin-confirmed-installation capability and the candidate carries a complete Agent Finder materialisation reference, which stays runtime-private. Never logged.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub handle: Option<String>,
    /// ISO 8601 timestamp after which the handle is stale and will be rejected. Present exactly when handle is present.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub handle_expires_at: Option<String>,
    /// HTTPS URL of the resource's icon on raw.githubusercontent.com, as reported by the feed. Untrusted external content: load it without credentials. Omitted when absent or outside the feed's icon host policy.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub icon_url: Option<String>,
    /// Validated, normalised catalogue resource URN. This identity comes only from the catalog identifier and is never inferred from display text or installed-plugin state.
    pub identity: String,
    /// Whether this plugin can be installed in the selected session. Present only when the caller requires the agent-plugin-confirmed-installation capability.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub installability: Option<CatalogAgentPluginInstallability>,
    /// How many times the resource has been installed, as reported by the feed. Omitted when the feed reports no count or an invalid one.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub install_count: Option<i64>,
    /// Reviewable install plan built only from verified catalogue install data. Present exactly when installability is `installable`; the runtime never derives it from the repository source, display text, or identity.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub install_plan: Option<CatalogAgentPluginInstallPlan>,
    /// Discriminator: this candidate describes an Agent Plugin.
    #[serde(deserialize_with = "CatalogAgentPluginCandidate::deserialize_kind")]
    pub kind: CatalogAgentPluginCandidateKind,
    /// Media type naming the plugin's format.
    pub media_type: CatalogAgentPluginMediaType,
    /// Where the Agent Plugin catalog reference was observed, without its descriptor, URL, raw data, or content digest.
    pub provenance: CatalogAgentPluginCandidateProvenance,
    /// Publisher taken verbatim from the card. Inert untrusted text.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub publisher: Option<String>,
    /// Bounded repository provenance declared through the catalog's sourceSet and repoPath metadata. It contains no descriptor URL. Always present, with a path, unless the caller requires agent-plugin-confirmed-installation. Such a caller also receives a repository-root plugin (sourceSet without repoPath) whose source has no path, and a plugin with no usable declaration (neither key, or repoPath alone) and no source; the latter carries a complete materialisation reference and a handle, and the verified descriptor supplies the repository, commit and root when it is planned. Never derived from the catalogue URL.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source: Option<CatalogPluginRepositorySource>,
    /// Versioned trust metadata observed from the catalog authority. Optional for protocol-3 compatibility and emitted only when the caller also requires the trust-snapshot capability.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub trust: Option<serde_json::Value>,
    /// Optional version declared by the catalog source. Omitted rather than guessed when the source supplies no version.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub version: Option<String>,
}

impl CatalogAgentPluginCandidate {
    fn deserialize_kind<'de, D>(
        deserializer: D,
    ) -> Result<CatalogAgentPluginCandidateKind, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let value = String::deserialize(deserializer)?;
        if value != "plugin" {
            return Err(serde::de::Error::unknown_variant(&value, &["plugin"]));
        }
        <CatalogAgentPluginCandidateKind>::deserialize(serde::de::value::StringDeserializer::<
            D::Error,
        >::new(value))
    }
}

/// Where and when an AI skill catalog reference was observed. Discovery provenance deliberately carries no content digest because search does not establish the exact validated content a later plan will bind.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogAiSkillCandidateProvenance {
    /// Host of the catalog authority that advertised the reference, without path, query, or credentials. Inert untrusted data.
    pub authority: String,
    /// Media type advertised for the referenced AI skill card
    #[serde(deserialize_with = "CatalogAiSkillCandidateProvenance::deserialize_media_type")]
    pub media_type: CatalogAiSkillMediaType,
    /// ISO 8601 timestamp at which the runtime observed the catalog reference. This is not a retrieval or validation timestamp.
    pub observed_at: String,
}

impl CatalogAiSkillCandidateProvenance {
    fn deserialize_media_type<'de, D>(deserializer: D) -> Result<CatalogAiSkillMediaType, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let value = String::deserialize(deserializer)?;
        if value != "application/ai-skill" {
            return Err(serde::de::Error::unknown_variant(
                &value,
                &["application/ai-skill"],
            ));
        }
        <CatalogAiSkillMediaType>::deserialize(
            serde::de::value::StringDeserializer::<D::Error>::new(value),
        )
    }
}

/// Candidate whose card is retrieved from a URL through the runtime's hardened fetch boundary.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogCandidateSourceUrl {
    /// Discriminator: the card is URL-backed, and carries no embedded data
    pub kind: CatalogCandidateSourceUrlKind,
    /// Card URL as advertised. Inert untrusted data: the runtime retrieves it only through its own hardened boundary, and it is never logged.
    pub url: String,
}

/// Candidate whose card reference arrived inline. The document and its content-derived properties stay behind the runtime boundary.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogCandidateSourceEmbedded {
    /// Discriminator: the card is embedded, and carries no URL
    pub kind: CatalogCandidateSourceEmbeddedKind,
}

/// An inert AI skill catalog result. Verified Skill candidates may be installable only when the runtime reports installability and the selected session is permitted to plan installation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogAiSkillCandidate {
    /// Category slugs reported by the feed, such as `build` or `review`, without repeats. Values are passed through, so clients must tolerate slugs they do not recognise. Inert untrusted text. Omitted when the feed reports none.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub categories: Option<Vec<String>>,
    /// Identifiers of the connectors the resource works with, as reported by the feed, without repeats. Inert untrusted text. Omitted when the feed reports none.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub connector_ids: Option<Vec<String>>,
    /// Description taken verbatim from the card. Inert untrusted text.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Display name taken verbatim from the card. Inert untrusted text.
    pub display_name: String,
    /// Opaque, runtime-instance scoped, TTL-bound, single-use handle for this candidate. Carries no readable information and is rejected when stale, replayed, or presented to a different runtime instance. Never logged.
    pub handle: String,
    /// ISO 8601 timestamp after which the handle is stale and will be rejected.
    pub handle_expires_at: String,
    /// HTTPS URL of the resource's icon on raw.githubusercontent.com, as reported by the feed. Untrusted external content: load it without credentials. Omitted when absent or outside the feed's icon host policy.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub icon_url: Option<String>,
    /// Whether this AI skill candidate can be planned for verified installation in the selected session.
    pub installability: CatalogAiSkillInstallability,
    /// How many times the resource has been installed, as reported by the feed. Omitted when the feed reports no count or an invalid one.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub install_count: Option<i64>,
    /// Discriminator: this candidate describes an AI skill
    #[serde(deserialize_with = "CatalogAiSkillCandidate::deserialize_kind")]
    pub kind: CatalogAiSkillCandidateKind,
    /// Media type of the underlying AI skill card
    #[serde(deserialize_with = "CatalogAiSkillCandidate::deserialize_media_type")]
    pub media_type: CatalogAiSkillMediaType,
    /// Where the catalog reference was observed, without the card itself or any content digest.
    pub provenance: CatalogAiSkillCandidateProvenance,
    /// Publisher taken verbatim from the card. Inert untrusted text.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub publisher: Option<String>,
    /// Where the card came from: exactly one of a URL or embedded data, encoded as a tagged union so neither both nor neither can be represented.
    pub source: CatalogCandidateSource,
    /// Versioned trust metadata observed from the catalog authority. Optional for protocol-3 compatibility with runtimes that predate trust snapshots. A trust-capable runtime emits an explicit snapshot even when the authority omitted or malformed its trust field.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub trust: Option<serde_json::Value>,
}

impl CatalogAiSkillCandidate {
    fn deserialize_kind<'de, D>(deserializer: D) -> Result<CatalogAiSkillCandidateKind, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let value = String::deserialize(deserializer)?;
        if value != "ai-skill" {
            return Err(serde::de::Error::unknown_variant(&value, &["ai-skill"]));
        }
        <CatalogAiSkillCandidateKind>::deserialize(
            serde::de::value::StringDeserializer::<D::Error>::new(value),
        )
    }

    fn deserialize_media_type<'de, D>(deserializer: D) -> Result<CatalogAiSkillMediaType, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let value = String::deserialize(deserializer)?;
        if value != "application/ai-skill" {
            return Err(serde::de::Error::unknown_variant(
                &value,
                &["application/ai-skill"],
            ));
        }
        <CatalogAiSkillMediaType>::deserialize(
            serde::de::value::StringDeserializer::<D::Error>::new(value),
        )
    }
}

/// Where and when an MCP server catalog reference was observed. Discovery provenance deliberately carries no content digest because search does not establish the exact validated content a later plan will bind.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogMcpServerCandidateProvenance {
    /// Host of the catalog authority that advertised the reference, without path, query, or credentials. Inert untrusted data.
    pub authority: String,
    /// JSON MCP media type advertised for the referenced card.
    pub media_type: McpServerCardMediaType,
    /// ISO 8601 timestamp at which the runtime observed the catalog reference. This is not a retrieval or validation timestamp.
    pub observed_at: String,
}

/// An inert MCP server catalog result. Every free-text field is untrusted external data and must never be treated as an instruction, and the handle is the only way to refer to the candidate in a later operation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogMcpServerCandidate {
    /// Category slugs reported by the feed, such as `build` or `review`, without repeats. Values are passed through, so clients must tolerate slugs they do not recognise. Inert untrusted text. Omitted when the feed reports none.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub categories: Option<Vec<String>>,
    /// Identifiers of the connectors the resource works with, as reported by the feed, without repeats. Inert untrusted text. Omitted when the feed reports none.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub connector_ids: Option<Vec<String>>,
    /// Description taken verbatim from the card. Inert untrusted text.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Display name taken verbatim from the card. Inert untrusted text.
    pub display_name: String,
    /// Opaque, runtime-instance scoped, TTL-bound, single-use handle for this candidate. Carries no readable information and is rejected when stale, replayed, or presented to a different runtime instance. Never logged.
    pub handle: String,
    /// ISO 8601 timestamp after which the handle is stale and will be rejected.
    pub handle_expires_at: String,
    /// HTTPS URL of the resource's icon on raw.githubusercontent.com, as reported by the feed. Untrusted external content: load it without credentials. Omitted when absent or outside the feed's icon host policy.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub icon_url: Option<String>,
    /// Whether this MCP server can be planned for installation, and if policy prevents it.
    pub installability: CatalogMcpServerInstallability,
    /// How many times the resource has been installed, as reported by the feed. Omitted when the feed reports no count or an invalid one.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub install_count: Option<i64>,
    /// Discriminator: this candidate describes an MCP server
    #[serde(deserialize_with = "CatalogMcpServerCandidate::deserialize_kind")]
    pub kind: CatalogMcpServerCandidateKind,
    /// JSON MCP media type of the underlying card.
    pub media_type: McpServerCardMediaType,
    /// Where the catalog reference was observed, without the card itself or any content digest.
    pub provenance: CatalogMcpServerCandidateProvenance,
    /// Publisher taken verbatim from the card. Inert untrusted text.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub publisher: Option<String>,
    /// Where the card came from: exactly one of a URL or embedded data, encoded as a tagged union so neither both nor neither can be represented.
    pub source: CatalogCandidateSource,
    /// Versioned trust metadata observed from the catalog authority. Optional for protocol-3 compatibility with runtimes that predate trust snapshots. A trust-capable runtime emits an explicit snapshot even when the authority omitted or malformed its trust field.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub trust: Option<serde_json::Value>,
}

impl CatalogMcpServerCandidate {
    fn deserialize_kind<'de, D>(deserializer: D) -> Result<CatalogMcpServerCandidateKind, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let value = String::deserialize(deserializer)?;
        if value != "mcp-server" {
            return Err(serde::de::Error::unknown_variant(&value, &["mcp-server"]));
        }
        <CatalogMcpServerCandidateKind>::deserialize(
            serde::de::value::StringDeserializer::<D::Error>::new(value),
        )
    }
}

/// An entry whose resource resolves and is presented as the same candidate catalog.search would return.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogFeedAvailableEntry {
    /// The entry's resource, with the same projection, installability, handle and display metadata as a catalog.search candidate.
    pub candidate: CatalogCandidate,
    /// The entry's resource identifier as saved in the feed.
    pub identifier: String,
    /// Discriminator: the entry's resource is available.
    #[serde(deserialize_with = "CatalogFeedAvailableEntry::deserialize_status")]
    pub status: CatalogFeedAvailableStatus,
}

impl CatalogFeedAvailableEntry {
    fn deserialize_status<'de, D>(deserializer: D) -> Result<CatalogFeedAvailableStatus, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let value = String::deserialize(deserializer)?;
        if value != "available" {
            return Err(serde::de::Error::unknown_variant(&value, &["available"]));
        }
        <CatalogFeedAvailableStatus>::deserialize(
            serde::de::value::StringDeserializer::<D::Error>::new(value),
        )
    }
}

/// An entry whose resource resolves but is not in one of the requested trust tiers.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogFeedFilteredEntry {
    /// The entry's resource identifier as saved in the feed.
    pub identifier: String,
    /// Discriminator: the entry was filtered by trust tier.
    #[serde(deserialize_with = "CatalogFeedFilteredEntry::deserialize_status")]
    pub status: CatalogFeedFilteredStatus,
}

impl CatalogFeedFilteredEntry {
    fn deserialize_status<'de, D>(deserializer: D) -> Result<CatalogFeedFilteredStatus, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let value = String::deserialize(deserializer)?;
        if value != "filtered" {
            return Err(serde::de::Error::unknown_variant(&value, &["filtered"]));
        }
        <CatalogFeedFilteredStatus>::deserialize(
            serde::de::value::StringDeserializer::<D::Error>::new(value),
        )
    }
}

/// An entry that cannot be presented, kept in its position so clients decide how to render it.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogFeedUnavailableEntry {
    /// The entry's resource identifier as saved in the feed.
    pub identifier: String,
    /// Why the entry cannot be presented.
    pub reason: CatalogFeedUnavailableReason,
    /// Discriminator: the entry's resource is unavailable.
    #[serde(deserialize_with = "CatalogFeedUnavailableEntry::deserialize_status")]
    pub status: CatalogFeedUnavailableStatus,
}

impl CatalogFeedUnavailableEntry {
    fn deserialize_status<'de, D>(deserializer: D) -> Result<CatalogFeedUnavailableStatus, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let value = String::deserialize(deserializer)?;
        if value != "unavailable" {
            return Err(serde::de::Error::unknown_variant(&value, &["unavailable"]));
        }
        <CatalogFeedUnavailableStatus>::deserialize(
            serde::de::value::StringDeserializer::<D::Error>::new(value),
        )
    }
}

/// A generated feed's identity. It has no version; generatedAt is its only freshness signal.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogGeneratedFeedDescription {
    /// When the feed was computed, as reported by the authority. Omitted when the authority reports no valid timestamp.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub generated_at: Option<String>,
    /// Feed key that was requested.
    pub key: String,
    /// Generation strategy that was requested.
    pub strategy: String,
}

/// Retrieves one generated feed, computed on every request, by strategy and key, such as `telemetry` and `popular`. Requires catalog-feeds and catalog-search-credential-required; add catalog-search-session-bound with policySessionId to retain the selected session's authority for installation, exactly as for catalog.search.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogGetGeneratedFeedRequest {
    /// Protocol version and capabilities the caller requires.
    pub contract: CatalogClientContract,
    /// Feed key within the strategy, such as `popular`.
    pub key: String,
    /// Select an existing attached local session. Requires authenticated, session-bound catalogue support.
    /// The runtime never creates, resumes or reconfigures a session to honour this selector.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub policy_session_id: Option<String>,
    /// Generation strategy, such as `telemetry`.
    pub strategy: String,
    /// Accept only resources currently assessed at one of these trust tiers. Entries whose resource is in another tier, or has none, are returned as filtered in their original position. Omit for an unfiltered feed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub trust_tiers: Option<Vec<CatalogTrustTier>>,
}

/// A retrieved generated feed. Entries keep the feed's order and are never more than 100.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogGetGeneratedFeedSucceeded {
    /// Feed entries in the feed's order.
    pub entries: Vec<CatalogFeedEntry>,
    /// The feed's own identity and freshness.
    pub feed: CatalogGeneratedFeedDescription,
    /// Discriminator: the feed was retrieved
    pub kind: CatalogGetGeneratedFeedSucceededKind,
    /// Protocol version and capabilities the runtime honoured.
    pub negotiated: CatalogNegotiatedContract,
    /// Runtime-issued identifier for this retrieval, with the same meaning as a catalog.search searchId, so a plan can be tied to the feed that produced its candidate. Derived from no user, account, device or feed data.
    pub search_id: String,
}

/// Retrieves one saved feed by its opaque ID. Requires catalog-feeds and catalog-search-credential-required; add catalog-search-session-bound with policySessionId to retain the selected session's authority for installation, exactly as for catalog.search.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogGetSavedFeedRequest {
    /// Protocol version and capabilities the caller requires.
    pub contract: CatalogClientContract,
    /// Opaque saved feed ID. Treat it as an indivisible string.
    pub feed_id: String,
    /// Select an existing attached local session. Requires authenticated, session-bound catalogue support.
    /// The runtime never creates, resumes or reconfigures a session to honour this selector.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub policy_session_id: Option<String>,
    /// Accept only resources currently assessed at one of these trust tiers. Entries whose resource is in another tier, or has none, are returned as filtered in their original position. Omit for an unfiltered feed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub trust_tiers: Option<Vec<CatalogTrustTier>>,
}

/// A saved feed's identity. Its version changes only when the feed's definition is replaced, not when a referenced resource changes.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogSavedFeedDescription {
    /// Opaque feed ID.
    pub id: String,
    /// Feed name as saved by its creator. Inert untrusted text.
    pub name: String,
    /// Version of the saved feed definition.
    pub version: i64,
}

/// A retrieved saved feed. Entries keep the feed's order and are never more than 100.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogGetSavedFeedSucceeded {
    /// Feed entries in the feed's order.
    pub entries: Vec<CatalogFeedEntry>,
    /// The feed's own identity and freshness.
    pub feed: CatalogSavedFeedDescription,
    /// Discriminator: the feed was retrieved
    pub kind: CatalogGetSavedFeedSucceededKind,
    /// Protocol version and capabilities the runtime honoured.
    pub negotiated: CatalogNegotiatedContract,
    /// Runtime-issued identifier for this retrieval, with the same meaning as a catalog.search searchId, so a plan can be tied to the feed that produced its candidate. Derived from no user, account, device or feed data.
    pub search_id: String,
}

/// An Agent Finder materialisation reference pinning one exact resource revision.
///
/// It carries no origin or URL: the runtime always builds the descriptor URL on its fixed Agent
/// Finder authority. It carries no display data either; every reviewed field comes from the
/// verified descriptor and Skill files.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogMaterializationReference {
    /// Expected `sha256:<lowercase hex>` digest of the descriptor's bundle manifest.
    pub bundle_digest: String,
    /// Expected `sha256:<lowercase hex>` digest of the canonical descriptor.
    pub descriptor_digest: String,
    /// Agent Finder resource identifier, a positive decimal integer.
    pub resource_id: String,
    /// Agent Finder materialisation revision identifier, a positive decimal integer. This is not a Git commit.
    pub revision_id: String,
    /// Materialisation descriptor schema version, currently `1.0.0`.
    pub schema_version: String,
}

/// Side-effect-free planning of whatever resource a materialisation reference pins, rather than a search candidate.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogPlanInstallFromReferenceRequest {
    /// Required authenticated bound catalogue and catalog-reference-installation capabilities, plus the installation capability of each kind the caller can apply.
    pub contract: CatalogClientContract,
    /// Existing local session attached to this connection.
    pub policy_session_id: String,
    /// Materialisation reference to verify and plan.
    pub reference: CatalogMaterializationReference,
}

/// The reference pins a Skill. Apply the plan with skills.applyInstall.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogReferenceInstallationOutcomeSkillInstallPlanned {
    /// Reference planning outcome discriminator naming the planned kind.
    pub kind: CatalogReferenceInstallationOutcomeSkillInstallPlannedKind,
    /// Prepared Skill install plan.
    pub plan: SkillInstallPlan,
}

/// The reference pins an Agent Plugin. Apply the plan with plugins.applyInstall.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogReferenceInstallationOutcomePluginInstallPlanned {
    /// Reference planning outcome discriminator naming the planned kind.
    pub kind: CatalogReferenceInstallationOutcomePluginInstallPlannedKind,
    /// Prepared Agent Plugin install plan.
    pub plan: PluginInstallPlan,
}

/// Nothing was planned.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogReferenceInstallationOutcomeRefused {
    /// Reference planning outcome discriminator naming the planned kind.
    pub kind: CatalogReferenceInstallationOutcomeRefusedKind,
    /// Bounded refusal reason.
    pub reason: CatalogReferenceInstallationFailureReason,
    /// Whole seconds GitHub asked the caller to wait, present only with source-rate-limited.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub retry_after_seconds: Option<i64>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogReferenceInstallationResultOutcome {
    /// Discriminator: handled planning outcome.
    pub kind: CatalogReferenceInstallationResultOutcomeKind,
    /// Capabilities honoured for this request.
    pub negotiated: CatalogNegotiatedContract,
    /// Planning outcome.
    pub outcome: CatalogReferenceInstallationOutcome,
}

/// An explicit numbered-page request. SDK consumers treat the token as opaque. For bound search, the runtime unwraps an expiring owner-bound reference to the private authority token; only the runtime changes the authority token's targetPage. Legacy unbound navigation keeps its authority-issued token semantics. No snapshot stability is promised.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogSearchPage {
    /// Requested one-based page. Must not exceed either the token's signed pageCount or the navigation window ceil(1000 / pageSize). Repeat the search without page to discover newly available pages beyond that signed pageCount.
    pub number: i32,
    /// Opaque pagination token from an earlier response, owner-bound when session-bound search was requested. Never decode, modify or log it in an SDK consumer. Expired or foreign bound references require a fresh bound search, not a legacy retry.
    pub token: String,
}

/// A bounded catalog search. Both the query length and the result count are capped by the schema so a caller cannot request an unbounded scan.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogSearchRequest {
    /// Protocol version and capabilities the caller requires.
    pub contract: CatalogClientContract,
    /// Restrict results to these candidate kinds. Agent Plugins are opt-in and require the `agent-plugin-discovery` capability so protocol-v3 clients generated before that variant cannot receive an unknown result; when omitted, the backwards-compatible MCP server and AI skill kinds are searched.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub kinds: Option<Vec<CatalogCandidateKind>>,
    /// Maximum number of candidates to return. Defaults to 10 when omitted.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub limit: Option<i32>,
    /// Numbered navigation using metadata from an earlier response. Requires catalog-search-pagination and the same query, kinds, categories and effective limit. Omit for a fresh first-page search.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub page: Option<CatalogSearchPage>,
    /// Free-text search query. Persisted as tool input for session continuity, but omitted from telemetry.
    pub query: String,
}

/// Extensible [`CatalogSearchRequest`], including inputs added after it was published.
///
/// Required inputs are [`CatalogSearchOptions::new`] arguments; optional inputs have fluent setters.
/// Input-only: it serialises to the flat wire request and is not deserialisable.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogSearchOptions {
    #[serde(flatten)]
    legacy: CatalogSearchRequest,
    #[serde(skip_serializing_if = "Option::is_none")]
    categories: Option<Vec<String>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    policy_session_id: Option<String>,
}

impl CatalogSearchOptions {
    /// Creates options with the required inputs.
    pub fn new(contract: CatalogClientContract, query: impl Into<String>) -> Self {
        Self {
            legacy: CatalogSearchRequest {
                contract,
                query: query.into(),
                limit: None,
                kinds: None,
                page: None,
            },
            categories: None,
            policy_session_id: None,
        }
    }

    /// Maximum number of candidates to return. Defaults to 10 when omitted.
    pub fn limit(mut self, value: i32) -> Self {
        self.legacy.limit = Some(value);
        self
    }

    /// Restrict results to these candidate kinds. Agent Plugins are opt-in and require the `agent-plugin-discovery` capability so protocol-v3 clients generated before that variant cannot receive an unknown result; when omitted, the backwards-compatible MCP server and AI skill kinds are searched.
    pub fn kinds(mut self, value: Vec<CatalogCandidateKind>) -> Self {
        self.legacy.kinds = Some(value);
        self
    }

    /// Numbered navigation using metadata from an earlier response. Requires catalog-search-pagination and the same query, kinds, categories and effective limit. Omit for a fresh first-page search.
    pub fn page(mut self, value: CatalogSearchPage) -> Self {
        self.legacy.page = Some(value);
        self
    }

    /// Restrict results to resources in at least one of these server-defined workflow categories, such as `design` or `test`. Requires the `catalog-search-categories` capability. Each value is a lowercase slug; the known set is not exhaustive, and a category the server does not recognise matches nothing. Omit to search every category.
    pub fn categories(mut self, value: Vec<String>) -> Self {
        self.categories = Some(value);
        self
    }

    /// Select an existing attached local session. Requires authenticated, session-bound search.
    /// The runtime never creates, resumes or reconfigures a session to honour this selector.
    pub fn policy_session_id(mut self, value: impl Into<String>) -> Self {
        self.policy_session_id = Some(value.into());
        self
    }
}

/// A completed catalog search containing inert candidate summaries. MCP server and AI skill variants carry a single-use handle; the Agent Plugin variant is handleless.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogSearchSucceeded {
    /// Matching candidates, never more than the requested limit. All text is inert untrusted data.
    pub candidates: Vec<CatalogCandidate>,
    /// Discriminator: the search completed
    pub kind: CatalogSearchSucceededKind,
    /// Protocol version and capabilities the runtime honoured.
    pub negotiated: CatalogNegotiatedContract,
    /// Navigation metadata for callers explicitly requiring catalog-search-pagination. Omitted when the authority returns no token, including tokenless first-page and continuation responses. Counts are never substituted from candidates.length.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub pagination: Option<CatalogSearchPagination>,
    /// Pseudonymous identifier for this search, issued by the runtime or by the catalog authority it queried and never by the caller, so it cannot be forged or replayed to attribute an install to a search that never happened. Always present on a success, so a result set can be tied to the installs it leads to. It identifies a search rather than a person: it is derived from no user, account, device, or query data, and must never be joined with user identity to re-identify anyone.
    pub search_id: String,
    /// Legacy indication that the authority returned a page token. Preserved for compatibility; this is not a has-next-page indicator. Use pagination.hasNextPage when pagination metadata is present.
    pub truncated: bool,
}

/// The request asked for a candidate kind this runtime does not serve.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogUnsupportedKindError {
    /// Discriminator: an unsupported candidate kind was requested
    pub kind: CatalogUnsupportedKindErrorKind,
    /// Human-readable explanation, safe to surface. Never contains a query, URL, handle, or secret.
    pub message: String,
    /// The kinds from the request that are not supported.
    pub requested_kinds: Vec<CatalogCandidateKind>,
    /// Every candidate kind this runtime can serve.
    pub supported_kinds: Vec<CatalogCandidateKind>,
}

/// The caller cancelled the selection interaction and the retained search state was released.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogSelectionCancelled {
    /// Discriminator: selection was cancelled
    pub kind: CatalogSelectionCancelledKind,
    /// The search identifier privately bound to the released selection group.
    pub search_id: String,
}

/// The caller explicitly declined every candidate and the retained search state was released.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogSelectionDeclined {
    /// Discriminator: the candidates were declined
    pub kind: CatalogSelectionDeclinedKind,
    /// The search identifier privately bound to the released selection group.
    pub search_id: String,
}

/// The selection reference belongs to another runtime instance or session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogSelectionForeign {
    /// Discriminator for this typed selection rejection
    pub kind: CatalogSelectionForeignKind,
    /// Human-readable explanation safe to surface. Never contains the presented reference or private candidate state.
    pub message: String,
}

/// The selection reference was malformed or unknown.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogSelectionInvalid {
    /// Discriminator for this typed selection rejection
    pub kind: CatalogSelectionInvalidKind,
    /// Human-readable explanation safe to surface. Never contains the presented reference or private candidate state.
    pub message: String,
}

/// The selection group was already terminated or its pending host hand-off was already claimed.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogSelectionReplayed {
    /// Discriminator for this typed selection rejection
    pub kind: CatalogSelectionReplayedKind,
    /// Human-readable explanation safe to surface. Never contains the presented reference or private candidate state.
    pub message: String,
}

/// Terminates one retained catalog selection group through an opaque reference previously returned by the model-safe search projection.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogSelectionRequest {
    /// Protocol version and capabilities the caller requires.
    pub contract: CatalogClientContract,
    /// The terminal outcome declared by the caller. Timed-out means the host's live interaction deadline elapsed; a reference whose runtime TTL elapsed is rejected separately as stale.
    pub outcome: CatalogSelectionDecision,
    /// Opaque runtime-instance scoped reference to one visible candidate. For a non-selected outcome, any candidate reference from the same search closes that search's retained group.
    pub selection_ref: String,
    /// Locally owned root session whose retained search state is being resolved.
    pub session_id: SessionId,
}

/// The chosen candidate was transferred into a fresh bounded single-use handle for a later explicit planning request.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogSelectionSelected {
    /// Fresh single-use candidate handle accepted by mcp.planInstall. Returned only to the native host and never included in model-tool output.
    pub candidate_handle: String,
    /// Discriminator: one candidate was selected
    pub kind: CatalogSelectionSelectedKind,
    /// The exact search identifier privately bound to the selected candidate.
    pub search_id: String,
}

/// The host declared that its live selection interaction timed out, and the retained search state was released.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogSelectionTimedOut {
    /// Discriminator: the host's live interaction timed out
    pub kind: CatalogSelectionTimedOutKind,
    /// The search identifier privately bound to the released selection group.
    pub search_id: String,
}

/// The runtime-enforced selection reference lifetime elapsed before the request arrived.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogSelectionStale {
    /// Discriminator for this typed selection rejection
    pub kind: CatalogSelectionStaleKind,
    /// Human-readable explanation safe to surface. Never contains the presented reference or private candidate state.
    pub message: String,
}

/// The presented opaque handle was issued for another catalog operation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogSelectionWrongKind {
    /// Discriminator for this typed selection rejection
    pub kind: CatalogSelectionWrongKindKind,
    /// Human-readable explanation safe to surface. Never contains the presented reference or private candidate state.
    pub message: String,
}

/// Where and when the runtime observed the trust metadata. Observation time is not the authority's evaluation time and must not be used to infer staleness.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogTrustProvenance {
    /// ISO 8601 timestamp with a timezone offset at which the runtime observed the search result carrying this trust field.
    pub observed_at: String,
    /// Bounded authority that supplied the trust field.
    pub source: CatalogTrustSource,
}

/// Discriminator: the authority omitted trust metadata.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogTrustSnapshotAbsent {
    /// Service-computed exposure eligibility. `unknown` is required while Agent Finder returns no explicit eligibility field.
    pub eligibility: CatalogTrustEligibility,
    /// Bounded source and observation time for this snapshot. This is distinct from evidence used by the authority to calculate trust.
    pub provenance: CatalogTrustProvenance,
    /// Schema version of this runtime-owned snapshot envelope.
    pub schema_version: CatalogTrustSnapshotSchemaVersion,
    /// Discriminator: the authority omitted trust metadata.
    #[serde(deserialize_with = "CatalogTrustSnapshotAbsent::deserialize_status")]
    pub status: CatalogTrustSnapshotAbsentStatus,
}

impl CatalogTrustSnapshotAbsent {
    fn deserialize_status<'de, D>(
        deserializer: D,
    ) -> Result<CatalogTrustSnapshotAbsentStatus, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let value = String::deserialize(deserializer)?;
        if value != "absent" {
            return Err(serde::de::Error::unknown_variant(&value, &["absent"]));
        }
        <CatalogTrustSnapshotAbsentStatus>::deserialize(serde::de::value::StringDeserializer::<
            D::Error,
        >::new(value))
    }
}

/// A recognised current Agent Finder T1 or T2 trust tier.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogTrustSnapshotCurrent {
    /// Service-computed exposure eligibility. `unknown` is required while Agent Finder returns no explicit eligibility field.
    pub eligibility: CatalogTrustEligibility,
    /// Bounded source and observation time for this snapshot. This is distinct from evidence used by the authority to calculate trust.
    pub provenance: CatalogTrustProvenance,
    /// Schema version of this runtime-owned snapshot envelope.
    pub schema_version: CatalogTrustSnapshotSchemaVersion,
    /// Discriminator: a recognised current trust tier was observed.
    #[serde(deserialize_with = "CatalogTrustSnapshotCurrent::deserialize_status")]
    pub status: CatalogTrustSnapshotCurrentStatus,
    /// Service-computed T1 or T2 trust tier.
    pub tier: CatalogTrustTier,
}

impl CatalogTrustSnapshotCurrent {
    fn deserialize_status<'de, D>(
        deserializer: D,
    ) -> Result<CatalogTrustSnapshotCurrentStatus, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let value = String::deserialize(deserializer)?;
        if value != "current" {
            return Err(serde::de::Error::unknown_variant(&value, &["current"]));
        }
        <CatalogTrustSnapshotCurrentStatus>::deserialize(serde::de::value::StringDeserializer::<
            D::Error,
        >::new(value))
    }
}

/// Discriminator: the authority explicitly reported a downgraded assessment.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogTrustSnapshotDowngraded {
    /// Service-computed exposure eligibility. `unknown` is required while Agent Finder returns no explicit eligibility field.
    pub eligibility: CatalogTrustEligibility,
    /// Bounded source and observation time for this snapshot. This is distinct from evidence used by the authority to calculate trust.
    pub provenance: CatalogTrustProvenance,
    /// Schema version of this runtime-owned snapshot envelope.
    pub schema_version: CatalogTrustSnapshotSchemaVersion,
    /// Discriminator: the authority explicitly reported a downgraded assessment.
    #[serde(deserialize_with = "CatalogTrustSnapshotDowngraded::deserialize_status")]
    pub status: CatalogTrustSnapshotDowngradedStatus,
}

impl CatalogTrustSnapshotDowngraded {
    fn deserialize_status<'de, D>(
        deserializer: D,
    ) -> Result<CatalogTrustSnapshotDowngradedStatus, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let value = String::deserialize(deserializer)?;
        if value != "downgraded" {
            return Err(serde::de::Error::unknown_variant(&value, &["downgraded"]));
        }
        <CatalogTrustSnapshotDowngradedStatus>::deserialize(serde::de::value::StringDeserializer::<
            D::Error,
        >::new(value))
    }
}

/// Discriminator: the trust field was empty, unbounded, or had the wrong JSON type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogTrustSnapshotMalformed {
    /// Service-computed exposure eligibility. `unknown` is required while Agent Finder returns no explicit eligibility field.
    pub eligibility: CatalogTrustEligibility,
    /// Bounded source and observation time for this snapshot. This is distinct from evidence used by the authority to calculate trust.
    pub provenance: CatalogTrustProvenance,
    /// Schema version of this runtime-owned snapshot envelope.
    pub schema_version: CatalogTrustSnapshotSchemaVersion,
    /// Discriminator: the trust field was empty, unbounded, or had the wrong JSON type.
    #[serde(deserialize_with = "CatalogTrustSnapshotMalformed::deserialize_status")]
    pub status: CatalogTrustSnapshotMalformedStatus,
}

impl CatalogTrustSnapshotMalformed {
    fn deserialize_status<'de, D>(
        deserializer: D,
    ) -> Result<CatalogTrustSnapshotMalformedStatus, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let value = String::deserialize(deserializer)?;
        if value != "malformed" {
            return Err(serde::de::Error::unknown_variant(&value, &["malformed"]));
        }
        <CatalogTrustSnapshotMalformedStatus>::deserialize(serde::de::value::StringDeserializer::<
            D::Error,
        >::new(value))
    }
}

/// Discriminator: the authority explicitly revoked the assessment.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogTrustSnapshotRevoked {
    /// Service-computed exposure eligibility. `unknown` is required while Agent Finder returns no explicit eligibility field.
    pub eligibility: CatalogTrustEligibility,
    /// Bounded source and observation time for this snapshot. This is distinct from evidence used by the authority to calculate trust.
    pub provenance: CatalogTrustProvenance,
    /// Schema version of this runtime-owned snapshot envelope.
    pub schema_version: CatalogTrustSnapshotSchemaVersion,
    /// Discriminator: the authority explicitly revoked the assessment.
    #[serde(deserialize_with = "CatalogTrustSnapshotRevoked::deserialize_status")]
    pub status: CatalogTrustSnapshotRevokedStatus,
}

impl CatalogTrustSnapshotRevoked {
    fn deserialize_status<'de, D>(
        deserializer: D,
    ) -> Result<CatalogTrustSnapshotRevokedStatus, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let value = String::deserialize(deserializer)?;
        if value != "revoked" {
            return Err(serde::de::Error::unknown_variant(&value, &["revoked"]));
        }
        <CatalogTrustSnapshotRevokedStatus>::deserialize(serde::de::value::StringDeserializer::<
            D::Error,
        >::new(value))
    }
}

/// Discriminator: the authority explicitly marked the assessment stale.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogTrustSnapshotStale {
    /// Service-computed exposure eligibility. `unknown` is required while Agent Finder returns no explicit eligibility field.
    pub eligibility: CatalogTrustEligibility,
    /// Bounded source and observation time for this snapshot. This is distinct from evidence used by the authority to calculate trust.
    pub provenance: CatalogTrustProvenance,
    /// Schema version of this runtime-owned snapshot envelope.
    pub schema_version: CatalogTrustSnapshotSchemaVersion,
    /// Discriminator: the authority explicitly marked the assessment stale.
    #[serde(deserialize_with = "CatalogTrustSnapshotStale::deserialize_status")]
    pub status: CatalogTrustSnapshotStaleStatus,
}

impl CatalogTrustSnapshotStale {
    fn deserialize_status<'de, D>(
        deserializer: D,
    ) -> Result<CatalogTrustSnapshotStaleStatus, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let value = String::deserialize(deserializer)?;
        if value != "stale" {
            return Err(serde::de::Error::unknown_variant(&value, &["stale"]));
        }
        <CatalogTrustSnapshotStaleStatus>::deserialize(serde::de::value::StringDeserializer::<
            D::Error,
        >::new(value))
    }
}

/// Discriminator: the authority supplied a bounded trust value this runtime does not understand.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogTrustSnapshotUnsupported {
    /// Service-computed exposure eligibility. `unknown` is required while Agent Finder returns no explicit eligibility field.
    pub eligibility: CatalogTrustEligibility,
    /// Bounded source and observation time for this snapshot. This is distinct from evidence used by the authority to calculate trust.
    pub provenance: CatalogTrustProvenance,
    /// Schema version of this runtime-owned snapshot envelope.
    pub schema_version: CatalogTrustSnapshotSchemaVersion,
    /// Discriminator: the authority supplied a bounded trust value this runtime does not understand.
    #[serde(deserialize_with = "CatalogTrustSnapshotUnsupported::deserialize_status")]
    pub status: CatalogTrustSnapshotUnsupportedStatus,
}

impl CatalogTrustSnapshotUnsupported {
    fn deserialize_status<'de, D>(
        deserializer: D,
    ) -> Result<CatalogTrustSnapshotUnsupportedStatus, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let value = String::deserialize(deserializer)?;
        if value != "unsupported" {
            return Err(serde::de::Error::unknown_variant(&value, &["unsupported"]));
        }
        <CatalogTrustSnapshotUnsupportedStatus>::deserialize(
            serde::de::value::StringDeserializer::<D::Error>::new(value),
        )
    }
}

/// Slash commands available in the session, after applying any include/exclude filters.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CommandList {
    /// Commands available in this session
    pub commands: Vec<SlashCommandInfo>,
}

/// The pending slash-command invocation effect to finalize, plus whether the host applied or cancelled it.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CommandsFinalizeInvocationEffectRequest {
    /// The slash-command result object that produced the pending effect, echoed back unchanged.
    pub effect: serde_json::Value,
    /// Whether the host applied or cancelled the pending invocation effect.
    pub outcome: CommandsInvocationEffectOutcome,
}

/// Whether finalizing the invocation effect succeeded, and the failure reason when it did not.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CommandsFinalizeInvocationEffectResult {
    /// Failure reason when the invocation effect could not be finalized.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Whether the pending invocation effect was finalized successfully.
    pub success: bool,
}

/// Pending command request ID and an optional error if the client handler failed.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CommandsHandlePendingCommandRequest {
    /// Error message if the command handler failed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Request ID from the command invocation event
    pub request_id: RequestId,
}

/// Indicates whether the pending client-handled command was completed successfully.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CommandsHandlePendingCommandResult {
    /// Whether the command was handled successfully
    pub success: bool,
}

/// Slash command name and optional raw input string to invoke.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CommandsInvokeRequest {
    /// Raw input after the command name
    #[serde(skip_serializing_if = "Option::is_none")]
    pub input: Option<String>,
    /// Command name. Leading slashes are stripped and the name is matched case-insensitively.
    pub name: String,
    /// Optional client surface that initiated the invocation
    #[serde(skip_serializing_if = "Option::is_none")]
    pub origin: Option<CommandsInvocationOrigin>,
}

/// Optional filters controlling which command sources to include in the listing.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CommandsListRequest {
    /// Include runtime built-in commands
    #[serde(skip_serializing_if = "Option::is_none")]
    pub include_builtins: Option<bool>,
    /// Include commands registered by protocol clients, including SDK clients and extensions
    #[serde(skip_serializing_if = "Option::is_none")]
    pub include_client_commands: Option<bool>,
    /// Include enabled user-invocable skills and commands
    #[serde(skip_serializing_if = "Option::is_none")]
    pub include_skills: Option<bool>,
}

/// Host report that its execution of a queued command started or stopped waiting for runtime state. The report is observational and never changes command execution.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CommandsReportQueuedCommandWaitRequest {
    /// Stable identity of the account whose managed policy the host awaits.
    pub account_identity: Option<String>,
    /// Request ID from the `command.queued` event the host is executing.
    pub request_id: RequestId,
    /// Runtime state the host waits for.
    pub requirement: QueuedCommandWaitRequirement,
    /// True when the wait starts and false when it ends.
    pub waiting: bool,
}

/// Whether the runtime recorded the queued-command wait.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CommandsReportQueuedCommandWaitResult {
    /// False when the queued command is no longer pending or the awaited state is not observed.
    pub recorded: bool,
}

/// Queued-command request ID and the result indicating whether the host executed it (and whether to stop processing further queued commands).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CommandsRespondToQueuedCommandRequest {
    /// Request ID from the `command.queued` event the host is responding to.
    pub request_id: RequestId,
    /// Result of the queued command execution.
    pub result: serde_json::Value,
}

/// Indicates whether the queued-command response was matched to a pending request.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CommandsRespondToQueuedCommandResult {
    /// Whether a pending queued command with the given request ID was found and resolved. False when the request was already resolved, cancelled, or unknown.
    pub success: bool,
}

/// Characters that, when typed in the composer, should trigger a `completions.request`. Empty when the session has no host-driven completions (e.g. local sessions, or a relay host that does not advertise `completionTriggerCharacters`).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CompletionsGetTriggerCharactersResult {
    /// Trigger characters advertised by the host (e.g. `["@", "#"]`). Empty disables host-driven completions for the session.
    pub trigger_characters: Vec<String>,
}

/// Request host-driven completions for the current composer input.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CompletionsRequestRequest {
    /// Cursor offset within `text`, in UTF-16 code units.
    pub offset: i64,
    /// The full composed composer input.
    pub text: String,
}

/// Host-driven completion items for the current composer input. Empty when the host returns no items or does not support completions.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CompletionsRequestResult {
    /// Completion items in host-ranked order.
    pub items: Vec<SessionCompletionItem>,
}

/// Identity of the integrating host, declared once on the `server.connect` handshake so telemetry from this connection is attributed to a single, consistent surface. All fields are optional; omit them to keep the default attribution.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ConnectClientInfo {
    /// Name of the host editor, e.g. `"vscode"`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub editor_name: Option<String>,
    /// Version of the host editor, e.g. `"1.124.2"`. Ignored unless it looks like a version string.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub editor_version: Option<String>,
    /// Name of the Copilot extension within the host, e.g. `"copilot-chat"`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub extension_name: Option<String>,
    /// Version of the Copilot extension within the host, e.g. `"0.54.0"`. Ignored unless it looks like a version string.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub extension_version: Option<String>,
}

/// Pins a Connector operation to one host-owned GitHub account through its opaque selection ID. Provider tokens are never accepted.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ConnectorAccountRequest {
    /// Opaque account selection ID previously returned by an account discovery API.
    pub account_id: String,
}

/// Feature detection and hard polling limits for the EXPERIMENTAL session connector API.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ConnectorCapabilities {
    /// Connector API contract version.
    pub api_version: i64,
    /// Current session availability. Disabled availability is reported without making a Connector request.
    pub availability: ConnectorAvailability,
    /// Whether connect and reconnect can return an opaque continuation for bounded consent polling.
    pub consent_continuation: bool,
    /// Maximum accepted wall-clock deadline in milliseconds for one continuation call.
    pub max_deadline_ms: i64,
    /// Maximum accepted polling attempts for one continuation call.
    pub max_poll_attempts: i64,
    /// Maximum accepted delay in milliseconds between polling attempts.
    pub max_poll_interval_ms: i64,
    /// Whether callers select a host-owned GitHub account through an opaque selection ID rather than supplying a provider token.
    pub opaque_account_selection: bool,
    /// Whether getAccount is supported. Absence means false.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_account_selection: Option<bool>,
    /// Whether reconcile accepts forceConnectorName. Absence means false.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub targeted_reconcile: Option<bool>,
}

/// Selects one Connector and the pinned host-owned account used for its service and MCP authorization.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ConnectorConnectRequest {
    /// Opaque account selection ID. It must match the account already pinned to the session, if any.
    pub account_id: String,
    /// Canonical Connector name from the current catalog.
    pub connector_name: String,
}

/// The service is connected and the session MCP graph was reconciled.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ConnectorConnectResultConnected {
    /// The service is connected and the session MCP graph was reconciled.
    pub kind: ConnectorConnectResultConnectedKind,
    /// Fresh authoritative Connector state after MCP reconciliation.
    pub status: ConnectorStatus,
}

/// Host-owned consent is required before bounded continuation can complete.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ConnectorConnectResultConsentRequired {
    /// Validated HTTPS consent URL. The runtime does not open it.
    pub consent_url: String,
    /// Opaque ID accepted by continueConnection.
    pub continuation_id: String,
    /// Host-owned consent is required before bounded continuation can complete.
    pub kind: ConnectorConnectResultConsentRequiredKind,
}

/// The service is still completing the connection without a consent URL.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ConnectorConnectResultPending {
    /// Opaque ID accepted by continueConnection.
    pub continuation_id: String,
    /// The service is still completing the connection without a consent URL.
    pub kind: ConnectorConnectResultPendingKind,
}

/// Explicitly bounded continuation of a pending Connector connection.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ConnectorContinueRequest {
    /// Opaque continuation ID returned by connect, reconnect, or an earlier continuation.
    pub continuation_id: String,
    /// Maximum wall-clock duration in milliseconds for this call. Must be between one and the capability limit.
    pub deadline_ms: i32,
    /// Maximum catalog requests made by this call. Must be between one and the capability limit.
    pub max_attempts: i32,
    /// Delay in milliseconds between attempts. Must not exceed the capability limit.
    pub poll_interval_ms: i32,
}

/// Authoritative result after disconnect and MCP reconciliation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ConnectorDisconnectResult {
    /// Whether the service accepted the idempotent disconnect.
    pub disconnected: bool,
    /// Fresh authoritative session state after removing Connector-owned MCP servers.
    pub status: ConnectorStatus,
}

/// Account metadata.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ConnectorDiscoveryAuthInfo {
    /// Host.
    pub host: String,
    /// Login.
    pub login: String,
    /// Authentication type.
    pub r#type: AuthInfoType,
}

/// Eligible account.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ConnectorDiscoveryAccount {
    /// Opaque account ID.
    pub account_id: String,
    /// Account metadata.
    pub auth_info: ConnectorDiscoveryAuthInfo,
}

/// Eligible accounts.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ConnectorDiscoveryAccountList {
    /// Eligible accounts.
    pub accounts: Vec<ConnectorDiscoveryAccount>,
    /// Availability.
    pub availability: ConnectorDiscoveryAvailability,
}

/// Selected account.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ConnectorDiscoveryAccountRequest {
    /// Opaque account ID.
    pub account_id: String,
}

/// Feature availability.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ConnectorDiscoveryCapabilities {
    /// API version.
    pub api_version: i64,
    /// Availability.
    pub availability: ConnectorDiscoveryAvailability,
    /// Whether results are cached.
    pub conditional_cache: bool,
    /// Whether accounts are selected by opaque ID.
    pub opaque_account_selection: bool,
}

/// Entry.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ConnectorDiscoveryCatalogEntry {
    /// Description.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Display name.
    pub display_name: String,
    /// Logo.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub logo: Option<String>,
    /// Name.
    pub name: String,
    /// Release tag.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub release_tag: Option<String>,
    /// Status.
    pub status: ConnectorCatalogStatus,
    /// Tier.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tier: Option<String>,
}

/// Entries for the selected account.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ConnectorDiscoveryCatalogResult {
    /// Opaque account ID.
    pub account_id: String,
    /// Entries.
    pub connectors: Vec<ConnectorDiscoveryCatalogEntry>,
    /// Refresh time in Unix epoch milliseconds.
    pub refreshed_at_ms: i64,
    /// Revision.
    pub revision: i64,
}

/// Requests authoritative Connector-to-MCP reconciliation for the pinned account.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ConnectorReconcileRequest {
    /// Opaque account selection ID. It must match the account already pinned to the session, if any.
    pub account_id: String,
    /// When true, refresh the catalog before reconciling. A disabled Connector API performs no service request.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub refresh_catalog: Option<bool>,
}

/// Extensible [`ConnectorReconcileRequest`], including inputs added after it was published.
///
/// Required inputs are [`ConnectorReconcileOptions::new`] arguments; optional inputs have fluent setters.
/// Input-only: it serialises to the flat wire request and is not deserialisable.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ConnectorReconcileOptions {
    #[serde(flatten)]
    legacy: ConnectorReconcileRequest,
    #[serde(skip_serializing_if = "Option::is_none")]
    force_connector_name: Option<String>,
}

impl ConnectorReconcileOptions {
    /// Creates options with the required inputs.
    pub fn new(account_id: impl Into<String>) -> Self {
        Self {
            legacy: ConnectorReconcileRequest {
                account_id: account_id.into(),
                refresh_catalog: None,
            },
            force_connector_name: None,
        }
    }

    /// When true, refresh the catalog before reconciling. A disabled Connector API performs no service request.
    pub fn refresh_catalog(mut self, value: bool) -> Self {
        self.legacy.refresh_catalog = Some(value);
        self
    }

    /// Optional Connector name to reinitialize. Requires the targetedReconcile capability.
    pub fn force_connector_name(mut self, value: impl Into<String>) -> Self {
        self.force_connector_name = Some(value.into());
        self
    }
}

/// Session account selection.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ConnectorSessionAccount {
    /// Opaque session-scoped account selection ID.
    pub account_id: String,
    /// Credential-free identity metadata.
    pub auth_info: AuthIdentityMetadata,
}

/// Remote session connection parameters.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ConnectRemoteSessionParams {
    /// Session ID to connect to.
    pub session_id: SessionId,
}

/// The host application's Microsoft Entra registration, shared by its sessions. This identifies the application requesting tokens, not their target resource.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct EntraAuthConfig {
    /// Application (client) ID of the host's Microsoft Entra registration. Must be nonblank. The runtime does not supply a default application.
    pub client_id: String,
    /// Application name reported by OneAuth to Microsoft Auth Telemetry System (MATS). Omit or leave blank to disable MATS telemetry without disabling authentication. OneAuth normalizes the name and may group applications into shared event tables.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mats_app_name: Option<String>,
}

/// Connection-level opt-ins for the `server.connect` handshake. Transport authentication is consumed by the native protocol boundary before dispatch.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ConnectRequest {
    /// Identity of the integrating host. Optional; omit it to keep the default attribution.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) client_info: Option<ConnectClientInfo>,
    /// Opt this connection in to GitHub telemetry forwarding for its lifetime. When set, the runtime forwards this host's telemetry across all its sessions, its sessionless events, and explicitly process-wide events over the `gitHubTelemetry.event` notification. Connections intentionally sharing one server receive that server's events; independently embedded runtime hosts do not receive each other's host-owned events. Regular events are also written to the runtime's normal GitHub/CTS path (dual-write); host-only compatibility events are forward-only and intentionally skip that path. Intended for first-party hosts that re-emit the events into their own telemetry stores. Both unrestricted and restricted events are forwarded, each tagged with a `restricted` discriminator; a backstop drops restricted events when restricted telemetry is disabled — using the process-global gate for ordinary events and an explicit session-scoped decision for host-only events.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_git_hub_telemetry_forwarding: Option<bool>,
    /// Entra application used by this client's authentication flows. Required to enable built-in Microsoft Entra sign-in and token renewal; other authentication methods remain available.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This type is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases.
    ///
    /// </div>
    #[serde(skip_serializing_if = "Option::is_none")]
    pub entra: Option<EntraAuthConfig>,
    /// Opt this connection in to host-owned Entra identity and token acquisition through entraToken.getToken. Requires entra.clientId and an explicit true acknowledgement in the response. SDKs set this internally when a callback is configured.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This type is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases.
    ///
    /// </div>
    #[serde(skip_serializing_if = "Option::is_none")]
    pub entra_token_provider: Option<bool>,
    /// Task kinds this connection can decode when observing session tasks. Omit to retain agent and shell compatibility.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub supported_task_kinds: Option<Vec<TaskKind>>,
    /// Connection token; required when the server was started with COPILOT_CONNECTION_TOKEN
    #[serde(skip_serializing_if = "Option::is_none")]
    pub token: Option<String>,
}

/// Handshake result reporting the server's protocol version and package version on success.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ConnectResult {
    /// Acknowledges that this connection's host-owned Entra token provider is enabled. Clients requesting it must reject a missing or false acknowledgement.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This type is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases.
    ///
    /// </div>
    #[serde(skip_serializing_if = "Option::is_none")]
    pub entra_token_provider: Option<bool>,
    /// Always true on success
    pub ok: bool,
    /// Server protocol version number
    pub protocol_version: i64,
    /// Task kinds the server may return to this connection.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub task_kinds: Option<Vec<TaskKind>>,
    /// Server package version
    pub version: String,
}

/// Local file system absolute paths within the session working directory to check against its content-exclusion policy.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ContentExclusionCheckPathsRequest {
    /// Local file system absolute paths within the session working directory to check. Results are returned in the same order, including duplicates.
    pub paths: Vec<String>,
}

/// Batch content-exclusion result. Callers must fail closed when policy evaluation is unavailable.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ContentExclusionCheckPathsResult {
    /// Whether the session's policy service was available for the complete batch. When false, checks is empty and callers must treat every requested path as excluded.
    pub available: bool,
    /// Per-path decisions in request order. Empty when available is false.
    pub checks: Vec<ContentExclusionPathCheck>,
}

/// Candidate content-exclusion policies to validate, as the raw JSON text a host received. The text is supplied unparsed so that a syntax error and a schema violation are both reported by the runtime's own validator rather than by the caller's JSON parser.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ContentExclusionValidatePoliciesRequest {
    /// Unparsed JSON text expected to hold an array of content-exclusion policies.
    pub policies_json: String,
}

/// The accepted policies, parsed and normalized. A rejected input is reported as an error rather than in this result.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ContentExclusionValidatePoliciesResult {
    /// Validated policy objects in input order. Raw JSON preserves every field returned by the runtime's policy parser.
    pub policies: Vec<serde_json::Value>,
}

/// Result of one customization reload component.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CustomizationReloadOutcome {
    /// Reason for a skipped component or description of a failure, when available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub detail: Option<String>,
    /// Whether the component reloaded, was skipped, or failed
    pub status: CustomizationReloadStatus,
    /// Component whose reload was attempted or skipped
    pub subsystem: CustomizationReloadSubsystem,
}

/// Results of reloading discovered session customizations. Inspect outcomes for reloaded, skipped, or failed subsystems; a rejection may follow partial mutation. Changes to the model-facing prompt and tools apply on the next turn.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CustomizationsReloadResult {
    /// Errors from any component that could not be refreshed
    pub errors: Vec<String>,
    /// Outcome of each component in reload order; a skipped component was not configured or loaded
    pub outcomes: Vec<CustomizationReloadOutcome>,
    /// Warnings from skill discovery
    pub warnings: Vec<String>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DebugCollectLogsDestinationArchive {
    /// Destination variant discriminator.
    pub kind: DebugCollectLogsDestinationArchiveKind,
    /// When true, create the archive atomically without overwriting an existing file by appending ` (N)` before the extension as needed. Defaults to false.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub no_overwrite: Option<bool>,
    /// Absolute or server-relative path for the .tgz archive to create.
    pub output_path: String,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DebugCollectLogsDestinationDirectory {
    /// Destination variant discriminator.
    pub kind: DebugCollectLogsDestinationDirectoryKind,
    /// Directory where files should be staged. The directory is created if needed.
    pub output_directory: String,
}

/// A caller-provided server-local file or directory to include in the debug bundle.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DebugCollectLogsEntry {
    /// Relative path to use inside the staged bundle/archive.
    pub bundle_path: String,
    /// Kind of source path to include.
    pub kind: DebugCollectLogsEntryKind,
    /// Server-local source path to read.
    pub path: String,
    /// How text content from this entry should be redacted. Defaults to plain-text. With none, no redaction is applied; the caller must ensure any necessary redaction is performed before this call.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub redaction: Option<DebugCollectLogsRedaction>,
    /// When true, collection fails if this entry cannot be read. Defaults to false, which records the entry in `skippedEntries`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub required: Option<bool>,
}

/// Built-in session diagnostics to include in the bundle. Omitted fields default to true.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DebugCollectLogsInclude {
    /// Server-local path to the current process log. When set, it is included as `process.log` and its directory is searched for prior logs from the same session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub current_process_log_path: Option<String>,
    /// Include the session event log (`events.jsonl`). Defaults to true.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub events: Option<bool>,
    /// Server-local path to the session's events.jsonl file. Internal callers normally omit this and let the runtime derive it from the session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub events_path: Option<String>,
    /// Maximum number of previous process logs to include. Defaults to 5.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub previous_process_log_limit: Option<i64>,
    /// Server-local process log directory to search when `currentProcessLogPath` is unavailable, useful for collecting logs for inactive sessions.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub process_log_directory: Option<String>,
    /// Include process logs for the session. Defaults to true.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub process_logs: Option<bool>,
    /// Include interactive shell logs written under the session's `shell-logs` directory. Defaults to true.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub shell_logs: Option<bool>,
}

/// Options for collecting a session debug bundle with configurable redaction.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DebugCollectLogsRequest {
    /// Caller-provided server-local files or directories to include in addition to the runtime's built-in session diagnostics. This lets host applications add their own diagnostics without changing the API shape.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub additional_entries: Option<Vec<DebugCollectLogsEntry>>,
    /// Where the bundle should be written. Use `archive` to produce a .tgz, or `directory` to stage files for caller-managed upload/post-processing.
    pub destination: DebugCollectLogsDestination,
    /// Which built-in session diagnostics to include. Omitted fields default to true.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub include: Option<DebugCollectLogsInclude>,
}

/// Result of collecting a session debug bundle.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DebugCollectLogsResult {
    /// Files included in the bundle.
    pub entries: Vec<DebugCollectLogsCollectedEntry>,
    /// Destination kind that was written.
    pub kind: DebugCollectLogsResultKind,
    /// Actual archive path or staging directory path written. This may differ from the requested path when no-overwrite suffixing or fallback-to-temp-directory was needed.
    pub path: String,
    /// Optional files or directories that could not be included.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub skipped_entries: Option<Vec<DebugCollectLogsSkippedEntry>>,
}

/// Extensions discovered from persisted Copilot home state and their effective loading mode. Launch-scoped additional plugins are not included.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DiscoveredExtensions {
    /// Discovered user and enabled installed-plugin extensions from persisted Copilot home state
    pub extensions: Vec<DiscoveredExtension>,
    /// Effective extension loading mode. Defaults to load_and_augment when unset.
    pub mode: DiscoveredExtensionMode,
}

/// Source-qualified extension identifiers to persistently disable for future sessions.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DiscoveredExtensionsDisableRequest {
    /// Source-qualified user or plugin extension IDs to disable
    pub ids: Vec<String>,
}

/// Source-qualified extension identifiers to persistently enable for future sessions.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DiscoveredExtensionsEnableRequest {
    /// Source-qualified user or plugin extension IDs to enable
    pub ids: Vec<String>,
}

/// One server-discovered hook action from user, repository, plugin, or managed-policy configuration.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DiscoveredHook {
    /// Durable content hash used by hook enablement. Identical actions may intentionally share this key. Omitted when changing the user's disabled-hooks setting cannot change the action's current server-discovered state, including managed-policy hooks, session-start prompt actions, actions suppressed by disable-all settings, and projectless plugin actions that require project-directory expansion.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub disable_key: Option<String>,
    /// Whether this action is enabled under the server-side discovery settings. Concrete sessions may differ because they can add session-specific directories, plugins, or trust. False when its disable key is present in the user's disabled-hooks setting or disable-all settings suppress the action.
    pub enabled: bool,
    /// Hook event that invokes this action.
    pub hook_type: HookType,
    /// Deterministic identifier for this server-discovered action row. It remains stable while the project, origin, source, event, action content, and duplicate ordinal are unchanged. This is row identity, not the key persisted in disabledHooks.
    pub id: String,
    /// Configuration tier that contributed this hook action.
    pub origin: HookOrigin,
    /// Input project path for which this server-side action was resolved. Set on every row returned for project-scoped discovery, including repeated user and policy actions.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub project_path: Option<String>,
    /// Human-readable source label, such as a hook file path, settings source, or plugin name.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source: Option<String>,
}

/// Models offered for agent conversations by one provider instance. Adapters exclude known-incompatible models, but retain candidates with unknown capabilities. Listing does not guarantee compatibility.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DiscoveredModelList {
    /// Provider-native models in provider order.
    pub models: Vec<DiscoveredModel>,
    /// Typed operation outcome.
    pub outcome: ModelProviderOperationOutcome,
}

/// Slash-prefixed command string to enqueue for FIFO processing.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct EnqueueCommandParams {
    /// Slash-prefixed command string to enqueue, e.g. '/compact' or '/model gpt-4'. Queued FIFO with any in-flight items; if the session is idle, processing kicks off immediately.
    pub command: String,
    /// Optional user-facing text for the queue row. The command string is shown when omitted.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub display_text: Option<String>,
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
pub struct EntraTokenAcquireRequestIdentity {
    /// Host account to revalidate. When supplied, the response must identify this exact account.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub account_id: Option<String>,
    /// Effective application client ID. Treat it as opaque.
    pub client_id: String,
    /// Entra identity or resource-token acquisition discriminator.
    pub kind: EntraTokenAcquireRequestIdentityKind,
    /// Interaction policy. Only a fresh explicit sign-in may select an unpinned account.
    pub reason: EntraTokenAcquireReason,
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
pub struct EntraTokenAcquireRequestToken {
    /// Pinned opaque host account ID. The response must identify this exact account.
    pub account_id: String,
    /// Target resource audience, when known.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub audience: Option<String>,
    /// Effective application client ID, including any resource-specific override.
    pub client_id: String,
    /// Entra identity or resource-token acquisition discriminator.
    pub kind: EntraTokenAcquireRequestTokenKind,
    /// Interaction policy. Renewal must remain silent.
    pub reason: EntraTokenAcquireReason,
    /// Scopes required for this resource token.
    pub scopes: Vec<String>,
    /// Bypass cached access tokens without changing account selection or interaction policy.
    pub skip_cached_access_token: bool,
    /// Target Entra tenant, when different from the host's default.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tenant: Option<String>,
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
pub struct EntraTokenAcquireResultIdentity {
    /// Stable nonblank opaque account ID owned by the host.
    pub account_id: String,
    /// Entra acquisition response variant discriminator.
    pub kind: EntraTokenAcquireResultIdentityKind,
    /// Optional display name; not an account identifier or credential.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub username: Option<String>,
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
pub struct EntraTokenAcquireResultToken {
    /// Nonblank resource access token. The host owns its caching and renewal.
    pub access_token: String,
    /// Exact host account ID supplied in the request.
    pub account_id: String,
    /// Entra acquisition response variant discriminator.
    pub kind: EntraTokenAcquireResultTokenKind,
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
pub struct EntraTokenAcquireResultUnavailable {
    /// Entra acquisition response variant discriminator.
    pub kind: EntraTokenAcquireResultUnavailableKind,
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
pub struct EntraTokenAcquireResultCancelled {
    /// Entra acquisition response variant discriminator.
    pub kind: EntraTokenAcquireResultCancelledKind,
}

/// Parameters for one connection-owned Entra acquisition callback.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct EntraTokenGetTokenRequest {
    /// Typed identity or resource-token acquisition request.
    pub request: EntraTokenAcquireRequest,
}

/// Hosting capabilities and session capacity advertised by an environment.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct EnvironmentCapabilities {
    /// Advertised Agent Host Protocol version.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub ahp_version: Option<String>,
    /// Current session count, when advertised.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub current_sessions: Option<i64>,
    /// Feature identifiers advertised by the environment.
    pub features: Vec<String>,
    /// Maximum session capacity, when advertised.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_sessions: Option<i64>,
}

/// Identify a user-managed Mission Control environment to delete.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct EnvironmentsDeleteRequest {
    /// User-managed environment to delete. GitHub-managed environments cannot be deleted.
    pub environment_id: String,
}

/// Acknowledgement that the requested environment was deleted.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct EnvironmentsDeleteResult {}

/// Identify a Mission Control environment to retrieve.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct EnvironmentsGetRequest {
    /// Identifier assigned by Mission Control.
    pub environment_id: String,
}

/// Safe discovery information. Host-side relay bootstrap credentials are never included.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct GitHubEnvironment {
    /// Hosting capabilities advertised by the environment.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub capabilities: Option<EnvironmentCapabilities>,
    /// Identifier assigned by Mission Control.
    pub id: String,
    /// Compute kind reported by Mission Control.
    pub kind: EnvironmentKind,
    /// Discovery labels attached to the environment.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub labels: Option<HashMap<String, String>>,
    /// Timestamp of the last heartbeat received by Mission Control.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub last_heartbeat_at: Option<String>,
    /// Human-readable environment name.
    pub name: String,
    /// Organization identifier, when the environment belongs to an organization.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub org_id: Option<String>,
    /// Identifier of the environment owner.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub owner_id: Option<String>,
    /// Owner category reported by Mission Control.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub owner_type: Option<String>,
    /// Open-ended operational status vocabulary.
    pub status: String,
}

/// Safe discovery information for the requested environment.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct EnvironmentsGetResult {
    /// The requested environment without relay bootstrap credentials.
    pub environment: GitHubEnvironment,
}

/// Optional discovery filters supported by GitHub Mission Control.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct EnvironmentsListRequest {
    /// Restrict discovery to this compute kind.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub kind: Option<EnvironmentKind>,
    /// Operational status, such as online, offline, degraded, waking, or draining.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub status: Option<String>,
}

/// Environments visible to the authenticated caller and matching the supplied filters.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct EnvironmentsListResult {
    /// Safe discovery records without relay bootstrap credentials.
    pub environments: Vec<GitHubEnvironment>,
    /// Server-requested minimum delay before automatically listing again, in milliseconds.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub retry_after_ms: Option<i64>,
}

/// Cursor, batch size, and optional long-poll/filter parameters for reading session events.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct EventLogReadRequest {
    /// Optional non-empty list of subagent identifiers. When provided, only events owned by one of these agents are returned; ownership recognizes the event envelope's agentId plus legacy data.agentId and data.parentToolCallId markers. This filter takes precedence over agentScope.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub agent_ids: Option<Vec<String>>,
    /// Agent-scope filter: 'primary' returns only main-agent events plus events whose type starts with 'subagent.' (matching the typed-subscription default behavior); 'all' returns events from all agents (matching wildcard-subscription behavior). Default is 'all' to preserve wildcard semantics for catch-up callers.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub agent_scope: Option<EventsAgentScope>,
    /// Opaque cursor returned by a previous read in the same view. Omit on the first call to start from the beginning of the selected view.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cursor: Option<String>,
    /// Direction to page through the session's persisted event history. 'forward' (default) pages from the cursor toward newer events (or from the start of history when no cursor is given). 'backward' enables tail-first reads: with no cursor it returns the NEWEST `max` events, and the returned cursor pages toward OLDER events on subsequent backward reads. Events within a returned batch are always in chronological (oldest-to-newest) order, even for a backward read. Backward reads cover PERSISTED history only; ephemeral events are never returned by a backward read. `direction` selects the INITIAL read only: the returned cursor is self-describing, so a continuation read pages in the cursor's own direction regardless of the `direction` passed alongside it — a forward cursor always pages forward and a backward cursor always pages backward. Pass the direction that matches the cursor to avoid confusion.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub direction: Option<EventsReadDirection>,
    /// When false, skip ephemeral events entirely and return only durable (persisted) events. History-backfill callers that discard ephemerals anyway should set this so the read is bounded by the durable log length instead of racing the ephemeral ring on a busy session. Defaults to true (ephemerals are interleaved with durable events in creation order). Ignored by backward reads, which always cover persisted history only.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub include_ephemeral: Option<bool>,
    /// Maximum number of events to return in this batch (1–1000, default 200).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max: Option<i64>,
    /// Either '*' to receive all event types, or a non-empty list of event types to receive
    #[serde(skip_serializing_if = "Option::is_none")]
    pub types: Option<serde_json::Value>,
    /// Milliseconds to wait for new events when the cursor is at the tail of history. 0 (default) returns immediately even if no events are available. Capped at 30000ms. Ephemeral events that arrive during the wait are delivered in this batch but are NOT replayable on a subsequent read (use a non-zero waitMs in your next call to capture future ephemerals as they happen). This applies to forward reads only: a backward read always returns immediately and ignores `waitMs`, because backward paging covers persisted history only while new events append at the tail (the opposite end from a backward page), so no blocking or ephemeral delivery can occur.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub wait_ms: Option<i32>,
}

/// Extensible [`EventLogReadRequest`], including inputs added after it was published.
///
/// Required inputs are [`EventLogReadOptions::new`] arguments; optional inputs have fluent setters.
/// Input-only: it serialises to the flat wire request and is not deserialisable.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct EventLogReadOptions {
    #[serde(flatten)]
    legacy: EventLogReadRequest,
    #[serde(skip_serializing_if = "Option::is_none")]
    view: Option<EventsReadView>,
}

impl EventLogReadOptions {
    /// Creates options with the required inputs.
    pub fn new() -> Self {
        Self {
            legacy: EventLogReadRequest {
                cursor: None,
                max: None,
                wait_ms: None,
                types: None,
                agent_scope: None,
                agent_ids: None,
                direction: None,
                include_ephemeral: None,
            },
            view: None,
        }
    }

    /// Opaque cursor returned by a previous read in the same view. Omit on the first call to start from the beginning of the selected view.
    pub fn cursor(mut self, value: impl Into<String>) -> Self {
        self.legacy.cursor = Some(value.into());
        self
    }

    /// Maximum number of events to return in this batch (1–1000, default 200).
    pub fn max(mut self, value: i64) -> Self {
        self.legacy.max = Some(value);
        self
    }

    /// Milliseconds to wait for new events when the cursor is at the tail of history. 0 (default) returns immediately even if no events are available. Capped at 30000ms. Ephemeral events that arrive during the wait are delivered in this batch but are NOT replayable on a subsequent read (use a non-zero waitMs in your next call to capture future ephemerals as they happen). This applies to forward reads only: a backward read always returns immediately and ignores `waitMs`, because backward paging covers persisted history only while new events append at the tail (the opposite end from a backward page), so no blocking or ephemeral delivery can occur.
    pub fn wait_ms(mut self, value: i32) -> Self {
        self.legacy.wait_ms = Some(value);
        self
    }

    /// Either '*' to receive all event types, or a non-empty list of event types to receive
    pub fn types(mut self, value: serde_json::Value) -> Self {
        self.legacy.types = Some(value);
        self
    }

    /// Agent-scope filter: 'primary' returns only main-agent events plus events whose type starts with 'subagent.' (matching the typed-subscription default behavior); 'all' returns events from all agents (matching wildcard-subscription behavior). Default is 'all' to preserve wildcard semantics for catch-up callers.
    pub fn agent_scope(mut self, value: EventsAgentScope) -> Self {
        self.legacy.agent_scope = Some(value);
        self
    }

    /// Optional non-empty list of subagent identifiers. When provided, only events owned by one of these agents are returned; ownership recognizes the event envelope's agentId plus legacy data.agentId and data.parentToolCallId markers. This filter takes precedence over agentScope.
    pub fn agent_ids(mut self, value: Vec<String>) -> Self {
        self.legacy.agent_ids = Some(value);
        self
    }

    /// Direction to page through the session's persisted event history. 'forward' (default) pages from the cursor toward newer events (or from the start of history when no cursor is given). 'backward' enables tail-first reads: with no cursor it returns the NEWEST `max` events, and the returned cursor pages toward OLDER events on subsequent backward reads. Events within a returned batch are always in chronological (oldest-to-newest) order, even for a backward read. Backward reads cover PERSISTED history only; ephemeral events are never returned by a backward read. `direction` selects the INITIAL read only: the returned cursor is self-describing, so a continuation read pages in the cursor's own direction regardless of the `direction` passed alongside it — a forward cursor always pages forward and a backward cursor always pages backward. Pass the direction that matches the cursor to avoid confusion.
    pub fn direction(mut self, value: EventsReadDirection) -> Self {
        self.legacy.direction = Some(value);
        self
    }

    /// When false, skip ephemeral events entirely and return only durable (persisted) events. History-backfill callers that discard ephemerals anyway should set this so the read is bounded by the durable log length instead of racing the ephemeral ring on a busy session. Defaults to true (ephemerals are interleaved with durable events in creation order). Ignored by backward reads, which always cover persisted history only.
    pub fn include_ephemeral(mut self, value: bool) -> Self {
        self.legacy.include_ephemeral = Some(value);
        self
    }

    /// Event stream to read. 'history' (default) returns original events and lazily follows predecessor journals, excluding checkpoint copies. 'replay' reads only the active replay head, including checkpoint state, without opening predecessor journals. Use replay for current state and fast transcript previews; use history to reconstruct the original visible transcript. Cursors must remain within the view that produced them.
    pub fn view(mut self, value: EventsReadView) -> Self {
        self.view = Some(value);
        self
    }
}

impl Default for EventLogReadOptions {
    fn default() -> Self {
        Self::new()
    }
}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct EventLogReleaseInterestResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// Snapshot of the current tail cursor without returning any events. Use this when a consumer wants to subscribe to live events going forward without first paginating through the entire persisted history (which would happen if `read` were called without a cursor on a long-lived session).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct EventLogTailResult {
    /// Opaque cursor pointing at the current tail of the session's persisted-events history. Pass back to `read` to receive only events that arrive AFTER this snapshot. When the session has no events, this returns the same sentinel as an unset cursor (i.e. equivalent to omitting the cursor on a first read).
    pub cursor: String,
}

/// Batch of session events returned by a read, with cursor and continuation metadata.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct EventsReadResult {
    /// Opaque cursor for the next read. Pass back unchanged in the next read.cursor to continue from where this read left off. Always present, even when no events were returned. For a backward read this cursor pages toward OLDER events; keep passing `direction: backward` with it (the cursor is also self-describing, so backward paging continues correctly).
    pub cursor: String,
    /// Cursor status: 'ok' means the cursor was applied successfully. For session.eventLog.read, 'expired' means the cursor referred to an event that no longer exists in active history and the read fell back to a boundary of the remaining history: the beginning for a forward read or the newest window for a backward read. That fallback may overlap already rendered events, so active-session consumers should reset, rebase, or deduplicate before continuing. sessions.readPersistedEvents has stricter snapshot semantics: 'expired' returns an empty terminal page and never switches to a replacement journal generation. Other persisted-read I/O failures are RPC errors with diagnostics, not cursor expiry.
    pub cursor_status: EventsCursorStatus,
    /// Session events for this batch, merged into a single stream in creation order: durable (persisted) events and ephemeral events interleave exactly as they were emitted. Set `includeEphemeral: false` to receive only durable events. Ephemeral events are never replayable once pruned from the in-memory ring, so a consumer that needs them should keep reading with a non-zero `waitMs`. For a backward (tail-first) read, the returned window contains persisted events only, still in chronological (oldest-to-newest) append order.
    pub events: Vec<SessionEvent>,
    /// True when more events are available in the read's direction. For a backward read, true means older persisted events remain before the returned window. A persisted-event page may contain fewer than `max` events because of its byte budget while still reporting hasMore true; continue according to this flag rather than the event count.
    pub has_more: bool,
}

/// Slash command name and argument string to execute synchronously.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExecuteCommandParams {
    /// Argument string to pass to the command (empty string if none).
    pub args: String,
    /// Name of the slash command to invoke (without the leading '/').
    pub command_name: String,
}

/// Error message produced while executing the command, if any.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExecuteCommandResult {
    /// Error message produced while executing the command, if any. Omitted when the handler succeeded.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
}

/// Expanded external tool result payload
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExternalToolTextResultForLlm {
    /// Base64-encoded binary results returned to the model
    #[serde(skip_serializing_if = "Option::is_none")]
    pub binary_results_for_llm: Option<Vec<ExternalToolTextResultForLlmBinaryResultsForLlm>>,
    /// Structured content blocks from the tool
    #[serde(skip_serializing_if = "Option::is_none")]
    pub contents: Option<Vec<serde_json::Value>>,
    /// Optional error message for failed executions
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Execution outcome classification. Optional for back-compat; normalized to 'success' (or 'failure' when error is present) when missing or unrecognized.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub result_type: Option<String>,
    /// Detailed log content for timeline display
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_log: Option<String>,
    /// Text result returned to the model
    pub text_result_for_llm: String,
    /// Tool references returned by a tool-search override: names of deferred tools to surface to the model. When set, the tool result is materialized as `tool_reference` content blocks (rather than plain text) so the model knows which deferred tools are now available.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_references: Option<Vec<String>>,
    /// Optional tool-specific telemetry
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_telemetry: Option<HashMap<String, serde_json::Value>>,
}

/// Audio content block with base64-encoded data
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExternalToolTextResultForLlmContentAudio {
    /// Base64-encoded audio data
    pub data: String,
    /// MIME type of the audio (e.g., audio/wav, audio/mpeg)
    pub mime_type: String,
    /// Content block type discriminator
    pub r#type: ExternalToolTextResultForLlmContentAudioType,
}

/// Image content block with base64-encoded data
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExternalToolTextResultForLlmContentImage {
    /// Base64-encoded image data
    pub data: String,
    /// MIME type of the image (e.g., image/png, image/jpeg)
    pub mime_type: String,
    /// Content block type discriminator
    pub r#type: ExternalToolTextResultForLlmContentImageType,
}

/// Embedded resource content block with inline text or binary data
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExternalToolTextResultForLlmContentResource {
    /// The embedded resource contents, either text or base64-encoded binary
    pub resource: serde_json::Value,
    /// Content block type discriminator
    pub r#type: ExternalToolTextResultForLlmContentResourceType,
}

/// Icon image for a resource
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExternalToolTextResultForLlmContentResourceLinkIcon {
    /// MIME type of the icon image
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mime_type: Option<String>,
    /// Available icon sizes (e.g., ['16x16', '32x32'])
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sizes: Option<Vec<String>>,
    /// URL or path to the icon image
    pub src: String,
    /// Theme variant this icon is intended for
    #[serde(skip_serializing_if = "Option::is_none")]
    pub theme: Option<ExternalToolTextResultForLlmContentResourceLinkIconTheme>,
}

/// Resource link content block referencing an external resource
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExternalToolTextResultForLlmContentResourceLink {
    /// Human-readable description of the resource
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Icons associated with this resource
    #[serde(skip_serializing_if = "Option::is_none")]
    pub icons: Option<Vec<ExternalToolTextResultForLlmContentResourceLinkIcon>>,
    /// MIME type of the resource content
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mime_type: Option<String>,
    /// Resource name identifier
    pub name: String,
    /// Size of the resource in bytes
    #[serde(skip_serializing_if = "Option::is_none")]
    pub size: Option<i64>,
    /// Human-readable display title for the resource
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
    /// Content block type discriminator
    pub r#type: ExternalToolTextResultForLlmContentResourceLinkType,
    /// URI identifying the resource
    pub uri: String,
}

/// Shell command exit metadata with optional output preview
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExternalToolTextResultForLlmContentShellExit {
    /// Working directory where the shell command was executed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cwd: Option<String>,
    /// Exit code from the completed shell command
    pub exit_code: i64,
    /// Path reported in the shell session's filesystem namespace when shell output exceeded the configured large-output threshold.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub output_file_path: Option<String>,
    /// Output associated with this shell command, if available. May be partial, truncated, or a preview; not guaranteed to be full output.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub output_preview: Option<String>,
    /// Whether outputPreview is known to be incomplete or truncated
    #[serde(skip_serializing_if = "Option::is_none")]
    pub output_truncated: Option<bool>,
    /// Shell id, as assigned by Copilot runtime
    pub shell_id: String,
    /// Content block type discriminator
    pub r#type: ExternalToolTextResultForLlmContentShellExitType,
}

/// Terminal/shell output content block with optional exit code and working directory
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExternalToolTextResultForLlmContentTerminal {
    /// Working directory where the command was executed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cwd: Option<String>,
    /// Process exit code, if the command has completed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub exit_code: Option<i64>,
    /// Terminal/shell output text
    pub text: String,
    /// Content block type discriminator
    pub r#type: ExternalToolTextResultForLlmContentTerminalType,
}

/// Plain text content block
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExternalToolTextResultForLlmContentText {
    /// The text content
    pub text: String,
    /// Content block type discriminator
    pub r#type: ExternalToolTextResultForLlmContentTextType,
}

/// Folder path to add to trusted folders.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct FolderTrustAddParams {
    /// Folder path to mark as trusted
    pub path: String,
}

/// Folder path to check for trust.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct FolderTrustCheckParams {
    /// Folder path to check
    pub path: String,
}

/// Folder trust check result.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct FolderTrustCheckResult {
    /// Whether the folder is trusted
    pub trusted: bool,
}

/// A folder to check against a persisted trusted-folders list, and the configuration directory holding that list.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct FolderTrustIsTrustedParams {
    /// Configuration directory to read the trusted-folders list from. Omit it to read the default location; supply it when the host has redirected its configuration, or the check answers from a list the host is not using.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub config_dir: Option<String>,
    /// Folder to check. A folder counts as trusted when it is itself listed, when an ancestor is listed, or when its git root is listed.
    pub path: String,
}

/// Selects the configuration directory whose machine-wide state to read.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GlobalStateLoadForConfigDirRequest {
    /// Copilot configuration directory to read the state document from, taking precedence over the server's own `COPILOT_HOME` and default home. Omit it, or pass an empty string, to read the directory the server resolved for itself.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub config_dir: Option<String>,
}

/// An account the host has signed in to, identified by the server it lives on and the login it uses there. The same person can appear more than once when they use both github.com and an Enterprise server.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct LoggedInUser {
    /// Source account this account was derived from, when one was recorded.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub derived_from: Option<String>,
    /// Host the account belongs to, such as `github.com` or an Enterprise server.
    pub host: String,
    /// Account kind, when the host recorded one. Consumers must tolerate new strings.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub kind: Option<String>,
    /// Account login on that host.
    pub login: String,
}

/// The host's machine-wide state. Every field is optional because a fresh install has recorded nothing yet, so a reader must treat an absent field as `not yet`, never as a negative answer. Stored credentials are deliberately absent from this shape.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GlobalStateLoadResult {
    /// Whether the app tip has been shown.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub app_tip_shown: Option<bool>,
    /// Terminals the user has already been asked to set up, so the host does not ask twice.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub asked_setup_terminals: Option<Vec<String>>,
    /// When the Auto-feedback hint was last shown, as an ISO 8601 timestamp. It enforces the once-per-day cap for non-staff users across restarts.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auto_feedback_last_prompted_at: Option<String>,
    /// When the host first ran on this machine.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub first_launch_at: Option<String>,
    /// Plugins installed on this machine.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub installed_plugins: Option<Vec<InstalledPlugin>>,
    /// Account used for the most recent sign-in.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) last_logged_in_user: Option<LoggedInUser>,
    /// Every account the host has signed in to on this machine.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) logged_in_users: Option<Vec<LoggedInUser>>,
    /// Whether the one-off cleanup of stored reasoning summaries has run.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_summaries_cleanup_done: Option<bool>,
    /// Models the user selected recently, most recent first.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub recent_model_ids: Option<Vec<String>>,
    /// Whether the user declined to trust the sandbox credential proxy CA.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sandbox_credential_proxy_ca_declined: Option<bool>,
    /// Whether the sandbox onboarding has been shown.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sandbox_onboarding_shown: Option<bool>,
    /// Whether the user is a GitHub or Microsoft staff member, which unlocks internal-only behavior.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub staff: Option<bool>,
    /// Whether the user was recognized as GitHub staff.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub staff_github: Option<bool>,
    /// When the staff-only log level migration last ran.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub staff_log_level_migration_at: Option<String>,
    /// Whether the user was recognized as Microsoft staff.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub staff_microsoft: Option<bool>,
    /// When the staff-only model reset last ran.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub staff_model_reset_at: Option<String>,
    /// When the staff-only update channel migration last ran.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub staff_update_channel_migration_at: Option<String>,
    /// Folders where the user declined the init prompt, so it stays hidden there.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub suppress_init_folders: Option<Vec<String>>,
    /// Folders the user has marked as trusted.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub trusted_folders: Option<Vec<String>>,
}

/// A single top-level key to record in the host's machine-wide state. The write replaces only that key and leaves the rest of the document untouched, so two writers recording different one-off flags do not overwrite each other. The stored credential keys cannot be written through this method.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GlobalStateWriteKeyRequest {
    /// Copilot configuration directory to write the state document in, taking precedence over the server's own `COPILOT_HOME` and default home. Omit it, or pass an empty string, to write the directory the server resolved for itself. Mirrors `globalState.loadForConfigDir`, so a caller can read and write the same directory.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub config_dir: Option<String>,
    /// Top-level key to write, named as it appears in the result of `globalState.load`. It must be one of the writable keys that `globalState.writeKey` lists.
    pub key: String,
    /// Value to store for the key. Omit it, or pass null, to remove the key instead.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub value: Option<serde_json::Value>,
}

/// Pending external tool call request ID, with the tool result or an error describing why it failed.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HandlePendingToolCallRequest {
    /// Error message if the tool call failed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Request ID of the pending tool call
    pub request_id: RequestId,
    /// Tool call result (string or expanded result object)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub result: Option<serde_json::Value>,
}

/// Indicates whether the external tool call result was handled successfully.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HandlePendingToolCallResult {
    /// Whether the tool call result was handled successfully
    pub success: bool,
}

/// Indicates whether an in-progress manual compaction was aborted.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HistoryAbortManualCompactionResult {
    /// Whether an in-progress manual compaction was aborted. False when no manual compaction was running, when its abort controller was already aborted, or when the session is remote.
    pub aborted: bool,
}

/// Indicates whether an in-progress background compaction was cancelled.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HistoryCancelBackgroundCompactionResult {
    /// Whether an in-progress background compaction was cancelled. False when no compaction was running, when the session is remote, or when the underlying processor was unavailable.
    pub cancelled: bool,
}

/// Parameters for clearing the conversation and seeding the window that replaces it.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HistoryClearContextRequest {
    /// First user message of the fresh context window. Required: a cleared window holding only system and developer messages is not a conversation a model can answer, so every clear seeds the window it creates. Delivered by the enclosing turn driver once the agentic loop exits, which is why the call must be made from inside a tool handler.
    pub prompt: String,
}

/// What a successful clear removed. A clear that could not be applied rejects instead of reporting a count.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HistoryClearContextResult {
    /// Number of non-system, non-developer messages that were removed from the conversation. Zero only when the window already held no conversation.
    pub messages_cleared: i64,
}

/// Optional compaction parameters.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HistoryCompactRequest {
    /// Optional user-provided instructions to focus the compaction summary
    #[serde(skip_serializing_if = "Option::is_none")]
    pub custom_instructions: Option<String>,
    /// Context window token limit this compaction is targeting, recorded as the `tokenLimit` on the persisted `session.compaction_start` / `session.compaction_complete` events. Set it when the compaction targets a window other than the compacting model's own, e.g. switching to a model with a smaller context window: the compaction still runs on the current model, so the limit that motivated it would otherwise be lost. When absent, the events record the compacting model's own resolved limit. Attribution metadata only - it does not change how much the compaction removes.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub token_limit: Option<i64>,
    /// What initiated this compaction request, recorded as the `trigger` on the persisted `session.compaction_start` / `session.compaction_complete` events. When absent, the compaction is persisted without trigger attribution (initiator unknown).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub trigger: Option<HistoryCompactRequestTrigger>,
}

/// Compaction outcome with the number of tokens and messages removed, summary text, and the resulting context window breakdown.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HistoryCompactResult {
    /// Post-compaction context window usage breakdown
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context_window: Option<HistoryCompactContextWindow>,
    /// Number of messages removed during compaction
    pub messages_removed: i64,
    /// Whether compaction completed successfully
    pub success: bool,
    /// Summary text produced by compaction. Omitted when compaction did not produce a summary (e.g. failure path).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub summary_content: Option<String>,
    /// Number of tokens freed by compaction
    pub tokens_removed: i64,
}

/// Rewind points and file-change-tracking availability for the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HistoryListRewindPointsResult {
    /// Whether this session captured file changes from its first turn.
    pub file_change_tracking_enabled: bool,
    /// Root user turns in chronological order. Empty when `unavailableReason` is set.
    pub points: Vec<HistoryRewindPoint>,
    /// Why the listed points could not be produced, when applicable; the points list is empty whenever it is set. `unsupported-remote-session` is permanent for the session and comes with `fileChangeTrackingEnabled: false`. `session-busy` is transient and only ever reported by a session that *is* tracking (`fileChangeTrackingEnabled: true`), because the file-change captures cannot be read while work that may still mutate them is in flight; the same request succeeds once the session settles, so a client that wants points should retry rather than treat it as a failure. It is never `file-change-tracking-disabled`: an untracked local session still lists conversation-only points and reports that through `fileChangeTrackingEnabled: false`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub unavailable_reason: Option<HistoryRewindUnavailableReason>,
}

/// Event boundary to preview for conversation-and-files rewind.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HistoryPreviewRewindRequest {
    /// ID of the user.message event that begins the discarded suffix.
    pub event_id: String,
}

/// Files and aggregate changes for a prospective rewind.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HistoryPreviewRewindResult {
    /// Whether file restore is available for this session. This is authoritative: switch on it and read `reason` only when it is false.
    pub available: bool,
    /// Number of unique files in the preview.
    pub file_count: i64,
    /// Files ordered by path.
    pub files: Vec<HistoryRewindFilePreview>,
    /// Why file restore is unavailable, when applicable. Populated only when `available` is false and never set when `available` is true.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reason: Option<HistoryRewindUnavailableReason>,
}

/// Boundary and mode for rewinding session history.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HistoryRewindRequest {
    /// ID of the user.message event that begins the discarded suffix.
    pub event_id: String,
    /// Whether to rewind only conversation history or also restore captured files.
    pub mode: HistoryRewindMode,
}

/// Structured outcome of a rewind request.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HistoryRewindResult {
    /// Failure detail. Set only for the failure and partial-failure outcomes (`files-rolled-back`, `rollback-incomplete`, `truncation-failed`, `checkpoint-cleanup-failed`, `snapshot-prune-failed`); omitted for `success` and for the unavailable outcomes (`session-busy`, `file-change-tracking-disabled`, `unsupported-remote-session`).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Number of persisted events removed by conversation truncation. Present only when truncation succeeded (outcomes `success`, `checkpoint-cleanup-failed`, and `snapshot-prune-failed`); omitted for every unavailable outcome (`session-busy`, `file-change-tracking-disabled`, `unsupported-remote-session`) and for `truncation-failed`, `files-rolled-back`, and `rollback-incomplete`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub events_removed: Option<i64>,
    /// Overall rewind outcome. This discriminates the result: it governs which of the remaining fields are populated, so consumers must switch on it before reading `eventsRemoved`, `restoredFiles`, `skippedFiles`, or `error`. See each field for the outcomes that populate it.
    pub outcome: HistoryRewindOutcome,
    /// Absolute paths restored to their captured preimages. Always empty for conversation-only rewinds and for the unavailable outcomes (`session-busy`, `file-change-tracking-disabled`, `unsupported-remote-session`); only conversation-and-files outcomes that reached the file-restore stage populate it.
    pub restored_files: Vec<String>,
    /// Captured files intentionally left unchanged. Always empty for conversation-only rewinds and for the unavailable outcomes (`session-busy`, `file-change-tracking-disabled`, `unsupported-remote-session`); only conversation-and-files outcomes that reached the file-restore stage populate it.
    pub skipped_files: Vec<HistorySkippedFileRestore>,
}

/// Markdown summary of the conversation context (empty when not available).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HistorySummarizeForHandoffResult {
    /// Markdown summary of the conversation context produced by an LLM. Empty string when there are no messages or when the session does not support local summarization.
    pub summary: String,
}

/// Identifier of the event to truncate to; this event and all later events are removed.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HistoryTruncateRequest {
    /// Event ID to truncate to. This event and all events after it are removed from the session.
    pub event_id: String,
}

/// Number of events that were removed by the truncation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HistoryTruncateResult {
    /// Failure detail when checkpointCleanupFailed is true.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub checkpoint_cleanup_error: Option<String>,
    /// True when conversation truncation succeeded but post-truncation workspace checkpoint cleanup failed. History is already truncated; callers may still prune snapshots but should report a checkpoint-cleanup rather than a truncation failure.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub checkpoint_cleanup_failed: Option<bool>,
    /// Number of events that were removed
    pub events_removed: i64,
}

/// Runtime-owned wire payload for a server-to-client hook callback invocation.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct HookInvokeRequest {
    pub hook_type: HookType,
    pub input: serde_json::Value,
    pub session_id: SessionId,
}

/// Optional output returned by an SDK callback hook.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct HookInvokeResponse {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub output: Option<serde_json::Value>,
}

/// Optional project paths and host-exclusion behavior for server-scoped hook discovery.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HooksDiscoverRequest {
    /// When true, omit host-owned user and plugin hook rows and their diagnostics. Managed-policy hooks and trusted repository hooks remain visible, and host disabledHooks still contribute to each remaining row's effective enabled state. This filters sources rather than simulating a host with no settings.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub exclude_host_hooks: Option<bool>,
    /// Optional project directory paths whose trusted repository and project-expanded plugin hooks should be discovered. When omitted or empty, user, managed-policy, and globally enabled installed or explicit plugin hooks are returned without project expansion.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub project_paths: Option<Vec<String>>,
}

/// Extensible [`HooksDiscoverRequest`], including inputs added after it was published.
///
/// Required inputs are [`HooksDiscoverOptions::new`] arguments; optional inputs have fluent setters.
/// Input-only: it serialises to the flat wire request and is not deserialisable.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct HooksDiscoverOptions {
    #[serde(flatten)]
    legacy: HooksDiscoverRequest,
    #[serde(skip_serializing_if = "Option::is_none")]
    hook_directories: Option<Vec<String>>,
}

impl HooksDiscoverOptions {
    /// Creates options with the required inputs.
    pub fn new() -> Self {
        Self {
            legacy: HooksDiscoverRequest {
                project_paths: None,
                exclude_host_hooks: None,
            },
            hook_directories: None,
        }
    }

    /// Optional project directory paths whose trusted repository and project-expanded plugin hooks should be discovered. When omitted or empty, user, managed-policy, and globally enabled installed or explicit plugin hooks are returned without project expansion.
    pub fn project_paths(mut self, value: Vec<String>) -> Self {
        self.legacy.project_paths = Some(value);
        self
    }

    /// When true, omit host-owned user and plugin hook rows and their diagnostics. Managed-policy hooks and trusted repository hooks remain visible, and host disabledHooks still contribute to each remaining row's effective enabled state. This filters sources rather than simulating a host with no settings.
    pub fn exclude_host_hooks(mut self, value: bool) -> Self {
        self.legacy.exclude_host_hooks = Some(value);
        self
    }

    /// Explicit discovery-only file-hook roots, separate from callback hooks and filesystem grants. Available without ambient discovery or repository file-hook enablement, subject to managed/hook disables. Relative paths resolve against the request's effective working directory when supplied. Null and omission are equivalent; an empty array contributes no explicit roots.
    pub fn hook_directories(mut self, value: Vec<String>) -> Self {
        self.hook_directories = Some(value);
        self
    }
}

impl Default for HooksDiscoverOptions {
    fn default() -> Self {
        Self::new()
    }
}

/// Server-discovered hook actions and partial-load diagnostics from user, repository, plugin, and managed-policy sources. Concrete sessions may include additional session-specific hook sources.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HooksDiscoverResult {
    /// Errors for hook sources or actions that could not be loaded, making the result partially incomplete. Other valid actions are still returned. Project-resolution and repository-settings errors are prefixed with their project path.
    pub errors: Vec<String>,
    /// All discovered hook actions. Byte-identical actions remain separate rows even when they share a disable key.
    pub hooks: Vec<DiscoveredHook>,
    /// Non-fatal source-loading warnings. Discovery remains complete for the affected source, although the source had a recoverable issue. Repository-settings warnings are prefixed with their project path when attribution is available.
    pub warnings: Vec<String>,
}

/// A decrypted credential presented by a supervised hosting participant.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HostAuthenticateRequest {
    /// Unique validation attempt, scoped to this host.
    pub request_id: RequestId,
    /// Decrypted admission token, never an inference credential.
    pub token: String,
}

/// Notifies the owning application that validation ended.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HostAuthenticationCancelledNotification {
    /// Owning listener UUID.
    pub host_id: String,
    /// Validation attempt whose signal must be aborted.
    pub request_id: RequestId,
}

/// A validated credential with a required, future RFC3339 expiration.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HostAuthenticationResult {
    /// Required future expiration in RFC3339 format.
    pub expires_at: String,
    /// Stable identity, required to match the configured owner exactly.
    pub identity: String,
}

/// Cancels a pending validation on the supervised participant's connection.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HostCancelAuthenticationRequest {
    /// Validation attempt to cancel.
    pub request_id: RequestId,
}

/// GitHub Mission Control registration options. The compute ID is application-owned and stable.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HostGitHubEnvironmentOptions {
    /// Stable application installation identity, reused across host restarts.
    pub compute_id: String,
    /// Human-readable environment display name.
    pub name: String,
    /// Require sealed, connection-bound authentication (default true), independent of listener address. False permits unsealed direct credentials and unbound sealed relay credentials; use only when the caller fully controls and trusts the direct transport. Relay token encryption remains mandatory.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub require_connection_binding: Option<bool>,
}

/// Normalized local WebSocket listener settings.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HostLocalServerConfiguration {
    /// Hostname or IP address to bind.
    pub hostname: String,
    /// Port to bind, with zero requesting OS allocation.
    pub port: i32,
    /// Whether the listener requires token authentication.
    pub require_connection_token: bool,
    /// Secret connection token, absent when authentication is disabled.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub token: Option<String>,
}

/// Normalized listener settings delivered only to the supervised hosting participant.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HostConfiguration {
    /// Initial remote creation policy, independent of application session factories. Defaults to denied when omitted.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub allow_creating_new_sessions: Option<bool>,
    /// Explicit host-wide admission policy. Always supplied by the runtime after validating host.start.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub authentication: Option<serde_json::Value>,
    /// Requested GitHub Mission Control registration.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub github_environment: Option<HostGitHubEnvironmentOptions>,
    /// Normalized local listener settings, absent for relay-only hosts.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub local_server: Option<HostLocalServerConfiguration>,
    /// Whether app-owned durable sessions are resumed by the owning application.
    pub resume_factory: bool,
    /// Whether session materialization is delegated to the owning application.
    pub session_factory: bool,
}

/// Cached error-reporting policy for the runtime. This data does not grant ownership or confirm network delivery.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HostDiagnostics {
    /// Whether host error reporting awaits policy, is enabled, or is disabled.
    pub error_reporting: HostErrorReportingStatus,
}

/// Stops a connection-owned listener and joins its teardown.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HostDisposeRequest {
    /// Listener UUID. Unknown or successfully stopped IDs are harmless.
    pub host_id: String,
}

/// Empty acknowledgement for a completed host lifecycle operation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HostEmptyResult {}

/// Private credentials delivered only to a runtime-owned Mission Control hosting participant.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HostEnvironmentCredentials {
    /// GitHub API base URL for the authenticated service.
    pub github_api_url: String,
    /// Hostname of the authenticated GitHub service.
    pub github_host: String,
    /// Mission Control API origin for environment registration and management.
    pub mission_control_url: String,
    /// Current bearer token for the authenticated GitHub identity.
    pub token: String,
}

/// Reports a supervised listener's hosting-task termination and cleanup outcome.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HostExitedNotification {
    /// Explicit startup or teardown failure, when present.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Process exit status when available; absent for in-process listener tasks.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub exit_code: Option<i64>,
    /// Listener UUID.
    pub host_id: String,
    /// Cause of termination.
    pub reason: HostExitReason,
}

/// Reads a running host's catalog or the runtime's saved default catalog.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HostListSessionsRequest {
    /// Listener UUID returned by host.start; the caller must own it. Omit to read
    /// the saved default compute-identity catalog without starting a listener.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub host_id: Option<String>,
}

/// Metadata advertised by the AHP host, not a guarantee that history can be resumed.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HostSessionSummary {
    /// Optional activity description advertised by the host.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub activity: Option<String>,
    /// Session creation time as an ISO 8601 timestamp.
    pub created_at: String,
    /// Last modification time as an ISO 8601 timestamp.
    pub modified_at: String,
    /// Stable AHP resource URI identifying the session on this host.
    pub resource: String,
    /// Unsigned 32-bit AHP session status bitset (0..=4294967295); unknown bits must be preserved.
    pub status: i64,
    /// Authoritative title advertised by the host.
    pub title: String,
}

/// The complete live and dormant advertised catalog, without subscribing to sessions.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HostListSessionsResult {
    /// Complete advertised session summaries, in the host's catalog order.
    pub sessions: Vec<HostSessionSummary>,
}

/// Local WebSocket transport options.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HostLocalServerOptions {
    /// Listener hostname. Defaults to 127.0.0.1; explicit non-loopback binds are allowed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub hostname: Option<String>,
    /// Listener port. Omitted or zero requests an OS-allocated port.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub port: Option<i32>,
    /// Require token authentication (default true). Cannot be false with a token.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub require_connection_token: Option<bool>,
    /// Nonempty connection token. Generated randomly when required and omitted.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub token: Option<String>,
}

/// Publishes a resident session attached to the listener's owning connection.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HostPublishSessionRequest {
    /// Listener UUID returned by host.start.
    pub host_id: String,
    /// Canonical runtime session ID attached to the listener's owning connection.
    pub session_id: SessionId,
}

/// The existing runtime identity and its resource on the listener.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HostPublishSessionResult {
    /// Canonical runtime ID of the published session.
    pub session_id: SessionId,
    /// AHP resource URI for the session on this listener.
    pub session_uri: String,
}

/// Readiness reported by the supervised hosting participant on its own SDK connection.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HostReadyRequest {
    /// Actual bound WebSocket URL.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub address: Option<String>,
    /// Registered environment ID, reported only once the relay transport is connected.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub environment_id: Option<String>,
    /// Configured secret token, absent when authentication is disabled.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub token: Option<String>,
}

/// Listener-scoped registration, not a copy or durable adoption of a session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HostRegisterSessionRequest {
    /// Additional directories already granted to the resident session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub additional_directories: Option<Vec<String>>,
    /// Session creation time in milliseconds since the Unix epoch, when available.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub created_at_unix_ms: Option<i64>,
    /// Last session modification time in milliseconds since the Unix epoch, when available.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub modified_at_unix_ms: Option<i64>,
    /// Canonical ID of the existing resident runtime session.
    pub session_id: SessionId,
    /// Current display title of the resident session, when available.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
    /// Absolute working directory of the resident session.
    pub working_directory: String,
}

/// Application callback routed over its existing SDK connection.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HostSessionCreateCallback {
    /// Trusted admission identity, separate from inference credentials. Always supplied by the runtime for admitted handoffs.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub authentication: Option<serde_json::Value>,
    /// Host-selected SDK creation or resume settings, without executable callbacks or tools.
    pub config: HashMap<String, serde_json::Value>,
    /// Unique identity of the session participation being requested.
    pub handoff_id: String,
    /// Listener UUID identifying the owning application's host.
    pub host_id: String,
    /// Resume an app-owned durable session instead of creating a new session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub resume: Option<bool>,
}

/// One application-owned session handoff, requested by the supervised hosting participant.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HostSessionCreateRequest {
    /// Trusted admission identity supplied by the hosting participant. Required by the runtime before a handoff is admitted.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub authentication: Option<serde_json::Value>,
    /// Host-selected SDK creation or resume settings, without executable callbacks or tools.
    pub config: HashMap<String, serde_json::Value>,
    /// Unique identity for this participation, independent of the session lifetime.
    pub handoff_id: String,
    /// For an explicit publication, borrow an existing owning-connection resident
    /// instead of invoking the resume factory. Cold sessions still use the factory.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub prefer_resident: Option<bool>,
    /// Resume an app-owned durable session instead of creating a new session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub resume: Option<bool>,
}

/// The resident session the application has materialized on its own connection.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HostSessionCreateResult {
    /// Runtime session UUID materialized on the application's SDK connection.
    pub session_id: SessionId,
}

/// Releases the original application session object retained for one handoff.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HostSessionReleasedNotification {
    /// Identity of the handoff retaining the original application session object.
    pub handoff_id: String,
    /// Listener UUID whose application session participation ended.
    pub host_id: String,
}

/// Ends one participation, not the application's session lifetime.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HostSessionReleaseRequest {
    /// Identity of the participation to release without destroying the session.
    pub handoff_id: String,
}

/// Changes remote session creation admission without disconnecting clients.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HostSetSessionCreationPolicyRequest {
    /// After acknowledgement, subsequent remote creation admissions use this policy. Already-admitted requests may finish.
    pub allow_creating_new_sessions: bool,
    /// Listener UUID owned by the calling SDK connection.
    pub host_id: String,
}

/// Starts a supervised AHP host with at least one explicitly selected transport.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HostStartRequest {
    /// Registers a GitHub Mission Control environment and enables its relay transport.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub github_environment: Option<HostGitHubEnvironmentOptions>,
    /// Caller-generated UUID identifying this connection-owned listener.
    pub host_id: String,
    /// Enables a local WebSocket listener.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub local_server: Option<HostLocalServerOptions>,
    /// Ask the owning application to resume its durable AHP sessions.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub resume_factory: Option<bool>,
    /// Ask the owning SDK application to materialize AHP sessions.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_factory: Option<bool>,
}

/// Extensible [`HostStartRequest`], including inputs added after it was published.
///
/// Required inputs are [`HostStartOptions::new`] arguments; optional inputs have fluent setters.
/// Input-only: it serialises to the flat wire request and is not deserialisable.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct HostStartOptions {
    #[serde(flatten)]
    legacy: HostStartRequest,
    #[serde(skip_serializing_if = "Option::is_none")]
    allow_creating_new_sessions: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    authentication: Option<serde_json::Value>,
    #[serde(skip_serializing_if = "Option::is_none")]
    compute_id: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    workspace: Option<String>,
}

impl HostStartOptions {
    /// Creates options with the required inputs.
    pub fn new(host_id: impl Into<String>) -> Self {
        Self {
            legacy: HostStartRequest {
                host_id: host_id.into(),
                local_server: None,
                github_environment: None,
                session_factory: None,
                resume_factory: None,
            },
            allow_creating_new_sessions: None,
            authentication: None,
            compute_id: None,
            workspace: None,
        }
    }

    /// Enables a local WebSocket listener.
    pub fn local_server(mut self, value: HostLocalServerOptions) -> Self {
        self.legacy.local_server = Some(value);
        self
    }

    /// Registers a GitHub Mission Control environment and enables its relay transport.
    pub fn github_environment(mut self, value: HostGitHubEnvironmentOptions) -> Self {
        self.legacy.github_environment = Some(value);
        self
    }

    /// Ask the owning SDK application to materialize AHP sessions.
    pub fn session_factory(mut self, value: bool) -> Self {
        self.legacy.session_factory = Some(value);
        self
    }

    /// Ask the owning application to resume its durable AHP sessions.
    pub fn resume_factory(mut self, value: bool) -> Self {
        self.legacy.resume_factory = Some(value);
        self
    }

    /// Whether remote clients may create new sessions. Defaults to denied when omitted. Does not restrict owner work, publication, or existing sessions.
    pub fn allow_creating_new_sessions(mut self, value: bool) -> Self {
        self.allow_creating_new_sessions = Some(value);
        self
    }

    /// Explicit admission policy, required by the runtime before hosting. Optional in the wire DTO only to preserve generated client construction.
    pub fn authentication(mut self, value: serde_json::Value) -> Self {
        self.authentication = Some(value);
        self
    }

    /// Stable application identity selecting its durable host catalog. Defaults to the persisted runtime compute GUID; must agree with githubEnvironment.computeId when both are supplied.
    pub fn compute_id(mut self, value: impl Into<String>) -> Self {
        self.compute_id = Some(value.into());
        self
    }

    /// Absolute directory on the runtime host to serve through AHP. Defaults to the runtime's configured working directory. Does not change process or session working directories.
    pub fn workspace(mut self, value: impl Into<String>) -> Self {
        self.workspace = Some(value.into());
        self
    }
}

/// Listener readiness, returned only after binding and the supervised participant's SDK handshake.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HostStartResult {
    /// GitHub Mission Control environment ID, present when its relay transport is ready.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub environment_id: Option<String>,
    /// Caller-generated listener UUID.
    pub host_id: String,
    /// Separate host process ID, when provided by a legacy runtime. Absent for in-process listeners.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub pid: Option<i64>,
    /// Secret connection token, absent when authentication is disabled.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub token: Option<String>,
    /// Actual bound WebSocket URL, including the allocated port.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub url: Option<String>,
}

/// Withdraws an application publication without deleting its underlying local session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HostUnpublishSessionRequest {
    /// Listener UUID returned by host.start; the caller must own the live listener.
    pub host_id: String,
    /// Canonical session ID in the durable catalog, whether resident or dormant.
    pub session_id: SessionId,
}

/// Publication withdrawal outcome; local history, worktrees and ongoing work are preserved.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HostUnpublishSessionResult {
    /// True when a publication was removed; false when it was already absent.
    pub unpublished: bool,
}

/// Removes a durable application publication and revokes its AHP attachments.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HostUnregisterSessionRequest {
    /// Canonical ID of the application publication to withdraw.
    pub session_id: SessionId,
}

/// Application-owned token validation callback.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HostValidateAuthenticationRequest {
    /// Owning listener UUID.
    pub host_id: String,
    /// Unique validation attempt.
    pub request_id: RequestId,
    /// Decrypted admission token.
    pub token: String,
}

/// Source descriptor for a direct GitHub plugin install, with `owner/repo`, optional ref or full commit SHA, and optional subpath.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InstalledPluginSourceGitHub {
    /// Optional repository-relative path to the plugin.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub path: Option<String>,
    /// Optional Git ref to resolve.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub r#ref: Option<String>,
    /// GitHub repository in `owner/repo` form.
    pub repo: String,
    /// Optional full 40-character hexadecimal commit SHA.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sha: Option<String>,
    /// Constant value. Always "github".
    pub source: InstalledPluginSourceGitHubSource,
}

/// Source descriptor for a direct local plugin install, with a local filesystem path.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InstalledPluginSourceLocal {
    /// Local filesystem path to the plugin.
    pub path: String,
    /// Constant value. Always "local".
    pub source: InstalledPluginSourceLocalSource,
}

/// Source descriptor for a direct URL plugin install, with URL, optional ref or full commit SHA, and optional subpath.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InstalledPluginSourceUrl {
    /// Optional source-relative path to the plugin.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub path: Option<String>,
    /// Optional Git ref to resolve.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub r#ref: Option<String>,
    /// Optional full 40-character hexadecimal commit SHA.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sha: Option<String>,
    /// Constant value. Always "url".
    pub source: InstalledPluginSourceUrlSource,
    /// URL of the plugin source.
    pub url: String,
}

/// Canonical file or directory where custom instructions can be discovered or created, with location, kind, preference, and project path.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InstructionDiscoveryPath {
    /// Whether the target is a single file or a directory of instruction files
    pub kind: InstructionDiscoveryPathKind,
    /// Which tier this target belongs to
    pub location: InstructionDiscoveryPathLocation,
    /// Absolute path of the file or directory (may not exist on disk yet)
    pub path: String,
    /// Whether this is the canonical target to create new instructions in its tier. At most one entry per tier is preferred.
    pub preferred_for_creation: bool,
    /// The input project path this target was derived from (only for repository targets)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub project_path: Option<String>,
}

/// Canonical files and directories where custom instructions can be created so the runtime will recognize them.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InstructionDiscoveryPathList {
    /// Canonical instruction create/discovery files and directories, in priority order
    pub paths: Vec<InstructionDiscoveryPath>,
}

/// Explicit discovery arguments for one location, supplied by the caller rather than derived from a session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct InstructionsDiscoverRepoSourcesRequest {
    /// Extra directories to scan alongside instruction directories configured in the runtime environment.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub additional_dirs: Option<Vec<String>>,
    /// Copilot configuration directory for user-level sources. An omitted or empty value uses the server's configuration home.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub config_dir: Option<String>,
    /// Working directory whose instruction sources should also be included. Omission adds no separate working-directory scope.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cwd_path: Option<String>,
    /// When true, also discover instruction files nested below the location.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_child_instructions: Option<bool>,
    /// When true, include the sources a command-line host reads. Hosts that are not the CLI omit it.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub is_cli: Option<bool>,
    /// Repository location to discover. An empty string selects no repository scope; cwdPath can still supply working-directory sources.
    pub location: String,
}

/// Optional project paths to include in instruction discovery.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InstructionsDiscoverRequest {
    /// When true, omit the host's instruction sources (user/home-level files and plugin rules), leaving only repository and working-directory sources. For multitenant deployments.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub exclude_host_instructions: Option<bool>,
    /// Optional list of project directory paths to scan for repository/working-directory instruction sources. When omitted or empty, only user-level and plugin instruction sources are returned (no project scan).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub project_paths: Option<Vec<String>>,
}

/// Extensible [`InstructionsDiscoverRequest`], including inputs added after it was published.
///
/// Required inputs are [`InstructionsDiscoverOptions::new`] arguments; optional inputs have fluent setters.
/// Input-only: it serialises to the flat wire request and is not deserialisable.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct InstructionsDiscoverOptions {
    #[serde(flatten)]
    legacy: InstructionsDiscoverRequest,
    #[serde(skip_serializing_if = "Option::is_none")]
    instruction_directories: Option<Vec<String>>,
}

impl InstructionsDiscoverOptions {
    /// Creates options with the required inputs.
    pub fn new() -> Self {
        Self {
            legacy: InstructionsDiscoverRequest {
                project_paths: None,
                exclude_host_instructions: None,
            },
            instruction_directories: None,
        }
    }

    /// Optional list of project directory paths to scan for repository/working-directory instruction sources. When omitted or empty, only user-level and plugin instruction sources are returned (no project scan).
    pub fn project_paths(mut self, value: Vec<String>) -> Self {
        self.legacy.project_paths = Some(value);
        self
    }

    /// When true, omit the host's instruction sources (user/home-level files and plugin rules), leaving only repository and working-directory sources. For multitenant deployments.
    pub fn exclude_host_instructions(mut self, value: bool) -> Self {
        self.legacy.exclude_host_instructions = Some(value);
        self
    }

    /// Explicit discovery-only instruction roots using existing SDK instruction formats, not filesystem grants. Available without ambient discovery, subject to instruction/managed disables. Relative paths resolve against the request's effective working directory when supplied. Null and omission are equivalent; an empty array contributes no explicit roots.
    pub fn instruction_directories(mut self, value: Vec<String>) -> Self {
        self.instruction_directories = Some(value);
        self
    }
}

impl Default for InstructionsDiscoverOptions {
    fn default() -> Self {
        Self::new()
    }
}

/// Optional project paths to include when enumerating instruction discovery targets.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InstructionsGetDiscoveryPathsRequest {
    /// When true, omit the host's user-level instruction targets, leaving only repository targets. For multitenant deployments (mirrors `discover`'s `excludeHostInstructions`).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub exclude_host_instructions: Option<bool>,
    /// Optional list of project directory paths. When omitted or empty, only the user-level targets are returned.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub project_paths: Option<Vec<String>>,
}

/// Extensible [`InstructionsGetDiscoveryPathsRequest`], including inputs added after it was published.
///
/// Required inputs are [`InstructionsGetDiscoveryPathsOptions::new`] arguments; optional inputs have fluent setters.
/// Input-only: it serialises to the flat wire request and is not deserialisable.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct InstructionsGetDiscoveryPathsOptions {
    #[serde(flatten)]
    legacy: InstructionsGetDiscoveryPathsRequest,
    #[serde(skip_serializing_if = "Option::is_none")]
    instruction_directories: Option<Vec<String>>,
}

impl InstructionsGetDiscoveryPathsOptions {
    /// Creates options with the required inputs.
    pub fn new() -> Self {
        Self {
            legacy: InstructionsGetDiscoveryPathsRequest {
                project_paths: None,
                exclude_host_instructions: None,
            },
            instruction_directories: None,
        }
    }

    /// Optional list of project directory paths. When omitted or empty, only the user-level targets are returned.
    pub fn project_paths(mut self, value: Vec<String>) -> Self {
        self.legacy.project_paths = Some(value);
        self
    }

    /// When true, omit the host's user-level instruction targets, leaving only repository targets. For multitenant deployments (mirrors `discover`'s `excludeHostInstructions`).
    pub fn exclude_host_instructions(mut self, value: bool) -> Self {
        self.legacy.exclude_host_instructions = Some(value);
        self
    }

    /// Explicit discovery-only instruction roots to enumerate, including configured missing directories. Relative paths resolve against the request's effective working directory when supplied. These are not filesystem grants or preferred creation targets. Null and omission are equivalent.
    pub fn instruction_directories(mut self, value: Vec<String>) -> Self {
        self.instruction_directories = Some(value);
        self
    }
}

impl Default for InstructionsGetDiscoveryPathsOptions {
    fn default() -> Self {
        Self::new()
    }
}

/// Instruction sources loaded for the session, in merge order.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InstructionsGetSourcesResult {
    /// Instruction sources for the session
    pub sources: Vec<InstructionSource>,
}

/// The prompt the init command runs.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct InstructionsInitPromptResult {
    /// Prompt text authoring repository instructions.
    pub prompt: String,
}

/// Parameters for interrupting the main agent turn.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InterruptMainTurnRequest {
    /// When true, the user's queued prompts are preserved and run as the next turn once the interrupted turn unwinds; when false (the default), the queue is cleared like a plain abort.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub flush_queued: Option<bool>,
}

/// Result of interrupting the main agent turn.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InterruptMainTurnResult {
    /// Whether an in-flight main agent turn was interrupted. False when the main loop was not processing.
    pub interrupted: bool,
}

/// A JSON Schema output contract. OpenAI receives the name, description, schema and strict setting; Anthropic receives the schema in output_config.format and always uses its native strict enforcement.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct JsonSchemaResponseFormat {
    /// Optional description passed to OpenAI providers.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Name of the output schema, subject to the provider's naming restrictions.
    pub name: String,
    /// JSON Schema passed unchanged to the inference provider. Schemas larger than 32 MiB when JSON-encoded are rejected before admission, using the runtime's existing request-size ceiling. This is not a guarantee that the entire model request fits. Supported keywords and schema restrictions are determined by the provider.
    pub schema: serde_json::Value,
    /// Optional strict enforcement setting for OpenAI providers. Omitted uses the provider default. Anthropic always enforces its supported schema subset.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub strict: Option<bool>,
}

/// Requests a fresh initial SPNEGO token for a Kerberos service principal.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct KerberosAuthAcquireRequest {
    /// Service principal in the platform-independent `service/host` form, optionally followed by `@REALM`.
    pub spn: String,
}

/// Client credentials for a single authentication exchange.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct KerberosAuthAcquireResult {
    /// Base64 initial SPNEGO token without the Negotiate prefix; omitted when unavailable. Never log or persist it.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub token: Option<String>,
}

/// Registers the calling SDK connection's Kerberos credential provider.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct KerberosAuthSetProviderRequest {
    /// Native lookup fallback or exclusive client callback.
    pub mode: KerberosAuthMode,
    /// Traffic the callback authenticates; defaults to `connection`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub scope: Option<KerberosAuthScope>,
}

/// Cursor-based request for experimental runtime lifecycle diagnostic events. The default max is 100 (maximum 500); the default waitMs is zero (maximum 30000).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct LifecycleDiagnosticsReadRequest {
    /// Opaque cursor returned by an earlier lifecycle diagnostics read. Omit to start at the oldest retained event.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cursor: Option<String>,
    /// Maximum number of events to return, from 1 through 500. Omit for 100.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max: Option<i64>,
    /// Maximum time in milliseconds to wait for a new event, from 0 through 30000.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub wait_ms: Option<i32>,
}

/// One cursor-addressed page of retained experimental runtime lifecycle diagnostic events.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct LifecycleDiagnosticsReadResult {
    /// Opaque cursor for the next independent lifecycle diagnostics read.
    pub cursor: String,
    /// Whether the requested cursor remained within the retained activation window.
    pub cursor_status: DiagnosticCursorStatus,
    /// Number of events lost before this page when known. Omitted when an activation change makes the count unknowable.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub dropped_count: Option<i64>,
    /// Retained lifecycle diagnostic events beginning at the requested cursor.
    pub events: Vec<LifecycleDiagnosticEvent>,
    /// Whether additional retained lifecycle diagnostic events follow this page.
    pub has_more: bool,
}

/// A request body chunk or cancellation signal.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct LlmInferenceHttpRequestChunkRequest {
    /// Identity of the agent invocation (one agentic loop) this body chunk belongs to, matching the `agentInvocationId` semantics on httpRequestStart. Carried per chunk so a persistent transport can attribute successive turns correctly: when a WebSocket connection is reused across turns, the httpRequestStart identity reflects only the turn that opened the connection, so each later turn stamps its own invocation id here. Absent when the runtime has no invocation context for the request, or on the plain-HTTP transport where every request has its own httpRequestStart.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub agent_invocation_id: Option<String>,
    /// When true, `data` is base64-encoded bytes. When absent or false, `data` is UTF-8 text.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub binary: Option<bool>,
    /// When true, the runtime is cancelling the in-flight request (e.g. upstream consumer aborted). `data` is ignored. Implies end-of-request.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cancel: Option<bool>,
    /// Optional human-readable reason for the cancellation, propagated for logging.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cancel_reason: Option<String>,
    /// Body byte range. UTF-8 text when `binary` is absent or false; base64-encoded bytes when `binary` is true. May be empty.
    pub data: String,
    /// When true, this is the final body chunk for the request. The SDK may rely on having received an end-marked chunk before treating the request body as complete.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub end: Option<bool>,
    /// Matches the requestId from the originating httpRequestStart frame.
    pub request_id: RequestId,
}

/// Acknowledgement. The SDK is free to ignore the ack and treat chunk delivery as fire-and-forget.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct LlmInferenceHttpRequestChunkResult {}

/// The head of an outbound model-layer HTTP request.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct LlmInferenceHttpRequestStartRequest {
    /// Stable identity of the agent trajectory that issued this request. Present when the request originates from an agent turn; absent for requests outside any agent context. This is the same identity used by lifecycle and bridged session events and remains constant across turns and retries.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub agent_id: Option<String>,
    /// Identity of the agent invocation (one agentic loop) that issued this request. It remains fixed across physical retries within the invocation and is distinct from the stable trajectory `agentId`. A caller-supplied invocation id always takes precedence (this covers auxiliary calls that have no model call id). Otherwise, first-party CAPI requests fall back to the runtime's agent task id — the same value the runtime emits as the `X-Agent-Task-Id` header — while custom-provider requests fall back to the model call id.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub agent_invocation_id: Option<String>,
    /// HTTP request headers, preserving multiple values per name.
    pub headers: HashMap<String, Vec<String>>,
    /// Coarse classification of the interaction that produced this request. Open string for forward-compatibility; known values include `conversation-agent`, `conversation-subagent`, `conversation-sampling`, `conversation-background`, `conversation-compaction`, and `conversation-user`. Absent when the runtime did not classify the request. Comes from the runtime's per-request agent context independently of transport; on the CAPI transport the runtime derives the upstream `X-Interaction-Type` header from this same context.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub interaction_type: Option<String>,
    /// HTTP method, e.g. GET, POST.
    pub method: String,
    /// Stable identity of the immediate parent trajectory. Present for child trajectories such as subagents and conversation-sampling requests; absent for root-agent and non-agent requests.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub parent_agent_id: Option<String>,
    /// Opaque runtime-minted id, unique per in-flight request. The SDK uses this to correlate httpRequestChunk frames and to address its httpResponseStart / httpResponseChunk replies back to the runtime.
    pub request_id: RequestId,
    /// Id of the runtime session that triggered this request, when one is in scope. Absent for requests issued outside any session (e.g. startup model-catalog or capability resolution). This is a payload field — not a dispatch key — because the client-global API is registered process-wide rather than per session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_id: Option<SessionId>,
    /// Transport the runtime would otherwise use for this request. `http` (the default when absent) covers plain HTTP and SSE responses; `websocket` indicates a full-duplex message channel where each body chunk maps to one WebSocket message and the `binary` flag distinguishes text from binary frames. The SDK consumer uses this to decide whether to service the request with an HTTP client or a WebSocket client. It is the one piece of request metadata the consumer cannot reliably infer from the URL or headers alone.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub transport: Option<LlmInferenceHttpRequestStartTransport>,
    /// Absolute request URL.
    pub url: String,
}

/// Acknowledgement. Returning successfully simply means the SDK accepted the start frame; it does not imply the request will succeed.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct LlmInferenceHttpRequestStartResult {}

/// Set to terminate the response with a transport-level failure. Implies end-of-stream; any further chunks for this requestId are ignored.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct LlmInferenceHttpResponseChunkError {
    /// Optional machine-readable error code.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub code: Option<String>,
    /// Human-readable failure description.
    pub message: String,
}

/// A response body chunk or terminal error.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct LlmInferenceHttpResponseChunkRequest {
    /// When true, `data` is base64-encoded bytes. When absent or false, `data` is UTF-8 text.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub binary: Option<bool>,
    /// Body byte range. UTF-8 text when `binary` is absent or false; base64-encoded bytes when `binary` is true. May be empty (e.g. when the response body is empty: send a single chunk with empty data and end=true).
    pub data: String,
    /// When true, this is the final body chunk for the response. The runtime treats the response body as complete after receiving an end-marked chunk.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub end: Option<bool>,
    /// Set to terminate the response with a transport-level failure. Implies end-of-stream; any further chunks for this requestId are ignored.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<LlmInferenceHttpResponseChunkError>,
    /// Matches the requestId from the originating httpRequestStart frame.
    pub request_id: RequestId,
}

/// Whether the chunk was accepted.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct LlmInferenceHttpResponseChunkResult {
    /// True when the chunk was matched to a pending request; false when unknown.
    pub accepted: bool,
}

/// Response head.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct LlmInferenceHttpResponseStartRequest {
    /// HTTP response headers, preserving multiple values per name.
    pub headers: HashMap<String, Vec<String>>,
    /// Matches the requestId from the originating httpRequestStart frame.
    pub request_id: RequestId,
    /// HTTP status code.
    pub status: i64,
    /// Optional HTTP status reason phrase.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub status_text: Option<String>,
}

/// Whether the start frame was accepted.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct LlmInferenceHttpResponseStartResult {
    /// True when the response start was matched to a pending request; false when unknown.
    pub accepted: bool,
}

/// Indicates whether the calling client was registered as the LLM inference provider.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct LlmInferenceSetProviderResult {
    /// Whether the provider was set successfully
    pub success: bool,
}

/// Which stored location permissions were dropped because their directory is gone.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct LocationPermissionsCleanupResult {
    /// Location keys whose grants were removed. Empty when every stored location still exists.
    pub removed_location_keys: Vec<String>,
}

/// Message text, optional severity level, persistence flag, optional follow-up URL, and optional tip.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct LogRequest {
    /// When true, the message is transient and not persisted to the session event log on disk
    #[serde(skip_serializing_if = "Option::is_none")]
    pub ephemeral: Option<bool>,
    /// Log severity level. Determines how the message is displayed in the timeline. Defaults to "info".
    #[serde(skip_serializing_if = "Option::is_none")]
    pub level: Option<SessionLogLevel>,
    /// Human-readable message
    pub message: String,
    /// Optional actionable tip displayed alongside the message. Only honored on `level: "info"`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tip: Option<String>,
    /// Domain category for this log entry (e.g., "mcp", "subscription", "policy", "model"). Maps to `infoType`/`warningType`/`errorType` on the emitted event. Defaults to "notification".
    #[serde(skip_serializing_if = "Option::is_none")]
    pub r#type: Option<String>,
    /// Optional URL the user can open in their browser for more details
    #[serde(skip_serializing_if = "Option::is_none")]
    pub url: Option<String>,
}

/// Identifier of the session event that was emitted for the log message.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct LogResult {
    /// The unique identifier of the emitted session event
    pub event_id: String,
}

/// Parameters for (re)loading the merged LSP configuration set.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct LspInitializeRequest {
    /// Force re-initialization even when LSP configs were already loaded for the working directory.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub force: Option<bool>,
    /// Git root used as the boundary when traversing for project-level LSP configs (supports monorepos).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub git_root: Option<String>,
    /// Working directory used to load project-level LSP configs. Defaults to the session working directory when omitted.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub working_directory: Option<String>,
}

/// Result of registering a new marketplace.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MarketplaceAddResult {
    /// Final name of the marketplace as resolved from its manifest
    pub name: String,
}

/// Plugins advertised by the marketplace.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MarketplaceBrowseResult {
    /// Plugins advertised by the marketplace
    pub plugins: Vec<MarketplacePluginInfo>,
}

/// All registered marketplaces, including built-in defaults.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MarketplaceListResult {
    /// Registered marketplaces
    pub marketplaces: Vec<MarketplaceInfo>,
}

/// Result of refreshing one or more marketplace catalogs.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MarketplaceRefreshResult {
    /// Per-marketplace refresh results in deterministic order.
    pub results: Vec<MarketplaceRefreshEntry>,
}

/// Outcome of the remove attempt, including dependent-plugin info when applicable.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MarketplaceRemoveResult {
    /// Names of installed plugins that prevented removal. Populated only when `removed=false`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub dependent_plugins: Option<Vec<String>>,
    /// True when the marketplace was actually removed. False when removal was skipped because the marketplace has dependent plugins and `force` was not set.
    pub removed: bool,
}

/// Metadata controlling an MCP task's lifetime.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpTaskMetadata {
    /// Task time-to-live.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub ttl: Option<i64>,
}

/// The six normalized `/context` header buckets, computed from the same tokenization as `entries` so the two never disagree. Convenience rollups: `freeSpace` and `buffer` describe window capacity rather than occupied context, so the values do not sum to `totalTokens`.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MetadataContextAttributionResultContextAttributionCategories {
    /// Overlapping output reservation plus post-blocking-threshold buffer.
    pub buffer: i64,
    /// Custom-instructions tokens (0 when none are configured).
    pub custom_instructions: i64,
    /// Remaining unused window capacity (clamped at 0).
    pub free_space: i64,
    /// MCP tool-definition tokens.
    pub mcp_tools: i64,
    /// Conversation (user/assistant/tool) message tokens.
    pub messages: i64,
    /// System prompt tokens, excluding custom instructions.
    pub system_prompt: i64,
    /// Non-MCP tool-definition tokens.
    pub system_tools: i64,
}

/// Successful compaction history for the session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MetadataContextAttributionResultContextAttributionCompactions {
    /// Number of successful compactions in this session.
    pub count: i64,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MetadataContextAttributionResultContextAttributionEntriesItem {
    /// Supplementary per-entry metadata (e.g. `messageCount`, `role`, `evictable`, `pluginSource`). Values are stringified; parse as needed and ignore unrecognized keys.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub attributes: Option<HashMap<String, String>>,
    /// Identifier for this entry, formed by joining its `kind` and source name (e.g. `tool:bash`, `skill:tmux`, `toolDefinition:bash`); unique within the snapshot. Use it to match the same entry across snapshots, to correlate with other APIs (skill/agent/MCP registries), and as the `parentId` target for nesting. Distinct from the human-facing `label`.
    pub id: String,
    /// Source category for this entry. Not a closed set — tolerate unknown values. Known values today: `skill`, `subagent`, `mcpServer`, `tool`, `system`, `toolDefinition`, `plugin`.
    pub kind: String,
    /// Human-readable display label, e.g. `bash` or `skill: tmux`. Presentation-only; may be localized/reformatted without notice — do not key off it.
    pub label: String,
    /// Optional `id` of the parent entry: e.g. a `plugin` entry parenting its `skill`/`mcpServer` entries, or the `system` entry parenting `toolDefinition` entries. Omitted for top-level entries.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub parent_id: Option<String>,
    /// Token count currently in context attributable to this entry.
    pub tokens: i64,
}

/// Per-source token attribution snapshot for the current context window. The heaviest individual messages are available separately via `metadata.getContextHeaviestMessages`.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MetadataContextAttributionResultContextAttribution {
    /// Output reservation overlapping the displayed prompt allowance plus the tokens past the effective input budget's buffer-exhaustion blocking threshold. Mirrors `SessionContextInfo.bufferTokens`.
    pub buffer_tokens: i64,
    /// The six normalized `/context` header buckets, computed from the same tokenization as `entries` so the two never disagree. Convenience rollups: `freeSpace` and `buffer` describe window capacity rather than occupied context, so the values do not sum to `totalTokens`.
    pub categories: MetadataContextAttributionResultContextAttributionCategories,
    /// Successful compaction history for the session.
    pub compactions: MetadataContextAttributionResultContextAttributionCompactions,
    /// Token count at which background compaction starts. Mirrors `SessionContextInfo.compactionThreshold`.
    pub compaction_threshold: i64,
    /// Flat list of per-source attribution entries. Group by `kind` and render unrecognized kinds generically. Nesting and rollups are expressed via `parentId`.
    pub entries: Vec<MetadataContextAttributionResultContextAttributionEntriesItem>,
    /// Advertised prompt allowance for the selected context tier: the denominator for context-usage displays and capacity for `categories.freeSpace` and `categories.buffer`. Mirrors `SessionContextInfo.limit`.
    pub limit: i64,
    /// The concrete model id the entire breakdown was tokenized against (feeds the per-model token multiplier). Under `Auto` (Free/Student) this is the resolved model, not the literal `auto` sentinel, so totals are not undercounted. A single-model approximation of a potentially multi-model Auto session.
    pub model_id: String,
    /// How `modelId` was chosen. Not a closed set — tolerate unknown values. Known values today: `autoResolved` (the model Auto resolved to), `selected` (the user's explicitly selected model), `default` (a fallback before any model is known).
    pub model_source: String,
    /// Effective input budget after reserving requested output against the combined context ceiling. Mirrors `SessionContextInfo.promptTokenLimit`.
    pub prompt_token_limit: i64,
    /// Total token count of the current context window the entries are measured against (system message + conversation messages + tool definitions — the same total reported by /context). Divide an entry's `tokens` by this to derive its share.
    pub total_tokens: i64,
}

/// Per-source attribution breakdown for the session's current context window, or null if uninitialized.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MetadataContextAttributionResult {
    /// Per-source context-window attribution, or null if the session has not yet been initialized (no system prompt or tool metadata cached).
    pub context_attribution: Option<MetadataContextAttributionResultContextAttribution>,
}

/// Parameters for the heaviest-messages query.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MetadataContextHeaviestMessagesRequest {
    /// Maximum number of messages to return, most-expensive first. Omit for the server default.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub limit: Option<i64>,
}

/// The heaviest individual messages in the session's context window, most-expensive first.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MetadataContextHeaviestMessagesResult {
    /// Heaviest messages, most-expensive first.
    pub messages: Vec<ContextHeaviestMessage>,
    /// Total token count of the current context window, so callers can compute each message's share without a second call.
    pub total_tokens: i64,
}

/// Model identifier and token limits used to compute the context-info breakdown.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MetadataContextInfoRequest {
    /// Requested output allowance to reserve against the combined context ceiling. Pass 0 to resolve the session's request cap, falling back to the model's advertised output limit.
    pub output_token_limit: i64,
    /// Advertised prompt allowance. Pass 0 to resolve the selected model and context tier from the session.
    pub prompt_token_limit: i64,
    /// Provider owning selectedModel. Omit to use the current provider for the current model, or deterministic legacy lookup for a different model.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub provider_id: Option<String>,
    /// Model identifier used for tokenization. Omit to use the session default. Used both for token counting and to compute display values.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub selected_model: Option<String>,
}

/// Token-usage breakdown for the session's current context window
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MetadataContextInfoResultContextInfo {
    /// Output reservation overlapping the displayed prompt allowance plus tokens after the effective input budget's buffer-exhaustion blocking threshold (default 95%).
    pub buffer_tokens: i64,
    /// Token count at which background compaction starts (configurable percentage of promptTokenLimit)
    pub compaction_threshold: i64,
    /// Tokens consumed by user/assistant/tool messages
    pub conversation_tokens: i64,
    /// Provider-owned model display label, or the Auto routing label. Presentation only; modelName remains the tokenization identity.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub display_model_name: Option<String>,
    /// Advertised prompt allowance for the selected context tier, without adding output tokens. The denominator for context-usage displays.
    pub limit: i64,
    /// Tokens consumed by MCP tool definitions (subset of toolDefinitionsTokens, excludes deferred tools)
    pub mcp_tools_tokens: i64,
    /// The model used for token counting
    pub model_name: String,
    /// Effective input budget: the selected tier's prompt allowance bounded by the combined context ceiling minus the requested output allowance. Uses DEFAULT_TOKEN_LIMIT when limits are unspecified.
    pub prompt_token_limit: i64,
    /// Provider whose catalog supplies this context window. Omitted when provider identity is unavailable.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub provider: Option<ModelProviderRef>,
    /// Tokens consumed by the system prompt
    pub system_tokens: i64,
    /// Tokens consumed by tool definitions sent to the model (excludes deferred tools)
    pub tool_definitions_tokens: i64,
    /// Sum of system, conversation and tool-definition tokens
    pub total_tokens: i64,
}

/// Token breakdown for the session's current context window, or null if uninitialized.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MetadataContextInfoResult {
    /// Token breakdown for the current context window, or null if the session has not yet been initialized (no system prompt or tool metadata cached).
    pub context_info: Option<MetadataContextInfoResultContextInfo>,
}

/// Indicates whether the local session is currently processing a turn or background continuation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MetadataIsProcessingResult {
    /// Whether the session is currently processing user/agent messages. False for non-local sessions (which don't run a local agentic loop). Reflects an in-flight turn or background continuation.
    pub processing: bool,
}

/// Model identifier to use when re-tokenizing the session's existing messages.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MetadataRecomputeContextTokensRequest {
    /// Model identifier used for tokenization. The runtime token-counts both chat-context and system-context messages against this model.
    pub model_id: String,
}

/// Re-tokenize the session's existing messages against `modelId` and return the token totals. Useful for hosts that want an initial estimate of context usage on session resume, before the next agent turn fires `session.context_info_changed` events. Returns zeros for an empty session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MetadataRecomputeContextTokensResult {
    /// Tokens contributed by user/assistant/tool messages (excludes system/developer prompts).
    pub messages_token_count: i64,
    /// Tokens contributed by system/developer prompt snapshots.
    pub system_token_count: i64,
    /// Sum of tokens across chat-context and system-context messages currently held by the session.
    pub total_tokens: i64,
}

/// Updated working directory and git context. Emitted as the new payload of `session.context_changed`.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkingDirectoryContext {
    /// Merge-base commit SHA (fork point from the remote default branch)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub base_commit: Option<String>,
    /// Current git branch name
    #[serde(skip_serializing_if = "Option::is_none")]
    pub branch: Option<String>,
    /// Current working directory path
    pub cwd: String,
    /// Root directory of the git repository, resolved via git rev-parse
    #[serde(skip_serializing_if = "Option::is_none")]
    pub git_root: Option<String>,
    /// Head commit of the current git branch
    #[serde(skip_serializing_if = "Option::is_none")]
    pub head_commit: Option<String>,
    /// Hosting platform type of the repository
    #[serde(skip_serializing_if = "Option::is_none")]
    pub host_type: Option<SessionWorkingDirectoryContextHostType>,
    /// Repository identifier derived from the git remote URL ("owner/name" for GitHub, "org/project/repo" for Azure DevOps)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repository: Option<String>,
    /// Raw host string from the git remote URL (e.g. "github.com", "dev.azure.com")
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repository_host: Option<String>,
}

/// Updated working-directory/git context to record on the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MetadataRecordContextChangeRequest {
    /// Updated working directory and git context. Emitted as the new payload of `session.context_changed`.
    pub context: SessionWorkingDirectoryContext,
}

/// Notify the session that its working directory context has changed. Emits a `session.context_changed` event so consumers (telemetry, OTel tracker, ACP, the timeline UI) can react. Use this when the host has detected a cwd/branch/repo change outside the session's normal lifecycle (e.g., after a shell command in interactive mode). For a local session, a report whose `cwd` diverges from the session's current working directory is ignored (the call still succeeds but records nothing and emits no event); move a local session's working directory via `metadata.setWorkingDirectory` instead.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MetadataRecordContextChangeResult {}

/// Absolute path to set as the session's new working directory. For local sessions the path must be absolute and exist on disk: it is validated before any session state changes, and a failing validation rejects the call with nothing mutated, persisted, or emitted. Remote sessions record the path as-is.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MetadataSetWorkingDirectoryRequest {
    /// Absolute path to set as the session's working directory. The runtime updates the session's recorded cwd so subsequent operations (shell tools, file lookups, telemetry) anchor to it.
    pub working_directory: String,
}

/// Update the session's working directory. Used by the host when the user explicitly changes cwd (e.g., the `/cd` slash command). The host is responsible for any related side-effects (file index, etc.); it does NOT change the process working directory (a session's cwd is per-session, not process-global). For local sessions the runtime validates the target first (an absolute path that exists on disk) and re-bases the permission primary directory; a rejected validation fails the call before anything is mutated, persisted, or emitted. Location-scoped permission rules are then re-keyed to the new directory (best-effort). Remote sessions only record the path.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MetadataSetWorkingDirectoryResult {
    /// Working directory after the update
    pub working_directory: String,
}

/// Atomic patch for client-owned session metadata. Operations apply in clear, remove, then set order. The resulting bag must satisfy the ClientMetadata entry and serialized-size limits. Local storage coordinates concurrent runtime processes; custom SessionFs providers must serialize writers that access the same session from multiple processes.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MetadataUpdateClientMetadataRequest {
    /// Remove every existing client metadata entry before applying remove and set. Defaults to false.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub clear: Option<bool>,
    /// Case-sensitive keys to remove. Missing keys are ignored. Each key must be non-empty, at most 256 UTF-8 bytes, and outside the reserved `copilot/` and `github/` namespaces.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub remove: Option<Vec<String>>,
    /// String entries to add or replace. Set wins when a key also appears in remove. Each key must be non-empty, at most 256 UTF-8 bytes, and outside the reserved `copilot/` and `github/` namespaces. Each value may contain at most 16 KiB of UTF-8 data.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub set: Option<HashMap<String, String>>,
}

/// Agent interaction mode to apply to the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModeSetRequest {
    /// Explicit response to a model-switch compaction preflight.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub compaction_decision: Option<String>,
    /// Mode the session must currently be in for the change to apply. When set and the session is in a different mode the request is a no-op and reports status 'unchanged'.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub expected_mode: Option<SessionMode>,
    /// Session whose plan-mode base state should be inherited.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub inherit_plan_base_from_session_id: Option<String>,
    /// The session mode the agent is operating in
    pub mode: SessionMode,
    /// Whether the selected plan model should be persisted.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub persist_plan_selection: Option<bool>,
    /// Settings context used when persisting the selected plan model.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub picker_settings_context: Option<ModelPickerSettingsContext>,
    /// Context tier to use with the dedicated plan model.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub plan_context_tier: Option<String>,
    /// Action to perform when leaving plan mode.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub plan_exit_action: Option<String>,
    /// Dedicated model to use in plan mode, when configured.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub plan_model: Option<String>,
    /// Whether a dedicated plan model is configured.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub plan_model_configured: Option<bool>,
    /// Provider owning planModel. Omit for legacy bare-model selection.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub plan_model_provider_id: Option<String>,
    /// Reasoning effort to use with the dedicated plan model.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub plan_reasoning_effort: Option<String>,
    /// Whether leaving plan mode should restore the session's previous model.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub restore_plan_model: Option<bool>,
}

/// Outcome of a session mode change, including any model switch it triggered and follow-up the host must perform.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModeSetResult {
    /// Whether the host should arm an interactive continuation after the mode change.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub arm_interactive_continuation: Option<bool>,
    /// Compaction confirmation required before the mode change can complete.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub confirmation: Option<ModelSwitchConfirmation>,
    /// Whether the host must defer implementing the requested mode change.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub defer_implementation: Option<bool>,
    /// Deprecation warnings associated with the model selected by the mode change.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub deprecation_warnings: Option<Vec<String>>,
    /// User-facing outcome message for the model switch triggered by the mode change.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub message: Option<String>,
    /// Whether the requested mode was applied to the session. False only when an 'expectedMode' precondition did not hold, in which case any model change reported alongside it was still applied.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mode_applied: Option<bool>,
    /// Whether applying the mode changed the active model.
    pub model_changed: bool,
    /// Lifecycle status of the requested mode change.
    pub status: String,
    /// User-facing warning produced while applying the mode change.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub warning: Option<String>,
}

/// Result of moving in-flight MCP loading to the background.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MoveMcpLoadingToBackgroundResult {
    /// Whether an in-flight MCP load was moved to the background, releasing turns that were waiting on it. False when no MCP load was in flight or the waiting turns had already been released.
    pub moved_to_background: bool,
}

/// The session's friendly name, or null when not yet set.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct NameGetResult {
    /// The session name (user-set or auto-generated), or null if not yet set
    pub name: Option<String>,
}

/// Auto-generated session summary to apply as the session's name when no user-set name exists.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct NameSetAutoRequest {
    /// Auto-generated session summary. Empty/whitespace-only values are ignored; values are trimmed before persisting.
    pub summary: String,
}

/// Indicates whether the auto-generated summary was applied as the session's name.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct NameSetAutoResult {
    /// Whether the auto-generated summary was persisted. False if the session already has a user-set name, the summary normalized to empty, or the session does not have a workspace.
    pub applied: bool,
}

/// New friendly name to apply to the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct NameSetRequest {
    /// New session name (1–100 characters, trimmed of leading/trailing whitespace)
    pub name: String,
}

/// External tool calls still waiting for a result.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PendingExternalToolRequestList {
    /// External tool calls of the session and its sub-agents that session.tools.handlePendingToolCall would still accept, in the order they were requested. Includes calls that were pending when the session was last suspended and resumed with pending work continued, so a host that did not observe the original external_tool.requested event can answer them.
    pub items: Vec<PendingExternalToolRequest>,
}

/// List of pending permission requests reconstructed from event history.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PendingPermissionRequestList {
    /// Pending permission prompts reconstructed from the session's event history. Equivalent to the set of `permission.requested` events that have not yet been followed by a matching `permission.completed` event. Used by clients (e.g. the CLI) to hydrate UI for prompts that were emitted before the client attached to the session.
    pub items: Vec<PendingPermissionRequest>,
}

/// A complete JSON snapshot to write under the runtime's per-path file lock.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PersistenceWriteRawJsonLockedRequest {
    /// The serialized JSON document to write. It replaces the file's contents entirely; nothing is merged.
    pub data_json: String,
    /// Comment lines to emit above the document, for a format that tolerates them. Omit for strict JSON.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub file_header: Option<String>,
    /// Absolute path of the file to replace. Parent directories are created when missing.
    pub path: String,
}

/// The outcome of a locked snapshot write.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PersistenceWriteRawJsonLockedResult {
    /// Why the write failed. Absent when `success` is true. A lock the runtime could not acquire in time reports here rather than raising.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Whether the file now holds the requested contents.
    pub success: bool,
}

/// Optional message to echo back to the caller.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PingRequest {
    /// Optional message to echo back
    #[serde(skip_serializing_if = "Option::is_none")]
    pub message: Option<String>,
}

/// Server liveness response, including the echoed message, current server timestamp, and protocol version.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PingResult {
    /// Echoed message (or default greeting)
    pub message: String,
    /// Server protocol version number
    pub protocol_version: i64,
    /// ISO 8601 timestamp when the server handled the ping
    pub timestamp: String,
}

/// Existence, contents, and resolved path of the session plan file.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PlanReadResult {
    /// The content of the plan file, or null if it does not exist
    pub content: Option<String>,
    /// Whether the plan file exists in the workspace
    pub exists: bool,
    /// Absolute file path of the plan file, or null if workspace is not enabled
    pub path: Option<String>,
}

/// Todo rows read from the session SQL database. Empty when no session database is available.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PlanReadSqlTodosResult {
    /// Rows from the session SQL todos table, ordered by creation time with insertion order used to break ties when available and id used for WITHOUT ROWID tables.
    pub rows: Vec<PlanSqlTodosRow>,
}

/// Todo rows + dependency edges read from the session SQL database.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PlanReadSqlTodosWithDependenciesResult {
    /// Edges from the session SQL todo_deps table. Empty when no database, no todo_deps table, or the SELECT failed. Read independently from `rows`, so a broken todo_deps table does not affect the rows result and vice versa.
    pub dependencies: Vec<PlanSqlTodoDependency>,
    /// Rows from the session SQL todos table, ordered by creation time with insertion order used to break ties when available and id used for WITHOUT ROWID tables. Empty when no database, no todos table, or the SELECT failed.
    pub rows: Vec<PlanSqlTodosRow>,
}

/// Replacement contents to write to the session plan file.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PlanUpdateRequest {
    /// The new content for the plan file
    pub content: String,
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
pub struct ProtocolStaticSectionOverride {
    /// Declarative operation applied to the section.
    pub action: ProtocolStaticSectionAction,
    /// Optional content used by replace, append, and prepend operations.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub content: Option<String>,
}

/// The `status.get` response, including optional cached error-reporting policy.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ProtocolStatusResponse {
    /// Cached host policy. This field does not grant ownership or confirm network delivery.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub host_diagnostics: Option<HostDiagnostics>,
    /// The runtime SDK protocol version.
    pub protocol_version: i64,
    /// The runtime package version.
    pub version: String,
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
pub struct ProtocolSystemMessageAppendConfig {
    /// Text appended to the standard system prompt.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub content: Option<String>,
    /// Append-mode discriminator. Omission also selects append mode.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mode: Option<ProtocolAppendMode>,
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
pub struct ProtocolSystemMessageCustomizeConfig {
    /// Text appended after the customized sections.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub content: Option<String>,
    /// Customize-mode discriminator.
    pub mode: ProtocolCustomizeMode,
    /// Named standard-prompt section overrides.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sections: Option<HashMap<String, serde_json::Value>>,
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
pub struct SystemMessageBlock {
    /// Whether providers with explicit prompt caching should place a cache breakpoint after this block.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cache_breakpoint: Option<bool>,
    /// Text content for this system-message block.
    pub content: String,
    /// Whether the block is static and may be cached independently of dynamic prompt content.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub is_static: Option<bool>,
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
pub struct ProtocolSystemMessageReplaceConfig {
    /// Complete replacement system-message text.
    pub content: String,
    /// Optional structured blocks corresponding to the replacement content.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub content_blocks: Option<Vec<SystemMessageBlock>>,
    /// Replace-mode discriminator.
    pub mode: ProtocolReplaceMode,
}

/// Blob attachment with inline base64-encoded data
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PushAttachmentBlob {
    /// Base64-encoded content
    pub data: String,
    /// User-facing display name for the attachment
    #[serde(skip_serializing_if = "Option::is_none")]
    pub display_name: Option<String>,
    /// MIME type of the inline data
    pub mime_type: String,
    /// Attachment type discriminator
    pub r#type: PushAttachmentBlobType,
}

/// Directory attachment
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PushAttachmentDirectory {
    /// User-facing display name for the attachment
    pub display_name: String,
    /// Absolute directory path
    pub path: String,
    /// Attachment type discriminator
    pub r#type: PushAttachmentDirectoryType,
}

/// Optional line range to scope the attachment to a specific section of the file
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PushAttachmentFileLineRange {
    /// End line number (1-based, inclusive)
    pub end: i64,
    /// Start line number (1-based)
    pub start: i64,
}

/// File attachment
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PushAttachmentFile {
    /// User-facing display name for the attachment
    pub display_name: String,
    /// Optional line range to scope the attachment to a specific section of the file
    #[serde(skip_serializing_if = "Option::is_none")]
    pub line_range: Option<PushAttachmentFileLineRange>,
    /// Absolute file path
    pub path: String,
    /// Attachment type discriminator
    pub r#type: PushAttachmentFileType,
}

/// Pointer to a GitHub repository.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PushGitHubRepoRef {
    /// Numeric GitHub repository id
    #[serde(skip_serializing_if = "Option::is_none")]
    pub id: Option<i64>,
    /// Repository name (without owner)
    pub name: String,
    /// Repository owner login (user or organization)
    pub owner: String,
}

/// Pointer to a GitHub Actions job.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PushAttachmentGitHubActionsJob {
    /// Terminal conclusion of the job when finished (e.g., success, failure, cancelled). Absent for in-progress jobs.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub conclusion: Option<String>,
    /// Job id within the workflow run
    pub job_id: i64,
    /// Display name of the job
    pub job_name: String,
    /// Repository the workflow run belongs to
    pub repo: PushGitHubRepoRef,
    /// Attachment type discriminator
    pub r#type: PushAttachmentGitHubActionsJobType,
    /// URL to the job on GitHub
    pub url: String,
    /// Display name of the workflow the job ran in
    pub workflow_name: String,
}

/// Pointer to a GitHub commit.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PushAttachmentGitHubCommit {
    /// First line of the commit message
    pub message: String,
    /// Full commit SHA
    pub oid: String,
    /// Repository the commit belongs to
    pub repo: PushGitHubRepoRef,
    /// Attachment type discriminator
    pub r#type: PushAttachmentGitHubCommitType,
    /// URL to the commit on GitHub
    pub url: String,
}

/// Pointer to a file in a GitHub repository at a specific ref.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PushAttachmentGitHubFile {
    /// Repository-relative path to the file
    pub path: String,
    /// Git ref the file is read at (branch, tag, or commit SHA)
    pub r#ref: String,
    /// Repository the file lives in
    pub repo: PushGitHubRepoRef,
    /// Attachment type discriminator
    pub r#type: PushAttachmentGitHubFileType,
    /// URL to the file on GitHub
    pub url: String,
}

/// One side of a file diff (head or base)
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PushAttachmentGitHubFileDiffSide {
    /// Repository-relative path to the file
    pub path: String,
    /// Git ref (branch, tag, or commit SHA) the file is read at
    pub r#ref: String,
    /// Repository the file lives in
    pub repo: PushGitHubRepoRef,
}

/// Pointer to a single-file diff. At least one of `head` and `base` must be present.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PushAttachmentGitHubFileDiff {
    /// File location on the base side of the diff. Absent for additions.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub base: Option<PushAttachmentGitHubFileDiffSide>,
    /// File location on the head side of the diff. Absent for deletions.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub head: Option<PushAttachmentGitHubFileDiffSide>,
    /// Attachment type discriminator
    pub r#type: PushAttachmentGitHubFileDiffType,
    /// URL to the diff on GitHub (e.g., a commit, compare, or PR-file URL)
    pub url: String,
}

/// GitHub issue, pull request, or discussion reference
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PushAttachmentGitHubReference {
    /// Issue, pull request, or discussion number
    pub number: i64,
    /// Type of GitHub reference
    pub reference_type: PushAttachmentGitHubReferenceType,
    /// Current state of the referenced item (e.g., open, closed, merged)
    pub state: String,
    /// Title of the referenced item
    pub title: String,
    /// Attachment type discriminator
    pub r#type: PushAttachmentGitHubReferenceType,
    /// URL to the referenced item on GitHub
    pub url: String,
}

/// Pointer to a GitHub release.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PushAttachmentGitHubRelease {
    /// Human-readable release name
    pub name: String,
    /// Repository the release belongs to
    pub repo: PushGitHubRepoRef,
    /// Git tag the release is anchored to
    pub tag_name: String,
    /// Attachment type discriminator
    pub r#type: PushAttachmentGitHubReleaseType,
    /// URL to the release on GitHub
    pub url: String,
}

/// Pointer to a GitHub repository.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PushAttachmentGitHubRepository {
    /// Short description of the repository
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Git ref this attachment is anchored at (branch, tag, or commit). When absent the default branch is implied.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub r#ref: Option<String>,
    /// Repository pointer
    pub repo: PushGitHubRepoRef,
    /// Attachment type discriminator
    pub r#type: PushAttachmentGitHubRepositoryType,
    /// URL to the repository on GitHub
    pub url: String,
}

/// Pointer to a line range inside a file in a GitHub repository.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PushAttachmentGitHubSnippet {
    /// Line range the snippet covers
    pub line_range: PushAttachmentFileLineRange,
    /// Repository-relative path to the file
    pub path: String,
    /// Git ref the file is read at (branch, tag, or commit SHA)
    pub r#ref: String,
    /// Repository the file lives in
    pub repo: PushGitHubRepoRef,
    /// Attachment type discriminator
    pub r#type: PushAttachmentGitHubSnippetType,
    /// URL to the snippet on GitHub (with line anchor)
    pub url: String,
}

/// One side of a tree comparison (head or base)
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PushAttachmentGitHubTreeComparisonSide {
    /// Repository the revision belongs to
    pub repo: PushGitHubRepoRef,
    /// Git revision (branch, tag, or commit SHA)
    pub revision: String,
}

/// Pointer to a comparison between two git revisions.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PushAttachmentGitHubTreeComparison {
    /// Base side of the comparison
    pub base: PushAttachmentGitHubTreeComparisonSide,
    /// Head side of the comparison
    pub head: PushAttachmentGitHubTreeComparisonSide,
    /// Attachment type discriminator
    pub r#type: PushAttachmentGitHubTreeComparisonType,
    /// URL to the comparison on GitHub
    pub url: String,
}

/// Generic GitHub URL reference.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PushAttachmentGitHubUrl {
    /// Attachment type discriminator
    pub r#type: PushAttachmentGitHubUrlType,
    /// URL to the GitHub resource
    pub url: String,
}

/// End position of the selection
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PushAttachmentSelectionDetailsEnd {
    /// End character offset within the line (0-based)
    pub character: i64,
    /// End line number (0-based)
    pub line: i64,
}

/// Start position of the selection
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PushAttachmentSelectionDetailsStart {
    /// Start character offset within the line (0-based)
    pub character: i64,
    /// Start line number (0-based)
    pub line: i64,
}

/// Position range of the selection within the file
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PushAttachmentSelectionDetails {
    /// End position of the selection
    pub end: PushAttachmentSelectionDetailsEnd,
    /// Start position of the selection
    pub start: PushAttachmentSelectionDetailsStart,
}

/// Code selection attachment from an editor
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PushAttachmentSelection {
    /// User-facing display name for the selection
    pub display_name: String,
    /// Absolute path to the file containing the selection
    pub file_path: String,
    /// Position range of the selection within the file
    pub selection: PushAttachmentSelectionDetails,
    /// The selected text content
    pub text: String,
    /// Attachment type discriminator
    pub r#type: PushAttachmentSelectionType,
}

/// Append to one pending steering message without changing its identity or delivery position.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct QueueAppendSteeringRequest {
    /// Mode captured at submission. Only steering messages in the same mode may be combined.
    pub agent_mode: SendAgentMode,
    /// Attachments to add after the message's existing attachments. An empty list preserves the existing attachments.
    pub attachments: Vec<serde_json::Value>,
    /// Display text to append to the existing preview after a blank line.
    pub display_prompt: String,
    /// Expected current prompt, including any previous appends. The runtime applies plan-mode normalization before comparing and refuses a changed message.
    pub expected_prompt: String,
    /// Message identity returned by send, not the queue item id. Only unclaimed user steering messages are eligible.
    pub message_id: String,
    /// Text to append after a blank line.
    pub prompt: String,
}

/// Inputs for starting a deferred-idle drain.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct QueueBeginDeferredIdleDrainRequest {
    /// Whether the host still has active background work.
    pub active_background_work: bool,
}

/// Whether a deferred-idle drain should run.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct QueueBeginDeferredIdleDrainResult {
    /// True when the host should run finishDeferredIdleDrain asynchronously.
    pub should_drain: bool,
}

/// Internal filter for consuming queued system notifications.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct QueueConsumeSystemNotificationsRequest {
    /// Opaque runtime-owned filter object.
    pub filter: serde_json::Value,
}

/// Queued-command response indicating the host executed the command, with an optional flag to stop queue processing.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct QueuedCommandHandled {
    /// The host actually executed the queued command.
    pub handled: bool,
    /// When true, the runtime will not process subsequent queued commands until a new request comes in.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub stop_processing_queue: Option<bool>,
}

/// Queued-command response indicating the host did not execute the command and the queue may continue.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct QueuedCommandNotHandled {
    /// The host did not execute the queued command. Unblocks the queue without claiming the command was processed (e.g. when the handler threw before completing).
    pub handled: bool,
}

/// Inputs for marking session.idle deferred in native state.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct QueueDeferSessionIdleRequest {
    /// Whether the deferred idle was caused by an aborted foreground turn.
    pub aborted: bool,
}

/// Parameters for duplicating a queued item.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct QueueDuplicateAtRequest {
    /// Stable opaque ID of the queued item to duplicate.
    pub id: String,
}

/// Result of duplicating a queued item.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct QueueDuplicateAtResult {
    /// Fresh stable opaque id assigned to the duplicate.
    pub id: String,
}

/// Result of enqueueing the resume-pending wake item.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct QueueEnqueueResumePendingResult {
    /// True when a wake item was newly queued.
    pub queued: bool,
}

/// Inputs for completing a deferred-idle drain.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct QueueFinishDeferredIdleDrainRequest {
    /// Whether the host still has active background work.
    pub active_background_work: bool,
    /// Whether native queued work remains.
    pub has_pending: bool,
}

/// Action selected by the native deferred-idle drain.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct QueueFinishDeferredIdleDrainResult {
    /// Whether the deferred idle was caused by an aborted foreground turn.
    pub aborted: bool,
    /// One of none, processQueue, or emitSessionIdle.
    pub action: String,
}

/// Whether the native queue has pending work.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct QueueHasPendingResult {
    /// True when queued or immediate native work is pending.
    pub has_pending: bool,
}

/// Serializable message fields accepted by queue.insertAt.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct QueueInsertMessage {
    /// Optional explicit agent mode. When omitted, the session's current mode is assigned.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub agent_mode: Option<SendAgentMode>,
    /// Optional attachments for the message.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub attachments: Option<Vec<serde_json::Value>>,
    /// Whether the message is billable.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub billable: Option<bool>,
    /// Accepted for internal SendOptions compatibility but ignored; delivery is derived from current session activity.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub delivery: Option<String>,
    /// Optional user-facing display text.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub display_prompt: Option<String>,
    /// Accepted for SendOptions compatibility but ignored; inserted items always use queued delivery semantics.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mode: Option<SendMode>,
    /// Accepted for SendOptions compatibility but ignored; the requested public position controls placement.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub prepend: Option<bool>,
    /// The user message text.
    pub prompt: String,
    /// Per-turn request headers.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub request_headers: Option<HashMap<String, String>>,
    /// Required tool name for the turn, when any.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub required_tool: Option<String>,
    /// Optional provenance source. `system` is rejected: it would hide the inserted row from `pendingItems` and make it unaddressable while still executing, so inserted items must stay visible.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source: Option<String>,
    /// Accepted for SendOptions compatibility but ignored; insertion scheduling is controlled by the queue drain state.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub wait: Option<bool>,
}

/// Parameters for inserting a queued message at a public visible position.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct QueueInsertAtRequest {
    /// Queued message contents and delivery metadata.
    pub message: QueueInsertMessage,
    /// Zero-based position in the public visible queue. Values outside the queue clamp to an end.
    pub position: i64,
}

/// Result of inserting a queued message.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct QueueInsertAtResult {
    /// Fresh stable opaque id assigned to the inserted item.
    pub id: String,
}

/// Parameters for moving a queued item by stable id.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct QueueMoveItemRequest {
    /// Stable opaque queued-item id.
    pub id: String,
    /// Zero-based target position in the public visible queue. Values outside the queue clamp to an end.
    pub to_position: i64,
}

/// Result of moving a queued item.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct QueueMoveItemResult {
    /// True when the item changed position; false when it was already at the requested position.
    pub changed: bool,
}

/// Snapshot of the session's pending queued items and immediate-steering messages.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct QueuePendingItemsResult {
    /// How many leading entries of `steeringMessages` have already been folded into the running turn (and so have an emitted `user.message`), as opposed to still waiting for one. Absent for hosts that do not distinguish the two.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub in_flight_steering_count: Option<i64>,
    /// Pending queued items in submission order. Includes user messages, queued slash commands, and queued model changes; omits internal system items.
    pub items: Vec<QueuePendingItems>,
    /// Display text for messages currently in the immediate steering queue (interjections sent during a running turn).
    pub steering_messages: Vec<String>,
    /// ID of the running turn's user message while the model has not answered it, so `withdrawMessage` can still take it back once nothing sent after it is pending. A message leaves `items` when its turn starts, before its `user.message` is recorded; this tells that message apart from one that was removed. Absent when no turn prompt can be taken back.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub withdrawable_turn_message_id: Option<String>,
}

/// Parameters for removing a queued item by stable id.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct QueueRemoveAtRequest {
    /// Stable opaque ID of the queued item to remove.
    pub id: String,
}

/// Result of removing a queued item.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct QueueRemoveAtResult {
    /// True when the addressed item was removed.
    pub removed: bool,
}

/// Indicates whether a user-facing pending item was removed.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct QueueRemoveMostRecentResult {
    /// True if a user-facing pending item was removed (LIFO across both queues); false when no removable items remained.
    pub removed: bool,
}

/// Parameters for steering a queued message into a live turn.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct QueueSendNowRequest {
    /// Stable opaque ID of the queued item to steer into the live turn.
    pub id: String,
}

/// Result of trying to steer a queued message into a live turn.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct QueueSendNowResult {
    /// True when the item was accepted into the steering lane; false when no main turn was live.
    pub steered: bool,
}

/// Parameters for acquiring or releasing the queued-lane drain pause. Acquisition is exclusive and non-idempotent: `paused: true` against an already-paused session fails with `queue_already_paused`. The pause is never released automatically — it is not tied to the caller's lifetime, so a client that exits without sending `paused: false` leaves the lane frozen. Release is unowned: `paused: false` clears the pause for any caller, including one that never acquired it.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct QueueSetDrainPausedRequest {
    /// Whether queued-lane draining should be paused.
    pub paused: bool,
}

/// Internal snapshot of native queue state for local session orchestration.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct QueueSnapshotResult {
    /// Queue item identifier of a model switch that has been dequeued but not yet applied.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub in_flight_model_change_id: Option<String>,
    /// Insertion orders for queued items, aligned with `items`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub item_orders: Option<Vec<i64>>,
    /// User-facing pending items in FIFO order.
    pub items: Vec<QueuePendingItems>,
    /// Insertion orders for immediate steering messages, aligned with `steeringMessages`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub steering_message_orders: Option<Vec<i64>>,
    /// Immediate steering messages waiting for an active turn.
    pub steering_messages: Vec<String>,
}

/// Parameters for editing a single queued message.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct QueueUpdateTextRequest {
    /// Optional replacement prompt displayed to the user.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub display_prompt: Option<String>,
    /// Stable opaque ID of the queued item to edit.
    pub id: String,
    /// Replacement prompt sent to the model.
    pub prompt: String,
}

/// Result of editing a queued message.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct QueueUpdateTextResult {
    /// True when the stored text changed.
    pub updated: bool,
}

/// Conditional withdrawal of a single user message, from its queue or from the running turn it started.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct QueueWithdrawMessageRequest {
    /// The prompt originally sent. A message edited since submission is not withdrawn, so an obsolete draft cannot replace the edit.
    pub expected_prompt: String,
    /// Message identity returned by send, not the queue item id. Batch messages are not eligible.
    pub message_id: String,
}

/// Result of withdrawing a user message.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct QueueWithdrawMessageResult {
    /// True when the running turn was interrupted to withdraw the message. With removed false, the turn was interrupted but its events could not be removed, for example because the model answered first, so the message stays in the interrupted turn.
    pub interrupted: bool,
    /// True when the message left the queue or, for a running turn, history.
    pub removed: bool,
}

/// Event type to register consumer interest for, used by runtime gating logic.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RegisterEventInterestParams {
    /// The event type the consumer wants the runtime to treat as 'observed' for behavior-switching gating. Some runtime code paths inspect whether any consumer is interested in a specific event type and choose a different implementation accordingly (e.g. `mcp.oauth_required`: when interest is registered the runtime delegates interactive OAuth token acquisition to the consumer via `mcp.oauth_required` events; when no interest is registered the runtime still attempts non-interactive reconnect from cached or refreshable tokens, and only marks the server `needs-auth` if usable credentials are unavailable — it does not open a browser or start interactive OAuth without a consumer). SDK clients that long-poll events do NOT automatically appear as listeners to these gating checks — they must explicitly call `registerInterest` for each event type they want the runtime to count as having a consumer. Multiple registrations for the same event type from the same or different consumers are tracked independently and must each be released. See: `mcp.oauth_required`, `sampling.requested`, `auto_mode_switch.requested`, `session_limits_exhausted.requested`, `user_input.requested`, `elicitation.requested`, `command.queued`, `exit_plan_mode.requested`.
    pub event_type: String,
}

/// Opaque handle representing an event-type interest registration.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RegisterEventInterestResult {
    /// Opaque handle for this registration. Pass to releaseInterest to release. Each call to registerInterest produces a fresh handle, even when the same eventType is registered multiple times.
    pub handle: String,
}

/// Opaque handle previously returned by `registerInterest` to release.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ReleaseEventInterestParams {
    /// Handle returned by a previous `registerInterest` call. Idempotent: releasing an unknown or already-released handle is a no-op (returns success). When the last outstanding handle for an event type is released, the runtime reverts to its 'no consumer' code path for that event type.
    pub handle: String,
}

/// One atomic edit of an allowed top-level repository setting.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct RepoSettingsEditRequest {
    /// One canonical top-level repository setting that the allowlist permits.
    pub key: String,
    /// Set a scalar, add or remove a string list item, or remove the setting. The handler rejects a missing or incompatible value.
    #[doc(hidden)]
    pub(crate) operation: SettingsEditOperation,
    /// Absolute path of the repository root.
    pub repo_root: String,
    /// Shared or local repository settings file.
    #[doc(hidden)]
    pub(crate) scope: RepoSettingsEditScope,
    /// Required boolean, string or number for set, required string for add-item and remove-item, and forbidden for remove. The runtime rejects all other JSON values.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub value: Option<serde_json::Value>,
}

/// Which repository settings file to read.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct RepoSettingsReadRawRequest {
    /// Absolute path of the repository root.
    pub repo_root: String,
    /// Use repo for shared settings or local for settings.local.json. Other scopes are rejected.
    pub scope: String,
}

/// Parsed repository settings without normalization or layering.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct RepoSettingsReadRawResult {
    /// Stored JSON values after the existing JSONC parser and prototype-safety checks. Empty when the file is missing, blank, or invalid.
    pub config: serde_json::Value,
    /// True when the file exists but did not parse, so `config` is empty for that reason rather than because nothing is set.
    pub invalid: bool,
}

/// The complete set of repository-overridable settings to store.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct RepoSettingsWriteAllRequest {
    /// Absolute path of the repository root.
    pub repo_root: String,
    /// Use repo for shared settings or local for settings.local.json. Other scopes are rejected.
    pub scope: String,
    /// Object of allowed settings to store. Missing allowlisted keys are removed. Other input keys are ignored, while unknown stored keys are preserved.
    pub settings: serde_json::Value,
}

/// One key to write or remove in a repository settings file.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct RepoSettingsWriteKeyRequest {
    /// True to remove the key instead of writing it.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub delete: Option<bool>,
    /// Canonical top-level key permitted by the repository settings allowlist.
    pub key: String,
    /// Absolute path of the repository root.
    pub repo_root: String,
    /// Use repo for shared settings or local for settings.local.json. Other scopes are rejected.
    pub scope: String,
    /// Value to validate and write. Required unless delete is true; a null value is validated, not treated as deletion.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub value: Option<serde_json::Value>,
}

/// Provider-native structured output format. JSON Schema is forwarded without rewriting or validating the schema or the generated output.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ResponseFormat {
    /// JSON Schema and provider options for the turn's output.
    pub json_schema: JsonSchemaResponseFormat,
    /// Output format discriminator. Currently only json_schema is supported.
    pub r#type: ResponseFormatType,
}

/// An authorization URL for the host browser.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RuntimeOpenBrowserRequest {
    /// The URL to open.
    pub url: String,
}

/// Register an absolute-time scheduled prompt.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ScheduleAddAtRequest {
    /// Epoch milliseconds when the prompt should fire.
    pub at: i64,
    /// Optional display-only prompt label.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub display_prompt: Option<String>,
    /// Prompt text to enqueue when the schedule fires.
    pub prompt: String,
    /// Whether the schedule should re-arm after each tick. Defaults to false.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub recurring: Option<bool>,
}

/// Register a cron scheduled prompt.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ScheduleAddCronRequest {
    /// 5-field cron expression.
    pub cron: String,
    /// Optional display-only prompt label.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub display_prompt: Option<String>,
    /// Prompt text to enqueue when the schedule fires.
    pub prompt: String,
    /// Whether the schedule should re-arm after each tick. Defaults to true.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub recurring: Option<bool>,
    /// IANA timezone for evaluating the cron expression.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tz: Option<String>,
}

/// Register a relative-interval scheduled prompt.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ScheduleAddRequest {
    /// Optional display-only prompt label.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub display_prompt: Option<String>,
    /// Human-readable interval such as `30s`, `5m`, or `2h`.
    pub interval: String,
    /// Prompt text to enqueue when the schedule fires.
    pub prompt: String,
    /// Whether the schedule should re-arm after each tick. Defaults to true.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub recurring: Option<bool>,
}

/// Result of registering or re-arming a scheduled prompt.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ScheduleAddResult {
    /// The registered or updated schedule entry.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub entry: Option<ScheduleEntry>,
    /// User-facing validation error, when registration failed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
}

/// Register a self-paced scheduled prompt.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ScheduleAddSelfPacedRequest {
    /// Optional display-only prompt label.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub display_prompt: Option<String>,
    /// Prompt text to enqueue when the schedule fires.
    pub prompt: String,
}

/// Whether the session currently has an active self-paced schedule.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ScheduleHasSelfPacedResult {
    /// True when at least one active schedule is self-paced.
    pub has_self_paced: bool,
}

/// Snapshot of the currently active recurring prompts for this session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ScheduleList {
    /// Active scheduled prompts, ordered by id.
    pub entries: Vec<ScheduleEntry>,
}

/// Re-arm a self-paced scheduled prompt.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ScheduleRearmSelfPacedRequest {
    /// Epoch milliseconds when the prompt should next fire.
    pub at: i64,
    /// Id of the self-paced scheduled prompt.
    pub id: i64,
}

/// Identifier of the scheduled prompt to remove.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ScheduleStopRequest {
    /// Id of the scheduled prompt to remove.
    pub id: i64,
}

/// Remove a scheduled prompt by id. The result entry is omitted if the id was unknown.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ScheduleStopResult {
    /// The removed entry, or omitted if no entry matched.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub entry: Option<ScheduleEntry>,
}

/// Secret values to add to the redaction filter.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SecretsAddFilterValuesRequest {
    /// Raw secret values to register for redaction
    pub values: Vec<String>,
}

/// Confirmation that the secret values were registered.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SecretsAddFilterValuesResult {
    /// Whether the values were successfully registered
    pub ok: bool,
}

/// Internal request-correlated completion of one submitted message.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SendAndWaitResult {
    /// Whether the request was aborted or removed before completing normally.
    pub aborted: bool,
    /// Existing event at the completion boundary. SDK helpers drain its synchronous subscriber delivery before returning. Absent for quiet queue removal.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub completion_event_id: Option<String>,
    /// Identifier assigned to the submitted user message.
    pub message_id: String,
    /// The exact existing root assistant event owned by this request. Absent when the request completed without an assistant response.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub response: Option<SessionEvent>,
}

/// Parameters for session.extensions.sendAttachmentsToMessage.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SendAttachmentsToMessageParams {
    /// Attachments to push into the next user-message turn. extension_context entries take the slim shape; standard variants take their full AttachmentSchema shape.
    pub attachments: Vec<serde_json::Value>,
    /// Optional canvas instance binding the push for provenance. When supplied, the runtime resolves the canvas, verifies it is owned by the calling extension, and stamps canvasId/instanceId onto each extension_context entry. When omitted, no resolution runs and those fields stay unset on the attachment.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub instance_id: Option<String>,
}

/// A single user message to append to the session as part of a `session.sendMessages` turn
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SendMessageItem {
    /// Optional attachments (files, directories, selections, blobs, GitHub references) to include with this message
    #[serde(skip_serializing_if = "Option::is_none")]
    pub attachments: Option<Vec<serde_json::Value>>,
    /// If false, this message will not trigger a Premium Request Unit charge. User messages default to billable.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) billable: Option<bool>,
    /// If provided, this is shown in the timeline instead of `prompt`
    #[serde(skip_serializing_if = "Option::is_none")]
    pub display_prompt: Option<String>,
    /// The user message text
    pub prompt: String,
    /// If set, the request will fail if the named tool is not available when this message is among the user messages at the start of the current exchange
    #[serde(skip_serializing_if = "Option::is_none")]
    pub required_tool: Option<String>,
    /// Optional provenance tag copied to the resulting user.message event. Must be `user`, `system`, `command-<command-id>` for command-originated messages, `schedule-<numeric-id>` for scheduled prompts, or `agent-<agent-id>` for prompts sent by another agent.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) source: Option<String>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SendMessagesRequestResponseFormat {
    /// JSON Schema and provider options for the turn's output.
    pub json_schema: JsonSchemaResponseFormat,
    /// Output format discriminator. Currently only json_schema is supported.
    pub r#type: SendMessagesRequestResponseFormatType,
}

/// Parameters for sending zero or more user messages to the session in a single turn. Remote-backed (Mission Control) sessions do not support this method and will return an error.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SendMessagesRequest {
    /// The UI mode the agent was in when these messages were sent. Defaults to the session's current mode.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub agent_mode: Option<SendAgentMode>,
    /// The user messages to append to the conversation, in order, before running one agent loop. When the batch starts a run, its final message is the primary initiating message; earlier messages provide context, not separate runs or replies. May be empty, in which case a single turn runs over the existing history with no new user message or originatingMessageId.
    pub messages: Vec<SendMessageItem>,
    /// How to deliver the messages. `enqueue` (default) appends to the message queue. `immediate` interjects during an in-progress turn.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mode: Option<SendMode>,
    /// If true, adds the messages to the front of the queue instead of the end
    #[serde(skip_serializing_if = "Option::is_none")]
    pub prepend: Option<bool>,
    /// Custom HTTP headers to include in outbound model requests for this turn. Merged with session-level provider headers; per-turn headers augment and overwrite session-level headers with the same key.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub request_headers: Option<HashMap<String, String>>,
    /// Provider-native output format for the whole turn, including an empty message batch and all tool-call iterations. Not inherited by later turns or subagents. Ordinary steering inherits the active format; specifying responseFormat with mode: immediate is an error, even while idle. Returned assistant content remains text; the runtime does not parse or validate it. Unsupported models or schemas produce provider errors.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub response_format: Option<SendMessagesRequestResponseFormat>,
    /// W3C Trace Context traceparent header for distributed tracing of this agent turn
    #[serde(skip_serializing_if = "Option::is_none")]
    pub traceparent: Option<String>,
    /// W3C Trace Context tracestate header for distributed tracing
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tracestate: Option<String>,
    /// If true, await completion of the agentic loop for this turn before returning. Defaults to false (fire-and-forget). When true, the result still contains the same `messageIds`; the caller can rely on the agent having processed the messages before the call resolves. Transport-dependent tail semantics: on a LOCAL (in-process) session the wait additionally blocks until the completed turn's event tail has been dispatched to this session's in-process subscribers, so a subsequent read of subscriber state already reflects the turn; on a REMOTE session the wait resolves once the loop completes and mirrored delivery follows over the wire. Callers that need the stronger local guarantee on remote sessions should await the event stream explicitly.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub wait: Option<bool>,
}

/// Result of sending zero or more user messages
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SendMessagesResult {
    /// Unique identifiers assigned to the messages, one per provided message in order. For a batch that starts a run, assistant messages use the final ID as originatingMessageId throughout that run, including tool iterations and stop-hook corrections. Immediate steering does not replace the active run's origin. Empty when no messages were provided; that run has no originatingMessageId.
    pub message_ids: Vec<String>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SendRequestResponseFormat {
    /// JSON Schema and provider options for the turn's output.
    pub json_schema: JsonSchemaResponseFormat,
    /// Output format discriminator. Currently only json_schema is supported.
    pub r#type: SendRequestResponseFormatType,
}

/// Parameters for sending a user message to the session
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SendRequest {
    /// The UI mode the agent was in when this message was sent. Defaults to the session's current mode.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub agent_mode: Option<SendAgentMode>,
    /// Optional attachments (files, directories, selections, blobs, GitHub references) to include with the message
    #[serde(skip_serializing_if = "Option::is_none")]
    pub attachments: Option<Vec<serde_json::Value>>,
    /// If false, this message will not trigger a Premium Request Unit charge. User messages default to billable.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub billable: Option<bool>,
    /// If provided, this is shown in the timeline instead of `prompt`
    #[serde(skip_serializing_if = "Option::is_none")]
    pub display_prompt: Option<String>,
    /// How to deliver the message. `enqueue` (default) appends to the message queue. `immediate` interjects during an in-progress turn.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mode: Option<SendMode>,
    /// If true, adds the message to the front of the queue instead of the end
    #[serde(skip_serializing_if = "Option::is_none")]
    pub prepend: Option<bool>,
    /// The user message text
    pub prompt: String,
    /// Custom HTTP headers to include in outbound model requests for this turn. Merged with session-level provider headers; per-turn headers augment and overwrite session-level headers with the same key.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub request_headers: Option<HashMap<String, String>>,
    /// If set, the request will fail if the named tool is not available when this message is among the user messages at the start of the current exchange
    #[serde(skip_serializing_if = "Option::is_none")]
    pub required_tool: Option<String>,
    /// Provider-native output format for this turn, including all tool-call iterations. Not inherited by later turns or subagents. Ordinary steering inherits the active format; specifying responseFormat with mode: immediate is an error, even while idle. Returned assistant content remains text; the runtime does not parse or validate it. Unsupported models or schemas produce provider errors.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub response_format: Option<SendRequestResponseFormat>,
    /// Optional provenance tag copied to the resulting user.message event. Must be `user`, `system`, `command-<command-id>` for command-originated messages, `schedule-<numeric-id>` for scheduled prompts, or `agent-<agent-id>` for prompts sent by another agent.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) source: Option<String>,
    /// W3C Trace Context traceparent header for distributed tracing of this agent turn
    #[serde(skip_serializing_if = "Option::is_none")]
    pub traceparent: Option<String>,
    /// W3C Trace Context tracestate header for distributed tracing
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tracestate: Option<String>,
    /// If true, await completion of the agentic loop for this message before returning. Defaults to false (fire-and-forget). When true, the result still contains the same `messageId`; the caller can rely on the agent having processed the message before the call resolves. Transport-dependent tail semantics: on a LOCAL (in-process) session the wait additionally blocks until the completed turn's event tail has been dispatched to this session's in-process subscribers, so a subsequent read of subscriber state already reflects the turn; on a REMOTE session the wait resolves once the loop completes and mirrored delivery follows over the wire. Callers that need the stronger local guarantee on remote sessions should await the event stream explicitly.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub wait: Option<bool>,
}

/// Result of sending a user message
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SendResult {
    /// Unique identifier assigned to the message
    pub message_id: String,
}

/// Internal request for sending a system notification.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SendSystemNotificationRequest {
    /// Optional structured notification kind.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub kind: Option<serde_json::Value>,
    /// Notification text to deliver to the model.
    pub message: String,
    /// Internal delivery options, including passive policy.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub options: Option<serde_json::Value>,
}

/// Outcome of an atomic user or repository settings edit.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct SettingsEditResult {
    /// Changed, unchanged or invalid existing file. Rejected input, validation failures and I/O errors remain RPC errors.
    #[doc(hidden)]
    pub(crate) status: SettingsEditStatus,
}

/// A settings value to redact before showing it.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct SettingsRedactValueForDisplayRequest {
    /// Where the value sits in the settings tree, which decides which parts of it are secret.
    pub base_path: Vec<String>,
    /// The value to redact.
    pub value: serde_json::Value,
}

/// A settings value with its secret-bearing parts replaced.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct SettingsRedactValueForDisplayResult {
    /// The value with every secret-bearing part replaced by a redacted placeholder.
    pub value: serde_json::Value,
}

/// One key to remove from a settings file, named exactly as it appears in the file.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct SettingsRemoveRawKeyRequest {
    /// Absolute path of the settings file to edit.
    pub file_path: String,
    /// The key to remove, spelled as it appears in the file. It need not be a recognized setting: this exists to remove keys the loader rejected.
    pub raw_key: String,
}

/// The outcome of the settings-file edit.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct SettingsRemoveRawKeyResult {
    /// `ok` when the edit completed, including an absent key. `invalid` means the existing file could not be parsed and remained unchanged.
    pub status: String,
}

/// User-requested shell execution cancellation handle.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ShellCancelUserRequestedRequest {
    /// Request ID previously passed to executeUserRequested
    pub request_id: RequestId,
}

/// Shell command to run, with optional working directory and timeout in milliseconds. Spawn failures return an RPC error.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ShellExecRequest {
    /// Shell command to execute
    pub command: String,
    /// Working directory (defaults to session working directory)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cwd: Option<String>,
    /// Timeout in milliseconds (default: 30000)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub timeout: Option<i64>,
}

/// Identifier of the spawned shell process, usable with shell.kill while the process is running.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ShellExecResult {
    /// Identifier usable with shell.kill while the process is running
    pub process_id: String,
}

/// User-requested shell command and cancellation handle.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ShellExecuteUserRequestedRequest {
    /// Shell command to execute
    pub command: String,
    /// Caller-provided cancellation handle for this execution
    pub request_id: RequestId,
}

/// Identifier of a process previously returned by "shell.exec" and the signal to send.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ShellKillRequest {
    /// Process identifier returned by shell.exec
    pub process_id: String,
    /// Signal to send (default: SIGTERM)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub signal: Option<ShellKillSignal>,
}

/// Indicates whether the signal was delivered; false if the process was unknown or already exited.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ShellKillResult {
    /// Whether the signal was sent successfully
    pub killed: bool,
}

/// Parameters for shutting down the session
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ShutdownRequest {
    /// Dispatch deferred sessionEnd hooks in the background with their full per-hook timeoutSec instead of awaiting them under the short shared shutdown budget. Set this when the host process keeps running after the session closes (for example the CLI's /clear), so a slow hook neither blocks the close nor is aborted. Hooks still detached when the process later exits are terminated with it. Defaults to false.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub detach_session_end_hooks: Option<bool>,
    /// Optional human-readable reason. Typically the message of the error that triggered shutdown when type is 'error'.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reason: Option<String>,
    /// Why the session is being shut down. Defaults to "routine" when omitted.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub r#type: Option<ShutdownType>,
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
pub struct SlashCommandTimelineEntry {
    /// What the user must do to recover, when the entry reports a failure the runtime knows an action for. The `text` never names a client affordance, so a client that offers one renders it from this value.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub remediation: Option<RemediationAction>,
    /// Text displayed for the timeline entry.
    pub text: String,
    /// Timeline entry presentation type.
    pub r#type: String,
    /// Optional URL associated with the timeline entry.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub url: Option<String>,
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
pub struct SlashCommandAddTimelineEntryResult {
    /// Timeline entry the host should append.
    pub entry: SlashCommandTimelineEntry,
    /// Discriminator for an add-timeline-entry result.
    pub kind: SlashCommandAddTimelineEntryResultKind,
    /// Optional text the host should prefill into the input editor.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub prefill_input: Option<String>,
    /// Whether command execution changed persisted runtime settings.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub runtime_settings_changed: Option<bool>,
}

/// Slash-command invocation result that submits an agent prompt, with display prompt, optional mode, optional user-facing notice, settings-change flag, and opaque reservation finalization token.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SlashCommandAgentPromptResult {
    /// Prompt text to display to the user
    pub display_prompt: String,
    /// Agent prompt result discriminator
    pub kind: SlashCommandAgentPromptResultKind,
    /// Optional target session mode for the agent prompt
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mode: Option<SessionMode>,
    /// Optional user-facing notice to show before the prompt is submitted
    #[serde(skip_serializing_if = "Option::is_none")]
    pub notice: Option<String>,
    /// Prompt to submit to the agent
    pub prompt: String,
    /// Opaque token used to finalize this invocation's objective prompt reservation
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reservation_token: Option<String>,
    /// True when the invocation mutated user runtime settings; consumers caching settings should refresh
    #[serde(skip_serializing_if = "Option::is_none")]
    pub runtime_settings_changed: Option<bool>,
}

/// Slash-command invocation result indicating completion, with optional message and settings-change flag.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SlashCommandCompletedResult {
    /// Completed result discriminator
    pub kind: SlashCommandCompletedResultKind,
    /// Optional user-facing message describing the completed command
    #[serde(skip_serializing_if = "Option::is_none")]
    pub message: Option<String>,
    /// Optional target session mode applied without submitting an agent prompt
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mode: Option<SessionMode>,
    /// True when the invocation mutated user runtime settings; consumers caching settings should refresh
    #[serde(skip_serializing_if = "Option::is_none")]
    pub runtime_settings_changed: Option<bool>,
}

/// Slash-command invocation result containing text output plus Markdown/ANSI rendering flags.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SlashCommandTextResult {
    /// Text result discriminator
    pub kind: SlashCommandTextResultKind,
    /// Whether text contains Markdown
    #[serde(skip_serializing_if = "Option::is_none")]
    pub markdown: Option<bool>,
    /// Whether ANSI sequences should be preserved
    #[serde(skip_serializing_if = "Option::is_none")]
    pub preserve_ansi: Option<bool>,
    /// True when the invocation mutated user runtime settings; consumers caching settings should refresh
    #[serde(skip_serializing_if = "Option::is_none")]
    pub runtime_settings_changed: Option<bool>,
    /// Present when the invocation changed the sandbox for this session only. Nothing was persisted, so consumers must mirror the change onto the live session rather than reloading settings, and must not treat it as a settings change.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sandbox_session_change: Option<SandboxSessionChange>,
    /// Text output for the client to render
    pub text: String,
}

/// Selectable slash-command subcommand option with name, description, and optional group label.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SlashCommandSelectSubcommandOption {
    /// Human-readable description of the subcommand
    pub description: String,
    /// Optional group label for organizing options
    #[serde(skip_serializing_if = "Option::is_none")]
    pub group: Option<String>,
    /// Subcommand name to invoke
    pub name: String,
}

/// Slash-command invocation result asking the client to present subcommand options for a parent command.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SlashCommandSelectSubcommandResult {
    /// Parent command name that requires subcommand selection
    pub command: String,
    /// Select subcommand result discriminator
    pub kind: SlashCommandSelectSubcommandResultKind,
    /// Available subcommand options for the client to present
    pub options: Vec<SlashCommandSelectSubcommandOption>,
    /// True when the invocation mutated user runtime settings; consumers caching settings should refresh
    #[serde(skip_serializing_if = "Option::is_none")]
    pub runtime_settings_changed: Option<bool>,
    /// Human-readable title for the selection UI
    pub title: String,
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
pub struct SlashCommandModelPickerDialog {
    /// Discriminator for a model-picker dialog.
    pub kind: SlashCommandModelPickerDialogKind,
    /// Model that should be enabled before it can be selected.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_to_enable: Option<String>,
    /// Settings scope the picker should modify.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub scope: Option<String>,
    /// Model-selection target represented by the picker.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub target: Option<String>,
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
pub struct SlashCommandShowDialogResult {
    /// Dialog the host should display.
    pub dialog: SlashCommandModelPickerDialog,
    /// Discriminator for a show-dialog result.
    pub kind: SlashCommandShowDialogResultKind,
    /// Whether command execution changed persisted runtime settings.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub runtime_settings_changed: Option<bool>,
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
pub struct SlashCommandSetModelResult {
    /// Auto routing profile selected by the command, when the model is Auto.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auto_tier: Option<AutoTier>,
    /// Discriminator for a set-model result.
    pub kind: SlashCommandSetModelResultKind,
    /// Model selected by the command.
    pub model: String,
    /// Reasoning effort selected for the model.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_effort: Option<String>,
    /// Repository settings scope modified by the command.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repo_scope: Option<String>,
    /// User-settings snapshot to restore if the host cancels the model switch.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub revert_on_cancel: Option<serde_json::Value>,
    /// Whether command execution changed persisted runtime settings.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub runtime_settings_changed: Option<bool>,
    /// Settings scope modified by the command.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub scope: Option<String>,
    /// User-facing warning produced while selecting the model.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub warning: Option<String>,
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
pub struct SlashCommandSetPlanModelResult {
    /// Discriminator for a set-plan-model result.
    pub kind: SlashCommandSetPlanModelResultKind,
    /// User-facing confirmation message for the plan-model selection.
    pub message: String,
    /// Dedicated model selected for plan mode.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub plan_model: Option<String>,
    /// Whether command execution changed persisted runtime settings.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub runtime_settings_changed: Option<bool>,
}

/// Feature override key/value pairs to attach to subsequent telemetry events from this session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TelemetrySetFeatureOverridesRequest {
    /// Override key/value pairs to attach to subsequent telemetry events from this session. Replaces any previously-set overrides.
    pub features: HashMap<String, String>,
}

/// Selectable option for a UI elicitation multi-select array item, with submitted value and display label.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationArrayAnyOfFieldItemsAnyOf {
    /// Value submitted when this option is selected.
    pub r#const: String,
    /// Display label for this option.
    pub title: String,
}

/// Schema applied to each item in the array.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationArrayAnyOfFieldItems {
    /// Selectable options, each with a value and a display label.
    pub any_of: Vec<UIElicitationArrayAnyOfFieldItemsAnyOf>,
}

/// Multi-select string field where each option pairs a value with a display label.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationArrayAnyOfField {
    /// Default values selected when the form is first shown.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub default: Option<Vec<String>>,
    /// Help text describing the field.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Schema applied to each item in the array.
    pub items: UIElicitationArrayAnyOfFieldItems,
    /// Maximum number of items the user may select.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_items: Option<i64>,
    /// Minimum number of items the user must select.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub min_items: Option<i64>,
    /// Human-readable label for the field.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
    /// Type discriminator. Always "array".
    pub r#type: UIElicitationArrayAnyOfFieldType,
}

/// Schema applied to each item in the array.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationArrayEnumFieldItems {
    /// Allowed string values for each selected item.
    pub r#enum: Vec<String>,
    /// Type discriminator. Always "string".
    pub r#type: UIElicitationArrayEnumFieldItemsType,
}

/// Multi-select string field whose allowed values are defined inline.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationArrayEnumField {
    /// Default values selected when the form is first shown.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub default: Option<Vec<String>>,
    /// Help text describing the field.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Schema applied to each item in the array.
    pub items: UIElicitationArrayEnumFieldItems,
    /// Maximum number of items the user may select.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_items: Option<i64>,
    /// Minimum number of items the user must select.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub min_items: Option<i64>,
    /// Human-readable label for the field.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
    /// Type discriminator. Always "array".
    pub r#type: UIElicitationArrayEnumFieldType,
}

/// JSON Schema describing the form fields to present to the user
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationSchema {
    /// Form field definitions, keyed by field name
    pub properties: HashMap<String, serde_json::Value>,
    /// List of required field names
    #[serde(skip_serializing_if = "Option::is_none")]
    pub required: Option<Vec<String>>,
    /// Schema type indicator (always 'object')
    pub r#type: UIElicitationSchemaType,
}

/// Prompt message and JSON schema describing the form fields to elicit from the user.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationRequest {
    /// MCP request metadata.
    #[serde(rename = "_meta", skip_serializing_if = "Option::is_none")]
    pub meta: Option<HashMap<String, serde_json::Value>>,
    /// Message describing what information is needed from the user
    pub message: String,
    /// Elicitation mode. Omitted and form are equivalent for structured elicitation.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mode: Option<McpElicitationFormMode>,
    /// JSON Schema describing the form fields to present to the user
    pub requested_schema: UIElicitationSchema,
    /// MCP task metadata.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub task: Option<McpTaskMetadata>,
}

/// The elicitation response (accept with form values, decline, or cancel)
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationResponse {
    /// MCP response metadata.
    #[serde(rename = "_meta", skip_serializing_if = "Option::is_none")]
    pub meta: Option<HashMap<String, serde_json::Value>>,
    /// The user's response: accept (submitted), decline (rejected), or cancel (dismissed)
    pub action: UIElicitationResponseAction,
    /// The form values submitted by the user (present when action is 'accept')
    #[serde(skip_serializing_if = "Option::is_none")]
    pub content: Option<HashMap<String, serde_json::Value>>,
}

/// Indicates whether the elicitation response was accepted; false if it was already resolved by another client.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationResult {
    /// Whether the response was accepted. False if the request was already resolved by another client.
    pub success: bool,
}

/// Boolean field rendered as a yes/no toggle.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationSchemaPropertyBoolean {
    /// Default value selected when the form is first shown.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub default: Option<bool>,
    /// Help text describing the field.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Human-readable label for the field.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
    /// Type discriminator. Always "boolean".
    pub r#type: UIElicitationSchemaPropertyBooleanType,
}

/// Numeric field accepting either a number or an integer.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationSchemaPropertyNumber {
    /// Default value populated in the input when the form is first shown.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub default: Option<f64>,
    /// Help text describing the field.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Maximum allowed value (inclusive).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub maximum: Option<f64>,
    /// Minimum allowed value (inclusive).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub minimum: Option<f64>,
    /// Human-readable label for the field.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
    /// Numeric type accepted by the field.
    pub r#type: UIElicitationSchemaPropertyNumberType,
}

/// Free-text string field with optional length and format constraints.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationSchemaPropertyString {
    /// Default value populated in the input when the form is first shown.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub default: Option<String>,
    /// Help text describing the field.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Optional format hint that constrains the accepted input.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub format: Option<UIElicitationSchemaPropertyStringFormat>,
    /// Maximum number of characters allowed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_length: Option<i64>,
    /// Minimum number of characters required.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub min_length: Option<i64>,
    /// Human-readable label for the field.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
    /// Type discriminator. Always "string".
    pub r#type: UIElicitationSchemaPropertyStringType,
}

/// Single-select string field whose allowed values are defined inline.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationStringEnumField {
    /// Default value selected when the form is first shown.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub default: Option<String>,
    /// Help text describing the field.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Allowed string values.
    pub r#enum: Vec<String>,
    /// Optional display labels for each enum value, in the same order as `enum`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enum_names: Option<Vec<String>>,
    /// Human-readable label for the field.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
    /// Type discriminator. Always "string".
    pub r#type: UIElicitationStringEnumFieldType,
}

/// Selectable option for a UI elicitation single-select string field, with submitted value and display label.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationStringOneOfFieldOneOf {
    /// Value submitted when this option is selected.
    pub r#const: String,
    /// Display label for this option.
    pub title: String,
}

/// Single-select string field where each option pairs a value with a display label.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationStringOneOfField {
    /// Default value selected when the form is first shown.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub default: Option<String>,
    /// Help text describing the field.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Selectable options, each with a value and a display label.
    pub one_of: Vec<UIElicitationStringOneOfFieldOneOf>,
    /// Human-readable label for the field.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
    /// Type discriminator. Always "string".
    pub r#type: UIElicitationStringOneOfFieldType,
}

/// Transient question to answer without adding it to conversation history.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIEphemeralQueryRequest {
    /// In-process `AbortSignal` forwarded to the model client to cancel an in-flight request. Internal and excluded from the public SDK surface.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) abort_signal: Option<serde_json::Value>,
    /// In-process streaming callback `(text) => void` invoked with each token as the model emits it. Internal and excluded from the public SDK surface.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) on_chunk: Option<serde_json::Value>,
    /// Question to answer from the current conversation context.
    pub question: String,
}

/// Completed transient query. Ordered chunks and the terminal outcome are also delivered through `ui.ephemeral_query` session events while it runs.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIEphemeralQueryResult {
    /// Answer returned by the model
    pub answer: String,
}

/// User response for a pending exit-plan-mode request, with approval state, selected action, auto-approve flag, and feedback.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIExitPlanModeResponse {
    /// Whether the plan was approved.
    pub approved: bool,
    /// Whether subsequent edits should be auto-approved without confirmation.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auto_approve_edits: Option<bool>,
    /// When true, the agent is instructed to end its turn without starting implementation so the client can restore the session model and auto-submit a fresh implementation turn on it. Set only when a distinct plan configuration (a different model, reasoning effort, or context tier) actually ran the planning turn.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub defer_implementation: Option<bool>,
    /// Feedback from the user when they declined the plan or requested changes.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub feedback: Option<String>,
    /// The action the user selected. Defaults to 'autopilot' when autoApproveEdits is true, otherwise 'interactive'.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub selected_action: Option<UIExitPlanModeAction>,
}

/// Request ID of a pending `auto_mode_switch.requested` event and the user's response.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIHandlePendingAutoModeSwitchRequest {
    /// The unique request ID from the auto_mode_switch.requested event
    pub request_id: RequestId,
    /// User's choice for auto-mode switching: yes (allow this turn), yes_always (allow + persist as setting), or no (decline).
    pub response: UIAutoModeSwitchResponse,
}

/// Pending elicitation request ID and the user's response (accept/decline/cancel + form values).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIHandlePendingElicitationRequest {
    /// The unique request ID from the elicitation.requested event
    pub request_id: RequestId,
    /// The elicitation response (accept with form values, decline, or cancel)
    pub result: UIElicitationResponse,
}

/// Request ID of a pending `exit_plan_mode.requested` event and the user's response.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIHandlePendingExitPlanModeRequest {
    /// The unique request ID from the exit_plan_mode.requested event
    pub request_id: RequestId,
    /// User response for a pending exit-plan-mode request, with approval state, selected action, auto-approve flag, and feedback.
    pub response: UIExitPlanModeResponse,
}

/// Indicates whether the pending UI request was resolved by this call.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIHandlePendingResult {
    /// True if the request was still pending and was resolved by this call. False if the request ID was unknown, already resolved by another client (e.g. GitHub), expired, or otherwise no longer pending.
    pub success: bool,
}

/// Optional sampling result payload. Omit to reject/cancel the sampling request without providing a result.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIHandlePendingSamplingResponse {}

/// Request ID of a pending `sampling.requested` event and an optional sampling result payload (omit to reject).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIHandlePendingSamplingRequest {
    /// The unique request ID from the sampling.requested event
    pub request_id: RequestId,
    /// Optional sampling result payload. Omit to reject/cancel the sampling request without providing a result.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub response: Option<UIHandlePendingSamplingResponse>,
}

/// The user's selected action for an exhausted session limit.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UISessionLimitsExhaustedResponse {
    /// Action selected by the user.
    pub action: UISessionLimitsExhaustedResponseAction,
    /// AI Credits to add to the current max when action is 'add'.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub additional_ai_credits: Option<f64>,
    /// New absolute max AI Credits when action is 'set'.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_ai_credits: Option<f64>,
}

/// Request ID of a pending `session_limits_exhausted.requested` event and the user's selected limit action.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIHandlePendingSessionLimitsExhaustedRequest {
    /// The unique request ID from the session_limits_exhausted.requested event
    pub request_id: RequestId,
    /// The selected session-limit action.
    pub response: UISessionLimitsExhaustedResponse,
}

/// User response for a pending user-input request, with answer text and whether it was typed freeform.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIUserInputResponse {
    /// The user's answer text
    pub answer: String,
    /// True if the user typed a freeform response, false if they selected a presented choice. Used by telemetry to differentiate between free text input and choice selection.
    pub was_freeform: bool,
}

/// Request ID of a pending `user_input.requested` event and the user's response.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIHandlePendingUserInputRequest {
    /// The unique request ID from the user_input.requested event
    pub request_id: RequestId,
    /// User response for a pending user-input request, with answer text and whether it was typed freeform.
    pub response: UIUserInputResponse,
}

/// Register an in-process handler for `auto_mode_switch.requested` events. The caller still attaches the actual listener via the standard event-subscription mechanism; this registration solely tells the server bridge to skip its own dispatch (so a remote client doesn't race the in-process handler for the same requestId).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIRegisterDirectAutoModeSwitchHandlerResult {
    /// Opaque handle representing the registration. Pass this same handle to `unregisterDirectAutoModeSwitchHandler` when the in-process handler is no longer active. Multiple registrations are reference-counted; the server bridge will only dispatch auto-mode-switch requests when no handles are active.
    pub handle: String,
}

/// Identifies one runtime-owned native action for an in-process SDK UI dispatcher.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UiThreadRunRequest {
    /// Opaque, one-shot action identifier scoped to the receiving native connection. Execute only through the synchronous native UI-action entry point.
    pub action_id: i64,
}

/// Opaque handle previously returned by `registerDirectAutoModeSwitchHandler` to release.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIUnregisterDirectAutoModeSwitchHandlerRequest {
    /// Handle previously returned by `registerDirectAutoModeSwitchHandler`
    pub handle: String,
}

/// Indicates whether the handle was active and the registration count was decremented.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIUnregisterDirectAutoModeSwitchHandlerResult {
    /// True if the handle was active and decremented the counter; false if the handle was unknown.
    pub unregistered: bool,
}

/// Subagent settings to apply to the current session
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UpdateSubagentSettingsRequest {
    /// Subagent settings to apply, or null to clear the live session override
    pub subagents: Option<SubagentSettingsConfig>,
}

/// Internal absolute code-change totals reported by the owning host.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct UsageSetCodeChangesRequest {
    /// Absolute changed-file count; omission preserves the previous count.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub files_count: Option<f64>,
    /// Absolute added-line total, replacing the previous reading.
    pub lines_added: f64,
    /// Absolute removed-line total, replacing the previous reading.
    pub lines_removed: f64,
}

/// Current sharing status and shareable GitHub URL for a session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct VisibilityGetResult {
    /// Shareable GitHub URL for the session. Present when the session is synced and the URL can be resolved.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub share_url: Option<String>,
    /// Current sharing status. Absent when the session is not synced or the status could not be retrieved (e.g. the user is not authenticated).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub status: Option<SessionVisibilityStatus>,
    /// Whether the session has been synced to Mission Control (i.e. has a GitHub task). When false, the session cannot be shared and `status`/`shareUrl` are absent.
    pub synced: bool,
}

/// Desired sharing status for the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct VisibilitySetRequest {
    /// Sharing status to apply. "repo" makes the session visible to repository readers; "unshared" restricts it to the creator and collaborators.
    pub status: SessionVisibilityStatus,
}

/// Effective sharing status and shareable GitHub URL after updating session visibility.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct VisibilitySetResult {
    /// Shareable GitHub URL for the session. Present when the session is synced and the URL can be resolved.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub share_url: Option<String>,
    /// Effective sharing status after the update. May differ from the requested status for task types that are already visible to repository readers by default. Absent when the update could not be applied (e.g. the session is not synced or the user is not authenticated).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub status: Option<SessionVisibilityStatus>,
    /// Whether the session has been synced to Mission Control (i.e. has a GitHub task). When false, the visibility change could not be applied and `status`/`shareUrl` are absent.
    pub synced: bool,
}

/// Outcome of an atomic user or repository settings edit.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct RepoSettingsEditResult {
    /// Changed, unchanged or invalid existing file. Rejected input, validation failures and I/O errors remain RPC errors.
    #[doc(hidden)]
    pub(crate) status: SettingsEditStatus,
}

/// The resident session the application has materialized on its own connection.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HostCreateSessionResult {
    /// Runtime session UUID materialized on the application's SDK connection.
    pub session_id: SessionId,
}

/// Empty acknowledgement for a completed host lifecycle operation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HostReleaseSessionResult {}

/// Empty acknowledgement for a completed host lifecycle operation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HostDisposeResult {}

/// Empty acknowledgement for a completed host lifecycle operation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HostSetSessionCreationPolicyResult {}

/// Private credentials delivered only to a runtime-owned Mission Control hosting participant.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HostGetEnvironmentCredentialsResult {
    /// GitHub API base URL for the authenticated service.
    pub github_api_url: String,
    /// Hostname of the authenticated GitHub service.
    pub github_host: String,
    /// Mission Control API origin for environment registration and management.
    pub mission_control_url: String,
    /// Current bearer token for the authenticated GitHub identity.
    pub token: String,
}

/// Normalized listener settings delivered only to the supervised hosting participant.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HostGetConfigurationResult {
    /// Initial remote creation policy, independent of application session factories. Defaults to denied when omitted.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub allow_creating_new_sessions: Option<bool>,
    /// Explicit host-wide admission policy. Always supplied by the runtime after validating host.start.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub authentication: Option<serde_json::Value>,
    /// Requested GitHub Mission Control registration.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub github_environment: Option<HostGitHubEnvironmentOptions>,
    /// Normalized local listener settings, absent for relay-only hosts.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub local_server: Option<HostLocalServerConfiguration>,
    /// Whether app-owned durable sessions are resumed by the owning application.
    pub resume_factory: bool,
    /// Whether session materialization is delegated to the owning application.
    pub session_factory: bool,
}

/// Private credentials delivered only to a runtime-owned Mission Control hosting participant.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HostGetAuthenticationCredentialsResult {
    /// GitHub API base URL for the authenticated service.
    pub github_api_url: String,
    /// Hostname of the authenticated GitHub service.
    pub github_host: String,
    /// Mission Control API origin for environment registration and management.
    pub mission_control_url: String,
    /// Current bearer token for the authenticated GitHub identity.
    pub token: String,
}

/// Empty acknowledgement for a completed host lifecycle operation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HostCancelAuthenticationResult {}

/// Empty acknowledgement for a completed host lifecycle operation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HostReadyResult {}

/// Eligible Azure ML model version and its provider metadata.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AzureMlLatestResult {
    /// Provider metadata retained for registration.
    pub metadata: serde_json::Value,
    /// Stable model-family name.
    pub name: String,
    /// Validated Windows ML requirements.
    pub profile: AzureMlProfile,
    /// Registry that owns this model version.
    pub registry: AzureMlRegistry,
    /// Exact published model version.
    pub version: String,
}

/// Opaque handle to a connection-owned family mutation interval.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AzureMlAcquireFamilyResult {
    /// Server-generated handle, valid only on the originating SDK connection.
    pub lease_id: String,
}

/// Empty acknowledgement for a completed host lifecycle operation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AzureMlReleaseFamilyResult {}

/// Empty acknowledgement for a completed host lifecycle operation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AzureMlDiscardResult {}

/// The prompt the init command runs.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct InstructionsGetInitPromptResult {
    /// Prompt text authoring repository instructions.
    pub prompt: String,
}

/// Instruction sources discovered across user, repository, and plugin sources.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InstructionsDiscoverResult {
    /// All discovered instruction sources
    pub sources: Vec<InstructionSource>,
}

/// Instruction sources discovered across user, repository, and plugin sources.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InstructionsDiscoverRepoSourcesResult {
    /// All discovered instruction sources
    pub sources: Vec<InstructionSource>,
}

/// Canonical files and directories where custom instructions can be created so the runtime will recognize them.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InstructionsGetDiscoveryPathsResult {
    /// Canonical instruction create/discovery files and directories, in priority order
    pub paths: Vec<InstructionDiscoveryPath>,
}

/// Agents discovered across user, project, plugin, and remote sources.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentsDiscoverResult {
    /// All discovered agents across all sources
    pub agents: Vec<AgentInfo>,
}

/// Canonical locations where custom agents can be created so the runtime will recognize them.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentsGetDiscoveryPathsResult {
    /// Canonical agent create/discovery directories, in priority order
    pub paths: Vec<AgentDiscoveryPath>,
}

/// The host's machine-wide state. Every field is optional because a fresh install has recorded nothing yet, so a reader must treat an absent field as `not yet`, never as a negative answer. Stored credentials are deliberately absent from this shape.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GlobalStateLoadForConfigDirResult {
    /// Whether the app tip has been shown.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub app_tip_shown: Option<bool>,
    /// Terminals the user has already been asked to set up, so the host does not ask twice.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub asked_setup_terminals: Option<Vec<String>>,
    /// When the Auto-feedback hint was last shown, as an ISO 8601 timestamp. It enforces the once-per-day cap for non-staff users across restarts.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auto_feedback_last_prompted_at: Option<String>,
    /// When the host first ran on this machine.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub first_launch_at: Option<String>,
    /// Plugins installed on this machine.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub installed_plugins: Option<Vec<InstalledPlugin>>,
    /// Account used for the most recent sign-in.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) last_logged_in_user: Option<LoggedInUser>,
    /// Every account the host has signed in to on this machine.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) logged_in_users: Option<Vec<LoggedInUser>>,
    /// Whether the one-off cleanup of stored reasoning summaries has run.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_summaries_cleanup_done: Option<bool>,
    /// Models the user selected recently, most recent first.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub recent_model_ids: Option<Vec<String>>,
    /// Whether the user declined to trust the sandbox credential proxy CA.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sandbox_credential_proxy_ca_declined: Option<bool>,
    /// Whether the sandbox onboarding has been shown.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sandbox_onboarding_shown: Option<bool>,
    /// Whether the user is a GitHub or Microsoft staff member, which unlocks internal-only behavior.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub staff: Option<bool>,
    /// Whether the user was recognized as GitHub staff.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub staff_github: Option<bool>,
    /// When the staff-only log level migration last ran.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub staff_log_level_migration_at: Option<String>,
    /// Whether the user was recognized as Microsoft staff.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub staff_microsoft: Option<bool>,
    /// When the staff-only model reset last ran.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub staff_model_reset_at: Option<String>,
    /// When the staff-only update channel migration last ran.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub staff_update_channel_migration_at: Option<String>,
    /// Folders where the user declined the init prompt, so it stays hidden there.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub suppress_init_folders: Option<Vec<String>>,
    /// Folders the user has marked as trusted.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub trusted_folders: Option<Vec<String>>,
}

/// Slash commands available in the session, after applying any include/exclude filters.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CommandsListResult {
    /// Commands available in this session
    pub commands: Vec<SlashCommandInfo>,
}

/// Feature availability.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ConnectorsGetCapabilitiesResult {
    /// API version.
    pub api_version: i64,
    /// Availability.
    pub availability: ConnectorDiscoveryAvailability,
    /// Whether results are cached.
    pub conditional_cache: bool,
    /// Whether accounts are selected by opaque ID.
    pub opaque_account_selection: bool,
}

/// Eligible accounts.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ConnectorsGetAccountsResult {
    /// Eligible accounts.
    pub accounts: Vec<ConnectorDiscoveryAccount>,
    /// Availability.
    pub availability: ConnectorDiscoveryAvailability,
}

/// Entries for the selected account.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ConnectorsListResult {
    /// Opaque account ID.
    pub account_id: String,
    /// Entries.
    pub connectors: Vec<ConnectorDiscoveryCatalogEntry>,
    /// Refresh time in Unix epoch milliseconds.
    pub refreshed_at_ms: i64,
    /// Revision.
    pub revision: i64,
}

/// Entries for the selected account.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ConnectorsRefreshResult {
    /// Opaque account ID.
    pub account_id: String,
    /// Entries.
    pub connectors: Vec<ConnectorDiscoveryCatalogEntry>,
    /// Refresh time in Unix epoch milliseconds.
    pub refreshed_at_ms: i64,
    /// Revision.
    pub revision: i64,
}

/// Whether an active connection-owned transfer accepted cancellation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type AzureMlCancelResult = bool;

/// Active Azure ML download progress or null.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type AzureMlStatusResult = Option<AzureMlDownloadStatus>;

/// SHA-256 digest encoded as exactly 64 lowercase hexadecimal characters.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type CardDigestValue = String;

/// Bounded extensible wire-feature identifier. Known values are described by `CatalogCapability`; newer callers may send future identifiers so an older runtime can return a typed negotiation refusal instead of failing schema validation. Capability negotiation establishes contract understanding, while each operation's result separately reports runtime availability.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type CatalogCapabilityId = String;

/// Canonical catalogue resource identity. The runtime rewrites accepted urn:ai and urn:air identifiers to urn:air, lowercases the authority, and preserves the remaining resource components.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type CatalogResourceIdentity = String;

/// Bounded source-declared resource version, preserved exactly after validation and never inferred.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type CatalogResourceVersion = String;

/// Session account selection, or null.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type ConnectorSessionAccountResult = Option<ConnectorSessionAccount>;

/// Validated admission credential, or null when the application rejects it.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type HostAuthenticateResult = Option<HostAuthenticationResult>;

/// HTTP headers as a map from lowercased header name to a list of values. Multi-valued headers (e.g. Set-Cookie) preserve all values.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type LlmInferenceHeaders = HashMap<String, Vec<String>>;

/// Pending warnings returned and cleared by session.quota.takeWarnings.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type QuotaTakeWarningsResult = Vec<QuotaWarningProjection>;

/// Configured subagent settings, or null when absent
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type SubagentSettings = Option<SubagentSettingsConfig>;

/// The form values submitted by the user (present when action is 'accept')
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type UIElicitationResponseContent = HashMap<String, serde_json::Value>;

/// Indicates whether the command was accepted into the local execution queue.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum EnqueueCommandResult {
    AcceptedEnqueueCommandResult(AcceptedEnqueueCommandResult),
    UnsupportedEnqueueCommandResult(UnsupportedEnqueueCommandResult),
}

/// Which tier this directory belongs to
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AgentDiscoveryPathScope {
    /// The user's personal agent configuration directory.
    #[serde(rename = "user")]
    User,
    /// A project's repository agent directory.
    #[serde(rename = "project")]
    Project,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Kind of attention required when status === "attention". Meaningful only when status === "attention".
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AgentRegistryLiveTargetEntryAttentionKind {
    /// Session is blocked on an unrecoverable error
    #[serde(rename = "error")]
    Error,
    /// Session is waiting for a tool-permission decision
    #[serde(rename = "permission")]
    Permission,
    /// Session is waiting for the user to approve or reject a plan
    #[serde(rename = "exit_plan")]
    ExitPlan,
    /// Session is waiting on an elicitation prompt
    #[serde(rename = "elicitation")]
    Elicitation,
    /// Session is waiting for free-form user input
    #[serde(rename = "user_input")]
    UserInput,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Process kind tag for the registry entry
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AgentRegistryLiveTargetEntryKind {
    /// Interactive Copilot CLI exposing a UI server (legacy/normal CLI process)
    #[serde(rename = "ui-server")]
    UiServer,
    /// Headless `--server --managed-server` child spawned by a controller
    #[serde(rename = "managed-server")]
    ManagedServer,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// How the most recent turn ended (clean vs aborted). Lets the renderer distinguish done from done_cancelled.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AgentRegistryLiveTargetEntryLastTerminalEvent {
    /// Last turn ended cleanly (model returned a final assistant message)
    #[serde(rename = "turn_end")]
    TurnEnd,
    /// Last turn was aborted (e.g. user interrupted)
    #[serde(rename = "abort")]
    Abort,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Coarse lifecycle status of the foreground session
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AgentRegistryLiveTargetEntryStatus {
    /// Session is actively processing a turn
    #[serde(rename = "working")]
    Working,
    /// Session is idle, waiting for input
    #[serde(rename = "waiting")]
    Waiting,
    /// Last turn completed successfully
    #[serde(rename = "done")]
    Done,
    /// Session needs user attention (see attentionKind for the specific reason)
    #[serde(rename = "attention")]
    Attention,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Categorized reason no canonical process log could be opened
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AgentRegistryLogCaptureOpenErrorReason {
    /// Filesystem permission denied opening the log file
    #[serde(rename = "permission")]
    Permission,
    /// No space left on device
    #[serde(rename = "disk_full")]
    DiskFull,
    /// Other / uncategorized open failure
    #[serde(rename = "other")]
    Other,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Discriminator: child_process.spawn itself failed
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AgentRegistrySpawnErrorKind {
    #[serde(rename = "spawn-error")]
    #[default]
    SpawnError,
}

/// Permission posture for the new session. 'yolo' requires the controller-local session to currently be in allow-all mode.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AgentRegistrySpawnPermissionMode {
    /// Standard permission posture (prompts for each request)
    #[serde(rename = "default")]
    Default,
    /// Full allow-all (requires the controller-local session to currently be in allow-all mode)
    #[serde(rename = "yolo")]
    Yolo,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Discriminator: spawn succeeded but child never registered
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AgentRegistrySpawnRegistryTimeoutKind {
    #[serde(rename = "registry-timeout")]
    #[default]
    RegistryTimeout,
}

/// Discriminator: managed-server child spawned successfully
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AgentRegistrySpawnSpawnedKind {
    #[serde(rename = "spawned")]
    #[default]
    Spawned,
}

/// Which parameter field was invalid. Omitted when the rejection is not field-specific.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AgentRegistrySpawnValidationErrorField {
    /// The cwd parameter
    #[serde(rename = "cwd")]
    Cwd,
    /// The session name parameter
    #[serde(rename = "name")]
    Name,
    /// The agentName parameter
    #[serde(rename = "agentName")]
    AgentName,
    /// The model parameter
    #[serde(rename = "model")]
    Model,
    /// The permissionMode parameter
    #[serde(rename = "permissionMode")]
    PermissionMode,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Discriminator: synchronous pre-validation rejected the request
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AgentRegistrySpawnValidationErrorKind {
    #[serde(rename = "validation-error")]
    #[default]
    ValidationError,
}

/// Categorized reason for the rejection. Low-cardinality enum so telemetry can aggregate by reason without leaking raw paths or agent/model names.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AgentRegistrySpawnValidationErrorReason {
    /// Provided cwd does not exist on disk
    #[serde(rename = "cwd-not-found")]
    CwdNotFound,
    /// Provided cwd exists but is not a directory
    #[serde(rename = "cwd-not-directory")]
    CwdNotDirectory,
    /// Session name failed validateSessionName
    #[serde(rename = "invalid-name")]
    InvalidName,
    /// Requested agent name was not found in builtin or custom agents
    #[serde(rename = "unknown-agent")]
    UnknownAgent,
    /// Requested model is not available to this session
    #[serde(rename = "unknown-model")]
    UnknownModel,
    /// Caller asked for permissionMode='yolo' but the controller is not currently in allow-all mode
    #[serde(rename = "yolo-not-allowed")]
    YoloNotAllowed,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Outcome of an agentRegistry.spawn call.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum AgentRegistrySpawnResult {
    Spawned(AgentRegistrySpawnSpawned),
    SpawnError(AgentRegistrySpawnError),
    RegistryTimeout(AgentRegistrySpawnRegistryTimeout),
    ValidationError(AgentRegistrySpawnValidationError),
}

/// Attachment type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AttachmentBlobType {
    #[serde(rename = "blob")]
    #[default]
    Blob,
}

/// Attachment type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AttachmentDirectoryType {
    #[serde(rename = "directory")]
    #[default]
    Directory,
}

/// Attachment type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AttachmentExtensionContextType {
    #[serde(rename = "extension_context")]
    #[default]
    ExtensionContext,
}

/// Attachment type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AttachmentFileType {
    #[serde(rename = "file")]
    #[default]
    File,
}

/// Attachment type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AttachmentGitHubActionsJobType {
    #[serde(rename = "github_actions_job")]
    #[default]
    GitHubActionsJob,
}

/// Attachment type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AttachmentGitHubCommitType {
    #[serde(rename = "github_commit")]
    #[default]
    GitHubCommit,
}

/// Attachment type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AttachmentGitHubFileType {
    #[serde(rename = "github_file")]
    #[default]
    GitHubFile,
}

/// Attachment type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AttachmentGitHubFileDiffType {
    #[serde(rename = "github_file_diff")]
    #[default]
    GitHubFileDiff,
}

/// Type of GitHub reference
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AttachmentGitHubReferenceType {
    /// GitHub issue reference.
    #[serde(rename = "issue")]
    Issue,
    /// GitHub pull request reference.
    #[serde(rename = "pr")]
    Pr,
    /// GitHub discussion reference.
    #[serde(rename = "discussion")]
    Discussion,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Attachment type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AttachmentGitHubReleaseType {
    #[serde(rename = "github_release")]
    #[default]
    GitHubRelease,
}

/// Attachment type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AttachmentGitHubRepositoryType {
    #[serde(rename = "github_repository")]
    #[default]
    GitHubRepository,
}

/// Attachment type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AttachmentGitHubSnippetType {
    #[serde(rename = "github_snippet")]
    #[default]
    GitHubSnippet,
}

/// Attachment type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AttachmentGitHubTreeComparisonType {
    #[serde(rename = "github_tree_comparison")]
    #[default]
    GitHubTreeComparison,
}

/// Attachment type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AttachmentGitHubUrlType {
    #[serde(rename = "github_url")]
    #[default]
    GitHubUrl,
}

/// Attachment type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AttachmentSelectionType {
    #[serde(rename = "selection")]
    #[default]
    Selection,
}

/// How catalog discovery handles a recent sign-in failure.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AzureMlCatalogRetryMode {
    /// Reuse a recent failure for automatic background discovery.
    #[serde(rename = "background")]
    Background,
    /// Retry sign-in after an explicit refresh.
    #[serde(rename = "explicit")]
    Explicit,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Registry that owns an Azure ML model version.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AzureMlRegistry {
    /// Public Azure ML model registry.
    #[serde(rename = "azureml")]
    Azureml,
    /// Microsoft staff development registry.
    #[serde(rename = "azureml-dev")]
    AzuremlDev,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Explicit Agent Plugin compatibility declared by exact catalog tags. Clients must not infer these values from display text or other metadata.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogAgentPluginCompatibilityTag {
    /// The plugin contributes at least one GitHub Copilot Canvas.
    #[serde(rename = "canvas")]
    Canvas,
    /// The plugin depends on Canvas for its intended functionality.
    #[serde(rename = "canvas-only")]
    CanvasOnly,
    /// The plugin targets GitHub Copilot.
    #[serde(rename = "github-copilot")]
    GitHubCopilot,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Typed installability state for an Agent Plugin candidate
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogAgentPluginInstallability {
    /// The candidate carries complete, verified install data and the selected session may install it. Its install plan is present.
    #[serde(rename = "installable")]
    Installable,
    /// The catalogue supplied no complete, verified install data, so no install target is offered.
    #[serde(rename = "install-data-unavailable")]
    InstallDataUnavailable,
    /// Plugin installation is understood but disabled for the selected session.
    #[serde(rename = "feature-disabled")]
    FeatureDisabled,
    /// Reserved for plugin formats this runtime cannot install. Every format surfaced today installs through a marketplace.
    #[serde(rename = "not-installable-kind")]
    NotInstallableKind,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// A kind of component an Agent Plugin contributes
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogAgentPluginComponent {
    /// Custom agents.
    #[serde(rename = "agent")]
    Agent,
    /// Slash commands.
    #[serde(rename = "command")]
    Command,
    /// Skills.
    #[serde(rename = "skill")]
    Skill,
    /// Hooks.
    #[serde(rename = "hook")]
    Hook,
    /// Bundled MCP servers.
    #[serde(rename = "mcp-server")]
    McpServer,
    /// Bundled LSP servers.
    #[serde(rename = "lsp-server")]
    LspServer,
    /// Extensions.
    #[serde(rename = "extension")]
    Extension,
    /// Canvases.
    #[serde(rename = "canvas")]
    Canvas,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Discriminator for a materialisation install source
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogAgentPluginMaterializationSourceKind {
    /// Install from Agent Finder's verified materialisation.
    #[serde(rename = "materialization")]
    Materialization,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Discriminator for a marketplace install source
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogAgentPluginMarketplaceSourceKind {
    /// Install from a plugin marketplace.
    #[serde(rename = "marketplace")]
    Marketplace,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Discriminator for a repository install source
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogAgentPluginRepositorySourceKind {
    /// Install directly from a GitHub repository.
    #[serde(rename = "repository")]
    Repository,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// One place an Agent Plugin can install from: Agent Finder's materialisation, a marketplace, or a GitHub repository. Each variant carries only its own fields.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum CatalogAgentPluginInstallSource {
    Materialization(CatalogAgentPluginMaterializationSource),
    Marketplace(CatalogAgentPluginMarketplaceSource),
    Repository(CatalogAgentPluginRepositorySource),
}

/// Discriminator for an Agent Plugin candidate
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogAgentPluginCandidateKind {
    /// An Agent Plugin.
    #[serde(rename = "plugin")]
    Plugin,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Media type naming the plugin format of an Agent Plugin candidate. Values other than `application/vnd.github.copilot-plugin` appear only when the caller requires the agent-plugin-formats capability.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogAgentPluginMediaType {
    /// A GitHub Copilot Agent Plugin descriptor.
    #[serde(rename = "application/vnd.github.copilot-plugin")]
    ApplicationVndGitHubCopilotPlugin,
    /// A Claude plugin descriptor (`.claude-plugin/plugin.json` layout).
    #[serde(rename = "application/vnd.anthropic.claude-plugin+json")]
    ApplicationVndAnthropicClaudePluginJson,
    /// A portable Agent Plugin descriptor.
    #[serde(rename = "application/agent-plugin+json")]
    ApplicationAgentPluginJson,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Typed installability state for an AI skill candidate
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogAiSkillInstallability {
    /// This AI skill candidate carries verified materialisation metadata and the selected session may plan installation.
    #[serde(rename = "installable")]
    Installable,
    /// Skill installation is understood but disabled for the selected session.
    #[serde(rename = "feature-disabled")]
    FeatureDisabled,
    /// The candidate lacks verified materialisation metadata required for installation.
    #[serde(rename = "materialisation-unavailable")]
    MaterialisationUnavailable,
    /// Policy refuses Skill installation for the selected session or authority.
    #[serde(rename = "policy-forbids")]
    PolicyForbids,
    /// Compatibility value for discovery-only callers that did not negotiate Skill installation.
    #[serde(rename = "not-installable-kind")]
    NotInstallableKind,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Discriminator for an AI skill candidate
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogAiSkillCandidateKind {
    /// An AI skill.
    #[serde(rename = "ai-skill")]
    AiSkill,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Canonical AI skill media type
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogAiSkillMediaType {
    /// An AI skill card.
    #[serde(rename = "application/ai-skill")]
    ApplicationAiSkill,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Discriminator: the card is URL-backed, and carries no embedded data
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogCandidateSourceUrlKind {
    #[serde(rename = "url")]
    #[default]
    Url,
}

/// Discriminator: the card is embedded, and carries no URL
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogCandidateSourceEmbeddedKind {
    #[serde(rename = "embedded")]
    #[default]
    Embedded,
}

/// Where a candidate's card came from. Exactly one of a URL or embedded data: the union has no variant carrying both, and no variant carrying neither, so the rule holds structurally rather than by validation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum CatalogCandidateSource {
    Url(CatalogCandidateSourceUrl),
    Embedded(CatalogCandidateSourceEmbedded),
}

/// Whether an MCP server candidate can be planned for installation
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogMcpServerInstallability {
    /// An install plan can be computed for this MCP server candidate.
    #[serde(rename = "installable")]
    Installable,
    /// Policy forbids installing this MCP server candidate.
    #[serde(rename = "not-installable-policy")]
    NotInstallablePolicy,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Discriminator for an MCP server candidate
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogMcpServerCandidateKind {
    /// An MCP server.
    #[serde(rename = "mcp-server")]
    McpServer,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// One inert catalog result, represented as an MCP server, discovery-only AI skill, or opt-in Agent Plugin variant so kind, media type, provenance, and available operations cannot contradict each other.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum CatalogCandidate {
    McpServer(CatalogMcpServerCandidate),
    AiSkill(CatalogAiSkillCandidate),
    Plugin(CatalogAgentPluginCandidate),
}

/// What kind of resource a catalog candidate describes
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogCandidateKind {
    /// An MCP server, which can be planned for installation.
    #[serde(rename = "mcp-server")]
    McpServer,
    /// An AI skill, which is discoverable but not installable through this surface.
    #[serde(rename = "ai-skill")]
    AiSkill,
    /// An inert Agent Plugin candidate, available only when explicitly requested.
    #[serde(rename = "plugin")]
    Plugin,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Discriminator for an available feed entry
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogFeedAvailableStatus {
    /// The entry's resource resolves and can be presented.
    #[serde(rename = "available")]
    Available,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Discriminator for a feed entry filtered by trust tier
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogFeedFilteredStatus {
    /// The entry's resource is not in a requested trust tier.
    #[serde(rename = "filtered")]
    Filtered,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Why a feed entry cannot be presented
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogFeedUnavailableReason {
    /// The identifier no longer resolves to a resource.
    #[serde(rename = "not-found")]
    NotFound,
    /// The resource is of a kind this caller did not negotiate or this runtime does not support.
    #[serde(rename = "unsupported-kind")]
    UnsupportedKind,
    /// The resource failed the same validation catalog.search applies, or repeats an earlier entry.
    #[serde(rename = "invalid-resource")]
    InvalidResource,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Discriminator for an unavailable feed entry
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogFeedUnavailableStatus {
    /// The entry's resource cannot be presented.
    #[serde(rename = "unavailable")]
    Unavailable,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Service-computed trust tier currently emitted by Agent Finder. It is independent of search score, popularity, and client-side ranking.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogTrustTier {
    /// Tier one as assigned by the catalogue authority.
    T1,
    /// Tier two as assigned by the catalogue authority.
    T2,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// One feed entry, in its feed position. Only available entries carry a candidate.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum CatalogFeedEntry {
    Available(CatalogFeedAvailableEntry),
    Unavailable(CatalogFeedUnavailableEntry),
    Filtered(CatalogFeedFilteredEntry),
}

/// Discriminator: the feed was retrieved
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogGetGeneratedFeedSucceededKind {
    #[serde(rename = "succeeded")]
    #[default]
    Succeeded,
}

/// Discriminator: the feed was retrieved
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogGetSavedFeedSucceededKind {
    #[serde(rename = "succeeded")]
    #[default]
    Succeeded,
}

/// Bounded refusal categories for planning from a materialisation reference.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogReferenceInstallationFailureReason {
    /// The selected session does not have the installation feature flag for the referenced kind enabled.
    #[serde(rename = "feature-disabled")]
    FeatureDisabled,
    /// The request is unsupported or malformed.
    #[serde(rename = "invalid-request")]
    InvalidRequest,
    /// The bounded operation limit was reached.
    #[serde(rename = "operation-limit")]
    OperationLimit,
    /// Installation storage or admission is busy.
    #[serde(rename = "busy")]
    Busy,
    /// The original host cannot receive human confirmation.
    #[serde(rename = "confirmation-unavailable")]
    ConfirmationUnavailable,
    /// Existing authenticated session and host authority is unavailable.
    #[serde(rename = "policy-context-unavailable")]
    PolicyContextUnavailable,
    /// Managed policy forbids installing this resource.
    #[serde(rename = "policy-blocked")]
    PolicyBlocked,
    /// The materialisation reference is malformed or uses an unsupported schema version.
    #[serde(rename = "invalid-reference")]
    InvalidReference,
    /// The materialisation descriptor could not be retrieved safely.
    #[serde(rename = "descriptor-unavailable")]
    DescriptorUnavailable,
    /// Agent Finder has no materialisation for the referenced resource revision (HTTP 404).
    #[serde(rename = "descriptor-not-found")]
    DescriptorNotFound,
    /// Agent Finder withdrew the referenced resource revision (HTTP 410).
    #[serde(rename = "descriptor-withdrawn")]
    DescriptorWithdrawn,
    /// The materialisation descriptor failed validation.
    #[serde(rename = "descriptor-invalid")]
    DescriptorInvalid,
    /// The retrieved descriptor does not match the referenced descriptor or bundle digest.
    #[serde(rename = "digest-mismatch")]
    DigestMismatch,
    /// The referenced resource is a kind this runtime cannot install, or one whose installation capability the caller did not require.
    #[serde(rename = "unsupported-resource")]
    UnsupportedResource,
    /// The resource's entrypoint could not be retrieved or verified.
    #[serde(rename = "entrypoint-unavailable")]
    EntrypointUnavailable,
    /// GitHub rate limited retrieval from the pinned public source. Retry after retryAfterSeconds when present.
    #[serde(rename = "source-rate-limited")]
    SourceRateLimited,
    /// No GitHub sign-in or credential is available, so the caller must sign in.
    #[serde(rename = "no-credential")]
    NoCredential,
    /// Agent Finder refused the GitHub credential (HTTP 401, or a 403 without rate-limit evidence); the caller must sign in again.
    #[serde(rename = "credential-rejected")]
    CredentialRejected,
    /// The Skill entrypoint is not a valid Skill.
    #[serde(rename = "invalid-skill")]
    InvalidSkill,
    /// The plugin manifest is not a valid plugin manifest, or the bundle has another manifest that would take precedence over the reviewed one.
    #[serde(rename = "invalid-plugin")]
    InvalidPlugin,
    /// The entrypoint exceeds the bounded complete review size.
    #[serde(rename = "review-too-large")]
    ReviewTooLarge,
    /// A resource with the same identity or install location already exists.
    #[serde(rename = "already-installed")]
    AlreadyInstalled,
    /// A storage operation failed; nothing was installed.
    #[serde(rename = "write-failed")]
    WriteFailed,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Reference planning outcome discriminator naming the planned kind.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogReferenceInstallationOutcomeSkillInstallPlannedKind {
    #[serde(rename = "skill-install-planned")]
    #[default]
    SkillInstallPlanned,
}

/// Reference planning outcome discriminator naming the planned kind.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogReferenceInstallationOutcomePluginInstallPlannedKind {
    #[serde(rename = "plugin-install-planned")]
    #[default]
    PluginInstallPlanned,
}

/// Reference planning outcome discriminator naming the planned kind.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogReferenceInstallationOutcomeRefusedKind {
    #[serde(rename = "refused")]
    #[default]
    Refused,
}

/// Planning outcome for a materialisation reference. A plan names its kind, so the caller applies it with that kind's apply method.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum CatalogReferenceInstallationOutcome {
    SkillInstallPlanned(CatalogReferenceInstallationOutcomeSkillInstallPlanned),
    PluginInstallPlanned(CatalogReferenceInstallationOutcomePluginInstallPlanned),
    Refused(CatalogReferenceInstallationOutcomeRefused),
}

/// Discriminator: handled planning outcome.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogReferenceInstallationResultOutcomeKind {
    #[serde(rename = "outcome")]
    #[default]
    Outcome,
}

/// Planning result for a materialisation reference with the honoured contract, or a typed refusal.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum CatalogReferenceInstallationResult {
    Outcome(CatalogReferenceInstallationResultOutcome),
    NegotiationRefused(CatalogNegotiationRefusedError),
    InvalidRequest(CatalogInvalidRequestError),
    NetworkFailure(CatalogNetworkFailureError),
}

/// Outcome of catalog.getGeneratedFeed: the feed with its entries, or one typed refusal. An entry that cannot be presented never fails the feed.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum CatalogResourceGeneratedFeedResult {
    Succeeded(CatalogGetGeneratedFeedSucceeded),
    NegotiationRefused(CatalogNegotiationRefusedError),
    InvalidRequest(CatalogInvalidRequestError),
    AuthenticationRequired(CatalogAuthenticationRequiredError),
    PolicyRejected(CatalogPolicyRejectedError),
    NetworkFailure(CatalogNetworkFailureError),
    UnsafeRetrieval(CatalogUnsafeRetrievalError),
    MalformedCard(CatalogMalformedCardError),
    Unavailable(CatalogUnavailableError),
}

/// Outcome of catalog.getSavedFeed: the feed with its entries, or one typed refusal. An entry that cannot be presented never fails the feed.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum CatalogResourceSavedFeedResult {
    Succeeded(CatalogGetSavedFeedSucceeded),
    NegotiationRefused(CatalogNegotiationRefusedError),
    InvalidRequest(CatalogInvalidRequestError),
    AuthenticationRequired(CatalogAuthenticationRequiredError),
    PolicyRejected(CatalogPolicyRejectedError),
    NetworkFailure(CatalogNetworkFailureError),
    UnsafeRetrieval(CatalogUnsafeRetrievalError),
    MalformedCard(CatalogMalformedCardError),
    Unavailable(CatalogUnavailableError),
}

/// Discriminator: the search completed
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogSearchSucceededKind {
    #[serde(rename = "succeeded")]
    #[default]
    Succeeded,
}

/// Discriminator: an unsupported candidate kind was requested
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogUnsupportedKindErrorKind {
    #[serde(rename = "unsupported-kind")]
    #[default]
    UnsupportedKind,
}

/// Outcome of a catalog.search call: either bounded inert candidates, or one typed refusal. Never a partial success.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum CatalogSearchResult {
    Succeeded(CatalogSearchSucceeded),
    NegotiationRefused(CatalogNegotiationRefusedError),
    UnsupportedKind(CatalogUnsupportedKindError),
    InvalidRequest(CatalogInvalidRequestError),
    AuthenticationRequired(CatalogAuthenticationRequiredError),
    PolicyRejected(CatalogPolicyRejectedError),
    NetworkFailure(CatalogNetworkFailureError),
    UnsafeRetrieval(CatalogUnsafeRetrievalError),
    MalformedCard(CatalogMalformedCardError),
    ContractViolation(CatalogContractViolationError),
    Unavailable(CatalogUnavailableError),
}

/// Discriminator: selection was cancelled
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogSelectionCancelledKind {
    #[serde(rename = "cancelled")]
    #[default]
    Cancelled,
}

/// Terminal outcome declared for a retained catalog selection group
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogSelectionDecision {
    /// Choose the candidate named by selectionRef.
    #[serde(rename = "selected")]
    Selected,
    /// Explicitly decline every candidate in the search.
    #[serde(rename = "declined")]
    Declined,
    /// Cancel the selection interaction without choosing a candidate.
    #[serde(rename = "cancelled")]
    Cancelled,
    /// Declare that the host's live interaction deadline elapsed while the reference remained valid.
    #[serde(rename = "timed-out")]
    TimedOut,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Discriminator: the candidates were declined
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogSelectionDeclinedKind {
    #[serde(rename = "declined")]
    #[default]
    Declined,
}

/// Discriminator for this typed selection rejection
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogSelectionForeignKind {
    #[serde(rename = "foreign")]
    #[default]
    Foreign,
}

/// Discriminator for this typed selection rejection
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogSelectionInvalidKind {
    #[serde(rename = "invalid")]
    #[default]
    Invalid,
}

/// Discriminator for this typed selection rejection
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogSelectionReplayedKind {
    #[serde(rename = "replayed")]
    #[default]
    Replayed,
}

/// Discriminator: one candidate was selected
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogSelectionSelectedKind {
    #[serde(rename = "selected")]
    #[default]
    Selected,
}

/// Discriminator: the host's live interaction timed out
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogSelectionTimedOutKind {
    #[serde(rename = "timed-out")]
    #[default]
    TimedOut,
}

/// Discriminator for this typed selection rejection
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogSelectionStaleKind {
    #[serde(rename = "stale")]
    #[default]
    Stale,
}

/// Discriminator for this typed selection rejection
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogSelectionWrongKindKind {
    #[serde(rename = "wrong-kind")]
    #[default]
    WrongKind,
}

/// Typed outcome of catalog.select. Only the selected host result carries a fresh candidate handle; the model-facing projection removes both that handle and searchId.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum CatalogSelectionResult {
    Selected(CatalogSelectionSelected),
    Declined(CatalogSelectionDeclined),
    Cancelled(CatalogSelectionCancelled),
    TimedOut(CatalogSelectionTimedOut),
    Invalid(CatalogSelectionInvalid),
    Stale(CatalogSelectionStale),
    Replayed(CatalogSelectionReplayed),
    Foreign(CatalogSelectionForeign),
    WrongKind(CatalogSelectionWrongKind),
    NegotiationRefused(CatalogNegotiationRefusedError),
    InvalidRequest(CatalogInvalidRequestError),
    Unavailable(CatalogUnavailableError),
}

/// Authority-computed exposure eligibility, kept separate from tier. The current tier-only Agent Finder response maps to `unknown`, never to a locally inferred eligibility.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogTrustEligibility {
    /// Eligible for default catalogue exposure.
    #[serde(rename = "default")]
    Default,
    /// Eligible only when expanded or community results are requested.
    #[serde(rename = "expanded")]
    Expanded,
    /// Not eligible for normal catalogue exposure.
    #[serde(rename = "hidden")]
    Hidden,
    /// The authority did not supply an eligibility decision.
    #[serde(rename = "unknown")]
    UnknownValue,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Bounded authority that supplied a catalogue trust observation
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogTrustSource {
    /// GitHub Agent Finder supplied the trust field on its search result.
    #[serde(rename = "agent-finder")]
    AgentFinder,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Schema version of the catalogue trust snapshot envelope
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogTrustSnapshotSchemaVersion {
    /// Initial envelope carrying one bounded service tier or one explicit unavailable state.
    #[serde(rename = "v1")]
    V1,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// The authority omitted trust metadata.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogTrustSnapshotAbsentStatus {
    /// The authority omitted trust metadata.
    #[serde(rename = "absent")]
    Absent,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// A recognised T1 or T2 service tier was observed.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogTrustSnapshotCurrentStatus {
    /// A recognised T1 or T2 service tier was observed.
    #[serde(rename = "current")]
    Current,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// The authority explicitly reported a downgraded assessment.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogTrustSnapshotDowngradedStatus {
    /// The authority explicitly reported a downgraded assessment.
    #[serde(rename = "downgraded")]
    Downgraded,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// The trust field was empty, unbounded, or had the wrong JSON type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogTrustSnapshotMalformedStatus {
    /// The trust field was empty, unbounded, or had the wrong JSON type.
    #[serde(rename = "malformed")]
    Malformed,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// The authority explicitly revoked its assessment.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogTrustSnapshotRevokedStatus {
    /// The authority explicitly revoked its assessment.
    #[serde(rename = "revoked")]
    Revoked,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// The authority explicitly marked its assessment stale.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogTrustSnapshotStaleStatus {
    /// The authority explicitly marked its assessment stale.
    #[serde(rename = "stale")]
    Stale,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// The authority supplied a bounded trust value this runtime does not understand.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogTrustSnapshotUnsupportedStatus {
    /// The authority supplied a bounded trust value this runtime does not understand.
    #[serde(rename = "unsupported")]
    Unsupported,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Whether a pending slash-command invocation effect was applied or cancelled by the host.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CommandsInvocationEffectOutcome {
    /// The host applied the pending invocation effect.
    #[serde(rename = "applied")]
    Applied,
    /// The host cancelled the pending invocation effect, so any provisional state must be reverted.
    #[serde(rename = "cancelled")]
    Cancelled,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CommandsInvocationOrigin {
    #[serde(rename = "settings")]
    Settings,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Runtime state a host waits for while it executes a queued command.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum QueuedCommandWaitRequirement {
    /// The host waits for managed policy to be published for the session's current account.
    #[serde(rename = "managed-policy-published")]
    ManagedPolicyPublished,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// The service is connected and the session MCP graph was reconciled.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ConnectorConnectResultConnectedKind {
    #[serde(rename = "connected")]
    #[default]
    Connected,
}

/// Host-owned consent is required before bounded continuation can complete.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ConnectorConnectResultConsentRequiredKind {
    #[serde(rename = "consent_required")]
    #[default]
    ConsentRequired,
}

/// The service is still completing the connection without a consent URL.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ConnectorConnectResultPendingKind {
    #[serde(rename = "pending")]
    #[default]
    Pending,
}

/// Typed result of initiating or continuing a Connector connection.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum ConnectorConnectResult {
    Connected(ConnectorConnectResultConnected),
    ConsentRequired(ConnectorConnectResultConsentRequired),
    Pending(ConnectorConnectResultPending),
}

/// Availability.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ConnectorDiscoveryAvailability {
    /// Enabled.
    #[serde(rename = "enabled")]
    Enabled,
    /// Disabled.
    #[serde(rename = "disabled")]
    Disabled,
    /// Unavailable.
    #[serde(rename = "unavailable")]
    Unavailable,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Closed set of public task kinds a connection can negotiate.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum TaskKind {
    /// Runtime-owned background agent task.
    #[serde(rename = "agent")]
    Agent,
    /// Runtime-owned shell task.
    #[serde(rename = "shell")]
    Shell,
    /// Client-owned externally executed task.
    #[serde(rename = "client")]
    Client,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Controls how MCP tool result content is filtered: none leaves content unchanged, markdown sanitizes HTML while preserving Markdown-friendly output, and hidden_characters removes characters that can hide directives.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ContentFilterMode {
    /// Leave MCP tool result content unchanged.
    #[serde(rename = "none")]
    None,
    /// Sanitize HTML while preserving Markdown-friendly output.
    #[serde(rename = "markdown")]
    Markdown,
    /// Remove characters that can hide directives.
    #[serde(rename = "hidden_characters")]
    HiddenCharacters,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Result of reloading a customization component.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(from = "String", into = "String")]
pub enum CustomizationReloadStatus {
    /// The component was refreshed successfully.
    Reloaded,
    /// The component was not configured, loaded, or eligible for refresh.
    Skipped,
    /// The component could not be refreshed; other components may still reload.
    Failed,
    /// Unknown variant for forward compatibility.
    Unknown(String),
}

impl Default for CustomizationReloadStatus {
    fn default() -> Self {
        Self::Unknown("unknown".to_owned())
    }
}

impl From<String> for CustomizationReloadStatus {
    fn from(value: String) -> Self {
        match value.as_str() {
            "reloaded" => Self::Reloaded,
            "skipped" => Self::Skipped,
            "failed" => Self::Failed,
            _ => Self::Unknown(value),
        }
    }
}

impl From<CustomizationReloadStatus> for String {
    fn from(value: CustomizationReloadStatus) -> Self {
        match value {
            CustomizationReloadStatus::Reloaded => "reloaded".to_owned(),
            CustomizationReloadStatus::Skipped => "skipped".to_owned(),
            CustomizationReloadStatus::Failed => "failed".to_owned(),
            CustomizationReloadStatus::Unknown(value) => value,
        }
    }
}

/// Component of session customization discovery.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(from = "String", into = "String")]
pub enum CustomizationReloadSubsystem {
    /// Repository metadata and working-directory context.
    RepositoryContext,
    /// Session instructions and their cached dynamic context.
    Instructions,
    /// Discovered plugin configuration.
    Plugins,
    /// Configured session and plugin hooks.
    Hooks,
    /// Discovered skills.
    Skills,
    /// Discovered custom agents.
    Agents,
    /// Loaded MCP server configuration.
    Mcp,
    /// Configured session extensions.
    Extensions,
    /// Unknown variant for forward compatibility.
    Unknown(String),
}

impl Default for CustomizationReloadSubsystem {
    fn default() -> Self {
        Self::Unknown("unknown".to_owned())
    }
}

impl From<String> for CustomizationReloadSubsystem {
    fn from(value: String) -> Self {
        match value.as_str() {
            "repositoryContext" => Self::RepositoryContext,
            "instructions" => Self::Instructions,
            "plugins" => Self::Plugins,
            "hooks" => Self::Hooks,
            "skills" => Self::Skills,
            "agents" => Self::Agents,
            "mcp" => Self::Mcp,
            "extensions" => Self::Extensions,
            _ => Self::Unknown(value),
        }
    }
}

impl From<CustomizationReloadSubsystem> for String {
    fn from(value: CustomizationReloadSubsystem) -> Self {
        match value {
            CustomizationReloadSubsystem::RepositoryContext => "repositoryContext".to_owned(),
            CustomizationReloadSubsystem::Instructions => "instructions".to_owned(),
            CustomizationReloadSubsystem::Plugins => "plugins".to_owned(),
            CustomizationReloadSubsystem::Hooks => "hooks".to_owned(),
            CustomizationReloadSubsystem::Skills => "skills".to_owned(),
            CustomizationReloadSubsystem::Agents => "agents".to_owned(),
            CustomizationReloadSubsystem::Mcp => "mcp".to_owned(),
            CustomizationReloadSubsystem::Extensions => "extensions".to_owned(),
            CustomizationReloadSubsystem::Unknown(value) => value,
        }
    }
}

/// Destination variant discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum DebugCollectLogsDestinationArchiveKind {
    #[serde(rename = "archive")]
    #[default]
    Archive,
}

/// Destination variant discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum DebugCollectLogsDestinationDirectoryKind {
    #[serde(rename = "directory")]
    #[default]
    Directory,
}

/// Destination for the session debug bundle.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum DebugCollectLogsDestination {
    Archive(DebugCollectLogsDestinationArchive),
    Directory(DebugCollectLogsDestinationDirectory),
}

/// Kind of caller-provided debug log entry.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum DebugCollectLogsEntryKind {
    /// Include a single server-local file.
    #[serde(rename = "file")]
    File,
    /// Include files from a server-local directory recursively.
    #[serde(rename = "directory")]
    Directory,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// How a collected debug entry should be redacted before being staged.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum DebugCollectLogsRedaction {
    /// Redact the file as plain UTF-8 log text.
    #[serde(rename = "plain-text")]
    PlainText,
    /// Redact each non-empty line as a session event JSON object, falling back to plain-text redaction for malformed lines.
    #[serde(rename = "events-jsonl")]
    EventsJsonl,
    /// No redaction is applied. The caller must ensure any necessary redaction is performed before this call.
    #[serde(rename = "none")]
    None,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Hook event name. Discovery emits the file-configurable subset; SDK callbacks additionally support callback-only events.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum HookType {
    /// Runs before a tool is invoked.
    #[serde(rename = "preToolUse")]
    PreToolUse,
    /// Runs before an MCP tool is invoked.
    #[serde(rename = "preMcpToolCall")]
    PreMcpToolCall,
    /// Runs after a tool completes successfully.
    #[serde(rename = "postToolUse")]
    PostToolUse,
    /// Runs after a tool fails.
    #[serde(rename = "postToolUseFailure")]
    PostToolUseFailure,
    /// Runs after the user submits a prompt.
    #[serde(rename = "userPromptSubmitted")]
    UserPromptSubmitted,
    /// Runs after the runtime transforms the submitted prompt for the model, before it is added to session history.
    #[serde(rename = "userPromptTransformed")]
    UserPromptTransformed,
    /// Runs when a session starts.
    #[serde(rename = "sessionStart")]
    SessionStart,
    /// Runs when a session ends.
    #[serde(rename = "sessionEnd")]
    SessionEnd,
    /// Runs when the agent encounters an error.
    #[serde(rename = "errorOccurred")]
    ErrorOccurred,
    /// Runs when the agent stops.
    #[serde(rename = "agentStop")]
    AgentStop,
    /// Runs when a subagent starts.
    #[serde(rename = "subagentStart")]
    SubagentStart,
    /// Runs when a subagent stops.
    #[serde(rename = "subagentStop")]
    SubagentStop,
    /// Runs before conversation context is compacted.
    #[serde(rename = "preCompact")]
    PreCompact,
    /// Runs when the agent requests permission.
    #[serde(rename = "permissionRequest")]
    PermissionRequest,
    /// Runs when the agent emits a notification.
    #[serde(rename = "notification")]
    Notification,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Configuration tier that contributed a discovered hook action.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum HookOrigin {
    /// Hook loaded from user settings or the user's hook directory.
    #[serde(rename = "user")]
    User,
    /// Hook loaded from repository settings or the repository hook directory.
    #[serde(rename = "repository")]
    Repository,
    /// Hook provided by an enabled installed or explicit plugin. Projectless rows omit projectPath and do not expand a project directory.
    #[serde(rename = "plugin")]
    Plugin,
    /// Hook enforced by centrally managed policy.
    #[serde(rename = "policy")]
    Policy,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Interaction policy for host-owned Microsoft Entra acquisition.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum EntraTokenAcquireReason {
    /// Establish an identity; interaction is permitted if necessary.
    #[serde(rename = "signIn")]
    SignIn,
    /// Renew the pinned identity silently; never select another account or prompt.
    #[serde(rename = "renewal")]
    Renewal,
    /// Explicitly show account selection rather than satisfying identity selection from cache.
    #[serde(rename = "forceInteractive")]
    ForceInteractive,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Entra identity or resource-token acquisition discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum EntraTokenAcquireRequestIdentityKind {
    #[serde(rename = "identity")]
    #[default]
    Identity,
}

/// Entra identity or resource-token acquisition discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum EntraTokenAcquireRequestTokenKind {
    #[serde(rename = "token")]
    #[default]
    Token,
}

/// Requests a host-selected Entra identity or a resource token for that exact identity. Host account ids are opaque and distinct from runtime account-selection ids.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum EntraTokenAcquireRequest {
    Identity(EntraTokenAcquireRequestIdentity),
    Token(EntraTokenAcquireRequestToken),
}

/// Entra acquisition response variant discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum EntraTokenAcquireResultIdentityKind {
    #[serde(rename = "identity")]
    #[default]
    Identity,
}

/// Entra acquisition response variant discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum EntraTokenAcquireResultTokenKind {
    #[serde(rename = "token")]
    #[default]
    Token,
}

/// Entra acquisition response variant discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum EntraTokenAcquireResultUnavailableKind {
    #[serde(rename = "unavailable")]
    #[default]
    Unavailable,
}

/// Entra acquisition response variant discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum EntraTokenAcquireResultCancelledKind {
    #[serde(rename = "cancelled")]
    #[default]
    Cancelled,
}

/// Host response to Entra acquisition. Unavailable, cancellation and errors are terminal: the runtime must not substitute another account or sign-in transport.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum EntraTokenAcquireResult {
    Identity(EntraTokenAcquireResultIdentity),
    Token(EntraTokenAcquireResultToken),
    Unavailable(EntraTokenAcquireResultUnavailable),
    Cancelled(EntraTokenAcquireResultCancelled),
}

/// GitHub Mission Control compute kind.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum EnvironmentKind {
    /// A user-managed environment on a local machine.
    #[serde(rename = "user-local")]
    UserLocal,
    /// A user-managed environment in a GitHub Codespace.
    #[serde(rename = "user-codespace")]
    UserCodespace,
    /// A GitHub-managed environment backed by GitHub Actions.
    #[serde(rename = "managed-actions")]
    ManagedActions,
    /// A GitHub-managed sandbox environment.
    #[serde(rename = "managed-sandbox")]
    ManagedSandbox,
    /// A GitHub-managed cloud coding agent environment.
    #[serde(rename = "managed-cca")]
    ManagedCca,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Agent-scope filter: 'primary' returns only main-agent events plus events whose type starts with 'subagent.' (matching the typed-subscription default behavior); 'all' returns events from all agents (matching wildcard-subscription behavior). Default is 'all' to preserve wildcard semantics for catch-up callers.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum EventsAgentScope {
    /// Return main-agent events and typed subagent lifecycle events.
    #[serde(rename = "primary")]
    Primary,
    /// Return events from all agents.
    #[serde(rename = "all")]
    All,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Original linked history or the self-contained active replay head.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum EventsReadView {
    /// Original events across linked journals, excluding checkpoint copies.
    #[serde(rename = "history")]
    History,
    /// The active replay head, including checkpoint state, without predecessor journals.
    #[serde(rename = "replay")]
    Replay,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Content block type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ExternalToolTextResultForLlmContentAudioType {
    #[serde(rename = "audio")]
    #[default]
    Audio,
}

/// Content block type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ExternalToolTextResultForLlmContentImageType {
    #[serde(rename = "image")]
    #[default]
    Image,
}

/// Content block type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ExternalToolTextResultForLlmContentResourceType {
    #[serde(rename = "resource")]
    #[default]
    Resource,
}

/// Theme variant this icon is intended for
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ExternalToolTextResultForLlmContentResourceLinkIconTheme {
    /// Icon intended for light themes.
    #[serde(rename = "light")]
    Light,
    /// Icon intended for dark themes.
    #[serde(rename = "dark")]
    Dark,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Content block type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ExternalToolTextResultForLlmContentResourceLinkType {
    #[serde(rename = "resource_link")]
    #[default]
    ResourceLink,
}

/// Content block type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ExternalToolTextResultForLlmContentShellExitType {
    #[serde(rename = "shell_exit")]
    #[default]
    ShellExit,
}

/// Content block type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ExternalToolTextResultForLlmContentTerminalType {
    #[serde(rename = "terminal")]
    #[default]
    Terminal,
}

/// Content block type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ExternalToolTextResultForLlmContentTextType {
    #[serde(rename = "text")]
    #[default]
    Text,
}

/// What initiated this compaction request, recorded as the `trigger` on the persisted `session.compaction_start` / `session.compaction_complete` events. When absent, the compaction is persisted without trigger attribution (initiator unknown).
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum HistoryCompactRequestTrigger {
    /// User-requested compaction, e.g. the /compact command or a direct history.compact call.
    #[serde(rename = "manual")]
    Manual,
    /// Compaction requested while switching to a model with a smaller context window.
    #[serde(rename = "model_switch")]
    ModelSwitch,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Scope of a rewind operation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum HistoryRewindMode {
    /// Discard conversation events while leaving files unchanged.
    #[serde(rename = "conversation")]
    Conversation,
    /// Discard conversation events and restore captured files changed by those turns.
    #[serde(rename = "conversation-and-files")]
    ConversationAndFiles,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// The cached host error-reporting policy. Pending means startup policy has not resolved. This does not describe network delivery.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum HostErrorReportingStatus {
    /// Startup policy resolution is incomplete.
    #[serde(rename = "pending")]
    Pending,
    /// The host policy permits error reports.
    #[serde(rename = "enabled")]
    Enabled,
    /// The host policy blocks error reports.
    #[serde(rename = "disabled")]
    Disabled,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum HostExitReason {
    /// The owner requested disposal.
    #[serde(rename = "disposed")]
    Disposed,
    /// The hosting task or its SDK transport exited; this does not mean the runtime process exited.
    #[serde(rename = "exited")]
    Exited,
    /// The owning SDK connection disconnected.
    #[serde(rename = "ownerDisconnected")]
    OwnerDisconnected,
    /// The runtime is shutting down.
    #[serde(rename = "runtimeShutdown")]
    RuntimeShutdown,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Constant value. Always "github".
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum InstalledPluginSourceGitHubSource {
    #[serde(rename = "github")]
    #[default]
    GitHub,
}

/// Constant value. Always "local".
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum InstalledPluginSourceLocalSource {
    #[serde(rename = "local")]
    #[default]
    Local,
}

/// Constant value. Always "url".
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum InstalledPluginSourceUrlSource {
    #[serde(rename = "url")]
    #[default]
    Url,
}

/// Whether the target is a single file or a directory of instruction files
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum InstructionDiscoveryPathKind {
    /// The target is a single instruction file.
    #[serde(rename = "file")]
    File,
    /// The target is a directory that holds instruction files.
    #[serde(rename = "directory")]
    Directory,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Which tier this target belongs to
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum InstructionDiscoveryPathLocation {
    /// Instructions live in user-level configuration.
    #[serde(rename = "user")]
    User,
    /// Instructions live in repository-level configuration.
    #[serde(rename = "repository")]
    Repository,
    /// Instructions live under the current working directory.
    #[serde(rename = "working-directory")]
    WorkingDirectory,
    /// Instructions live in plugin-provided configuration.
    #[serde(rename = "plugin")]
    Plugin,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Selects whether Kerberos credentials come from native lookup with a client fallback, or only from the client.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum KerberosAuthMode {
    /// Use the callback only when native credential lookup fails.
    #[serde(rename = "fallback")]
    Fallback,
    /// Always use the callback without native credential lookup.
    #[serde(rename = "always")]
    Always,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Selects which traffic a client's Kerberos callback authenticates.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum KerberosAuthScope {
    /// Authenticate only requests made for this connection's sessions.
    #[serde(rename = "connection")]
    Connection,
    /// Also authenticate the runtime's shared background traffic. Only one connection at a time may claim this scope.
    #[serde(rename = "runtime")]
    Runtime,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Transport the runtime would otherwise use for this request. `http` (the default when absent) covers plain HTTP and SSE responses; `websocket` indicates a full-duplex message channel where each body chunk maps to one WebSocket message and the `binary` flag distinguishes text from binary frames. The SDK consumer uses this to decide whether to service the request with an HTTP client or a WebSocket client. It is the one piece of request metadata the consumer cannot reliably infer from the URL or headers alone.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum LlmInferenceHttpRequestStartTransport {
    /// Plain HTTP or SSE response. Each body chunk is an opaque byte range; the response is a status line, headers, and a (possibly streamed) body.
    #[serde(rename = "http")]
    Http,
    /// Full-duplex WebSocket channel. Each body chunk maps to exactly one WebSocket message and the `binary` flag distinguishes text from binary frames; request and response chunks flow concurrently.
    #[serde(rename = "websocket")]
    Websocket,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Log severity level. Determines how the message is displayed in the timeline. Defaults to "info".
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionLogLevel {
    /// Informational message.
    #[serde(rename = "info")]
    Info,
    /// Warning message that may require attention.
    #[serde(rename = "warning")]
    Warning,
    /// Error message describing a failure.
    #[serde(rename = "error")]
    Error,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Structured MCP elicitation mode.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpElicitationFormMode {
    #[serde(rename = "form")]
    Form,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ProtocolAppendMode {
    #[serde(rename = "append")]
    Append,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ProtocolCustomizeMode {
    #[serde(rename = "customize")]
    Customize,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ProtocolReplaceMode {
    #[serde(rename = "replace")]
    Replace,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ProtocolStaticSectionAction {
    /// Replace the section content.
    #[serde(rename = "replace")]
    Replace,
    /// Remove the section content.
    #[serde(rename = "remove")]
    Remove,
    /// Append content to the section.
    #[serde(rename = "append")]
    Append,
    /// Prepend content to the section.
    #[serde(rename = "prepend")]
    Prepend,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Attachment type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PushAttachmentBlobType {
    #[serde(rename = "blob")]
    #[default]
    Blob,
}

/// Attachment type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PushAttachmentDirectoryType {
    #[serde(rename = "directory")]
    #[default]
    Directory,
}

/// Attachment type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PushAttachmentFileType {
    #[serde(rename = "file")]
    #[default]
    File,
}

/// Attachment type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PushAttachmentGitHubActionsJobType {
    #[serde(rename = "github_actions_job")]
    #[default]
    GitHubActionsJob,
}

/// Attachment type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PushAttachmentGitHubCommitType {
    #[serde(rename = "github_commit")]
    #[default]
    GitHubCommit,
}

/// Attachment type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PushAttachmentGitHubFileType {
    #[serde(rename = "github_file")]
    #[default]
    GitHubFile,
}

/// Attachment type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PushAttachmentGitHubFileDiffType {
    #[serde(rename = "github_file_diff")]
    #[default]
    GitHubFileDiff,
}

/// Type of GitHub reference
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PushAttachmentGitHubReferenceType {
    /// GitHub issue reference.
    #[serde(rename = "issue")]
    Issue,
    /// GitHub pull request reference.
    #[serde(rename = "pr")]
    Pr,
    /// GitHub discussion reference.
    #[serde(rename = "discussion")]
    Discussion,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Attachment type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PushAttachmentGitHubReleaseType {
    #[serde(rename = "github_release")]
    #[default]
    GitHubRelease,
}

/// Attachment type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PushAttachmentGitHubRepositoryType {
    #[serde(rename = "github_repository")]
    #[default]
    GitHubRepository,
}

/// Attachment type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PushAttachmentGitHubSnippetType {
    #[serde(rename = "github_snippet")]
    #[default]
    GitHubSnippet,
}

/// Attachment type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PushAttachmentGitHubTreeComparisonType {
    #[serde(rename = "github_tree_comparison")]
    #[default]
    GitHubTreeComparison,
}

/// Attachment type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PushAttachmentGitHubUrlType {
    #[serde(rename = "github_url")]
    #[default]
    GitHubUrl,
}

/// Attachment type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PushAttachmentSelectionType {
    #[serde(rename = "selection")]
    #[default]
    Selection,
}

/// How to deliver the message. `enqueue` (default) appends to the message queue. `immediate` interjects during an in-progress turn.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SendMode {
    /// Append the message to the normal session queue.
    #[serde(rename = "enqueue")]
    Enqueue,
    /// Interject the message during the in-progress turn.
    #[serde(rename = "immediate")]
    Immediate,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Which repository settings file to edit.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub(crate) enum RepoSettingsEditScope {
    /// The shared repository settings file.
    #[serde(rename = "repo")]
    Repo,
    /// The local repository settings file.
    #[serde(rename = "local")]
    Local,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Output format discriminator. Currently only json_schema is supported.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ResponseFormatType {
    #[serde(rename = "json_schema")]
    #[default]
    JsonSchema,
}

/// A session-scoped sandbox transition applied while handling a slash command
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SandboxSessionChange {
    /// The sandbox is off for the rest of this session; nothing was persisted and a new session starts from saved and managed policy.
    #[serde(rename = "disabled")]
    Disabled,
    /// The sandbox is enabled for this session; nothing was persisted.
    #[serde(rename = "restored")]
    Restored,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Output format discriminator. Currently only json_schema is supported.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SendMessagesRequestResponseFormatType {
    #[serde(rename = "json_schema")]
    #[default]
    JsonSchema,
}

/// Output format discriminator. Currently only json_schema is supported.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SendRequestResponseFormatType {
    #[serde(rename = "json_schema")]
    #[default]
    JsonSchema,
}

/// Signal to send (default: SIGTERM)
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ShellKillSignal {
    /// Request graceful process termination.
    SIGTERM,
    /// Forcefully terminate the process.
    SIGKILL,
    /// Send an interrupt signal to the process.
    SIGINT,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Discriminator for an add-timeline-entry result.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SlashCommandAddTimelineEntryResultKind {
    #[serde(rename = "add-timeline-entry")]
    #[default]
    AddTimelineEntry,
}

/// Agent prompt result discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SlashCommandAgentPromptResultKind {
    #[serde(rename = "agent-prompt")]
    #[default]
    AgentPrompt,
}

/// Completed result discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SlashCommandCompletedResultKind {
    #[serde(rename = "completed")]
    #[default]
    Completed,
}

/// Text result discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SlashCommandTextResultKind {
    #[serde(rename = "text")]
    #[default]
    Text,
}

/// Select subcommand result discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SlashCommandSelectSubcommandResultKind {
    #[serde(rename = "select-subcommand")]
    #[default]
    SelectSubcommand,
}

/// Discriminator for a model-picker dialog.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SlashCommandModelPickerDialogKind {
    #[serde(rename = "model-picker")]
    #[default]
    ModelPicker,
}

/// Discriminator for a show-dialog result.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SlashCommandShowDialogResultKind {
    #[serde(rename = "show-dialog")]
    #[default]
    ShowDialog,
}

/// Discriminator for a set-model result.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SlashCommandSetModelResultKind {
    #[serde(rename = "set-model")]
    #[default]
    SetModel,
}

/// Discriminator for a set-plan-model result.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SlashCommandSetPlanModelResultKind {
    #[serde(rename = "set-plan-model")]
    #[default]
    SetPlanModel,
}

/// Result of invoking the slash command (text output, prompt to send to the agent, completion, or subcommand selection).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum SlashCommandInvocationResult {
    Text(SlashCommandTextResult),
    AgentPrompt(SlashCommandAgentPromptResult),
    Completed(SlashCommandCompletedResult),
    SelectSubcommand(SlashCommandSelectSubcommandResult),
    AddTimelineEntry(SlashCommandAddTimelineEntryResult),
    ShowDialog(SlashCommandShowDialogResult),
    SetModel(SlashCommandSetModelResult),
    SetPlanModel(SlashCommandSetPlanModelResult),
}

/// User's choice for auto-mode switching: yes (allow this turn), yes_always (allow + persist as setting), or no (decline).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum UIAutoModeSwitchResponse {
    /// Allow the automatic mode switch for this turn.
    #[serde(rename = "yes")]
    Yes,
    /// Allow this mode switch and persist the preference.
    #[serde(rename = "yes_always")]
    YesAlways,
    /// Decline the automatic mode switch.
    #[serde(rename = "no")]
    No,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Type discriminator. Always "array".
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum UIElicitationArrayAnyOfFieldType {
    #[serde(rename = "array")]
    #[default]
    Array,
}

/// Type discriminator. Always "string".
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum UIElicitationArrayEnumFieldItemsType {
    #[serde(rename = "string")]
    #[default]
    String,
}

/// Type discriminator. Always "array".
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum UIElicitationArrayEnumFieldType {
    #[serde(rename = "array")]
    #[default]
    Array,
}

/// Schema type indicator (always 'object')
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum UIElicitationSchemaType {
    #[serde(rename = "object")]
    #[default]
    Object,
}

/// Type discriminator. Always "boolean".
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum UIElicitationSchemaPropertyBooleanType {
    #[serde(rename = "boolean")]
    #[default]
    Boolean,
}

/// Numeric type accepted by the field.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum UIElicitationSchemaPropertyNumberType {
    /// Any JSON number.
    #[serde(rename = "number")]
    Number,
    /// Integer JSON number.
    #[serde(rename = "integer")]
    Integer,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Optional format hint that constrains the accepted input.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum UIElicitationSchemaPropertyStringFormat {
    /// Email address string format.
    #[serde(rename = "email")]
    Email,
    /// URI string format.
    #[serde(rename = "uri")]
    Uri,
    /// Calendar date string format.
    #[serde(rename = "date")]
    Date,
    /// Date-time string format.
    #[serde(rename = "date-time")]
    DateTime,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Type discriminator. Always "string".
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum UIElicitationSchemaPropertyStringType {
    #[serde(rename = "string")]
    #[default]
    String,
}

/// Type discriminator. Always "string".
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum UIElicitationStringEnumFieldType {
    #[serde(rename = "string")]
    #[default]
    String,
}

/// Type discriminator. Always "string".
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum UIElicitationStringOneOfFieldType {
    #[serde(rename = "string")]
    #[default]
    String,
}

/// The action the user selected. Defaults to 'autopilot' when autoApproveEdits is true, otherwise 'interactive'.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum UIExitPlanModeAction {
    /// Exit plan mode without starting implementation.
    #[serde(rename = "exit_only")]
    ExitOnly,
    /// Exit plan mode and continue interactively.
    #[serde(rename = "interactive")]
    Interactive,
    /// Exit plan mode and continue in autopilot mode.
    #[serde(rename = "autopilot")]
    Autopilot,
    /// Exit plan mode and continue in autopilot mode with parallel subagent execution.
    #[serde(rename = "autopilot_fleet")]
    AutopilotFleet,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// User action selected for an exhausted session limit.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum UISessionLimitsExhaustedResponseAction {
    /// Increase the current max by an exact AI Credits amount.
    #[serde(rename = "add")]
    Add,
    /// Set a new absolute max AI Credits value.
    #[serde(rename = "set")]
    Set,
    /// Remove the current session limit.
    #[serde(rename = "unset")]
    Unset,
    /// Leave the limit unchanged and cancel the blocked model request.
    #[serde(rename = "cancel")]
    Cancel,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

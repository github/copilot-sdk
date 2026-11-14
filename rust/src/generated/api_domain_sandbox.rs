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

/// The stored sandbox settings to resolve against the defaults.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SandboxApplyDefaultsRequest {
    /// The sandbox settings as stored in the settings file. An empty object resolves to the defaults alone.
    pub stored: serde_json::Value,
}

/// Possible unconfigured secrets in the sandbox shell environment, sorted by variable name, with no secret values.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SandboxCredentialSuggestionsResult {
    /// Candidates for user review. Empty when sandboxing is disabled or no unconfigured candidates are present.
    pub suggestions: Vec<SandboxCredentialSuggestion>,
}

/// Request to disable sandboxing for the current session while resolving an active sandbox-bypass permission prompt.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SandboxDisableForSessionRequest {
    /// Optional attribution for the permission decision.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub decision_context: Option<PermissionDecisionContext>,
    /// Identifier of the exact pending sandbox-bypass permission request that authorized the session opt-out.
    pub request_id: RequestId,
}

/// Result of attempting to disable sandboxing for the current session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SandboxDisableForSessionResult {
    /// The authoritative sandbox enabled state after the operation.
    pub enabled: bool,
    /// Whether this call resolved the pending request and applied the session opt-out.
    pub success: bool,
}

/// The documentation URL a host cites when it declines or downgrades a sandbox setting.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SandboxDocsUrlResult {
    /// Documentation URL describing the sandbox and the settings that control it.
    pub url: String,
}

/// A sandbox config and the per-feature routing flag to resolve it for.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct SandboxEffectiveForOptionalParams {
    /// The routing flag naming the feature being launched, such as `sandboxMcpServers` or `sandboxLspServers`.
    pub routing_flag: String,
    /// The session's resolved `sandbox` slice. Omitted when the host has none, which yields a disabled sandbox rather than an error.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sandbox: Option<serde_json::Value>,
}

/// The sandbox a named feature actually launches under.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct SandboxEffectiveForOptionalResult {
    /// The effective sandbox config for the requested feature, or a disabled sandbox when the feature is routed out of it.
    pub sandbox_json: serde_json::Value,
}

/// One slash command, as typed, to test against the enforcement-block allowlist.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct SandboxEnforcementCommandAllowedParams {
    /// The command's arguments, in order. Pass an empty array for a bare command: several entries in the allowlist admit the bare form and refuse every argument.
    pub args: Vec<String>,
    /// The command name as the user typed it, with or without its leading slash.
    pub command_name: String,
}

/// Managed sandbox enforcement state for a session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SandboxEnforcementStatus {
    /// Whether an enforcement failure has permanently blocked the session.
    pub blocked: bool,
    /// The first sandbox enforcement failure that blocked the session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reason: Option<String>,
    /// Whether the effective managed policy requires an available sandbox backend.
    pub required: bool,
}

/// One file-picker suggestion offered up for the sandbox filesystem check.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct SandboxPickerCandidate {
    /// Whether the suggestion is a directory. A directory the policy does not itself allow may still be needed as a waypoint to an allowed subtree.
    pub is_directory: bool,
    /// Absolute path the suggestion would attach.
    pub path: String,
}

/// A batch of file-picker candidates to check against a prepared policy.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct SandboxFilterPickerCandidatesParams {
    /// The candidates to check, in the order the answers are returned.
    #[doc(hidden)]
    pub(crate) candidates: Vec<SandboxPickerCandidate>,
    /// The session's working directory, matching the one the policy was prepared from.
    pub cwd: String,
    /// The policy returned by `sandbox.preparePickerPolicy`. Omitted keeps every candidate.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sandbox: Option<serde_json::Value>,
}

/// Per-candidate allow decisions, positionally aligned with the request.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct SandboxFilterPickerCandidatesResult {
    /// One decision per candidate, in the same order. Always the same length as the request's `candidates`.
    pub allowed: Vec<bool>,
}

/// Request to accept the sandbox path grant offered on an active sandbox escalation permission prompt.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SandboxGrantPathForRequestRequest {
    /// Optional attribution for the permission decision.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub decision_context: Option<PermissionDecisionContext>,
    /// Identifier of the exact pending sandbox escalation permission request whose sandboxPathGrant to accept.
    pub request_id: RequestId,
}

/// Result of accepting a sandbox path grant.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SandboxGrantPathForRequestResult {
    /// Whether this call resolved the pending request and added the path to the session's sandbox policy.
    pub success: bool,
}

/// Whether this host can run one sandbox policy feature. A session whose effective policy uses an unsupported feature fails each sandboxed command with `reason`, except `filesystem_enumeration`, whose absence degrades sandboxed PowerShell instead of failing it.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SandboxHostCapability {
    /// The policy feature, as an extensible string: ignore names you do not recognize. Known values: `network` (sandboxed commands can reach the network; on Linux this needs the tooling for Bubblewrap's private network namespace, such as slirp4netns), `network_filtering` (host rules and the sandbox proxy; on Linux this needs the same tooling as `network`; on Windows it needs Process Security Environment 1.1 host-loopback support or MXC's PSEC 1.0-only proxy-loopback compatibility capability, and a policy that uses it must also set `network.allowLocalNetwork`; compatibility applies only to an explicit identity-less runtime proxy, not general host-loopback access, and other policy restrictions still apply), `denied_paths` (native enforcement of `filesystem.deniedPaths`), `shell` (shell commands inside the sandbox), and `filesystem_enumeration` (enumerate-only filesystem grants; on Windows this needs Process Security Environment 1.1 filesystem enumeration support, and without it sandboxed PowerShell still runs but cannot resolve its current location; other platforms always report it).
    pub name: String,
    /// Human-readable reason and remedy when the feature is unsupported, such as a package to install or an OS update. Present only when `supported` is false.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reason: Option<String>,
    /// Whether this host can run the feature.
    pub supported: bool,
}

/// Whether the host running this runtime can run the command sandbox. The runtime checks `supported` once per process. A capability answer can change while the process runs, for example after the user installs a missing package.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SandboxHostSupport {
    /// Sandbox policy features whose availability varies between hosts that can run the backend. Empty when `supported` is false, because no feature can run without a backend. Later runtimes can add entries; ignore an entry whose `name` you do not recognize.
    pub capabilities: Vec<SandboxHostCapability>,
    /// Human-readable reason the sandbox cannot run on this host. Present only when `supported` is false.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reason: Option<String>,
    /// Whether a process-containment backend is usable on this host: Seatbelt on macOS, Bubblewrap on Linux, or ProcessContainer on Windows.
    pub supported: bool,
}

/// The path a user is about to grant.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct SandboxPathGrantWarningParams {
    /// Whether the grant is read-only. Omitted means read/write, which gets the stricter advice.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub read_only: Option<bool>,
    /// The grant as it will reach the sandbox: unquoted, anchored to the session's working directory and normalized. A relative path can give a false negative.
    pub resolved_path: String,
}

/// Advice for a path grant, when it needs any.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct SandboxPathGrantWarningResult {
    /// The user-facing warning. Absent when the grant needs none, so a host can call unconditionally and show only what it gets.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub warning: Option<String>,
}

/// The working directory and sandbox settings to build a file-picker policy from.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct SandboxPreparePickerPolicyParams {
    /// The session's working directory. Git discovery and grant canonicalization are resolved against it.
    pub cwd: String,
    /// The session's resolved `sandbox` slice. Omitted, or disabled, yields no policy and every candidate is kept.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sandbox: Option<serde_json::Value>,
}

/// The prepared file-picker policy, or none when the sandbox is disabled.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct SandboxPreparePickerPolicyResult {
    /// The prepared policy to pass back to `sandbox.filterPickerCandidates`. Absent when the sandbox is disabled or absent, in which case filtering keeps every candidate.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sandbox_json: Option<serde_json::Value>,
}

/// Result of creating the persistent certificate authority of the sandbox credential proxy.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SandboxProxyCaCreateResult {
    /// Absolute path of the public certificate of the certificate authority, in PEM format.
    pub certificate_path: String,
}

/// Identifies the credential hosts that the persistent certificate authority of the sandbox credential proxy must cover. The runtime always adds the hosts from the saved user settings.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SandboxProxyCaRequest {
    /// The sandbox configuration that the host gives its sessions. The runtime reads the credential hosts from `auth` and `credentials`; it ignores `enabled` and the other fields.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sandbox_config: Option<SandboxConfig>,
}

/// Status of the persistent certificate authority of the sandbox credential proxy.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SandboxProxyCaStatus {
    /// Whether this process can add the certificate authority to OS trust without credentials from a different user. False where OS trust is unsupported, and on Windows when the process cannot elevate itself to write the machine trust store. When false, do not offer to set up the certificate authority.
    pub can_install: bool,
    /// Human-readable reason for the state. On `installed` or `notInstalled`, present only when the certificate authority must be rotated, and then says why.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub detail: Option<String>,
    /// The state of the certificate authority.
    pub state: SandboxProxyCaState,
}

/// The stored sandbox proxy password, if one is present.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SandboxProxySecretGetResult {
    /// The stored password. Absent when no entry exists in either the keychain or the on-disk fallback store.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub value: Option<String>,
}

/// Where this runtime keeps the sandbox proxy password.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SandboxProxySecretLocation {
    /// Absolute path of the credential store holding the sandbox proxy password. The proxy password belongs to no MCP server, so it is kept in its own directory rather than in the MCP secret store.
    pub config_dir: String,
    /// Identifier of the proxy password entry within that store. Derived from the configuration directory, so two Copilot homes on one machine never share an entry. Use it to recognize a `${secret:…}` reference in stored settings as this runtime's own proxy password.
    pub secret_id: String,
}

/// The sandbox proxy password to store.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SandboxProxySecretSaveRequest {
    /// The password to store for the sandbox proxy.
    pub value: String,
}

/// Where the sandbox proxy password was stored.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SandboxProxySecretSaveResult {
    /// Whether the OS keychain accepted the value. False means it was written to the on-disk fallback store instead, which is still readable by `sandbox.proxySecret.get`.
    pub stored_in_keychain: bool,
}

/// The user's sandbox settings and the managed sandbox floor to raise them to.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct SandboxResolveFlooredConfigParams {
    /// The effective managed `sandbox` slice acting as a floor. Omitted when no administrator policy applies, in which case the user's own settings stand unchanged.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub managed_sandbox: Option<serde_json::Value>,
    /// The user's own `sandbox` settings slice. Omitted when the user has configured none, which still yields the managed floor rather than a disabled sandbox.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub user_sandbox: Option<serde_json::Value>,
}

/// The user's sandbox settings raised to the managed floor.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SandboxResolveFlooredConfigResult {
    /// The floored sandbox config, normalized through the declared sandbox shape so only declared fields in a fixed order are present.
    pub sandbox_json: serde_json::Value,
}

/// The managed sandbox policy in force, which selects which remedy the notice offers.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct SandboxUnsupportedStartupNoticeParams {
    /// True when the sandbox stayed on only because the managed policy could not be determined. Distinguishes a policy that positively enforces the sandbox from one that merely failed to resolve, which is the difference between telling the user to contact an administrator and telling them to wait. Defaults to false.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enabled_by_undetermined_policy: Option<bool>,
    /// The managed `sandbox` slice, when a policy supplies one. The notice treats the sandbox as enforced only when this both enables it and sets `failIfUnavailable`, because a managed sandbox that tolerates being unavailable still leaves the user able to turn it off.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub managed_sandbox: Option<serde_json::Value>,
}

/// The startup warning, absent when the machine can run a sandbox.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct SandboxUnsupportedStartupNoticeResult {
    /// The user-facing warning, carrying the platform remedy and the recovery the policy in force actually allows. Absent when this machine supports a sandbox, so a host can call unconditionally and print only what it gets.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub notice: Option<String>,
}

/// Status of the persistent certificate authority of the sandbox credential proxy.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SandboxProxyCaGetStatusResult {
    /// Whether this process can add the certificate authority to OS trust without credentials from a different user. False where OS trust is unsupported, and on Windows when the process cannot elevate itself to write the machine trust store. When false, do not offer to set up the certificate authority.
    pub can_install: bool,
    /// Human-readable reason for the state. On `installed` or `notInstalled`, present only when the certificate authority must be rotated, and then says why.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub detail: Option<String>,
    /// The state of the certificate authority.
    pub state: SandboxProxyCaState,
}

/// Status of the persistent certificate authority of the sandbox credential proxy.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SandboxProxyCaRotateResult {
    /// Whether this process can add the certificate authority to OS trust without credentials from a different user. False where OS trust is unsupported, and on Windows when the process cannot elevate itself to write the machine trust store. When false, do not offer to set up the certificate authority.
    pub can_install: bool,
    /// Human-readable reason for the state. On `installed` or `notInstalled`, present only when the certificate authority must be rotated, and then says why.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub detail: Option<String>,
    /// The state of the certificate authority.
    pub state: SandboxProxyCaState,
}

/// Status of the persistent certificate authority of the sandbox credential proxy.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SandboxProxyCaTrustResult {
    /// Whether this process can add the certificate authority to OS trust without credentials from a different user. False where OS trust is unsupported, and on Windows when the process cannot elevate itself to write the machine trust store. When false, do not offer to set up the certificate authority.
    pub can_install: bool,
    /// Human-readable reason for the state. On `installed` or `notInstalled`, present only when the certificate authority must be rotated, and then says why.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub detail: Option<String>,
    /// The state of the certificate authority.
    pub state: SandboxProxyCaState,
}

/// Status of the persistent certificate authority of the sandbox credential proxy.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SandboxProxyCaRemoveResult {
    /// Whether this process can add the certificate authority to OS trust without credentials from a different user. False where OS trust is unsupported, and on Windows when the process cannot elevate itself to write the machine trust store. When false, do not offer to set up the certificate authority.
    pub can_install: bool,
    /// Human-readable reason for the state. On `installed` or `notInstalled`, present only when the certificate authority must be rotated, and then says why.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub detail: Option<String>,
    /// The state of the certificate authority.
    pub state: SandboxProxyCaState,
}

/// Where this runtime keeps the sandbox proxy password.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SandboxProxySecretGetLocationResult {
    /// Absolute path of the credential store holding the sandbox proxy password. The proxy password belongs to no MCP server, so it is kept in its own directory rather than in the MCP secret store.
    pub config_dir: String,
    /// Identifier of the proxy password entry within that store. Derived from the configuration directory, so two Copilot homes on one machine never share an entry. Use it to recognize a `${secret:…}` reference in stored settings as this runtime's own proxy password.
    pub secret_id: String,
}

/// The sandbox a named feature actually launches under.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct SandboxResolveForOptionalResult {
    /// The effective sandbox config for the requested feature, or a disabled sandbox when the feature is routed out of it.
    pub sandbox_json: serde_json::Value,
}

/// The startup warning, absent when the machine can run a sandbox.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct SandboxGetUnsupportedStartupNoticeResult {
    /// The user-facing warning, carrying the platform remedy and the recovery the policy in force actually allows. Absent when this machine supports a sandbox, so a host can call unconditionally and print only what it gets.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub notice: Option<String>,
}

/// Advice for a path grant, when it needs any.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct SandboxGetPathGrantWarningResult {
    /// The user-facing warning. Absent when the grant needs none, so a host can call unconditionally and show only what it gets.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub warning: Option<String>,
}

/// The documentation URL a host cites when it declines or downgrades a sandbox setting.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SandboxGetDocsUrlResult {
    /// Documentation URL describing the sandbox and the settings that control it.
    pub url: String,
}

/// Whether the host running this runtime can run the command sandbox. The runtime checks `supported` once per process. A capability answer can change while the process runs, for example after the user installs a missing package.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SandboxGetHostSupportResult {
    /// Sandbox policy features whose availability varies between hosts that can run the backend. Empty when `supported` is false, because no feature can run without a backend. Later runtimes can add entries; ignore an entry whose `name` you do not recognize.
    pub capabilities: Vec<SandboxHostCapability>,
    /// Human-readable reason the sandbox cannot run on this host. Present only when `supported` is false.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reason: Option<String>,
    /// Whether a process-containment backend is usable on this host: Seatbelt on macOS, Bubblewrap on Linux, or ProcessContainer on Windows.
    pub supported: bool,
}

/// Extensible identifier of a sandbox policy feature whose availability varies between hosts. A plain string, so an older client decodes a name added by a newer runtime; ignore names you do not recognize. Known values: `network` — sandboxed commands can reach the network (`network.allowOutbound`, on by default); on Linux this needs the tooling for Bubblewrap's private network namespace, such as slirp4netns. `network_filtering` — host rules and the sandbox proxy (`network.allowedHosts`, `network.blockedHosts`, `network.proxy`); on Linux this needs the same tooling as `network`; on Windows it needs Process Security Environment 1.1 host-loopback support or MXC's PSEC 1.0-only proxy-loopback compatibility capability, and a policy that uses it must also set `network.allowLocalNetwork`, because Windows reaches the local proxy only together with private-network access. Compatibility applies only to an explicit identity-less runtime proxy, not general host-loopback access, and other policy restrictions still apply. `denied_paths` — native enforcement of `filesystem.deniedPaths`; on Windows this needs a version whose sandbox contract reports denied-path support. `shell` — shell commands inside the sandbox: bash on macOS and Linux, PowerShell on Windows. `filesystem_enumeration` — enumerate-only filesystem grants, which PowerShell's drive roots use on Windows; this needs a version with Process Security Environment 1.1 filesystem enumeration support. Without it, sandboxed PowerShell still runs, but `Get-Location` may report the drive root, `Set-Location` may fail, and relative paths may resolve against the drive root; the session also receives a `session.warning` with `warningType` `sandbox`. Other platforms always report it.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type SandboxHostCapabilityName = String;

/// True when this process can offer installing the sandbox credential-proxy certificate authority into OS trust.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type SandboxProxyCaCheckInstallationSupportResult = bool;

/// True when the sandbox configuration uses the credential proxy and can need certificate-authority setup.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type SandboxProxyCaCheckRelevanceResult = bool;

/// The effective sandbox settings, with every unset value resolved to its default.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type SandboxSettingsApplyDefaultsResult = HashMap<String, serde_json::Value>;

/// True when an OS-level sandbox can run on this host.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type SandboxCheckPlatformSupportResult = bool;

/// True when the sandbox on this host can reach host loopback directly.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type SandboxCheckWindowsHostLoopbackSupportResult = bool;

/// The user-facing refusal, including the platform remedy and the documentation link.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type SandboxGetUnsupportedMessageResult = String;

/// The user-facing notice that a saved sandbox setting is inert for this session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type SandboxGetUnsupportedSettingDisabledNoticeResult = String;

/// True when the command stays available while enforcement is blocked.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type SandboxCheckEnforcementCommandResult = bool;

/// State of the persistent certificate authority of the sandbox credential proxy.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SandboxProxyCaState {
    /// This platform has no supported OS trust store. The proxy uses a per-process certificate bundle.
    #[serde(rename = "unsupported")]
    Unsupported,
    /// OS trust does not include the certificate authority, or none is stored.
    #[serde(rename = "notInstalled")]
    NotInstalled,
    /// OS trust includes the stored certificate authority.
    #[serde(rename = "installed")]
    Installed,
    /// The runtime could not read the certificate authority or the OS trust store.
    #[serde(rename = "error")]
    Error,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

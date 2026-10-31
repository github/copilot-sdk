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

/// Canvas action invocation parameters.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CanvasActionInvokeRequest {
    /// Action name to invoke
    pub action_name: String,
    /// Action input
    #[serde(skip_serializing_if = "Option::is_none")]
    pub input: Option<serde_json::Value>,
    /// Open canvas instance identifier
    pub instance_id: String,
}

/// Canvas action invocation result.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CanvasActionInvokeResult {
    /// Provider-supplied action result
    #[serde(skip_serializing_if = "Option::is_none")]
    pub result: Option<serde_json::Value>,
}

/// Canvas close parameters.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CanvasCloseRequest {
    /// Open canvas instance identifier
    pub instance_id: String,
}

/// Host capabilities
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CanvasHostContextCapabilities {
    /// Whether canvas rendering is supported
    #[serde(skip_serializing_if = "Option::is_none")]
    pub canvases: Option<bool>,
}

/// Host context supplied by the runtime.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CanvasHostContext {
    /// Host capabilities
    #[serde(skip_serializing_if = "Option::is_none")]
    pub capabilities: Option<CanvasHostContextCapabilities>,
}

/// Declared canvases available in this session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CanvasList {
    /// Declared canvases available in this session
    pub canvases: Vec<DiscoveredCanvas>,
}

/// Live open-canvas snapshot.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CanvasListOpenResult {
    /// Currently open canvas instances
    pub open_canvases: Vec<OpenCanvasInstance>,
}

/// Canvas open parameters.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CanvasOpenRequest {
    /// Provider-local canvas identifier
    pub canvas_id: String,
    /// Owning provider identifier. Optional when the canvasId is unique across providers; required to disambiguate when multiple providers register the same canvasId.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub extension_id: Option<String>,
    /// Canvas open input
    #[serde(skip_serializing_if = "Option::is_none")]
    pub input: Option<serde_json::Value>,
    /// Caller-supplied stable instance identifier
    pub instance_id: String,
}

/// Session context supplied by the runtime.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CanvasSessionContext {
    /// Active session working directory, when known.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub working_directory: Option<String>,
}

/// Canvas close parameters sent to the provider.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CanvasProviderCloseRequest {
    /// Target session identifier
    pub session_id: SessionId,
    /// Owning provider identifier
    pub extension_id: String,
    /// Provider-local canvas identifier
    pub canvas_id: String,
    /// Canvas instance identifier
    pub instance_id: String,
    /// Host context supplied by the runtime.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub host: Option<CanvasHostContext>,
    /// Session context supplied by the runtime.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session: Option<CanvasSessionContext>,
}

/// Canvas action invocation parameters sent to the provider.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CanvasProviderInvokeActionRequest {
    /// Target session identifier
    pub session_id: SessionId,
    /// Owning provider identifier
    pub extension_id: String,
    /// Provider-local canvas identifier
    pub canvas_id: String,
    /// Canvas instance identifier
    pub instance_id: String,
    /// Action name to invoke
    pub action_name: String,
    /// Action input
    #[serde(skip_serializing_if = "Option::is_none")]
    pub input: Option<serde_json::Value>,
    /// Host context supplied by the runtime.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub host: Option<CanvasHostContext>,
    /// Session context supplied by the runtime.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session: Option<CanvasSessionContext>,
}

/// Canvas open parameters sent to the provider.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CanvasProviderOpenRequest {
    /// Target session identifier
    pub session_id: SessionId,
    /// Owning provider identifier
    pub extension_id: String,
    /// Provider-local canvas identifier
    pub canvas_id: String,
    /// Stable caller-supplied canvas instance identifier
    pub instance_id: String,
    /// Canvas open input
    #[serde(skip_serializing_if = "Option::is_none")]
    pub input: Option<serde_json::Value>,
    /// Host context supplied by the runtime.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub host: Option<CanvasHostContext>,
    /// Session context supplied by the runtime.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session: Option<CanvasSessionContext>,
}

/// Canvas open result returned by the provider.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CanvasProviderOpenResult {
    /// Provider-supplied status text
    #[serde(skip_serializing_if = "Option::is_none")]
    pub status: Option<String>,
    /// Provider-supplied title
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
    /// URL for web-rendered canvases
    #[serde(skip_serializing_if = "Option::is_none")]
    pub url: Option<String>,
}

/// Internal canvas provider registration parameters.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CanvasProviderRegisterRequest {
    /// Canvas contributions supplied by the provider
    pub canvases: Vec<serde_json::Value>,
    /// Connection identifier for callback routing
    pub connection_id: String,
    /// Provider metadata supplied by the host
    pub info: serde_json::Value,
}

/// Internal canvas provider unregistration parameters.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CanvasProviderUnregisterRequest {
    /// Connection identifier to unregister
    pub connection_id: String,
}

/// Semantic digest of a strictly parsed and schema-validated JSON MCP card. Both URL-backed and embedded cards are canonicalised with RFC 8785 JSON Canonicalization Scheme, encoded as UTF-8, and hashed with SHA-256.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CardDigest {
    /// Digest algorithm and canonical representation
    #[serde(deserialize_with = "CardDigest::deserialize_algorithm")]
    pub algorithm: CardDigestAlgorithm,
    /// SHA-256 digest of the RFC 8785 canonical UTF-8 bytes, encoded as exactly 64 lowercase hexadecimal characters.
    pub value: String,
}

impl CardDigest {
    fn deserialize_algorithm<'de, D>(deserializer: D) -> Result<CardDigestAlgorithm, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let value = String::deserialize(deserializer)?;
        if value != "sha256-rfc8785" {
            return Err(serde::de::Error::unknown_variant(
                &value,
                &["sha256-rfc8785"],
            ));
        }
        <CardDigestAlgorithm>::deserialize(serde::de::value::StringDeserializer::<D::Error>::new(
            value,
        ))
    }
}

/// A presented handle was not accepted. Handles are runtime-instance scoped, TTL-bound, and single-use, so each way of failing is reported distinctly.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogHandleRejectedError {
    /// Which kind of handle was presented.
    pub handle_type: CatalogHandleType,
    /// Discriminator: a handle was rejected
    pub kind: CatalogHandleRejectedErrorKind,
    /// Human-readable explanation, safe to surface. Never contains the handle itself, nor a query, URL, or secret.
    pub message: String,
    /// Why the handle was rejected.
    pub reason: CatalogHandleRejectionReason,
}

/// The candidate is discoverable but cannot be installed. `application/ai-skill` resolves here, because it stays searchable while remaining typed non-installable.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogNotInstallableError {
    /// Discriminator: the candidate cannot be installed
    pub kind: CatalogNotInstallableErrorKind,
    /// Human-readable explanation, safe to surface. Never contains a query, URL, handle, or secret.
    pub message: String,
    /// Why the candidate cannot be installed.
    pub reason: CatalogNotInstallableReason,
}

/// No transport this runtime can use is available for the requested server.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogUnavailableTransportError {
    /// Discriminator: no usable transport is available
    pub kind: CatalogUnavailableTransportErrorKind,
    /// Human-readable explanation, safe to surface. Never contains a query, URL, handle, or secret.
    pub message: String,
    /// Why no transport could be offered.
    pub reason: CatalogUnavailableTransportReason,
}

/// Concrete configuration file containing an MCP server declaration.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpSourceFile {
    /// RFC 6901 JSON Pointer to the server declaration, when known
    #[serde(skip_serializing_if = "Option::is_none")]
    pub json_pointer: Option<String>,
    /// Canonical file URI for the configuration document
    pub uri: String,
}

/// Plugin identity associated with an MCP server declaration.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpSourcePlugin {
    /// Canonical plugin identity
    pub id: String,
    /// Human-readable plugin name, when available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    /// Plugin version, when available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub version: Option<String>,
}

/// Canonical identity and location of the effective MCP server declaration. The declaration is uniquely addressed by this source id together with the discovered server name.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpSourceRef {
    /// Open semantic editability identifier. Known values are editable and read-only.
    pub editability: String,
    /// Configuration file location, when the declaration is file-backed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub file: Option<McpSourceFile>,
    /// Opaque stable identity for the configuration source. Clients must not parse this value.
    pub id: String,
    /// Open source-kind identifier. Known values include user, workspace, invocation, plugin, builtin, and device-registry.
    pub kind: String,
    /// Plugin identity, when the declaration is plugin-provided.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub plugin: Option<McpSourcePlugin>,
}

/// MCP server discovered by `mcp.discover`, with config source, optional plugin source, transport type, and enabled state.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DiscoveredMcpServer {
    /// Canonical identity and location of the effective server declaration.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub effective_source: Option<McpSourceRef>,
    /// Whether the server is enabled (not in the disabled list)
    pub enabled: bool,
    /// Server name (config key)
    pub name: String,
    /// Configuration source: user, workspace, plugin, or builtin
    pub source: McpServerSource,
    /// Plugin name that provided this server, when source is plugin.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source_plugin: Option<String>,
    /// Plugin version that provided this server, when source is plugin.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source_plugin_version: Option<String>,
    /// Server transport type: stdio, http, sse (deprecated), or memory
    #[serde(skip_serializing_if = "Option::is_none")]
    pub r#type: Option<DiscoveredMcpServerType>,
}

/// Slim input shape for extension_context attachments; identity fields are runtime-derived.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExtensionContextPushInput {
    /// Caller-supplied JSON payload (required, may be null but not undefined)
    pub payload: serde_json::Value,
    /// Human-readable composer pill label
    pub title: String,
    /// Attachment type discriminator
    pub r#type: ExtensionContextPushInputType,
}

/// Opaque integrator-owned process launch profile for one extension entrypoint.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExtensionLaunchProfile {
    /// Opaque integrator-defined arguments passed to the executable. The runtime does not append the extension entrypoint.
    pub args: Vec<String>,
    /// Opaque integrator-defined environment variables. Runtime-owned COPILOT_SDK_PATH, SESSION_ID, and COPILOT_EXTENSION_PARENT_PID values take precedence.
    pub env: HashMap<String, String>,
    /// Executable used to launch the extension entrypoint.
    pub executable: String,
}

/// A discovered extension entrypoint that the registered integrator may classify and resolve to an opaque launch profile.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExtensionLaunchProviderResolveRequest {
    /// Source-qualified extension identifier.
    pub id: String,
    /// Absolute path to the discovered extension entrypoint.
    pub module_path: String,
    /// Human-readable extension name.
    pub name: String,
    /// Discovery source for the extension entrypoint.
    pub source: ExtensionSource,
}

/// The launch profile for a supported entrypoint. Omit launch when the provider does not support the entrypoint.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExtensionLaunchProviderResolveResult {
    /// Opaque launch profile, omitted when this provider does not support the entrypoint.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub launch: Option<ExtensionLaunchProfile>,
}

/// Extensions discovered for the session, with their current status.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExtensionList {
    /// Discovered extensions and their current status
    pub extensions: Vec<Extension>,
}

/// Runtime support and session-context standalone executable extension policy.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExtensionManagedPolicy {
    /// Executable entries discovered from this session's runtime-verified eligible plugins. Under plugin_only, independently launched targets must match these entry paths; an external package, a caller-supplied plugin label, or a synthetic transport bootstrap does not establish eligibility. Empty while pending or when no extension controller is configured.
    pub eligible_plugin_entries: Vec<EligiblePluginExtensionEntry>,
    /// Extension policy enforcement contract version. Version 1 includes standalone discovery, launch admission, and live-policy revocation.
    pub enforcement_version: i64,
    /// Whether standalone extensions are allowed, prohibited by plugin-only policy, or pending unresolved managed policy.
    pub standalone: StandaloneExtensionAdmission,
}

/// Source-qualified extension identifier to disable for the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExtensionsDisableRequest {
    /// Source-qualified extension ID to disable
    pub id: String,
}

/// Source-qualified extension identifier to enable for the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExtensionsEnableRequest {
    /// Source-qualified extension ID to enable
    pub id: String,
}

/// The configuration-change alternative for the transportChoices entry at the same index. Only the selected alternative is applied; entries are not cumulative. The payload stays behind the runtime boundary.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpPlanConfigurationChange {
    /// Names of the configuration fields the change would set, without their values.
    pub changed_fields: Vec<String>,
    /// Configuration key the change applies to.
    pub config_key: String,
    /// Whether the change would create a new entry or modify an existing one.
    pub operation: McpPlanConfigurationOperation,
    /// Scope the change would be written to.
    pub scope: McpPlanScope,
    /// Secret placeholders the written configuration would reference. The constrained placeholder type cannot carry a literal secret value.
    pub secret_references: Vec<String>,
}

/// Final remote configuration, not a template. The producer refuses external-value
/// expansion before presenting this review. Receipt-owned secrets appear only as
/// `${installation-secret:<id>}` references whose `<id>` matches a reviewed
/// `${secret:<id>}` placeholder; values are never included.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpInstallationRemoteConfiguration {
    /// Configured headers, excluding separately authorised OAuth tokens. Values may
    /// contain owned secret references, never secret values.
    pub headers: HashMap<String, String>,
    /// Configured tool selection, not permission to invoke those tools.
    pub tools: Vec<String>,
    /// Transport in the effective persisted remote configuration.
    pub transport: McpPlanRemoteTransport,
    /// Exact resolved endpoint, without templates or secret placeholders.
    pub url: String,
}

/// Normalised identity of the MCP server a plan targets, independent of how the card spelled it.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpPlanResourceIdentity {
    /// Canonical, normalised name of the server, for example `io.github.owner/server`.
    pub canonical_name: String,
    /// Registry identifier of the server, when it came from a registry.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub registry_id: Option<String>,
    /// Local configuration key the server would be recorded under.
    pub server_name: String,
    /// Version advertised by the card, when it declares one.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub version: Option<String>,
}

/// One Registry string-valued configuration entry for the selected transport.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpInstallationInput {
    /// Exact category declared by the selected choice.
    pub category: McpPlanValueCategory,
    /// Exact key declared by the selected choice.
    pub key: String,
    /// Explicit non-secret value. Secret placeholders use a separate input channel.
    pub value: String,
}

/// Outcome of evaluating the planned server against registry and enterprise policy. Evaluation is read-only.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpPlanPolicyResult {
    /// What policy decided for this server.
    pub decision: McpPlanPolicyDecision,
    /// Human-readable explanation, safe to surface. Never contains a query, URL, handle, or secret.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reason: Option<String>,
    /// Which authority produced the decision.
    pub source: McpPlanPolicySource,
}

/// Provenance of the exact validated JSON MCP card content bound privately to a completed plan and its opaque handle.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpPlanProvenance {
    /// Authority associated with the validated card, without path, query, or credentials. Inert untrusted data.
    pub authority: String,
    /// Semantic digest of the exact validated JSON content bound to the plan handle.
    pub card_digest: CardDigest,
    /// JSON MCP media type the validated card was interpreted as.
    pub media_type: McpServerCardMediaType,
    /// ISO 8601 timestamp at which the runtime completed strict parsing and schema validation of the card content.
    pub validated_at: String,
}

/// One non-secret scalar value a transport choice needs before it can be applied.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpPlanRequiredValueScalar {
    /// Where the value is applied when the server is launched.
    pub category: McpPlanValueCategory,
    /// Default supplied by the card, when the value can be resolved without input. Presence is the authoritative indication that a default exists. Inert untrusted data.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub default_value: Option<String>,
    /// Human-readable explanation from the card. Inert untrusted text.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Whether the value may be supplied more than once.
    pub is_repeated: bool,
    /// Key the value is supplied under. Inert untrusted data.
    pub key: String,
    /// Discriminator: this required value uses a scalar type.
    #[serde(deserialize_with = "McpPlanRequiredValueScalar::deserialize_kind")]
    pub kind: McpPlanRequiredValueScalarKind,
    /// Whether the value must be present for the plan to be applicable.
    pub required: bool,
    /// Human-readable label from the card. Inert untrusted text.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
    /// Scalar type the value must conform to.
    pub value_type: McpPlanScalarValueType,
}

impl McpPlanRequiredValueScalar {
    fn deserialize_kind<'de, D>(deserializer: D) -> Result<McpPlanRequiredValueScalarKind, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let value = String::deserialize(deserializer)?;
        if value != "scalar" {
            return Err(serde::de::Error::unknown_variant(&value, &["scalar"]));
        }
        <McpPlanRequiredValueScalarKind>::deserialize(serde::de::value::StringDeserializer::<
            D::Error,
        >::new(value))
    }
}

/// One enumerated non-secret value a transport choice needs before it can be applied. The permitted values are structurally required.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpPlanRequiredValueEnum {
    /// Where the value is applied when the server is launched.
    pub category: McpPlanValueCategory,
    /// Default supplied by the card, when the value can be resolved without input. Presence is the authoritative indication that a default exists. Inert untrusted data.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub default_value: Option<String>,
    /// Human-readable explanation from the card. Inert untrusted text.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Non-empty permitted value set. Inert untrusted data.
    pub enum_values: Vec<String>,
    /// Whether the value may be supplied more than once.
    pub is_repeated: bool,
    /// Key the value is supplied under. Inert untrusted data.
    pub key: String,
    /// Discriminator: this required value uses a fixed enumeration.
    #[serde(deserialize_with = "McpPlanRequiredValueEnum::deserialize_kind")]
    pub kind: McpPlanRequiredValueEnumKind,
    /// Whether the value must be present for the plan to be applicable.
    pub required: bool,
    /// Human-readable label from the card. Inert untrusted text.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
    /// Discriminator: the value must be one of `enumValues`.
    #[serde(deserialize_with = "McpPlanRequiredValueEnum::deserialize_value_type")]
    pub value_type: McpPlanEnumValueType,
}

impl McpPlanRequiredValueEnum {
    fn deserialize_kind<'de, D>(deserializer: D) -> Result<McpPlanRequiredValueEnumKind, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let value = String::deserialize(deserializer)?;
        if value != "enum" {
            return Err(serde::de::Error::unknown_variant(&value, &["enum"]));
        }
        <McpPlanRequiredValueEnumKind>::deserialize(
            serde::de::value::StringDeserializer::<D::Error>::new(value),
        )
    }

    fn deserialize_value_type<'de, D>(deserializer: D) -> Result<McpPlanEnumValueType, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let value = String::deserialize(deserializer)?;
        if value != "enum" {
            return Err(serde::de::Error::unknown_variant(&value, &["enum"]));
        }
        <McpPlanEnumValueType>::deserialize(serde::de::value::StringDeserializer::<D::Error>::new(
            value,
        ))
    }
}

/// A secret a transport choice needs, referenced by placeholder. No secret value ever appears in a plan, and the placeholder resolves against the keychain only when a plan is applied.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpPlanSecretPlaceholder {
    /// Key the secret is supplied under. Inert untrusted data.
    pub key: String,
    /// The runtime-assigned `${secret:<id>}` placeholder written into configuration in place of the value.
    pub placeholder: String,
    /// Human-readable label from the card. Inert untrusted text.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
}

/// An eligible local-package transport choice. Package identity is required and a remote endpoint cannot be represented.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpPlanTransportChoicePackage {
    /// Stable identifier for this choice within the plan, used to select it when the plan is applied.
    pub choice_id: String,
    /// Discriminator: this choice runs a local package
    #[serde(deserialize_with = "McpPlanTransportChoicePackage::deserialize_install_method")]
    pub install_method: McpPlanPackageInstallMethod,
    /// Package identifier. Inert untrusted data.
    pub package_identifier: String,
    /// Packaging ecosystem, for example `oci` or `npm`.
    pub package_type: String,
    /// Typed values this choice requires, excluding secrets.
    pub required_values: Vec<McpPlanRequiredValue>,
    /// Secrets this choice requires, referenced by placeholder only.
    pub secret_placeholders: Vec<McpPlanSecretPlaceholder>,
    /// Local process transport this package choice would use.
    pub transport: McpPlanPackageTransport,
}

impl McpPlanTransportChoicePackage {
    fn deserialize_install_method<'de, D>(
        deserializer: D,
    ) -> Result<McpPlanPackageInstallMethod, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let value = String::deserialize(deserializer)?;
        if value != "package" {
            return Err(serde::de::Error::unknown_variant(&value, &["package"]));
        }
        <McpPlanPackageInstallMethod>::deserialize(
            serde::de::value::StringDeserializer::<D::Error>::new(value),
        )
    }
}

/// An eligible remote-endpoint transport choice. The endpoint is required and package identity cannot be represented.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpPlanTransportChoiceRemote {
    /// Stable identifier for this choice within the plan, used to select it when the plan is applied.
    pub choice_id: String,
    /// Endpoint URL. Inert untrusted data.
    pub endpoint: String,
    /// Discriminator: this choice connects to a remote endpoint
    #[serde(deserialize_with = "McpPlanTransportChoiceRemote::deserialize_install_method")]
    pub install_method: McpPlanRemoteInstallMethod,
    /// Typed values this choice requires, excluding secrets.
    pub required_values: Vec<McpPlanRequiredValue>,
    /// Secrets this choice requires, referenced by placeholder only.
    pub secret_placeholders: Vec<McpPlanSecretPlaceholder>,
    /// Endpoint transport this remote choice would use.
    pub transport: McpPlanRemoteTransport,
}

impl McpPlanTransportChoiceRemote {
    fn deserialize_install_method<'de, D>(
        deserializer: D,
    ) -> Result<McpPlanRemoteInstallMethod, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let value = String::deserialize(deserializer)?;
        if value != "remote" {
            return Err(serde::de::Error::unknown_variant(&value, &["remote"]));
        }
        <McpPlanRemoteInstallMethod>::deserialize(
            serde::de::value::StringDeserializer::<D::Error>::new(value),
        )
    }
}

/// Where a plan would be written.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpPlanTarget {
    /// Configuration key the server would be recorded under within that scope.
    pub config_key: String,
    /// Configuration scope the plan targets.
    pub scope: McpPlanScope,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpInstallationReviewInstall {
    /// Exact reviewed installation action.
    pub action: McpInstallationReviewInstallAction,
    /// Catalogue identity retained from the bound candidate when available.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub catalogue: Option<InstallationCatalogueIdentity>,
    /// Original catalogue trust metadata, not a verification claim.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub catalogue_trust: Option<serde_json::Value>,
    /// The configuration change for the selected alternative only.
    pub configuration_change: McpPlanConfigurationChange,
    /// Complete effective remote configuration for final installation review.
    /// Earlier private selection reviews and package choices omit this field.
    /// The owned remote resource requires it before issuing confirmation.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub effective_configuration: Option<McpInstallationRemoteConfiguration>,
    /// Identity from the retained plan, not caller display text.
    pub identity: McpPlanResourceIdentity,
    /// Non-secret values supplied for this selected alternative.
    pub inputs: Vec<McpInstallationInput>,
    /// Policy decision bound to this plan.
    pub policy: McpPlanPolicyResult,
    /// Original source identity and content commitment.
    pub provenance: McpPlanProvenance,
    /// Explicit reviewed backend selection; no backend is accessed when no secrets are supplied.
    pub secret_storage: McpInstallationSecretStorage,
    /// Only the selected alternative is applied.
    pub selected_choice: McpPlanTransportChoice,
    /// Exact reviewed placeholders supplied separately. Never secret values.
    pub supplied_secrets: Vec<String>,
    /// Exact reviewed user-scope destination.
    pub target: McpPlanTarget,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpInstallationReviewUninstall {
    /// Exact reviewed installation action.
    pub action: McpInstallationReviewUninstallAction,
    /// Identity from the installed receipt.
    pub identity: McpPlanResourceIdentity,
    /// Receipt-owned installation being removed.
    pub installation_id: String,
    /// Exact planner-owned secret slots to remove, excluding shared OAuth grants.
    pub owned_secret_count: i64,
    /// Current removal policy, independent of permission to activate the server.
    pub policy: McpPlanPolicyResult,
    /// Shared profile authentication is deliberately retained, not pending cleanup.
    pub preserves_shared_authentication: bool,
    /// Source identity and content commitment retained by the installed receipt.
    pub provenance: McpPlanProvenance,
    /// Whether uninstall restores a protected pre-install configuration.
    pub restores_previous_configuration: bool,
    /// Exact destination, checked for intervening changes before mutation.
    pub target: McpPlanTarget,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InstallationReviewMcp {
    /// Reviewed resource discriminator.
    pub resource: InstallationReviewMcpResource,
    /// The exact MCP action and its reviewed changes.
    pub review: McpInstallationReview,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InstallationReviewSkill {
    /// Reviewed resource discriminator.
    pub resource: InstallationReviewSkillResource,
    /// The exact verified Skill action and its reviewed files.
    pub review: SkillInstallationReview,
}

/// Review for installing a verified Agent Plugin.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InstallationReviewPluginReview {
    /// Exact reviewed Agent Plugin installation action.
    pub action: InstallationReviewPluginReviewAction,
    /// Catalogue identity retained from the bound candidate, or verified data for a reference.
    pub catalogue: InstallationCatalogueIdentity,
    /// Manifest description when present.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Reviewed files and digests.
    pub files: Vec<PluginInstallationFileReview>,
    /// Installing never grants immediate use; the plugin is registered disabled.
    pub installs_disabled: bool,
    /// Complete verified manifest content. Planning refuses with review-too-large
    /// when this exceeds 262144 UTF-8 bytes; it is never truncated.
    pub manifest_content: String,
    /// Relative path of the verified plugin manifest.
    pub manifest_path: String,
    /// MCP servers the verified files declare, exactly as an install of these files loads
    /// them. Omitted when the plugin declares none.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mcp_servers: Option<Vec<PluginInstallationMcpServerReview>>,
    /// Installed plugin name, derived from the verified manifest exactly as a direct install derives it.
    pub name: String,
    /// Exact verified source identity.
    pub source: PluginInstallationSource,
    /// Exact install location without an absolute host path.
    pub target: PluginInstallationLocation,
    /// Total reviewed payload size in bytes.
    pub total_bytes: i64,
    /// Manifest version when present.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub version: Option<String>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InstallationReviewPlugin {
    /// Reviewed resource discriminator.
    pub resource: InstallationReviewPluginResource,
    /// The exact verified Agent Plugin action and its reviewed files.
    pub review: InstallationReviewPluginReview,
}

/// One connection-owned, expiring request for a trusted host's explicit user decision.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InstallationConfirmationRequest {
    /// Opaque one-use challenge. Return unchanged; never log or persist.
    pub confirmation_id: String,
    /// Original plan expiry as an ISO 8601 timestamp. Confirmation never extends it.
    pub expires_at: String,
    /// Random identifier of this installation operation, not a plan handle.
    pub operation_id: String,
    /// Original engine-resolved selector for a bound operation, never a dispatch default.
    /// Bound MCP confirmation always includes it; correlate it with the original pending action.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub policy_session_id: Option<String>,
    /// Resource-specific review to present before collecting the user's decision.
    pub review: InstallationReview,
    /// Opaque commitment to the exact review and inputs. Return unchanged; never log.
    pub review_fingerprint: String,
}

/// A response is meaningful only on the connection and request that issued its challenge.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InstallationConfirmationResponse {
    /// Exact challenge from the request.
    pub confirmation_id: String,
    /// Fresh explicit user decision. There is no default.
    pub decision: InstallationDecision,
    /// Exact review commitment from the request.
    pub review_fingerprint: String,
}

/// Applies exactly one previously prepared operation on its original connection.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpApplyInstallRequest {
    /// Capabilities required by the original prepared operation.
    pub contract: CatalogClientContract,
    /// Runtime-issued ID already returned by prepareInstall, never reused or rebound.
    pub operation_id: String,
    /// Same existing attached or privately borrowed session as preparation.
    pub policy_session_id: String,
}

/// One-use application of the exact retained removal plan.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpApplyUninstallRequest {
    /// Required authenticated bound installation capabilities.
    pub contract: CatalogClientContract,
    /// Opaque original removal plan, consumed once.
    pub plan_handle: String,
    /// Same existing selected session as removal preparation.
    pub policy_session_id: String,
}

/// MCP server, tool name, and arguments to invoke from an MCP App view.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpAppsCallToolRequest {
    /// Tool arguments
    #[serde(skip_serializing_if = "Option::is_none")]
    pub arguments: Option<HashMap<String, serde_json::Value>>,
    /// **Required.** Server whose ui:// view issued the request. Per SEP-1865 ('callable by the app from this server only'), the call is rejected when this differs from `serverName`, and rejected outright when missing.
    pub origin_server_name: String,
    /// MCP server hosting the tool
    pub server_name: String,
    /// MCP tool name
    pub tool_name: String,
}

/// MCP server to diagnose MCP Apps wiring for.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpAppsDiagnoseRequest {
    /// MCP server to probe
    pub server_name: String,
}

/// Diagnostic snapshot of MCP Apps wiring for the named server.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpAppsDiagnoseResult {
    /// Capability negotiation snapshot
    pub capability: McpAppsDiagnoseCapability,
    /// What the server returned for this session
    pub server: McpAppsDiagnoseServer,
}

/// Current host context advertised to MCP App guests.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpAppsHostContext {
    /// Current host context
    pub context: McpAppsHostContextDetails,
}

/// MCP server to list app-callable tools for.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpAppsListToolsRequest {
    /// **Required.** Server whose ui:// view issued the request. Per SEP-1865 ('callable by the app from this server only'), the call is rejected when this differs from `serverName`, and rejected outright when missing.
    pub origin_server_name: String,
    /// MCP server hosting the app
    pub server_name: String,
}

/// App-callable tools from the named MCP server.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpAppsListToolsResult {
    /// App-callable tools from the server
    pub tools: Vec<HashMap<String, serde_json::Value>>,
}

/// MCP server and resource URI to fetch.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpAppsReadResourceRequest {
    /// Name of the MCP server hosting the resource
    pub server_name: String,
    /// Resource URI (typically ui://...)
    pub uri: String,
}

/// Resource contents returned by the MCP server.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpAppsReadResourceResult {
    /// Resource contents returned by the server
    pub contents: Vec<McpAppsResourceContent>,
}

/// Host context advertised to MCP App guests
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpAppsSetHostContextDetails {
    /// Display modes the host supports
    #[serde(skip_serializing_if = "Option::is_none")]
    pub available_display_modes: Option<Vec<McpAppsSetHostContextDetailsAvailableDisplayMode>>,
    /// Current display mode (SEP-1865)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub display_mode: Option<McpAppsSetHostContextDetailsDisplayMode>,
    /// BCP-47 locale, e.g. 'en-US'
    #[serde(skip_serializing_if = "Option::is_none")]
    pub locale: Option<String>,
    /// Platform type for responsive design
    #[serde(skip_serializing_if = "Option::is_none")]
    pub platform: Option<McpAppsSetHostContextDetailsPlatform>,
    /// UI theme preference per SEP-1865
    #[serde(skip_serializing_if = "Option::is_none")]
    pub theme: Option<McpAppsSetHostContextDetailsTheme>,
    /// IANA timezone, e.g. 'America/New_York'
    #[serde(skip_serializing_if = "Option::is_none")]
    pub time_zone: Option<String>,
    /// Host application identifier
    #[serde(skip_serializing_if = "Option::is_none")]
    pub user_agent: Option<String>,
}

/// Host context to advertise to MCP App guests.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpAppsSetHostContextRequest {
    /// Host context advertised to MCP App guests
    pub context: McpAppsSetHostContextDetails,
}

/// The requestId previously passed to executeSampling that should be cancelled.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpCancelSamplingExecutionParams {
    /// The requestId previously passed to executeSampling that should be cancelled
    pub request_id: RequestId,
}

/// Indicates whether an in-flight sampling execution with the given requestId was found and cancelled.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpCancelSamplingExecutionResult {
    /// True if an in-flight execution with the given requestId was found and signalled to cancel. False when no such execution is in flight (already completed, never started, or cancelled by another caller).
    pub cancelled: bool,
}

/// MCP server name and configuration to add to user configuration.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpConfigAddRequest {
    /// MCP server configuration (stdio process or remote HTTP/SSE)
    pub config: serde_json::Value,
    /// Unique name for the MCP server
    pub name: String,
}

/// MCP server names to disable for new sessions.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpConfigDisableRequest {
    /// Names of MCP servers to disable. Each server is added to the persisted disabled list so new sessions skip it. Already-disabled names are ignored. Active sessions keep their current connections until they end.
    pub names: Vec<String>,
}

/// MCP server names to enable for new sessions.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpConfigEnableRequest {
    /// Names of MCP servers to enable. Each server is removed from the persisted disabled list so new sessions spawn it. Unknown or already-enabled names are ignored.
    pub names: Vec<String>,
}

/// User-configured MCP servers, keyed by server name.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpConfigList {
    /// All MCP servers from user config, keyed by name
    pub servers: HashMap<String, serde_json::Value>,
}

/// MCP server name to remove from user configuration.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpConfigRemoveRequest {
    /// OAuth Client ID Metadata Document URL whose persisted credentials should also be removed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auth_client_id_metadata_url: Option<String>,
    /// Name of the MCP server to remove
    pub name: String,
}

/// MCP server name and replacement configuration to write to user configuration.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpConfigUpdateRequest {
    /// MCP server configuration (stdio process or remote HTTP/SSE)
    pub config: serde_json::Value,
    /// Name of the MCP server to update
    pub name: String,
}

/// Effective MCP configuration with optional live observations from matching already materialized servers.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpConfiguredServerList {
    /// Effective configured MCP servers.
    pub servers: Vec<McpConfiguredServer>,
}

/// Credential-free authentication identity used to configure GitHub MCP.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct McpConfigureGitHubRequest {
    /// Opaque runtime auth info for GitHub MCP configuration. Marked internal: an in-process runtime shape (configureGitHubMcp is a no-op over the wire).
    #[doc(hidden)]
    pub(crate) auth_info: serde_json::Value,
}

/// Result of configuring GitHub MCP.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpConfigureGitHubResult {
    /// Whether GitHub MCP configuration changed.
    pub changed: bool,
}

/// Name of the MCP server to disable for the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpDisableRequest {
    /// Name of the MCP server to disable
    pub server_name: String,
}

/// Extensible [`McpDisableRequest`], including inputs added after it was published.
///
/// Required inputs are [`McpDisableOptions::new`] arguments; optional inputs have fluent setters.
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
pub struct McpDisableOptions {
    #[serde(flatten)]
    legacy: McpDisableRequest,
    #[serde(skip_serializing_if = "Option::is_none")]
    expected_installation_id: Option<String>,
}

impl McpDisableOptions {
    /// Creates options with the required inputs.
    pub fn new(server_name: impl Into<String>) -> Self {
        Self {
            legacy: McpDisableRequest {
                server_name: server_name.into(),
            },
            expected_installation_id: None,
        }
    }

    /// Required for an owned installation; omission preserves only manual-server behaviour.
    pub fn expected_installation_id(mut self, value: impl Into<String>) -> Self {
        self.expected_installation_id = Some(value.into());
        self
    }
}

/// Optional working directory used as context for MCP server discovery.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpDiscoverRequest {
    /// Whether to include canonical effectiveSource metadata for each discovered server. Callers must opt in so protocol-3 clients retain the legacy closed response shape.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub include_effective_source: Option<bool>,
    /// Working directory used as context for discovery (e.g., plugin resolution)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub working_directory: Option<String>,
}

/// MCP servers discovered from user, workspace, plugin, and built-in sources.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpDiscoverResult {
    /// MCP servers discovered from all sources
    pub servers: Vec<DiscoveredMcpServer>,
}

/// Name of the MCP server to enable for the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpEnableRequest {
    /// Name of the MCP server to enable
    pub server_name: String,
}

/// Extensible [`McpEnableRequest`], including inputs added after it was published.
///
/// Required inputs are [`McpEnableOptions::new`] arguments; optional inputs have fluent setters.
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
pub struct McpEnableOptions {
    #[serde(flatten)]
    legacy: McpEnableRequest,
    #[serde(skip_serializing_if = "Option::is_none")]
    expected_installation_id: Option<String>,
}

impl McpEnableOptions {
    /// Creates options with the required inputs.
    pub fn new(server_name: impl Into<String>) -> Self {
        Self {
            legacy: McpEnableRequest {
                server_name: server_name.into(),
            },
            expected_installation_id: None,
        }
    }

    /// Exact receipt identity for explicit owned activation in this session.
    pub fn expected_installation_id(mut self, value: impl Into<String>) -> Self {
        self.expected_installation_id = Some(value.into());
        self
    }
}

/// Raw MCP CreateMessageRequest params, as received in the `sampling.requested` event. Treated as opaque at the schema layer; the runtime converts the embedded MCP messages into the OpenAI chat-completion shape internally.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpExecuteSamplingRequest {}

/// Identifiers and raw MCP CreateMessageRequest params used to run a sampling inference.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpExecuteSamplingParams {
    /// The original MCP JSON-RPC request ID (string or number). Used by the runtime to correlate the inference with the originating MCP request for telemetry; this is distinct from `requestId` (which is the schema-level cancellation handle).
    pub mcp_request_id: serde_json::Value,
    /// Raw MCP CreateMessageRequest params, as received in the `sampling.requested` event. Treated as opaque at the schema layer; the runtime converts the embedded MCP messages into the OpenAI chat-completion shape internally.
    pub request: McpExecuteSamplingRequest,
    /// Caller-provided unique identifier for this sampling execution. Use this same ID with cancelSamplingExecution to cancel the in-flight call. Must be unique within the session for the lifetime of the call.
    pub request_id: RequestId,
    /// Name of the MCP server that initiated the sampling request
    pub server_name: String,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpHeadersHandlePendingHeadersRefreshRequestHeaders {
    /// Headers to overlay onto the MCP request. Dynamic headers override static config headers but do not replace SDK-managed request headers.
    pub headers: HashMap<String, String>,
    /// Headers-refresh response variant discriminator.
    pub kind: McpHeadersHandlePendingHeadersRefreshRequestHeadersKind,
    /// Optional lifetime in milliseconds for these returned headers. The runtime clamps its configured cache lifetime to this value.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub ttl_ms: Option<i64>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpHeadersHandlePendingHeadersRefreshRequestNone {
    /// Headers-refresh response variant discriminator.
    pub kind: McpHeadersHandlePendingHeadersRefreshRequestNoneKind,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpHeadersHandlePendingHeadersRefreshRequestError {
    /// Headers-refresh response variant discriminator.
    pub kind: McpHeadersHandlePendingHeadersRefreshRequestErrorKind,
    /// Host credential broker failure, denial, or revocation reason.
    pub message: String,
}

/// MCP headers refresh request id and the host response.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpHeadersHandlePendingHeadersRefreshRequestRequest {
    /// Headers refresh request identifier from mcp.headers_refresh_required
    pub request_id: RequestId,
    /// Host response: supply dynamic headers or decline this refresh.
    pub result: McpHeadersHandlePendingHeadersRefreshRequest,
}

/// Indicates whether the pending MCP headers refresh response was accepted.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpHeadersHandlePendingHeadersRefreshRequestResult {
    /// Whether the response was accepted. False if the request was unknown, timed out, or already resolved.
    pub success: bool,
}

/// Already-confirmed durable work must be reconciled before new mutations or inventory.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpInstallationManagementOutcomeRecoveryRequired {
    /// Installation management outcome discriminator.
    pub kind: McpInstallationManagementOutcomeRecoveryRequiredKind,
}

/// Inert, runtime-owned admission. The operation ID is known before confirmation or effects.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpPreparedInstall {
    /// Original plan expiry in Unix epoch milliseconds; preparation does not extend it.
    pub expires_at_epoch_ms: i64,
    /// Original connection-owned operation, known before the first confirmation callback.
    pub operation_id: String,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpInstallationManagementOutcomeInstallPrepared {
    /// Installation management outcome discriminator.
    pub kind: McpInstallationManagementOutcomeInstallPreparedKind,
    /// Known original operation identity, returned before callback or effects.
    pub operation: McpPreparedInstall,
}

/// Durable configuration ownership is distinct from session-specific usability.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpInstallationSummary {
    /// Catalogue identity retained from the installed plan when available.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub catalogue: Option<InstallationCatalogueIdentity>,
    /// Exact alternative retained in the installing receipt.
    pub choice_id: String,
    /// Identity retained from the validated original plan.
    pub identity: McpPlanResourceIdentity,
    /// Exact durable installation receipt identity.
    pub installation_id: String,
    /// ISO 8601 wall-clock installation time when available.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub installed_at: Option<String>,
    /// Original installing operation, not a fresh management operation.
    pub operation_id: String,
    /// Ownership or setup state, never inferred proof of tool usability.
    pub state: McpInstallationState,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpInstallationManagementOutcomeListed {
    /// Owned receipts visible to the selected account and host.
    pub installations: Vec<McpInstallationSummary>,
    /// Installation management outcome discriminator.
    pub kind: McpInstallationManagementOutcomeListedKind,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpInstallationManagementOutcomeRecovered {
    /// Freshly inspected receipts after successful durable reconciliation.
    pub installations: Vec<McpInstallationSummary>,
    /// Installation management outcome discriminator.
    pub kind: McpInstallationManagementOutcomeRecoveredKind,
}

/// Exact inert removal plan. No configuration or credentials have changed.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpUninstallPlan {
    /// Original wall-clock expiry in milliseconds. Applying never renews it.
    pub expires_at_epoch_ms: i64,
    /// Original owned receipt being removed.
    pub installation: McpInstallationSummary,
    /// The original operation, inspectable and cancellable on this same connection.
    pub operation_id: String,
    /// Exact configured input slots owned by this installation, never shared OAuth tokens.
    pub owned_secret_count: i64,
    /// One-use original connection and authority-bound plan handle.
    pub plan_handle: String,
    /// Shared authentication is deliberately retained; revocation is a separate action.
    pub preserves_shared_authentication: bool,
    /// Whether removal restores a protected earlier configuration.
    pub restores_previous_configuration: bool,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpInstallationManagementOutcomeUninstallPlanned {
    /// Installation management outcome discriminator.
    pub kind: McpInstallationManagementOutcomeUninstallPlannedKind,
    /// Original owned removal plan and operation.
    pub plan: McpUninstallPlan,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpInstallationOperationStatusPreparing {
    /// Whether cancellation has been requested, not proof that a write was undone.
    pub cancellation_requested: bool,
    /// Original runtime-issued operation identity.
    pub operation_id: String,
    /// Original operation progress discriminator.
    pub phase: McpInstallationOperationStatusPreparingPhase,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpInstallationOperationStatusPrepared {
    /// Whether the inert prepared operation was asked to cancel.
    pub cancellation_requested: bool,
    /// Original runtime-issued operation identity.
    pub operation_id: String,
    /// Original operation progress discriminator.
    pub phase: McpInstallationOperationStatusPreparedPhase,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpInstallationOperationStatusAwaitingConfirmation {
    /// Whether the pending human callback was asked to cancel.
    pub cancellation_requested: bool,
    /// Original runtime-issued operation identity.
    pub operation_id: String,
    /// Original operation progress discriminator.
    pub phase: McpInstallationOperationStatusAwaitingConfirmationPhase,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpInstallationOperationStatusRevalidating {
    /// Whether source or authority revalidation was asked to cancel.
    pub cancellation_requested: bool,
    /// Original runtime-issued operation identity.
    pub operation_id: String,
    /// Original operation progress discriminator.
    pub phase: McpInstallationOperationStatusRevalidatingPhase,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpInstallationOperationStatusApplying {
    /// Whether applying was asked to cancel; already-started effects retain their lease.
    pub cancellation_requested: bool,
    /// Original runtime-issued operation identity.
    pub operation_id: String,
    /// Original operation progress discriminator.
    pub phase: McpInstallationOperationStatusApplyingPhase,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpInstallationOutcomeInstalled {
    /// Durable installation succeeded but final transaction cleanup remains.
    pub cleanup_pending: bool,
    /// Receipt identity produced by the confirmed transaction.
    pub installation: McpInstallationSummary,
    /// Terminal installation outcome discriminator.
    pub kind: McpInstallationOutcomeInstalledKind,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpInstallationOutcomeUninstalled {
    /// Durable removal succeeded but final cleanup remains.
    pub cleanup_pending: bool,
    /// Exact removed receipt identity.
    pub installation_id: String,
    /// Terminal installation outcome discriminator.
    pub kind: McpInstallationOutcomeUninstalledKind,
    /// Original removal operation.
    pub operation_id: String,
    /// Any grants in the incumbent shared OAuth store remain unowned and retained.
    pub preserved_shared_authentication: bool,
    /// Exact owned input slots removed, excluding shared OAuth credentials.
    pub removed_owned_secrets: i64,
    /// Whether protected pre-install configuration was restored.
    pub restored_previous_configuration: bool,
}

/// The durable transaction was aborted or fully compensated.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpInstallationOutcomeRolledBack {
    /// Terminal installation outcome discriminator.
    pub kind: McpInstallationOutcomeRolledBackKind,
    /// Original connection-owned operation.
    pub operation_id: String,
    /// Cause of the fully aborted or compensated operation.
    pub reason: McpInstallationFailureReason,
}

/// A write may have completed. Recover and inspect durable state before retrying.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpInstallationOutcomeRecoveryRequired {
    /// Terminal installation outcome discriminator.
    pub kind: McpInstallationOutcomeRecoveryRequiredKind,
    /// Operation whose durable result must be recovered and inspected.
    pub operation_id: String,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpInstallationOutcomeDeclined {
    /// Terminal installation outcome discriminator.
    pub kind: McpInstallationOutcomeDeclinedKind,
    /// Original operation explicitly declined by the user.
    pub operation_id: String,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpInstallationOutcomeCancelled {
    /// Terminal installation outcome discriminator.
    pub kind: McpInstallationOutcomeCancelledKind,
    /// Original operation cancelled before a terminal application result.
    pub operation_id: String,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpInstallationOutcomeRefused {
    /// Terminal installation outcome discriminator.
    pub kind: McpInstallationOutcomeRefusedKind,
    /// Present once an operation has been allocated; never a plan handle.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub operation_id: Option<String>,
    /// Specific bounded refusal, never a success-shaped fallback.
    pub reason: McpInstallationFailureReason,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpInstallationOperationStatusCompleted {
    /// Whether cancellation was requested before the terminal result.
    pub cancellation_requested: bool,
    /// Original runtime-issued operation identity.
    pub operation_id: String,
    /// Immutable terminal receipt.
    pub outcome: McpInstallationOutcome,
    /// Original operation progress discriminator.
    pub phase: McpInstallationOperationStatusCompletedPhase,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpInstallationManagementOutcomeOperation {
    /// Installation management outcome discriminator.
    pub kind: McpInstallationManagementOutcomeOperationKind,
    /// Original-connection operation snapshot.
    pub operation: McpInstallationOperationStatus,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpInstallationManagementOutcomeRefused {
    /// Installation management outcome discriminator.
    pub kind: McpInstallationManagementOutcomeRefusedKind,
    /// Specific bounded refusal.
    pub reason: McpInstallationFailureReason,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpInstallationManagementResultOutcome {
    /// A handled management outcome with an honoured contract.
    pub kind: McpInstallationManagementResultOutcomeKind,
    /// Capabilities actually honoured for this request.
    pub negotiated: CatalogNegotiatedContract,
    /// Observed management outcome.
    pub outcome: McpInstallationManagementOutcome,
}

/// Existing-operation control. A new session selector is deliberately not accepted.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpInstallationOperationRequest {
    /// Original installation wire capability; new-work authentication is not reacquired.
    pub contract: CatalogClientContract,
    /// Exact runtime-issued operation ID on the original connection.
    pub operation_id: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpInstallationResultOutcome {
    /// A handled operation outcome with an honoured contract.
    pub kind: McpInstallationResultOutcomeKind,
    /// Capabilities actually honoured for this request.
    pub negotiated: CatalogNegotiatedContract,
    /// Terminal result of the original operation.
    pub outcome: McpInstallationOutcome,
}

/// A request-local value for one exact reviewed placeholder. Never logged or persisted in a plan.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpInstallationSecret {
    /// Exact placeholder from the selected choice, not a caller-chosen backend identifier.
    pub placeholder: String,
    /// Fresh explicit secret value. It is omitted from confirmation reviews and telemetry.
    pub value: String,
}

/// New-work inventory or recovery request under an explicitly selected existing session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpInstallationsRequest {
    /// Required authenticated bound installation contract.
    pub contract: CatalogClientContract,
    /// Existing selected local session on this connection.
    pub policy_session_id: String,
}

/// A normalised, inert description of what installing an MCP server would involve. Carries no raw card, no install specification, and no secret value.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpInstallPlan {
    /// Alternative configuration changes, with exactly one entry for each transportChoices entry in the same order. Only the entry for the subsequently selected transport applies; these are not cumulative writes. Payloads remain behind the runtime boundary.
    pub configuration_changes: Vec<McpPlanConfigurationChange>,
    /// Normalised identity of the server the plan would install.
    pub identity: McpPlanResourceIdentity,
    /// Opaque, runtime-instance scoped, TTL-bound, single-use handle for this plan. Rejected when stale, replayed, or presented to a different runtime instance. Never logged.
    pub plan_handle: String,
    /// ISO 8601 timestamp after which the plan handle is stale and will be rejected. Abandoning a plan needs no call: an unused handle simply expires, so cancellation before commit is side-effect free.
    pub plan_handle_expires_at: String,
    /// Outcome of evaluating the server against registry and enterprise policy.
    pub policy: McpPlanPolicyResult,
    /// Origin and semantic digest of the exact validated JSON MCP card content bound to this plan.
    pub provenance: McpPlanProvenance,
    /// Identifier of the choice the runtime would pick by default. Omitted when there is no eligible transport, or when the runtime expresses no preference.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub recommended_transport_choice_id: Option<String>,
    /// Whether applying this plan would require an MCP reload to take effect. Planning itself never reloads.
    pub reload_required: bool,
    /// True only when every eligible transport choice needs additional values or secrets. False means at least one choice needs no additional configuration, not that every choice is ready. A later apply operation must validate the selected choice's own inputs, secrets and policy after explicit confirmation.
    pub requires_interactive_configuration: bool,
    /// Configuration scope and key the plan would write to.
    pub target: McpPlanTarget,
    /// Every eligible transport, so a host can present an explicit choice. A completed plan always has at least one; when none is eligible, planning returns `CatalogUnavailableTransportError` instead.
    pub transport_choices: Vec<McpPlanTransportChoice>,
}

/// Server name to check running status for.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpIsServerRunningRequest {
    /// Name of the MCP server to check
    pub server_name: String,
}

/// Whether the named MCP server is running.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpIsServerRunningResult {
    /// True if the server has an active client and transport.
    pub running: bool,
}

/// Names of the MCP servers the runtime supplies itself.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct McpListBuiltinServerNamesResult {
    /// Every built-in server name. A host disabling the built-ins wholesale suppresses exactly this set.
    pub names: Vec<String>,
}

/// Server name whose tool list should be returned.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpListToolsRequest {
    /// Name of the connected MCP server whose tools to list.
    pub server_name: String,
}

/// Tools exposed by the connected MCP server. Throws when the server is not connected.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpListToolsResult {
    /// Tools exposed by the server.
    pub tools: Vec<McpTools>,
}

/// Identifies the MCP server whose persisted OAuth credentials were updated.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpOauthAuthenticationStateChangedRequest {
    /// Whether the target session must mint a session-scoped access token instead of reusing a shared access token persisted by another session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub refresh_session_token: Option<bool>,
    /// Name of the MCP server whose OAuth credentials were updated. Omit only when the host cannot identify the server.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub server_name: Option<String>,
}

/// Targets only the original prepared/applying owned login on this exact session requester.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpOauthCancelLoginRequest {
    /// The same authoritative installation identity supplied during preparation.
    pub expected_installation_id: String,
    /// Runtime-issued login handle known before the effectful login request begins.
    pub login_id: String,
}

/// Honest terminal cancellation result; persistence or recovery failures remain RPC errors.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpOauthCancelLoginResult {
    /// True after cancellation settles, false when the original login already connected successfully.
    pub cancelled: bool,
}

/// A remote MCP server whose authorization server should be asked about Dynamic Client Registration.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct McpOauthCheckDcrSupportRequest {
    /// Raw protected-resource metadata text from the server's `WWW-Authenticate` challenge. Omit to discover the authorization server from the server URL instead.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub resource_metadata_text: Option<String>,
    /// Proxy the request must travel through. Omit to use the session's own egress settings.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sandbox_proxy: Option<String>,
    /// URL of the remote MCP server that answered 401.
    pub server_url: String,
    /// Locally owned session whose egress policy and sandbox proxy constrain the request. Omitted or nonlocal session IDs provide no local session egress state.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_id: Option<SessionId>,
}

/// Whether the server's authorization server registers clients dynamically.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct McpOauthCheckDcrSupportResult {
    /// Authorization server the metadata pointed at. Absent when none could be discovered.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub authorization_server_url: Option<String>,
    /// Whether the authorization server advertises a client registration endpoint.
    pub supports_dcr: bool,
}

/// Host-delivered callback for a runtime-managed MCP OAuth login.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpOauthCompleteRequest {
    /// Opaque identifier returned by session.mcp.oauth.login for the pending external callback.
    pub authorization_id: String,
    /// Full externally visible HTTPS callback URL received by the host, including the authorization response query parameters. Applications behind a reverse proxy must reconstruct the public URL rather than passing an internal proxy URL.
    pub callback_url: String,
}

/// An OAuth challenge from a remote MCP server whose protected-resource metadata should be read.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct McpOauthFetchProtectedResourceMetadataRequest {
    /// Request headers the configured server sends, as a serialized JSON object. Omit to send none.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub headers_json: Option<String>,
    /// URL of the remote MCP server that answered 401.
    pub request_url: String,
    /// Metadata URL named in the server's `WWW-Authenticate` challenge. Omit to try conventional metadata endpoints derived from the request URL. It is server-controlled, so it passes the same egress policy check as the request URL.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub resource_metadata_url: Option<String>,
    /// Proxy the request must travel through. Omit to use the session's own egress settings.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sandbox_proxy: Option<String>,
    /// Locally owned session whose egress policy and sandbox proxy constrain the request. Omitted or nonlocal session IDs provide no local session egress state.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_id: Option<SessionId>,
    /// User agent to send. Omit to use the runtime's own.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub user_agent: Option<String>,
}

/// Raw protected-resource metadata from the explicit metadata URL or conventional discovery.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct McpOauthFetchProtectedResourceMetadataResult {
    /// Metadata document exactly as the server returned it, unparsed. Absent when no document could be read from the explicit metadata URL or, when omitted, the conventional metadata endpoints.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub metadata_text: Option<String>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpOauthPendingRequestResponseToken {
    /// Access token acquired by the SDK host
    pub access_token: String,
    /// Token lifetime in seconds, if known.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub expires_in: Option<i64>,
    /// OAuth response variant discriminator.
    pub kind: McpOauthPendingRequestResponseTokenKind,
    /// OAuth token type. Defaults to bearer when omitted.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub token_type: Option<String>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpOauthPendingRequestResponseCancelled {
    /// OAuth response variant discriminator.
    pub kind: McpOauthPendingRequestResponseCancelledKind,
}

/// Delegate noninteractive refresh to the original runtime authorization operation.
/// Valid only for a Refresh request advertising runtimeRefreshSupported.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpOauthPendingRequestResponseRuntimeRefresh {
    /// OAuth response variant discriminator.
    pub kind: McpOauthPendingRequestResponseRuntimeRefreshKind,
}

/// Pending MCP OAuth request ID and host token, cancellation, or advertised runtime refresh response.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpOauthHandlePendingRequest {
    /// OAuth request identifier from the mcp.oauth_required event
    pub request_id: RequestId,
    /// Host response to the pending OAuth request.
    pub result: McpOauthPendingRequestResponse,
}

/// Indicates whether the pending MCP OAuth response was accepted.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpOauthHandlePendingResult {
    /// Whether the response was accepted. False if the request was unknown, timed out, or already resolved.
    pub success: bool,
}

/// An authorization server URL to classify.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct McpOauthIsMicrosoftAuthorizationServerRequest {
    /// Authorization server URL, typically the one `mcp.oauth.checkDcrSupport` discovered.
    pub authorization_server_url: String,
}

/// Whether the URL is a Microsoft Entra ID authorization server.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct McpOauthIsMicrosoftAuthorizationServerResult {
    /// Whether the host is one Microsoft Entra ID serves authorization from.
    pub is_microsoft_authorization_server: bool,
}

/// Remote MCP server name and optional overrides controlling reauthentication, OAuth client display name, callback handling, and static OAuth client selection.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpOauthLoginRequest {
    /// Optional override for the body text shown on the OAuth loopback callback success page. When omitted, the runtime applies a neutral fallback; callers driving interactive auth should pass surface-specific copy telling the user where to return.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub callback_success_message: Option<String>,
    /// Optional OAuth client ID override for this login. When set, the runtime uses this pre-registered static client instead of dynamic client registration.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub client_id: Option<String>,
    /// Optional override for the OAuth client display name shown on the consent screen. Applies to newly registered dynamic clients only — existing registrations keep the name they were created with. When omitted, the runtime applies a neutral fallback; callers driving interactive auth should pass their own surface-specific label so the consent screen matches the product the user sees.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub client_name: Option<String>,
    /// Optional OAuth client secret override for this login. The runtime treats this as an ephemeral host-owned secret, uses it for this authentication attempt and does not persist it.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub client_secret: Option<String>,
    /// When true, clears any cached OAuth token for the server and runs a full new authorization. Use when the user explicitly wants to switch accounts or believes their session is stuck.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub force_reauth: Option<bool>,
    /// Optional OAuth grant type override for this login. Defaults to the server configuration, or authorization_code when no grant type is specified.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub grant_type: Option<McpOauthLoginGrantType>,
    /// Optional override indicating whether the static OAuth client is public. When false, the runtime treats it as confidential and uses the per-login clientSecret if provided, otherwise retrieving the client secret from the MCP OAuth secret store.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub public_client: Option<bool>,
    /// Name of the remote MCP server to authenticate
    pub server_name: String,
}

/// Extensible [`McpOauthLoginRequest`], including inputs added after it was published.
///
/// Required inputs are [`McpOauthLoginOptions::new`] arguments; optional inputs have fluent setters.
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
pub struct McpOauthLoginOptions {
    #[serde(flatten)]
    legacy: McpOauthLoginRequest,
    #[serde(skip_serializing_if = "Option::is_none")]
    redirect_uri: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    login_id: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    expected_installation_id: Option<String>,
}

impl McpOauthLoginOptions {
    /// Creates options with the required inputs.
    pub fn new(server_name: impl Into<String>) -> Self {
        Self {
            legacy: McpOauthLoginRequest {
                server_name: server_name.into(),
                force_reauth: None,
                client_name: None,
                callback_success_message: None,
                client_id: None,
                client_secret: None,
                public_client: None,
                grant_type: None,
            },
            redirect_uri: None,
            login_id: None,
            expected_installation_id: None,
        }
    }

    /// When true, clears any cached OAuth token for the server and runs a full new authorization. Use when the user explicitly wants to switch accounts or believes their session is stuck.
    pub fn force_reauth(mut self, value: bool) -> Self {
        self.legacy.force_reauth = Some(value);
        self
    }

    /// Optional override for the OAuth client display name shown on the consent screen. Applies to newly registered dynamic clients only — existing registrations keep the name they were created with. When omitted, the runtime applies a neutral fallback; callers driving interactive auth should pass their own surface-specific label so the consent screen matches the product the user sees.
    pub fn client_name(mut self, value: impl Into<String>) -> Self {
        self.legacy.client_name = Some(value.into());
        self
    }

    /// Optional override for the body text shown on the OAuth loopback callback success page. When omitted, the runtime applies a neutral fallback; callers driving interactive auth should pass surface-specific copy telling the user where to return.
    pub fn callback_success_message(mut self, value: impl Into<String>) -> Self {
        self.legacy.callback_success_message = Some(value.into());
        self
    }

    /// Optional OAuth client ID override for this login. When set, the runtime uses this pre-registered static client instead of dynamic client registration.
    pub fn client_id(mut self, value: impl Into<String>) -> Self {
        self.legacy.client_id = Some(value.into());
        self
    }

    /// Optional OAuth client secret override for this login. The runtime treats this as an ephemeral host-owned secret, uses it for this authentication attempt and does not persist it.
    pub fn client_secret(mut self, value: impl Into<String>) -> Self {
        self.legacy.client_secret = Some(value.into());
        self
    }

    /// Optional override indicating whether the static OAuth client is public. When false, the runtime treats it as confidential and uses the per-login clientSecret if provided, otherwise retrieving the client secret from the MCP OAuth secret store.
    pub fn public_client(mut self, value: bool) -> Self {
        self.legacy.public_client = Some(value);
        self
    }

    /// Optional OAuth grant type override for this login. Defaults to the server configuration, or authorization_code when no grant type is specified.
    pub fn grant_type(mut self, value: McpOauthLoginGrantType) -> Self {
        self.legacy.grant_type = Some(value);
        self
    }

    /// Optional externally visible HTTPS redirect URI for a host-managed callback endpoint. When supplied, the runtime still owns discovery, PKCE, token exchange, persistence, and reconnect, but does not bind a loopback listener or terminate HTTPS. The URI must not contain query parameters or a fragment and must be registered for the selected CIMD, DCR, or static OAuth client.
    pub fn redirect_uri(mut self, value: impl Into<String>) -> Self {
        self.redirect_uri = Some(value.into());
        self
    }

    /// Required for owned login. Consumes the exact prepareLogin handle once.
    /// Set forceReauth and display options during preparation, not consumption.
    pub fn login_id(mut self, value: impl Into<String>) -> Self {
        self.login_id = Some(value.into());
        self
    }

    /// Exact owned receipt identity. Owned login never uses an implicit helper session.
    pub fn expected_installation_id(mut self, value: impl Into<String>) -> Self {
        self.expected_installation_id = Some(value.into());
        self
    }
}

/// OAuth authorization URL the caller should open, or empty when cached tokens already authenticated the server.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpOauthLoginResult {
    /// Opaque authorization identifier returned only for a host-managed redirect URI. The runtime also sends it as the OAuth state value, so the callback endpoint can read state and pass it with the full callback URL to session.mcp.oauth.complete.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub authorization_id: Option<String>,
    /// URL the caller should open in a browser to complete OAuth. Omitted when cached tokens were still valid and no browser interaction was needed — the server is already reconnected in that case. For the default loopback flow, the runtime starts its listener before returning. With redirectUri, the host receives the callback and completes it through session.mcp.oauth.complete. The runtime continues the flow in the background and signals completion via session.mcp_server_status_changed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub authorization_url: Option<String>,
    /// Runtime-issued owned flow identity; never a server name or installation operation ID.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub login_id: Option<String>,
    /// Explicit outcome for owned sign-in. Manual callers retain their legacy response shape.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub status: Option<McpOwnedOauthLoginStatus>,
}

/// Effect-free preparation bound to the existing local session, requester and installation, with frozen options.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpOauthPrepareLoginRequest {
    /// Text shown on the loopback callback page after successful authorisation.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub callback_success_message: Option<String>,
    /// Display name used by the incumbent OAuth client-registration flow.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub client_name: Option<String>,
    /// Exact installation identity from owned inventory, never a server-name alias.
    pub expected_installation_id: String,
    /// Request a new authorisation rather than accepting a usable cached grant.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub force_reauth: Option<bool>,
    /// Name recorded by the authoritative owned installation receipt.
    pub server_name: String,
}

/// An inert runtime-issued login handle. Preparation alone performs no activation or OAuth work.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpOauthPrepareLoginResult {
    /// Original expiry, not extended by consumption, retries or cancellation.
    pub expires_at: String,
    /// Retain with the original requester and use for one login or cancellation.
    pub login_id: String,
}

/// Remote MCP server name for a passive OAuth status probe.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpOauthProbeRequest {
    /// Name of the configured remote MCP server to probe.
    pub server_name: String,
}

/// Extensible [`McpOauthProbeRequest`], including inputs added after it was published.
///
/// Required inputs are [`McpOauthProbeOptions::new`] arguments; optional inputs have fluent setters.
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
pub struct McpOauthProbeOptions {
    #[serde(flatten)]
    legacy: McpOauthProbeRequest,
    #[serde(skip_serializing_if = "Option::is_none")]
    expected_installation_id: Option<String>,
}

impl McpOauthProbeOptions {
    /// Creates options with the required inputs.
    pub fn new(server_name: impl Into<String>) -> Self {
        Self {
            legacy: McpOauthProbeRequest {
                server_name: server_name.into(),
            },
            expected_installation_id: None,
        }
    }

    /// Exact owned receipt identity; probing never activates a dormant installation.
    pub fn expected_installation_id(mut self, value: impl Into<String>) -> Self {
        self.expected_installation_id = Some(value.into());
        self
    }
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpOauthProbeResultNoAuthRequired {
    /// HTTP response returned by the server.
    pub http_response: McpOauthHttpResponse,
    /// Probe outcome variant discriminator.
    pub status: McpOauthProbeResultNoAuthRequiredStatus,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpOauthProbeResultAuthenticated {
    /// HTTP response returned by the server.
    pub http_response: McpOauthHttpResponse,
    /// Probe outcome variant discriminator.
    pub status: McpOauthProbeResultAuthenticatedStatus,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpOauthProbeResultNeedsAuth {
    /// HTTP 401 or 403 response returned by the server.
    pub http_response: McpOauthHttpResponse,
    /// Why authentication is needed.
    pub reason: McpOauthProbeNeedsAuthReason,
    /// Probe outcome variant discriminator.
    pub status: McpOauthProbeResultNeedsAuthStatus,
    /// Parsed WWW-Authenticate challenge parameters, when present and parseable.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub www_authenticate_params: Option<McpOauthWWWAuthenticateParams>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpOauthProbeResultFailed {
    /// Human-readable probe failure detail.
    pub error: String,
    /// HTTP response returned by the server, when the probe reached the server and captured the complete response.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub http_response: Option<McpOauthHttpResponse>,
    /// Probe outcome variant discriminator.
    pub status: McpOauthProbeResultFailedStatus,
}

/// Pending MCP OAuth request id to respond to.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpOauthRespondRequest {
    /// OAuth request identifier from the mcp.oauth_required event
    pub request_id: RequestId,
}

/// Indicates whether the pending MCP OAuth response was accepted.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpOauthRespondResult {
    /// Whether the response was accepted. False if the request was unknown, timed out, or already resolved.
    pub success: bool,
}

/// An OAuth client a server configuration pins, without its secret.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct McpOauthStaticClientConfig {
    /// Configured OAuth client identifier.
    pub client_id: String,
    /// Grant type, carried only when the configuration pins the headless `client_credentials` flow. Absent for the default interactive authorization-code flow.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub grant_type: Option<McpServerConfigHttpOauthGrantType>,
    /// Whether the configuration declares a public client. Absent when the configuration does not say, which the flow reads as a public client.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub public_client: Option<bool>,
    /// Configured OAuth scopes joined into one space-delimited scope value. Absent when the configuration specifies no scopes.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub scope: Option<String>,
}

/// A remote MCP server configuration whose pinned OAuth client should be read.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct McpOauthStaticClientConfigRequest {
    /// The server configuration to read. A configuration that pins no client answers nothing.
    pub config: serde_json::Value,
}

/// The OAuth client the configuration pins, when it pins one.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct McpOauthStaticClientConfigResult {
    /// The pinned client. Absent when the configuration pins no non-empty client id, so the flow should register one dynamically.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) client_config: Option<McpOauthStaticClientConfig>,
}

/// A computed MCP install plan. Nothing has been applied: the plan describes what installing would change, and the plan handle is what a later apply operation would consume.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpPlanInstallPlanned {
    /// Discriminator: a plan was computed and nothing was changed
    pub kind: McpPlanInstallPlannedKind,
    /// Protocol version and capabilities the runtime honoured.
    pub negotiated: CatalogNegotiatedContract,
    /// The normalised plan.
    pub plan: McpInstallPlan,
}

/// Plan from a candidate returned by a previous catalog search.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpPlanInstallSourceCandidate {
    /// Single-use candidate handle. Consumed by this call, so a replay of the same handle is rejected.
    pub candidate_handle: String,
    /// Discriminator: plan from a previously returned candidate
    #[serde(deserialize_with = "McpPlanInstallSourceCandidate::deserialize_kind")]
    pub kind: McpPlanInstallSourceCandidateKind,
    /// The runtime- or authority-minted `searchId` returned with the search that produced this candidate. Planning verifies the private correlation and atomically consumes a matching candidate before downstream work, including attempts that subsequently fail or report unavailable. A mismatched search does not consume the candidate. It identifies a search rather than a person and must never be joined with user identity to re-identify anyone.
    pub search_id: String,
}

impl McpPlanInstallSourceCandidate {
    fn deserialize_kind<'de, D>(
        deserializer: D,
    ) -> Result<McpPlanInstallSourceCandidateKind, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let value = String::deserialize(deserializer)?;
        if value != "candidate" {
            return Err(serde::de::Error::unknown_variant(&value, &["candidate"]));
        }
        <McpPlanInstallSourceCandidateKind>::deserialize(serde::de::value::StringDeserializer::<
            D::Error,
        >::new(value))
    }
}

/// An MCP server card to be retrieved from a URL through the runtime's hardened fetch boundary.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpServerCardUrl {
    /// Discriminator: the card is URL-backed, and carries no embedded data
    #[serde(deserialize_with = "McpServerCardUrl::deserialize_kind")]
    pub kind: McpServerCardUrlKind,
    /// Media type the card is expected to conform to.
    pub media_type: McpServerCardMediaType,
    /// Card URL. Retrieved only through the runtime's hardened boundary, with scheme, credential, address-range, redirect, timeout, and response-size controls applied. Never logged.
    pub url: String,
}

impl McpServerCardUrl {
    fn deserialize_kind<'de, D>(deserializer: D) -> Result<McpServerCardUrlKind, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let value = String::deserialize(deserializer)?;
        if value != "url" {
            return Err(serde::de::Error::unknown_variant(&value, &["url"]));
        }
        <McpServerCardUrlKind>::deserialize(serde::de::value::StringDeserializer::<D::Error>::new(
            value,
        ))
    }
}

/// An MCP server card supplied inline as an inert document.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpServerCardEmbedded {
    /// The card document verbatim, treated as inert untrusted bytes. The runtime parses and validates it; the host is not expected to interpret it. Never logged.
    pub data: String,
    /// Discriminator: the card is embedded, and carries no URL
    #[serde(deserialize_with = "McpServerCardEmbedded::deserialize_kind")]
    pub kind: McpServerCardEmbeddedKind,
    /// Media type the card is expected to conform to.
    pub media_type: McpServerCardMediaType,
}

impl McpServerCardEmbedded {
    fn deserialize_kind<'de, D>(deserializer: D) -> Result<McpServerCardEmbeddedKind, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let value = String::deserialize(deserializer)?;
        if value != "embedded" {
            return Err(serde::de::Error::unknown_variant(&value, &["embedded"]));
        }
        <McpServerCardEmbeddedKind>::deserialize(
            serde::de::value::StringDeserializer::<D::Error>::new(value),
        )
    }
}

/// Plan from a card supplied directly by the caller, without a preceding search.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpPlanInstallSourceCard {
    /// The card to plan from: exactly one of a URL or embedded data.
    pub card: McpServerCardReference,
    /// Discriminator: plan from a caller-supplied card
    #[serde(deserialize_with = "McpPlanInstallSourceCard::deserialize_kind")]
    pub kind: McpPlanInstallSourceCardKind,
}

impl McpPlanInstallSourceCard {
    fn deserialize_kind<'de, D>(deserializer: D) -> Result<McpPlanInstallSourceCardKind, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let value = String::deserialize(deserializer)?;
        if value != "card" {
            return Err(serde::de::Error::unknown_variant(&value, &["card"]));
        }
        <McpPlanInstallSourceCardKind>::deserialize(
            serde::de::value::StringDeserializer::<D::Error>::new(value),
        )
    }
}

/// A side-effect-free request for an MCP install plan. Computing a plan never writes configuration, stores a secret, or reloads MCP servers.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpPlanInstallRequest {
    /// Protocol version and capabilities the caller requires.
    pub contract: CatalogClientContract,
    /// Configuration scope the plan targets. Defaults to user scope when omitted.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub scope: Option<McpPlanScope>,
    /// What to plan: either a candidate handle from a previous search, or a card supplied directly.
    pub source: McpPlanInstallSource,
}

/// Extensible [`McpPlanInstallRequest`], including inputs added after it was published.
///
/// Required inputs are [`McpPlanInstallOptions::new`] arguments; optional inputs have fluent setters.
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
pub struct McpPlanInstallOptions {
    #[serde(flatten)]
    legacy: McpPlanInstallRequest,
    #[serde(skip_serializing_if = "Option::is_none")]
    policy_session_id: Option<String>,
}

impl McpPlanInstallOptions {
    /// Creates options with the required inputs.
    pub fn new(contract: CatalogClientContract, source: McpPlanInstallSource) -> Self {
        Self {
            legacy: McpPlanInstallRequest {
                contract,
                source,
                scope: None,
            },
            policy_session_id: None,
        }
    }

    /// Configuration scope the plan targets. Defaults to user scope when omitted.
    pub fn scope(mut self, value: McpPlanScope) -> Self {
        self.legacy.scope = Some(value);
        self
    }

    /// The same existing attached session that owns the original catalogue candidate.
    pub fn policy_session_id(mut self, value: impl Into<String>) -> Self {
        self.policy_session_id = Some(value.into());
        self
    }
}

/// Read-only preparation of one owned removal under fresh selected-session authority.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpPlanUninstallRequest {
    /// Required authenticated bound installation capabilities.
    pub contract: CatalogClientContract,
    /// Exact receipt to inspect, not a server-name guess.
    pub installation_id: String,
    /// Existing selected session on the original connection.
    pub policy_session_id: String,
}

/// Side-effect-free preparation of one original bound remote MCP choice.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpPrepareInstallRequest {
    /// Exact selected alternative from that plan.
    pub choice_id: String,
    /// Required bound catalogue and confirmed remote installation capabilities.
    pub contract: CatalogClientContract,
    /// Declared non-secret values. Non-empty only when the caller requires
    /// `mcp-configured-remote-installation`; omitted values use the card default.
    pub inputs: Vec<McpInstallationInput>,
    /// Original single-use bound plan, never a client-authored configuration.
    pub plan_handle: String,
    /// An existing local session attached to this connection, not permission to attach one.
    pub policy_session_id: String,
    /// One entry per declared secret placeholder of the selected choice. Non-empty
    /// only when the caller requires `mcp-configured-remote-installation`.
    pub secrets: Vec<McpInstallationSecret>,
    /// The trusted host presents this choice alongside the exact secret placeholders.
    pub secret_storage: McpInstallationSecretStorage,
    /// The exact original source, used transiently only after confirmation.
    pub source: McpServerCardReference,
}

/// MCP server, prompt name, and optional string-valued arguments.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpPromptsGetRequest {
    /// String-valued arguments to pass to the prompt
    #[serde(skip_serializing_if = "Option::is_none")]
    pub arguments: Option<HashMap<String, String>>,
    /// The programmatic name of the prompt
    pub prompt_name: String,
    /// Name of the MCP server hosting the prompt
    pub server_name: String,
}

/// Prompt messages returned by the MCP server without sending them to the model.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpPromptsGetResult {
    /// MCP result metadata
    #[serde(rename = "_meta", skip_serializing_if = "Option::is_none")]
    pub meta: Option<HashMap<String, serde_json::Value>>,
    /// Server-provided non-standard result fields
    #[serde(skip_serializing_if = "Option::is_none")]
    pub additional_properties: Option<HashMap<String, serde_json::Value>>,
    /// Description of the prompt
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Ordered prompt messages
    pub messages: Vec<McpPromptMessage>,
}

/// MCP server whose prompts to enumerate.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpPromptsListRequest {
    /// Opaque MCP pagination cursor from a prior `nextCursor` value
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cursor: Option<String>,
    /// Name of the MCP server whose prompts to enumerate
    pub server_name: String,
}

/// One page of prompts advertised by the named MCP server.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpPromptsListResult {
    /// MCP result metadata
    #[serde(rename = "_meta", skip_serializing_if = "Option::is_none")]
    pub meta: Option<HashMap<String, serde_json::Value>>,
    /// Server-provided non-standard result fields
    #[serde(skip_serializing_if = "Option::is_none")]
    pub additional_properties: Option<HashMap<String, serde_json::Value>>,
    /// Opaque cursor for the next page, if the server has more prompts
    #[serde(skip_serializing_if = "Option::is_none")]
    pub next_cursor: Option<String>,
    /// Prompts advertised by the server
    pub prompts: Vec<McpPrompt>,
}

/// Registration parameters for an external MCP client.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct McpRegisterExternalClientRequest {
    /// In-process MCP Client instance. Marked internal: cannot be serialized across the JSON-RPC boundary.
    #[doc(hidden)]
    pub(crate) client: serde_json::Value,
    /// In-process server config (MCPServerConfig) paired with the in-process client/transport. Marked internal alongside its companions.
    #[doc(hidden)]
    pub(crate) config: serde_json::Value,
    /// Logical server name for the external client
    pub server_name: String,
    /// In-process MCP Transport instance. Marked internal: cannot be serialized across the JSON-RPC boundary.
    #[doc(hidden)]
    pub(crate) transport: serde_json::Value,
}

/// Registry search to abandon.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct McpRegistryCancelRequest {
    /// Request ID from `mcp.registry.allocateRequestId` that the search uses.
    pub request_id: i64,
}

/// Whether the cancel reached a live search.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct McpRegistryCancelResult {
    /// True when the cancel stopped a running search. False for unknown or reclaimed IDs, completed or canceled searches, and unused reservations. The cancel releases an unused reservation.
    pub canceled: bool,
}

/// Request id naming a cancellable registry search.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct McpRegistryRequestIdResult {
    /// Request ID for `mcp.registry.search` and `mcp.registry.cancel`. It serves one search. Allocation can reclaim unused IDs at the 1,024-ID reservation limit.
    pub request_id: i64,
}

/// Registry search terms, the credential to search under, and the request id that makes the search cancellable.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct McpRegistrySearchRequest {
    /// The credential the search runs under, carried opaquely. Hosts usually send credential-free `AuthIdentity`; a `token` identity can be resolved only when it embeds a token, while `env` and `gh-cli` identities can use a token embedded in the request first.
    pub auth_info: serde_json::Value,
    /// Maximum number of servers to return.
    pub limit: i64,
    /// Free-text query. Omitted or empty asks the registry for its top servers rather than searching. A value that is not a string is refused.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub query: Option<String>,
    /// Repository used for the policy lookup, as `owner/name`. The policy selects the registry URL and whether the user token goes to the registry. The registry receives this repository only when the policy entry lists it as required context. A value that is not a string is refused.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repository: Option<String>,
    /// Request ID from `mcp.registry.allocateRequestId`. The search refuses unknown, reclaimed, or canceled IDs and IDs that another search or request already uses.
    pub request_id: i64,
}

/// Servers selected from the registry response.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct McpRegistrySearchResult {
    /// The server objects, carried opaquely. The runtime follows pages, keeps the newest entry per server name, cuts the list to `limit`, and sorts an empty-query result by GitHub stars. Each server object remains unchanged because the registry owns that shape.
    pub servers: serde_json::Value,
}

/// In-process MCP reload configuration.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct McpReloadConfig {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub active_git_hub_token: Option<String>,
    /// Server names the CLI enabled for this session via `--enable-mcp-server`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cli_enabled_servers: Option<Vec<String>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub config_filter: Option<serde_json::Value>,
    /// Caller-resolved settings disables, below CLI enables and explicit session disables.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub configured_disabled_servers: Option<Vec<String>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub disabled_servers: Option<Vec<String>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enabled_servers: Option<Vec<String>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub force_restart: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub github_mcp_tool_options: Option<serde_json::Value>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub github_mcp_user_override: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub include_workspace_sources: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mcp3p_enabled: Option<bool>,
    pub mcp_servers: HashMap<String, serde_json::Value>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub secret_store: Option<serde_json::Value>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub use_cached_tool_snapshots: Option<bool>,
}

/// Opaque MCP reload configuration.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct McpReloadWithConfigRequest {
    /// Opaque runtime MCP reload configuration. Marked internal: an in-process runtime shape (reloadMcpServers throws over the wire).
    #[doc(hidden)]
    pub(crate) config: serde_json::Value,
}

/// Indicates whether the auto-managed `github` MCP server was removed (false when nothing to remove).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpRemoveGitHubResult {
    /// True when the auto-managed `github` MCP server was removed; false when no removal happened (e.g. user has explicitly configured a `github` server, or the server was not registered).
    pub removed: bool,
}

/// MCP server whose resources to enumerate.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpResourcesListRequest {
    /// Opaque MCP pagination cursor from a prior `nextCursor` value
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cursor: Option<String>,
    /// Name of the MCP server whose resources to enumerate
    pub server_name: String,
}

/// One page of resources advertised by the named MCP server.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpResourcesListResult {
    /// Opaque cursor for the next page, if the server has more resources
    #[serde(skip_serializing_if = "Option::is_none")]
    pub next_cursor: Option<String>,
    /// Resources advertised by the server (proxied MCP `resources/list`)
    pub resources: Vec<McpResource>,
}

/// MCP server whose resource templates to enumerate.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpResourcesListTemplatesRequest {
    /// Opaque MCP pagination cursor from a prior `nextCursor` value
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cursor: Option<String>,
    /// Name of the MCP server whose resource templates to enumerate
    pub server_name: String,
}

/// One page of resource templates advertised by the named MCP server.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpResourcesListTemplatesResult {
    /// Opaque cursor for the next page, if the server has more resource templates
    #[serde(skip_serializing_if = "Option::is_none")]
    pub next_cursor: Option<String>,
    /// Resource templates advertised by the server (proxied MCP `resources/templates/list`)
    pub resource_templates: Vec<McpResourceTemplate>,
}

/// MCP server and resource URI to fetch.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpResourcesReadRequest {
    /// Name of the MCP server hosting the resource
    pub server_name: String,
    /// Resource URI
    pub uri: String,
}

/// Resource contents returned by the MCP server.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpResourcesReadResult {
    /// Resource contents returned by the server
    pub contents: Vec<McpResourceContent>,
}

/// Server name and optional replacement configuration for an individual MCP server restart. Omit `config` for a config-free restart-by-name of an already-configured server.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpRestartServerRequest {
    /// Replacement MCP server configuration (stdio process or remote HTTP/SSE). Omit to restart the server with its already-registered configuration (config-free restart-by-name).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub config: Option<serde_json::Value>,
    /// Name of the MCP server to restart
    pub server_name: String,
}

/// Extensible [`McpRestartServerRequest`], including inputs added after it was published.
///
/// Required inputs are [`McpRestartServerOptions::new`] arguments; optional inputs have fluent setters.
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
pub struct McpRestartServerOptions {
    #[serde(flatten)]
    legacy: McpRestartServerRequest,
    #[serde(skip_serializing_if = "Option::is_none")]
    expected_installation_id: Option<String>,
}

impl McpRestartServerOptions {
    /// Creates options with the required inputs.
    pub fn new(server_name: impl Into<String>) -> Self {
        Self {
            legacy: McpRestartServerRequest {
                server_name: server_name.into(),
                config: None,
            },
            expected_installation_id: None,
        }
    }

    /// Replacement MCP server configuration (stdio process or remote HTTP/SSE). Omit to restart the server with its already-registered configuration (config-free restart-by-name).
    pub fn config(mut self, value: serde_json::Value) -> Self {
        self.legacy.config = Some(value);
        self
    }

    /// Exact receipt identity for an explicit owned restart; configuration overrides are refused.
    pub fn expected_installation_id(mut self, value: impl Into<String>) -> Self {
        self.expected_installation_id = Some(value.into());
        self
    }
}

/// Per-field MCP telemetry-obfuscation policy.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpSafeForTelemetryFields {
    /// Whether MCP tool input names may be included in telemetry without obfuscation.
    pub inputs_names: bool,
    /// Whether the MCP tool name may be included in telemetry without obfuscation.
    pub name: bool,
}

/// Outcome of an MCP sampling execution: success result, failure error, or cancellation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpSamplingExecutionResult {
    /// Outcome of the sampling inference. 'success' produced a response; 'failure' encountered an error (including agent-side rejection by content filter or criteria); 'cancelled' the caller cancelled this execution via cancelSamplingExecution.
    pub action: McpSamplingExecutionAction,
    /// Error description, present when action='failure'.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// MCP CreateMessageResult payload (with optional 'tools' extension), present when action='success'. Treated as opaque at the schema layer; consumers should construct/consume it per the MCP CreateMessageResult shape.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub result: Option<McpExecuteSamplingResult>,
}

/// Authentication settings with optional redirect port configuration.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpServerAuthConfigRedirectPort {
    /// Fixed port for the OAuth redirect callback server.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub redirect_port: Option<i32>,
}

/// Remote MCP server configuration accessed over HTTP or SSE.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpServerConfigHttp {
    /// Set to `true` to use defaults, or provide an object with additional auth or OIDC settings.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auth: Option<serde_json::Value>,
    /// Configuration warnings recorded while loading the server.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub config_warnings: Option<Vec<String>>,
    /// Controls if tools provided by this server can be loaded on demand via tool search (auto) or always included in the initial tool list (never)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub defer_tools: Option<McpServerConfigDeferTools>,
    /// Whether secret masking is disabled for calls to this server.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub disable_secret_masking: Option<bool>,
    /// Set to true to disable persisted MCP tool snapshots for this server. Live tool discovery is unaffected.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub disable_tool_cache: Option<bool>,
    /// Optional human-readable server name.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub display_name: Option<String>,
    /// Event types this server receives as Copilot notifications.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub events: Option<Vec<String>>,
    /// Tool names excluded after the include filter is applied.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub exclude_tools: Option<Vec<String>>,
    /// Content filtering mode to apply to all tools, or a map of tool name to content filtering mode.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub filter_mapping: Option<serde_json::Value>,
    /// HTTP headers to include in requests to the remote MCP server.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub headers: Option<HashMap<String, String>>,
    /// Dynamic-header refresh cache lifetime in milliseconds.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub headers_refresh_ttl_ms: Option<i64>,
    /// Whether this server is a built-in fallback used when the user has not configured their own server.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub is_default_server: Option<bool>,
    /// Copilot notification types this server may send to the host.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub notifications: Option<Vec<String>>,
    /// OAuth client ID for a pre-registered remote MCP OAuth client.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub oauth_client_id: Option<String>,
    /// OAuth grant type to use when authenticating to the remote MCP server.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub oauth_grant_type: Option<McpServerConfigHttpOauthGrantType>,
    /// Whether the configured OAuth client is public and does not require a client secret.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub oauth_public_client: Option<bool>,
    /// Non-empty array of valid RFC 6749 scope-token strings to request for the statically configured OAuth client when the server challenge omits scope or provides an empty scope. Requires a non-empty oauthClientId. These scopes take precedence over protected-resource metadata.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub oauth_scopes: Option<Vec<String>>,
    /// Set to `true` to use defaults, or provide an object with additional auth or OIDC settings.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub oidc: Option<serde_json::Value>,
    /// Telemetry-obfuscation policy for this server's tools.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub safe_for_telemetry: Option<serde_json::Value>,
    /// Milliseconds this server may spend connecting before the CLI warns that it is taking longer than expected. Presentation only: it does not change how long the connection is allowed to take.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub slow_connection_threshold_ms: Option<i64>,
    /// The origin of this server configuration.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source: Option<McpServerSource>,
    /// Source file path recorded while loading the config.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source_path: Option<String>,
    /// Plugin that provided this server.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source_plugin: Option<String>,
    /// Whether the providing plugin uses the Open Plugin Spec.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source_plugin_spec: Option<bool>,
    /// Version of the plugin that provided this server.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source_plugin_version: Option<String>,
    /// Timeout in milliseconds for tool discovery and tool calls.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub timeout: Option<i64>,
    /// Tools to include. Defaults to all tools if not specified.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tools: Option<Vec<String>>,
    /// Remote transport type. Defaults to "http" when omitted.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub r#type: Option<McpServerConfigHttpType>,
    /// URL of the remote MCP server endpoint.
    pub url: String,
}

/// In-process MCP server configuration used by embedded SDK clients.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct McpServerConfigMemory {
    /// Configuration warnings recorded while loading the server.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub config_warnings: Option<Vec<String>>,
    /// Controls whether tools can be loaded on demand.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub defer_tools: Option<McpServerConfigDeferTools>,
    /// Whether secret masking is disabled for calls to this server.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub disable_secret_masking: Option<bool>,
    /// Whether persisted tool snapshots are disabled.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub disable_tool_cache: Option<bool>,
    /// Optional human-readable server name.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub display_name: Option<String>,
    /// Event types this server receives as Copilot notifications.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub events: Option<Vec<String>>,
    /// Tool names excluded after the include filter is applied.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub exclude_tools: Option<Vec<String>>,
    /// Content filtering mode to apply to this server's tools.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub filter_mapping: Option<serde_json::Value>,
    /// Whether this server is a built-in fallback.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub is_default_server: Option<bool>,
    /// Copilot notification types this server may send to the host.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub notifications: Option<Vec<String>>,
    /// Set to `true` to use default OIDC settings.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub oidc: Option<serde_json::Value>,
    /// Telemetry-obfuscation policy for this server's tools.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub safe_for_telemetry: Option<serde_json::Value>,
    /// In-process MCP server instance. This value cannot cross a JSON-RPC boundary.
    #[doc(hidden)]
    pub(crate) server_instance: serde_json::Value,
    /// Milliseconds this server may spend connecting before the CLI warns that it is taking longer than expected. Presentation only: it does not change how long the connection is allowed to take.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub slow_connection_threshold_ms: Option<i64>,
    /// The origin of this server configuration.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source: Option<McpServerSource>,
    /// Source file path recorded while loading the config.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source_path: Option<String>,
    /// Plugin that provided this server.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source_plugin: Option<String>,
    /// Whether the providing plugin uses the Open Plugin Spec.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source_plugin_spec: Option<bool>,
    /// Version of the plugin that provided this server.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source_plugin_version: Option<String>,
    /// Timeout in milliseconds for tool discovery and tool calls.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub timeout: Option<i64>,
    /// Tools to include. Defaults to all tools if not specified.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tools: Option<Vec<String>>,
    #[doc(hidden)]
    pub(crate) r#type: McpServerConfigMemoryType,
}

/// Stdio MCP server configuration launched as a child process.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpServerConfigStdio {
    /// Command-line arguments passed to the Stdio MCP server process.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub args: Option<Vec<String>>,
    /// Set to `true` to use defaults, or provide an object with additional auth or OIDC settings.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auth: Option<serde_json::Value>,
    /// Executable command used to start the Stdio MCP server process.
    pub command: String,
    /// Configuration warnings recorded while loading the server.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub config_warnings: Option<Vec<String>>,
    /// Working directory for the Stdio MCP server process.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cwd: Option<String>,
    /// Controls if tools provided by this server can be loaded on demand via tool search (auto) or always included in the initial tool list (never)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub defer_tools: Option<McpServerConfigDeferTools>,
    /// Whether secret masking is disabled for calls to this server.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub disable_secret_masking: Option<bool>,
    /// Set to true to disable persisted MCP tool snapshots for this server. Live tool discovery is unaffected.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub disable_tool_cache: Option<bool>,
    /// Optional human-readable server name.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub display_name: Option<String>,
    /// Environment variables to pass to the Stdio MCP server process.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub env: Option<HashMap<String, String>>,
    /// Event types this server receives as Copilot notifications.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub events: Option<Vec<String>>,
    /// Tool names excluded after the include filter is applied.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub exclude_tools: Option<Vec<String>>,
    /// Content filtering mode to apply to all tools, or a map of tool name to content filtering mode.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub filter_mapping: Option<serde_json::Value>,
    /// Whether this server is a built-in fallback used when the user has not configured their own server.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub is_default_server: Option<bool>,
    /// Copilot notification types this server may send to the host.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub notifications: Option<Vec<String>>,
    /// Set to `true` to use defaults, or provide an object with additional auth or OIDC settings.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub oidc: Option<serde_json::Value>,
    /// Telemetry-obfuscation policy for this server's tools.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub safe_for_telemetry: Option<serde_json::Value>,
    /// Milliseconds this server may spend connecting before the CLI warns that it is taking longer than expected. Presentation only: it does not change how long the connection is allowed to take.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub slow_connection_threshold_ms: Option<i64>,
    /// The origin of this server configuration.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source: Option<McpServerSource>,
    /// Source file path recorded while loading the config.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source_path: Option<String>,
    /// Plugin that provided this server.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source_plugin: Option<String>,
    /// Whether the providing plugin uses the Open Plugin Spec.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source_plugin_spec: Option<bool>,
    /// Version of the plugin that provided this server.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source_plugin_version: Option<String>,
    /// Timeout in milliseconds for tool discovery and tool calls.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub timeout: Option<i64>,
    /// Tools to include. Defaults to all tools if not specified.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tools: Option<Vec<String>>,
    /// Local transport type. Defaults to stdio when omitted.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub r#type: Option<McpServerConfigStdioType>,
}

/// MCP servers configured for the session, with their connection status and host-level state.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpServerList {
    /// Host-level state, omitted when no MCP host is initialized.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub host: Option<McpHostState>,
    /// Configured MCP servers
    pub servers: Vec<McpServer>,
}

/// Mode controlling how MCP server env values are resolved (`direct` or `indirect`).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpSetEnvValueModeParams {
    /// How environment-variable values supplied to MCP servers are resolved. "direct" passes literal string values; "indirect" treats values as references (e.g. names of environment variables on the host) that the runtime resolves before launch. Defaults to the runtime's startup mode; clients that intentionally launch MCP servers with literal values (e.g. CLI prompt mode and ACP) set this to "direct".
    pub mode: McpSetEnvValueModeDetails,
}

/// Env-value mode recorded on the session after the update.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpSetEnvValueModeResult {
    /// Mode recorded on the session after the update
    pub mode: McpSetEnvValueModeDetails,
}

/// The session tool filters that decide whether the session still has a shell to run `gh` with.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct McpShouldExcludeGitHubToolsRequest {
    /// The session's tool allowlist, when it set one. Omitted means the session constrains nothing this way.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub available_tools: Option<Vec<String>>,
    /// The session's tool denylist, when it set one. Omitted means the session constrains nothing this way.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub excluded_tools: Option<Vec<String>>,
    /// How the allowlist and denylist combine when both are set. Omitted means the default every session gets, so a caller that never chose a precedence is answered as its sessions behave.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_filter_precedence: Option<OptionsUpdateToolFilterPrecedence>,
}

/// Whether the gh-replaceable GitHub MCP tools may be clipped for the session described by the request.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct McpShouldExcludeGitHubToolsResult {
    /// True only when both halves hold: the session's filters still reach the platform shell tool, and the host actually has the `gh` those tools would be replaced by. Feed it straight back as the `excludeGhReplaceableTools` build option.
    pub exclude_gh_replaceable_tools: bool,
}

/// Server name and optional configuration for an individual MCP server start. Omit `config` for a config-free start-by-name of an already-configured server.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpStartServerRequest {
    /// MCP server configuration (stdio process or remote HTTP/SSE). Omit to start the server with its already-registered configuration (config-free start-by-name).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub config: Option<serde_json::Value>,
    /// Name of the MCP server to start
    pub server_name: String,
}

/// Extensible [`McpStartServerRequest`], including inputs added after it was published.
///
/// Required inputs are [`McpStartServerOptions::new`] arguments; optional inputs have fluent setters.
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
pub struct McpStartServerOptions {
    #[serde(flatten)]
    legacy: McpStartServerRequest,
    #[serde(skip_serializing_if = "Option::is_none")]
    expected_installation_id: Option<String>,
}

impl McpStartServerOptions {
    /// Creates options with the required inputs.
    pub fn new(server_name: impl Into<String>) -> Self {
        Self {
            legacy: McpStartServerRequest {
                server_name: server_name.into(),
                config: None,
            },
            expected_installation_id: None,
        }
    }

    /// MCP server configuration (stdio process or remote HTTP/SSE). Omit to start the server with its already-registered configuration (config-free start-by-name).
    pub fn config(mut self, value: serde_json::Value) -> Self {
        self.legacy.config = Some(value);
        self
    }

    /// Exact receipt identity for explicit owned activation in this session.
    pub fn expected_installation_id(mut self, value: impl Into<String>) -> Self {
        self.expected_installation_id = Some(value.into());
        self
    }
}

/// MCP server startup filtering result.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpStartServersResult {
    /// Non-default servers allowed by policy
    #[serde(skip_serializing_if = "Option::is_none")]
    pub allowed_servers: Option<Vec<McpAllowedServer>>,
    /// Servers whose connection attempt failed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub failed_servers: Option<Vec<McpFailedServer>>,
    /// Servers filtered out before startup
    pub filtered_servers: Vec<McpFilteredServer>,
}

/// Server name for an individual MCP server stop.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpStopServerRequest {
    /// Name of the MCP server to stop
    pub server_name: String,
}

/// Extensible [`McpStopServerRequest`], including inputs added after it was published.
///
/// Required inputs are [`McpStopServerOptions::new`] arguments; optional inputs have fluent setters.
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
pub struct McpStopServerOptions {
    #[serde(flatten)]
    legacy: McpStopServerRequest,
    #[serde(skip_serializing_if = "Option::is_none")]
    expected_installation_id: Option<String>,
}

impl McpStopServerOptions {
    /// Creates options with the required inputs.
    pub fn new(server_name: impl Into<String>) -> Self {
        Self {
            legacy: McpStopServerRequest {
                server_name: server_name.into(),
            },
            expected_installation_id: None,
        }
    }

    /// Exact owned receipt identity. Stop also forgets this session's durable activation.
    pub fn expected_installation_id(mut self, value: impl Into<String>) -> Self {
        self.expected_installation_id = Some(value.into());
        self
    }
}

/// Server name identifying the external client to remove.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct McpUnregisterExternalClientRequest {
    /// Server name of the external client to unregister
    pub server_name: String,
}

/// Applies exactly one retained verified Agent Plugin installation plan.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginApplyInstallRequest {
    /// Required authenticated bound catalogue and agent-plugin-confirmed-installation capabilities.
    pub contract: CatalogClientContract,
    /// Opaque original plan, consumed once.
    pub plan_handle: String,
    /// Same existing selected session as planning.
    pub policy_session_id: String,
}

/// A verified Agent Plugin installation plan was prepared.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginInstallationManagementOutcomeInstallPlanned {
    /// Agent Plugin installation management outcome discriminator.
    pub kind: PluginInstallationManagementOutcomeInstallPlannedKind,
    /// Prepared install plan.
    pub plan: PluginInstallPlan,
}

/// The request was refused without preparing a plan.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginInstallationManagementOutcomeRefused {
    /// Agent Plugin installation management outcome discriminator.
    pub kind: PluginInstallationManagementOutcomeRefusedKind,
    /// Bounded refusal reason.
    pub reason: PluginInstallationFailureReason,
    /// Whole seconds GitHub asked the caller to wait, present only with source-rate-limited.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub retry_after_seconds: Option<i64>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginInstallationManagementResultOutcome {
    /// Discriminator: handled management outcome.
    pub kind: PluginInstallationManagementResultOutcomeKind,
    /// Capabilities honoured for this request.
    pub negotiated: CatalogNegotiatedContract,
    /// Observed management outcome.
    pub outcome: PluginInstallationManagementOutcome,
}

/// A plugin registered from a verified materialisation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginInstallationSummary {
    /// Catalogue identity retained at install time.
    pub catalogue: InstallationCatalogueIdentity,
    /// Whether the plugin is enabled for new sessions. Always false after installation.
    pub enabled: bool,
    /// ISO 8601 wall-clock installation time.
    pub installed_at: String,
    /// Installed plugin name, as plugins.list, plugins.enable and plugins.uninstall identify it.
    pub name: String,
    /// Operation that installed the plugin.
    pub operation_id: String,
    /// Exact retained verified source identity.
    pub source: PluginInstallationSource,
    /// Install location without an absolute host path.
    pub target: PluginInstallationLocation,
}

/// The verified plugin was registered disabled.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginInstallationOutcomeInstalled {
    /// Installed plugin summary.
    pub installation: PluginInstallationSummary,
    /// Terminal Agent Plugin installation outcome discriminator.
    pub kind: PluginInstallationOutcomeInstalledKind,
}

/// The user declined the confirmation request.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginInstallationOutcomeDeclined {
    /// Terminal Agent Plugin installation outcome discriminator.
    pub kind: PluginInstallationOutcomeDeclinedKind,
    /// Original operation identity.
    pub operation_id: String,
}

/// The confirmation was cancelled before any change.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginInstallationOutcomeCancelled {
    /// Terminal Agent Plugin installation outcome discriminator.
    pub kind: PluginInstallationOutcomeCancelledKind,
    /// Original operation identity.
    pub operation_id: String,
}

/// The operation was refused and nothing was installed.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginInstallationOutcomeRefused {
    /// Terminal Agent Plugin installation outcome discriminator.
    pub kind: PluginInstallationOutcomeRefusedKind,
    /// Present once an operation has been allocated.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub operation_id: Option<String>,
    /// Bounded refusal reason.
    pub reason: PluginInstallationFailureReason,
    /// Whole seconds GitHub asked the caller to wait, present only with source-rate-limited.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub retry_after_seconds: Option<i64>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginInstallationResultOutcome {
    /// Discriminator: handled operation outcome.
    pub kind: PluginInstallationResultOutcomeKind,
    /// Capabilities honoured for this request.
    pub negotiated: CatalogNegotiatedContract,
    /// Terminal operation outcome.
    pub outcome: PluginInstallationOutcome,
}

/// Safe verified Agent Plugin review fields. No raw credential, candidate handle or plan handle.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginInstallationReview {
    /// Exact reviewed Agent Plugin installation action.
    pub action: PluginInstallationReviewAction,
    /// Catalogue identity retained from the bound candidate, or verified data for a reference.
    pub catalogue: InstallationCatalogueIdentity,
    /// Manifest description when present.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Reviewed files and digests.
    pub files: Vec<PluginInstallationFileReview>,
    /// Installing never grants immediate use; the plugin is registered disabled.
    pub installs_disabled: bool,
    /// Complete verified manifest content. Planning refuses with review-too-large
    /// when this exceeds 262144 UTF-8 bytes; it is never truncated.
    pub manifest_content: String,
    /// Relative path of the verified plugin manifest.
    pub manifest_path: String,
    /// MCP servers the verified files declare, exactly as an install of these files loads
    /// them. Omitted when the plugin declares none.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mcp_servers: Option<Vec<PluginInstallationMcpServerReview>>,
    /// Installed plugin name, derived from the verified manifest exactly as a direct install derives it.
    pub name: String,
    /// Exact verified source identity.
    pub source: PluginInstallationSource,
    /// Exact install location without an absolute host path.
    pub target: PluginInstallationLocation,
    /// Total reviewed payload size in bytes.
    pub total_bytes: i64,
    /// Manifest version when present.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub version: Option<String>,
}

/// Result of installing a plugin.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginInstallResult {
    /// Set when the install path is deprecated (e.g. direct repo / URL / local installs). Callers should surface this to end users.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub deprecation_warning: Option<String>,
    /// The newly installed plugin's metadata
    pub plugin: InstalledPluginInfo,
    /// Optional post-install message provided by the plugin (e.g. setup instructions)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub post_install_message: Option<String>,
    /// Number of skills discovered and installed from the plugin
    pub skills_installed: i64,
    /// Where the completed plugin tree was staged before atomic promotion
    #[serde(skip_serializing_if = "Option::is_none")]
    pub staging_mode: Option<PluginInstallStagingMode>,
}

/// Plugins installed for the session, with their enabled state and version metadata.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginList {
    /// Installed plugins
    pub plugins: Vec<Plugin>,
}

/// Plugins installed in user/global state.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginListResult {
    /// Installed plugins
    pub plugins: Vec<InstalledPluginInfo>,
}

/// Side-effect-free planning of one Agent Plugin search candidate through its Agent Finder materialisation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginPlanInstallRequest {
    /// Fresh single-use Agent Plugin candidate handle returned by a bound catalogue search.
    pub candidate_handle: String,
    /// Required authenticated bound catalogue and agent-plugin-confirmed-installation capabilities.
    pub contract: CatalogClientContract,
    /// Existing local session attached to this connection.
    pub policy_session_id: String,
}

/// Trusted built-in plugin directories to use for this runtime process.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginsBuiltinSetRequest {
    /// Complete replacement set of trusted built-in plugin directories. Every entry must be an absolute local filesystem path no longer than 4096 characters.
    pub paths: Vec<String>,
}

/// Plugin names (or specs) to disable, plus the optional working directory the repository-controlled guard is evaluated against.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginsDisableRequest {
    /// Plugin names or "plugin@marketplace" specs to disable. Unknown names are ignored. Non-marketplace direct installs cannot be disabled via this API; uninstall them instead. Plugin-owned MCP servers are stopped in active sessions immediately; other plugin contributions remain available until each session reloads plugins.
    pub names: Vec<String>,
    /// Working directory whose repository `enabledPlugins` overlay decides whether this mutation is repository-controlled. Hosts that serve sessions across several repositories (the SDK server) should pass the session's directory; otherwise the guard is evaluated against the server process's own working directory, which may belong to a different repository. Defaults to the server's current working directory.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub working_directory: Option<String>,
}

/// Plugin names (or specs) to enable, plus the optional working directory the repository-controlled guard is evaluated against.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginsEnableRequest {
    /// Plugin names or "plugin@marketplace" specs to enable. Unknown names are ignored. Non-marketplace direct installs are always enabled and cannot be toggled via this API.
    pub names: Vec<String>,
    /// Working directory whose repository `enabledPlugins` overlay decides whether this mutation is repository-controlled. Hosts that serve sessions across several repositories (the SDK server) should pass the session's directory; otherwise the guard is evaluated against the server process's own working directory, which may belong to a different repository. Defaults to the server's current working directory.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub working_directory: Option<String>,
}

/// Plugin source and optional working directory for relative-path resolution.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginsInstallRequest {
    /// Plugin install spec. Accepts the same forms as the CLI: "plugin@marketplace" (marketplace install), "owner/repo" or "owner/repo:subpath" (GitHub direct), an http/https/ssh URL, or a local path. Direct (non-marketplace) installs are deprecated and will produce a deprecationWarning in the result.
    pub source: String,
    /// Working directory used to resolve relative local paths in `source`. Defaults to the server's current working directory.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub working_directory: Option<String>,
}

/// Marketplace source and optional working directory for relative-path resolution.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginsMarketplacesAddRequest {
    /// Marketplace source. Accepts the same forms as the CLI: "owner/repo" or "owner/repo#ref" (GitHub), an http/https/ssh URL (optionally with #ref), a git scp-style URL (user@host:path), or a local path. The marketplace's own name (from its manifest) is used as the registration key.
    pub source: String,
    /// Working directory used to resolve relative local paths in `source`. Defaults to the server's current working directory.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub working_directory: Option<String>,
}

/// Name of the marketplace whose plugin catalog to fetch.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginsMarketplacesBrowseRequest {
    /// Marketplace name to browse
    pub name: String,
}

/// Optional marketplace name; omit to refresh all.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginsMarketplacesRefreshRequest {
    /// Marketplace name to refresh. When omitted, every registered marketplace is refreshed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
}

/// Name of the marketplace to remove and an optional force flag.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginsMarketplacesRemoveRequest {
    /// When true, also uninstall every plugin sourced from this marketplace. When false (default), removal is a no-op if any plugin from this marketplace is installed and the dependent plugin names are returned in the result.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub force: Option<bool>,
    /// Marketplace name to remove
    pub name: String,
}

/// Optional flags controlling which side effects the reload performs.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginsReloadRequest {
    /// When true, skip repo-level hooks during the hook reload. Use before folder trust is confirmed; load them post-trust via `sessions.loadDeferredRepoHooks`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub defer_repo_hooks: Option<bool>,
    /// Re-run custom-agent discovery after refreshing plugins. Defaults to true.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reload_custom_agents: Option<bool>,
    /// Re-discover and relaunch subprocess extensions (including plugin-shipped extensions) after refreshing plugins. Defaults to true. Has no effect when the session has no active extension controller (e.g. extensions were not requested for the session).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reload_extensions: Option<bool>,
    /// Re-load user, plugin, and (subject to `deferRepoHooks`) repo hooks. Defaults to true. Has no effect when the host has not registered a hook reloader (e.g. remote sessions).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reload_hooks: Option<bool>,
    /// Reload MCP server connections after refreshing plugins. Defaults to true.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reload_mcp: Option<bool>,
}

/// Name (or spec) of the plugin to uninstall.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginsUninstallRequest {
    /// Stable source identity for a direct (non-marketplace) install. Disambiguates uninstall when multiple installed plugins share the same name.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub direct_source_id: Option<String>,
    /// Plugin name or "plugin@marketplace" spec to uninstall. When ambiguous, prefer the fully-qualified spec.
    pub name: String,
}

/// Name (or spec) of the plugin to update.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginsUpdateRequest {
    /// Plugin name or "plugin@marketplace" spec to update.
    pub name: String,
}

/// Per-plugin result from updating all plugins, with versions, skills installed, success flag, and optional error.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginUpdateAllEntry {
    /// Error message (failure only)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Marketplace the plugin came from. Empty string ("") for direct installs.
    pub marketplace: String,
    /// Plugin name that was updated
    pub name: String,
    /// Version after the update, when available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub new_version: Option<String>,
    /// Previously installed version, when available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub previous_version: Option<String>,
    /// Number of skills installed after the update (success only)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub skills_installed: Option<i64>,
    /// Whether the update succeeded for this plugin
    pub success: bool,
}

/// Result of updating all installed plugins.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginUpdateAllResult {
    /// Per-plugin update results in deterministic order.
    pub results: Vec<PluginUpdateAllEntry>,
}

/// Result of updating a single plugin.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginUpdateResult {
    /// Version after the update, when reported by the plugin manifest
    #[serde(skip_serializing_if = "Option::is_none")]
    pub new_version: Option<String>,
    /// Version that was previously installed, when available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub previous_version: Option<String>,
    /// Number of skills discovered and installed after the update
    pub skills_installed: i64,
}

/// Applies exactly one retained verified Skill installation plan.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillApplyInstallRequest {
    /// Required authenticated bound catalogue and Skill installation capabilities.
    pub contract: CatalogClientContract,
    /// Opaque original plan, consumed once.
    pub plan_handle: String,
    /// Same existing selected session as planning.
    pub policy_session_id: String,
}

/// One-use application of the exact retained Skill removal plan.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillApplyUninstallRequest {
    /// Required authenticated bound installation contract.
    pub contract: CatalogClientContract,
    /// Opaque original removal plan, consumed once.
    pub plan_handle: String,
    /// Same existing selected session as removal preparation.
    pub policy_session_id: String,
}

/// Canonical directory where skills can be discovered or created, with scope, preference, and optional project path.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillDiscoveryPath {
    /// Absolute path of the create/discovery target (may not exist on disk yet)
    pub path: String,
    /// Whether this is the canonical directory to create a new skill in its tier. At most one entry per tier is preferred; the `personal-agents` and `custom` scopes are never preferred.
    pub preferred_for_creation: bool,
    /// The input project path this directory was derived from (only for project scope)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub project_path: Option<String>,
    /// Which tier this directory belongs to
    pub scope: SkillDiscoveryScope,
}

/// Canonical locations where skills can be created so the runtime will recognize them.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillDiscoveryPathList {
    /// Canonical skill create/discovery directories, in priority order
    pub paths: Vec<SkillDiscoveryPath>,
}

/// Already-confirmed durable work must be reconciled before new mutations or inventory.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillInstallationManagementOutcomeRecoveryRequired {
    /// Skill installation management outcome discriminator.
    pub kind: SkillInstallationManagementOutcomeRecoveryRequiredKind,
}

/// A verified Skill installation plan was prepared.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillInstallationManagementOutcomeInstallPlanned {
    /// Skill installation management outcome discriminator.
    pub kind: SkillInstallationManagementOutcomeInstallPlannedKind,
    /// Prepared install plan.
    pub plan: SkillInstallPlan,
}

/// Owned Skill installations were listed.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillInstallationManagementOutcomeListed {
    /// Owned Skill installation summaries.
    pub installations: Vec<SkillInstallationSummary>,
    /// Skill installation management outcome discriminator.
    pub kind: SkillInstallationManagementOutcomeListedKind,
}

/// Recovery completed and inventory was inspected.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillInstallationManagementOutcomeRecovered {
    /// Owned Skill installation summaries after recovery.
    pub installations: Vec<SkillInstallationSummary>,
    /// Skill installation management outcome discriminator.
    pub kind: SkillInstallationManagementOutcomeRecoveredKind,
}

/// Interrupted work was safely compensated and the pending marker was cleared.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillInstallationManagementOutcomeRolledBack {
    /// Skill installation management outcome discriminator.
    pub kind: SkillInstallationManagementOutcomeRolledBackKind,
    /// Original operation identity.
    #[serde(rename = "operation_id")]
    pub operation_id: String,
    /// Cause of the compensation.
    pub reason: SkillInstallationFailureReason,
}

/// A computed Skill uninstall plan. Nothing has been removed.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillUninstallPlan {
    /// Original wall-clock expiry as an ISO 8601 timestamp.
    pub expires_at: String,
    /// Owned Skill installation being removed.
    pub installation: SkillInstallationSummary,
    /// Original removal operation identifier returned before confirmation.
    pub operation_id: String,
    /// One-use uninstall plan handle.
    pub plan_handle: String,
    /// Safe review to present before applying removal.
    pub review: SkillInstallationReview,
}

/// An owned Skill uninstall plan was prepared.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillInstallationManagementOutcomeUninstallPlanned {
    /// Skill installation management outcome discriminator.
    pub kind: SkillInstallationManagementOutcomeUninstallPlannedKind,
    /// Prepared uninstall plan.
    pub plan: SkillUninstallPlan,
}

/// Original operation progress discriminator.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillInstallationOperationStatusPreparing {
    /// Whether cancellation has been requested.
    pub cancellation_requested: bool,
    /// Original runtime-issued operation identity.
    pub operation_id: String,
    pub phase: SkillInstallationOperationStatusPreparingPhase,
}

/// Original operation progress discriminator.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillInstallationOperationStatusPrepared {
    /// Whether cancellation has been requested.
    pub cancellation_requested: bool,
    /// Original runtime-issued operation identity.
    pub operation_id: String,
    pub phase: SkillInstallationOperationStatusPreparedPhase,
}

/// Original operation progress discriminator.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillInstallationOperationStatusAwaitingConfirmation {
    /// Whether cancellation has been requested.
    pub cancellation_requested: bool,
    /// Original runtime-issued operation identity.
    pub operation_id: String,
    pub phase: SkillInstallationOperationStatusAwaitingConfirmationPhase,
}

/// Original operation progress discriminator.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillInstallationOperationStatusRevalidating {
    /// Whether cancellation has been requested.
    pub cancellation_requested: bool,
    /// Original runtime-issued operation identity.
    pub operation_id: String,
    pub phase: SkillInstallationOperationStatusRevalidatingPhase,
}

/// Original operation progress discriminator.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillInstallationOperationStatusApplying {
    /// Whether cancellation has been requested; already-started effects require recovery.
    pub cancellation_requested: bool,
    /// Original runtime-issued operation identity.
    pub operation_id: String,
    pub phase: SkillInstallationOperationStatusApplyingPhase,
}

/// The verified Skill was installed disabled.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillInstallationOutcomeInstalled {
    /// Durable installed Skill summary.
    pub installation: SkillInstallationSummary,
    /// Terminal Skill installation outcome discriminator.
    pub kind: SkillInstallationOutcomeInstalledKind,
}

/// The owned Skill was removed.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillInstallationOutcomeUninstalled {
    /// Removed installation identity.
    pub installation_id: String,
    /// Terminal Skill installation outcome discriminator.
    pub kind: SkillInstallationOutcomeUninstalledKind,
    /// Original removal operation identity.
    pub operation_id: String,
}

/// The durable transaction was aborted or fully compensated.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillInstallationOutcomeRolledBack {
    /// Terminal Skill installation outcome discriminator.
    pub kind: SkillInstallationOutcomeRolledBackKind,
    /// Original operation identity.
    pub operation_id: String,
    /// Cause of the fully aborted or compensated operation.
    pub reason: SkillInstallationFailureReason,
}

/// A write may have completed. Recover and inspect durable state before retrying.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillInstallationOutcomeRecoveryRequired {
    /// Terminal Skill installation outcome discriminator.
    pub kind: SkillInstallationOutcomeRecoveryRequiredKind,
    /// Operation whose durable result must be recovered and inspected.
    pub operation_id: String,
}

/// The user declined the confirmation request.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillInstallationOutcomeDeclined {
    /// Terminal Skill installation outcome discriminator.
    pub kind: SkillInstallationOutcomeDeclinedKind,
    /// Original operation identity.
    pub operation_id: String,
}

/// The operation was cancelled before a terminal mutation.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillInstallationOutcomeCancelled {
    /// Terminal Skill installation outcome discriminator.
    pub kind: SkillInstallationOutcomeCancelledKind,
    /// Original operation identity.
    pub operation_id: String,
}

/// The operation was refused without applying changes.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillInstallationOutcomeRefused {
    /// Terminal Skill installation outcome discriminator.
    pub kind: SkillInstallationOutcomeRefusedKind,
    /// Present once an operation has been allocated.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub operation_id: Option<String>,
    /// Bounded refusal reason.
    pub reason: SkillInstallationFailureReason,
    /// Whole seconds GitHub asked the caller to wait, present only with source-rate-limited.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub retry_after_seconds: Option<i64>,
}

/// Original operation progress discriminator.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillInstallationOperationStatusCompleted {
    /// Whether cancellation was requested before the terminal result.
    pub cancellation_requested: bool,
    /// Original runtime-issued operation identity.
    pub operation_id: String,
    /// Immutable terminal receipt.
    pub outcome: SkillInstallationOutcome,
    pub phase: SkillInstallationOperationStatusCompletedPhase,
}

/// Original-connection operation snapshot.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillInstallationManagementOutcomeOperation {
    /// Skill installation management outcome discriminator.
    pub kind: SkillInstallationManagementOutcomeOperationKind,
    /// Operation status.
    pub operation: SkillInstallationOperationStatus,
}

/// Enablement changed and the selected session was reconciled.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillInstallationManagementOutcomeEnabledChanged {
    /// Safe reload or reconciliation diagnostics.
    pub diagnostics: Vec<String>,
    /// Updated installation summary.
    pub installation: SkillInstallationSummary,
    /// Skill installation management outcome discriminator.
    pub kind: SkillInstallationManagementOutcomeEnabledChangedKind,
}

/// The management request was refused.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillInstallationManagementOutcomeRefused {
    /// Skill installation management outcome discriminator.
    pub kind: SkillInstallationManagementOutcomeRefusedKind,
    /// Bounded refusal reason.
    pub reason: SkillInstallationFailureReason,
    /// Whole seconds GitHub asked the caller to wait, present only with source-rate-limited.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub retry_after_seconds: Option<i64>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillInstallationManagementResultOutcome {
    /// Discriminator: handled management outcome.
    pub kind: SkillInstallationManagementResultOutcomeKind,
    /// Capabilities honoured for this request.
    pub negotiated: CatalogNegotiatedContract,
    /// Observed management outcome.
    pub outcome: SkillInstallationManagementOutcome,
}

/// Existing-operation control. A new session selector is deliberately not accepted.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillInstallationOperationRequest {
    /// Required authenticated bound Skill installation capability.
    pub contract: CatalogClientContract,
    /// Exact runtime-issued operation ID on the original connection.
    pub operation_id: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillInstallationResultOutcome {
    /// Discriminator: handled operation outcome.
    pub kind: SkillInstallationResultOutcomeKind,
    /// Capabilities honoured for this request.
    pub negotiated: CatalogNegotiatedContract,
    /// Terminal operation outcome.
    pub outcome: SkillInstallationOutcome,
}

/// Inventory request under an explicitly selected existing session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillInstallationsRequest {
    /// Required authenticated bound installation contract.
    pub contract: CatalogClientContract,
    /// Existing selected local session on this connection.
    pub policy_session_id: String,
}

/// Skills available to the session, with their enabled state.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillList {
    /// Available skills
    pub skills: Vec<Skill>,
}

/// Side-effect-free planning of one verified Agent Finder Skill candidate.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillPlanInstallRequest {
    /// Fresh single-use AI skill candidate handle returned by a bound catalogue search.
    pub candidate_handle: String,
    /// Required authenticated bound catalogue and Skill installation capabilities.
    pub contract: CatalogClientContract,
    /// Existing local session attached to this connection.
    pub policy_session_id: String,
}

/// Read-only preparation of one owned Skill removal under fresh selected-session authority.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillPlanUninstallRequest {
    /// Required authenticated bound installation contract.
    pub contract: CatalogClientContract,
    /// Exact receipt to inspect.
    pub installation_id: String,
    /// Existing selected local session on this connection.
    pub policy_session_id: String,
}

/// Authoritative catalog metadata for one SDK-provided skill. The skill's SKILL.md text is fetched separately and lazily.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillProviderDescriptor {
    /// Optional freeform argument hint used by slash-command catalogs.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub argument_hint: Option<String>,
    /// Description used in skill catalogs without fetching content.
    pub description: String,
    /// Absolute path to the folder that holds the skill's supporting files (scripts, references, templates, assets), in the session's path conventions: the host OS for local disk, or the registered SessionFs conventions. When the skill is activated the runtime lists the folder's files for the model through the session's filesystem, with the same rules as a file-based skill's folder; it never reads or copies them. On local disk the folder becomes a read-only skill root, like a configured skill directory. It is not shown in skill catalogs.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub directory: Option<String>,
    /// Whether model invocation is disabled. Defaults to false.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub disable_model_invocation: Option<bool>,
    /// Invocation and display name.
    pub name: String,
    /// Whether users may invoke the skill directly. Defaults to true.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub user_invocable: Option<bool>,
}

/// Catalog metadata returned by an SDK session's skill provider. Catalogs are limited to 1024 descriptors and 1 MiB of aggregate metadata.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct SkillProviderListResult {
    /// Skill descriptors in provider order. Invocation names must be unique under case-insensitive comparison.
    pub skills: Vec<SkillProviderDescriptor>,
}

/// Identifies one SDK-provided skill by invocation name.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct SkillProviderReadRequest {
    /// Target session identifier
    pub session_id: SessionId,
    /// Invocation name of the skill to read.
    pub name: String,
}

/// SKILL.md content returned by an SDK session's skill provider. YAML frontmatter is optional: fields it omits come from the catalog descriptor, fields it declares must match the descriptor, and `allowed-tools` is read only from frontmatter. Supporting files come from the descriptor's `directory`.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct SkillProviderReadResult {
    /// SKILL.md text, with or without YAML frontmatter, or null when the provider has no skill with the requested name. The runtime enforces a 1 MiB UTF-8 byte limit.
    pub markdown: Option<String>,
}

/// Skill names to mark as disabled in global configuration, replacing any previous list.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillsConfigSetDisabledSkillsRequest {
    /// List of skill names to disable
    pub disabled_skills: Vec<String>,
}

/// Adds or removes a single skill from the global disabled list, leaving every other entry untouched.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillsConfigSetSkillDisabledRequest {
    /// True to disable the skill, false to enable it
    pub disabled: bool,
    /// Name of the skill to add to or remove from the disabled list
    pub name: String,
}

/// Name of the skill to disable for the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillsDisableRequest {
    /// Name of the skill to disable
    pub name: String,
}

/// Optional project paths and additional skill directories to include in discovery.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillsDiscoverRequest {
    /// When true, omit skills from the host's global sources (personal, custom, plugin, and built-in), returning only project-scoped skills. For multitenant deployments.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub exclude_host_skills: Option<bool>,
    /// Optional skill scan paths to exclude from discovery.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub ignored_skills_locations: Option<Vec<String>>,
    /// Optional list of project directory paths to scan for project-scoped skills
    #[serde(skip_serializing_if = "Option::is_none")]
    pub project_paths: Option<Vec<String>>,
    /// Explicit discovery-only skill roots, not filesystem grants. Available without ambient discovery, subject to skill/managed disables. Relative paths resolve against the request's effective working directory when supplied. Null and omission are equivalent; an empty array contributes no explicit roots.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub skill_directories: Option<Vec<String>>,
}

/// Name of the skill to enable for the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillsEnableRequest {
    /// Name of the skill to enable
    pub name: String,
}

/// Persisted enablement update for one owned Skill installation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillSetEnabledRequest {
    /// Required authenticated bound Skill installation capability.
    pub contract: CatalogClientContract,
    /// Persisted enablement value.
    pub enabled: bool,
    /// Exact receipt identity to update.
    pub installation_id: String,
    /// Existing selected local session to reconcile after persistence.
    pub policy_session_id: String,
}

/// Optional project paths to enumerate.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillsGetDiscoveryPathsRequest {
    /// When true, omit the host's personal and custom skill directories, leaving only project directories. For multitenant deployments.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub exclude_host_skills: Option<bool>,
    /// Optional skill scan paths to exclude from discovery.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub ignored_skills_locations: Option<Vec<String>>,
    /// Optional list of project directory paths. When omitted or empty, only personal and custom directories are returned.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub project_paths: Option<Vec<String>>,
}

/// Extensible [`SkillsGetDiscoveryPathsRequest`], including inputs added after it was published.
///
/// Required inputs are [`SkillsGetDiscoveryPathsOptions::new`] arguments; optional inputs have fluent setters.
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
pub struct SkillsGetDiscoveryPathsOptions {
    #[serde(flatten)]
    legacy: SkillsGetDiscoveryPathsRequest,
    #[serde(skip_serializing_if = "Option::is_none")]
    skill_directories: Option<Vec<String>>,
}

impl SkillsGetDiscoveryPathsOptions {
    /// Creates options with the required inputs.
    pub fn new() -> Self {
        Self {
            legacy: SkillsGetDiscoveryPathsRequest {
                project_paths: None,
                ignored_skills_locations: None,
                exclude_host_skills: None,
            },
            skill_directories: None,
        }
    }

    /// Optional list of project directory paths. When omitted or empty, only personal and custom directories are returned.
    pub fn project_paths(mut self, value: Vec<String>) -> Self {
        self.legacy.project_paths = Some(value);
        self
    }

    /// Optional skill scan paths to exclude from discovery.
    pub fn ignored_skills_locations(mut self, value: Vec<String>) -> Self {
        self.legacy.ignored_skills_locations = Some(value);
        self
    }

    /// When true, omit the host's personal and custom skill directories, leaving only project directories. For multitenant deployments.
    pub fn exclude_host_skills(mut self, value: bool) -> Self {
        self.legacy.exclude_host_skills = Some(value);
        self
    }

    /// Explicit discovery-only skill roots to enumerate, including configured missing directories. Relative paths resolve against the request's effective working directory when supplied. These are not filesystem grants or preferred creation targets. Null and omission are equivalent.
    pub fn skill_directories(mut self, value: Vec<String>) -> Self {
        self.skill_directories = Some(value);
        self
    }
}

impl Default for SkillsGetDiscoveryPathsOptions {
    fn default() -> Self {
        Self::new()
    }
}

/// Skills invoked during this session, ordered by invocation time (most recent last).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillsGetInvokedResult {
    /// Skills invoked during this session, ordered by invocation time (most recent last)
    pub skills: Vec<SkillsInvokedSkill>,
}

/// Diagnostics from reloading skill definitions, with warnings and errors as separate lists.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillsLoadDiagnostics {
    /// Errors emitted while loading skills (e.g. skills that failed to load entirely)
    pub errors: Vec<String>,
    /// Warnings emitted while loading skills (e.g. skills that loaded but had issues)
    pub warnings: Vec<String>,
}

/// User-configured MCP servers, keyed by server name.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpConfigListResult {
    /// All MCP servers from user config, keyed by name
    pub servers: HashMap<String, serde_json::Value>,
}

/// Request id naming a cancellable registry search.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct McpRegistryAllocateRequestIdResult {
    /// Request ID for `mcp.registry.search` and `mcp.registry.cancel`. It serves one search. Allocation can reclaim unused IDs at the 1,024-ID reservation limit.
    pub request_id: i64,
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
pub struct ExtensionsDiscoverResult {
    /// Discovered user and enabled installed-plugin extensions from persisted Copilot home state
    pub extensions: Vec<DiscoveredExtension>,
    /// Effective extension loading mode. Defaults to load_and_augment when unset.
    pub mode: DiscoveredExtensionMode,
}

/// Skills discovered across global and project sources.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillsDiscoverResult {
    /// Messages for skills that failed to load (e.g. malformed SKILL.md). Empty when host skills are excluded so host-local paths are not disclosed to multitenant callers.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub errors: Option<Vec<String>>,
    /// All discovered skills across all sources
    pub skills: Vec<ServerSkill>,
}

/// Canonical locations where skills can be created so the runtime will recognize them.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillsGetDiscoveryPathsResult {
    /// Canonical skill create/discovery directories, in priority order
    pub paths: Vec<SkillDiscoveryPath>,
}

/// Plugins installed in user/global state.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginsListResult {
    /// Installed plugins
    pub plugins: Vec<InstalledPluginInfo>,
}

/// Result of installing a plugin.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginsInstallResult {
    /// Set when the install path is deprecated (e.g. direct repo / URL / local installs). Callers should surface this to end users.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub deprecation_warning: Option<String>,
    /// The newly installed plugin's metadata
    pub plugin: InstalledPluginInfo,
    /// Optional post-install message provided by the plugin (e.g. setup instructions)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub post_install_message: Option<String>,
    /// Number of skills discovered and installed from the plugin
    pub skills_installed: i64,
    /// Where the completed plugin tree was staged before atomic promotion
    #[serde(skip_serializing_if = "Option::is_none")]
    pub staging_mode: Option<PluginInstallStagingMode>,
}

/// Result of updating a single plugin.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginsUpdateResult {
    /// Version after the update, when reported by the plugin manifest
    #[serde(skip_serializing_if = "Option::is_none")]
    pub new_version: Option<String>,
    /// Version that was previously installed, when available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub previous_version: Option<String>,
    /// Number of skills discovered and installed after the update
    pub skills_installed: i64,
}

/// Result of updating all installed plugins.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginsUpdateAllResult {
    /// Per-plugin update results in deterministic order.
    pub results: Vec<PluginUpdateAllEntry>,
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
pub struct PluginsMarketplacesListResult {
    /// Registered marketplaces
    pub marketplaces: Vec<MarketplaceInfo>,
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
pub struct PluginsMarketplacesAddResult {
    /// Final name of the marketplace as resolved from its manifest
    pub name: String,
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
pub struct PluginsMarketplacesRemoveResult {
    /// Names of installed plugins that prevented removal. Populated only when `removed=false`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub dependent_plugins: Option<Vec<String>>,
    /// True when the marketplace was actually removed. False when removal was skipped because the marketplace has dependent plugins and `force` was not set.
    pub removed: bool,
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
pub struct PluginsMarketplacesBrowseResult {
    /// Plugins advertised by the marketplace
    pub plugins: Vec<MarketplacePluginInfo>,
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
pub struct PluginsMarketplacesRefreshResult {
    /// Per-marketplace refresh results in deterministic order.
    pub results: Vec<MarketplaceRefreshEntry>,
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
pub struct SkillProviderListParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Canvas open result returned by the provider.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CanvasOpenResult {
    /// Provider-supplied status text
    #[serde(skip_serializing_if = "Option::is_none")]
    pub status: Option<String>,
    /// Provider-supplied title
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
    /// URL for web-rendered canvases
    #[serde(skip_serializing_if = "Option::is_none")]
    pub url: Option<String>,
}

/// A runtime-assigned secret placeholder. The identifier is carried once, inside the placeholder, so it cannot contradict a separate secret-id field.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type McpPlanSecretReference = String;

/// Canonical digest algorithm for a validated MCP card
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CardDigestAlgorithm {
    /// SHA-256 over RFC 8785 canonical JSON encoded as UTF-8.
    #[serde(rename = "sha256-rfc8785")]
    Sha256Rfc8785,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Which kind of opaque handle was presented
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogHandleType {
    /// A search candidate handle.
    #[serde(rename = "candidate")]
    Candidate,
    /// An install plan handle.
    #[serde(rename = "plan")]
    Plan,
    /// A model-safe reference to one retained search candidate.
    #[serde(rename = "selection")]
    Selection,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Discriminator: a handle was rejected
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogHandleRejectedErrorKind {
    #[serde(rename = "handle-rejected")]
    #[default]
    HandleRejected,
}

/// Why a presented handle was rejected
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogHandleRejectionReason {
    /// The handle is unparseable or unknown.
    #[serde(rename = "invalid")]
    Invalid,
    /// The handle's time to live has elapsed.
    #[serde(rename = "stale")]
    Stale,
    /// The handle has already been used, and handles are single-use.
    #[serde(rename = "replayed")]
    Replayed,
    /// The handle was issued by a different runtime instance or session.
    #[serde(rename = "foreign")]
    Foreign,
    /// The handle was issued for another catalog operation.
    #[serde(rename = "wrong-kind")]
    WrongKind,
    /// The supplied search identifier does not match the retained candidate.
    #[serde(rename = "search-mismatch")]
    SearchMismatch,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Discriminator: the candidate cannot be installed
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogNotInstallableErrorKind {
    #[serde(rename = "not-installable")]
    #[default]
    NotInstallable,
}

/// Why a discoverable candidate cannot be installed
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogNotInstallableReason {
    /// This kind of resource is not installable through this surface.
    #[serde(rename = "kind-not-installable")]
    KindNotInstallable,
    /// AI skills are discoverable but have no typed importer in this phase.
    #[serde(rename = "ai-skill-not-installable")]
    AiSkillNotInstallable,
    /// Policy forbids installing this candidate.
    #[serde(rename = "policy-forbids")]
    PolicyForbids,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Discriminator: no usable transport is available
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogUnavailableTransportErrorKind {
    #[serde(rename = "unavailable-transport")]
    #[default]
    UnavailableTransport,
}

/// Why no usable transport could be offered
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogUnavailableTransportReason {
    /// The card advertises no transport this runtime can use.
    #[serde(rename = "no-eligible-transport")]
    NoEligibleTransport,
    /// Every advertised transport is of a kind this runtime does not implement.
    #[serde(rename = "transport-not-supported")]
    TransportNotSupported,
    /// Eligible remotes could not be enumerated, so no explicit choice can be offered.
    #[serde(rename = "remote-enumeration-unavailable")]
    RemoteEnumerationUnavailable,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Server transport type: stdio, http, sse (deprecated), or memory
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum DiscoveredMcpServerType {
    /// Server communicates over stdio with a local child process.
    #[serde(rename = "stdio")]
    Stdio,
    /// Server communicates over streamable HTTP.
    #[serde(rename = "http")]
    Http,
    /// Server communicates over Server-Sent Events (deprecated).
    #[serde(rename = "sse")]
    Sse,
    /// Server is backed by an in-memory runtime implementation.
    #[serde(rename = "memory")]
    Memory,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Attachment type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ExtensionContextPushInputType {
    #[serde(rename = "extension_context")]
    #[default]
    ExtensionContext,
}

/// Reviewed resource discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum InstallationReviewMcpResource {
    #[serde(rename = "mcp")]
    #[default]
    Mcp,
}

/// Exact reviewed installation action.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpInstallationReviewInstallAction {
    #[serde(rename = "install")]
    #[default]
    Install,
}

/// Whether a planned configuration change would create or modify an entry
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpPlanConfigurationOperation {
    /// Creates a configuration entry that does not exist yet.
    #[serde(rename = "add")]
    Add,
    /// Modifies a configuration entry that already exists.
    #[serde(rename = "update")]
    Update,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Configuration scope an MCP install plan targets
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpPlanScope {
    /// The user's own MCP configuration.
    #[serde(rename = "user")]
    User,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Transport exposed by a remote endpoint
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpPlanRemoteTransport {
    /// An HTTP endpoint.
    #[serde(rename = "http")]
    Http,
    /// A streamable HTTP endpoint.
    #[serde(rename = "streamable-http")]
    StreamableHttp,
    /// A server-sent events endpoint.
    #[serde(rename = "sse")]
    Sse,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Where a required value is applied when the planned server is launched
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpPlanValueCategory {
    /// Set as an environment variable on the launched process.
    #[serde(rename = "environment-variable")]
    EnvironmentVariable,
    /// Passed to the runtime that launches the package.
    #[serde(rename = "runtime-argument")]
    RuntimeArgument,
    /// Passed to the packaged server itself.
    #[serde(rename = "package-argument")]
    PackageArgument,
    /// Sent as a request header to a remote endpoint.
    #[serde(rename = "header")]
    Header,
    /// Substituted into the remote endpoint URL.
    #[serde(rename = "url-variable")]
    UrlVariable,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// What policy decided for a planned server
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpPlanPolicyDecision {
    /// Policy permits the server.
    #[serde(rename = "allowed")]
    Allowed,
    /// Policy forbids the server, so the plan cannot be applied.
    #[serde(rename = "blocked")]
    Blocked,
    /// Policy permits the server only after an explicit approval.
    #[serde(rename = "requires-approval")]
    RequiresApproval,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Explicit backend selection is part of the final review; failures never switch backends.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpInstallationSecretStorage {
    /// The selected operating-system keychain, without fallback to file storage.
    #[serde(rename = "keychain")]
    Keychain,
    /// The explicitly selected private file backend.
    #[serde(rename = "private-file")]
    PrivateFile,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Discriminator for a package-backed transport choice
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpPlanPackageInstallMethod {
    /// Install and run a local package.
    #[serde(rename = "package")]
    Package,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Discriminator for a scalar required value
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpPlanRequiredValueScalarKind {
    /// The value uses one scalar type.
    #[serde(rename = "scalar")]
    Scalar,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Scalar type a required value must conform to
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpPlanScalarValueType {
    /// Free text.
    #[serde(rename = "string")]
    String,
    /// A number.
    #[serde(rename = "number")]
    Number,
    /// A boolean.
    #[serde(rename = "boolean")]
    Boolean,
    /// A filesystem path.
    #[serde(rename = "path")]
    Path,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Discriminator for an enumerated required value
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpPlanRequiredValueEnumKind {
    /// The value uses a fixed non-empty enumeration.
    #[serde(rename = "enum")]
    Enum,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Discriminator for an enumerated required value
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpPlanEnumValueType {
    /// One of a fixed, non-empty set of permitted values.
    #[serde(rename = "enum")]
    Enum,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// One non-secret value a transport choice needs, represented as a scalar or enumerated variant so enum values cannot be missing or attached to another type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum McpPlanRequiredValue {
    Scalar(McpPlanRequiredValueScalar),
    Enum(McpPlanRequiredValueEnum),
}

/// Transport exposed by a locally launched package
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpPlanPackageTransport {
    /// A locally launched process spoken to over standard input and output.
    #[serde(rename = "stdio")]
    Stdio,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Discriminator for a remote-endpoint transport choice
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpPlanRemoteInstallMethod {
    /// Connect to a remote endpoint.
    #[serde(rename = "remote")]
    Remote,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// One eligible way to run the server, represented as a tagged package or remote variant so package identity and endpoint states cannot contradict the install method.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum McpPlanTransportChoice {
    Package(McpPlanTransportChoicePackage),
    Remote(McpPlanTransportChoiceRemote),
}

/// Exact reviewed installation action.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpInstallationReviewUninstallAction {
    #[serde(rename = "uninstall")]
    #[default]
    Uninstall,
}

/// Safe MCP review fields. No raw card, retrieval URL, plan handle or secret value.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum McpInstallationReview {
    Install(McpInstallationReviewInstall),
    Uninstall(McpInstallationReviewUninstall),
}

/// Reviewed resource discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum InstallationReviewSkillResource {
    #[serde(rename = "skill")]
    #[default]
    Skill,
}

/// Reviewed resource discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum InstallationReviewPluginResource {
    #[serde(rename = "plugin")]
    #[default]
    Plugin,
}

/// Exact reviewed Agent Plugin installation action.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum InstallationReviewPluginReviewAction {
    #[serde(rename = "install")]
    #[default]
    Install,
}

/// Only resource kinds with an implemented installation engine have a review variant.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum InstallationReview {
    Mcp(InstallationReviewMcp),
    Skill(InstallationReviewSkill),
    Plugin(InstallationReviewPlugin),
}

/// Explicit user decisions, never inferred from a permission grant or model response.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum InstallationDecision {
    /// The user explicitly approved the exact review on this request.
    #[serde(rename = "confirm")]
    Confirm,
    /// The user declined the reviewed operation.
    #[serde(rename = "decline")]
    Decline,
    /// The user cancelled the pending decision without granting consent.
    #[serde(rename = "cancel")]
    Cancel,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Allowed values for the `McpAppsSetHostContextDetailsAvailableDisplayMode` enumeration.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpAppsSetHostContextDetailsAvailableDisplayMode {
    /// Rendered inline within the host conversation surface
    #[serde(rename = "inline")]
    Inline,
    /// Rendered as a fullscreen overlay
    #[serde(rename = "fullscreen")]
    Fullscreen,
    /// Rendered as a picture-in-picture floating panel
    #[serde(rename = "pip")]
    Pip,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Current display mode (SEP-1865)
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpAppsSetHostContextDetailsDisplayMode {
    /// Rendered inline within the host conversation surface
    #[serde(rename = "inline")]
    Inline,
    /// Rendered as a fullscreen overlay
    #[serde(rename = "fullscreen")]
    Fullscreen,
    /// Rendered as a picture-in-picture floating panel
    #[serde(rename = "pip")]
    Pip,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Platform type for responsive design
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpAppsSetHostContextDetailsPlatform {
    /// Host runs in a web browser
    #[serde(rename = "web")]
    Web,
    /// Host runs as a desktop application
    #[serde(rename = "desktop")]
    Desktop,
    /// Host runs on a mobile device
    #[serde(rename = "mobile")]
    Mobile,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// UI theme preference per SEP-1865
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpAppsSetHostContextDetailsTheme {
    /// Light UI theme
    #[serde(rename = "light")]
    Light,
    /// Dark UI theme
    #[serde(rename = "dark")]
    Dark,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Headers-refresh response variant discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpHeadersHandlePendingHeadersRefreshRequestHeadersKind {
    #[serde(rename = "headers")]
    #[default]
    Headers,
}

/// Headers-refresh response variant discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpHeadersHandlePendingHeadersRefreshRequestNoneKind {
    #[serde(rename = "none")]
    #[default]
    None,
}

/// Headers-refresh response variant discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpHeadersHandlePendingHeadersRefreshRequestErrorKind {
    #[serde(rename = "error")]
    #[default]
    Error,
}

/// Host response: supply dynamic headers or decline this refresh.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum McpHeadersHandlePendingHeadersRefreshRequest {
    Headers(McpHeadersHandlePendingHeadersRefreshRequestHeaders),
    None(McpHeadersHandlePendingHeadersRefreshRequestNone),
    Error(McpHeadersHandlePendingHeadersRefreshRequestError),
}

/// Bounded refusal categories, without echoing handles, credentials or configuration.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpInstallationFailureReason {
    /// The selected choice or request is unsupported or malformed.
    #[serde(rename = "invalid-request")]
    InvalidRequest,
    /// The bounded original-connection operation limit was reached.
    #[serde(rename = "operation-limit")]
    OperationLimit,
    /// The original operation was cancelled.
    #[serde(rename = "cancelled")]
    Cancelled,
    /// Required installation capabilities were omitted.
    #[serde(rename = "capability-required")]
    CapabilityRequired,
    /// The original host cannot receive human confirmation.
    #[serde(rename = "confirmation-unavailable")]
    ConfirmationUnavailable,
    /// The confirmation response is malformed or mismatched.
    #[serde(rename = "confirmation-invalid")]
    ConfirmationInvalid,
    /// Existing authenticated session and host authority is unavailable.
    #[serde(rename = "policy-context-unavailable")]
    PolicyContextUnavailable,
    /// The original authority or policy changed.
    #[serde(rename = "policy-changed")]
    PolicyChanged,
    /// Current managed policy refuses the operation.
    #[serde(rename = "policy-denied")]
    PolicyDenied,
    /// Configuration changed after the reviewed snapshot.
    #[serde(rename = "configuration-changed")]
    ConfigurationChanged,
    /// Installed configuration no longer matches ownership evidence.
    #[serde(rename = "configuration-modified")]
    ConfigurationModified,
    /// No matching owned resource or original operation exists.
    #[serde(rename = "resource-not-found")]
    ResourceNotFound,
    /// The original plan deadline elapsed.
    #[serde(rename = "plan-expired")]
    PlanExpired,
    /// The one-use plan or prepared operation was already consumed.
    #[serde(rename = "plan-replayed")]
    PlanReplayed,
    /// The handle belongs to a different runtime, session or connection.
    #[serde(rename = "foreign-runtime")]
    ForeignRuntime,
    /// Fresh bound planning is required.
    #[serde(rename = "replan-required")]
    ReplanRequired,
    /// Exact original source revalidation is unsupported.
    #[serde(rename = "source-revalidation-unavailable")]
    SourceRevalidationUnavailable,
    /// The source differs from the retained commitment.
    #[serde(rename = "source-changed")]
    SourceChanged,
    /// The original source could not be retrieved safely.
    #[serde(rename = "source-unavailable")]
    SourceUnavailable,
    /// Authoritative Registry interpretation is unavailable.
    #[serde(rename = "registry-unavailable")]
    RegistryUnavailable,
    /// The selected secret backend is unavailable.
    #[serde(rename = "secret-store-unavailable")]
    SecretStoreUnavailable,
    /// Required owned admission and lifecycle support is absent.
    #[serde(rename = "lifecycle-unavailable")]
    LifecycleUnavailable,
    /// A storage operation failed; inspect any allocated operation before retrying.
    #[serde(rename = "write-failed")]
    WriteFailed,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Installation management outcome discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpInstallationManagementOutcomeRecoveryRequiredKind {
    #[serde(rename = "recovery-required")]
    #[default]
    RecoveryRequired,
}

/// Installation management outcome discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpInstallationManagementOutcomeInstallPreparedKind {
    #[serde(rename = "install-prepared")]
    #[default]
    InstallPrepared,
}

/// Configuration ownership and setup observations, distinct from tool permissions.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpInstallationState {
    /// Owned configuration exists; inventory alone does not grant activation.
    #[serde(rename = "needs-setup")]
    NeedsSetup,
    /// The selected authorised session reports an active installation.
    #[serde(rename = "active")]
    Active,
    /// The selected server requires explicit sign-in.
    #[serde(rename = "authentication-required")]
    AuthenticationRequired,
    /// The selected server could not be activated.
    #[serde(rename = "activation-failed")]
    ActivationFailed,
    /// Owned configuration no longer matches its receipt.
    #[serde(rename = "configuration-modified")]
    ConfigurationModified,
    /// Confirmed durable work or unsafe evidence requires recovery.
    #[serde(rename = "recovery-required")]
    RecoveryRequired,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Installation management outcome discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpInstallationManagementOutcomeListedKind {
    #[serde(rename = "listed")]
    #[default]
    Listed,
}

/// Installation management outcome discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpInstallationManagementOutcomeRecoveredKind {
    #[serde(rename = "recovered")]
    #[default]
    Recovered,
}

/// Installation management outcome discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpInstallationManagementOutcomeUninstallPlannedKind {
    #[serde(rename = "uninstall-planned")]
    #[default]
    UninstallPlanned,
}

/// Installation management outcome discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpInstallationManagementOutcomeOperationKind {
    #[serde(rename = "operation")]
    #[default]
    Operation,
}

/// Original operation progress discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpInstallationOperationStatusPreparingPhase {
    #[serde(rename = "preparing")]
    #[default]
    Preparing,
}

/// Original operation progress discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpInstallationOperationStatusPreparedPhase {
    #[serde(rename = "prepared")]
    #[default]
    Prepared,
}

/// Original operation progress discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpInstallationOperationStatusAwaitingConfirmationPhase {
    #[serde(rename = "awaiting-confirmation")]
    #[default]
    AwaitingConfirmation,
}

/// Original operation progress discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpInstallationOperationStatusRevalidatingPhase {
    #[serde(rename = "revalidating")]
    #[default]
    Revalidating,
}

/// Original operation progress discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpInstallationOperationStatusApplyingPhase {
    #[serde(rename = "applying")]
    #[default]
    Applying,
}

/// Terminal installation outcome discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpInstallationOutcomeInstalledKind {
    #[serde(rename = "installed")]
    #[default]
    Installed,
}

/// Terminal installation outcome discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpInstallationOutcomeUninstalledKind {
    #[serde(rename = "uninstalled")]
    #[default]
    Uninstalled,
}

/// Terminal installation outcome discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpInstallationOutcomeRolledBackKind {
    #[serde(rename = "rolled-back")]
    #[default]
    RolledBack,
}

/// Terminal installation outcome discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpInstallationOutcomeRecoveryRequiredKind {
    #[serde(rename = "recovery-required")]
    #[default]
    RecoveryRequired,
}

/// Terminal installation outcome discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpInstallationOutcomeDeclinedKind {
    #[serde(rename = "declined")]
    #[default]
    Declined,
}

/// Terminal installation outcome discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpInstallationOutcomeCancelledKind {
    #[serde(rename = "cancelled")]
    #[default]
    Cancelled,
}

/// Terminal installation outcome discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpInstallationOutcomeRefusedKind {
    #[serde(rename = "refused")]
    #[default]
    Refused,
}

/// Terminal mutation result. Uncertainty is not approval, rollback or permission to replay.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum McpInstallationOutcome {
    Installed(McpInstallationOutcomeInstalled),
    Uninstalled(McpInstallationOutcomeUninstalled),
    RolledBack(McpInstallationOutcomeRolledBack),
    RecoveryRequired(McpInstallationOutcomeRecoveryRequired),
    Declined(McpInstallationOutcomeDeclined),
    Cancelled(McpInstallationOutcomeCancelled),
    Refused(McpInstallationOutcomeRefused),
}

/// Original operation progress discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpInstallationOperationStatusCompletedPhase {
    #[serde(rename = "completed")]
    #[default]
    Completed,
}

/// Status snapshot from the original connection, independent of new-work account availability.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum McpInstallationOperationStatus {
    Preparing(McpInstallationOperationStatusPreparing),
    Prepared(McpInstallationOperationStatusPrepared),
    AwaitingConfirmation(McpInstallationOperationStatusAwaitingConfirmation),
    Revalidating(McpInstallationOperationStatusRevalidating),
    Applying(McpInstallationOperationStatusApplying),
    Completed(McpInstallationOperationStatusCompleted),
}

/// Installation management outcome discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpInstallationManagementOutcomeRefusedKind {
    #[serde(rename = "refused")]
    #[default]
    Refused,
}

/// Read-only or recovery management result, never permission to activate or replay.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum McpInstallationManagementOutcome {
    RecoveryRequired(McpInstallationManagementOutcomeRecoveryRequired),
    InstallPrepared(McpInstallationManagementOutcomeInstallPrepared),
    Listed(McpInstallationManagementOutcomeListed),
    Recovered(McpInstallationManagementOutcomeRecovered),
    UninstallPlanned(McpInstallationManagementOutcomeUninstallPlanned),
    Operation(McpInstallationManagementOutcomeOperation),
    Refused(McpInstallationManagementOutcomeRefused),
}

/// A handled management outcome with an honoured contract.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpInstallationManagementResultOutcomeKind {
    #[serde(rename = "outcome")]
    #[default]
    Outcome,
}

/// Management result with contract receipt, or a typed request/negotiation refusal.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum McpInstallationManagementResult {
    Outcome(McpInstallationManagementResultOutcome),
    NegotiationRefused(CatalogNegotiationRefusedError),
    InvalidRequest(CatalogInvalidRequestError),
}

/// A handled operation outcome with an honoured contract.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpInstallationResultOutcomeKind {
    #[serde(rename = "outcome")]
    #[default]
    Outcome,
}

/// An installation result together with the exact honoured contract, or a negotiation refusal.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum McpInstallationResult {
    Outcome(McpInstallationResultOutcome),
    NegotiationRefused(CatalogNegotiationRefusedError),
    InvalidRequest(CatalogInvalidRequestError),
}

/// OAuth response variant discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpOauthPendingRequestResponseTokenKind {
    #[serde(rename = "token")]
    #[default]
    Token,
}

/// OAuth response variant discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpOauthPendingRequestResponseCancelledKind {
    #[serde(rename = "cancelled")]
    #[default]
    Cancelled,
}

/// OAuth response variant discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpOauthPendingRequestResponseRuntimeRefreshKind {
    #[serde(rename = "runtimeRefresh")]
    #[default]
    RuntimeRefresh,
}

/// Host response to the pending OAuth request.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum McpOauthPendingRequestResponse {
    Token(McpOauthPendingRequestResponseToken),
    Cancelled(McpOauthPendingRequestResponseCancelled),
    RuntimeRefresh(McpOauthPendingRequestResponseRuntimeRefresh),
}

/// OAuth grant type override for this login.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpOauthLoginGrantType {
    /// Interactive browser-based OAuth flow using an authorization code, typically with PKCE.
    #[serde(rename = "authorization_code")]
    AuthorizationCode,
    /// Headless OAuth flow where a confidential client authenticates directly with a client secret.
    #[serde(rename = "client_credentials")]
    ClientCredentials,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Why a passive MCP OAuth probe determined authentication is needed.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpOauthProbeNeedsAuthReason {
    /// No token was sent and the server requires authentication.
    #[serde(rename = "initial")]
    Initial,
    /// A cached token was sent and rejected.
    #[serde(rename = "refresh")]
    Refresh,
    /// The server returned a 403 insufficient_scope challenge, indicating additional scopes or audience are needed.
    #[serde(rename = "upscope")]
    Upscope,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Probe outcome variant discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpOauthProbeResultNoAuthRequiredStatus {
    #[serde(rename = "no-auth-required")]
    #[default]
    NoAuthRequired,
}

/// Probe outcome variant discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpOauthProbeResultAuthenticatedStatus {
    #[serde(rename = "authenticated")]
    #[default]
    Authenticated,
}

/// Probe outcome variant discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpOauthProbeResultNeedsAuthStatus {
    #[serde(rename = "needs-auth")]
    #[default]
    NeedsAuth,
}

/// Probe outcome variant discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpOauthProbeResultFailedStatus {
    #[serde(rename = "failed")]
    #[default]
    Failed,
}

/// Passive MCP OAuth probe result. `authenticated` means the server accepted the probe request while an OAuth-origin access token was attached; it does not prove the server required or independently validated that token. The probe does not make a second unauthenticated request. Failed is an expected probe-domain outcome; JSON-RPC errors are reserved for API-call failures.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum McpOauthProbeResult {
    NoAuthRequired(McpOauthProbeResultNoAuthRequired),
    Authenticated(McpOauthProbeResultAuthenticated),
    NeedsAuth(McpOauthProbeResultNeedsAuth),
    Failed(McpOauthProbeResultFailed),
}

/// OAuth grant type to use when authenticating to the remote MCP server.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpServerConfigHttpOauthGrantType {
    /// Interactive browser-based authorization code flow with PKCE.
    #[serde(rename = "authorization_code")]
    AuthorizationCode,
    /// Headless client credentials flow using the configured OAuth client.
    #[serde(rename = "client_credentials")]
    ClientCredentials,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Discriminator: a plan was computed and nothing was changed
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpPlanInstallPlannedKind {
    #[serde(rename = "planned")]
    #[default]
    Planned,
}

/// Discriminator for a candidate-backed install-plan source
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpPlanInstallSourceCandidateKind {
    /// Plan from a candidate returned by catalog search.
    #[serde(rename = "candidate")]
    Candidate,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Discriminator for a URL-backed MCP server card
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpServerCardUrlKind {
    /// Retrieve the card from its URL.
    #[serde(rename = "url")]
    Url,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Discriminator for an embedded MCP server card
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpServerCardEmbeddedKind {
    /// Use the embedded card document.
    #[serde(rename = "embedded")]
    Embedded,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// A card supplied directly by the caller. Exactly one of a URL or embedded data, encoded structurally so neither both nor neither can be expressed.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum McpServerCardReference {
    Url(McpServerCardUrl),
    Embedded(McpServerCardEmbedded),
}

/// Discriminator for a caller-supplied-card install-plan source
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpPlanInstallSourceCardKind {
    /// Plan directly from a caller-supplied card.
    #[serde(rename = "card")]
    Card,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// What an install plan is computed from: a candidate handle from a previous search, or a card supplied directly.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum McpPlanInstallSource {
    Candidate(McpPlanInstallSourceCandidate),
    Card(McpPlanInstallSourceCard),
}

/// Outcome of an mcp.planInstall call: either a normalised plan, or one typed refusal. Nothing is written in either case.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum McpPlanInstallResult {
    Planned(McpPlanInstallPlanned),
    NegotiationRefused(CatalogNegotiationRefusedError),
    HandleRejected(CatalogHandleRejectedError),
    InvalidRequest(CatalogInvalidRequestError),
    AuthenticationRequired(CatalogAuthenticationRequiredError),
    PolicyRejected(CatalogPolicyRejectedError),
    NetworkFailure(CatalogNetworkFailureError),
    UnsafeRetrieval(CatalogUnsafeRetrievalError),
    MalformedCard(CatalogMalformedCardError),
    ContractViolation(CatalogContractViolationError),
    UnavailableTransport(CatalogUnavailableTransportError),
    NotInstallable(CatalogNotInstallableError),
    Unavailable(CatalogUnavailableError),
}

/// Controls if tools provided by this server can be loaded on demand via tool search (auto) or always included in the initial tool list (never)
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpServerConfigDeferTools {
    /// Tools may be deferred under certain conditions
    #[serde(rename = "auto")]
    Auto,
    /// Tools are always included in the initial tool list, even when tool search is enabled.
    #[serde(rename = "never")]
    Never,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Remote transport type. Defaults to "http" when omitted.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpServerConfigHttpType {
    /// Streamable HTTP transport.
    #[serde(rename = "http")]
    Http,
    /// Server-Sent Events transport.
    #[serde(rename = "sse")]
    Sse,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// In-process MCP transport type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub(crate) enum McpServerConfigMemoryType {
    #[serde(rename = "memory")]
    Memory,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Local MCP transport type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpServerConfigStdioType {
    /// Legacy alias for the local stdio transport.
    #[serde(rename = "local")]
    Local,
    /// Server communicates over stdio with a local child process.
    #[serde(rename = "stdio")]
    Stdio,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Bounded refusal categories for verified Agent Plugin installation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PluginInstallationFailureReason {
    /// The selected session does not have the Agent Finder plugin installation feature flag enabled.
    #[serde(rename = "feature-disabled")]
    FeatureDisabled,
    /// The request is unsupported or malformed.
    #[serde(rename = "invalid-request")]
    InvalidRequest,
    /// The original host cannot receive human confirmation.
    #[serde(rename = "confirmation-unavailable")]
    ConfirmationUnavailable,
    /// The confirmation response is malformed or mismatched.
    #[serde(rename = "confirmation-invalid")]
    ConfirmationInvalid,
    /// Existing authenticated session and host authority is unavailable.
    #[serde(rename = "policy-context-unavailable")]
    PolicyContextUnavailable,
    /// The original authority or policy changed.
    #[serde(rename = "policy-changed")]
    PolicyChanged,
    /// Managed policy only allows plugins from known marketplaces, which excludes direct installs.
    #[serde(rename = "policy-blocked")]
    PolicyBlocked,
    /// The original plan deadline elapsed.
    #[serde(rename = "plan-expired")]
    PlanExpired,
    /// The one-use plan was already consumed.
    #[serde(rename = "plan-replayed")]
    PlanReplayed,
    /// The handle belongs to a different runtime, session or connection.
    #[serde(rename = "foreign-runtime")]
    ForeignRuntime,
    /// The handle is not the expected handle kind.
    #[serde(rename = "wrong-kind")]
    WrongKind,
    /// The handle was minted for a different search result or authority.
    #[serde(rename = "search-mismatch")]
    SearchMismatch,
    /// The handle or operation expired.
    #[serde(rename = "expired")]
    Expired,
    /// The candidate handle does not belong to an Agent Plugin with a complete materialisation reference.
    #[serde(rename = "invalid-candidate")]
    InvalidCandidate,
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
    /// The candidate's materialisation is not an Agent Plugin.
    #[serde(rename = "unsupported-resource")]
    UnsupportedResource,
    /// The plugin manifest could not be retrieved or verified.
    #[serde(rename = "entrypoint-unavailable")]
    EntrypointUnavailable,
    /// The plugin manifest is not a valid plugin manifest, or the bundle has another manifest that would take precedence over the reviewed one.
    #[serde(rename = "invalid-plugin")]
    InvalidPlugin,
    /// The plugin manifest exceeds the bounded complete review size.
    #[serde(rename = "review-too-large")]
    ReviewTooLarge,
    /// The reviewed plugin files could not be acquired.
    #[serde(rename = "payload-unavailable")]
    PayloadUnavailable,
    /// The acquired files no longer match the reviewed descriptor.
    #[serde(rename = "payload-mismatch")]
    PayloadMismatch,
    /// GitHub rate limited retrieval from the pinned public source; nothing was installed. Retry after retryAfterSeconds when present.
    #[serde(rename = "source-rate-limited")]
    SourceRateLimited,
    /// No GitHub sign-in or credential is available, so the caller must sign in.
    #[serde(rename = "no-credential")]
    NoCredential,
    /// Agent Finder refused the GitHub credential (HTTP 401, or a 403 without rate-limit evidence); the caller must sign in again.
    #[serde(rename = "credential-rejected")]
    CredentialRejected,
    /// Plugin installation storage or admission is busy.
    #[serde(rename = "busy")]
    Busy,
    /// A plugin with the same name or install location already exists.
    #[serde(rename = "already-installed")]
    AlreadyInstalled,
    /// A storage operation failed; nothing was registered.
    #[serde(rename = "write-failed")]
    WriteFailed,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Agent Plugin installation management outcome discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PluginInstallationManagementOutcomeInstallPlannedKind {
    #[serde(rename = "install-planned")]
    #[default]
    InstallPlanned,
}

/// Agent Plugin installation management outcome discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PluginInstallationManagementOutcomeRefusedKind {
    #[serde(rename = "refused")]
    #[default]
    Refused,
}

/// Management outcome for verified Agent Plugin installation planning.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum PluginInstallationManagementOutcome {
    InstallPlanned(PluginInstallationManagementOutcomeInstallPlanned),
    Refused(PluginInstallationManagementOutcomeRefused),
}

/// Discriminator: handled management outcome.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PluginInstallationManagementResultOutcomeKind {
    #[serde(rename = "outcome")]
    #[default]
    Outcome,
}

/// Agent Plugin installation planning result with the honoured contract, or a typed refusal.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum PluginInstallationManagementResult {
    Outcome(PluginInstallationManagementResultOutcome),
    NegotiationRefused(CatalogNegotiationRefusedError),
    InvalidRequest(CatalogInvalidRequestError),
    NetworkFailure(CatalogNetworkFailureError),
}

/// Terminal Agent Plugin installation outcome discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PluginInstallationOutcomeInstalledKind {
    #[serde(rename = "installed")]
    #[default]
    Installed,
}

/// Terminal Agent Plugin installation outcome discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PluginInstallationOutcomeDeclinedKind {
    #[serde(rename = "declined")]
    #[default]
    Declined,
}

/// Terminal Agent Plugin installation outcome discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PluginInstallationOutcomeCancelledKind {
    #[serde(rename = "cancelled")]
    #[default]
    Cancelled,
}

/// Terminal Agent Plugin installation outcome discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PluginInstallationOutcomeRefusedKind {
    #[serde(rename = "refused")]
    #[default]
    Refused,
}

/// Terminal verified Agent Plugin installation result.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum PluginInstallationOutcome {
    Installed(PluginInstallationOutcomeInstalled),
    Declined(PluginInstallationOutcomeDeclined),
    Cancelled(PluginInstallationOutcomeCancelled),
    Refused(PluginInstallationOutcomeRefused),
}

/// Discriminator: handled operation outcome.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PluginInstallationResultOutcomeKind {
    #[serde(rename = "outcome")]
    #[default]
    Outcome,
}

/// Agent Plugin installation result with the honoured contract, or a typed request/negotiation refusal.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum PluginInstallationResult {
    Outcome(PluginInstallationResultOutcome),
    NegotiationRefused(CatalogNegotiationRefusedError),
    InvalidRequest(CatalogInvalidRequestError),
    NetworkFailure(CatalogNetworkFailureError),
}

/// Exact reviewed Agent Plugin installation action.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PluginInstallationReviewAction {
    #[serde(rename = "install")]
    #[default]
    Install,
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
pub enum SkillDiscoveryScope {
    /// A project's repository skill directory.
    #[serde(rename = "project")]
    Project,
    /// The user's personal Copilot skill directory.
    #[serde(rename = "personal-copilot")]
    PersonalCopilot,
    /// The user's personal agents skill directory.
    #[serde(rename = "personal-agents")]
    PersonalAgents,
    /// A configured custom skill directory.
    #[serde(rename = "custom")]
    Custom,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Bounded refusal categories for verified Skill installation management.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SkillInstallationFailureReason {
    /// The selected session does not have the Agent Finder Skill installation feature flag enabled for acquisition.
    #[serde(rename = "feature-disabled")]
    FeatureDisabled,
    /// The request is unsupported or malformed.
    #[serde(rename = "invalid-request")]
    InvalidRequest,
    /// The bounded operation limit was reached.
    #[serde(rename = "operation-limit")]
    OperationLimit,
    /// The original operation was cancelled.
    #[serde(rename = "cancelled")]
    Cancelled,
    /// The original host cannot receive human confirmation.
    #[serde(rename = "confirmation-unavailable")]
    ConfirmationUnavailable,
    /// The confirmation response is malformed or mismatched.
    #[serde(rename = "confirmation-invalid")]
    ConfirmationInvalid,
    /// Existing authenticated session and host authority is unavailable.
    #[serde(rename = "policy-context-unavailable")]
    PolicyContextUnavailable,
    /// The original authority or policy changed.
    #[serde(rename = "policy-changed")]
    PolicyChanged,
    /// No matching owned resource or original operation exists.
    #[serde(rename = "resource-not-found")]
    ResourceNotFound,
    /// The original plan deadline elapsed.
    #[serde(rename = "plan-expired")]
    PlanExpired,
    /// The one-use plan was already consumed.
    #[serde(rename = "plan-replayed")]
    PlanReplayed,
    /// The handle belongs to a different runtime, session or connection.
    #[serde(rename = "foreign-runtime")]
    ForeignRuntime,
    /// The handle is not the expected Skill handle kind.
    #[serde(rename = "wrong-kind")]
    WrongKind,
    /// The handle was minted for a different search result or authority.
    #[serde(rename = "search-mismatch")]
    SearchMismatch,
    /// The handle or operation expired.
    #[serde(rename = "expired")]
    Expired,
    /// The candidate handle is not a verified installable Skill candidate.
    #[serde(rename = "invalid-candidate")]
    InvalidCandidate,
    /// The verified Skill descriptor could not be retrieved safely.
    #[serde(rename = "descriptor-unavailable")]
    DescriptorUnavailable,
    /// The verified Skill descriptor failed validation.
    #[serde(rename = "descriptor-invalid")]
    DescriptorInvalid,
    /// The Skill entrypoint could not be retrieved or verified.
    #[serde(rename = "entrypoint-unavailable")]
    EntrypointUnavailable,
    /// The Skill entrypoint is not a valid Skill.
    #[serde(rename = "invalid-skill")]
    InvalidSkill,
    /// The reviewed Skill payload could not be acquired.
    #[serde(rename = "payload-unavailable")]
    PayloadUnavailable,
    /// The acquired payload no longer matches the reviewed descriptor.
    #[serde(rename = "payload-mismatch")]
    PayloadMismatch,
    /// GitHub rate limited retrieval from the pinned public source; nothing was installed. Retry after retryAfterSeconds when present. Reported only to callers that require catalog-reference-installation; others receive payload-unavailable.
    #[serde(rename = "source-rate-limited")]
    SourceRateLimited,
    /// No GitHub sign-in or credential is available, so the caller must sign in. Reported only to callers that require catalog-reference-installation; others receive policy-context-unavailable.
    #[serde(rename = "no-credential")]
    NoCredential,
    /// Agent Finder refused the GitHub credential (HTTP 401, or a 403 without rate-limit evidence); the caller must sign in again. Reported only to callers that require catalog-reference-installation; others receive descriptor-unavailable.
    #[serde(rename = "credential-rejected")]
    CredentialRejected,
    /// The retained Skill source changed after planning.
    #[serde(rename = "source-changed")]
    SourceChanged,
    /// The selected runtime cannot inspect or control the requested lifecycle operation.
    #[serde(rename = "lifecycle-unavailable")]
    LifecycleUnavailable,
    /// Durable Skill installation evidence requires recovery before mutation.
    #[serde(rename = "recovery-required")]
    RecoveryRequired,
    /// The Skill entrypoint exceeds the bounded complete review size.
    #[serde(rename = "review-too-large")]
    ReviewTooLarge,
    /// Skill installation storage or admission is busy.
    #[serde(rename = "busy")]
    Busy,
    /// An owned Skill with the same identity or target already exists.
    #[serde(rename = "already-installed")]
    AlreadyInstalled,
    /// Installed Skill files no longer match ownership evidence.
    #[serde(rename = "configuration-modified")]
    ConfigurationModified,
    /// A storage operation failed; inspect durable state before retrying.
    #[serde(rename = "write-failed")]
    WriteFailed,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Skill installation management outcome discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SkillInstallationManagementOutcomeRecoveryRequiredKind {
    #[serde(rename = "recovery-required")]
    #[default]
    RecoveryRequired,
}

/// Skill installation management outcome discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SkillInstallationManagementOutcomeInstallPlannedKind {
    #[serde(rename = "install-planned")]
    #[default]
    InstallPlanned,
}

/// Skill installation management outcome discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SkillInstallationManagementOutcomeListedKind {
    #[serde(rename = "listed")]
    #[default]
    Listed,
}

/// Skill installation management outcome discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SkillInstallationManagementOutcomeRecoveredKind {
    #[serde(rename = "recovered")]
    #[default]
    Recovered,
}

/// Skill installation management outcome discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SkillInstallationManagementOutcomeRolledBackKind {
    #[serde(rename = "rolled-back")]
    #[default]
    RolledBack,
}

/// Skill installation management outcome discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SkillInstallationManagementOutcomeUninstallPlannedKind {
    #[serde(rename = "uninstall-planned")]
    #[default]
    UninstallPlanned,
}

/// Skill installation management outcome discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SkillInstallationManagementOutcomeOperationKind {
    #[serde(rename = "operation")]
    #[default]
    Operation,
}

#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SkillInstallationOperationStatusPreparingPhase {
    #[serde(rename = "preparing")]
    #[default]
    Preparing,
}

#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SkillInstallationOperationStatusPreparedPhase {
    #[serde(rename = "prepared")]
    #[default]
    Prepared,
}

#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SkillInstallationOperationStatusAwaitingConfirmationPhase {
    #[serde(rename = "awaiting-confirmation")]
    #[default]
    AwaitingConfirmation,
}

#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SkillInstallationOperationStatusRevalidatingPhase {
    #[serde(rename = "revalidating")]
    #[default]
    Revalidating,
}

#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SkillInstallationOperationStatusApplyingPhase {
    #[serde(rename = "applying")]
    #[default]
    Applying,
}

/// Terminal Skill installation outcome discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SkillInstallationOutcomeInstalledKind {
    #[serde(rename = "installed")]
    #[default]
    Installed,
}

/// Terminal Skill installation outcome discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SkillInstallationOutcomeUninstalledKind {
    #[serde(rename = "uninstalled")]
    #[default]
    Uninstalled,
}

/// Terminal Skill installation outcome discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SkillInstallationOutcomeRolledBackKind {
    #[serde(rename = "rolled-back")]
    #[default]
    RolledBack,
}

/// Terminal Skill installation outcome discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SkillInstallationOutcomeRecoveryRequiredKind {
    #[serde(rename = "recovery-required")]
    #[default]
    RecoveryRequired,
}

/// Terminal Skill installation outcome discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SkillInstallationOutcomeDeclinedKind {
    #[serde(rename = "declined")]
    #[default]
    Declined,
}

/// Terminal Skill installation outcome discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SkillInstallationOutcomeCancelledKind {
    #[serde(rename = "cancelled")]
    #[default]
    Cancelled,
}

/// Terminal Skill installation outcome discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SkillInstallationOutcomeRefusedKind {
    #[serde(rename = "refused")]
    #[default]
    Refused,
}

/// Terminal verified Skill mutation result.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum SkillInstallationOutcome {
    Installed(SkillInstallationOutcomeInstalled),
    Uninstalled(SkillInstallationOutcomeUninstalled),
    RolledBack(SkillInstallationOutcomeRolledBack),
    RecoveryRequired(SkillInstallationOutcomeRecoveryRequired),
    Declined(SkillInstallationOutcomeDeclined),
    Cancelled(SkillInstallationOutcomeCancelled),
    Refused(SkillInstallationOutcomeRefused),
}

#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SkillInstallationOperationStatusCompletedPhase {
    #[serde(rename = "completed")]
    #[default]
    Completed,
}

/// Status snapshot from the original connection, independent of new-work account availability.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum SkillInstallationOperationStatus {
    Preparing(SkillInstallationOperationStatusPreparing),
    Prepared(SkillInstallationOperationStatusPrepared),
    AwaitingConfirmation(SkillInstallationOperationStatusAwaitingConfirmation),
    Revalidating(SkillInstallationOperationStatusRevalidating),
    Applying(SkillInstallationOperationStatusApplying),
    Completed(SkillInstallationOperationStatusCompleted),
}

/// Skill installation management outcome discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SkillInstallationManagementOutcomeEnabledChangedKind {
    #[serde(rename = "enabled-changed")]
    #[default]
    EnabledChanged,
}

/// Skill installation management outcome discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SkillInstallationManagementOutcomeRefusedKind {
    #[serde(rename = "refused")]
    #[default]
    Refused,
}

/// Management outcome for verified Skill inventory, planning and removal.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum SkillInstallationManagementOutcome {
    RecoveryRequired(SkillInstallationManagementOutcomeRecoveryRequired),
    InstallPlanned(SkillInstallationManagementOutcomeInstallPlanned),
    Listed(SkillInstallationManagementOutcomeListed),
    Recovered(SkillInstallationManagementOutcomeRecovered),
    RolledBack(SkillInstallationManagementOutcomeRolledBack),
    UninstallPlanned(SkillInstallationManagementOutcomeUninstallPlanned),
    Operation(SkillInstallationManagementOutcomeOperation),
    EnabledChanged(SkillInstallationManagementOutcomeEnabledChanged),
    Refused(SkillInstallationManagementOutcomeRefused),
}

/// Discriminator: handled management outcome.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SkillInstallationManagementResultOutcomeKind {
    #[serde(rename = "outcome")]
    #[default]
    Outcome,
}

/// Skill installation management result with the honoured contract, or a typed refusal.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum SkillInstallationManagementResult {
    Outcome(SkillInstallationManagementResultOutcome),
    NegotiationRefused(CatalogNegotiationRefusedError),
    InvalidRequest(CatalogInvalidRequestError),
    NetworkFailure(CatalogNetworkFailureError),
}

/// Discriminator: handled operation outcome.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SkillInstallationResultOutcomeKind {
    #[serde(rename = "outcome")]
    #[default]
    Outcome,
}

/// Skill installation result with the honoured contract, or a typed request/negotiation refusal.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum SkillInstallationResult {
    Outcome(SkillInstallationResultOutcome),
    NegotiationRefused(CatalogNegotiationRefusedError),
    InvalidRequest(CatalogInvalidRequestError),
    NetworkFailure(CatalogNetworkFailureError),
}

/// Compatibility alias for the confirmation review wrapper.
pub type InstallationConfirmationRequestReview = InstallationReview;

/// Resource discriminator for installation confirmation reviews.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum InstallationReviewResource {
    #[serde(rename = "mcp")]
    #[default]
    Mcp,
    #[serde(rename = "skill")]
    Skill,
}

/// Compatibility alias for the confirmation review resource discriminator.
pub type InstallationConfirmationRequestReviewResource = InstallationReviewResource;

//! Auto-generated from api.schema.json — do not edit manually.

#![allow(clippy::large_enum_variant)]
#![allow(deprecated)]
#![allow(dead_code)]
#![allow(rustdoc::invalid_html_tags)]

use std::collections::HashMap;

use serde::{Deserialize, Serialize};

pub use super::session_events::{McpOauthHttpResponse, McpOauthWWWAuthenticateParams};
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
    /// `session.mcp.oauth.prepareFlow`
    pub const SESSION_MCP_OAUTH_PREPAREFLOW: &str = "session.mcp.oauth.prepareFlow";
    /// `session.mcp.oauth.cancelFlow`
    pub const SESSION_MCP_OAUTH_CANCELFLOW: &str = "session.mcp.oauth.cancelFlow";
    /// `session.mcp.oauth.authenticate`
    pub const SESSION_MCP_OAUTH_AUTHENTICATE: &str = "session.mcp.oauth.authenticate";
    /// `session.mcp.oauth.hasValidTokens`
    pub const SESSION_MCP_OAUTH_HASVALIDTOKENS: &str = "session.mcp.oauth.hasValidTokens";
    /// `session.mcp.oauth.tryRefreshTokens`
    pub const SESSION_MCP_OAUTH_TRYREFRESHTOKENS: &str = "session.mcp.oauth.tryRefreshTokens";
    /// `session.mcp.oauth.saveStaticClientSecret`
    pub const SESSION_MCP_OAUTH_SAVESTATICCLIENTSECRET: &str =
        "session.mcp.oauth.saveStaticClientSecret";
    /// `session.mcp.oauth.disposeStore`
    pub const SESSION_MCP_OAUTH_DISPOSESTORE: &str = "session.mcp.oauth.disposeStore";
    /// `session.mcp.oauth.testConnection`
    pub const SESSION_MCP_OAUTH_TESTCONNECTION: &str = "session.mcp.oauth.testConnection";
    /// `session.mcp.oauth.storeKey`
    pub const SESSION_MCP_OAUTH_STOREKEY: &str = "session.mcp.oauth.storeKey";
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

/// Token projection for the existing host-delegated provider.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpOauthAuthenticateResult {
    /// Access token from the runtime-owned store.
    pub access_token: String,
    /// Remaining lifetime in seconds when the issuer supplied an expiry.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub expires_in: Option<i64>,
    /// Token scheme. Defaults to Bearer.
    pub token_type: String,
}

/// A pre-registered OAuth client for this attempt.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpOauthFlowStaticClientConfig {
    /// Pre-registered client identifier.
    pub client_id: String,
    /// Ephemeral secret that the runtime never persists.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub client_secret: Option<String>,
    /// OAuth grant override.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub grant_type: Option<McpOauthLoginGrantType>,
    /// Whether this client has no secret. Defaults to true.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub public_client: Option<bool>,
    /// Scope used when the server omits its challenge scope.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub scope: Option<String>,
}

/// Use the current session authorizer's store.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpOauthFlowStoreSession {
    pub kind: McpOauthFlowStoreSessionKind,
}

/// Retain an isolated in-memory store until disposal or requester detachment.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpOauthFlowStoreMemory {
    pub kind: McpOauthFlowStoreMemoryKind,
    /// Identity chosen by the host, scoped to this session requester.
    pub store_id: String,
}

/// Use the host's configured profile directory.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpOauthFlowStorePersistent {
    /// Profile directory containing the OAuth credential store.
    pub config_dir: String,
    pub kind: McpOauthFlowStorePersistentKind,
    /// Identity chosen by the host, scoped to this session requester.
    pub store_id: String,
    /// Original store's effective token session. An empty value selects unscoped storage.
    pub token_session_id: String,
}

/// Call the original requester's OAuth store host effects.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpOauthFlowStoreCallback {
    pub kind: McpOauthFlowStoreCallbackKind,
    /// Host-owned callback store identity.
    pub store_id: String,
}

/// The server accepted an unauthenticated initialize request.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpOauthTestConnectionResultConnected {
    pub status: McpOauthTestConnectionResultConnectedStatus,
}

/// The server refused the initialize request with an authentication challenge.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpOauthTestConnectionResultNeedsAuth {
    /// The transport's authentication error message.
    pub error: String,
    /// Exact refusing HTTP response, when the transport captured it.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub http_response: Option<McpOauthHttpResponse>,
    pub status: McpOauthTestConnectionResultNeedsAuthStatus,
    /// Raw WWW-Authenticate header, when the refusing response supplies one.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub www_authenticate_header: Option<String>,
    /// Parsed challenge parameters from the refusing HTTP response.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub www_authenticate_params: Option<McpOauthWWWAuthenticateParams>,
}

/// The handshake failed for a reason other than authentication.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpOauthTestConnectionResultFailed {
    /// Transport or session policy failure detail.
    pub error: String,
    pub status: McpOauthTestConnectionResultFailedStatus,
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

/// Correlation identity reserved before an effectful authentication request.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpOauthPrepareFlowParams {
    /// Target session identifier
    pub session_id: SessionId,
    /// Host-chosen identity, unique within this session requester.
    pub flow_id: String,
}

/// Correlation identity reserved before an effectful authentication request.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpOauthCancelFlowParams {
    /// Target session identifier
    pub session_id: SessionId,
    /// Host-chosen identity, unique within this session requester.
    pub flow_id: String,
}

/// Full host-driven authentication for an arbitrary, possibly unsaved, MCP URL.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpOauthAuthenticateParams {
    /// Target session identifier
    pub session_id: SessionId,
    /// Correlation identity reserved through prepareFlow.
    pub flow_id: String,
    /// MCP endpoint, without a requirement for saved configuration.
    pub server_url: String,
    /// Use environment session claims for a hostless compatibility call, while retaining this requester's egress authority.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sessionless: Option<bool>,
    /// Credential owner. Omission uses the authoritative session store.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub store: Option<McpOauthFlowStore>,
    /// Proxy fallback when the session has not published an egress entry.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sandbox_proxy: Option<String>,
    /// Static OAuth client settings for this attempt.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub static_client_config: Option<McpOauthFlowStaticClientConfig>,
    /// Bypass cached grants for explicit account selection.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub force_reauth: Option<bool>,
    /// Fail with browser_required when silent authentication cannot complete.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub browserless: Option<bool>,
    /// Start device authorization instead of a browser flow.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub force_device_code: Option<bool>,
    /// Let the host present the authorization URL without a runtime browser launch.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub skip_browser_open: Option<bool>,
    /// Authorization timeout in milliseconds. Defaults to five minutes.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub authorization_timeout_ms: Option<i64>,
    /// Override the device authorization poll interval in milliseconds.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub device_code_poll_interval_ms: Option<i64>,
    /// Delay before the runtime offers device authorization.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub device_code_offer_timeout_ms: Option<i64>,
    /// Display name for new dynamic client registrations.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub client_name: Option<String>,
    /// Client metadata document URL. Omission uses the session identity.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub client_id_metadata_url: Option<String>,
    /// Text displayed after a successful loopback callback.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub callback_success_message: Option<String>,
    /// Fixed callback port. A busy port fails without fallback.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub redirect_port: Option<i64>,
    /// Reject the cached access token but permit a refresh grant.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub skip_cached_access_token: Option<bool>,
    /// Preserve previously granted scopes during additional consent.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub include_existing_token_scopes: Option<bool>,
    /// Preserve replacement policy when another pending flow owns consent.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub preserve_pending_flow_replacement_flags: Option<bool>,
    /// Challenge captured by the host, including an empty challenge.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub www_authenticate_params: Option<McpOauthWWWAuthenticateParams>,
    /// Raw protected-resource metadata already fetched by the host.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub resource_metadata: Option<String>,
}

/// Token projection for the existing host-delegated provider.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpOauthAuthenticateResult {
    /// Access token from the runtime-owned store.
    pub access_token: String,
    /// Remaining lifetime in seconds when the issuer supplied an expiry.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub expires_in: Option<i64>,
    /// Token scheme. Defaults to Bearer.
    pub token_type: String,
}

/// A narrow token-validity query against the chosen credential owner.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpOauthHasValidTokensParams {
    /// Target session identifier
    pub session_id: SessionId,
    /// Runtime OAuth credential key.
    pub store_key: String,
    /// Preserve hostless credential and account selection.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sessionless: Option<bool>,
    /// Credential owner. Omission uses the authoritative session store.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub store: Option<McpOauthFlowStore>,
    /// Static client identity required for refresh, when configured.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub static_client_config: Option<McpOauthFlowStaticClientConfig>,
}

/// A narrow token-validity query against the chosen credential owner.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpOauthTryRefreshTokensParams {
    /// Target session identifier
    pub session_id: SessionId,
    /// Runtime OAuth credential key.
    pub store_key: String,
    /// Preserve hostless credential and account selection.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sessionless: Option<bool>,
    /// Credential owner. Omission uses the authoritative session store.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub store: Option<McpOauthFlowStore>,
    /// Static client identity required for refresh, when configured.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub static_client_config: Option<McpOauthFlowStaticClientConfig>,
}

/// Persist a confidential client's static secret without exposing token-store CRUD.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpOauthSaveStaticClientSecretParams {
    /// Target session identifier
    pub session_id: SessionId,
    /// Runtime OAuth credential key.
    pub store_key: String,
    /// Preserve hostless credential and account selection.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sessionless: Option<bool>,
    /// Secret to persist in the selected credential owner.
    pub secret: String,
    /// Credential owner. Omission uses the authoritative session store.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub store: Option<McpOauthFlowStore>,
}

/// Dispose an explicit host store without altering the session authorizer's owner.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpOauthDisposeStoreParams {
    /// Target session identifier
    pub session_id: SessionId,
    /// Host-selected identity of the explicit store.
    pub store_id: String,
}

/// Tokenless initialize handshake for a possibly unsaved remote MCP server.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpOauthTestConnectionParams {
    /// Target session identifier
    pub session_id: SessionId,
    /// Remote endpoint to initialize without an OAuth token.
    pub url: String,
    /// Transport. Omission selects Streamable HTTP.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub r#type: Option<McpOauthTestConnectionTransport>,
    /// Headers sent during the handshake.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub headers: Option<HashMap<String, String>>,
    /// Proxy fallback when no session egress entry exists.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sandbox_proxy: Option<String>,
}

/// Derive a credential key through the selected account and store owner.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpOauthStoreKeyParams {
    /// Target session identifier
    pub session_id: SessionId,
    /// MCP endpoint whose credentials the key identifies.
    pub server_url: String,
    /// Static OAuth client identity, when configured.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub client_id: Option<String>,
    /// Metadata document identity when no static client is configured.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub client_id_metadata_url: Option<String>,
    /// Credential owner. Omission uses the authoritative session store.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub store: Option<McpOauthFlowStore>,
    /// Preserve hostless credential and account selection.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sessionless: Option<bool>,
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

/// Whether the credential operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type SessionMcpOauthHasValidTokensResult = bool;

/// Whether the credential operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type SessionMcpOauthTryRefreshTokensResult = bool;

/// The selected owner's OAuth credential key.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type SessionMcpOauthStoreKeyResult = String;

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

#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpOauthFlowStoreSessionKind {
    #[serde(rename = "session")]
    #[default]
    Session,
}

#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpOauthFlowStoreMemoryKind {
    #[serde(rename = "memory")]
    #[default]
    Memory,
}

#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpOauthFlowStorePersistentKind {
    #[serde(rename = "persistent")]
    #[default]
    Persistent,
}

#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpOauthFlowStoreCallbackKind {
    #[serde(rename = "callback")]
    #[default]
    Callback,
}

/// Credential owner for a host-driven OAuth flow.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum McpOauthFlowStore {
    Session(McpOauthFlowStoreSession),
    Memory(McpOauthFlowStoreMemory),
    Persistent(McpOauthFlowStorePersistent),
    Callback(McpOauthFlowStoreCallback),
}

#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpOauthTestConnectionResultConnectedStatus {
    #[serde(rename = "connected")]
    #[default]
    Connected,
}

#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpOauthTestConnectionResultNeedsAuthStatus {
    #[serde(rename = "needs-auth")]
    #[default]
    NeedsAuth,
}

#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpOauthTestConnectionResultFailedStatus {
    #[serde(rename = "failed")]
    #[default]
    Failed,
}

/// Outcome of the tokenless handshake, with the original challenge when available.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum McpOauthTestConnectionResult {
    Connected(McpOauthTestConnectionResultConnected),
    NeedsAuth(McpOauthTestConnectionResultNeedsAuth),
    Failed(McpOauthTestConnectionResultFailed),
}

/// Remote protocol selected for the tokenless handshake.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpOauthTestConnectionTransport {
    /// Streamable HTTP.
    #[serde(rename = "http")]
    Http,
    /// Legacy HTTP with server-sent events.
    #[serde(rename = "sse")]
    Sse,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

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
    ContextTier, IdeConnectedInfo, IdeSelection, IndexedSearchState, ManagedPermissionsContext,
    ManagedSettingsResolvedSource, McpOauthHttpResponse, McpOauthWWWAuthenticateParams,
    McpServerMetadata, McpServerSource, McpServerStatus, McpSkillOrigin, ModelChangeSource,
    ModelProviderKind, ModelProviderRef, OmittedBinaryOmittedReason, PermissionDecisionSource,
    PermissionMode, PermissionPromptRequest, PermissionRule, PolicyHelperCapabilities,
    PolicyHelperFailure, ProviderQuotaState, ReasoningSummary, RemediationAction,
    SessionLimitsConfig, SessionMode, ShutdownType, SkillSource, TaskBlocker,
    TaskCompletionOutcome, UsageGetMetricsResult, UserToolSessionApproval, Verbosity,
};

use super::sdk_api_shared::*;
use crate::generated::{lifecycle_diagnostics, session_events};
use crate::types::{RequestId, SessionEvent, SessionId};

/// Authenticated account entry returned by `account.getAllUsers`.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AccountAllUsers {
    /// Authentication information for this user
    pub auth_info: AuthInfo,
    /// Opaque identifier accepted by account and model selection APIs
    #[serde(skip_serializing_if = "Option::is_none")]
    pub selection_id: Option<String>,
    /// Associated token, if available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub token: Option<String>,
}

/// The authentication identity whose shared wording should be rendered.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct AccountDescribeAuthInfoRequest {
    /// Identity to describe.
    pub identity: AuthIdentity,
}

/// The wording the runtime uses for an authentication identity.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct AccountDescribeAuthInfoResult {
    /// One-line description of the identity, suitable after a verb such as "Switched to".
    pub description: String,
    /// Sign-in banner announcing the authenticated user.
    pub message: String,
}

/// Current authentication state
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AccountGetCurrentAuthResult {
    /// Authentication errors from the last auth attempt, if any
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auth_errors: Option<Vec<String>>,
    /// Current authentication information, if authenticated
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auth_info: Option<AuthInfo>,
}

/// The authentication identity whose gist support should be reported.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct AccountGetGistSupportRequest {
    /// Identity to evaluate.
    pub identity: AuthIdentity,
}

/// Whether the identity can create gists, and why not when it cannot.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct AccountGetGistSupportResult {
    /// Machine-readable reason category when unsupported.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub kind: Option<String>,
    /// Human-readable explanation to show where the gist action would have been.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reason: Option<String>,
    /// True when the identity can create gists.
    pub supported: bool,
}

/// Configured names of the GitHub host to resolve.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct AccountGetHostUrlRequest {
    /// Host from the Copilot-specific setting, which wins when both are set. A bare hostname is normalized to `https://`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub copilot_gh_host: Option<String>,
    /// Host from the general GitHub CLI setting, used when the Copilot-specific one is absent.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub gh_host: Option<String>,
}

/// Absolute base URL of the GitHub host.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct AccountGetHostUrlResult {
    /// Absolute base URL, for example `https://github.com`.
    pub url: String,
}

/// Optional opaque account selection or compatibility GitHub token used to look up quota.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AccountGetQuotaRequest {
    /// GitHub token accepted for compatibility with existing SDK clients. When provided, resolves this token instead of using the current account.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub git_hub_token: Option<String>,
    /// Opaque account identifier returned by `account.getAllUsers`. When omitted, the current account is used.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub selection_id: Option<String>,
}

/// Quota usage snapshot for a Copilot quota type, including entitlement, used requests, overage, reset date, and remaining percentage.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AccountQuotaSnapshot {
    /// Number of requests included in the entitlement, or -1 for unlimited entitlements
    pub entitlement_requests: i64,
    /// Whether the user has an unlimited usage entitlement
    pub is_unlimited_entitlement: bool,
    /// Number of additional usage requests made this period
    pub overage: f64,
    /// Whether additional usage is allowed when quota is exhausted
    pub overage_allowed_with_exhausted_quota: bool,
    /// Percentage of entitlement remaining
    pub remaining_percentage: f64,
    /// Date when the quota resets (ISO 8601 string)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reset_date: Option<String>,
    /// Whether usage is still permitted after quota exhaustion
    pub usage_allowed_with_exhausted_quota: bool,
    /// Number of requests used so far this period
    pub used_requests: i64,
}

/// Quota usage snapshots for the resolved user, keyed by quota type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AccountGetQuotaResult {
    /// Quota snapshots keyed by type (e.g., chat, completions, premium_interactions)
    pub quota_snapshots: HashMap<String, AccountQuotaSnapshot>,
}

/// The environment variable supplying a classic personal access token, when one is set.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct AccountGetTokenEnvVarResult {
    /// Variable name, for example `GH_TOKEN`. Absent when no classic token is present in the environment.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
}

/// A path to resolve against the GitHub host an identity is signed in to.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct AccountGetWebUrlRequest {
    /// Identity whose host the path resolves against. Omit to resolve against the public github.com host.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub identity: Option<AuthIdentity>,
    /// Host-relative path, without a leading slash, for example `settings/copilot/features`.
    pub path: String,
}

/// An absolute URL on the identity's GitHub host.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct AccountGetWebUrlResult {
    /// Absolute URL for the requested path.
    pub url: String,
}

/// Credentials to validate and store. Omit login to resolve the authenticated user from the token.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AccountLoginRequest {
    /// GitHub host URL
    pub host: String,
    /// User login/username. When omitted, the runtime validates the token and resolves the login from GitHub.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub login: Option<String>,
    /// GitHub authentication token
    pub token: String,
}

/// Result of a successful login; throws on failure
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AccountLoginResult {
    /// Whether the credential was persisted to a secure store (system keychain, or the config file when plaintext storage is enabled). False when no secure store was available and the token was not saved, so the consumer can decide how to proceed.
    pub stored_in_vault: bool,
}

/// User to log out
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AccountLogoutRequest {
    /// Authentication information for the user to log out
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auth_info: Option<AuthInfo>,
    /// Opaque account identifier returned by `account.getAllUsers`
    #[serde(skip_serializing_if = "Option::is_none")]
    pub selection_id: Option<String>,
}

/// Logout result indicating if more users remain
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AccountLogoutResult {
    /// Whether other authenticated users remain after logout
    pub has_more_users: bool,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AuthEnumerateQueryAccounts {
    /// Account-collection query variant discriminator.
    pub kind: AuthEnumerateQueryAccountsKind,
    /// Whether to discard the cached inventory and re-run credential reads and availability checks for every account before returning the canonical account projection.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub refresh: Option<bool>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AuthEnumerateQueryProviders {
    /// Whether an interactive Entra broker is available on the host; gates Entra availability in the returned list.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub broker_available: Option<bool>,
    /// Account-collection query variant discriminator.
    pub kind: AuthEnumerateQueryProvidersKind,
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
pub struct AuthEnumerateQueryLoginAccounts {
    /// Account-collection query variant discriminator.
    pub kind: AuthEnumerateQueryLoginAccountsKind,
    /// The provider whose cached base identities should be listed.
    pub provider: LoginProviderKind,
}

/// Enumerate request carrying the typed collection query.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AccountsEnumerateRequest {
    /// Which typed accounts collection to enumerate.
    pub query: AuthEnumerateQuery,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AuthReadQueryActiveAccount {
    /// Account read-datum query variant discriminator.
    pub kind: AuthReadQueryActiveAccountKind,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AuthReadQueryStatus {
    /// Account read-datum query variant discriminator.
    pub kind: AuthReadQueryStatusKind,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AuthReadQueryLastErrors {
    /// Account read-datum query variant discriminator.
    pub kind: AuthReadQueryLastErrorsKind,
}

/// Read request carrying the typed datum query.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AccountsGetRequest {
    /// Which typed accounts datum to read.
    pub query: AuthReadQuery,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AuthWriteSwitchActive {
    /// Account mutation command variant discriminator.
    pub kind: AuthWriteSwitchActiveKind,
    /// Opaque selection id of the account to make active.
    pub selection_id: String,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AuthWriteLogout {
    /// Stable account id of the account to log out. Use this for canonical account entries that are not switchable.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub account_id: Option<String>,
    /// Account mutation command variant discriminator.
    pub kind: AuthWriteLogoutKind,
    /// Opaque selection id of the account to log out; absent logs out the active account.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub selection_id: Option<String>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AuthWriteSetCredentials {
    /// Authentication host URL.
    pub host: String,
    /// Account mutation command variant discriminator.
    pub kind: AuthWriteSetCredentialsKind,
    /// Login/username for the credential.
    pub login: String,
    /// GitHub authentication token to install.
    pub token: String,
}

/// Mutation request carrying the typed write command.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AccountsSetRequest {
    /// The non-interactive mutation command to apply.
    pub command: AuthWrite,
}

/// One canonical signed-in account entry shared by account-list and account-switch surfaces.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AccountStatus {
    /// Stable opaque account id used for forest lineage and account management.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub account_id: Option<String>,
    /// Whether this is the active account.
    pub active: bool,
    /// Human-readable credential source used to authenticate this account, such as `gh` or `GITHUB_TOKEN`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auth_source: Option<String>,
    /// Opaque accountId of the account this one was derived from; absent for a root account.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub derived_from: Option<String>,
    /// Canonical availability treatment for this account.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub health: Option<AccountHealth>,
    /// Authentication host URL.
    pub host: String,
    /// The provider kind of this account.
    pub kind: AccountKind,
    /// Human-readable identity and provider label owned by the runtime.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub label: Option<String>,
    /// Authenticated login/username.
    pub login: String,
    /// Opaque selection id accepted by switchActive. Absent when switching is not currently valid.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub selection_id: Option<String>,
    /// Human-readable availability or recovery reason.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub status_message: Option<String>,
    /// Whether the account is currently a valid switch target.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub switchable: Option<bool>,
}

/// The authentication identity to enroll in Copilot Free.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct AccountSubscribeCopilotFreeRequest {
    /// Identity to enroll, in the shape `session.gitHubAuth.getCurrentAuthInfo` returns. The runtime resolves the credential for it, so no credential is sent.
    pub identity: AuthIdentity,
}

/// What came of a Copilot Free enrollment attempt.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct AccountSubscribeCopilotFreeResult {
    /// `subscribed` when GitHub answered, and `status` then carries its answer. `no-token` when no credential could be resolved for the identity, which is the caller's cue to ask the user to sign in again rather than to report a failure. `failed` when the attempt itself errored, and `message` then explains it.
    pub kind: String,
    /// Explanation to show the user. Present on `failed`, and optionally alongside a `subscribed` status.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub message: Option<String>,
    /// GitHub's enrollment status when `kind` is `subscribed`, for example `subscribed` or `already_subscribed`. Absent otherwise.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub status: Option<String>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AuthEnumerateValueAccounts {
    /// The signed-in account forest; empty when not logged in.
    pub items: Vec<AccountStatus>,
    /// Enumerated account-collection variant discriminator.
    pub kind: AuthEnumerateValueAccountsKind,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AuthEnumerateValueProviders {
    /// The providers offered for interactive login.
    pub items: Vec<ProviderDescriptor>,
    /// Enumerated account-collection variant discriminator.
    pub kind: AuthEnumerateValueProvidersKind,
}

/// A cached base identity a provider can reuse for login without asking the user to choose an account again.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AuthLoginIdentity {
    /// Opaque provider-owned identity id. Pass it back to accounts.login.begin; consumers must not parse it.
    pub account_id: String,
    /// Whether this base identity owns the session's active derived account.
    pub active: bool,
    /// Human-readable sign-in label owned by the runtime.
    pub label: String,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AuthEnumerateValueLoginAccounts {
    /// Cached base identities reusable by the selected login provider, active first.
    pub items: Vec<AuthLoginIdentity>,
    /// Enumerated account-collection variant discriminator.
    pub kind: AuthEnumerateValueLoginAccountsKind,
}

/// Advance an in-flight login flow, optionally fulfilling an input-required step.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AuthLoginAdvanceRequest {
    /// Opaque flow id from begin.
    pub flow_id: String,
    /// Neutral input fulfilling a preceding input-required step (e.g. a GHEC host); ignored otherwise.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub input: Option<String>,
}

/// Begin an interactive login flow for a provider kind, optionally reusing a cached base identity or explicitly requesting provider account selection.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AuthLoginBeginRequest {
    /// The provider kind to sign in with.
    pub kind: LoginProviderKind,
}

/// Extensible [`AuthLoginBeginRequest`], including inputs added after it was published.
///
/// Required inputs are [`AuthLoginBeginOptions::new`] arguments; optional inputs have fluent setters.
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
pub struct AuthLoginBeginOptions {
    #[serde(flatten)]
    legacy: AuthLoginBeginRequest,
    #[serde(skip_serializing_if = "Option::is_none")]
    method: Option<AuthLoginMethod>,
    #[serde(skip_serializing_if = "Option::is_none")]
    mode: Option<AuthLoginMode>,
    #[serde(skip_serializing_if = "Option::is_none")]
    account_id: Option<String>,
}

impl AuthLoginBeginOptions {
    /// Creates options with the required inputs.
    pub fn new(kind: LoginProviderKind) -> Self {
        Self {
            legacy: AuthLoginBeginRequest { kind },
            method: None,
            mode: None,
            account_id: None,
        }
    }

    /// How to authorize: `web` (default) or `device-code`. Device code is offered for `githubDotCom` and `proxima`; `proxima` still asks for its host through `input-required` first.
    pub fn method(mut self, value: AuthLoginMethod) -> Self {
        self.method = Some(value);
        self
    }

    /// Neutral identity-selection intent. Omitted is equivalent to default.
    pub fn mode(mut self, value: AuthLoginMode) -> Self {
        self.mode = Some(value);
        self
    }

    /// Opaque cached base identity id returned by accounts.enumerate loginAccounts. Valid only with default mode; consumers must not parse it.
    pub fn account_id(mut self, value: impl Into<String>) -> Self {
        self.account_id = Some(value.into());
        self
    }
}

/// A started login flow: its opaque id and first step.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AuthLoginBegun {
    /// Opaque flow id used to advance or cancel this login.
    pub flow_id: String,
    /// The first step of the flow.
    pub step: AuthLoginStep,
}

/// Cancel an in-flight login flow.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AuthLoginCancelRequest {
    /// Opaque flow id from begin.
    pub flow_id: String,
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
pub struct AuthReadValueActiveAccount {
    /// The active account, or absent when not logged in.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub account: Option<AccountStatus>,
    /// Credential-free identity metadata for the active account, including resolved Copilot user information when available.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auth_info: Option<AuthIdentity>,
    /// Opaque credential selection id for the active authentication. This remains available when the canonical account is not currently switchable.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auth_selection_id: Option<String>,
    /// Account read-datum variant discriminator.
    pub kind: AuthReadValueActiveAccountKind,
}

/// Neutral authentication status summary.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AuthStatusDto {
    /// Number of signed-in accounts in the roster.
    pub account_count: i64,
    /// Active account host, if authenticated.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub active_host: Option<String>,
    /// Active account login, if authenticated.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub active_login: Option<String>,
    /// Copilot plan tier of the active account, if known.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub copilot_plan: Option<String>,
    /// Whether the session has resolved authentication.
    pub is_authenticated: bool,
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
pub struct AuthReadValueStatus {
    /// Account read-datum variant discriminator.
    pub kind: AuthReadValueStatusKind,
    /// The neutral authentication status summary.
    pub status: AuthStatusDto,
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
pub struct AuthReadValueLastErrors {
    /// Validation errors from the most recent authentication attempt.
    pub errors: Vec<AuthValidationError>,
    /// Account read-datum variant discriminator.
    pub kind: AuthReadValueLastErrorsKind,
}

/// Result of a non-interactive accounts mutation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AuthWriteResult {
    /// For a logout, whether other signed-in accounts remain.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub more_users: Option<bool>,
    /// Whether the mutation was applied.
    pub ok: bool,
}

/// Runtime-to-owner cancellation request for a client-owned task.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ClientTaskCancelRequest {
    /// Opaque identifier shared by coalesced cancellation callers
    pub cancellation_id: String,
    /// Owner-scoped task key included for correlation
    pub client_task_id: String,
    /// Canonical runtime-generated task identifier
    pub id: String,
    /// Reason the runtime requests cancellation
    pub reason: ClientTaskCancelReason,
    /// Session that owns the client task
    pub session_id: SessionId,
}

/// Whether the client authoritatively confirmed its external work stopped.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ClientTaskCancelResult {
    /// True only when the owner confirms that external work stopped before responding
    pub cancelled: bool,
}

/// One retained session-scoped diagnostic record. Potentially content-bearing diagnostic data is opt-in and must not be exported automatically as telemetry.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DiagnosticEntry {
    /// Agent identifier for a subagent host. Omitted for the root agent.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub agent_id: Option<String>,
    /// Typed MCP diagnostic detail.
    pub details: McpDiagnosticDetails,
    /// Severity of this emitted diagnostic record.
    pub level: DiagnosticSeverity,
    /// Human-readable diagnostic summary.
    pub message: String,
    /// Original byte count when a known-size message or data value was truncated.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub original_bytes: Option<i64>,
    /// Diagnostic source identifying the typed details payload.
    pub source: DiagnosticEntrySource,
    /// UTC RFC 3339 timestamp captured at the diagnostic source.
    pub timestamp: String,
    /// Whether message or data was truncated to the record-size bound.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub truncated: Option<bool>,
}

/// Per-source session diagnostics configuration.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DiagnosticsConfiguration {
    /// Diagnostic thresholds keyed by supported source.
    pub sources: DiagnosticSourcesConfiguration,
}

/// Patch session diagnostic thresholds for explicitly supplied sources.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DiagnosticsConfigureRequest {
    /// Sources to configure. At least one supported source must be supplied.
    pub sources: DiagnosticSourcesConfiguration,
}

/// Cursor-based request for session diagnostics. The default limit is 100 (maximum 500); the default waitMs is zero (maximum 30000).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DiagnosticsReadRequest {
    /// Opaque cursor returned by an earlier read. Omit to start at the oldest retained record.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cursor: Option<String>,
    /// Maximum number of records to return, from 1 through 500. Omit for 100.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max: Option<i64>,
    /// Nonempty selection of sources to read. Each source may be listed once.
    pub sources: Vec<DiagnosticSource>,
    /// Maximum time in milliseconds to wait for a new record, from 0 through 30000.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub wait_ms: Option<i32>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DiagnosticsReadResultEntriesItem {
    /// Agent identifier for a subagent host. Omitted for the root agent.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub agent_id: Option<String>,
    /// Typed MCP diagnostic detail.
    pub details: McpDiagnosticDetails,
    /// Severity of this emitted diagnostic record.
    pub level: DiagnosticSeverity,
    /// Human-readable diagnostic summary.
    pub message: String,
    /// Original byte count when a known-size message or data value was truncated.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub original_bytes: Option<i64>,
    /// Diagnostic source identifying the typed details payload.
    pub source: DiagnosticsReadResultEntriesItemSource,
    /// UTC RFC 3339 timestamp captured at the diagnostic source.
    pub timestamp: String,
    /// Whether message or data was truncated to the record-size bound.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub truncated: Option<bool>,
}

/// One cursor-addressed page of retained session diagnostics.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DiagnosticsReadResult {
    /// Opaque cursor for the next independent read.
    pub cursor: String,
    /// Whether the requested cursor remained within the retained buffer window.
    pub cursor_status: DiagnosticCursorStatus,
    /// Number of records lost before this page when known. Omitted when a buffer generation change makes the count unknowable.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub dropped_count: Option<i64>,
    /// Retained records beginning at the requested cursor.
    pub entries: Vec<DiagnosticsReadResultEntriesItem>,
    /// Whether additional retained records follow this page.
    pub has_more: bool,
}

/// The remote the checked-out branch tracks.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GitCurrentBranchRemoteResult {
    /// Name of the tracked remote. Reports `origin` whenever the working tree has no tracking configuration to read, including on a detached HEAD, so this is never null and never empty.
    pub remote: String,
}

/// Working-tree path a git query applies to.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GitCwdRequest {
    /// Absolute path to a directory inside the git working tree to query.
    pub cwd: String,
}

/// One check reported against a pull request's head commit.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GitHubPullRequestCheck {
    /// Name the check reports itself under.
    pub name: String,
    /// Rolled-up state of the check.
    #[doc(hidden)]
    pub(crate) status: GitHubPullRequestCheckStatus,
}

/// The pull request a branch is open against, with the review state a caller needs to summarize it without a second request.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GitHubBranchPullRequest {
    /// Lines added across the pull request.
    pub additions: i64,
    /// Pull request description body. Empty when the pull request has none.
    pub body: String,
    /// Number of files the pull request changes.
    pub changed_files: i64,
    /// Checks reported against the pull request's head commit.
    #[doc(hidden)]
    pub(crate) checks: Vec<GitHubPullRequestCheck>,
    /// Number of comments on the pull request.
    pub comments: i64,
    /// Lines deleted across the pull request.
    pub deletions: i64,
    /// Head branch name the pull request was opened from.
    pub head_ref_name: String,
    /// Whether the pull request is a draft.
    pub is_draft: bool,
    /// Label names applied to the pull request.
    pub labels: Vec<String>,
    /// Pull request number within its repository.
    pub number: i64,
    /// Owner login of the repository the pull request was opened against.
    pub owner: String,
    /// Name of the repository the pull request was opened against.
    pub repo: String,
    /// Logins of the requested reviewers.
    pub reviewers: Vec<String>,
    /// Pull request state as GitHub reports it, such as `OPEN`, `CLOSED` or `MERGED`.
    pub state: String,
    /// Pull request title.
    pub title: String,
    /// Web URL of the pull request.
    pub url: String,
}

/// Owner and name of a GitHub repository. Deliberately distinct from `GitHubRepositoryIdentity`, which also carries a host: a by-id lookup takes the host as an input and the API's answer reports none, so there is no host here to report faithfully.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GitHubRepositoryOwnerName {
    /// Repository name, without the owner prefix or the `.git` suffix.
    pub name: String,
    /// Repository owner login (user or organization).
    pub owner: String,
}

/// Selects the pull request a branch is open against, and the credential to look it up under. The branch is searched across every repository in `baseRepositories`, which lets a fork's branch find the pull request it opened against upstream.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GitHubBranchPullRequestLookupRequest {
    /// The credential the lookup runs under, carried opaquely because its shape is the host's own and the runtime only resolves a token and a GitHub host from it. Required: the search answers per-account, so a pull request invisible to this credential is reported as absent rather than refused.
    pub auth_info: serde_json::Value,
    /// Repositories to search for the pull request, in preference order. A fork supplies both its own repository and its upstream, because the branch may have opened the pull request against either.
    #[doc(hidden)]
    pub(crate) base_repositories: Vec<GitHubRepositoryOwnerName>,
    /// Head branch name to find the pull request for, without a `refs/heads/` prefix.
    pub branch: String,
    /// Login that must own the pull request's head branch. A branch name alone is ambiguous across forks, so a candidate whose head is owned by anyone else is discarded.
    pub head_owner: String,
    /// Whether a closed or merged pull request counts as a match. False considers only open ones, so a caller acting on the branch's current pull request does not resurface a merged one.
    pub include_closed: bool,
}

/// Outcome of a branch pull-request lookup. `status` carries why, because 'not authenticated', 'no pull request' and 'the search failed' are three different answers and only the first two are conclusive.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GitHubBranchPullRequestLookupResult {
    /// The matching pull request, or null for every status other than `found`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub pull_request: Option<GitHubBranchPullRequest>,
    /// Why the lookup reported what it did.
    #[doc(hidden)]
    pub(crate) status: GitHubBranchPullRequestLookupStatus,
}

/// Whether cancellation reached a registered GitHub request.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GitHubFetchCancelResult {
    /// True when cancellation reached a registered request.
    pub canceled: bool,
}

/// One HTTP header in an ordered list.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GitHubFetchHeader {
    /// HTTP header name. The list preserves order and repeated names.
    pub name: String,
    /// HTTP header value.
    pub value: String,
}

/// A user-facing HTTP failure formatted in the runtime.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GitHubFetchHttpError {
    /// Complete user-facing error text.
    pub message: String,
    /// Parsed response body serialized as JSON, when available.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub response_body_json: Option<String>,
}

/// Arguments for a named authenticated operation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GitHubFetchOperationRequest {
    /// Opaque selector for a live host-owned credential binding. Only the owning host connection may use it.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auth_owner_id: Option<String>,
    /// JSON request body, preserved as supplied.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub body_json: Option<String>,
    /// The operation to execute.
    #[doc(hidden)]
    pub(crate) operation: GitHubFetchOperation,
    /// Named operation arguments, such as owner, repo, or jobId.
    pub params: HashMap<String, String>,
    /// A fresh request id.
    pub request_id: i64,
    /// Session whose auth manager to use. Omission selects the process manager.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_id: Option<SessionId>,
}

/// A pre-resolved address for a GitHub request.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct GitHubFetchPinnedAddress {
    /// Address the network transport must use.
    pub address: String,
}

/// Proxy environment for one authenticated GitHub request.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct GitHubFetchProxyEnvironment {
    /// Proxy for HTTP requests.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub http_proxy: Option<String>,
    /// Proxy for HTTPS requests.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub https_proxy: Option<String>,
    /// Hosts that bypass the proxy.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub no_proxy: Option<String>,
}

/// Optional overrides for the runtime retry policy.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GitHubFetchRetryConfig {
    /// Delay multiplier; the runtime default is two.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub backoff_factor: Option<f64>,
    /// Initial retry delay; the runtime default is five seconds.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub default_retry_delay_seconds: Option<f64>,
    /// Whether repeated failures open the runtime circuit breaker. Defaults to true.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_circuit_breaker: Option<bool>,
    /// Maximum retries; the runtime default is five.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_retries: Option<i64>,
}

/// A GitHub API request without caller-supplied auth identity or credential fields.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GitHubFetchRequest {
    /// Whether the request permits loopback addresses. Defaults to true.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub allow_localhost: Option<bool>,
    /// API label used in runtime diagnostics.
    pub api_name: String,
    /// Opaque selector for a live host-owned credential binding. Only the owning host connection may use it.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auth_owner_id: Option<String>,
    /// Request bytes encoded as base64. Absent for a request without a body.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub body_base64: Option<String>,
    /// Maximum response body size in bytes.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub body_limit_bytes: Option<i64>,
    /// Whether the request bypasses idle connections.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub disable_connection_reuse: Option<bool>,
    /// Caller-supplied HTTP headers.
    #[doc(hidden)]
    pub(crate) headers: Vec<GitHubFetchHeader>,
    /// HTTP method.
    pub method: String,
    /// Addresses to pin for this request, without another DNS lookup.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub pinned_addresses: Option<Vec<GitHubFetchPinnedAddress>>,
    /// Explicit proxy environment. Omission uses the process environment.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub proxy_env: Option<GitHubFetchProxyEnvironment>,
    /// Whether the proxy uses Negotiate authentication.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub proxy_negotiate_auth: Option<bool>,
    /// Optional service principal for proxy Negotiate authentication.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub proxy_negotiate_spn: Option<String>,
    /// Response read timeout in milliseconds. Defaults to 700000.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub read_timeout_ms: Option<i64>,
    /// Redirect behavior. Defaults to following redirects.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub redirect: Option<String>,
    /// A fresh request id.
    pub request_id: i64,
    /// Optional retry overrides.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) retry_options: Option<GitHubFetchRetryConfig>,
    /// Session whose auth manager to use. Omission selects the process manager.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_id: Option<SessionId>,
    /// Total request timeout in milliseconds.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub timeout_ms: Option<i64>,
    /// Absolute URL on the resolved GitHub API origin.
    pub url: String,
}

/// One registered GitHub request, shared by execution and cancellation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GitHubFetchRequestId {
    /// One fresh id from `gitHubFetch.allocateRequestId`.
    pub request_id: i64,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct GitHubFetchResultResponse {
    /// Complete response bytes encoded as base64.
    pub body_base64: String,
    /// Response headers.
    #[doc(hidden)]
    pub(crate) headers: Vec<GitHubFetchHeader>,
    /// Formatted non-success response for a named operation.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) http_error: Option<GitHubFetchHttpError>,
    pub kind: GitHubFetchResultResponseKind,
    /// HTTP status, including non-success responses.
    pub status: i32,
    /// HTTP status text.
    pub status_text: String,
    /// Final response URL.
    pub url: String,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct GitHubFetchResultCircuitBreaker {
    /// Host whose breaker is open.
    pub host: String,
    pub kind: GitHubFetchResultCircuitBreakerKind,
    /// Complete circuit-breaker message.
    pub message: String,
    /// Remaining backoff in milliseconds.
    pub time_until_retry_ms: i64,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct GitHubFetchResultProxyResponse {
    pub kind: GitHubFetchResultProxyResponseKind,
    /// Complete proxy failure message.
    pub message: String,
    /// HTTP proxy status.
    pub status: i32,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct GitHubFetchResultFailed {
    pub kind: GitHubFetchResultFailedKind,
    /// Authentication, transport, or cancellation error.
    pub message: String,
}

/// A GitHub login the authenticated user may act as: their own account, or an organization they belong to.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GitHubOwnerOption {
    /// The owner's GitHub login.
    pub login: String,
    /// Which kind of owner this is. The authenticated user's own account is always reported as `user`.
    pub r#type: String,
}

/// The owner listing to abandon.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GitHubOwnersCancelRequest {
    /// Request id the listing was started with.
    pub request_id: i64,
}

/// Whether the id named a running owner listing.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GitHubOwnersCancelResult {
    /// True when a listing with the id was running and the cancel stopped it. False when the id was never registered, was registered but unused, was released after being abandoned, or its listing had ended. An unused id is released and cannot start a later listing.
    pub canceled: bool,
}

/// Credential to list owners under, and the request id that makes the listing cancellable.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GitHubOwnersListRequest {
    /// The credential the listing runs under, carried opaquely because its shape is the host's own and the runtime only resolves a token and a GitHub host from it. No credential travels: this selects one the runtime already holds.
    pub auth_info: serde_json::Value,
    /// Request id from `gitHubOwners.nextRequestId`. An id that was never registered, canceled before use, released after being abandoned, or already used is refused rather than silently running uncancellable.
    pub request_id: i64,
}

/// Outcome of an owner listing. Exactly one of `owners` and `message` is present, except that `throwError` reports a failure the caller is expected to raise rather than render.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GitHubOwnersListResult {
    /// Why no owners could be listed, phrased for a user. Present when the listing failed in a way the caller should render rather than raise.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub message: Option<String>,
    /// The owners, on success: the authenticated user first, then the organizations they belong to.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) owners: Option<Vec<GitHubOwnerOption>>,
    /// A malformed request or an unreadable credential, which the caller raises instead of rendering. Kept a field rather than a dispatch error so it stays distinct from `message`, which the caller renders.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub throw_error: Option<String>,
    /// A line the caller should log. Present only alongside `message`, and only for failures worth recording.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub warning: Option<String>,
}

/// A freshly registered request id. Registering it before the listing starts is what lets a cancel that races the request still find the owner listing slot. The id serves one listing only. Long-abandoned unused ids can be released by later allocations.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GitHubOwnersRequestIdResult {
    /// Request id to pass to `gitHubOwners.list` and, to abandon it, `gitHubOwners.cancel`.
    pub request_id: i64,
}

/// An unused release allocation that this caller will never dispatch. This is separate from cancellation before a request arrives.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GitHubReleaseAbandonRequestParams {
    /// The unused request id owned by this connection.
    pub request_id: i64,
}

/// Whether the runtime retired an unused allocation without affecting active work.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GitHubReleaseAbandonRequestResult {
    /// True only when this connection owned an unclaimed allocation with no active guard. False for active, foreign, unknown or previously retired ids. A successful abandonment prevents any later use of that id.
    pub abandoned: bool,
}

/// The release lookup or asset download to abandon.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GitHubReleaseCancelRequest {
    /// Request id the lookup or download was started with.
    pub request_id: i64,
}

/// Whether the cancel reached live work.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GitHubReleaseCancelResult {
    /// True when the cancel reached a registered lookup or download; that call's own result still says how it ended. False when the id names none, which is the normal answer for work that already finished.
    pub canceled: bool,
}

/// Which release asset to download, the credential to download it under, and the request id that makes the download cancellable.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GitHubReleaseDownloadAssetRequest {
    /// The asset's `browser_download_url` from the release JSON: an HTTPS release download URL of a repository that `gitHubRelease.lookup` serves.
    pub asset_url: String,
    /// The credential the download runs under, carried opaquely because its shape is the host's own and the runtime only resolves a token from it. No credential travels: this selects one the runtime already holds. Omitted when the caller has no credential, which still succeeds against the public release repository at a lower rate limit.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auth_info: Option<serde_json::Value>,
    /// Expected SHA-256 of the asset: 64 hex digits in either case, or the release asset's `digest` value as is (`sha256:` and the hex digits). When given and the downloaded bytes differ, the file is removed and the result is `digest-mismatch`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub expected_sha256: Option<String>,
    /// Request id from `gitHubRelease.allocateRequestId`. An id that was never registered, or one already used, is refused rather than silently running uncancellable.
    pub request_id: i64,
    /// Snapshot request id from this connection. The download retains that snapshot's original credential source instead of authInfo. Missing, foreign, canceled, or revoked references fail without fallback. Omission preserves ordinary downloads. The download still uses its own requestId for progress and cancellation.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub snapshot_request_id: Option<i64>,
}

/// Outcome of an asset download. Each field is present only for the kinds its description names.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GitHubReleaseDownloadAssetResult {
    /// How the download ended.
    #[doc(hidden)]
    pub(crate) kind: GitHubReleaseDownloadKind,
    /// Why the download produced no file, phrased for a user, for `digest-mismatch` and `failed`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub message: Option<String>,
    /// Absolute path of the downloaded file, for `downloaded`. The file is alone in a fresh directory that the caller deletes once it has used the file.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub path: Option<String>,
    /// SHA-256 of the downloaded bytes as lowercase hex, for `downloaded` and `digest-mismatch`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sha256: Option<String>,
    /// Size of the downloaded file in bytes, for `downloaded`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub size_bytes: Option<i64>,
    /// HTTP status of the final response, for `http-error`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub status: Option<i64>,
    /// HTTP status text of the final response, for `http-error`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub status_text: Option<String>,
}

/// The download whose progress to report.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GitHubReleaseDownloadProgressRequest {
    /// Request id the download was started with.
    pub request_id: i64,
}

/// Byte progress of a running download.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GitHubReleaseDownloadProgressResult {
    /// Bytes written so far.
    pub received_bytes: i64,
    /// Size the server announced for the body. Omitted when it announced none.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub total_bytes: Option<i64>,
}

/// Which release to look up, the credential to look it up under, and the request id that makes the lookup cancellable.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GitHubReleaseLookupRequest {
    /// The credential the lookup runs under, carried opaquely because its shape is the host's own and the runtime only resolves a token and a GitHub host from it. No credential travels: this selects one the runtime already holds. Omitted when the caller has no credential, which still succeeds against the public release repository at a lower rate limit.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auth_info: Option<serde_json::Value>,
    /// Which release to look up.
    #[doc(hidden)]
    pub(crate) query: GitHubReleaseQuery,
    /// Request id from `gitHubRelease.allocateRequestId`. An id that was never registered, or one already used, is refused rather than silently running uncancellable.
    pub request_id: i64,
    /// The tag to look up. Required by the `tag` query and ignored by the others.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub value: Option<String>,
}

/// Outcome of a release lookup. `releaseJson` is present only for `found`, and `message` only for the kinds that carry an explanation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GitHubReleaseLookupResult {
    /// How the lookup ended.
    #[doc(hidden)]
    pub(crate) kind: GitHubReleaseLookupKind,
    /// Why the lookup produced no release, phrased for a user.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub message: Option<String>,
    /// The release as GitHub's own JSON, verbatim. Carried as text rather than a modelled type so the contract does not have to track every field of GitHub's release payload, and so a caller reads the same shape the REST API documents.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub release_json: Option<String>,
}

/// A freshly registered request id. Registering it before the work starts is what lets a cancel that races the request still find something to cancel.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GitHubReleaseRequestIdResult {
    /// Request id to pass to `gitHubRelease.lookup` or `gitHubRelease.downloadAsset` and, to abandon the work, `gitHubRelease.cancel`.
    pub request_id: i64,
}

/// An asset in the release snapshot. GitHub field names remain unchanged.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GitHubReleaseSnapshotAsset {
    /// The published download URL.
    #[serde(rename = "browser_download_url")]
    pub browser_download_url: String,
    /// The published asset filename.
    pub name: String,
}

/// Release facts needed for update selection and admission. GitHub field names remain unchanged.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GitHubReleaseSnapshotEntry {
    /// The published release assets.
    #[doc(hidden)]
    pub(crate) assets: Vec<GitHubReleaseSnapshotAsset>,
    /// Whether GitHub marks this release as a draft.
    pub draft: bool,
    /// Whether GitHub marks this release as a prerelease.
    pub prerelease: bool,
    /// The release tag.
    #[serde(rename = "tag_name")]
    pub tag_name: String,
}

/// Immutable options for one connection-owned release snapshot.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GitHubReleaseSnapshotRequest {
    /// The version embedded in the caller's build.
    pub build_version: String,
    /// Optional runtime version override. Invalid or noncanonical values do not replace buildVersion.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cli_version_override: Option<String>,
    /// The embedded package identity. Omission uses the existing development-version fallback rule.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) package_identity: Option<GitHubReleasePackageIdentity>,
    /// An unused id from gitHubRelease.allocateRequestId. Reuse the same id and options to read the same snapshot.
    pub request_id: i64,
    /// The release channel used for update selection.
    #[doc(hidden)]
    pub(crate) update_channel: GitHubReleaseUpdateChannel,
}

/// The release lookup succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GitHubReleaseSnapshotResultSuccess {
    /// The newest release in the selected update channel, when one exists.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) automatic_update_candidate: Option<GitHubReleaseSnapshotEntry>,
    /// Whether the running release is eligible for release admission.
    pub eligible: bool,
    /// Up to three stable releases, newest first.
    #[doc(hidden)]
    pub(crate) latest_stable_releases: Vec<GitHubReleaseSnapshotEntry>,
    /// The original lookup duration in milliseconds.
    pub lookup_duration_ms: f64,
    pub outcome: GitHubReleaseSnapshotResultSuccessOutcome,
    /// Prereleases newer than the newest stable release, newest first.
    #[doc(hidden)]
    pub(crate) prereleases_newer_than_latest_stable: Vec<GitHubReleaseSnapshotEntry>,
    /// The effective running version after the existing override rules.
    pub running_version: String,
    /// The original update channel.
    #[doc(hidden)]
    pub(crate) update_channel: GitHubReleaseUpdateChannel,
}

/// The release lookup failed.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GitHubReleaseSnapshotResultFailed {
    /// The original user-facing lookup failure.
    pub error: String,
    /// The original lookup duration in milliseconds.
    pub lookup_duration_ms: f64,
    pub outcome: GitHubReleaseSnapshotResultFailedOutcome,
    /// The original update channel.
    #[doc(hidden)]
    pub(crate) update_channel: GitHubReleaseUpdateChannel,
}

/// Working-tree path whose owning GitHub repository should be resolved.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GitHubRepositoryAtPathRequest {
    /// Absolute path to a directory inside the git working tree to resolve.
    pub path: String,
}

/// Owner, name, and host of a GitHub repository, as resolved from a git remote URL.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct GitHubRepositoryIdentity {
    /// Host the remote points at, for example `github.com` or a GitHub Enterprise hostname.
    pub host: String,
    /// Repository name, without the owner prefix or the `.git` suffix.
    pub name: String,
    /// Repository owner login (user or organization).
    pub owner: String,
}

/// The GitHub repository that owns the requested path, when the selected remote (`origin`, else the first) is on a GitHub host.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GitHubRepositoryAtPathResult {
    /// Resolved repository identity, or null when the selected remote resolves to no GitHub host.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repository: Option<GitHubRepositoryIdentity>,
}

/// Selects a GitHub repository by its numeric repository id, and the credential to look it up under.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GitHubRepositoryByIdRequest {
    /// Selects which stored credential to resolve when the host holds more than one. Omit it to resolve the default for the supplied `authInfo`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auth_context_id: Option<String>,
    /// The credential the lookup runs under, carried opaquely because its shape is the host's own and the runtime only resolves a token and a GitHub host from it. Required: the endpoint answers per-account, so a repository invisible to this credential is reported as absent rather than refused.
    pub auth_info: serde_json::Value,
    /// GitHub's own numeric identifier for the repository, as carried by API payloads that reference a repository by id rather than by owner and name.
    pub repository_id: i64,
}

/// The repository the requested id names, when the credential can see one.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GitHubRepositoryByIdResult {
    /// Owner and name of the repository, or null when the id names no repository visible to the credential.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repository: Option<GitHubRepositoryOwnerName>,
}

/// Client environment metadata describing the process that produced a telemetry event.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct GitHubTelemetryClientInfo {
    /// Copilot CLI version string.
    #[serde(rename = "cli_version")]
    pub cli_version: String,
    /// Name of the client application.
    #[serde(rename = "client_name", skip_serializing_if = "Option::is_none")]
    pub client_name: Option<String>,
    /// Type of client.
    #[serde(rename = "client_type", skip_serializing_if = "Option::is_none")]
    pub client_type: Option<String>,
    /// Copilot subscription plan, when known.
    #[serde(rename = "copilot_plan", skip_serializing_if = "Option::is_none")]
    pub copilot_plan: Option<String>,
    /// Number of logical CPU cores on the host.
    #[serde(rename = "cpu_count", skip_serializing_if = "Option::is_none")]
    pub cpu_count: Option<i64>,
    /// Distinct CPU model names for the host, comma-separated.
    #[serde(rename = "cpu_model", skip_serializing_if = "Option::is_none")]
    pub cpu_model: Option<String>,
    /// Stable machine identifier for the device.
    #[serde(rename = "dev_device_id", skip_serializing_if = "Option::is_none")]
    pub dev_device_id: Option<String>,
    /// Whether the user is a GitHub/Microsoft staff member.
    #[serde(rename = "is_staff", skip_serializing_if = "Option::is_none")]
    pub is_staff: Option<bool>,
    /// Node.js runtime version string.
    #[serde(rename = "node_version")]
    pub node_version: String,
    /// Operating system architecture (e.g. arm64, x64).
    #[serde(rename = "os_arch")]
    pub os_arch: String,
    /// Operating system platform (e.g. darwin, linux, win32).
    #[serde(rename = "os_platform")]
    pub os_platform: String,
    /// Operating system version string.
    #[serde(rename = "os_version")]
    pub os_version: String,
}

/// A single telemetry event in the runtime's native GitHub-shaped telemetry format, forwarded verbatim to opted-in hosts. The `restricted` flag on the enclosing GitHubTelemetryNotification distinguishes standard from restricted events; the payload shape is identical for both.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct GitHubTelemetryEvent {
    /// Client environment metadata.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub client: Option<GitHubTelemetryClientInfo>,
    /// Copilot tracking ID for user-level attribution.
    #[serde(
        rename = "copilot_tracking_id",
        skip_serializing_if = "Option::is_none"
    )]
    pub copilot_tracking_id: Option<String>,
    /// Timestamp when the event was created (ISO 8601 format).
    #[serde(rename = "created_at", skip_serializing_if = "Option::is_none")]
    pub created_at: Option<String>,
    /// Experiment assignment context.
    #[serde(
        rename = "exp_assignment_context",
        skip_serializing_if = "Option::is_none"
    )]
    pub exp_assignment_context: Option<String>,
    /// Feature flags enabled for this session, as a map from flag to value.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub features: Option<HashMap<String, String>>,
    /// Event type/kind (e.g. get_completion_with_tools_turn, tool_call_executed).
    pub kind: String,
    /// Numeric metrics as a map from key to value.
    pub metrics: HashMap<String, f64>,
    /// Reference to the model call that produced this event.
    #[serde(rename = "model_call_id", skip_serializing_if = "Option::is_none")]
    pub model_call_id: Option<String>,
    /// String-valued properties as a map from key to value.
    pub properties: HashMap<String, String>,
    /// Session identifier the event belongs to.
    #[serde(rename = "session_id", skip_serializing_if = "Option::is_none")]
    pub session_id: Option<String>,
}

/// Payload for a `gitHubTelemetry.event` notification: a single GitHub telemetry event the runtime forwards to a host connection that opted into telemetry forwarding during the `server.connect` handshake.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct GitHubTelemetryNotification {
    /// The telemetry event, in the runtime's native GitHub-shaped telemetry format.
    pub event: GitHubTelemetryEvent,
    /// Whether this is a restricted telemetry event (cli.restricted_telemetry). Hosts must route restricted events to first-party Microsoft stores only.
    pub restricted: bool,
    /// Session the telemetry event belongs to, when it is session-scoped. Omitted for sessionless events (for example, `server.sendTelemetry` calls with no session id), which are still forwarded to opted-in connections.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_id: Option<SessionId>,
}

/// Asks the SDK client to acquire a GitHub access token from an opaque callback registration.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct GitHubTokenAcquireRequest {
    /// Effective GitHub host for which the callback must return a token.
    pub host: String,
    /// Why the runtime is requesting a GitHub credential.
    pub reason: GitHubTokenAcquireReason,
    /// Opaque identifier generated by the SDK for this callback registration.
    pub registration_id: String,
    /// Session receiving the token. Absent only before a cloud session has been assigned its id.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_id: Option<SessionId>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct GitHubTokenAcquireResultToken {
    /// GitHub access token acquired by the SDK host.
    pub access_token: String,
    /// Remaining token lifetime in seconds when callback execution completes. It must exceed the one-hour preflight refresh threshold, and still exceed it after the runtime validates the token: validation time counts against this lifetime, so a value just above 3600 can be rejected.
    pub expires_in: i64,
    /// GitHub credential response variant discriminator.
    pub kind: GitHubTokenAcquireResultTokenKind,
    /// OAuth token type. Defaults to bearer when omitted.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub token_type: Option<String>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct GitHubTokenAcquireResultCancelled {
    /// GitHub credential response variant discriminator.
    pub kind: GitHubTokenAcquireResultCancelledKind,
}

/// A GitHub repository one of a working tree's remotes points at.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GitRemoteRepository {
    /// GitHub host serving the repository, which is not `github.com` for a GitHub Enterprise remote.
    pub host: String,
    /// Repository name, without the owner.
    pub name: String,
    /// Account or organization owning the repository.
    pub owner: String,
    /// Name of the first remote that produced this distinct repository entry, such as `origin` or `upstream`.
    pub remote_name: String,
}

/// Git working tree whose GitHub remotes should be listed.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GitReposFromRemotesRequest {
    /// Absolute path to the root of the git working tree.
    pub git_root: String,
}

/// The GitHub repositories a working tree's remotes point at.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GitReposFromRemotesResult {
    /// One entry per distinct GitHub repository, in the order git reports the first remote for each repository. Empty when no remote points at a GitHub host, which a caller should read as `not connected to GitHub`. Failing to read the remotes is an error, not an empty list.
    #[doc(hidden)]
    pub(crate) repositories: Vec<GitRemoteRepository>,
}

/// Remote control is connected to a local session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RemoteControlStatusActive {
    /// Session id remote control is pointed at.
    pub attached_session_id: String,
    /// True while a read-only/session-sync export is deferred, awaiting the first `user.message` before its MC session exists. Marked internal: this field is excluded from the public SDK surface and is populated only on the CLI in-process path.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) awaiting_first_message: Option<bool>,
    /// MC frontend URL for this session, when known.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub frontend_url: Option<String>,
    /// Whether the MC session may steer this session.
    pub is_steerable: bool,
    /// In-process prompt-manager handle (CLI-only optimization). Marked internal: this field is excluded from the public SDK surface. Retained as an optional compatibility field; native remote control does not populate or consume it.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) prompt_manager: Option<serde_json::Value>,
    /// Remote control state tag: active.
    pub state: RemoteControlStatusActiveState,
}

/// Remote control is in the middle of initial setup.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RemoteControlStatusConnecting {
    /// Session id the connection is attaching to.
    pub attached_session_id: String,
    /// Remote control state tag: connecting.
    pub state: RemoteControlStatusConnectingState,
}

/// The last setup attempt failed. The singleton is otherwise off.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RemoteControlStatusError {
    /// Session id the failing setup attempt targeted, when known.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub attached_session_id: Option<String>,
    /// Human-readable error message from the last setup attempt.
    pub error: String,
    /// Remote control state tag: setup failed.
    pub state: RemoteControlStatusErrorState,
}

/// Remote control is not connected.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RemoteControlStatusOff {
    /// Remote control state tag: not connected.
    pub state: RemoteControlStatusOffState,
}

/// Wrapper for the singleton's current status.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RemoteControlStatusResult {
    /// State of the runtime-managed remote-control singleton.
    pub status: serde_json::Value,
}

/// Outcome of a stopRemoteControl call.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RemoteControlStopResult {
    /// State of the runtime-managed remote-control singleton.
    pub status: serde_json::Value,
    /// Whether the singleton was actually torn down by this call.
    pub stopped: bool,
}

/// Outcome of a transferRemoteControl call.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RemoteControlTransferResult {
    /// State of the runtime-managed remote-control singleton.
    pub status: serde_json::Value,
    /// Whether the rebinding actually happened.
    pub transferred: bool,
}

/// Optional remote session mode ("off", "export", or "on"); defaults to enabling both export and remote steering.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RemoteEnableRequest {
    /// Per-session remote mode. "off" disables remote, "export" exports session events to GitHub without enabling remote steering, "on" enables both export and remote steering.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mode: Option<RemoteSessionMode>,
}

/// GitHub URL and remote export and steering state, with an optional warning when cloud session storage policy refuses export.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RemoteEnableResult {
    /// Whether remote steering is enabled
    pub remote_steerable: bool,
    /// Whether this request accepted or retained remote export. False when export is disabled or refused; initialization may still be pending when true.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub started: Option<bool>,
    /// GitHub frontend URL for this session
    #[serde(skip_serializing_if = "Option::is_none")]
    pub url: Option<String>,
    /// Warning explaining why cloud session storage policy prevented export. Absent when no policy warning applies.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub warning: Option<String>,
}

/// New remote-steerability state to persist as a `session.remote_steerable_changed` event.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RemoteNotifySteerableChangedRequest {
    /// Whether the session now supports remote steering via GitHub. The runtime persists this as a `session.remote_steerable_changed` event so resume/replay sees the up-to-date capability.
    pub remote_steerable: bool,
}

/// Persist a steerability change as a `session.remote_steerable_changed` event. Used by the host (CLI / SDK consumer) when it has just finished enabling or disabling steering on a remote exporter that the runtime does not directly own.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RemoteNotifySteerableChangedResult {}

/// Remote session connection result.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RemoteSessionConnectionResult {
    /// Metadata for a connected remote session.
    pub metadata: ConnectedRemoteSessionMetadata,
    /// SDK session ID for the connected remote session.
    pub session_id: SessionId,
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
pub struct ServerAgentList {
    /// All discovered agents across all sources
    pub agents: Vec<AgentInfo>,
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
pub struct ServerInstructionSourceList {
    /// All discovered instruction sources
    pub sources: Vec<InstructionSource>,
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
pub struct ServerSkillList {
    /// Messages for skills that failed to load (e.g. malformed SKILL.md). Empty when host skills are excluded so host-local paths are not disclosed to multitenant callers.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub errors: Option<Vec<String>>,
    /// All discovered skills across all sources
    pub skills: Vec<ServerSkill>,
}

/// Workspace diff result for the requested mode.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspaceDiffResult {
    /// Default branch used for a branch diff, when branch mode was requested.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub base_branch: Option<String>,
    /// Changed files and their unified diffs.
    pub changes: Vec<WorkspaceDiffFileChange>,
    /// Whether the requested diff fell back to unstaged changes, either because branch diff failed or session diff was unavailable.
    pub is_fallback: bool,
    /// Effective mode used for the returned changes.
    pub mode: WorkspaceDiffMode,
    /// Diff mode requested by the client.
    pub requested_mode: WorkspaceDiffMode,
    /// Why the session diff could not be produced, when applicable. Set only when `session` mode was requested and `isFallback` is true, so a client can tell the permanent `file-change-tracking-disabled` apart from the transient `session-busy`, which the same request answers once the session settles. Never set for `unstaged` or `branch` mode, and never `unsupported-remote-session`: a remote session's captures live on its own host, so a `session`-mode diff is rejected for one rather than answered with a controller-side fallback.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub unavailable_reason: Option<HistoryRewindUnavailableReason>,
}

/// Compaction summary checkpoint to persist.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesAddSummaryRequest {
    /// Markdown summary content to persist.
    pub content: String,
    /// Summary title shown in checkpoint listings.
    pub title: String,
}

/// Persisted summary metadata and refreshed workspace metadata.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesAddSummaryResult {
    /// Metadata for the persisted summary.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub summary: Option<serde_json::Value>,
    /// Refreshed metadata for the containing workspace.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub workspace: Option<serde_json::Value>,
}

/// Whether the autopilot objective file exists.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesAutopilotObjectiveExistsResult {
    /// True when the objective file exists.
    pub exists: bool,
}

/// Directory to create within the session workspace files directory.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesCreateDirectoryRequest {
    /// Slash-separated relative path within the workspace files directory
    pub path: String,
    /// Whether to create missing parent directories. Defaults to false.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub recursive: Option<bool>,
}

/// Relative path and UTF-8 content for the workspace file to create or overwrite.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesCreateFileRequest {
    /// File content to write as a UTF-8 string
    pub content: String,
    /// Slash-separated relative path within the workspace files directory
    pub path: String,
}

/// Result of deleting the autopilot objective file.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesDeleteAutopilotObjectiveResult {
    /// True when a file was deleted.
    pub deleted: bool,
}

/// Parameters for computing a workspace diff.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesDiffRequest {
    /// When true, ignore whitespace-only changes (git `--ignore-all-space`). Defaults to false.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub ignore_whitespace: Option<bool>,
    /// Diff mode requested by the client.
    pub mode: WorkspaceDiffMode,
}

/// Extensible [`WorkspacesDiffRequest`], including inputs added after it was published.
///
/// Required inputs are [`WorkspacesDiffOptions::new`] arguments; optional inputs have fluent setters.
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
pub struct WorkspacesDiffOptions {
    #[serde(flatten)]
    legacy: WorkspacesDiffRequest,
    #[serde(skip_serializing_if = "Option::is_none")]
    include_contents: Option<bool>,
}

impl WorkspacesDiffOptions {
    /// Creates options with the required inputs.
    pub fn new(mode: WorkspaceDiffMode) -> Self {
        Self {
            legacy: WorkspacesDiffRequest {
                mode,
                ignore_whitespace: None,
            },
            include_contents: None,
        }
    }

    /// When true, ignore whitespace-only changes (git `--ignore-all-space`). Defaults to false.
    pub fn ignore_whitespace(mut self, value: bool) -> Self {
        self.legacy.ignore_whitespace = Some(value);
        self
    }

    /// Include the full before/after text used to compute each session diff. Defaults to false; true is accepted only for session mode. Existing capture/read limits still apply, and binary or unavailable contents are not returned. This can substantially increase response size.
    pub fn include_contents(mut self, value: bool) -> Self {
        self.include_contents = Some(value);
        self
    }
}

/// Optional session context used when creating a local workspace.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesEnsureRequest {
    /// Opaque workspace context supplied by the session host.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context: Option<serde_json::Value>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesGetWorkspaceResultWorkspace {
    /// Current Git branch.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub branch: Option<String>,
    /// Whether the per-session Chronicle upgrade prompt was dismissed for the workspace.
    #[serde(
        rename = "chronicle_sync_dismissed",
        skip_serializing_if = "Option::is_none"
    )]
    pub chronicle_sync_dismissed: Option<bool>,
    /// Name of the client that created the workspace.
    #[serde(rename = "client_name", skip_serializing_if = "Option::is_none")]
    pub client_name: Option<String>,
    /// Timestamp when the workspace was created.
    #[serde(rename = "created_at", skip_serializing_if = "Option::is_none")]
    pub created_at: Option<String>,
    /// Current working directory associated with the workspace.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cwd: Option<String>,
    /// Git repository root associated with the workspace.
    #[serde(rename = "git_root", skip_serializing_if = "Option::is_none")]
    pub git_root: Option<String>,
    /// Allowed values for the `WorkspacesWorkspaceDetailsHostType` enumeration.
    #[serde(rename = "host_type", skip_serializing_if = "Option::is_none")]
    pub host_type: Option<WorkspacesWorkspaceDetailsHostType>,
    /// Stable workspace identifier.
    pub id: String,
    /// Most recent Mission Control event identifier observed for the workspace.
    #[serde(rename = "mc_last_event_id", skip_serializing_if = "Option::is_none")]
    pub mc_last_event_id: Option<String>,
    /// Mission Control session identifier associated with the workspace.
    #[serde(rename = "mc_session_id", skip_serializing_if = "Option::is_none")]
    pub mc_session_id: Option<String>,
    /// Mission Control task identifier associated with the workspace.
    #[serde(rename = "mc_task_id", skip_serializing_if = "Option::is_none")]
    pub mc_task_id: Option<String>,
    /// Workspace display name.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    /// Whether the workspace session can be steered remotely.
    #[serde(rename = "remote_steerable", skip_serializing_if = "Option::is_none")]
    pub remote_steerable: Option<bool>,
    /// Repository identifier associated with the workspace.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repository: Option<String>,
    /// Number of persisted summaries in the workspace.
    #[serde(rename = "summary_count", skip_serializing_if = "Option::is_none")]
    pub summary_count: Option<i64>,
    /// Timestamp when the workspace was last updated.
    #[serde(rename = "updated_at", skip_serializing_if = "Option::is_none")]
    pub updated_at: Option<String>,
    /// Whether the workspace name was explicitly chosen by the user.
    #[serde(rename = "user_named", skip_serializing_if = "Option::is_none")]
    pub user_named: Option<bool>,
}

/// Current workspace metadata for the session, including its absolute filesystem path when available.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesGetWorkspaceResult {
    /// Absolute filesystem path to the workspace directory. Omitted when the session has no workspace (e.g. remote sessions).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub path: Option<String>,
    /// Current workspace metadata, or null if not available
    pub workspace: Option<WorkspacesGetWorkspaceResultWorkspace>,
}

/// Workspace checkpoints in chronological order; empty when the workspace is not enabled.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesListCheckpointsResult {
    /// Workspace checkpoints in chronological order. Empty when workspace is not enabled.
    pub checkpoints: Vec<WorkspacesCheckpoints>,
}

/// Relative paths of files stored in the session workspace files directory.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesListFilesResult {
    /// Slash-separated relative file paths in the workspace files directory
    pub files: Vec<String>,
}

/// Autopilot objective file content, or null when missing.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesReadAutopilotObjectiveResult {
    /// Autopilot objective file content, or null when missing.
    pub content: Option<String>,
}

/// Checkpoint number to read.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesReadCheckpointRequest {
    /// Checkpoint number to read
    pub number: i64,
}

/// Checkpoint content as a UTF-8 string, or null when the checkpoint or workspace is missing.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesReadCheckpointResult {
    /// Checkpoint content as a UTF-8 string, or null when the checkpoint or workspace is missing
    pub content: Option<String>,
}

/// Relative path of the workspace file to read.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesReadFileRequest {
    /// Slash-separated relative path within the workspace files directory
    pub path: String,
}

/// Contents of the requested workspace file as a UTF-8 string.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesReadFileResult {
    /// File content as a UTF-8 string
    pub content: String,
}

/// File or directory to remove from the session workspace files directory.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesRemovePathRequest {
    /// Whether a missing path should be treated as success. Defaults to false.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub force: Option<bool>,
    /// Slash-separated relative path within the workspace files directory
    pub path: String,
    /// Whether to remove directory contents recursively. Defaults to false.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub recursive: Option<bool>,
}

/// Source and destination paths for a rename within the session workspace files directory.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesRenamePathRequest {
    /// Slash-separated destination path relative to the workspace files directory
    pub destination: String,
    /// Slash-separated source path relative to the workspace files directory
    pub source: String,
}

/// Pasted content to save as a UTF-8 file in the session workspace.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesSaveLargePasteRequest {
    /// Pasted content to save as a UTF-8 file
    pub content: String,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesSaveLargePasteResultSaved {
    /// Filename within the workspace files directory
    pub filename: String,
    /// Absolute filesystem path to the saved paste file
    pub file_path: String,
    /// Size of the saved file in bytes
    pub size_bytes: i64,
}

/// Descriptor for the saved paste file, or null when the workspace is unavailable.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesSaveLargePasteResult {
    /// Saved-paste descriptor, or null when the workspace is unavailable (e.g. CCA runtime, non-infinite sessions, remote sessions)
    pub saved: Option<WorkspacesSaveLargePasteResultSaved>,
}

/// Relative path of the workspace file or directory to inspect.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesStatFileRequest {
    /// Slash-separated relative path within the workspace files directory
    pub path: String,
}

/// Filesystem metadata for a path in the session workspace files directory.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesStatFileResult {
    /// Creation time in Unix epoch milliseconds
    pub birthtime_ms: f64,
    /// Whether the path identifies a directory
    pub is_directory: bool,
    /// Whether the path identifies a regular file
    pub is_file: bool,
    /// Last modification time in Unix epoch milliseconds
    pub mtime_ms: f64,
    /// Size in bytes
    pub size: f64,
}

/// Rollback point for local workspace summaries.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesTruncateSummariesRequest {
    /// Highest checkpoint number to keep. Checkpoints 1 through keepCount (the oldest) are kept and every later checkpoint is deleted; 0 deletes all.
    pub keep_count: i64,
}

/// Public-facing workspace metadata for this session, or null if the session has no associated workspace. Excludes runtime-internal fields (GitHub IDs, summary count, internal flags).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspaceSummary {
    /// Branch checked out at session start, if any
    #[serde(skip_serializing_if = "Option::is_none")]
    pub branch: Option<String>,
    /// ISO 8601 timestamp when the workspace was created
    #[serde(rename = "created_at", skip_serializing_if = "Option::is_none")]
    pub created_at: Option<String>,
    /// Current working directory at session start
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cwd: Option<String>,
    /// Resolved git root for cwd, if any
    #[serde(rename = "git_root", skip_serializing_if = "Option::is_none")]
    pub git_root: Option<String>,
    /// Repository host type, if known
    #[serde(rename = "host_type", skip_serializing_if = "Option::is_none")]
    pub host_type: Option<WorkspaceSummaryHostType>,
    /// Workspace identifier (1:1 with sessionId)
    pub id: String,
    /// Display name for the session, if set
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    /// Repository identifier in 'owner/repo' or 'org/project/repo' format, if any
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repository: Option<String>,
    /// ISO 8601 timestamp when the workspace was last updated
    #[serde(rename = "updated_at", skip_serializing_if = "Option::is_none")]
    pub updated_at: Option<String>,
    /// Whether the display name was explicitly set by the user
    #[serde(rename = "user_named", skip_serializing_if = "Option::is_none")]
    pub user_named: Option<bool>,
}

/// Workspace metadata fields to update.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesUpdateMetadataRequest {
    /// Opaque workspace context supplied by the session host.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context: Option<serde_json::Value>,
    /// Optional workspace display name override.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
}

/// Autopilot objective file content to persist.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesWriteAutopilotObjectiveRequest {
    /// Autopilot objective file content.
    pub content: String,
}

/// Result of writing the autopilot objective file.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesWriteAutopilotObjectiveResult {
    /// Filesystem operation performed.
    pub operation: String,
}

/// The repository the requested id names, when the credential can see one.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GitHubRepositoryGetByIdResult {
    /// Owner and name of the repository, or null when the id names no repository visible to the credential.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repository: Option<GitHubRepositoryOwnerName>,
}

/// Outcome of a branch pull-request lookup. `status` carries why, because 'not authenticated', 'no pull request' and 'the search failed' are three different answers and only the first two are conclusive.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GitHubRepositoryLookupBranchPullRequestResult {
    /// The matching pull request, or null for every status other than `found`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub pull_request: Option<GitHubBranchPullRequest>,
    /// Why the lookup reported what it did.
    #[doc(hidden)]
    pub(crate) status: GitHubBranchPullRequestLookupStatus,
}

/// A freshly registered request id. Registering it before the listing starts is what lets a cancel that races the request still find the owner listing slot. The id serves one listing only. Long-abandoned unused ids can be released by later allocations.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GitHubOwnersNextRequestIdResult {
    /// Request id to pass to `gitHubOwners.list` and, to abandon it, `gitHubOwners.cancel`.
    pub request_id: i64,
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
pub struct GitWorkingDirectoryContextResult {
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

/// One registered GitHub request, shared by execution and cancellation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GitHubFetchAllocateRequestIdResult {
    /// One fresh id from `gitHubFetch.allocateRequestId`.
    pub request_id: i64,
}

/// A freshly registered request id. Registering it before the work starts is what lets a cancel that races the request still find something to cancel.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GitHubReleaseAllocateRequestIdResult {
    /// Request id to pass to `gitHubRelease.lookup` or `gitHubRelease.downloadAsset` and, to abandon the work, `gitHubRelease.cancel`.
    pub request_id: i64,
}

/// Byte progress of a running download.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct GitHubReleaseGetDownloadProgressResult {
    /// Bytes written so far.
    pub received_bytes: i64,
    /// Size the server announced for the body. Omitted when it announced none.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub total_bytes: Option<i64>,
}

/// Validation errors from the most recent authentication attempt.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type AuthValidationErrors = Vec<AuthValidationError>;

/// Client-owned, case-sensitive string metadata persisted with a local session. Clients should namespace keys by owner. Keys must be non-empty and at most 256 UTF-8 bytes; keys under `copilot/` and `github/` are reserved. Values may contain at most 16 KiB of UTF-8 data. A bag may contain at most 128 entries and its serialized sidecar may contain at most 64 KiB. The runtime stores but never interprets these values.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type ClientMetadata = HashMap<String, String>;

/// List of all authenticated users
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type AccountGetAllUsersResult = Vec<AccountAllUsers>;

/// Canonical availability treatment for an account row.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AccountHealth {
    /// The account is available and may be switchable.
    #[serde(rename = "available")]
    Available,
    /// The account is intentionally unavailable, for example because access is disabled or the platform is unsupported.
    #[serde(rename = "unavailable")]
    Unavailable,
    /// Availability is unknown because a transient or retryable check failed.
    #[serde(rename = "warning")]
    Warning,
    /// Authentication or saved credentials require user action.
    #[serde(rename = "error")]
    Error,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Account-collection query variant discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AuthEnumerateQueryAccountsKind {
    #[serde(rename = "accounts")]
    #[default]
    Accounts,
}

/// Account-collection query variant discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AuthEnumerateQueryProvidersKind {
    #[serde(rename = "providers")]
    #[default]
    Providers,
}

/// Account-collection query variant discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AuthEnumerateQueryLoginAccountsKind {
    #[serde(rename = "loginAccounts")]
    #[default]
    LoginAccounts,
}

/// Selects which accounts collection to enumerate. Collection-specific options are optional.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum AuthEnumerateQuery {
    Accounts(AuthEnumerateQueryAccounts),
    Providers(AuthEnumerateQueryProviders),
    LoginAccounts(AuthEnumerateQueryLoginAccounts),
}

/// Account read-datum query variant discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AuthReadQueryActiveAccountKind {
    #[serde(rename = "activeAccount")]
    #[default]
    ActiveAccount,
}

/// Account read-datum query variant discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AuthReadQueryStatusKind {
    #[serde(rename = "status")]
    #[default]
    Status,
}

/// Account read-datum query variant discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AuthReadQueryLastErrorsKind {
    #[serde(rename = "lastErrors")]
    #[default]
    LastErrors,
}

/// Selects which typed accounts datum to read.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum AuthReadQuery {
    ActiveAccount(AuthReadQueryActiveAccount),
    Status(AuthReadQueryStatus),
    LastErrors(AuthReadQueryLastErrors),
}

/// Account mutation command variant discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AuthWriteSwitchActiveKind {
    #[serde(rename = "switchActive")]
    #[default]
    SwitchActive,
}

/// Account mutation command variant discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AuthWriteLogoutKind {
    #[serde(rename = "logout")]
    #[default]
    Logout,
}

/// Account mutation command variant discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AuthWriteSetCredentialsKind {
    #[serde(rename = "setCredentials")]
    #[default]
    SetCredentials,
}

/// One non-interactive accounts mutation command (the selector is fused with its typed args).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum AuthWrite {
    SwitchActive(AuthWriteSwitchActive),
    Logout(AuthWriteLogout),
    SetCredentials(AuthWriteSetCredentials),
}

/// Enumerated account-collection variant discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AuthEnumerateValueAccountsKind {
    #[serde(rename = "accounts")]
    #[default]
    Accounts,
}

/// Enumerated account-collection variant discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AuthEnumerateValueProvidersKind {
    #[serde(rename = "providers")]
    #[default]
    Providers,
}

/// Enumerated account-collection variant discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AuthEnumerateValueLoginAccountsKind {
    #[serde(rename = "loginAccounts")]
    #[default]
    LoginAccounts,
}

/// The enumerated collection, keyed by the same selector as the query.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum AuthEnumerateValue {
    Accounts(AuthEnumerateValueAccounts),
    Providers(AuthEnumerateValueProviders),
    LoginAccounts(AuthEnumerateValueLoginAccounts),
}

/// How an interactive GitHub login is authorized.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AuthLoginMethod {
    /// Browser authorization with a loopback callback; the flow surfaces an `open-url` step.
    #[serde(rename = "web")]
    Web,
    /// OAuth device authorization; the flow surfaces a `device-code` step with a one-time code, for terminals whose browser cannot reach a loopback callback.
    #[serde(rename = "device-code")]
    DeviceCode,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// How an interactive provider login should choose its base identity.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AuthLoginMode {
    /// Use normal provider behavior, optionally pinned to accountId.
    #[serde(rename = "default")]
    Default,
    /// Explicitly ask the provider to present its account-selection UI instead of serving from a retained identity.
    #[serde(rename = "selectAccount")]
    SelectAccount,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Account read-datum variant discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AuthReadValueActiveAccountKind {
    #[serde(rename = "activeAccount")]
    #[default]
    ActiveAccount,
}

/// Account read-datum variant discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AuthReadValueStatusKind {
    #[serde(rename = "status")]
    #[default]
    Status,
}

/// Account read-datum variant discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AuthReadValueLastErrorsKind {
    #[serde(rename = "lastErrors")]
    #[default]
    LastErrors,
}

/// The read result, keyed by the same selector as the query.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum AuthReadValue {
    ActiveAccount(AuthReadValueActiveAccount),
    Status(AuthReadValueStatus),
    LastErrors(AuthReadValueLastErrors),
}

/// Why the runtime requests client-task cancellation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ClientTaskCancelReason {
    /// A caller requested task cancellation.
    #[serde(rename = "cancel_requested")]
    CancelRequested,
    /// The session is shutting down.
    #[serde(rename = "session_shutdown")]
    SessionShutdown,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Diagnostic source identifying the typed details payload.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum DiagnosticEntrySource {
    #[serde(rename = "mcp")]
    #[default]
    Mcp,
}

/// A supported diagnostic source.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum DiagnosticSource {
    #[serde(rename = "mcp")]
    Mcp,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Diagnostic source identifying the typed details payload.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum DiagnosticsReadResultEntriesItemSource {
    #[serde(rename = "mcp")]
    #[default]
    Mcp,
}

/// Rolled-up state of one pull-request check. GitHub's own conclusions are collapsed to these three so a caller renders a check without enumerating every API conclusion.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub(crate) enum GitHubPullRequestCheckStatus {
    /// The check concluded successfully, or was neutral or skipped.
    #[serde(rename = "pass")]
    Pass,
    /// The check concluded unsuccessfully, timed out, or was cancelled.
    #[serde(rename = "fail")]
    Fail,
    /// The check has not concluded, or reported a conclusion with no settled meaning.
    #[serde(rename = "pending")]
    Pending,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Why a branch pull-request lookup reported what it did. The three unsuccessful outcomes are kept apart because each one asks the caller for something different.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub(crate) enum GitHubBranchPullRequestLookupStatus {
    /// A matching pull request was found and is carried in `pullRequest`.
    #[serde(rename = "found")]
    Found,
    /// The credential resolved to no usable token, so the caller must authenticate.
    #[serde(rename = "no-token")]
    NoToken,
    /// The search succeeded and the branch has no matching pull request.
    #[serde(rename = "no-pr")]
    NoPr,
    /// The search itself could not be completed, so whether a pull request exists is unknown.
    #[serde(rename = "lookup-failed")]
    LookupFailed,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// A runtime-owned GitHub or Copilot operation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub(crate) enum GitHubFetchOperation {
    /// Check repository access.
    #[serde(rename = "repository-access")]
    RepositoryAccess,
    /// Look up a branch.
    #[serde(rename = "branch-exists")]
    BranchExists,
    /// Create a Copilot job.
    #[serde(rename = "job-create")]
    JobCreate,
    /// Read a Copilot job.
    #[serde(rename = "job-status")]
    JobStatus,
    /// List remote sessions.
    #[serde(rename = "sessions-list")]
    SessionsList,
    /// Resolve pull requests through GraphQL.
    #[serde(rename = "pull-requests-graphql")]
    PullRequestsGraphql,
    /// Read a pull request.
    #[serde(rename = "pull-request")]
    PullRequest,
    /// Read pull request files.
    #[serde(rename = "pull-request-files")]
    PullRequestFiles,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum GitHubFetchResultResponseKind {
    #[serde(rename = "response")]
    #[default]
    Response,
}

#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum GitHubFetchResultCircuitBreakerKind {
    #[serde(rename = "circuit-breaker")]
    #[default]
    CircuitBreaker,
}

#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum GitHubFetchResultProxyResponseKind {
    #[serde(rename = "proxy-response")]
    #[default]
    ProxyResponse,
}

#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum GitHubFetchResultFailedKind {
    #[serde(rename = "failed")]
    #[default]
    Failed,
}

/// The HTTP response or a classified request failure.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub(crate) enum GitHubFetchResult {
    Response(GitHubFetchResultResponse),
    CircuitBreaker(GitHubFetchResultCircuitBreaker),
    ProxyResponse(GitHubFetchResultProxyResponse),
    Failed(GitHubFetchResultFailed),
}

/// How an asset download ended. A caller tells a refused response from a transport failure or a cancellation by this alone, without parsing `message`.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub(crate) enum GitHubReleaseDownloadKind {
    /// The asset is on disk at `path`.
    #[serde(rename = "downloaded")]
    Downloaded,
    /// GitHub answered with a status other than 2xx, including after the retry without the credential. `status` and `statusText` describe it.
    #[serde(rename = "http-error")]
    HttpError,
    /// The downloaded bytes did not match `expectedSha256`, and the file was removed.
    #[serde(rename = "digest-mismatch")]
    DigestMismatch,
    /// `gitHubRelease.cancel` abandoned the download, and any partial file was removed.
    #[serde(rename = "cancelled")]
    Cancelled,
    /// The download failed for any other reason, described by `message`.
    #[serde(rename = "failed")]
    Failed,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// How a release lookup ended. A caller tells a conclusive absence from a transport error by this alone, without parsing `message`.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub(crate) enum GitHubReleaseLookupKind {
    /// A release was found, and `releaseJson` carries it.
    #[serde(rename = "found")]
    Found,
    /// The tag names no release. Conclusive, not a failure.
    #[serde(rename = "not-found")]
    NotFound,
    /// The repository publishes no release matching the query at all.
    #[serde(rename = "no-release")]
    NoRelease,
    /// GitHub refused the request for rate limiting. `message` is phrased for the user and already accounts for whether the lookup ran authenticated.
    #[serde(rename = "rate-limited")]
    RateLimited,
    /// The lookup failed for any other reason, described by `message`.
    #[serde(rename = "failed")]
    Failed,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Which release to look up. Enumerated rather than free text because the runtime accepts exactly these and rejects anything else.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub(crate) enum GitHubReleaseQuery {
    /// The newest stable command-line release.
    #[serde(rename = "latest-stable")]
    LatestStable,
    /// The newest prerelease command-line release, which is found by scanning recent releases rather than by asking for the latest one.
    #[serde(rename = "latest-prerelease")]
    LatestPrerelease,
    /// The newest release of the desktop application.
    #[serde(rename = "latest-app")]
    LatestApp,
    /// The release a specific tag names, supplied in `value`.
    #[serde(rename = "tag")]
    Tag,
    /// Recent releases for process-scoped update and metrics decisions.
    #[serde(rename = "release-list")]
    ReleaseList,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Whether the caller runs a development build or a release package.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub(crate) enum GitHubReleasePackageIdentity {
    /// Development builds are not eligible for release admission.
    #[serde(rename = "dev")]
    Dev,
    /// The caller runs a release package.
    #[serde(rename = "release")]
    Release,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// The release channel used for update selection.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub(crate) enum GitHubReleaseUpdateChannel {
    /// Select the newest stable release.
    #[serde(rename = "stable")]
    Stable,
    /// Select the newest published release marked as a prerelease. A stable-only feed has no candidate.
    #[serde(rename = "prerelease")]
    Prerelease,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub(crate) enum GitHubReleaseSnapshotResultSuccessOutcome {
    #[serde(rename = "success")]
    #[default]
    Success,
}

#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub(crate) enum GitHubReleaseSnapshotResultFailedOutcome {
    #[serde(rename = "failed")]
    #[default]
    Failed,
}

/// The shared release result with its original lookup duration. Runtime worker faults remain RPC errors.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub(crate) enum GitHubReleaseSnapshotResult {
    Success(GitHubReleaseSnapshotResultSuccess),
    Failed(GitHubReleaseSnapshotResultFailed),
}

/// Why the runtime is requesting a GitHub credential.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum GitHubTokenAcquireReason {
    /// The runtime is acquiring the registration's first credential.
    #[serde(rename = "initial")]
    Initial,
    /// The runtime is replacing a credential that is approaching expiry.
    #[serde(rename = "refresh")]
    Refresh,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// GitHub credential response variant discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum GitHubTokenAcquireResultTokenKind {
    #[serde(rename = "token")]
    #[default]
    Token,
}

/// GitHub credential response variant discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum GitHubTokenAcquireResultCancelledKind {
    #[serde(rename = "cancelled")]
    #[default]
    Cancelled,
}

/// SDK host response to a GitHub credential request.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum GitHubTokenAcquireResult {
    Token(GitHubTokenAcquireResultToken),
    Cancelled(GitHubTokenAcquireResultCancelled),
}

/// Remote control state tag: active.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum RemoteControlStatusActiveState {
    #[serde(rename = "active")]
    #[default]
    Active,
}

/// Remote control state tag: connecting.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum RemoteControlStatusConnectingState {
    #[serde(rename = "connecting")]
    #[default]
    Connecting,
}

/// Remote control state tag: setup failed.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum RemoteControlStatusErrorState {
    #[serde(rename = "error")]
    #[default]
    Error,
}

/// Remote control state tag: not connected.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum RemoteControlStatusOffState {
    #[serde(rename = "off")]
    #[default]
    Off,
}

/// Per-session remote mode. "off" disables remote, "export" exports session events to GitHub without enabling remote steering, "on" enables both export and remote steering.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum RemoteSessionMode {
    /// Disable remote session export and steering.
    #[serde(rename = "off")]
    Off,
    /// Export session events to GitHub without enabling remote steering.
    #[serde(rename = "export")]
    Export,
    /// Enable both remote session export and remote steering.
    #[serde(rename = "on")]
    On,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

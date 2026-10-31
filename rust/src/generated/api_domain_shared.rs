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

use crate::generated::{lifecycle_diagnostics, session_events};
use crate::types::{RequestId, SessionEvent, SessionId};

/// Endpoint URLs from the raw Copilot `/copilot_internal/v2/token` user-response passthrough.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CopilotUserResponseEndpoints {
    /// Copilot API endpoint URL.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub api: Option<String>,
    /// Experimental-service endpoint URL.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub exp: Option<String>,
    /// Origin-tracker endpoint URL.
    #[serde(rename = "origin-tracker", skip_serializing_if = "Option::is_none")]
    pub origin_tracker: Option<String>,
    /// Copilot proxy endpoint URL.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub proxy: Option<String>,
    /// Copilot telemetry endpoint URL.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub telemetry: Option<String>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CopilotUserResponseEnterpriseListItem {
    /// JavaScript-safe numeric database ID of the enterprise.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub id: Option<i64>,
}

/// Chat quota snapshot from the raw Copilot user-response passthrough, with entitlement, overage, remaining quota, reset, and billing fields.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CopilotUserResponseQuotaSnapshotsChat {
    /// Number of requests/units included in the entitlement for this period; `-1` denotes an unlimited entitlement.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub entitlement: Option<f64>,
    /// Whether the user currently has quota available; when `false` and not unlimited, further requests are blocked until the quota resets.
    #[serde(rename = "has_quota", skip_serializing_if = "Option::is_none")]
    pub has_quota: Option<bool>,
    /// Count of additional pay-per-request usage consumed this period beyond the entitlement.
    #[serde(rename = "overage_count", skip_serializing_if = "Option::is_none")]
    pub overage_count: Option<f64>,
    /// Whether usage may continue at pay-per-request rates once the entitlement is exhausted.
    #[serde(rename = "overage_permitted", skip_serializing_if = "Option::is_none")]
    pub overage_permitted: Option<bool>,
    /// Percentage of the entitlement remaining at the snapshot timestamp.
    #[serde(rename = "percent_remaining", skip_serializing_if = "Option::is_none")]
    pub percent_remaining: Option<f64>,
    /// Identifier of the quota bucket this snapshot describes.
    #[serde(rename = "quota_id", skip_serializing_if = "Option::is_none")]
    pub quota_id: Option<String>,
    /// Amount of quota remaining at the snapshot timestamp.
    #[serde(rename = "quota_remaining", skip_serializing_if = "Option::is_none")]
    pub quota_remaining: Option<f64>,
    /// Unix epoch time, in seconds, when this quota next resets.
    #[serde(rename = "quota_reset_at", skip_serializing_if = "Option::is_none")]
    pub quota_reset_at: Option<f64>,
    /// Remaining entitlement/quota amount at the snapshot timestamp.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub remaining: Option<f64>,
    /// UTC timestamp when this snapshot was captured.
    #[serde(rename = "timestamp_utc", skip_serializing_if = "Option::is_none")]
    pub timestamp_utc: Option<String>,
    /// Whether this category uses usage-based (token/AI-credit) billing rather than a fixed premium-request count.
    #[serde(
        rename = "token_based_billing",
        skip_serializing_if = "Option::is_none"
    )]
    pub token_based_billing: Option<bool>,
    /// Whether the entitlement for this category is unlimited.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub unlimited: Option<bool>,
}

/// Completions quota snapshot from the raw Copilot user-response passthrough, with entitlement, overage, remaining quota, reset, and billing fields.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CopilotUserResponseQuotaSnapshotsCompletions {
    /// Number of requests/units included in the entitlement for this period; `-1` denotes an unlimited entitlement.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub entitlement: Option<f64>,
    /// Whether the user currently has quota available; when `false` and not unlimited, further requests are blocked until the quota resets.
    #[serde(rename = "has_quota", skip_serializing_if = "Option::is_none")]
    pub has_quota: Option<bool>,
    /// Count of additional pay-per-request usage consumed this period beyond the entitlement.
    #[serde(rename = "overage_count", skip_serializing_if = "Option::is_none")]
    pub overage_count: Option<f64>,
    /// Whether usage may continue at pay-per-request rates once the entitlement is exhausted.
    #[serde(rename = "overage_permitted", skip_serializing_if = "Option::is_none")]
    pub overage_permitted: Option<bool>,
    /// Percentage of the entitlement remaining at the snapshot timestamp.
    #[serde(rename = "percent_remaining", skip_serializing_if = "Option::is_none")]
    pub percent_remaining: Option<f64>,
    /// Identifier of the quota bucket this snapshot describes.
    #[serde(rename = "quota_id", skip_serializing_if = "Option::is_none")]
    pub quota_id: Option<String>,
    /// Amount of quota remaining at the snapshot timestamp.
    #[serde(rename = "quota_remaining", skip_serializing_if = "Option::is_none")]
    pub quota_remaining: Option<f64>,
    /// Unix epoch time, in seconds, when this quota next resets.
    #[serde(rename = "quota_reset_at", skip_serializing_if = "Option::is_none")]
    pub quota_reset_at: Option<f64>,
    /// Remaining entitlement/quota amount at the snapshot timestamp.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub remaining: Option<f64>,
    /// UTC timestamp when this snapshot was captured.
    #[serde(rename = "timestamp_utc", skip_serializing_if = "Option::is_none")]
    pub timestamp_utc: Option<String>,
    /// Whether this category uses usage-based (token/AI-credit) billing rather than a fixed premium-request count.
    #[serde(
        rename = "token_based_billing",
        skip_serializing_if = "Option::is_none"
    )]
    pub token_based_billing: Option<bool>,
    /// Whether the entitlement for this category is unlimited.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub unlimited: Option<bool>,
}

/// Premium-interactions quota snapshot from the raw Copilot user-response passthrough, with entitlement, overage, remaining quota, reset, and billing fields.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CopilotUserResponseQuotaSnapshotsPremiumInteractions {
    /// Number of requests/units included in the entitlement for this period; `-1` denotes an unlimited entitlement.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub entitlement: Option<f64>,
    /// Whether the user currently has quota available; when `false` and not unlimited, further requests are blocked until the quota resets.
    #[serde(rename = "has_quota", skip_serializing_if = "Option::is_none")]
    pub has_quota: Option<bool>,
    /// Count of additional pay-per-request usage consumed this period beyond the entitlement.
    #[serde(rename = "overage_count", skip_serializing_if = "Option::is_none")]
    pub overage_count: Option<f64>,
    /// Whether usage may continue at pay-per-request rates once the entitlement is exhausted.
    #[serde(rename = "overage_permitted", skip_serializing_if = "Option::is_none")]
    pub overage_permitted: Option<bool>,
    /// Percentage of the entitlement remaining at the snapshot timestamp.
    #[serde(rename = "percent_remaining", skip_serializing_if = "Option::is_none")]
    pub percent_remaining: Option<f64>,
    /// Identifier of the quota bucket this snapshot describes.
    #[serde(rename = "quota_id", skip_serializing_if = "Option::is_none")]
    pub quota_id: Option<String>,
    /// Amount of quota remaining at the snapshot timestamp.
    #[serde(rename = "quota_remaining", skip_serializing_if = "Option::is_none")]
    pub quota_remaining: Option<f64>,
    /// Unix epoch time, in seconds, when this quota next resets.
    #[serde(rename = "quota_reset_at", skip_serializing_if = "Option::is_none")]
    pub quota_reset_at: Option<f64>,
    /// Remaining entitlement/quota amount at the snapshot timestamp.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub remaining: Option<f64>,
    /// UTC timestamp when this snapshot was captured.
    #[serde(rename = "timestamp_utc", skip_serializing_if = "Option::is_none")]
    pub timestamp_utc: Option<String>,
    /// Whether this category uses usage-based (token/AI-credit) billing rather than a fixed premium-request count.
    #[serde(
        rename = "token_based_billing",
        skip_serializing_if = "Option::is_none"
    )]
    pub token_based_billing: Option<bool>,
    /// Whether the entitlement for this category is unlimited.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub unlimited: Option<bool>,
}

/// Quota snapshot map from the raw Copilot user-response passthrough, with chat, completions, premium-interactions, and other entries.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CopilotUserResponseQuotaSnapshots {
    /// Chat quota snapshot from the raw Copilot user-response passthrough, with entitlement, overage, remaining quota, reset, and billing fields.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub chat: Option<CopilotUserResponseQuotaSnapshotsChat>,
    /// Completions quota snapshot from the raw Copilot user-response passthrough, with entitlement, overage, remaining quota, reset, and billing fields.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub completions: Option<CopilotUserResponseQuotaSnapshotsCompletions>,
    /// Premium-interactions quota snapshot from the raw Copilot user-response passthrough, with entitlement, overage, remaining quota, reset, and billing fields.
    #[serde(
        rename = "premium_interactions",
        skip_serializing_if = "Option::is_none"
    )]
    pub premium_interactions: Option<CopilotUserResponseQuotaSnapshotsPremiumInteractions>,
}

/// Snapshot of the authenticated user's Copilot subscription info, if known. Mirrors the GitHub API `/copilot_internal/v2/token` user response shape — the runtime trusts this verbatim and does not re-fetch when set.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CopilotUserResponse {
    /// Copilot access SKU identifier (e.g. `free_limited_copilot`, `copilot_for_business_seat_quota`) used to gate model and feature access.
    #[serde(rename = "access_type_sku", skip_serializing_if = "Option::is_none")]
    pub access_type_sku: Option<String>,
    /// Opaque analytics tracking identifier for the user, forwarded from the Copilot API.
    #[serde(
        rename = "analytics_tracking_id",
        skip_serializing_if = "Option::is_none"
    )]
    pub analytics_tracking_id: Option<String>,
    /// Date the Copilot seat was assigned to the user, if applicable.
    #[serde(rename = "assigned_date", skip_serializing_if = "Option::is_none")]
    pub assigned_date: Option<serde_json::Value>,
    /// Whether the user is eligible to sign up for the free/limited Copilot tier.
    #[serde(
        rename = "can_signup_for_limited",
        skip_serializing_if = "Option::is_none"
    )]
    pub can_signup_for_limited: Option<bool>,
    /// Whether the user is able to upgrade their Copilot plan.
    #[serde(rename = "can_upgrade_plan", skip_serializing_if = "Option::is_none")]
    pub can_upgrade_plan: Option<bool>,
    /// Whether Copilot chat is enabled for the user.
    #[serde(rename = "chat_enabled", skip_serializing_if = "Option::is_none")]
    pub chat_enabled: Option<bool>,
    /// Whether CLI remote control is enabled for the user.
    #[serde(
        rename = "cli_remote_control_enabled",
        skip_serializing_if = "Option::is_none"
    )]
    pub cli_remote_control_enabled: Option<bool>,
    /// Whether cloud session storage is enabled for the user.
    #[serde(
        rename = "cloud_session_storage_enabled",
        skip_serializing_if = "Option::is_none"
    )]
    pub cloud_session_storage_enabled: Option<bool>,
    /// Whether the Codex agent is enabled for the user.
    #[serde(
        rename = "codex_agent_enabled",
        skip_serializing_if = "Option::is_none"
    )]
    pub codex_agent_enabled: Option<bool>,
    /// Copilot plan name for the user (e.g. `individual`, `business`, `enterprise`).
    #[serde(rename = "copilot_plan", skip_serializing_if = "Option::is_none")]
    pub copilot_plan: Option<String>,
    /// Whether `.copilotignore` content-exclusion support is enabled for the user.
    #[serde(
        rename = "copilotignore_enabled",
        skip_serializing_if = "Option::is_none"
    )]
    pub copilotignore_enabled: Option<bool>,
    /// Endpoint URLs from the raw Copilot `/copilot_internal/v2/token` user-response passthrough.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub endpoints: Option<CopilotUserResponseEndpoints>,
    /// Enterprises that provide the user's Copilot license; malformed entries are normalized to null or ID-less shapes.
    #[serde(rename = "enterprise_list", skip_serializing_if = "Option::is_none")]
    pub enterprise_list: Option<Vec<Option<CopilotUserResponseEnterpriseListItem>>>,
    /// Whether MCP (Model Context Protocol) support is enabled for the user.
    #[serde(rename = "is_mcp_enabled", skip_serializing_if = "Option::is_none")]
    pub is_mcp_enabled: Option<serde_json::Value>,
    /// Whether the user is a GitHub/Microsoft staff member.
    #[serde(rename = "is_staff", skip_serializing_if = "Option::is_none")]
    pub is_staff: Option<bool>,
    /// Per-category quota allotments for free/limited-tier users, keyed by quota category.
    #[serde(
        rename = "limited_user_quotas",
        skip_serializing_if = "Option::is_none"
    )]
    pub limited_user_quotas: Option<HashMap<String, f64>>,
    /// Date the free/limited-tier user's quotas next reset, as a raw string from the Copilot API.
    #[serde(
        rename = "limited_user_reset_date",
        skip_serializing_if = "Option::is_none"
    )]
    pub limited_user_reset_date: Option<String>,
    /// GitHub login of the authenticated user.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub login: Option<String>,
    /// Per-category monthly quota allotments, keyed by quota category.
    #[serde(rename = "monthly_quotas", skip_serializing_if = "Option::is_none")]
    pub monthly_quotas: Option<HashMap<String, f64>>,
    /// Organizations the user belongs to, each with an optional ID, login, and display name.
    #[serde(rename = "organization_list", skip_serializing_if = "Option::is_none")]
    pub organization_list: Option<serde_json::Value>,
    /// Logins of the organizations the user belongs to.
    #[serde(
        rename = "organization_login_list",
        skip_serializing_if = "Option::is_none"
    )]
    pub organization_login_list: Option<Vec<String>>,
    /// Date the user's usage quota next resets, as a raw string from the Copilot API; see `quota_reset_date_utc` for the UTC-normalized value.
    #[serde(rename = "quota_reset_date", skip_serializing_if = "Option::is_none")]
    pub quota_reset_date: Option<String>,
    /// UTC-normalized form of `quota_reset_date` (the date the user's usage quota next resets).
    #[serde(
        rename = "quota_reset_date_utc",
        skip_serializing_if = "Option::is_none"
    )]
    pub quota_reset_date_utc: Option<String>,
    /// Quota snapshot map from the raw Copilot user-response passthrough, with chat, completions, premium-interactions, and other entries.
    #[serde(rename = "quota_snapshots", skip_serializing_if = "Option::is_none")]
    pub quota_snapshots: Option<CopilotUserResponseQuotaSnapshots>,
    /// Whether the user's telemetry is subject to restricted-data handling.
    #[serde(
        rename = "restricted_telemetry",
        skip_serializing_if = "Option::is_none"
    )]
    pub restricted_telemetry: Option<bool>,
    /// Raw passthrough of the Copilot API `te` flag for the user (an opaque server-side eligibility signal surfaced in telemetry); not otherwise interpreted by the runtime.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub te: Option<bool>,
    /// Whether the account is on usage-based (token/AI-credit) billing rather than a fixed premium-request quota.
    #[serde(
        rename = "token_based_billing",
        skip_serializing_if = "Option::is_none"
    )]
    pub token_based_billing: Option<bool>,
}

/// Authentication-info input variant for GitHub-internal HMAC auth, carrying the public GitHub host and HMAC secret.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HMACAuthInfo {
    /// Snapshot of the authenticated user's Copilot subscription info, if known. Mirrors the GitHub API `/copilot_internal/v2/token` user response shape — the runtime trusts this verbatim and does not re-fetch when set.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub copilot_user: Option<CopilotUserResponse>,
    /// HMAC secret used to sign requests.
    pub hmac: String,
    /// Authentication host. HMAC auth always targets the public GitHub host.
    pub host: HMACAuthInfoHost,
    /// HMAC-based authentication used by GitHub-internal services.
    pub r#type: HMACAuthInfoType,
}

/// Authentication-info input variant for a token sourced from an environment variable, with host, optional login, token, and env var name.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct EnvAuthInfo {
    /// Snapshot of the authenticated user's Copilot subscription info, if known. Mirrors the GitHub API `/copilot_internal/v2/token` user response shape — the runtime trusts this verbatim and does not re-fetch when set.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub copilot_user: Option<CopilotUserResponse>,
    /// Name of the environment variable the token was sourced from.
    pub env_var: String,
    /// Authentication host (e.g. https://github.com or a GHES host).
    pub host: String,
    /// User login associated with the token. Undefined for server-to-server tokens (those starting with `ghs_`).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub login: Option<String>,
    /// The token value itself. Treat as a secret.
    pub token: String,
    /// Personal access token (PAT) or server-to-server token sourced from an environment variable.
    pub r#type: EnvAuthInfoType,
}

/// Authentication-info input variant for SDK-configured token authentication, carrying host and the secret token value.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TokenAuthInfo {
    /// Snapshot of the authenticated user's Copilot subscription info, if known. Mirrors the GitHub API `/copilot_internal/v2/token` user response shape — the runtime trusts this verbatim and does not re-fetch when set.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub copilot_user: Option<CopilotUserResponse>,
    /// Authentication host.
    pub host: String,
    /// Opaque native GitHub credential registration backing this token identity, when applicable.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub registration_id: Option<String>,
    /// The token value itself. Treat as a secret.
    pub token: String,
    /// SDK-side token authentication; the host configured the token directly via the SDK.
    pub r#type: TokenAuthInfoType,
}

/// Authentication-info variant backed by an SDK GitHub token callback. It carries routing metadata but never a plaintext token.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TokenProviderAuthInfo {
    /// Snapshot of the authenticated user's Copilot subscription info, if known.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub copilot_user: Option<CopilotUserResponse>,
    /// Authentication host.
    pub host: String,
    /// Opaque SDK callback registration identifier.
    pub registration_id: String,
    /// SDK callback-backed GitHub token authentication.
    pub r#type: TokenProviderAuthInfoType,
}

/// Authentication-info variant for direct Copilot API token auth sourced from environment variables, with public GitHub host.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CopilotApiTokenAuthInfo {
    /// Snapshot of the authenticated user's Copilot subscription info, if known. Mirrors the GitHub API `/copilot_internal/v2/token` user response shape — the runtime trusts this verbatim and does not re-fetch when set.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub copilot_user: Option<CopilotUserResponse>,
    /// Authentication host (always the public GitHub host).
    pub host: CopilotApiTokenAuthInfoHost,
    /// Direct Copilot API authentication via the `GITHUB_COPILOT_API_TOKEN` + `COPILOT_API_URL` environment-variable pair. The token itself is read from the environment by the runtime, not carried in this struct.
    pub r#type: CopilotApiTokenAuthInfoType,
}

/// Authentication-info variant for OAuth user auth, with host and login; the token remains in the runtime secret store.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserAuthInfo {
    /// Snapshot of the authenticated user's Copilot subscription info, if known. Mirrors the GitHub API `/copilot_internal/v2/token` user response shape — the runtime trusts this verbatim and does not re-fetch when set.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub copilot_user: Option<CopilotUserResponse>,
    /// Authentication host.
    pub host: String,
    /// OAuth user login.
    pub login: String,
    /// OAuth user authentication. The token itself is held in the runtime's secret token store (keyed by host+login) and is NOT carried in this struct.
    pub r#type: UserAuthInfoType,
}

/// An interactive account whose model provider owns its credentials. It carries no GitHub credential.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AccountAuthInfo {
    /// Host coordinate owned by the account's model provider.
    pub host: String,
    /// Login identifying the provider-owned account.
    pub login: String,
    /// Provider-owned account authentication.
    pub r#type: AccountAuthInfoType,
}

/// Authentication-info input variant for GitHub CLI credentials, carrying host, login, and the `gh auth token` value.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct GhCliAuthInfo {
    /// Snapshot of the authenticated user's Copilot subscription info, if known. Mirrors the GitHub API `/copilot_internal/v2/token` user response shape — the runtime trusts this verbatim and does not re-fetch when set.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub copilot_user: Option<CopilotUserResponse>,
    /// Authentication host.
    pub host: String,
    /// User login as reported by `gh auth status`.
    pub login: String,
    /// The token returned by `gh auth token`. Treat as a secret.
    pub token: String,
    /// Authentication via the `gh` CLI's saved credentials.
    pub r#type: GhCliAuthInfoType,
}

/// Authentication-info input variant for API-key authentication to a non-GitHub LLM provider, carrying the secret `apiKey` and host.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ApiKeyAuthInfo {
    /// The API key. Treat as a secret.
    pub api_key: String,
    /// Snapshot of the authenticated user's Copilot subscription info, if known. Mirrors the GitHub API `/copilot_internal/v2/token` user response shape — the runtime trusts this verbatim and does not re-fetch when set.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub copilot_user: Option<CopilotUserResponse>,
    /// Authentication host.
    pub host: String,
    /// API-key authentication for non-GitHub LLM providers (e.g. when running BYOM-style).
    pub r#type: ApiKeyAuthInfoType,
}

/// Credential-free authentication identity safe to expose to hosts and user interfaces.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AuthIdentity {
    /// Snapshot of the authenticated user's Copilot subscription info, if known
    #[serde(skip_serializing_if = "Option::is_none")]
    pub copilot_user: Option<CopilotUserResponse>,
    /// Name of the environment variable that supplied the credential, when applicable
    #[serde(skip_serializing_if = "Option::is_none")]
    pub env_var: Option<String>,
    /// Authentication host
    pub host: String,
    /// Authenticated login, when available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub login: Option<String>,
    /// Opaque SDK GitHub credential registration backing this identity. Routing metadata only; never a credential.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub registration_id: Option<String>,
    /// Authentication type
    pub r#type: AuthInfoType,
}

/// Agent metadata, including identifiers, display details, source, tools, model, models, reasoning effort, MCP servers, skills, and file path.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentInfo {
    /// Description of the agent's purpose
    pub description: String,
    /// Whether model-driven invocation is disabled for this agent.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub disable_model_invocation: Option<bool>,
    /// Human-readable display name
    pub display_name: String,
    /// Declarative hook actions keyed by event name, executed only while this agent is active.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub hooks: Option<HashMap<String, Vec<serde_json::Value>>>,
    /// Stable identifier for selection. For most agents this is the same as `name`; for plugin/builtin agents it may differ. Always populated; defaults to `name` when no distinct id was assigned.
    pub id: String,
    /// MCP server configurations attached to this agent, keyed by server name. Server config shape mirrors the MCP `mcpServers` schema.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This type is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases.
    ///
    /// </div>
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mcp_servers: Option<HashMap<String, serde_json::Value>>,
    /// Authored preferred model id for this agent. Runtime model selection may choose a different model; omitted means no authored preference.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model: Option<String>,
    /// Whether authored models are preferences or required constraints.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_policy: Option<AgentModelPolicy>,
    /// Authored preferred model ids for this agent, in priority order. Runtime model selection chooses the first available model; omitted means no authored preference.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub models: Option<Vec<String>>,
    /// Name of the agent. Use `id` as the stable selection identifier.
    pub name: String,
    /// Absolute local file path of the agent definition. Only set for file-based agents loaded from disk; remote agents do not have a path.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub path: Option<String>,
    /// Authored base prompt for the agent. Runtime prompt assembly may add dynamic context at invocation time. Omitted from `session.agent.list` unless `includePrompt` is true.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub prompt: Option<String>,
    /// Authored reasoning effort for this agent. Applied on selection to models that support it; omitted means no authored preference.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_effort: Option<String>,
    /// Skill names preloaded into this agent's context. Omitted means none.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub skills: Option<Vec<String>>,
    /// Where the agent definition was loaded from
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source: Option<AgentInfoSource>,
    /// Allowed tool names for this agent. Empty array means none; omitted means inherit defaults.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tools: Option<Vec<String>>,
    /// Whether the agent can be selected directly by the user. Agents marked `false` are subagent-only.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub user_invocable: Option<bool>,
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
pub struct GitHubRepoRef {
    /// Numeric GitHub repository id
    #[serde(skip_serializing_if = "Option::is_none")]
    pub id: Option<i64>,
    /// Repository name (without owner)
    pub name: String,
    /// Repository owner login (user or organization)
    pub owner: String,
}

/// A provider offered for interactive login.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ProviderDescriptor {
    /// Whether this provider is currently available to sign in with.
    pub available: bool,
    /// The neutral provider kind.
    pub kind: LoginProviderKind,
    /// Human-readable menu label, owned by the runtime so every consumer renders identical text.
    pub label: String,
}

/// A credential-free account choice after sign-in.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AuthLoginAccount {
    /// Host coordinate owned by the selected account's provider.
    pub host: String,
    /// Provider kind that owns this account choice.
    pub kind: AccountKind,
    /// Human-readable login for the account choice.
    pub login: String,
    /// Opaque identifier supplied to the next login step to select this account.
    pub selection_id: String,
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
pub struct AuthLoginStepOpenUrl {
    /// Login flow step variant discriminator.
    pub kind: AuthLoginStepOpenUrlKind,
    /// Authorize URL the consumer should open in a browser (consumer-driven browser-open).
    pub url: String,
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
pub struct AuthLoginStepInputRequired {
    /// Login flow step variant discriminator.
    pub kind: AuthLoginStepInputRequiredKind,
    /// Prompt for the value the provider needs; the consumer supplies it as advance input (e.g. a GitHub Enterprise Cloud host, *.ghe.com).
    pub prompt: String,
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
pub struct AuthLoginStepAwaiting {
    /// Login flow step variant discriminator.
    pub kind: AuthLoginStepAwaitingKind,
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
pub struct AuthLoginStepNeedsInteraction {
    /// Login flow step variant discriminator.
    pub kind: AuthLoginStepNeedsInteractionKind,
}

/// Result of an interactive login flow. Pending consent or account selection is not terminal.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AuthLoginResultDto {
    /// Available accounts when sign-in is awaiting account selection, ordered with Microsoft 365 first.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub accounts: Option<Vec<AuthLoginAccount>>,
    /// Host that was signed in, when completed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub host: Option<String>,
    /// Login that was signed in, when completed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub login: Option<String>,
    /// Current disposition of the login, including pending user decisions.
    pub status: AuthLoginResultStatus,
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
pub struct AuthLoginStepCompleted {
    /// Login flow step variant discriminator.
    pub kind: AuthLoginStepCompletedKind,
    /// Login result. When status is needs-plaintext-consent or needs-account-selection, advance with the user's decision to continue.
    pub result: AuthLoginResultDto,
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
pub struct AuthLoginStepError {
    /// Login flow step variant discriminator.
    pub kind: AuthLoginStepErrorKind,
    /// Human-readable failure message.
    pub message: String,
}

/// Validation error from an authentication attempt.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AuthValidationError {
    /// Optional message returned by GitHub
    #[serde(skip_serializing_if = "Option::is_none")]
    pub github_message: Option<String>,
    /// Authentication validation error message
    pub message: String,
}

/// Current per-window credit limit and consumption for an autopilot objective.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AutopilotObjectiveCreditLimit {
    /// Configured AI-credit cap, when one is set.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub credits: Option<f64>,
    /// Window consumption in fractional AI credits, for display.
    pub credits_used: f64,
    /// Exact window consumption in non-negative integer nano-AIU, encoded as a decimal string.
    pub credits_used_nano_aiu: String,
}

/// Public, persistence-independent projection of an autopilot objective.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AutopilotObjectiveState {
    /// Optional summary recorded when the objective completed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub completion_summary: Option<String>,
    /// Exact lifetime AI-credit consumption in non-negative integer nano-AIU, encoded as a decimal string.
    pub credit_count_nano_aiu: String,
    /// Current per-window consumption and optional cap, when a credit-tracking window is present.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub credit_limit: Option<AutopilotObjectiveCreditLimit>,
    /// Session-local objective identifier.
    pub id: i64,
    /// User-provided objective text.
    pub objective: String,
    /// Optional reason the objective is paused.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub pause_reason: Option<String>,
    /// Current normalized lifecycle status.
    pub status: AutopilotObjectiveStatus,
    /// Number of objective turns started.
    pub turn_count: i64,
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
pub struct AutopilotOptions {
    /// Enable runtime-owned automatic continuation while in Autopilot mode. Defaults to true for SDK sessions; does not change the session mode.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub continuation_enabled: Option<bool>,
    /// Maximum automatic continuations outside an active objective. Zero allows none; defaults to five. Omitted values preserve the current limit on update or warm resume.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_continues: Option<i64>,
}

/// Server-driven promotion for an Auto routing preference. It can be announced before it starts.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AutoTierBillingPromo {
    /// Percentage discount (0-100) while the promotion is active. Absent or 0 means the promotion is an announcement only: do not present a discount.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub discount_percent: Option<f64>,
    /// UTC ISO 8601 timestamp marking when the promotion ends. Absent means open-ended. The runtime omits promotions that have already ended.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub ends_at: Option<String>,
    /// Stable identifier for the promotion campaign.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub id: Option<String>,
    /// Human-readable promotion message. Does not include the start or end timestamps.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub message: Option<String>,
    /// Whether the service asked hosts to give this promotion a prominent surface, such as a banner. Absent means the service expressed no preference, so hosts should apply their own default rather than read it as `false`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub show_banner: Option<bool>,
    /// UTC ISO 8601 timestamp marking when the promotion starts. The service can announce a promotion before it starts, so do not present the discount as active before this time. Absent means it has already started.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub starts_at: Option<String>,
}

/// Billing information for a server-advertised routing preference.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AutoTierBilling {
    /// Promotion the service attached to this preference, if any.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub promo: Option<AutoTierBillingPromo>,
}

/// Availability of a server-advertised routing preference.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AutoTierStatus {
    /// Whether the provider permits selecting this preference.
    pub enabled: bool,
    /// Human-readable explanation of availability.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub message: Option<String>,
    /// Whether hosts should list this preference in a picker. Absent means visible. A hidden preference can still be requested explicitly through the SDK.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub picker_visible: Option<bool>,
    /// Extensible machine-readable unavailability reason.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reason: Option<String>,
}

/// A server-advertised routing preference. Identifiers and execution types are extensible.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AutoTierDescriptor {
    /// Billing information for this preference, such as an announced or active promotion.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub billing: Option<AutoTierBilling>,
    /// Description displayed beside the preference.
    pub description: String,
    /// Human-readable label, not a routing identifier.
    pub display_name: String,
    /// Runtime-owned managed Fusion tier represented by this execution descriptor, if any.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub fusion_tier: Option<String>,
    /// Opaque routing identifier transmitted unchanged to the provider.
    pub id: String,
    /// Current account-specific availability.
    pub status: AutoTierStatus,
    /// Execution kind; supported preferences use `auto` or the runtime's `fusion` modes.
    pub r#type: String,
}

/// Account-bound discovery metadata for the virtual `auto` model.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AutoTierMetadata {
    /// Provider-default preference, used only when no explicit preference exists.
    pub default_tier: String,
    /// Provider that supplied this metadata, when the catalog is provider-attributed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub provider_id: Option<String>,
    /// Routing preferences in the server's presentation order.
    pub tiers: Vec<AutoTierDescriptor>,
}

/// A well-known model in the runtime's built-in catalog.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct BuiltInModelCatalogEntry {
    /// Well-known runtime model ID suitable for provider or provider-model metadata. This is not necessarily the provider-facing deployment or model name and does not indicate CAPI entitlement or provider availability.
    pub id: String,
}

/// Canvas action that the agent or host can invoke. To discover the input schema for a particular action, call the list_canvas_capabilities tool.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CanvasAction {
    /// Description of the action
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// JSON Schema for the action input
    #[serde(skip_serializing_if = "Option::is_none")]
    pub input_schema: Option<serde_json::Value>,
    /// Action name exposed by the canvas provider
    pub name: String,
}

/// Canvas available in the current session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DiscoveredCanvas {
    /// Actions the agent or host may invoke on an open instance
    #[serde(skip_serializing_if = "Option::is_none")]
    pub actions: Option<Vec<CanvasAction>>,
    /// Provider-local canvas identifier
    pub canvas_id: String,
    /// Short, single-sentence description shown to the agent in canvas catalogs.
    pub description: String,
    /// Human-readable canvas name
    pub display_name: String,
    /// Owning provider identifier
    pub extension_id: String,
    /// Owning extension display name, when available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub extension_name: Option<String>,
    /// Host-local PNG path for the canvas icon, when supplied
    #[serde(skip_serializing_if = "Option::is_none")]
    pub icon: Option<String>,
    /// JSON Schema for canvas open input
    #[serde(skip_serializing_if = "Option::is_none")]
    pub input_schema: Option<serde_json::Value>,
}

/// Open canvas instance snapshot.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct OpenCanvasInstance {
    /// Provider-local canvas identifier
    pub canvas_id: String,
    /// Owning provider identifier
    pub extension_id: String,
    /// Owning extension display name, when available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub extension_name: Option<String>,
    /// Host-local PNG path for the canvas icon, when supplied
    #[serde(skip_serializing_if = "Option::is_none")]
    pub icon: Option<String>,
    /// Input supplied when the instance was opened
    #[serde(skip_serializing_if = "Option::is_none")]
    pub input: Option<serde_json::Value>,
    /// Stable caller-supplied canvas instance identifier
    pub instance_id: String,
    /// Provider-supplied status text
    #[serde(skip_serializing_if = "Option::is_none")]
    pub status: Option<String>,
    /// Rendered title
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
    /// URL for web-rendered canvases
    #[serde(skip_serializing_if = "Option::is_none")]
    pub url: Option<String>,
}

/// Options scoped to the built-in CAPI (Copilot API) provider.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CapiSessionOptions {
    /// Routing preference for sessions whose model is `auto`. On create or cold resume, this establishes the preference sent as `tier` on CAPI `/auto` requests; when omitted on cold resume, the runtime restores the last committed preference. On resident resume, a different value requests a safe switch after resume succeeds and cannot change an in-flight turn. Successful switches are persisted for later cold resume. When no preference is supplied or restored, CAPI default routing is used. `fast` is an integrator-only latency preset, not a first-party GitHub Copilot product preference.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auto_tier: Option<AutoTier>,
    /// Whether to use WebSocket transport for the CAPI Responses API. Enabled by default when the model advertises `ws:/responses` support; set to `false` to force the HTTP Responses transport in environments where WebSockets are blocked (e.g. behind a proxy). Setting this to `false` is equivalent to the `COPILOT_CLI_DISABLE_WEBSOCKET_RESPONSES` environment variable.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_web_socket_responses: Option<bool>,
}

/// An optional catalog authentication exchange did not establish the caller's identity. Anonymous search remains supported; this refusal is reserved for an operation that cannot continue after the attempted exchange. It is distinct from `policy-rejected` and from a network failure, and the reason identifies the recovery action.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogAuthenticationRequiredError {
    /// Discriminator: the caller is not authenticated
    pub kind: CatalogAuthenticationRequiredErrorKind,
    /// Human-readable explanation, safe to surface. Never contains a credential or token, nor a query, URL, handle, or secret.
    pub message: String,
    /// Why authentication failed. Only an expired credential justifies attempting a silent refresh; an absent or rejected credential requires sign-in.
    pub reason: CatalogAuthenticationRequiredReason,
}

/// The protocol version and capability set a caller requires, supplied on every catalog request so negotiation cannot be skipped by omission.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogClientContract {
    /// SDK protocol version the caller was generated against. A caller below the runtime's minimum supported version is refused rather than served a partial result.
    pub protocol_version: i64,
    /// Wire features the caller requires the runtime to understand. Identifiers are bounded but extensible so a newer caller can negotiate with an older runtime. Requiring an unknown feature yields a typed refusal listing what is understood, never a partial grant. A grant does not promise that a deployment has enabled the operation; typed unavailable results report that separately.
    pub required_capabilities: Vec<String>,
}

/// An upstream catalog response broke the wire contract. Most importantly, every result must carry exactly one of a URL or embedded data: a result carrying both, or neither, is refused here rather than being guessed at.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogContractViolationError {
    /// Discriminator: the upstream response broke the contract
    pub kind: CatalogContractViolationErrorKind,
    /// Human-readable explanation, safe to surface. Never echoes response content, nor a query, URL, handle, or secret.
    pub message: String,
    /// Which rule the response broke.
    pub reason: CatalogContractViolationReason,
}

/// The protocol version and capability set the runtime actually honoured for a successful catalog operation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogNegotiatedContract {
    /// Wire features the runtime understood for this operation. Includes the five original catalog capabilities and only explicitly requested supported additions, in supported order without duplicates. Capabilities that introduce new success-union variants or operations are therefore included only when explicitly required, preserving older protocol-v3 clients. Always a superset of the caller's required features, because any shortfall is a refusal instead. Operation availability remains a separate typed result.
    pub granted_capabilities: Vec<CatalogCapability>,
    /// Protocol version of the runtime that served the request.
    pub runtime_protocol_version: i64,
}

/// The request was rejected because a bounded field fell outside its permitted range or a required field was unusable. Pagination may also be rejected by the authority after a continuation request; repeat the search without page.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogInvalidRequestError {
    /// Which request field was rejected.
    pub field: CatalogInvalidRequestField,
    /// Discriminator: the request itself was invalid
    pub kind: CatalogInvalidRequestErrorKind,
    /// Human-readable explanation, safe to surface. Never echoes the offending value, nor a query, URL, handle, or secret.
    pub message: String,
}

/// A card could not be parsed or did not satisfy its declared media type's schema.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogMalformedCardError {
    /// Discriminator: the card was malformed
    pub kind: CatalogMalformedCardErrorKind,
    /// Media type the card was interpreted as, when it declared one this runtime recognises.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub media_type: Option<CatalogMediaType>,
    /// Human-readable explanation, safe to surface. Never echoes card content, nor a query, URL, handle, or secret.
    pub message: String,
    /// How the card failed validation.
    pub reason: CatalogMalformedCardReason,
}

/// The caller's protocol version or required capabilities cannot be honoured. Returned instead of a partial or ambiguous success.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogNegotiationRefusedError {
    /// Discriminator: capability or protocol-version negotiation failed
    pub kind: CatalogNegotiationRefusedErrorKind,
    /// Human-readable explanation, safe to surface. Never contains a query, URL, handle, or secret.
    pub message: String,
    /// Lowest caller protocol version this runtime will serve.
    pub minimum_supported_protocol_version: i64,
    /// Whether the version or the capability set was the problem.
    pub reason: CatalogNegotiationRefusedReason,
    /// Protocol version of the runtime that refused the request.
    pub runtime_protocol_version: i64,
    /// Capabilities this runtime can safely advertise to this caller. The complete five-capability protocol-3 legacy set is always present; every capability added after that baseline appears only when the caller required it, so an older closed-enum decoder can still consume a refusal. This list does not imply that every deployment has enabled every operation.
    pub supported_capabilities: Vec<String>,
    /// The subset of the caller's bounded extensible capability identifiers this runtime cannot honour.
    pub unsupported_capabilities: Vec<String>,
}

/// The runtime could not reach the catalog authority or retrieve a card. Covers being offline as well as transport-level failure.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogNetworkFailureError {
    /// Discriminator: the network operation failed
    pub kind: CatalogNetworkFailureErrorKind,
    /// Human-readable explanation, safe to surface. Never contains a query, URL, handle, or secret.
    pub message: String,
    /// Categorised failure, low cardinality so it can be aggregated without carrying a URL.
    pub reason: CatalogNetworkFailureReason,
    /// Bounded cooldown in seconds before another catalog request should be attempted, when the authority supplied a numeric Retry-After value or the runtime applied its documented fallback.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub retry_after_seconds: Option<i32>,
    /// HTTP status code, when the failure was a rejected response.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub status_code: Option<i32>,
}

/// Registry or enterprise policy refused the operation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogPolicyRejectedError {
    /// Discriminator: policy refused the operation
    pub kind: CatalogPolicyRejectedErrorKind,
    /// Human-readable explanation, safe to surface. Never contains a query, URL, handle, or secret.
    pub message: String,
    /// Which authority produced the decision.
    pub source: McpPlanPolicySource,
}

/// Catalogue identity retained from a bound candidate or plan at installation time.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InstallationCatalogueIdentity {
    /// Catalogue description retained at install planning time.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Human display name retained from the catalogue candidate.
    pub display_name: String,
    /// HTTPS URL of the resource's icon on raw.githubusercontent.com, retained from the catalogue
    /// candidate. Untrusted external content: load it without credentials. Omitted when the
    /// candidate had none.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub icon_url: Option<String>,
    /// Catalogue item URL when supplied by the authority.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub item_url: Option<String>,
    /// Catalogue publisher retained at install planning time.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub publisher: Option<String>,
    /// Authority resource identifier when supplied by the catalogue.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub resource_id: Option<String>,
    /// Catalogue authority/source string that supplied the candidate.
    pub source: String,
    /// Catalogue trust observation retained at install planning time.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub trust_at_install: Option<serde_json::Value>,
    /// Catalogue version retained at install planning time.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub version: Option<String>,
}

/// One reviewed Skill file.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillInstallationFileReview {
    /// SHA-256 digest of the exact file bytes.
    pub digest: String,
    /// Whether the file is installed with executable permissions.
    pub executable: bool,
    /// Declared media type for the file.
    pub media_type: String,
    /// Relative file path within the Skill root.
    pub path: String,
    /// Exact reviewed file size in bytes.
    pub size_bytes: i64,
}

/// Source identity retained from Agent Finder and the pinned GitHub descriptor.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillInstallationSource {
    /// Digest of the descriptor's bundle manifest.
    pub bundle_digest: String,
    /// Agent Finder materialisation revision identifier.
    pub catalog_revision_id: String,
    /// Digest of the canonical materialisation descriptor.
    pub descriptor_digest: String,
    /// Repository full name, for example owner/name.
    pub repository: String,
    /// GitHub repository database identifier.
    pub repository_id: String,
    /// Agent Finder resource identifier.
    pub resource_id: String,
    /// Pinned Git commit revision.
    pub revision: String,
    /// Root path within the pinned repository.
    pub root: String,
}

/// A user-facing personal Skill installation location without absolute host paths.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillInstallationLocation {
    /// Diagnostics-only absolute host path. Hosts must not display it by default.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub diagnostics_absolute_path: Option<String>,
    /// Safe display label, for example ~/.copilot/skills/run-checks.
    pub display_label: String,
    /// Path relative to the Copilot home.
    pub relative_path: String,
    /// Installation scope. Agent Finder Skills are installed in the user's personal Copilot home.
    pub scope: SkillInstallationScope,
}

/// Review for installing a verified Skill.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillInstallationReviewInstall {
    /// Exact reviewed Skill installation action.
    pub action: SkillInstallationReviewInstallAction,
    /// Catalogue identity retained from the bound candidate before consent.
    pub catalogue: InstallationCatalogueIdentity,
    /// Skill description from SKILL.md when present.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Complete verified SKILL.md content. Planning refuses with review-too-large
    /// when this exceeds 262144 UTF-8 bytes; it is never truncated.
    pub entrypoint_content: String,
    /// Relative path of the verified Skill entrypoint.
    pub entrypoint_path: String,
    /// Reviewed files and digests.
    pub files: Vec<SkillInstallationFileReview>,
    /// Installing never grants immediate use; the Skill is written disabled.
    pub installs_disabled: bool,
    /// Skill invocation name from SKILL.md.
    pub name: String,
    /// Exact verified source identity.
    pub source: SkillInstallationSource,
    /// Exact user-scope target location without an absolute host path.
    pub target: SkillInstallationLocation,
    /// Total reviewed payload size in bytes.
    pub total_bytes: i64,
}

/// Durable verified Skill ownership summary.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillInstallationSummary {
    /// Catalogue identity retained at install time.
    pub catalogue: InstallationCatalogueIdentity,
    /// Persisted enablement requested for this installation.
    pub configured_enabled: bool,
    /// Exact durable installation receipt identity.
    pub installation_id: String,
    /// ISO 8601 wall-clock installation time.
    pub installed_at: String,
    /// Skill invocation name.
    pub name: String,
    /// Operation that installed this Skill.
    pub operation_id: String,
    /// Ownership state observed from files and receipts.
    pub ownership_state: SkillInstallationOwnershipState,
    /// Bound-session load observation.
    pub session_state: SkillInstallationSessionState,
    /// Exact retained verified source identity.
    pub source: SkillInstallationSource,
    /// User-facing installation location without an absolute host path.
    pub target: SkillInstallationLocation,
}

/// Review for uninstalling an owned verified Skill.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillInstallationReviewUninstall {
    /// Exact reviewed Skill installation action.
    pub action: SkillInstallationReviewUninstallAction,
    /// Catalogue identity retained at install time.
    pub catalogue: InstallationCatalogueIdentity,
    /// Files recorded by the installation receipt.
    pub files: Vec<SkillInstallationFileReview>,
    /// Whether current files differ from the receipt. Apply refuses drift.
    pub files_modified: bool,
    /// Owned installation being removed.
    pub installation: SkillInstallationSummary,
    /// Total receipt-owned payload size in bytes.
    pub total_bytes: i64,
}

/// A computed Skill install plan. Nothing has been applied.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillInstallPlan {
    /// Original wall-clock expiry as an ISO 8601 timestamp.
    pub expires_at: String,
    /// Original operation identifier returned before confirmation.
    pub operation_id: String,
    /// One-use plan handle, bound to the original candidate authority.
    pub plan_handle: String,
    /// Safe review to present before applying the plan.
    pub review: SkillInstallationReview,
}

/// One reviewed plugin file.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginInstallationFileReview {
    /// SHA-256 digest of the exact file bytes.
    pub digest: String,
    /// Whether the file is installed with executable permissions.
    pub executable: bool,
    /// Declared media type for the file.
    pub media_type: String,
    /// Relative file path within the plugin root.
    pub path: String,
    /// Exact reviewed file size in bytes.
    pub size_bytes: i64,
}

/// One MCP server a reviewed plugin declares.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginInstallationMcpServerReview {
    /// Host of a remote server's URL.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub host: Option<String>,
    /// Server name as the plugin declares it.
    pub name: String,
    /// `remote` for an HTTP or SSE server, `local` for a command the plugin runs.
    pub transport: PluginInstallationMcpTransport,
}

/// Source identity retained from Agent Finder and the pinned GitHub descriptor.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginInstallationSource {
    /// Digest of the descriptor's bundle manifest.
    pub bundle_digest: String,
    /// Agent Finder materialisation revision identifier.
    pub catalog_revision_id: String,
    /// Digest of the canonical materialisation descriptor.
    pub descriptor_digest: String,
    /// Repository full name, for example owner/name.
    pub repository: String,
    /// GitHub repository database identifier.
    pub repository_id: String,
    /// Agent Finder resource identifier.
    pub resource_id: String,
    /// Pinned Git commit revision.
    pub revision: String,
    /// Plugin root within the pinned repository. Empty for the repository root.
    pub root: String,
}

/// Where a plugin is installed, without an absolute host path.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginInstallationLocation {
    /// Safe display label, for example ~/.copilot/installed-plugins/_direct/owner--repo--plugins-work-hub.
    pub display_label: String,
    /// Path relative to the Copilot home, under installed-plugins/_direct like any direct plugin install.
    pub relative_path: String,
}

/// Review for installing a verified Agent Plugin.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginInstallPlanReview {
    /// Exact reviewed Agent Plugin installation action.
    pub action: PluginInstallPlanReviewAction,
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

/// A computed Agent Plugin install plan. Nothing has been applied.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginInstallPlan {
    /// Original wall-clock expiry as an ISO 8601 timestamp.
    pub expires_at: String,
    /// Original operation identifier returned before confirmation.
    pub operation_id: String,
    /// One-use plan handle, bound to the original authority.
    pub plan_handle: String,
    /// Safe review to present before applying the plan.
    pub review: PluginInstallPlanReview,
}

/// Retrieval was refused by the runtime's hardened fetch boundary before any request left the process, or before a redirect was followed.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogUnsafeRetrievalError {
    /// Discriminator: retrieval was refused as unsafe
    pub kind: CatalogUnsafeRetrievalErrorKind,
    /// Human-readable explanation, safe to surface. Never contains the refused URL, nor a query, handle, or secret.
    pub message: String,
    /// Which control refused the retrieval, low cardinality so it can be aggregated without carrying a URL.
    pub reason: CatalogUnsafeRetrievalReason,
}

/// The operation is not available on this runtime. Distinct from a network failure: nothing was attempted.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogUnavailableError {
    /// Discriminator: the operation is not available
    pub kind: CatalogUnavailableErrorKind,
    /// Human-readable explanation, safe to surface. Never contains a query, URL, handle, or secret.
    pub message: String,
    /// Why the operation is unavailable.
    pub reason: CatalogUnavailableReason,
}

/// Authority-reported navigation metadata, returned only to callers requiring catalog-search-pagination and only when a supported token is present. Tokenless first-page and continuation responses omit this object; no counts are inferred from candidates. The opaque token may be retained for previous or numbered navigation even when hasNextPage is false.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CatalogSearchPagination {
    /// One-based page returned by the authority.
    pub current_page: i32,
    /// Whether the authority token advertises a valid next target within the navigation window. Not inferred from token presence, truncated, or currentPage being less than pageCount.
    pub has_next_page: bool,
    /// Navigation window ceiling ceil(1000 / pageSize), not the number of existing pages. Legal targets must not exceed this ceiling or the token's signed pageCount.
    pub max_page: i32,
    /// Backend-reported page count, which may exceed maxPage. Present pagination metadata always describes a multi-page result; zero- and single-page responses omit pagination. Navigation targets must also be within the signed pageCount carried by the supplied token.
    pub page_count: i64,
    /// Page size bound to the search, equal to the effective request limit.
    pub page_size: i32,
    /// Opaque pagination token. Session-bound search returns an expiring runtime-owned reference retaining the exact private authority token, original search and authority. Legacy unbound search returns the authority token unchanged, without a runtime-created expiry. Only the runtime unwraps tokens or changes targetPage; SDK consumers must not decode, modify or log them.
    pub token: String,
    /// Backend-reported count for this response, not the number of returned candidates. Its relationship to the full query result set is unknown.
    pub total_count: i64,
    /// The relationship of totalCount to the complete query result set is unknown; neither exactness nor a lower-bound guarantee is implied.
    pub total_count_relation: CatalogSearchTotalCountRelation,
}

/// A literal choice the command input accepts, with a human-facing description
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SlashCommandInputChoice {
    /// Human-readable description shown alongside the choice
    pub description: String,
    /// The literal choice value (e.g. 'on', 'off', 'show')
    pub name: String,
}

/// Optional unstructured input hint
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SlashCommandInput {
    /// Optional literal choices the input accepts, each with a human-facing description; clients may render these as selectable options
    #[serde(skip_serializing_if = "Option::is_none")]
    pub choices: Option<Vec<SlashCommandInputChoice>>,
    /// Optional completion hint for the input (e.g. 'directory' for filesystem path completion)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub completion: Option<SlashCommandInputCompletion>,
    /// Hint to display when command input has not been provided
    pub hint: String,
    /// When true, clients should pass the full text after the command name as a single argument rather than splitting on whitespace
    #[serde(skip_serializing_if = "Option::is_none")]
    pub preserve_multiline_input: Option<bool>,
    /// When true, the command requires non-empty input; clients should render the input hint as required
    #[serde(skip_serializing_if = "Option::is_none")]
    pub required: Option<bool>,
}

/// Slash-command metadata with name, aliases, description, kind, input hint, execution allowance, and schedulability.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SlashCommandInfo {
    /// Canonical aliases without leading slashes
    #[serde(skip_serializing_if = "Option::is_none")]
    pub aliases: Option<Vec<String>>,
    /// Whether the command may run while an agent turn is active
    pub allow_during_agent_execution: bool,
    /// Human-readable command description
    pub description: String,
    /// Whether the command is experimental
    #[serde(skip_serializing_if = "Option::is_none")]
    pub experimental: Option<bool>,
    /// Optional unstructured input hint
    #[serde(skip_serializing_if = "Option::is_none")]
    pub input: Option<SlashCommandInput>,
    /// Coarse command category for grouping and behavior: runtime built-in, skill-backed command, or SDK/client-owned command
    pub kind: SlashCommandKind,
    /// Canonical command name without a leading slash
    pub name: String,
    /// Whether the command may be the target of `/every` / `/after` schedules. Resolution happens at every tick, so only set this when the command is safe to re-invoke and produces an agent prompt.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub schedulable: Option<bool>,
}

/// A single host-driven completion. Accepting an item replaces `[rangeStart, rangeEnd)` (UTF-16 code units) in the composer with `insertText`; when the range is absent, the active token around the cursor is replaced.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionCompletionItem {
    /// Text spliced into the composer when the item is accepted.
    pub insert_text: String,
    /// Render-kind hint for the picker row (e.g. `"document"`, `"directory"`), derived from the host's display kind.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub kind: Option<String>,
    /// Primary display label for the picker row. Falls back to `insertText` when absent.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub label: Option<String>,
    /// End (exclusive) of the replacement range in `text`, in UTF-16 code units.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub range_end: Option<i64>,
    /// Start of the replacement range in `text`, in UTF-16 code units.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub range_start: Option<i64>,
}

/// Repository associated with the connected remote session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ConnectedRemoteSessionMetadataRepository {
    /// Branch associated with the remote session.
    pub branch: String,
    /// Repository name.
    pub name: String,
    /// Repository owner or organization login.
    pub owner: String,
}

/// Metadata for a connected remote session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ConnectedRemoteSessionMetadata {
    /// Neutral SDK discriminator for the connected remote session kind.
    pub kind: ConnectedRemoteSessionMetadataKind,
    /// Last session update time as an ISO 8601 string.
    pub modified_time: String,
    /// Optional friendly session name.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    /// Pull request number associated with the session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub pull_request_number: Option<i64>,
    /// Repository associated with the connected remote session.
    pub repository: ConnectedRemoteSessionMetadataRepository,
    /// Original remote resource identifier.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub resource_id: Option<String>,
    /// SDK session ID for the connected remote session.
    pub session_id: SessionId,
    /// Remote session staleness deadline as an ISO 8601 string.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub stale_at: Option<String>,
    /// Session start time as an ISO 8601 string.
    pub start_time: String,
    /// Remote session state returned by the backing service.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub state: Option<String>,
    /// Optional session summary.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub summary: Option<String>,
}

/// Account-targeted authorization update required by the Connector service. The account ID is an opaque host routing identifier; no credential is included.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ConnectorAuthorizationRequirement {
    /// Exact opaque account selection that made the Connector request.
    pub account_id: String,
    /// Stable OAuth scope the selected account must grant.
    pub scope: ConnectorAuthorizationScope,
}

/// Credential-free Connector catalog entry.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ConnectorCatalogEntry {
    /// Untrusted service description, when present.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Untrusted display label from the service.
    pub display_name: String,
    /// Optional catalog logo.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub logo: Option<String>,
    /// Canonical Connector name used by lifecycle methods.
    pub name: String,
    /// Optional catalog release tag.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub release_tag: Option<String>,
    /// Opaque stable runtime IDs currently projected into the session for this Connector.
    pub runtime_server_ids: Vec<String>,
    /// Current authoritative service connection state.
    pub status: ConnectorCatalogStatus,
    /// Optional catalog tier.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tier: Option<String>,
}

/// Validated Connector catalog snapshot cached by the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ConnectorCatalogResult {
    /// Validated catalog entries in service order.
    pub connectors: Vec<ConnectorCatalogEntry>,
    /// Unix epoch milliseconds when this snapshot was accepted.
    pub refreshed_at_ms: i64,
    /// Monotonically increasing session-local catalog revision.
    pub revision: i64,
}

/// Live status of one session-owned MCP projection.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ConnectorRuntimeStatus {
    /// Canonical Connector name that owns this server.
    pub connector_name: String,
    /// Opaque runtime server ID.
    pub runtime_server_id: String,
    /// Current live MCP host status.
    pub status: ConnectorMcpStatus,
}

/// Authoritative session connector state. Account IDs are opaque routing identifiers and credentials are never included.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ConnectorStatus {
    /// Opaque account selection pinned to this session, when one has been selected.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub account_id: Option<String>,
    /// Connector API contract version.
    pub api_version: i64,
    /// Exact selected account and stable scope requiring an authorization update, when proven by the Connector service.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub authorization_requirement: Option<ConnectorAuthorizationRequirement>,
    /// Current feature and session availability.
    pub availability: ConnectorAvailability,
    /// Latest validated catalog snapshot, when available.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub catalog: Option<ConnectorCatalogResult>,
    /// Number of active opaque connection continuations.
    pub pending_connections: i64,
    /// Live MCP status for every Connector-owned runtime server.
    pub runtime_servers: Vec<ConnectorRuntimeStatus>,
}

/// Content-exclusion decision for one requested path.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ContentExclusionPathCheck {
    /// Whether the session's complete content-exclusion policy excludes the path.
    pub excluded: bool,
    /// The path supplied by the caller.
    pub path: String,
}

/// A single large message currently in context.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ContextHeaviestMessage {
    /// Stable identifier for this message within the snapshot.
    pub id: String,
    /// Human-readable source label, e.g. `tool: bash` or `skill: tmux`. Presentation-only.
    pub label: String,
    /// Role of the chat message (`user`, `assistant`, or `tool`).
    pub role: String,
    /// Token count currently in context for this individual message.
    pub tokens: i64,
}

/// The session's authoritative model snapshot. Auto preference fields are configuration for the virtual `auto` model and do not change the selected model identifier. The context tier reflects `Session.getContextTier()`, restored from the session journal on resume.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CurrentModel {
    /// Auto preference currently claimed by an in-progress activation. Null means the activation is returning to provider-default routing.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub activating_auto_tier: Option<AutoTier>,
    /// Auto preference currently committed for the session. This can remain available while another model is selected so a later switch to `auto` can reuse it.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auto_tier: Option<AutoTier>,
    /// Context tier for models that support multiple context-window sizes.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context_tier: Option<ContextTier>,
    /// Currently active model identifier
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_id: Option<String>,
    /// Latest unclaimed Auto preference waiting for a future user turn. Null means the pending request is returning to provider-default routing.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub pending_auto_tier: Option<AutoTier>,
    /// Captured base model to restore when leaving plan mode. Omitted outside plan mode or when no plan override has captured a base model. Persistent agent model requirements apply to this model rather than the temporary plan model.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub plan_base_model_id: Option<String>,
    /// Provider of planBaseModelId, when that saved selection is provider-qualified.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub plan_base_provider_id: Option<String>,
    /// Provider selected for this model. Together with modelId, identifies a catalog entry even when providers serve the same model. Omitted for unattributed legacy selections.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub provider_id: Option<String>,
    /// Reasoning effort level currently applied to the active model, when one is set. Reads `Session.getReasoningEffort()` synchronously after `getSelectedModel()` resolves so the two values are reported as a snapshot.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_effort: Option<String>,
}

/// A file included in the session debug bundle.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DebugCollectLogsCollectedEntry {
    /// Relative path of the file in the staged bundle/archive.
    pub bundle_path: String,
    /// Redacted output size in bytes.
    pub size_bytes: i64,
    /// Source category for this entry.
    pub source: DebugCollectLogsSource,
}

/// An optional debug bundle entry that could not be included.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DebugCollectLogsSkippedEntry {
    /// Relative path requested for this bundle entry.
    pub bundle_path: String,
    /// Server-local source path that could not be read.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub path: Option<String>,
    /// Reason the entry was skipped.
    pub reason: String,
}

/// MCP-specific detail for a source-discriminated diagnostic entry.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpDiagnosticDetails {
    /// Fresh identifier for the MCP connection attempt, including failed starts.
    pub connection_id: String,
    /// Serialized diagnostic detail. Protocol and HTTP records use JSON when detail is present.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub data: Option<String>,
    /// Protocol-frame direction when kind is protocol.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub direction: Option<McpDiagnosticDirection>,
    /// Diagnostic record category.
    pub kind: McpDiagnosticKind,
    /// Configured MCP server name.
    pub server_name: String,
}

/// MCP diagnostic source configuration.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpDiagnosticSourceConfiguration {
    /// Threshold to apply to MCP diagnostic producers in this session.
    pub level: DiagnosticLogLevel,
}

/// Typed diagnostic source configuration. At least one source is required by diagnostics configuration methods.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DiagnosticSourcesConfiguration {
    /// MCP diagnostic capture threshold. Omit to leave the current threshold unchanged.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mcp: Option<McpDiagnosticSourceConfiguration>,
}

/// Installed plugin that contributes a discovered extension.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DiscoveredExtensionPlugin {
    /// Installed plugin name
    pub name: String,
}

/// Discovered extension metadata and persistent enablement state.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DiscoveredExtension {
    /// Whether this extension's persistent per-ID preference is enabled
    pub enabled: bool,
    /// Source-qualified ID accepted by both server and session extension enablement methods
    pub id: String,
    /// Human-readable extension name
    pub name: String,
    /// Absolute path to the extension entry module, suitable for revealing it in a file manager
    pub path: String,
    /// Containing plugin metadata for plugin-contributed extensions
    #[serde(skip_serializing_if = "Option::is_none")]
    pub plugin: Option<DiscoveredExtensionPlugin>,
    /// Discovery source
    pub source: DiscoveredExtensionSource,
}

/// Vision-specific limits
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelCapabilitiesLimitsVision {
    /// Maximum image size in bytes
    #[serde(rename = "max_prompt_image_size")]
    pub max_prompt_image_size: i64,
    /// Maximum number of images per prompt
    #[serde(rename = "max_prompt_images")]
    pub max_prompt_images: i64,
    /// MIME types the model accepts
    #[serde(rename = "supported_media_types")]
    pub supported_media_types: Vec<String>,
}

/// Token limits for prompts, outputs, and context window
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelCapabilitiesLimits {
    /// Maximum total context window size in tokens
    #[serde(
        rename = "max_context_window_tokens",
        skip_serializing_if = "Option::is_none"
    )]
    pub max_context_window_tokens: Option<i64>,
    /// Maximum number of output/completion tokens
    #[serde(rename = "max_output_tokens", skip_serializing_if = "Option::is_none")]
    pub max_output_tokens: Option<i64>,
    /// Maximum number of prompt/input tokens
    #[serde(rename = "max_prompt_tokens", skip_serializing_if = "Option::is_none")]
    pub max_prompt_tokens: Option<i64>,
    /// Vision-specific limits
    #[serde(skip_serializing_if = "Option::is_none")]
    pub vision: Option<ModelCapabilitiesLimitsVision>,
}

/// Feature flags indicating what the model supports
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelCapabilitiesSupports {
    /// Resolved Anthropic adaptive-thinking capability — unsupported / optional / required / adaptive_only. 'required' models reject thinking.type='enabled' with HTTP 400 but still accept 'disabled' (e.g. opus-4.7/4.8/5, sonnet-5); 'adaptive_only' models accept nothing but 'adaptive' (e.g. fable, mythos).
    #[serde(rename = "adaptive_thinking", skip_serializing_if = "Option::is_none")]
    pub adaptive_thinking: Option<AdaptiveThinkingSupport>,
    /// Whether this model supports reasoning effort configuration
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_effort: Option<bool>,
    /// Whether the model supports provider-native thinking. Independent of configurable reasoning effort; omission means unknown.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub thinking: Option<bool>,
    /// Whether this model supports canonical tool calling
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_calls: Option<bool>,
    /// Whether this model supports vision/image input
    #[serde(skip_serializing_if = "Option::is_none")]
    pub vision: Option<bool>,
}

/// Model capabilities and limits
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelCapabilities {
    /// Token limits for prompts, outputs, and context window
    #[serde(skip_serializing_if = "Option::is_none")]
    pub limits: Option<ModelCapabilitiesLimits>,
    /// Feature flags indicating what the model supports
    #[serde(skip_serializing_if = "Option::is_none")]
    pub supports: Option<ModelCapabilitiesSupports>,
}

/// Provider-reported model artifact metadata.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelArtifactDetails {
    /// Provider-reported model architecture.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub architecture: Option<String>,
    /// Provider-reported model families.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub families: Option<Vec<String>>,
    /// Primary model family.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub family: Option<String>,
    /// Artifact format, such as `gguf`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub format: Option<String>,
    /// Provider-reported parameter count label.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub parameter_size: Option<String>,
    /// Provider-reported quantization label.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub quantization: Option<String>,
    /// Provider-reported tokenizer.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tokenizer: Option<String>,
}

/// Attribution for the adapter that produced a provider row.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelProviderProvenance {
    /// Stable opaque adapter identity from the effective catalog. Treat this as a whole identifier, not a parseable owner or kind.
    pub adapter_id: String,
    /// Human-readable contributor name, not the adapter display name.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub owner_display_name: Option<String>,
    /// Stable contributor identifier when the adapter has an owner outside the runtime. Independent of the contribution mechanism and not a routing key.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub owner_id: Option<String>,
    /// Descriptive provider family that produced this row; not a routing key.
    pub provider_kind: String,
    /// Kind of component that supplied the adapter.
    pub source: ModelProviderProvenanceSource,
}

/// A non-fatal provider observation warning.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelProviderWarning {
    /// Machine-readable warning code.
    pub code: String,
    /// Human-readable warning message.
    pub message: String,
}

/// A model offered for agent conversations. Missing capability metadata does not disqualify a candidate. Models known to be incompatible, such as embedding-only models, are excluded by the adapter.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DiscoveredModel {
    /// Provider-reported model capabilities. Omitted capability fields are unknown; explicit false values are preserved.
    pub capabilities: ModelCapabilities,
    /// Provider-reported model artifact details.
    pub details: ModelArtifactDetails,
    /// Provider-reported artifact digest.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub digest: Option<String>,
    /// Provider-native model identifier.
    pub id: String,
    /// Provider-reported last-modified timestamp.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub modified_at: Option<String>,
    /// Provider-reported display name.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    /// Attribution for the adapter that produced this model row.
    pub provenance: ModelProviderProvenance,
    /// Provider-reported artifact size in bytes.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub size_bytes: Option<i64>,
    /// Non-fatal warnings encountered while enriching this model.
    pub warnings: Vec<ModelProviderWarning>,
}

/// Typed provider-operation outcome. Use the code for control flow and the optional message for display.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelProviderOperationOutcome {
    /// Machine-readable operation outcome.
    pub code: ModelProviderOperationOutcomeCode,
    /// Human-readable detail for non-success outcomes.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub message: Option<String>,
}

/// An executable extension entry with runtime-verified plugin provenance.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct EligiblePluginExtensionEntry {
    /// Runtime-discovered extension identifier.
    pub id: String,
    /// Absolute executable module path discovered under an eligible plugin root.
    pub module_path: String,
}

/// Discovered extension metadata, including source-qualified ID, name, discovery source, status, and optional process ID.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Extension {
    /// Source-qualified ID (e.g., 'project:my-ext', 'user:auth-helper', 'plugin:my-plugin:my-ext')
    pub id: String,
    /// Extension name (directory name)
    pub name: String,
    /// Process ID if the extension is running
    #[serde(skip_serializing_if = "Option::is_none")]
    pub pid: Option<i64>,
    /// Discovery source: project (.github/extensions/), user (~/.copilot/extensions/), plugin (installed plugin), or session (session-state/`<id>`/extensions/)
    pub source: ExtensionSource,
    /// Current status: running, disabled, failed, or starting
    pub status: ExtensionStatus,
}

/// Binary result returned by a tool for the model
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExternalToolTextResultForLlmBinaryResultsForLlm {
    /// Base64-encoded binary data
    pub data: String,
    /// Human-readable description of the binary data
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Optional metadata from the producing tool.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub metadata: Option<HashMap<String, serde_json::Value>>,
    /// MIME type of the binary data
    pub mime_type: String,
    /// Binary result type discriminator. Use "image" for images and "resource" for other binary data.
    pub r#type: ExternalToolTextResultForLlmBinaryResultsForLlmType,
}

/// Installed plugin record from global state, with marketplace, version, install time, enabled state, cache path, and source.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InstalledPlugin {
    /// Path where the plugin is cached locally
    #[serde(rename = "cache_path", skip_serializing_if = "Option::is_none")]
    pub cache_path: Option<String>,
    /// Whether the plugin is currently enabled
    pub enabled: bool,
    /// Installation timestamp
    #[serde(rename = "installed_at")]
    pub installed_at: String,
    /// Absolute path of the marketplace directory a live plugin was resolved from. Present only on live, never-persisted records — those synthesized at session start for a directory/local marketplace, whose cache_path points at the real plugin directory on disk rather than a copy under the installed-plugins cache. Its presence is what marks a record as live, and no record carrying it is ever written to the persisted installedPlugins key.
    #[serde(rename = "installed_from", skip_serializing_if = "Option::is_none")]
    pub installed_from: Option<String>,
    /// Installation-time marketplace or direct source, independent of mutable marketplace aliases. Local paths are absolute. Absent for older installs; reinstall to record provenance.
    #[serde(rename = "installed_source", skip_serializing_if = "Option::is_none")]
    pub installed_source: Option<serde_json::Value>,
    /// Marketplace the plugin came from (empty string for direct repo installs)
    pub marketplace: String,
    /// Plugin name
    pub name: String,
    /// Source for direct repo installs (when marketplace is empty)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source: Option<serde_json::Value>,
    /// Per-plugin source fingerprint (a SHA-256 hash of the plugin's catalog source spec plus its resolved source subtree — NOT a Git commit SHA) captured at marketplace install/update time. Auto-update compares it against the freshly recomputed fingerprint to detect a content change that does not bump the version. Absent for pre-existing installs and for direct (non-marketplace) installs.
    #[serde(rename = "source_sha", skip_serializing_if = "Option::is_none")]
    pub source_sha: Option<String>,
    /// Version installed (if available)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub version: Option<String>,
}

/// Post-compaction context window usage breakdown
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HistoryCompactContextWindow {
    /// Token count from non-system messages (user, assistant, tool)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub conversation_tokens: Option<i64>,
    /// Current total tokens in the context window (system + conversation + tool definitions)
    pub current_tokens: i64,
    /// Current number of messages in the conversation
    pub messages_length: i64,
    /// Token count from system message(s)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub system_tokens: Option<i64>,
    /// Maximum token count for the model's context window
    pub token_limit: i64,
    /// Token count from tool definitions
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_definitions_tokens: Option<i64>,
}

/// A root user turn that the session can rewind to.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HistoryRewindPoint {
    /// Whether at least one file in this turn or a later turn can be restored.
    pub can_restore_files: bool,
    /// ID of the user.message event that begins the discarded suffix.
    pub event_id: String,
    /// Number of unique files in this turn and all later turns that have captured changes.
    pub file_count: i64,
    /// Whether this turn was an automatically injected autopilot continuation.
    pub is_autopilot_continuation: bool,
    /// Lines added by this turn's captured file changes.
    pub lines_added: i64,
    /// Lines removed by this turn's captured file changes.
    pub lines_removed: i64,
    /// ISO timestamp of the user turn.
    pub timestamp: String,
    /// Whether this turn itself captured any file changes.
    pub turn_changed_files: bool,
    /// User-visible message text for the turn.
    pub user_message: String,
}

/// A file that a conversation-and-files rewind would restore.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HistoryRewindFilePreview {
    /// Aggregate change made across the discarded turns.
    pub change_type: HistoryRewindChangeType,
    /// Lines added across the discarded turns.
    pub lines_added: i64,
    /// Lines removed across the discarded turns.
    pub lines_removed: i64,
    /// Absolute path of the captured file.
    pub path: String,
}

/// A captured file that rewind intentionally left unchanged.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HistorySkippedFileRestore {
    /// Absolute path of the skipped file.
    pub path: String,
    /// Reason the file was not restored.
    pub reason: HistoryFileRestoreSkipReason,
}

/// The GitHub repository and commit an installed plugin is pinned to.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InstalledPluginVerifiedSource {
    /// GitHub repository in `owner/repo` form.
    pub repository: String,
    /// Full 40-character hexadecimal commit SHA the plugin is pinned to.
    pub revision: String,
}

/// Information about an installed plugin tracked in global state.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InstalledPluginInfo {
    /// Description from the manifest of the installed copy. Present only for a direct install pinned to one GitHub commit, such as a verified install from GitHub.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Opaque, stable hash identifying a direct (non-marketplace) install source. Present only for direct repo / URL / local installs; absent for marketplace plugins. Same source yields the same id; distinct sources never collide.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub direct_source_id: Option<String>,
    /// Whether the plugin is currently enabled for new sessions
    pub enabled: bool,
    /// HTTPS URL of the plugin's icon on raw.githubusercontent.com, retained from the catalogue entry it was installed from. Untrusted external content: load it without credentials. Present only for a direct install pinned to one GitHub commit whose catalogue entry had an icon.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub icon_url: Option<String>,
    /// Whether the managed desired plugin currently has an installed or live record. Set to false for a managed desired entry retained in the listing after installation or reconciliation failed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub installed: Option<bool>,
    /// Absolute path of the marketplace directory a live plugin was resolved from. Present only on live, never-persisted records — a plugin belonging to a directory/local marketplace, which is loaded from its real directory on every pass instead of a copy under the installed-plugins cache. Its presence is what marks a listed plugin as live: such a plugin is always present on disk, so `enabled` is its only meaningful state and it is never "not installed".
    #[serde(skip_serializing_if = "Option::is_none")]
    pub installed_from: Option<String>,
    /// Whether enterprise managed settings control this plugin's enabled state.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub managed: Option<bool>,
    /// The enabled state required by enterprise managed settings, when this plugin spec is managed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub managed_desired_enabled: Option<bool>,
    /// Marketplace the plugin came from. Empty string ("") for direct repo / URL / local installs.
    pub marketplace: String,
    /// Plugin name
    pub name: String,
    /// Runtime-reported plugin provenance. Currently set to "builtin" only for plugins registered through the trusted host built-in boundary; absent for installed, marketplace, direct, and live plugins.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source: Option<String>,
    /// The GitHub repository and commit a direct install is pinned to, from its install record. Present only for a direct install pinned to one GitHub commit, such as a verified install from GitHub; absent for marketplace, local and unpinned installs.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub verified_source: Option<InstalledPluginVerifiedSource>,
    /// Installed version (when reported by the plugin manifest)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub version: Option<String>,
}

/// Loaded instruction source for a session, including path, content, category, location, applicability, and optional description.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InstructionSource {
    /// Glob pattern(s) from frontmatter — when set, this instruction applies only to matching files
    #[serde(skip_serializing_if = "Option::is_none")]
    pub apply_to: Option<Vec<String>>,
    /// Raw content of the instruction file
    pub content: String,
    /// When true, this source starts disabled and must be toggled on by the user
    #[serde(skip_serializing_if = "Option::is_none")]
    pub default_disabled: Option<bool>,
    /// Short description (body after frontmatter) for use in instruction tables
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Unique identifier for this source (used for toggling)
    pub id: String,
    /// Human-readable label
    pub label: String,
    /// Where this source lives — used for UI grouping
    pub location: InstructionSourceLocation,
    /// The project path this source was discovered from. Only set by sessionless discovery for repository, working-directory, and project-scoped plugin sources, where it disambiguates sources across multiple workspace roots. The session-scoped getSources leaves it unset.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub project_path: Option<String>,
    /// File path relative to repo or absolute for home
    pub source_path: String,
    /// Category of instruction source — used for merge logic
    pub r#type: InstructionSourceType,
}

/// Plugin entry advertised by a marketplace.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MarketplacePluginInfo {
    /// Short description from the marketplace catalog, when present
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Plugin name as listed in the marketplace catalog
    pub name: String,
}

/// Registered marketplace summary.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MarketplaceInfo {
    /// Whether the managed marketplace currently resolved into the runtime marketplace registry. Set to false when the desired managed entry is retained for governance visibility after loading or reconciliation failed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub available: Option<bool>,
    /// True when this is a default marketplace shipped with the runtime. Defaults are not removable.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub is_default: Option<bool>,
    /// Whether enterprise managed settings provide and control this marketplace entry.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub managed: Option<bool>,
    /// Marketplace name (matches the @marketplace suffix in plugin specs)
    pub name: String,
    /// Human-readable description of where the marketplace data is fetched from (e.g. "GitHub: owner/repo").
    pub source: String,
}

/// Per-marketplace refresh result, including marketplace name, success flag, and optional failure error.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MarketplaceRefreshEntry {
    /// Error message (failure only)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Marketplace name that was refreshed
    pub name: String,
    /// Whether the refresh succeeded
    pub success: bool,
}

/// MCP server allowed by policy, with server name and optional PII-free explanatory note.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpAllowedServer {
    /// Allowed server name
    pub name: String,
    /// PII-free note explaining why the server was allowed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub redacted_note: Option<String>,
}

/// Capability negotiation snapshot
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpAppsDiagnoseCapability {
    /// Whether the runtime advertises `extensions.io.modelcontextprotocol/ui` to MCP servers
    pub advertised: bool,
    /// Whether the MCP_APPS feature flag (or COPILOT_MCP_APPS env override) is on
    pub feature_flag_enabled: bool,
    /// Whether the session has the `mcp-apps` capability
    pub session_has_mcp_apps: bool,
}

/// What the server returned for this session
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpAppsDiagnoseServer {
    /// Whether the named server is currently connected
    pub connected: bool,
    /// Up to 5 tool names with `_meta.ui` for quick inspection
    pub sample_tool_names: Vec<String>,
    /// Total tools returned by the server's tools/list
    pub tool_count: f64,
    /// Tools whose `_meta.ui` is populated (resourceUri and/or visibility set)
    pub tools_with_ui_meta: f64,
}

/// Current host context
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpAppsHostContextDetails {
    /// Display modes the host supports
    #[serde(skip_serializing_if = "Option::is_none")]
    pub available_display_modes: Option<Vec<McpAppsHostContextDetailsAvailableDisplayMode>>,
    /// Current display mode (SEP-1865)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub display_mode: Option<McpAppsHostContextDetailsDisplayMode>,
    /// BCP-47 locale, e.g. 'en-US'
    #[serde(skip_serializing_if = "Option::is_none")]
    pub locale: Option<String>,
    /// Platform type for responsive design
    #[serde(skip_serializing_if = "Option::is_none")]
    pub platform: Option<McpAppsHostContextDetailsPlatform>,
    /// UI theme preference per SEP-1865
    #[serde(skip_serializing_if = "Option::is_none")]
    pub theme: Option<McpAppsHostContextDetailsTheme>,
    /// IANA timezone, e.g. 'America/New_York'
    #[serde(skip_serializing_if = "Option::is_none")]
    pub time_zone: Option<String>,
    /// Host application identifier
    #[serde(skip_serializing_if = "Option::is_none")]
    pub user_agent: Option<String>,
}

/// MCP Apps resource content with URI, optional MIME type, text or base64 blob, and resource metadata.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpAppsResourceContent {
    /// Resource-level metadata (CSP, permissions, etc.)
    #[serde(rename = "_meta", skip_serializing_if = "Option::is_none")]
    pub meta: Option<HashMap<String, serde_json::Value>>,
    /// Base64-encoded binary content
    #[serde(skip_serializing_if = "Option::is_none")]
    pub blob: Option<String>,
    /// MIME type of the content
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mime_type: Option<String>,
    /// Text content (e.g. HTML)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub text: Option<String>,
    /// The resource URI (typically ui://...)
    pub uri: String,
}

/// Observational state for a matching already materialized MCP server.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpConfiguredServerState {
    /// Observed connection error, when the materialized server failed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Observed connection status. This is not a configuration or readiness guarantee.
    pub status: McpServerStatus,
    /// Whether the server's current connection has loaded its tool list (from tools/list or a cached snapshot). A connected server without loaded tools is still loading them; the change is signalled by session.mcp_server_tools_loaded. Absent when the runtime does not report it.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tools_loaded: Option<bool>,
}

/// Effective MCP configuration entry. Configuration enablement is distinct from the optional live observation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpConfiguredServer {
    /// Human-readable display name supplied by configuration.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub display_name: Option<String>,
    /// Whether this configured server is enabled after session configuration and policy filtering.
    pub enabled: bool,
    /// Observed state from an already materialized matching server. Omitted when no live graph has this configured server; it never determines configuration enablement.
    pub live: McpConfiguredServerState,
    /// Server name (config key)
    pub name: String,
    /// Configuration provenance: user, workspace, plugin, builtin, or managed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source: Option<McpServerSource>,
    /// Plugin name that provided this server, when source is plugin.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source_plugin: Option<String>,
    /// Plugin version that provided this server, when source is plugin.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source_plugin_version: Option<String>,
}

/// MCP server whose connection attempt failed.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpFailedServer {
    /// The captured connection failure detail.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// The config key of the server that failed to connect.
    pub name: String,
}

/// MCP server filtered by policy, with name, reason, and optional redacted reason.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpFilteredServer {
    /// Deprecated. This field is no longer populated.
    #[doc(hidden)]
    #[deprecated]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enterprise_name: Option<String>,
    /// Filtered server name
    pub name: String,
    /// Human-readable filter reason
    pub reason: String,
    /// PII-free filter reason
    #[serde(skip_serializing_if = "Option::is_none")]
    pub redacted_reason: Option<String>,
}

/// Recorded MCP server connection failure.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpServerFailureInfo {
    /// Failure message produced when the MCP server connection failed.
    pub message: String,
    /// epoch-ms timestamp at which the failure was recorded.
    pub timestamp: i64,
}

/// Recorded MCP server pending-auth state.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpServerNeedsAuthInfo {
    /// epoch-ms timestamp at which the server signalled it needs authentication.
    pub timestamp: i64,
}

/// Host-level state, omitted when no MCP host is initialized.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpHostState {
    /// Names of currently-connected MCP clients.
    pub clients: Vec<String>,
    /// Configured servers that are explicitly disabled.
    pub disabled_servers: Vec<String>,
    /// Map of server name to recorded connection failure.
    pub failed_servers: HashMap<String, McpServerFailureInfo>,
    /// Configured servers filtered out by MCP server policy.
    pub filtered_servers: Vec<String>,
    /// Whether third-party MCP servers are policy-enabled for this session.
    pub mcp3p_enabled: bool,
    /// Map of server name to recorded pending-auth state.
    pub needs_auth_servers: HashMap<String, McpServerNeedsAuthInfo>,
    /// Names of servers with in-flight connection attempts.
    pub pending_connections: Vec<String>,
}

/// Normalized MCP Apps discovery metadata from a tool's `_meta.ui` block.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpToolUi {
    /// URI of the tool's MCP App resource, typically a `ui://` resource identifier. Use `session.mcp.resources.read` to fetch its HTML and resource metadata.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub resource_uri: Option<String>,
    /// Tool visibility advertised by the server. When absent, MCP Apps defaults apply.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub visibility: Option<Vec<McpToolUiVisibility>>,
}

/// MCP tool metadata with tool name, optional description, and normalized MCP Apps discovery metadata.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpTools {
    /// Tool description, when provided.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Tool name.
    pub name: String,
    /// Normalized MCP Apps discovery metadata. An empty object indicates that a valid `_meta.ui` block was present without recognized fields.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub ui: Option<McpToolUi>,
}

/// An argument accepted by an MCP prompt.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpPromptArgument {
    /// Argument-level metadata
    #[serde(rename = "_meta", skip_serializing_if = "Option::is_none")]
    pub meta: Option<HashMap<String, serde_json::Value>>,
    /// Server-provided non-standard argument fields
    #[serde(skip_serializing_if = "Option::is_none")]
    pub additional_properties: Option<HashMap<String, serde_json::Value>>,
    /// Description of the argument
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Name of the argument
    pub name: String,
    /// Whether the argument is required; omission is distinct from false
    #[serde(skip_serializing_if = "Option::is_none")]
    pub required: Option<bool>,
}

/// An MCP prompt icon with standard size hints and preserved non-standard fields.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpPromptIcon {
    /// Server-provided non-standard icon fields
    #[serde(skip_serializing_if = "Option::is_none")]
    pub additional_properties: Option<HashMap<String, serde_json::Value>>,
    /// Icon MIME type, when known
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mime_type: Option<String>,
    /// Icon sizes, such as `48x48` or `any`
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sizes: Option<Vec<String>>,
    /// Icon URI
    pub src: String,
    /// Theme hint for this icon
    #[serde(skip_serializing_if = "Option::is_none")]
    pub theme: Option<String>,
}

/// An MCP prompt descriptor. Server-provided non-standard fields are exposed under `additionalProperties`.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpPrompt {
    /// Prompt-level metadata
    #[serde(rename = "_meta", skip_serializing_if = "Option::is_none")]
    pub meta: Option<HashMap<String, serde_json::Value>>,
    /// Server-provided non-standard descriptor fields
    #[serde(skip_serializing_if = "Option::is_none")]
    pub additional_properties: Option<HashMap<String, serde_json::Value>>,
    /// Arguments accepted by the prompt
    #[serde(skip_serializing_if = "Option::is_none")]
    pub arguments: Option<Vec<McpPromptArgument>>,
    /// Description of what this prompt provides
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Icons associated with this prompt
    #[serde(skip_serializing_if = "Option::is_none")]
    pub icons: Option<Vec<McpPromptIcon>>,
    /// The programmatic name of the prompt
    pub name: String,
    /// Human-readable display title
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
}

/// An MCP prompt message with opaque JSON content preserved without flattening or content-type filtering.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpPromptMessage {
    /// Message-level metadata
    #[serde(rename = "_meta", skip_serializing_if = "Option::is_none")]
    pub meta: Option<HashMap<String, serde_json::Value>>,
    /// Server-provided non-standard message fields
    #[serde(skip_serializing_if = "Option::is_none")]
    pub additional_properties: Option<HashMap<String, serde_json::Value>>,
    /// The original MCP content block, including nested metadata and unfamiliar content types
    pub content: serde_json::Value,
    /// The role of the message sender
    pub role: McpPromptRole,
}

/// Standard MCP resource annotations plus preserved non-standard annotation fields.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpResourceAnnotations {
    /// Server-provided non-standard annotation fields preserved from the MCP response
    #[serde(skip_serializing_if = "Option::is_none")]
    pub additional_properties: Option<HashMap<String, serde_json::Value>>,
    /// Intended audience roles for this resource
    #[serde(skip_serializing_if = "Option::is_none")]
    pub audience: Option<Vec<String>>,
    /// Last-modified timestamp hint
    #[serde(skip_serializing_if = "Option::is_none")]
    pub last_modified: Option<String>,
    /// Priority hint for model/client use
    #[serde(skip_serializing_if = "Option::is_none")]
    pub priority: Option<f64>,
}

/// A resource icon descriptor plus preserved non-standard icon fields.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpResourceIcon {
    /// Server-provided non-standard icon fields preserved from the MCP response
    #[serde(skip_serializing_if = "Option::is_none")]
    pub additional_properties: Option<HashMap<String, serde_json::Value>>,
    /// Icon MIME type, when known
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mime_type: Option<String>,
    /// Icon sizes hint
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sizes: Option<String>,
    /// Icon URI
    pub src: String,
    /// Theme hint for this icon
    #[serde(skip_serializing_if = "Option::is_none")]
    pub theme: Option<String>,
}

/// An MCP resource descriptor (spec `Resource`): URI, name, and optional title, description, MIME type, size, icons, annotations, and metadata. Server-provided fields outside the standard descriptor shape are exposed under `additionalProperties`.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpResource {
    /// Resource-level metadata
    #[serde(rename = "_meta", skip_serializing_if = "Option::is_none")]
    pub meta: Option<HashMap<String, serde_json::Value>>,
    /// Server-provided non-standard descriptor fields preserved from the MCP response
    #[serde(skip_serializing_if = "Option::is_none")]
    pub additional_properties: Option<HashMap<String, serde_json::Value>>,
    /// Model/client annotations associated with this resource
    #[serde(skip_serializing_if = "Option::is_none")]
    pub annotations: Option<McpResourceAnnotations>,
    /// Optional description of what this resource represents
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Icons associated with this resource
    #[serde(skip_serializing_if = "Option::is_none")]
    pub icons: Option<Vec<McpResourceIcon>>,
    /// MIME type of the resource, if known
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mime_type: Option<String>,
    /// The programmatic name of the resource
    pub name: String,
    /// Resource size in bytes, when known
    #[serde(skip_serializing_if = "Option::is_none")]
    pub size: Option<i64>,
    /// Optional human-readable display title
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
    /// The resource URI (e.g. ui://... or file:///...)
    pub uri: String,
}

/// MCP resource content with URI, optional MIME type, text or base64 blob, and resource metadata.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpResourceContent {
    /// Resource-level metadata (CSP, permissions, etc.)
    #[serde(rename = "_meta", skip_serializing_if = "Option::is_none")]
    pub meta: Option<HashMap<String, serde_json::Value>>,
    /// Base64-encoded binary content
    #[serde(skip_serializing_if = "Option::is_none")]
    pub blob: Option<String>,
    /// MIME type of the content
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mime_type: Option<String>,
    /// Text content (e.g. HTML)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub text: Option<String>,
    /// The resource URI
    pub uri: String,
}

/// An MCP resource template descriptor (spec `ResourceTemplate`): an RFC 6570 URI template, name, and optional title, description, MIME type, icons, annotations, and metadata. Server-provided fields outside the standard descriptor shape are exposed under `additionalProperties`.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpResourceTemplate {
    /// Resource-template-level metadata
    #[serde(rename = "_meta", skip_serializing_if = "Option::is_none")]
    pub meta: Option<HashMap<String, serde_json::Value>>,
    /// Server-provided non-standard descriptor fields preserved from the MCP response
    #[serde(skip_serializing_if = "Option::is_none")]
    pub additional_properties: Option<HashMap<String, serde_json::Value>>,
    /// Model/client annotations associated with this template
    #[serde(skip_serializing_if = "Option::is_none")]
    pub annotations: Option<McpResourceAnnotations>,
    /// Optional description of what this template is for
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Icons associated with resources matching this template
    #[serde(skip_serializing_if = "Option::is_none")]
    pub icons: Option<Vec<McpResourceIcon>>,
    /// MIME type for resources matching this template, if uniform
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mime_type: Option<String>,
    /// The programmatic name of the resource template
    pub name: String,
    /// Optional human-readable display title
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
    /// An RFC 6570 URI template for constructing resource URIs
    pub uri_template: String,
}

/// Owned installation that a listed MCP server's live configuration came from.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpServerOwnership {
    /// Stable installation identifier from the owned installation receipt.
    pub installation_id: String,
}

/// MCP server status entry, including config source/plugin source and any connection error.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpServer {
    /// Human-readable display name supplied by a managed server catalog.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub display_name: Option<String>,
    /// Error message if the server failed to connect
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Server name (config key)
    pub name: String,
    /// Owned installation this entry's live configuration came from. Absent for manual, workspace, plugin, builtin and same-name servers, and on runtimes without owned installations.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub owned: Option<McpServerOwnership>,
    /// Server-advertised metadata for a connected server. Omitted when no live connection metadata is available, including while pending or when failed, disabled, stopped, or not configured.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub server_metadata: Option<McpServerMetadata>,
    /// Configuration source: user, workspace, plugin, builtin, or managed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source: Option<McpServerSource>,
    /// Plugin name that provided this server, when source is plugin.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source_plugin: Option<String>,
    /// Plugin version that provided this server, when source is plugin.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source_plugin_version: Option<String>,
    /// Connection status: connected, failed, needs-auth, pending, disabled, stopped, or not_configured
    pub status: McpServerStatus,
    /// Configured URL for an HTTP/SSE server, regardless of configuration source. Omitted for local and in-memory servers.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub url: Option<String>,
}

/// Active server-driven promotion for a model, including its discount and optional expiry.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelBillingPromo {
    /// Percentage discount (0-100) applied while the promotion is active. May be fractional.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub discount_percent: Option<f64>,
    /// UTC ISO 8601 timestamp marking when the promotion ends. Optional: an open-ended promotion omits this field. When present, the API only surfaces a promo whose expiry parses and is in the future, so consumers should treat a past value as expired.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub ends_at: Option<String>,
    /// Stable identifier for the promotion campaign.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub id: Option<String>,
    /// Human-readable promotion message. Does not include the expiry timestamp; consumers may format endsAt and append it when present.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub message: Option<String>,
    /// Whether the service asked hosts to give this promotion a prominent surface, such as a dedicated banner, in addition to listing it with the model. `true` requests that surface and `false` asks for the model list only. Absent means the service expressed no preference — for example a response that predates the field — so hosts should apply their own default rather than read it as `false`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub show_banner: Option<bool>,
}

/// Long context tier pricing (available for models with extended context windows)
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelBillingTokenPricesLongContext {
    /// Use cacheReadPrice instead. AI Credits cost per billing batch of cached tokens
    #[doc(hidden)]
    #[deprecated]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cache_price: Option<f64>,
    /// AI Credits cost per billing batch of cached (read) tokens
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cache_read_price: Option<f64>,
    /// AI Credits cost per billing batch of 1-hour cache-write (cache creation) tokens.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cache_write1h_price: Option<f64>,
    /// AI Credits cost per billing batch of cache-write (cache creation) tokens.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cache_write_price: Option<f64>,
    /// Use maxPromptTokens instead. Prompt token budget for the long context tier. The total context window is this value plus the model's max_output_tokens.
    #[doc(hidden)]
    #[deprecated]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context_max: Option<i64>,
    /// AI Credits cost per billing batch of input tokens
    #[serde(skip_serializing_if = "Option::is_none")]
    pub input_price: Option<f64>,
    /// Prompt token budget for the long context tier. The total context window is this value plus the model's max_output_tokens.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_prompt_tokens: Option<i64>,
    /// AI Credits cost per billing batch of output tokens
    #[serde(skip_serializing_if = "Option::is_none")]
    pub output_price: Option<f64>,
}

/// Token-level pricing information for this model
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelBillingTokenPrices {
    /// Number of tokens per standard billing batch
    #[serde(skip_serializing_if = "Option::is_none")]
    pub batch_size: Option<i64>,
    /// Use cacheReadPrice when available. Cost per billing batch of cached tokens in the unit reported by models.listTokenPriceUnits.
    #[doc(hidden)]
    #[deprecated]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cache_price: Option<f64>,
    /// AI Credits cost per billing batch of cached (read) tokens
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cache_read_price: Option<f64>,
    /// AI Credits cost per billing batch of 1-hour cache-write (cache creation) tokens.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cache_write1h_price: Option<f64>,
    /// AI Credits cost per billing batch of cache-write (cache creation) tokens.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cache_write_price: Option<f64>,
    /// Use maxPromptTokens instead. Prompt token budget for the default tier. The total context window is this value plus the model's max_output_tokens.
    #[doc(hidden)]
    #[deprecated]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context_max: Option<i64>,
    /// Cost per billing batch of input tokens. The unit is reported by models.listTokenPriceUnits.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub input_price: Option<f64>,
    /// Long context tier pricing (available for models with extended context windows)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub long_context: Option<ModelBillingTokenPricesLongContext>,
    /// Prompt token budget for the default tier. The total context window is this value plus the model's max_output_tokens.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_prompt_tokens: Option<i64>,
    /// Cost per billing batch of output tokens. The unit is reported by models.listTokenPriceUnits.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub output_price: Option<f64>,
}

/// Billing information
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelBilling {
    /// Whole-number percentage discount (0-100) applied to usage billed through this model. Populated for the synthetic `auto` model, where requests routed by auto-mode are billed at a reduced rate; absent for concrete models.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub discount_percent: Option<i32>,
    /// Billing cost multiplier relative to the base rate
    #[serde(skip_serializing_if = "Option::is_none")]
    pub multiplier: Option<f64>,
    /// Active server-driven promotion for this model, if any. Present when the model is being promoted with a discount, which may be time-boxed or open-ended.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub promo: Option<ModelBillingPromo>,
    /// Token-level pricing information for this model
    #[serde(skip_serializing_if = "Option::is_none")]
    pub token_prices: Option<ModelBillingTokenPrices>,
}

/// A service-published message about a model, carrying a stable machine-readable code alongside human-readable text.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelMessage {
    /// Stable machine-readable identifier for the message, such as `client_version_deprecated`. Hosts can key custom presentation off this; unrecognized codes should fall back to displaying `message`.
    pub code: String,
    /// Human-readable message text intended for display to the user.
    pub message: String,
}

/// Policy state (if applicable)
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelPolicy {
    /// Current policy state for this model
    pub state: ModelPolicyState,
    /// Usage terms or conditions for this model
    #[serde(skip_serializing_if = "Option::is_none")]
    pub terms: Option<String>,
}

/// Service-published warning text that hosts should display when presenting a model.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelWarningText {
    /// Data-retention warning for the model. The text may contain Markdown links and should be rendered as Markdown when supported.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub data_retention: Option<String>,
}

/// Copilot model metadata, including identifier, display name, capabilities, policy, billing, reasoning efforts, and picker categories.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Model {
    /// Billing information
    #[serde(skip_serializing_if = "Option::is_none")]
    pub billing: Option<ModelBilling>,
    /// Model capabilities and limits
    pub capabilities: ModelCapabilities,
    /// Default reasoning effort level (only present if model supports reasoning effort)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub default_reasoning_effort: Option<String>,
    /// Model identifier (e.g., "claude-sonnet-4.5")
    pub id: String,
    /// Informational notices the service published for this model, such as an upcoming change or a recommended alternative. Present only when the service published at least one notice. Hosts should surface these without implying anything is wrong with the model.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub info_messages: Option<Vec<ModelMessage>>,
    /// Provider-supplied model metadata. Keys and JSON-compatible values are preserved unchanged. This is factual metadata published by the model provider; it carries no picker or UX semantics.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub metadata: Option<HashMap<String, serde_json::Value>>,
    /// Model capability category for grouping in the model picker
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_picker_category: Option<ModelPickerCategory>,
    /// Relative cost tier for token-based billing users
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_picker_price_category: Option<ModelPickerPriceCategory>,
    /// Display name
    pub name: String,
    /// Policy state (if applicable)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub policy: Option<ModelPolicy>,
    /// The native model provider that produced this model, as a neutral reference (opaque id, human-readable label, and provider kind). Preserved by `session.model.list` and `server.models.list` when supplied by the catalog producer; omitted for unattributed and configured BYOK models. This identifies the model source, not an account or agent harness.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub provider: Option<ModelProviderRef>,
    /// Context-window tiers this model offers, when the provider advertises them independently of tiered token pricing. Copilot models carry their tiers in `billing.tokenPrices`; a provider that has no pricing to publish (an agent host reached over AHP, for example) declares them here instead, so the model picker can still offer the tier toggle.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub supported_context_tiers: Option<Vec<String>>,
    /// Supported reasoning effort levels (only present if model supports reasoning effort)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub supported_reasoning_efforts: Option<Vec<String>>,
    /// Model vendor as the Copilot API reports it, for example "Anthropic" or "Azure OpenAI". Open vocabulary, passed through unchanged. It can name the vendor that serves the model instead of the one that built it, or a label that is not a vendor, such as "Experimental". Absent when the Copilot API reports no vendor.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub vendor: Option<String>,
    /// Warnings the service published for this model, such as a deprecated client version. Present only when the service published at least one warning. The model remains usable; hosts should surface these as advisory rather than blocking.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub warning_messages: Option<Vec<ModelMessage>>,
    /// Warning text the service requires hosts to surface for this model. Present only when the service published at least one warning.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub warning_text: Option<ModelWarningText>,
}

/// Vision-specific limits
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelCapabilitiesOverrideLimitsVision {
    /// Maximum image size in bytes
    #[serde(
        rename = "max_prompt_image_size",
        skip_serializing_if = "Option::is_none"
    )]
    pub max_prompt_image_size: Option<i64>,
    /// Maximum number of images per prompt
    #[serde(rename = "max_prompt_images", skip_serializing_if = "Option::is_none")]
    pub max_prompt_images: Option<i64>,
    /// MIME types the model accepts
    #[serde(
        rename = "supported_media_types",
        skip_serializing_if = "Option::is_none"
    )]
    pub supported_media_types: Option<Vec<String>>,
}

/// Token limits for prompts, outputs, and context window
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelCapabilitiesOverrideLimits {
    /// Maximum total context window size in tokens
    #[serde(
        rename = "max_context_window_tokens",
        skip_serializing_if = "Option::is_none"
    )]
    pub max_context_window_tokens: Option<i64>,
    /// Maximum number of output/completion tokens
    #[serde(rename = "max_output_tokens", skip_serializing_if = "Option::is_none")]
    pub max_output_tokens: Option<i64>,
    /// Maximum number of prompt/input tokens
    #[serde(rename = "max_prompt_tokens", skip_serializing_if = "Option::is_none")]
    pub max_prompt_tokens: Option<i64>,
    /// Vision-specific limits
    #[serde(skip_serializing_if = "Option::is_none")]
    pub vision: Option<ModelCapabilitiesOverrideLimitsVision>,
}

/// Feature flags indicating what the model supports
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelCapabilitiesOverrideSupports {
    /// Resolved Anthropic adaptive-thinking capability — unsupported / optional / required / adaptive_only. 'required' models reject thinking.type='enabled' with HTTP 400 but still accept 'disabled' (e.g. opus-4.7/4.8/5, sonnet-5); 'adaptive_only' models accept nothing but 'adaptive' (e.g. fable, mythos).
    #[serde(rename = "adaptive_thinking", skip_serializing_if = "Option::is_none")]
    pub adaptive_thinking: Option<AdaptiveThinkingSupport>,
    /// Whether this model supports reasoning effort configuration
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_effort: Option<bool>,
    /// Whether this model supports canonical tool calling
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_calls: Option<bool>,
    /// Whether this model supports vision/image input
    #[serde(skip_serializing_if = "Option::is_none")]
    pub vision: Option<bool>,
}

/// Optional capability overrides (vision, tool_calls, reasoning, etc.).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelCapabilitiesOverride {
    /// Token limits for prompts, outputs, and context window
    #[serde(skip_serializing_if = "Option::is_none")]
    pub limits: Option<ModelCapabilitiesOverrideLimits>,
    /// Feature flags indicating what the model supports
    #[serde(skip_serializing_if = "Option::is_none")]
    pub supports: Option<ModelCapabilitiesOverrideSupports>,
}

/// Filesystem and environment context used to resolve model-picker settings.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelPickerSettingsContext {
    /// Optional Copilot configuration directory containing persisted settings.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub config_dir: Option<String>,
    /// Environment variables consulted while resolving model-picker settings.
    pub environment: serde_json::Value,
    /// User home directory used when resolving persisted settings.
    pub home_directory: String,
}

/// Adapter-declared policy that tells clients whether discovery may run automatically.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelProviderAutomaticDiscoveryPolicy {
    /// Whether automatic discovery is allowed, limited to configured providers, or explicit-only.
    pub mode: ModelProviderAutomaticDiscoveryMode,
    /// Maximum network scope used by this adapter during discovery.
    pub network_scope: ModelProviderDiscoveryNetworkScope,
    /// True when discovery requires non-null caller input. Omission or null is rejected before adapter execution. When false, omitted or null input selects adapter defaults without schema validation.
    pub requires_input: bool,
    /// True when the adapter must be enabled by a trusted owner, such as a trusted extension, before automatic discovery may run.
    pub requires_trust: bool,
}

/// An operation supported by a model-provider adapter.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelProviderAdapterOperationDescriptor {
    /// Optional self-contained JSON Schema Draft 7 for non-null discovery input. Only supported on discover. No external references are resolved. Omitted or null input selects defaults when requiresInput is false. Without a schema, the adapter validates supplied input.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub input_schema: Option<serde_json::Value>,
    /// Supported operation name: `discover`, `getStatus`, or `models.list`. Unknown names and duplicate declarations are rejected.
    pub name: String,
}

/// Contributor attribution, independent of routing identity and authorization.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelProviderAttribution {
    /// Human-readable contributor name, not the adapter display name.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub owner_display_name: Option<String>,
    /// Stable contributor identifier. Required and nonblank for extension and custom sources; optional for built-in and configured sources. Does not grant authority.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub owner_id: Option<String>,
    /// Kind of component that supplied the adapter. Attribution does not confer authority.
    pub source: ModelProviderProvenanceSource,
}

/// A normalized model-provider adapter in the session's effective catalog.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelProviderAdapterDescriptor {
    /// Stable opaque identity for routing to this adapter. Unique in the effective catalog, independent of live registration generations.
    pub adapter_id: String,
    /// Adapter-declared policy for passive and automatic discovery.
    pub automatic_discovery: ModelProviderAutomaticDiscoveryPolicy,
    /// Human-readable provider name.
    pub display_name: String,
    /// Operations supported by this provider adapter.
    pub operations: Vec<ModelProviderAdapterOperationDescriptor>,
    /// Attribution for the adapter itself.
    pub provenance: ModelProviderAttribution,
    /// Descriptive provider family, such as `ollama`. Different adapters may have the same family; use adapterId for routing.
    pub provider_kind: String,
}

/// A BYOK model definition referencing a named provider.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ProviderModelConfig {
    /// Optional capability overrides (vision, tool_calls, reasoning, etc.).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub capabilities: Option<ModelCapabilitiesOverride>,
    /// Provider-local model id, unique within its provider. The session-wide selection id (shown in the model list and passed to switchTo) is the provider-qualified `provider/id`.
    pub id: String,
    /// Maximum context window tokens for the model.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_context_window_tokens: Option<i64>,
    /// Maximum output tokens for the model.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_output_tokens: Option<i64>,
    /// Maximum prompt/input tokens for the model.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_prompt_tokens: Option<i64>,
    /// Provider-published model metadata, preserved verbatim as the public Model.metadata object.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub metadata: Option<HashMap<String, serde_json::Value>>,
    /// Well-known base model id used for behavior/capability/config lookup. Defaults to `id`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_id: Option<String>,
    /// Display name for model pickers. Defaults to the provider-qualified selection id (`provider/id`).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    /// Name of the configured provider that serves this model.
    pub provider: String,
    /// System-message configuration used when the runtime builds the standard prompt for this provider-qualified model, including general-purpose subagents. It uses the same object hierarchy as session-level systemMessage configuration, except transform actions are rejected because the current callback protocol is not model-scoped. When present, it overrides the session-wide configuration on those prompt paths. Selected custom-agent and specialized-subagent prompts remain authoritative.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub system_message: Option<serde_json::Value>,
    /// The model name sent to the provider API for inference. Defaults to `id`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub wire_model: Option<String>,
}

/// Azure-specific provider options.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ProviderConfigAzure {
    /// API version. When set, uses the versioned deployment route. When omitted, uses the GA versionless v1 route.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub api_version: Option<String>,
}

/// External SDK input for a named custom model provider. Ingested by the native protocol boundary before host dispatch.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct NamedProviderConfig {
    /// Static API key used to authenticate provider requests.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub api_key: Option<String>,
    /// Azure authentication configuration for the provider.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub azure: Option<ProviderConfigAzure>,
    /// Base URL for provider API requests.
    pub base_url: String,
    /// Static bearer token used to authenticate provider requests.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub bearer_token: Option<String>,
    /// Whether the host supplies bearer tokens dynamically.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub has_bearer_token_provider: Option<bool>,
    /// Additional HTTP headers included with provider requests.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub headers: Option<HashMap<String, String>>,
    /// The product serving the provider's models, reported in telemetry as `model_provider`. Besides telemetry, `azure_openai` makes OpenAI Responses requests omit explicit `prompt_cache_breakpoint` markers, which limit cross-turn cache reuse on Azure.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_provider: Option<ProviderConfigModelProvider>,
    /// Unique provider name used to qualify model selection IDs.
    pub name: String,
    /// Transport used to communicate with the provider.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub transport: Option<ProviderConfigTransport>,
    /// Provider protocol family.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub r#type: Option<ProviderConfigType>,
    /// Wire API used to communicate with the provider.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub wire_api: Option<ProviderConfigWireApi>,
}

/// Serializable reference to a discovered provider instance.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelProviderInstanceReference {
    /// Stable opaque identity of the adapter that owns this reference. Must be present in the target session's effective catalog.
    pub adapter_id: String,
    /// Stable instance identifier derived by the provider adapter, such as `ollama:{normalizedEndpoint}`.
    pub id: String,
    /// Absolute provider management URI. The adapter validates normalization, supported schemes, and permission to access it against its bound configuration; a reference does not grant authority.
    pub management_endpoint: String,
    /// Descriptive provider family. Must match the selected adapter; not a routing key.
    pub provider_kind: String,
}

/// A normalized model-provider instance discovered by the runtime.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelProviderInstance {
    /// Human-readable instance name.
    pub display_name: String,
    /// Inference API endpoint when the provider exposes one separately from its management endpoint.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub inference_endpoint: Option<String>,
    /// Transport to use for inference against this instance.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub inference_transport: Option<ProviderEndpointTransport>,
    /// Provider family to use for inference against this instance.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub inference_type: Option<ProviderEndpointType>,
    /// Wire API to use for inference against this instance, when required by the provider family.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub inference_wire_api: Option<ProviderEndpointWireApi>,
    /// Attribution for the adapter that produced this instance.
    pub provenance: ModelProviderProvenance,
    /// Self-contained reference for subsequent provider operations.
    pub reference: ModelProviderInstanceReference,
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
pub struct ModelSwitchConfirmation {
    /// Current conversation token count before switching models.
    pub current_tokens: f64,
    /// Effective prompt-token limit with the target model's long_context tier, present only when that tier can fit the conversation and managed policy permits it. Resubmit the switch with contextTier set to long_context to avoid compaction.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub long_context_limit: Option<f64>,
    /// Target model token limit used by the compaction preflight.
    pub target_limit: f64,
    /// Display name of the model that requires compaction confirmation.
    pub target_model_display_name: String,
}

/// External tool call of the session or one of its sub-agents that is still waiting for session.tools.handlePendingToolCall.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PendingExternalToolRequest {
    /// Sub-agent instance identifier (the envelope agentId of its events) of the agent that issued the call; absent for calls issued by the root agent
    #[serde(skip_serializing_if = "Option::is_none")]
    pub agent_id: Option<String>,
    /// Arguments to pass to the external tool
    #[serde(skip_serializing_if = "Option::is_none")]
    pub arguments: Option<serde_json::Value>,
    /// Stable identity of the provider that offered the tool, for hosts that route extension-owned tools by provider
    #[serde(skip_serializing_if = "Option::is_none")]
    pub provider_id: Option<String>,
    /// Request ID to pass to session.tools.handlePendingToolCall
    pub request_id: RequestId,
    /// Tool call ID assigned to this external tool invocation
    pub tool_call_id: String,
    /// Name of the external tool to invoke
    pub tool_name: String,
}

/// Pending permission prompt reconstructed from event history, with request ID and user-facing prompt details.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PendingPermissionRequest {
    /// Canonical runtime-owned Assisted Permissions assessment captured on the originating permission request. Absent for historical events.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This type is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases.
    ///
    /// </div>
    #[serde(skip_serializing_if = "Option::is_none")]
    pub assisted_approval_assessment: Option<AssistedApprovalAssessment>,
    /// Permission-recovery episode that authorized this request to surface for interactive attention
    #[serde(skip_serializing_if = "Option::is_none")]
    pub recovery_episode_id: Option<String>,
    /// The user-facing permission prompt details (commands, write, read, mcp, url, memory, custom-tool, path, hook)
    pub request: PermissionPromptRequest,
    /// Unique identifier for the pending permission request
    pub request_id: RequestId,
}

/// Optional informational context describing how and where the permission decision was made. This does not affect permission behavior.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionContext {
    /// Disposition of the permission request as observed by the responding client.
    pub outcome: PermissionDecisionOutcome,
    /// Whether the responding client could ask a user interactively, was running headlessly, or had no response path. Omit when the client cannot determine this authoritatively.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub response_capability: Option<PermissionResponseCapability>,
    /// Controlled reason or actor responsible for the response.
    pub source: PermissionDecisionSource,
    /// Client surface that submitted the response.
    pub surface: PermissionDecisionSurface,
    /// Optional user-authored explanation of this decision, independent of approval scope. Collected only in restricted telemetry for accepted human responses to Assisted Permissions prompts. Never persisted in session history, sent to the agent or judge, or used to create approval rules. Existing rejection feedback is separate.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub user_feedback: Option<String>,
}

/// A single todo row read from the session SQL `todos` table. All fields are optional because the SQL schema is best-effort and the agent may not have populated every column.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PlanSqlTodosRow {
    /// Todo creation time, as stored by the session SQL schema's `datetime('now')` default: `YYYY-MM-DD HH:MM:SS` in UTC. Lets clients attribute todos to the work item that created them (e.g. scoping a goal's progress to the todos it produced) rather than to the whole session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub created_at: Option<String>,
    /// Todo description.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Todo identifier.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub id: Option<String>,
    /// Todo status.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub status: Option<String>,
    /// Todo title.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
}

/// A single dependency edge read from the session SQL `todo_deps` table, indicating that one todo must complete before another.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PlanSqlTodoDependency {
    /// ID of the todo it depends on.
    pub depends_on: String,
    /// ID of the todo that has the dependency.
    pub todo_id: String,
}

/// Session plugin metadata, with name, marketplace, optional version, and enabled state.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Plugin {
    /// Opaque stable identity for a direct plugin source.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub direct_source_id: Option<String>,
    /// Whether the plugin is currently enabled
    pub enabled: bool,
    /// Whether this managed desired plugin has an installed or live record.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub installed: Option<bool>,
    /// Absolute marketplace directory for a live plugin.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub installed_from: Option<String>,
    /// Whether enterprise managed settings control this plugin.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub managed: Option<bool>,
    /// Enabled state required by enterprise managed settings.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub managed_desired_enabled: Option<bool>,
    /// Marketplace the plugin came from
    pub marketplace: String,
    /// Plugin name
    pub name: String,
    /// Runtime plugin provenance, such as "builtin".
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source: Option<String>,
    /// Installed version
    #[serde(skip_serializing_if = "Option::is_none")]
    pub version: Option<String>,
}

/// Opt into native CAPI image generation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ProtocolImageGenerationOptions {
    /// Disabled by default for SDK sessions. CAPI model access and permissions still apply.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enabled: Option<bool>,
    /// Experimental. Save each generated or edited image under the session-state
    /// directory's `images` folder and add a `saved_to: <path>` line per image to
    /// the tool result the model sees. Defaults to true; set false to opt out.
    /// Subagents follow their
    /// parent's value unless they set their own. A failed save still returns the
    /// image, without `saved_to`. Sessions without a local session-state
    /// directory, such as those using a session filesystem provider, never save.
    /// Re-supply after a cold resume. Does not enable image generation.
    /// This option may change or be removed in a future release.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This type is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases.
    ///
    /// </div>
    #[serde(skip_serializing_if = "Option::is_none")]
    pub save_to_disk: Option<bool>,
    /// Experimental. Request a visible "Made with AI" label on generated and edited images.
    /// This option may change or be removed in a future release.
    /// Overrides the user's imageGenerationWatermark preference for this session;
    /// otherwise that preference applies, defaulting to false. Re-supply after a
    /// cold resume. Does not enable image generation or enforce enterprise policy.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This type is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases.
    ///
    /// </div>
    #[serde(skip_serializing_if = "Option::is_none")]
    pub watermark: Option<bool>,
}

/// Short-lived, rotating credential the caller must send on every request, in addition to `apiKey` if one is present. Omitted when the endpoint does not require one.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ProviderSessionToken {
    /// When the token expires, if known. Callers should refresh by calling `getEndpoint` again before this time, or reactively on any 401/403 response from `baseUrl`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub expires_at: Option<String>,
    /// HTTP header name the token must be sent under.
    pub header: String,
    /// The model the token is bound to, when applicable. When set, the token is only valid for requests against this model.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model: Option<String>,
    /// The short-lived token value.
    pub token: String,
}

/// User-facing pending queue entry, with kind and display text for a queued message, slash command, or model change.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct QueuePendingItems {
    /// Agent mode stored on this queued entry, as stamped when it was enqueued. Items without an explicit mode report interactive. This is not necessarily the mode that will constrain the turn: a plan or autopilot session applies its own write gate, continuation loop and permission posture to every drained item regardless of the mode stored here.
    pub agent_mode: SendAgentMode,
    /// Human-readable text to display for this queue entry in the UI
    pub display_text: String,
    /// Stable opaque id for the canonical queued item. Batch rows share one id.
    pub id: String,
    /// Whether this item is a queued user message or a queued slash command / model change
    pub kind: QueuePendingItemsKind,
    /// Stable identity of the queued user message. Present for message rows and absent for slash commands and model changes.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub message_id: Option<String>,
    /// Optional source tag associated with this pending queue entry. This is an open string, not authenticated authorship. In particular, `user` does not prove that a person typed the message. If the source is absent or unrecognized, consumers must not infer human or agent authorship and should handle the entry neutrally. Consumers should tolerate future source values.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source: Option<String>,
}

/// A pending session quota warning, consumed once by takeWarnings.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct QuotaWarningProjection {
    /// User-facing warning text.
    pub message: String,
    /// Related account or upgrade link.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub url: Option<String>,
    /// Existing warning category.
    pub warning_type: String,
}

/// Credential-injection capability flags applied while the sandbox is enabled. For the same capability independent of sandboxing, and matched to the credential's GitHub host, see `shell.credentials`; the two are additive.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SandboxConfigAuth {
    /// Whether to authenticate sandboxed gh through the local masking proxy. The child receives a fake GH_TOKEN; its real value is substituted only at github.com, api.github.com and uploads.github.com (github.com because gh repo clone authenticates git through gh auth git-credential). The repository's GitHub account takes precedence over the Copilot login. Default: false (opt-in).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub gh: Option<bool>,
    /// Whether to authenticate sandboxed HTTPS git through the local masking proxy. The child receives a fake `http.<url>.extraheader`; the real Authorization header is substituted only at its original HTTPS host, port, and repository path scope. github.com uses the Copilot token; other forges use credentials resolved from the user's own helper on the host. Default: false (opt-in).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub git: Option<bool>,
}

/// Destinations authorized to receive one masked environment credential.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SandboxMaskedEnvVar {
    /// Nonempty list of HTTPS injection hostnames or *.example.com patterns. Bare * is not accepted. These grants never override the sandbox network policy. Values in plaintext HTTP requests, URLs, bodies, encoded credentials, and signed requests are not substituted.
    pub inject_hosts: Vec<String>,
}

/// Whole-value environment credential masking for sandboxed children.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SandboxCredentialsConfig {
    /// Environment variable names and their HTTPS injection destinations. Absent variables stay absent. No real values or sentinels are stored in this map.
    pub env_vars: HashMap<String, SandboxMaskedEnvVar>,
}

/// macOS seatbelt experimental options.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SandboxConfigUserPolicyExperimentalSeatbelt {
    /// Whether the macOS seatbelt profile may access the keychain.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub keychain_access: Option<bool>,
}

/// Platform-specific experimental policy fields.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SandboxConfigUserPolicyExperimental {
    /// macOS seatbelt experimental options.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub seatbelt: Option<SandboxConfigUserPolicyExperimentalSeatbelt>,
}

/// Filesystem rules to merge into the base policy.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SandboxConfigUserPolicyFilesystem {
    /// Paths explicitly denied.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub denied_paths: Option<Vec<String>>,
    /// Paths granted read-only access.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub readonly_paths: Option<Vec<String>>,
    /// Paths granted read/write access.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub readwrite_paths: Option<Vec<String>>,
}

/// HTTP proxy configuration for sandboxed traffic.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SandboxConfigUserPolicyNetworkProxy {
    /// Optional password for proxy authentication, combined with the URL at spawn time. The persisted value may be a literal password, a `${secret:…}` reference resolved from the OS keychain, or a `${VAR}`/`$VAR` environment reference; it is resolved just before the sandboxed process routes through the proxy. The /sandbox dialog stores a real password in the OS keychain and persists only a `${secret:…}` placeholder (never plaintext in settings.json); the field is masked in the dialog and redacted by /settings show.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub password: Option<String>,
    /// Proxy URL (e.g. http://proxy.example.com:8080). The port is optional and defaults to the scheme's standard port when omitted; an explicit port must be between 1 and 65535. Credentials must not be embedded here — a `user:pass@` authority is rejected; put them in the separate `username`/`password` fields. A credential-free http:// loopback proxy URL is routed through the localhost proxy automatically; loopback covers localhost and any *.localhost subdomain, the whole 127.0.0.0/8 range, ::1, and IPv4-mapped loopback (::ffff:127.0.0.1). An https:// URL, or one with a username/password set, is used as-is.
    pub url: String,
    /// Optional username for proxy authentication. Combined with the URL (and `password`) into `user:pass@host` when the sandboxed process routes through the proxy.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub username: Option<String>,
}

/// Network rules to merge into the base policy.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SandboxConfigUserPolicyNetwork {
    /// Hosts allowed through the built-in sandbox proxy. A non-empty list denies unmatched hosts; an absent or empty list allows all hosts not blocked. Supports exact hostnames, IP addresses, and *.example.com for strict subdomains. Host rules do not override the outbound or local-network toggles.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub allowed_hosts: Option<Vec<String>>,
    /// Whether traffic to local/loopback addresses is allowed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub allow_local_network: Option<bool>,
    /// Whether outbound network traffic is allowed at all.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub allow_outbound: Option<bool>,
    /// Hosts denied by the built-in sandbox proxy. Deny rules take precedence over allowedHosts. A domain also denies all its subdomains. IP addresses match exactly; *.example.com matches strict subdomains, and * denies every host.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub blocked_hosts: Option<Vec<String>>,
    /// HTTP(S) proxy for sandboxed traffic. This is the built-in local proxy's upstream: every sandboxed command reaches it through a loopback listener, so credentials stay in the runtime and never reach the child. On Windows the sandbox also needs local network access, because it reaches that listener over host loopback. Configure credentials in the separate username/password fields. The transient local listener URL is never persisted.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub proxy: Option<SandboxConfigUserPolicyNetworkProxy>,
}

/// macOS seatbelt-specific options.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SandboxConfigUserPolicySeatbelt {
    /// Whether the macOS seatbelt profile may access the keychain.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub keychain_access: Option<bool>,
}

/// User-managed sandbox policy fragment merged into the auto-discovered base policy.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SandboxConfigUserPolicy {
    /// Deprecated legacy location for `seatbelt`; read only when the top-level `seatbelt` is absent.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub experimental: Option<SandboxConfigUserPolicyExperimental>,
    /// Filesystem rules to merge into the base policy.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub filesystem: Option<SandboxConfigUserPolicyFilesystem>,
    /// Network rules to merge into the base policy.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub network: Option<SandboxConfigUserPolicyNetwork>,
    /// macOS seatbelt options to merge into the base policy.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub seatbelt: Option<SandboxConfigUserPolicySeatbelt>,
}

/// Resolved sandbox configuration.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SandboxConfig {
    /// Whether to auto-add the current working directory to readwritePaths. Default: true.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub add_current_working_directory: Option<bool>,
    /// Whether the agent may request that an individual command run outside the sandbox, which the host then approves or denies through the usual permission flow. A host capability flag rather than part of the policy: it is stripped from the effective spawn policy and only has an effect while `enabled` is true. Fail-closed, unlike the opt-out flags on this object: omitting it offers no bypass. Default: false (opt-in).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub allow_bypass: Option<bool>,
    /// Whether to auto-grant read access to tool directories discovered on PATH and in toolchain environment variables (GOROOT, JAVA_HOME, VIRTUAL_ENV, and similar), and to common developer-tool caches, config, and toolchains. Writable grants cover scratch caches, the Unix GitHub CLI cache, and Cargo's registry, git store, and lock/tracker files. A relocated CARGO_HOME gets the same narrow split: registry and git are read-write; bin is read-only; the home root, config.toml, and credentials.toml stay ungranted. Set to false to disable every grant listed above; user-installed toolchains and caches then need explicit userPolicy.filesystem readonlyPaths and readwritePaths entries. The working directory (see addCurrentWorkingDirectory), temporary storage, session log paths, and system locations follow their own rules and stay granted. Default: true (enabled by default; set to false to opt out).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub allow_dev_tool_access: Option<bool>,
    /// Credential-injection capability flags.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auth: Option<SandboxConfigAuth>,
    /// Opt-in whole-value environment masking for sandboxed shell, MCP, and LSP children. Configured names get random sentinels; the local proxy substitutes them only in HTTPS request headers at their injection hosts. Approved bypasses skip masking and the sandbox proxy, so bypassed shells may receive the real environment values. Disabled or explicitly opted-out routes are not protected. No credential values are stored in this configuration.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub credentials: Option<SandboxCredentialsConfig>,
    /// Whether sandboxing is enabled for the session.
    pub enabled: bool,
    /// The `sandboxExtensions` counterpart of `managedMcpRoutingLocked`.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) managed_extensions_routing_locked: Option<bool>,
    /// The `sandboxLspServers` counterpart of `managedMcpRoutingLocked`.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) managed_lsp_routing_locked: Option<bool>,
    /// Set by the runtime when a managed policy forced `sandboxMcpServers` on and took the local opt-out away. Provenance rather than policy: it lets a sandbox startup failure point at the administrator instead of a setting the next managed merge would override, and it is ignored when comparing two configs for change. Only the managed merge may set it; a caller-supplied value is stripped.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) managed_mcp_routing_locked: Option<bool>,
    /// Whether extension processes the session launches are confined by the sandbox. Only an explicit `false` opts out. Ignored while `enabled` is false. Default: true (enabled by default; set to false to opt out).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sandbox_extensions: Option<bool>,
    /// Whether language servers the session launches are confined by the sandbox. Only an explicit `false` opts out. Ignored while `enabled` is false. Default: true (enabled by default; set to false to opt out).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sandbox_lsp_servers: Option<bool>,
    /// Whether MCP servers the session launches are confined by the sandbox. Only an explicit `false` opts out; doing so also lets remote-MCP egress leave the sandbox, so the flag and `enabled` are always read together. Ignored while `enabled` is false. Default: true (enabled by default; set to false to opt out).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sandbox_mcp_servers: Option<bool>,
    /// User-managed sandbox policy fragment merged into the auto-discovered base policy.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub user_policy: Option<SandboxConfigUserPolicy>,
}

/// A possible secret-bearing environment variable and optional locally suggested HTTPS injection hosts. This is a draft for user review, not an active grant.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SandboxCredentialSuggestion {
    /// Environment variable name. The secret value is never returned.
    pub name: String,
    /// HTTPS hostnames suggested by a local known-provider mapping. An empty list requires the user to supply injection hosts before adding a masking entry. These suggestions do not allow network access.
    pub suggested_inject_hosts: Vec<String>,
}

/// Scheduled prompt entry with ID, timing (`intervalMs`, `cron`, or `at`), prompt text, recurrence, and next run time.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ScheduleEntry {
    /// Absolute fire time (epoch milliseconds) for a one-shot calendar schedule.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub at: Option<i64>,
    /// 5-field cron expression for a recurring calendar schedule, evaluated in `tz`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cron: Option<String>,
    /// Display-only label for the prompt as shown in the UI (e.g. `/skill-name` for a skill-invocation schedule). The actual enqueued prompt is `prompt`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub display_prompt: Option<String>,
    /// Sequential id assigned by the runtime within the session. Stable across resumes (rebuilt from the event log).
    pub id: i64,
    /// Interval between scheduled ticks, in milliseconds (relative-interval schedules).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub interval_ms: Option<i64>,
    /// ISO 8601 timestamp when the next tick is scheduled to fire.
    pub next_run_at: String,
    /// Prompt text that gets enqueued on every tick.
    pub prompt: String,
    /// Whether the schedule re-arms after each tick (`/every`) or fires once (`/after`).
    pub recurring: bool,
    /// True for a self-paced (`dynamic`) schedule: no fixed cadence; the model arms each next run via the `manage_schedule` `wakeup` action. `nextRunAt` is model-controlled.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub self_paced: Option<bool>,
    /// IANA timezone the `cron` expression is evaluated in.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tz: Option<String>,
}

/// Server-side skill metadata, including name, description, source, enabled/invocable state, path, project path, and argument hint.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ServerSkill {
    /// Optional freeform hint describing the skill's expected arguments, from the `argument-hint` frontmatter field
    #[serde(skip_serializing_if = "Option::is_none")]
    pub argument_hint: Option<String>,
    /// Canonical slash command name used to invoke the skill, without the leading '/'
    #[serde(skip_serializing_if = "Option::is_none")]
    pub command_name: Option<String>,
    /// Description of what the skill does
    pub description: String,
    /// Whether the skill is currently enabled (based on global config)
    pub enabled: bool,
    /// Unique identifier for the skill
    pub name: String,
    /// Absolute path to the skill file
    #[serde(skip_serializing_if = "Option::is_none")]
    pub path: Option<String>,
    /// The project path this skill belongs to (only for project/inherited skills)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub project_path: Option<String>,
    /// Source location type (e.g., project, personal-copilot, plugin, builtin)
    pub source: SkillSource,
    /// Whether the skill can be invoked by the user as a slash command
    pub user_invocable: bool,
}

/// Command-scoped GitHub credential injection for the shell commands an agent runs.
///
/// Each channel is opt-in and independent, and injection is scoped to the individual command
/// spawn: the credential is resolved from the session's *current* authentication at every spawn
/// and reaches only spawns whose script actually invokes `git` or `gh`. Because nothing is
/// retained between spawns, replacing the session credential (`session.gitHubAuth.setCredentials`)
/// changes what the next spawned command presents — which seeding a credential into the runtime
/// process's own environment cannot do, since a child's environment is fixed at `exec`.
///
/// The credential is matched to the host it authenticates to, so a github.com credential is never
/// presented to a GitHub Enterprise host and vice versa. Where a channel cannot express that
/// boundary it injects nothing rather than crossing it -- see `gh` below.
///
/// This is independent of `sandboxConfig`: it is a decision about which identity the agent
/// presents, not about what the agent may touch, and it works on every platform whether or not
/// an OS sandboxing backend is available. `sandboxConfig.auth` remains the sandbox-scoped
/// spelling and is additive with this one.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ShellCredentials {
    /// Whether to authenticate the agent's `gh` commands as the session's GitHub credential, by
    /// exporting `GH_TOKEN` to a spawn that runs `gh`. Any inherited `gh` credential is removed from
    /// spawns that do not, so the credential stays command-scoped.
    ///
    /// Applies to a github.com credential only. `gh` picks its credential variable from the host a
    /// command targets rather than the one the credential belongs to, and the command can choose that
    /// target, so `GH_ENTERPRISE_TOKEN` would offer a single-tenant enterprise credential to every
    /// other enterprise host. A session whose credential is enterprise-scoped therefore runs `gh`
    /// unauthenticated; its `git` commands are unaffected, because `http.<host>.extraheader` is scoped
    /// to one host by construction. Default: false (opt-in).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub gh: Option<bool>,
    /// Whether to authenticate the agent's `git` commands as the session's GitHub credential, by
    /// injecting an `http.<host>.extraheader` (plus `insteadOf` rewrites so SSH-spelled remotes for
    /// that host use the authenticated HTTPS transport). Applied only to a spawn that runs a
    /// remote-contacting `git` subcommand. Default: false (opt-in).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub git: Option<bool>,
}

/// A host-provided script sourced before each built-in shell command when its shell target matches the active shell.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ShellInitScript {
    /// Path to the script to source.
    pub path: String,
    /// Built-in shell that may source this script.
    pub shell: ShellInitScriptShell,
}

/// Per-session settings for built-in shell tools.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ShellOptions {
    /// Command-scoped GitHub credential injection for shell commands.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub credentials: Option<ShellCredentials>,
    /// Controls automatic non-interactive profile loading where supported. Explicit initScripts are unaffected.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub init_profile: Option<ShellInitProfile>,
    /// Ordered host-provided script paths sourced before each built-in shell command when the
    /// entry's shell target matches the active shell. Use these for rc files, environment setup scripts,
    /// or other custom scripts. A script that returns a nonzero status is reported, and later scripts
    /// and the user command continue while the shell remains running. Because scripts are sourced into
    /// the command shell, `exit`, `exec`, failures under `set -e`, or other shell-terminating behavior
    /// can prevent continuation. Script standard output is preserved; Bash script stderr is discarded,
    /// PowerShell exception messages are replaced, and runtime-generated failure notices omit
    /// configured script paths. When sandboxing is enabled, each script must already be readable under
    /// the active sandbox filesystem policy. Pass an empty array to clear the list.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub init_scripts: Option<Vec<ShellInitScript>>,
    /// Flags passed to the active built-in shell process on startup, replacing its default flags.
    /// When omitted, the built-in Bash shell uses `--norc --noprofile`,
    /// and the built-in PowerShell shell uses `-NoProfile -NoLogo`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub process_flags: Option<Vec<String>>,
}

/// Skill metadata available to a session, with name, description, source, enabled/invocable state, path, plugin, and argument hint.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Skill {
    /// Optional freeform hint describing the skill's expected arguments, from the `argument-hint` frontmatter field
    #[serde(skip_serializing_if = "Option::is_none")]
    pub argument_hint: Option<String>,
    /// Canonical slash command name used to invoke the skill, without the leading '/'
    #[serde(skip_serializing_if = "Option::is_none")]
    pub command_name: Option<String>,
    /// Original frontmatter name for an MCP skill, for display only. Use name for identity and enable/disable operations.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub declared_name: Option<String>,
    /// Description of what the skill does
    pub description: String,
    /// Whether the skill is currently enabled
    pub enabled: bool,
    /// MCP server and skill resource address when source is 'mcp'. Contains no activation grants or internal verification metadata.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mcp_origin: Option<McpSkillOrigin>,
    /// Unique identifier for the skill
    pub name: String,
    /// Absolute path to the skill file
    #[serde(skip_serializing_if = "Option::is_none")]
    pub path: Option<String>,
    /// Name of the plugin that provides the skill, when source is 'plugin'
    #[serde(skip_serializing_if = "Option::is_none")]
    pub plugin_name: Option<String>,
    /// Source location type (e.g., project, personal-copilot, plugin, builtin)
    pub source: SkillSource,
    /// Whether the skill can be invoked by the user as a slash command
    pub user_invocable: bool,
}

/// Skill invocation record with name, path, content, allowed tools, and turn number.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillsInvokedSkill {
    /// Tools that should be auto-approved when this skill is active, captured at invocation time
    #[serde(skip_serializing_if = "Option::is_none")]
    pub allowed_tools: Option<Vec<String>>,
    /// Exact invocation arguments when recorded, retained separately from the reusable skill body
    #[serde(skip_serializing_if = "Option::is_none")]
    pub arguments: Option<String>,
    /// Full content of the skill file
    pub content: String,
    /// Whether model invocation was disabled when this skill was invoked
    #[serde(skip_serializing_if = "Option::is_none")]
    pub disable_model_invocation: Option<bool>,
    /// Turn number when the skill was invoked
    pub invoked_at_turn: i64,
    /// Unique identifier for the skill
    pub name: String,
    /// Path to the SKILL.md file, or an empty string for an SDK-provided skill without a filesystem identity
    pub path: String,
}

/// Subagent model, reasoning effort, context tier, and auto-invocation settings
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SubagentSettingsEntry {
    /// Whether this agent's runtime-defined proactive invocation prompting is enabled, if supported. Currently consumed by the built-in rubber-duck agent.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auto_invoke: Option<bool>,
    /// Context tier override for matching subagents
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context_tier: Option<SubagentSettingsEntryContextTier>,
    /// Reasoning effort override for matching subagents
    #[serde(skip_serializing_if = "Option::is_none")]
    pub effort_level: Option<String>,
    /// Model override for matching subagents
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model: Option<String>,
    /// Whether the configured model strategy is preferred or required
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_policy: Option<AgentModelPolicy>,
    /// Provider identifier for the configured model override
    #[serde(skip_serializing_if = "Option::is_none")]
    pub provider_id: Option<String>,
}

/// Configured per-agent subagent overrides
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SubagentSettingsConfig {
    /// Per-agent settings keyed by subagent agent_type
    #[serde(skip_serializing_if = "Option::is_none")]
    pub agents: Option<HashMap<String, SubagentSettingsEntry>>,
    /// Configured opt-in for subagent context management tools; applied from persisted settings when the session starts
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context_management_tools: Option<bool>,
    /// Names of subagents the user has turned off; they cannot be dispatched
    #[serde(skip_serializing_if = "Option::is_none")]
    pub disabled_subagents: Option<Vec<String>>,
    /// Maximum number of subagents that can run concurrently; applies to usage-based billing users only
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_concurrency: Option<i32>,
    /// Maximum subagent nesting depth; applies to usage-based billing users only
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_depth: Option<i32>,
}

/// Requested and resolved context configuration observed when a direct workflow agent launched.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkflowAgentContextObservation {
    /// Full context-window capacity resolved for the initialized child session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_context_window_tokens: Option<i64>,
    /// Effective prompt-token input budget resolved for the initialized child session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub prompt_token_limit: Option<i64>,
    /// Context-tier override explicitly supplied by the workflow call.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub requested_tier: Option<ContextTier>,
    /// Effective context-tier identifier reported by the initialized child session. Native sessions currently report `default` or `long_context`; remote participants may report another supported identifier.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub resolved_tier: Option<String>,
}

/// Requested and resolved model observations for a direct workflow agent.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkflowAgentModelObservation {
    /// Non-empty model override explicitly supplied by the workflow call.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub requested: Option<String>,
    /// Concrete model reported by the initialized child session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub resolved: Option<String>,
}

/// Requested and resolved reasoning-effort observations for a direct workflow agent.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkflowAgentReasoningEffortObservation {
    /// Non-empty reasoning-effort override explicitly supplied by the workflow call.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub requested: Option<String>,
    /// Effective reasoning effort reported by the initialized child session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub resolved: Option<String>,
}

/// Prompt-safe durable identity and live status for a direct workflow agent.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkflowAgentSummary {
    /// Accumulated active agent time in milliseconds.
    pub active_ms: i64,
    /// Prompt-safe live activity text.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub activity: Option<String>,
    /// Stable direct-agent identifier.
    pub agent_id: String,
    /// Registered agent type.
    pub agent_type: String,
    /// Epoch milliseconds when the agent completed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub completed_at: Option<i64>,
    /// Known AI-credit spend, in nano-AIU, produced by this direct agent and its descendants. Omitted for historical agents when unattributed charges prevent a known zero.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub consumed_nano_aiu: Option<i64>,
    /// Requested and resolved context observations, when available.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context: Option<WorkflowAgentContextObservation>,
    /// Friendly, non-unique name intended for display
    #[serde(skip_serializing_if = "Option::is_none")]
    pub display_name: Option<String>,
    /// Friendly, non-unique name intended for display
    pub label: String,
    /// Requested and resolved model observations, when available.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model: Option<WorkflowAgentModelObservation>,
    /// Phase identifier active when the agent was launched, or null.
    pub phase_id: Option<String>,
    /// Requested and resolved reasoning-effort observations, when available.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_effort: Option<WorkflowAgentReasoningEffortObservation>,
    /// Deprecated compatibility alias for model.requested.
    #[doc(hidden)]
    #[deprecated]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub requested_model: Option<String>,
    /// Deprecated compatibility alias for model.resolved.
    #[doc(hidden)]
    #[deprecated]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub resolved_model: Option<String>,
    /// Owning workflow run identifier.
    pub run_id: String,
    /// Epoch milliseconds when the agent started.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub started_at: Option<i64>,
    /// Current durable or live agent status.
    pub status: String,
    /// Tool-call identifier that launched the agent.
    pub tool_call_id: String,
}

/// Current workflow phase identity.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkflowCurrentPhase {
    /// Current phase identifier.
    pub id: String,
    /// Zero-based declared phase ordinal, or null for an undeclared phase.
    pub ordinal: Option<i64>,
}

/// Declared or approved workflow resource ceilings.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkflowDeclaredLimits {
    /// Maximum AI credits consumed by subagents and descendants.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_ai_credits: Option<f64>,
    /// Maximum concurrently active subagents.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_concurrent_subagents: Option<i64>,
    /// Maximum total subagents spawned by the run.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_total_subagents: Option<i64>,
    /// Maximum accumulated active execution time in seconds.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub timeout_seconds: Option<f64>,
}

/// Durable workflow resource consumption.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkflowRunConsumed {
    /// Accumulated active execution time in milliseconds.
    pub active_ms: i64,
    /// AI usage consumed by the run in nano-AIU.
    pub nano_aiu: i64,
    /// Total subagents spawned by the run.
    pub subagents: i64,
}

/// Prompt-safe terminal workflow outcome.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkflowRunTerminal {
    /// Human-readable terminal error.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Machine-readable terminal failure.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub failure: Option<serde_json::Value>,
    /// Pause initiator metadata, or null when the run did not pause.
    pub pause_info: Option<serde_json::Value>,
    /// Human-readable terminal reason.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reason: Option<String>,
    /// Prompt-safe preview of the completed result.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub result_preview: Option<String>,
}

/// Durable workflow run summary with read-time live overlays.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkflowRunSummary {
    /// Epoch milliseconds when the current active segment started, or null while inactive.
    pub active_segment_started_at: Option<i64>,
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

/// Durable lifecycle and timing for one workflow phase.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkflowPhaseObservation {
    /// Completed active time accumulated by this phase in milliseconds.
    pub accumulated_active_ms: i64,
    /// Epoch milliseconds when this phase completed; for a skipped phase, the synthetic skip timestamp (equal to `startedAt`).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub completed_at: Option<i64>,
    /// Current live active time for this phase in milliseconds.
    pub current_active_ms: i64,
    /// Optional human-readable phase detail.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub detail: Option<String>,
    /// Number of times execution entered this phase.
    pub entry_count: i64,
    /// Phase identifier.
    pub id: String,
    /// Most recent run attempt that entered this phase, or `0` if the phase has never been entered.
    pub last_entered_run_attempt: i64,
    /// Direct agents in this phase that are currently live.
    pub live_agent_count: i64,
    /// Zero-based declared phase ordinal, or null for an undeclared phase.
    pub ordinal: Option<i64>,
    /// Epoch milliseconds when this phase first started; for a skipped phase, the synthetic skip timestamp (equal to `completedAt`).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub started_at: Option<i64>,
    /// Derived lifecycle state of the phase.
    pub status: WorkflowPhaseStatus,
    /// Human-readable phase title.
    pub title: String,
    /// Total direct agents associated with this phase.
    pub total_agent_count: i64,
}

/// One durable workflow progress record.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkflowProgressLine {
    /// Resume attempt that emitted this record.
    pub attempt: i64,
    /// Progress record kind.
    pub kind: WorkflowLogLineKind,
    /// Phase active when the record was emitted, or null before any phase.
    pub phase_id: Option<String>,
    /// Epoch milliseconds when the record was persisted.
    pub recorded_at: i64,
    /// Global monotonic sequence number within the run.
    pub seq: i64,
    /// Prompt-safe progress text.
    pub text: String,
}

/// A bidirectional page of workflow progress.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkflowProgressPage {
    /// Whether progress records newer than this page exist.
    pub has_more_newer: bool,
    /// Whether progress records older than this page exist.
    pub has_more_older: bool,
    /// Newest sequence number in this page, or null when empty.
    pub newest_seq: Option<i64>,
    /// Oldest sequence number in this page, or null when empty.
    pub oldest_seq: Option<i64>,
    /// Progress records in sequence order.
    pub records: Vec<WorkflowProgressLine>,
    /// Run revision reflected by this page.
    pub revision: i64,
}

/// Complete current or terminal workflow run envelope.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkflowRunResult {
    /// One-based execution attempt represented by this envelope. Absent before the first attempt starts or when returned by an older runtime.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub attempt: Option<i64>,
    /// Error message for an errored run.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Machine-readable failure details for a halted or errored run.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub failure: Option<serde_json::Value>,
    /// Structured pause initiator metadata for a paused attempt.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub pause_info: Option<serde_json::Value>,
    /// Reason for a halted or cancelled run.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reason: Option<String>,
    /// Completed workflow result.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub result: Option<serde_json::Value>,
    /// Workflow run identifier.
    pub run_id: String,
    /// Partial journal and progress snapshot for a halted, cancelled, or errored run.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub snapshot: Option<serde_json::Value>,
    /// Current or terminal workflow run status.
    pub status: WorkflowRunStatus,
}

/// Complete text used to generate one session diff. These are display contents, with the same text decoding as the patch, not a file-restore contract.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspaceDiffContents {
    /// Complete current text read when computing the diff. Omitted for a deleted file; an empty string represents an existing empty file.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub after: Option<String>,
    /// Complete text before the session first changed the file. Omitted when the file did not exist; an empty string represents an existing empty file.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub before: Option<String>,
}

/// A single changed file and its unified diff.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspaceDiffFileChange {
    /// Type of change represented by this file diff.
    pub change_type: WorkspaceDiffFileChangeType,
    /// Full text used for this patch, only when includeContents was requested for session mode. Omitted for binary, oversized or unavailable contents, and for fallback results. Read isFallback and isTruncated before treating an absent value as a missing file.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub contents: Option<WorkspaceDiffContents>,
    /// Unified diff content for the file. Empty when the diff was truncated.
    pub diff: String,
    /// Whether the diff content was omitted because it exceeded the per-file size limit.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub is_truncated: Option<bool>,
    /// Original file path for renamed files.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub old_path: Option<String>,
    /// Path to the changed file, relative to the workspace root when the file lives under it. A file changed outside the workspace root keeps a `../`-relative path, or an absolute path when no relative path exists (for example a different Windows drive).
    pub path: String,
}

/// Workspace checkpoint metadata with assigned number, human-readable title, and checkpoint filename.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesCheckpoints {
    /// Filename of the checkpoint within the workspace checkpoints directory
    pub filename: String,
    /// Checkpoint number assigned by the workspace manager
    pub number: i64,
    /// Human-readable checkpoint title
    pub title: String,
}

/// MCP CreateMessageResult payload (with optional 'tools' extension), present when action='success'. Treated as opaque at the schema layer; consumers should construct/consume it per the MCP CreateMessageResult shape.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type McpExecuteSamplingResult = HashMap<String, serde_json::Value>;

/// Authentication host. HMAC auth always targets the public GitHub host.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum HMACAuthInfoHost {
    #[serde(rename = "https://github.com")]
    #[default]
    HttpsGitHubCom,
}

/// HMAC-based authentication used by GitHub-internal services.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum HMACAuthInfoType {
    #[serde(rename = "hmac")]
    #[default]
    Hmac,
}

/// Personal access token (PAT) or server-to-server token sourced from an environment variable.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum EnvAuthInfoType {
    #[serde(rename = "env")]
    #[default]
    Env,
}

/// SDK-side token authentication; the host configured the token directly via the SDK.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum TokenAuthInfoType {
    #[serde(rename = "token")]
    #[default]
    Token,
}

/// SDK callback-backed GitHub token authentication.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum TokenProviderAuthInfoType {
    #[serde(rename = "token-provider")]
    #[default]
    TokenProvider,
}

/// Authentication host (always the public GitHub host).
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CopilotApiTokenAuthInfoHost {
    #[serde(rename = "https://github.com")]
    #[default]
    HttpsGitHubCom,
}

/// Direct Copilot API authentication via the `GITHUB_COPILOT_API_TOKEN` + `COPILOT_API_URL` environment-variable pair. The token itself is read from the environment by the runtime, not carried in this struct.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CopilotApiTokenAuthInfoType {
    #[serde(rename = "copilot-api-token")]
    #[default]
    CopilotApiToken,
}

/// OAuth user authentication. The token itself is held in the runtime's secret token store (keyed by host+login) and is NOT carried in this struct.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum UserAuthInfoType {
    #[serde(rename = "user")]
    #[default]
    User,
}

/// Provider-owned account authentication.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AccountAuthInfoType {
    #[serde(rename = "account")]
    #[default]
    Account,
}

/// Authentication via the `gh` CLI's saved credentials.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum GhCliAuthInfoType {
    #[serde(rename = "gh-cli")]
    #[default]
    GhCli,
}

/// API-key authentication for non-GitHub LLM providers (e.g. when running BYOM-style).
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ApiKeyAuthInfoType {
    #[serde(rename = "api-key")]
    #[default]
    ApiKey,
}

/// Authentication credentials accepted only at native protocol ingress. Runtime outputs use credential-free `AuthIdentity` metadata.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum AuthInfo {
    Hmac(HMACAuthInfo),
    Env(EnvAuthInfo),
    Token(TokenAuthInfo),
    TokenProvider(TokenProviderAuthInfo),
    CopilotApiToken(CopilotApiTokenAuthInfo),
    User(UserAuthInfo),
    Account(AccountAuthInfo),
    GhCli(GhCliAuthInfo),
    ApiKey(ApiKeyAuthInfo),
}

/// Authentication type
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AuthInfoType {
    /// Authentication provided by a GitHub App HMAC credential.
    #[serde(rename = "hmac")]
    Hmac,
    /// Authentication resolved from environment-provided credentials.
    #[serde(rename = "env")]
    Env,
    /// Authentication from an interactive user sign-in.
    #[serde(rename = "user")]
    User,
    /// Authentication from a selected provider-owned account, without a GitHub credential.
    #[serde(rename = "account")]
    Account,
    /// Authentication delegated to the GitHub CLI.
    #[serde(rename = "gh-cli")]
    GhCli,
    /// Authentication from an API key credential.
    #[serde(rename = "api-key")]
    ApiKey,
    /// Authentication from a GitHub token.
    #[serde(rename = "token")]
    Token,
    /// Authentication from an SDK GitHub token callback.
    #[serde(rename = "token-provider")]
    TokenProvider,
    /// Authentication from a Copilot API token.
    #[serde(rename = "copilot-api-token")]
    CopilotApiToken,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// The provider kind stamped on a signed-in account.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AccountKind {
    /// An OAuth github.com account.
    #[serde(rename = "githubDotCom")]
    GitHubDotCom,
    /// A GitHub Enterprise Cloud account — a GitHub account on a non-github.com host, e.g. *.ghe.com.
    #[serde(rename = "proxima")]
    Proxima,
    /// A GitHub (EMU) account derived from a base Entra identity.
    #[serde(rename = "entraEmu")]
    EntraEmu,
    /// A base Microsoft Entra identity.
    #[serde(rename = "entra")]
    Entra,
    /// A Microsoft 365 Copilot (Loki) inference account derived from the same base Entra identity as an EMU account; its bearer is a Loki-scoped inference token consumed through the model-provider path, not the GitHub switcher.
    #[serde(rename = "loki")]
    Loki,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// A provider a consumer may interactively sign in with.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum LoginProviderKind {
    /// OAuth github.com sign-in via the browser (web loopback + PKCE).
    #[serde(rename = "githubDotCom")]
    GitHubDotCom,
    /// A GitHub Enterprise Cloud account — a GitHub account on a non-github.com host, e.g. *.ghe.com; the host is supplied interactively through the neutral input-required step.
    #[serde(rename = "proxima")]
    Proxima,
    /// Microsoft Entra sign-in that derives a GitHub (EMU) credential.
    #[serde(rename = "entra")]
    Entra,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Resolved Anthropic adaptive-thinking capability for a model.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AdaptiveThinkingSupport {
    /// The model does not accept thinking.type='adaptive'
    #[serde(rename = "unsupported")]
    Unsupported,
    /// The model accepts adaptive thinking but also accepts thinking.type='enabled'
    #[serde(rename = "optional")]
    Optional,
    /// The model defaults to adaptive thinking and rejects thinking.type='enabled' with HTTP 400, but still accepts thinking.type='disabled' (e.g. opus-4.7/4.8/5, sonnet-5)
    #[serde(rename = "required")]
    Required,
    /// The model accepts only thinking.type='adaptive'; 'enabled', 'disabled', and an omitted thinking block all fail with HTTP 400 (e.g. fable, mythos)
    #[serde(rename = "adaptive_only")]
    AdaptiveOnly,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Where the agent definition was loaded from
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AgentInfoSource {
    /// Agent loaded from the user's personal agent configuration.
    #[serde(rename = "user")]
    User,
    /// Agent loaded from the current project's repository configuration.
    #[serde(rename = "project")]
    Project,
    /// Agent inherited from a parent project or workspace.
    #[serde(rename = "inherited")]
    Inherited,
    /// Agent provided by a remote runtime or service.
    #[serde(rename = "remote")]
    Remote,
    /// Agent contributed by an installed plugin.
    #[serde(rename = "plugin")]
    Plugin,
    /// Agent built into the Copilot runtime.
    #[serde(rename = "builtin")]
    Builtin,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Login flow step variant discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AuthLoginStepOpenUrlKind {
    #[serde(rename = "open-url")]
    #[default]
    OpenUrl,
}

/// Login flow step variant discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AuthLoginStepInputRequiredKind {
    #[serde(rename = "input-required")]
    #[default]
    InputRequired,
}

/// Login flow step variant discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AuthLoginStepAwaitingKind {
    #[serde(rename = "awaiting")]
    #[default]
    Awaiting,
}

/// Login flow step variant discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AuthLoginStepNeedsInteractionKind {
    #[serde(rename = "needs-interaction")]
    #[default]
    NeedsInteraction,
}

/// Login flow step variant discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AuthLoginStepCompletedKind {
    #[serde(rename = "completed")]
    #[default]
    Completed,
}

/// Disposition of a login attempt, including pending user decisions.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AuthLoginResultStatus {
    /// The credential was persisted and the selected account is signed in.
    #[serde(rename = "completed")]
    Completed,
    /// Persistence needs explicit consent to store the token in plaintext.
    #[serde(rename = "needs-plaintext-consent")]
    NeedsPlaintextConsent,
    /// Credentials are saved; select an account using a returned selectionId as advance input to complete sign-in.
    #[serde(rename = "needs-account-selection")]
    NeedsAccountSelection,
    /// The user declined plaintext persistence.
    #[serde(rename = "declined")]
    Declined,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Login flow step variant discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AuthLoginStepErrorKind {
    #[serde(rename = "error")]
    #[default]
    Error,
}

/// One step in an interactive login flow. The consumer acts on the step and calls advance to proceed. Browser-open is encoded as two distinct steps by design: `open-url` is CONSUMER-driven (the provider surfaces the authorize URL and the consumer opens it — github.com/GHEC web), while `needs-interaction` is PROVIDER-driven (the provider opens the browser or broker UI itself and does not surface a URL — Entra).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum AuthLoginStep {
    OpenUrl(AuthLoginStepOpenUrl),
    InputRequired(AuthLoginStepInputRequired),
    Awaiting(AuthLoginStepAwaiting),
    NeedsInteraction(AuthLoginStepNeedsInteraction),
    Completed(AuthLoginStepCompleted),
    Error(AuthLoginStepError),
}

/// Outcome of applying the session-idle Autopilot continuation policy.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AutopilotEvaluateIdleContinuationOutcome {
    /// The policy admitted and scheduled another Autopilot turn.
    #[serde(rename = "continued")]
    Continued,
    /// No Autopilot continuation was scheduled after the policy was applied.
    #[serde(rename = "stopped")]
    Stopped,
    /// The session was not eligible for an Autopilot continuation.
    #[serde(rename = "notApplicable")]
    NotApplicable,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Current normalized autopilot objective lifecycle status.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AutopilotObjectiveStatus {
    /// The objective is actively running.
    #[serde(rename = "active")]
    Active,
    /// The objective is paused and may be resumed.
    #[serde(rename = "paused")]
    Paused,
    /// The objective completed.
    #[serde(rename = "completed")]
    Completed,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Discriminator: the caller is not authenticated
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogAuthenticationRequiredErrorKind {
    #[serde(rename = "authentication-required")]
    #[default]
    AuthenticationRequired,
}

/// Why the catalog authority did not accept the caller's identity
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogAuthenticationRequiredReason {
    /// No credential was presented, so there is nothing to refresh and the caller must sign in.
    #[serde(rename = "no-credential")]
    NoCredential,
    /// A credential was presented and its lifetime has elapsed. A silent refresh is worth attempting before prompting anyone.
    #[serde(rename = "credential-expired")]
    CredentialExpired,
    /// A credential was presented and the authority refused it, for example because it was revoked, malformed, or issued for another audience. Refreshing the same rejected credential is not useful; the caller must sign in again.
    #[serde(rename = "credential-rejected")]
    CredentialRejected,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// JSON MCP card media type accepted for install planning
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpServerCardMediaType {
    /// The current MCP server card media type.
    #[serde(rename = "application/mcp-server-card+json")]
    ApplicationMcpServerCardJson,
    /// The legacy MCP server card media type, accepted for compatibility.
    #[serde(rename = "application/mcp-server+json")]
    ApplicationMcpServerJson,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// A wire feature a caller can require of the catalog surface, negotiated per request. A grant means the runtime understands the feature's contract, not that the deployment has enabled the operation; typed unavailable results report availability separately.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogCapability {
    /// Understands the current `application/mcp-server-card+json` media type.
    #[serde(rename = "mcp-server-card")]
    McpServerCard,
    /// Understands the legacy `application/mcp-server+json` media type.
    #[serde(rename = "legacy-mcp-server-card")]
    LegacyMcpServerCard,
    /// Understands `application/ai-skill` candidates as discovery-only and typed non-installable.
    #[serde(rename = "ai-skill-discovery")]
    AiSkillDiscovery,
    /// Understands opt-in `application/vnd.github.copilot-plugin` candidates and their typed identity, version, source, and compatibility fields.
    #[serde(rename = "agent-plugin-discovery")]
    AgentPluginDiscovery,
    /// Extends agent-plugin-discovery to Claude plugin (`application/vnd.anthropic.claude-plugin+json`) and portable Agent Plugin (`application/agent-plugin+json`) candidates, each reporting its format as its media type. Requires agent-plugin-discovery; without it, plugin searches return only Copilot plugins.
    #[serde(rename = "agent-plugin-formats")]
    AgentPluginFormats,
    /// Understands side-effect-free MCP install-plan requests, results, and plan handles; `planning-unavailable` separately reports that planning is not enabled.
    #[serde(rename = "mcp-install-planning")]
    McpInstallPlanning,
    /// Understands plans that enumerate every eligible transport rather than a single preferred one.
    #[serde(rename = "multiple-transport-choice")]
    MultipleTransportChoice,
    /// Understands explicit numbered navigation and authority-reported pagination metadata with opaque tokens. Advertised and granted only when requested.
    #[serde(rename = "catalog-search-pagination")]
    CatalogSearchPagination,
    /// Understands versioned candidate trust snapshots. Protocol-3 callers must require this capability before the runtime adds the optional snapshot field.
    #[serde(rename = "trust-snapshot")]
    TrustSnapshot,
    /// Understands exact candidate selection through model-safe opaque references and host-only candidate-handle hand-off.
    #[serde(rename = "catalog-selection")]
    CatalogSelection,
    /// Requires an eligible credential for the selected GitHub.com account before search egress and prohibits client-side anonymous retry, including after HTTP 401 or 403. The credential is scoped to the fixed catalog authority without redirect forwarding. Neither a grant nor successful response proves that the authority accepted the identity or selected a particular backend. Preserve this requirement on every page and retry; callers omitting it retain optional authentication.
    #[serde(rename = "catalog-search-credential-required")]
    CatalogSearchCredentialRequired,
    /// Captures the exact existing native session, account, host and connection for authenticated catalogue search, selection and planning. Requires catalog-search-credential-required; does not grant installation or create a session.
    #[serde(rename = "catalog-search-session-bound")]
    CatalogSearchSessionBound,
    /// Understands effect-free preparation, exact human-confirmed apply and owned removal for fully resolved personal remote MCP choices without supplied inputs or configured secrets. Advertised only when the real producer and lower owned admission are linked; requires original connection and bound session authority for new work.
    #[serde(rename = "mcp-confirmed-remote-installation")]
    McpConfirmedRemoteInstallation,
    /// Extends mcp-confirmed-remote-installation to declared non-secret header and URL values and receipt-owned header secrets for personal remote MCP choices. Requires mcp-confirmed-remote-installation and bound session authority. Secret values are written only to the reviewed backend after confirmation and are never returned; package and stdio choices remain unsupported. Advertised only when owned secret effects and owned secret activation are linked.
    #[serde(rename = "mcp-configured-remote-installation")]
    McpConfiguredRemoteInstallation,
    /// Understands verified Agent Finder Skill install, uninstall, recovery and installation-scoped enablement APIs. Advertised only by runtimes with the Skill installation engine linked; acquisition may still be refused as feature-disabled per selected session.
    #[serde(rename = "skill-confirmed-installation")]
    SkillConfirmedInstallation,
    /// Understands `installability`, the reviewable `installPlan` and the optional materialisation `handle` on Agent Plugin candidates, and the verified Agent Plugin installation APIs plugins.planInstall and plugins.applyInstall, and Agent Plugin plans from catalog.planInstallFromReference. Requires agent-plugin-discovery; without it, plugin candidates are unchanged. Advertised only by runtimes with the plugin installer linked; acquisition may still be refused as feature-disabled per selected session.
    #[serde(rename = "agent-plugin-confirmed-installation")]
    AgentPluginConfirmedInstallation,
    /// Understands catalog.planInstallFromReference, which plans installing whatever resource an Agent Finder materialisation reference pins. It plans only kinds whose installation capability the caller also requires: skill-confirmed-installation for Skills, agent-plugin-confirmed-installation for Agent Plugins. Advertised only by runtimes with the Skill installation engine linked.
    #[serde(rename = "catalog-reference-installation")]
    CatalogReferenceInstallation,
    /// Understands catalog.getSavedFeed and catalog.getGeneratedFeed, which retrieve a saved or generated feed with entries projected exactly like catalog.search candidates. Requires catalog-search-credential-required; add catalog-search-session-bound to bind the retrieval to a selected session.
    #[serde(rename = "catalog-feeds")]
    CatalogFeeds,
    /// Understands the optional `categories` filter on catalog.search, which the runtime forwards to the catalog authority. Required to send the filter, so an older runtime refuses the request instead of silently returning unfiltered results.
    #[serde(rename = "catalog-search-categories")]
    CatalogSearchCategories,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Discriminator: the upstream response broke the contract
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogContractViolationErrorKind {
    #[serde(rename = "contract-violation")]
    #[default]
    ContractViolation,
}

/// Which wire-contract rule an upstream response broke
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogContractViolationReason {
    /// A result carried both a URL and embedded data, when exactly one is permitted.
    #[serde(rename = "both-url-and-data")]
    BothUrlAndData,
    /// A result carried neither a URL nor embedded data, when exactly one is required.
    #[serde(rename = "neither-url-nor-data")]
    NeitherUrlNorData,
    /// Two results claimed the same collision key: normalised identity for existing kinds, or normalised identity and declared version for Agent Plugins.
    #[serde(rename = "duplicate-identity")]
    DuplicateIdentity,
    /// A result declared no media type, or one this contract does not model.
    #[serde(rename = "unknown-media-type")]
    UnknownMediaType,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Which request field was rejected locally or by the catalog authority
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogInvalidRequestField {
    /// The selected existing attached session was missing, malformed or unavailable.
    #[serde(rename = "policySessionId")]
    PolicySessionId,
    /// The search query was empty or longer than permitted.
    #[serde(rename = "query")]
    Query,
    /// The requested result count fell outside its permitted range.
    #[serde(rename = "limit")]
    Limit,
    /// The requested candidate kinds were empty or contained a duplicate.
    #[serde(rename = "kinds")]
    Kinds,
    /// The negotiation block was missing or malformed.
    #[serde(rename = "contract")]
    Contract,
    /// The plan source was missing or malformed.
    #[serde(rename = "source")]
    Source,
    /// The supplied card was missing its media type, URL, or data.
    #[serde(rename = "card")]
    Card,
    /// The requested configuration scope is not one this runtime writes.
    #[serde(rename = "scope")]
    Scope,
    /// The pagination token or target was invalid, or the authority rejected the continuation. Repeat the search without page.
    #[serde(rename = "page")]
    Page,
    /// The locally owned session identifier was missing or malformed.
    #[serde(rename = "sessionId")]
    SessionId,
    /// The opaque selection reference was missing or malformed.
    #[serde(rename = "selectionRef")]
    SelectionRef,
    /// The terminal selection outcome was missing or unsupported.
    #[serde(rename = "outcome")]
    Outcome,
    /// The saved feed ID, or the generated feed strategy or key, was missing or malformed.
    #[serde(rename = "feed")]
    Feed,
    /// The trust tier filter was empty, repeated a tier or named an unknown tier.
    #[serde(rename = "trustTiers")]
    TrustTiers,
    /// The category filter was empty, too long, repeated a category or contained a value that is not a lowercase slug.
    #[serde(rename = "categories")]
    Categories,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Discriminator: the request itself was invalid
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogInvalidRequestErrorKind {
    #[serde(rename = "invalid-request")]
    #[default]
    InvalidRequest,
}

/// Discriminator: the card was malformed
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogMalformedCardErrorKind {
    #[serde(rename = "malformed-card")]
    #[default]
    MalformedCard,
}

/// Media type a catalog card is interpreted as
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogMediaType {
    /// The current MCP server card media type.
    #[serde(rename = "application/mcp-server-card+json")]
    ApplicationMcpServerCardJson,
    /// The legacy MCP server card media type, accepted for compatibility.
    #[serde(rename = "application/mcp-server+json")]
    ApplicationMcpServerJson,
    /// An AI skill card. Representable and searchable, but typed non-installable.
    #[serde(rename = "application/ai-skill")]
    ApplicationAiSkill,
    /// An inert Agent Plugin descriptor.
    #[serde(rename = "application/vnd.github.copilot-plugin")]
    ApplicationVndGitHubCopilotPlugin,
    /// An inert Claude plugin descriptor, surfaced only under the agent-plugin-formats capability.
    #[serde(rename = "application/vnd.anthropic.claude-plugin+json")]
    ApplicationVndAnthropicClaudePluginJson,
    /// An inert portable Agent Plugin descriptor, surfaced only under the agent-plugin-formats capability.
    #[serde(rename = "application/agent-plugin+json")]
    ApplicationAgentPluginJson,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// How a card failed validation
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogMalformedCardReason {
    /// The document is not well-formed JSON.
    #[serde(rename = "invalid-json")]
    InvalidJson,
    /// The document does not satisfy its media type's schema.
    #[serde(rename = "schema-violation")]
    SchemaViolation,
    /// The declared media type is not one this runtime understands.
    #[serde(rename = "unsupported-media-type")]
    UnsupportedMediaType,
    /// A field the media type requires is absent.
    #[serde(rename = "missing-required-field")]
    MissingRequiredField,
    /// The document exceeded the permitted size.
    #[serde(rename = "size-limit-exceeded")]
    SizeLimitExceeded,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Discriminator: capability or protocol-version negotiation failed
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogNegotiationRefusedErrorKind {
    #[serde(rename = "negotiation-refused")]
    #[default]
    NegotiationRefused,
}

/// Why capability and protocol-version negotiation refused a caller
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogNegotiationRefusedReason {
    /// The caller's protocol version is below the lowest this runtime serves.
    #[serde(rename = "unsupported-protocol-version")]
    UnsupportedProtocolVersion,
    /// The caller requires at least one capability this runtime cannot honour.
    #[serde(rename = "unsupported-capability")]
    UnsupportedCapability,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Discriminator: the network operation failed
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogNetworkFailureErrorKind {
    #[serde(rename = "network-failure")]
    #[default]
    NetworkFailure,
}

/// Categorised network failure, low cardinality so it can be aggregated without carrying a URL
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogNetworkFailureReason {
    /// No network is available, so nothing was attempted.
    #[serde(rename = "offline")]
    Offline,
    /// The authority's name could not be resolved.
    #[serde(rename = "dns")]
    Dns,
    /// The request exceeded its time budget.
    #[serde(rename = "timeout")]
    Timeout,
    /// The TLS handshake or certificate validation failed.
    #[serde(rename = "tls")]
    Tls,
    /// The connection was refused or reset.
    #[serde(rename = "connection-refused")]
    ConnectionRefused,
    /// The configured proxy returned 407 and requires authentication.
    #[serde(rename = "proxy-authentication-required")]
    ProxyAuthenticationRequired,
    /// The authority rate-limited requests and supplied or implied a bounded cooldown.
    #[serde(rename = "rate-limited")]
    RateLimited,
    /// The authority returned a transient 5xx response.
    #[serde(rename = "service-unavailable")]
    ServiceUnavailable,
    /// The authority returned another status the runtime treats as a failure.
    #[serde(rename = "http-status")]
    HttpStatus,
    /// The response exceeded the permitted size.
    #[serde(rename = "response-too-large")]
    ResponseTooLarge,
    /// A redirect was refused by the runtime's redirect policy.
    #[serde(rename = "redirect-rejected")]
    RedirectRejected,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Discriminator: policy refused the operation
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogPolicyRejectedErrorKind {
    #[serde(rename = "policy-rejected")]
    #[default]
    PolicyRejected,
}

/// Which authority produced a policy decision
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpPlanPolicySource {
    /// No policy applied, so the server is permitted by default.
    #[serde(rename = "none")]
    None,
    /// An enterprise allowlist evaluated the server.
    #[serde(rename = "enterprise-allowlist")]
    EnterpriseAllowlist,
    /// The registry the card came from evaluated the server.
    #[serde(rename = "registry-policy")]
    RegistryPolicy,
    /// Local trust settings evaluated the server.
    #[serde(rename = "local-trust")]
    LocalTrust,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Exact reviewed Skill installation action.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SkillInstallationReviewInstallAction {
    #[serde(rename = "install")]
    #[default]
    Install,
}

///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SkillInstallationScope {
    /// The user's personal Copilot home.
    #[serde(rename = "personal")]
    Personal,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Exact reviewed Skill installation action.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SkillInstallationReviewUninstallAction {
    #[serde(rename = "uninstall")]
    #[default]
    Uninstall,
}

/// Owned Skill state observed from files and receipts.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SkillInstallationOwnershipState {
    /// Owned files and receipt evidence match.
    #[serde(rename = "intact")]
    Intact,
    /// Owned files no longer match the receipt.
    #[serde(rename = "modified")]
    Modified,
    /// Ownership evidence requires recovery before mutation.
    #[serde(rename = "recovery-required")]
    RecoveryRequired,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Bound-session observation after reconciling persisted enablement.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SkillInstallationSessionState {
    /// The selected session has loaded this Skill and it is enabled.
    #[serde(rename = "loaded-enabled")]
    LoadedEnabled,
    /// The selected session has loaded this Skill or settings and it is disabled.
    #[serde(rename = "loaded-disabled")]
    LoadedDisabled,
    /// The selected session has not loaded Skills after the latest change.
    #[serde(rename = "not-loaded")]
    NotLoaded,
    /// The selected session could not be inspected.
    #[serde(rename = "unknown")]
    UnknownValue,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Safe verified Skill review fields. No raw credential, candidate handle or plan handle.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum SkillInstallationReview {
    Install(SkillInstallationReviewInstall),
    Uninstall(SkillInstallationReviewUninstall),
}

/// Exact reviewed Agent Plugin installation action.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PluginInstallPlanReviewAction {
    #[serde(rename = "install")]
    #[default]
    Install,
}

/// How a reviewed plugin's MCP server is reached.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PluginInstallationMcpTransport {
    /// An HTTP or SSE server reached over the network, which may ask the user to sign in.
    #[serde(rename = "remote")]
    Remote,
    /// A command the plugin runs on this machine.
    #[serde(rename = "local")]
    Local,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Discriminator: retrieval was refused as unsafe
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogUnsafeRetrievalErrorKind {
    #[serde(rename = "unsafe-retrieval")]
    #[default]
    UnsafeRetrieval,
}

/// Which hardened-fetch control refused a retrieval
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogUnsafeRetrievalReason {
    /// The URL used a scheme the runtime refuses to fetch.
    #[serde(rename = "blocked-scheme")]
    BlockedScheme,
    /// The URL embedded credentials.
    #[serde(rename = "credentials-in-url")]
    CredentialsInUrl,
    /// The URL resolved to a loopback, private, link-local, or cloud metadata address.
    #[serde(rename = "blocked-address")]
    BlockedAddress,
    /// A redirect target resolved to a blocked address.
    #[serde(rename = "redirect-to-blocked-address")]
    RedirectToBlockedAddress,
    /// The configured proxy policy refused the request.
    #[serde(rename = "proxy-rejected")]
    ProxyRejected,
    /// The authority is not permitted for card retrieval.
    #[serde(rename = "host-not-permitted")]
    HostNotPermitted,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Discriminator: the operation is not available
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogUnavailableErrorKind {
    #[serde(rename = "unavailable")]
    #[default]
    Unavailable,
}

/// Why a catalog operation is not available on this runtime
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogUnavailableReason {
    /// Bounded search is not wired up on this runtime build.
    #[serde(rename = "search-unavailable")]
    SearchUnavailable,
    /// Install planning is not wired up on this runtime build.
    #[serde(rename = "planning-unavailable")]
    PlanningUnavailable,
    /// Exact candidate selection is not available in this session or runtime.
    #[serde(rename = "selection-unavailable")]
    SelectionUnavailable,
    /// No catalog authority is configured for this runtime.
    #[serde(rename = "authority-not-configured")]
    AuthorityNotConfigured,
    /// The surface is disabled by policy on this runtime.
    #[serde(rename = "disabled-by-policy")]
    DisabledByPolicy,
    /// The requested saved feed, or generated feed strategy or key, does not exist.
    #[serde(rename = "feed-not-found")]
    FeedNotFound,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Relationship of the backend-reported count to the complete query result set.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CatalogSearchTotalCountRelation {
    /// No exact/full-query or lower-bound guarantee is available.
    #[serde(rename = "unknown")]
    UnknownValue,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Optional completion hint for the input (e.g. 'directory' for filesystem path completion)
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SlashCommandInputCompletion {
    /// Input should complete filesystem directories.
    #[serde(rename = "directory")]
    Directory,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Coarse command category for grouping and behavior: runtime built-in, skill-backed command, or SDK/client-owned command
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SlashCommandKind {
    /// Command implemented by the runtime.
    #[serde(rename = "builtin")]
    Builtin,
    /// Command backed by a skill.
    #[serde(rename = "skill")]
    Skill,
    /// Command registered by an SDK client or extension.
    #[serde(rename = "client")]
    Client,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Neutral SDK discriminator for the connected remote session kind.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ConnectedRemoteSessionMetadataKind {
    /// Remote CLI session.
    #[serde(rename = "remote-session")]
    RemoteSession,
    /// GitHub Copilot coding agent session.
    #[serde(rename = "coding-agent")]
    CodingAgent,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Stable OAuth scope whose absence prevents Connector management.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ConnectorAuthorizationScope {
    /// Allows Copilot to manage Connector connections and use their tools.
    #[serde(rename = "write_plugin_gateway_connections")]
    WritePluginGatewayConnections,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Availability of the EXPERIMENTAL session connector API.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ConnectorAvailability {
    /// The resolved Connector feature is enabled and Connector requests are permitted.
    #[serde(rename = "enabled")]
    Enabled,
    /// The resolved Connector feature is off. No Connector service request is made while disabled.
    #[serde(rename = "disabled")]
    Disabled,
    /// The session has no eligible host-owned GitHub account or does not support local Connector projection.
    #[serde(rename = "unavailable")]
    Unavailable,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Authoritative service connection state for one Connector.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ConnectorCatalogStatus {
    /// The Connector is available but not connected.
    #[serde(rename = "not_connected")]
    NotConnected,
    /// The Connector service is still completing connection or consent.
    #[serde(rename = "pending")]
    Pending,
    /// The Connector is connected and may contribute MCP servers.
    #[serde(rename = "connected")]
    Connected,
    /// The Connector service reports an unusable connection.
    #[serde(rename = "error")]
    Error,
    /// The service returned a future or unrecognized state.
    #[serde(rename = "unknown")]
    UnknownValue,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Live MCP status of one Connector-owned runtime server.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ConnectorMcpStatus {
    /// The server is connected and its tools are available.
    #[serde(rename = "connected")]
    Connected,
    /// The server connection is still being established.
    #[serde(rename = "pending")]
    Pending,
    /// The server requires refreshed GitHub authorization.
    #[serde(rename = "needs_auth")]
    NeedsAuth,
    /// The server failed to connect or initialize.
    #[serde(rename = "failed")]
    Failed,
    /// The server is intentionally stopped, including when managed policy blocks it.
    #[serde(rename = "stopped")]
    Stopped,
    /// The server is configured but explicitly disabled.
    #[serde(rename = "disabled")]
    Disabled,
    /// The Connector currently has no live server configuration.
    #[serde(rename = "not_configured")]
    NotConfigured,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Source category for a collected debug bundle entry.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum DebugCollectLogsSource {
    /// Session event log.
    #[serde(rename = "events")]
    Events,
    /// Process log for the session.
    #[serde(rename = "process-log")]
    ProcessLog,
    /// Interactive shell log for the session.
    #[serde(rename = "shell-log")]
    ShellLog,
    /// Caller-provided diagnostic entry.
    #[serde(rename = "additional")]
    Additional,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Destination kind that was written.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum DebugCollectLogsResultKind {
    /// A .tgz archive was written.
    #[serde(rename = "archive")]
    Archive,
    /// A directory containing the collected files was written.
    #[serde(rename = "directory")]
    Directory,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Whether the supplied diagnostic cursor remained within the retained buffer window.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum DiagnosticCursorStatus {
    /// The cursor is valid for the current retained window.
    #[serde(rename = "ok")]
    Ok,
    /// The cursor no longer addresses retained records; reading resumes at the oldest retained record.
    #[serde(rename = "expired")]
    Expired,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Direction of an observed MCP protocol frame.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpDiagnosticDirection {
    /// Frame emitted by the Copilot MCP client.
    #[serde(rename = "client-to-server")]
    ClientToServer,
    /// Frame received from the MCP server.
    #[serde(rename = "server-to-client")]
    ServerToClient,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Category for an MCP diagnostic record.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpDiagnosticKind {
    /// Connection lifecycle transition or failure.
    #[serde(rename = "lifecycle")]
    Lifecycle,
    /// JSON-RPC protocol frame.
    #[serde(rename = "protocol")]
    Protocol,
    /// HTTP request or response metadata.
    #[serde(rename = "http")]
    Http,
    /// Local MCP server standard-error output.
    #[serde(rename = "stderr")]
    Stderr,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Severity of an emitted diagnostic record.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum DiagnosticSeverity {
    /// Failure that prevented or interrupted communication.
    #[serde(rename = "error")]
    Error,
    /// A recoverable warning or server standard-error output.
    #[serde(rename = "warning")]
    Warning,
    /// Lifecycle transition.
    #[serde(rename = "info")]
    Info,
    /// Protocol-frame or launch diagnostic.
    #[serde(rename = "debug")]
    Debug,
    /// HTTP metadata diagnostic.
    #[serde(rename = "trace")]
    Trace,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Session-scoped diagnostic threshold. Capture is disabled by default and is never persisted with the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum DiagnosticLogLevel {
    /// Disable capture and clear retained diagnostics.
    #[serde(rename = "off")]
    Off,
    /// Capture failures only.
    #[serde(rename = "error")]
    Error,
    /// Capture failures, warnings, and stderr.
    #[serde(rename = "warning")]
    Warning,
    /// Capture lifecycle diagnostics.
    #[serde(rename = "info")]
    Info,
    /// Capture protocol frames and launch diagnostics.
    #[serde(rename = "debug")]
    Debug,
    /// Capture HTTP metadata in addition to debug diagnostics.
    #[serde(rename = "trace")]
    Trace,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Persisted extension discovery source
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum DiscoveredExtensionSource {
    /// Extension discovered from the user's extensions directory.
    #[serde(rename = "user")]
    User,
    /// Extension contributed by an installed plugin.
    #[serde(rename = "plugin")]
    Plugin,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Effective extension loading and agent-management mode
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum DiscoveredExtensionMode {
    /// Extensions are not loaded.
    #[serde(rename = "disabled")]
    Disabled,
    /// Extensions are loaded, but the agent cannot create, reload, or manage them.
    #[serde(rename = "load_only")]
    LoadOnly,
    /// Extensions are loaded and the agent can create, reload, and manage them.
    #[serde(rename = "load_and_augment")]
    LoadAndAugment,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Kind of component that supplied a provider adapter or row. Attribution does not confer authority.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ModelProviderProvenanceSource {
    /// Built into the runtime.
    #[serde(rename = "builtIn")]
    BuiltIn,
    /// Derived from existing user configuration.
    #[serde(rename = "configured")]
    Configured,
    /// Supplied by an extension.
    #[serde(rename = "extension")]
    Extension,
    /// Supplied by another trusted contributor.
    #[serde(rename = "custom")]
    Custom,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Typed outcome for a provider operation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ModelProviderOperationOutcomeCode {
    /// The operation completed successfully; an empty inventory is valid.
    #[serde(rename = "success")]
    Success,
    /// The provider or instance is absent during discovery, status, or model listing. Distinct from a successful empty inventory.
    #[serde(rename = "absent")]
    Absent,
    /// The provider is configured or expected but could not be reached.
    #[serde(rename = "unreachable")]
    Unreachable,
    /// The operation failed for a reason other than absence or reachability.
    #[serde(rename = "failed")]
    Failed,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Direction to page through the session's persisted event history. 'forward' pages from the cursor toward newer events; 'backward' returns the newest window first (tail-first) and pages toward older events. Events within a returned batch are always chronological (oldest-to-newest), even for a backward read.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum EventsReadDirection {
    /// Page from the cursor toward newer events (default).
    #[serde(rename = "forward")]
    Forward,
    /// Tail-first: return the newest events and page toward older events.
    #[serde(rename = "backward")]
    Backward,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Cursor status: 'ok' means the read succeeded against the requested history; 'expired' means the requested continuation is unavailable. Recovery is endpoint-specific: session.eventLog.read returns a boundary window of remaining active history that may overlap prior pages, while sessions.readPersistedEvents returns an empty terminal page and never switches journal generations. An expired persisted read is not successful completion; a complete persisted snapshot requires cursorStatus 'ok' and hasMore false.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum EventsCursorStatus {
    /// The read succeeded against the requested history.
    #[serde(rename = "ok")]
    Ok,
    /// The requested continuation is unavailable; see the endpoint's recovery semantics.
    #[serde(rename = "expired")]
    Expired,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Discovery source: project (.github/extensions/), user (~/.copilot/extensions/), plugin (installed plugin), or session (session-state/<id>/extensions/)
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ExtensionSource {
    /// Extension discovered from the current project's .github/extensions directory.
    #[serde(rename = "project")]
    Project,
    /// Extension discovered from the user's ~/.copilot/extensions directory.
    #[serde(rename = "user")]
    User,
    /// Extension contributed by an installed plugin.
    #[serde(rename = "plugin")]
    Plugin,
    /// Extension discovered from the current session's state directory (loaded only for this session).
    #[serde(rename = "session")]
    Session,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Current status: running, disabled, failed, or starting
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ExtensionStatus {
    /// The extension process is running.
    #[serde(rename = "running")]
    Running,
    /// The extension is installed but disabled.
    #[serde(rename = "disabled")]
    Disabled,
    /// The extension failed to start or crashed.
    #[serde(rename = "failed")]
    Failed,
    /// The extension process is starting.
    #[serde(rename = "starting")]
    Starting,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Runtime-derived standalone extension eligibility.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum StandaloneExtensionAdmission {
    /// Policy is resolved and permits standalone extensions.
    #[serde(rename = "allowed")]
    Allowed,
    /// Standalone extensions are prohibited; eligible plugin extensions remain governed by existing plugin controls.
    #[serde(rename = "plugin_only")]
    PluginOnly,
    /// Managed policy is unresolved; standalone extensions must not execute.
    #[serde(rename = "pending")]
    Pending,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Binary result type discriminator. Use "image" for images and "resource" for other binary data.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ExternalToolTextResultForLlmBinaryResultsForLlmType {
    /// Binary image data.
    #[serde(rename = "image")]
    Image,
    /// Other binary resource data.
    #[serde(rename = "resource")]
    Resource,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Reason a captured file was not restored.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum HistoryFileRestoreSkipReason {
    /// The file changed after Copilot's last captured write.
    #[serde(rename = "user-modified")]
    UserModified,
    /// A faithful preimage was not captured.
    #[serde(rename = "skipped-capture")]
    SkippedCapture,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Reason a rewind read (rewind points, file-restore preview, or session diff) could not be answered from the session's file-change captures.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum HistoryRewindUnavailableReason {
    /// The session did not opt into file-change tracking before its first turn.
    #[serde(rename = "file-change-tracking-disabled")]
    FileChangeTrackingDisabled,
    /// The session still has work that may mutate files or history. Transient: the same request succeeds once the session settles, so callers should retry rather than treat it as a failure.
    #[serde(rename = "session-busy")]
    SessionBusy,
    /// Remote-backed rewind routing is not supported.
    #[serde(rename = "unsupported-remote-session")]
    UnsupportedRemoteSession,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Aggregate file change represented by a rewind preview.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum HistoryRewindChangeType {
    /// The discarded turns created the file.
    #[serde(rename = "created")]
    Created,
    /// The discarded turns deleted the file.
    #[serde(rename = "deleted")]
    Deleted,
    /// The discarded turns modified the file.
    #[serde(rename = "modified")]
    Modified,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Outcome of a rewind request.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum HistoryRewindOutcome {
    /// The requested rewind completed; reachable in either mode.
    #[serde(rename = "success")]
    Success,
    /// The session still has work that may mutate files or history; reachable in either mode.
    #[serde(rename = "session-busy")]
    SessionBusy,
    /// A conversation-and-files rewind was requested for a session that did not enable capture; conversation-only rewinds never produce this.
    #[serde(rename = "file-change-tracking-disabled")]
    FileChangeTrackingDisabled,
    /// Remote-backed rewind routing is not supported; reachable in either mode.
    #[serde(rename = "unsupported-remote-session")]
    UnsupportedRemoteSession,
    /// File restore failed and all applied file changes were rolled back; only conversation-and-files rewinds produce this.
    #[serde(rename = "files-rolled-back")]
    FilesRolledBack,
    /// File restore failed and its rollback could not fully restore the pre-rewind state; only conversation-and-files rewinds produce this.
    #[serde(rename = "rollback-incomplete")]
    RollbackIncomplete,
    /// Conversation truncation failed. In conversation-and-files mode any files that were restored are left in place because conversation history cannot be un-truncated; in conversation-only mode no files are restored. Consult restoredFiles for what, if anything, was applied.
    #[serde(rename = "truncation-failed")]
    TruncationFailed,
    /// The conversation was rewound (and, in conversation-and-files mode, captured files were restored), but persisted checkpoints could not be cleaned up; reachable in either mode.
    #[serde(rename = "checkpoint-cleanup-failed")]
    CheckpointCleanupFailed,
    /// Files and conversation were rewound, but obsolete file snapshots could not be removed; only conversation-and-files rewinds produce this.
    #[serde(rename = "snapshot-prune-failed")]
    SnapshotPruneFailed,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Where this source lives — used for UI grouping
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum InstructionSourceLocation {
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

/// Category of instruction source — used for merge logic
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum InstructionSourceType {
    /// Instructions loaded from the user's home configuration.
    #[serde(rename = "home")]
    Home,
    /// Instructions loaded from repository-scoped files.
    #[serde(rename = "repo")]
    Repo,
    /// Instructions loaded from model-specific files.
    #[serde(rename = "model")]
    Model,
    /// Instructions loaded from VS Code instruction files.
    #[serde(rename = "vscode")]
    Vscode,
    /// Instructions discovered from nested agent files.
    #[serde(rename = "nested-agents")]
    NestedAgents,
    /// Instructions inherited from child instruction files.
    #[serde(rename = "child-instructions")]
    ChildInstructions,
    /// Instructions supplied by an installed plugin.
    #[serde(rename = "plugin")]
    Plugin,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Allowed values for the `McpAppsHostContextDetailsAvailableDisplayMode` enumeration.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpAppsHostContextDetailsAvailableDisplayMode {
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
pub enum McpAppsHostContextDetailsDisplayMode {
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
pub enum McpAppsHostContextDetailsPlatform {
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
pub enum McpAppsHostContextDetailsTheme {
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

/// Consumer allowed to call an MCP tool.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpToolUiVisibility {
    /// The model may call the tool.
    #[serde(rename = "model")]
    Model,
    /// An MCP App view may call the tool.
    #[serde(rename = "app")]
    App,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Outcome of starting the original prepared owned login.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpOwnedOauthLoginStatus {
    /// The original requester may open the returned authorisation URL.
    #[serde(rename = "awaiting-browser")]
    AwaitingBrowser,
    /// Cached credentials were accepted and the original server finished reconnecting.
    #[serde(rename = "connected")]
    Connected,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// The sender role of an MCP prompt message.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpPromptRole {
    /// A message from the user.
    #[serde(rename = "user")]
    User,
    /// A message from the assistant.
    #[serde(rename = "assistant")]
    Assistant,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Outcome of the sampling inference. 'success' produced a response; 'failure' encountered an error (including agent-side rejection by content filter or criteria); 'cancelled' the caller cancelled this execution via cancelSamplingExecution.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpSamplingExecutionAction {
    /// The sampling inference completed and produced a result.
    #[serde(rename = "success")]
    Success,
    /// The sampling inference failed or was rejected.
    #[serde(rename = "failure")]
    Failure,
    /// The sampling inference was cancelled before completion.
    #[serde(rename = "cancelled")]
    Cancelled,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// How environment-variable values supplied to MCP servers are resolved. "direct" passes literal string values; "indirect" treats values as references (e.g. names of environment variables on the host) that the runtime resolves before launch. Defaults to the runtime's startup mode; clients that intentionally launch MCP servers with literal values (e.g. CLI prompt mode and ACP) set this to "direct".
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpSetEnvValueModeDetails {
    /// Treat MCP server environment values as literal strings.
    #[serde(rename = "direct")]
    Direct,
    /// Treat MCP server environment values as host-side references to resolve before launch.
    #[serde(rename = "indirect")]
    Indirect,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Controls how availableTools (allowlist) and excludedTools (denylist) combine when both are set.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum OptionsUpdateToolFilterPrecedence {
    /// If availableTools is set, it is the only constraint that applies (excludedTools is ignored). Preserves CLI / pre-existing client behavior. Default.
    #[serde(rename = "available")]
    Available,
    /// A tool is enabled if and only if it matches the allowlist (or the allowlist is unset) AND it does not match the denylist. Makes 'all except X' expressible by combining the two lists.
    #[serde(rename = "excluded")]
    Excluded,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Hosting platform type of the repository
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionWorkingDirectoryContextHostType {
    /// The working directory repository is hosted on GitHub.
    #[serde(rename = "github")]
    GitHub,
    /// The working directory repository is hosted on Azure DevOps.
    #[serde(rename = "ado")]
    Ado,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Model capability category for grouping in the model picker
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ModelPickerCategory {
    /// Lightweight model category optimized for faster, lower-cost interactions.
    #[serde(rename = "lightweight")]
    Lightweight,
    /// Versatile model category suitable for a broad range of tasks.
    #[serde(rename = "versatile")]
    Versatile,
    /// Powerful model category optimized for complex tasks.
    #[serde(rename = "powerful")]
    Powerful,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Relative cost tier for token-based billing users
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ModelPickerPriceCategory {
    /// Lowest relative token cost tier.
    #[serde(rename = "low")]
    Low,
    /// Medium relative token cost tier.
    #[serde(rename = "medium")]
    Medium,
    /// High relative token cost tier.
    #[serde(rename = "high")]
    High,
    /// Highest relative token cost tier.
    #[serde(rename = "very_high")]
    VeryHigh,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Current policy state for this model
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ModelPolicyState {
    /// The model is enabled by policy.
    #[serde(rename = "enabled")]
    Enabled,
    /// The model is disabled by policy.
    #[serde(rename = "disabled")]
    Disabled,
    /// No explicit policy is configured for the model.
    #[serde(rename = "unconfigured")]
    Unconfigured,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// When the runtime may run an adapter without an explicit user action.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ModelProviderAutomaticDiscoveryMode {
    /// The adapter declares that automatic discovery is safe when the other policy fields are satisfied.
    #[serde(rename = "automatic")]
    Automatic,
    /// The adapter may refresh instances the user already configured, but must not scan for new instances automatically.
    #[serde(rename = "configuredOnly")]
    ConfiguredOnly,
    /// The adapter must run only after an explicit user action.
    #[serde(rename = "explicit")]
    Explicit,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Network reach an adapter may use during discovery.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ModelProviderDiscoveryNetworkScope {
    /// Discovery does not contact a network service.
    #[serde(rename = "none")]
    None,
    /// Discovery is limited to loopback addresses on the local machine.
    #[serde(rename = "loopbackOnly")]
    LoopbackOnly,
    /// Discovery contacts only endpoints the user already configured.
    #[serde(rename = "configuredEndpointOnly")]
    ConfiguredEndpointOnly,
    /// Discovery may scan or contact the local network.
    #[serde(rename = "localNetwork")]
    LocalNetwork,
    /// Discovery may contact remote internet services.
    #[serde(rename = "internet")]
    Internet,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Whether a planned configuration entry is new or already present in the session registry.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ModelProviderConfigurationDisposition {
    /// No matching entry is registered; the caller should add the entry.
    #[serde(rename = "create")]
    Create,
    /// An equivalent entry is already registered; the caller should reuse it rather than adding a duplicate.
    #[serde(rename = "alreadyConfigured")]
    AlreadyConfigured,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// The product serving the model, reported in telemetry as `model_provider`.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ProviderConfigModelProvider {
    /// OpenAI API.
    #[serde(rename = "openai")]
    Openai,
    /// Anthropic API.
    #[serde(rename = "anthropic")]
    Anthropic,
    /// Azure OpenAI Service.
    #[serde(rename = "azure_openai")]
    AzureOpenai,
    /// Ollama.
    #[serde(rename = "ollama")]
    Ollama,
    /// LM Studio.
    #[serde(rename = "lm_studio")]
    LmStudio,
    /// Foundry Local.
    #[serde(rename = "foundry_local")]
    FoundryLocal,
    /// llama.cpp server.
    #[serde(rename = "llama_cpp")]
    LlamaCpp,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Provider transport. Defaults to "http".
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ProviderConfigTransport {
    /// HTTP request/streaming transport.
    #[serde(rename = "http")]
    Http,
    /// WebSocket transport.
    #[serde(rename = "websockets")]
    Websockets,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Provider type. Defaults to "openai" for generic OpenAI-compatible APIs.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ProviderConfigType {
    /// Generic OpenAI-compatible API.
    #[serde(rename = "openai")]
    Openai,
    /// Azure OpenAI Service endpoint.
    #[serde(rename = "azure")]
    Azure,
    /// Anthropic API endpoint.
    #[serde(rename = "anthropic")]
    Anthropic,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Wire API format (openai/azure only). Defaults to "completions".
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ProviderConfigWireApi {
    /// OpenAI Chat Completions wire format.
    #[serde(rename = "completions")]
    Completions,
    /// OpenAI Responses API wire format.
    #[serde(rename = "responses")]
    Responses,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Transport to be used for provider requests.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ProviderEndpointTransport {
    /// HTTP request/streaming transport.
    #[serde(rename = "http")]
    Http,
    /// WebSocket transport.
    #[serde(rename = "websockets")]
    Websockets,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Provider family. Matches the `type` field of a BYOK provider config.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ProviderEndpointType {
    /// OpenAI-compatible endpoint (use the OpenAI client library).
    #[serde(rename = "openai")]
    Openai,
    /// Azure OpenAI endpoint (use the OpenAI client library with the Azure base URL).
    #[serde(rename = "azure")]
    Azure,
    /// Anthropic endpoint (use the Anthropic client library).
    #[serde(rename = "anthropic")]
    Anthropic,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Wire API to be used, when required for the provider type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ProviderEndpointWireApi {
    /// Classic chat-completions request shape.
    #[serde(rename = "completions")]
    Completions,
    /// Newer responses request shape.
    #[serde(rename = "responses")]
    Responses,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Whether the requested preference was already effective or was accepted for later transactional activation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ModelSwitchAutoTierStatus {
    /// The requested preference is already effective. No activation is pending for it, although this request may have cancelled an earlier unclaimed preference reported in `supersededAutoTier`.
    #[serde(rename = "unchanged")]
    Unchanged,
    /// The request was accepted but has not committed. A later user turn using the `auto` model must mint and validate the replacement before it becomes effective.
    #[serde(rename = "pending")]
    Pending,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Disposition of a permission request as observed by the responding client.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionOutcome {
    /// The request was approved automatically without a new human decision.
    #[serde(rename = "auto_approved")]
    AutoApproved,
    /// The request was denied without an interactive user decision; source records why.
    #[serde(rename = "autopilot_denied")]
    AutopilotDenied,
    /// The response came from an interactive user prompt.
    #[serde(rename = "prompted_user")]
    PromptedUser,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Response capability available to the client when it settled a permission request.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionResponseCapability {
    /// The client could ask a user for this decision.
    #[serde(rename = "interactive")]
    Interactive,
    /// The client could return an automated response but could not ask a user.
    #[serde(rename = "headless")]
    Headless,
    /// The client had no response path available.
    #[serde(rename = "none")]
    None,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Client surface that submitted a permission response.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionSurface {
    /// The interactive Copilot CLI terminal UI.
    #[serde(rename = "tui")]
    Tui,
    /// The non-interactive Copilot CLI prompt mode.
    #[serde(rename = "prompt_mode")]
    PromptMode,
    /// The Copilot App client.
    #[serde(rename = "copilot_app")]
    CopilotApp,
    /// An Agent Client Protocol host.
    #[serde(rename = "acp")]
    Acp,
    /// A generic Copilot SDK client.
    #[serde(rename = "sdk")]
    Sdk,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Where completed plugin content was staged before atomic promotion.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PluginInstallStagingMode {
    /// A sibling of the installed-plugins root, outside the recursively watched tree.
    #[serde(rename = "external")]
    External,
    /// A sibling of the destination plugin directory, used when external staging is unavailable.
    #[serde(rename = "destination_sibling")]
    DestinationSibling,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// The UI mode the agent was in when this message was sent. Defaults to the session's current mode.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SendAgentMode {
    /// The agent is responding interactively to the user.
    #[serde(rename = "interactive")]
    Interactive,
    /// The agent is preparing a plan before making changes.
    #[serde(rename = "plan")]
    Plan,
    /// The agent is working autonomously toward task completion.
    #[serde(rename = "autopilot")]
    Autopilot,
    /// The agent is in shell-focused UI mode.
    #[serde(rename = "shell")]
    Shell,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Whether this item is a queued user message or a queued slash command / model change
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum QueuePendingItemsKind {
    /// A queued user message.
    #[serde(rename = "message")]
    Message,
    /// A queued slash command or model-change command.
    #[serde(rename = "command")]
    Command,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// One atomic settings mutation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub(crate) enum SettingsEditOperation {
    /// Set one scalar value.
    #[serde(rename = "set")]
    Set,
    /// Add one string to a list, unless present.
    #[serde(rename = "add-item")]
    AddItem,
    /// Remove one string from a list, unless absent.
    #[serde(rename = "remove-item")]
    RemoveItem,
    /// Remove the setting without supplying a value.
    #[serde(rename = "remove")]
    Remove,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Repository host type, if known
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum WorkspaceSummaryHostType {
    /// Workspace summary repository is hosted on GitHub.
    #[serde(rename = "github")]
    GitHub,
    /// Workspace summary repository is hosted on Azure DevOps.
    #[serde(rename = "ado")]
    Ado,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Controls automatic non-interactive profile loading where supported. Explicit initScripts are unaffected.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ShellInitProfile {
    /// Disable automatic non-interactive profile loading. Explicit initScripts still run.
    #[serde(rename = "none")]
    None,
    /// Allow automatic non-interactive profile loading when supported. Explicit initScripts still run.
    #[serde(rename = "non-interactive")]
    NonInteractive,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Supported built-in shells for initialization scripts.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ShellInitScriptShell {
    /// Source the script in the built-in Bash shell on macOS and Linux.
    #[serde(rename = "bash")]
    Bash,
    /// Source the script in the built-in PowerShell shell on Windows.
    #[serde(rename = "powershell")]
    Powershell,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Sharing status for a synced session. "repo" makes the session visible to anyone with read access to the repository; "unshared" restricts it to the creator and collaborators.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionVisibilityStatus {
    /// The session is visible to repository readers.
    #[serde(rename = "repo")]
    Repo,
    /// The session is restricted to its creator and collaborators.
    #[serde(rename = "unshared")]
    Unshared,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Whether one atomic settings edit changed a file.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub(crate) enum SettingsEditStatus {
    /// The edit changed the setting and was stored.
    #[serde(rename = "changed")]
    Changed,
    /// The setting already had the requested state.
    #[serde(rename = "unchanged")]
    Unchanged,
    /// The existing settings file could not be edited safely; nothing was written.
    #[serde(rename = "invalid")]
    Invalid,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Context tier override for matching subagents
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SubagentSettingsEntryContextTier {
    /// Inherit the parent session's effective context tier at dispatch time.
    #[serde(rename = "inherit")]
    Inherit,
    /// Use the model's default context window.
    #[serde(rename = "default")]
    Default,
    /// Pin the subagent to the long-context tier when supported.
    #[serde(rename = "long_context")]
    LongContext,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// The user's response: accept (submitted), decline (rejected), or cancel (dismissed)
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum UIElicitationResponseAction {
    /// The user submitted the requested form values.
    #[serde(rename = "accept")]
    Accept,
    /// The user explicitly declined to provide the requested input.
    #[serde(rename = "decline")]
    Decline,
    /// The user dismissed the elicitation request.
    #[serde(rename = "cancel")]
    Cancel,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Current or terminal state of a workflow run.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum WorkflowRunStatus {
    /// The run was minted and is awaiting approval.
    #[serde(rename = "pending")]
    Pending,
    /// The run is executing.
    #[serde(rename = "running")]
    Running,
    /// The run completed successfully.
    #[serde(rename = "completed")]
    Completed,
    /// The run was interrupted while resource budget remained.
    #[serde(rename = "halted")]
    Halted,
    /// The current attempt stopped intentionally and the run may be resumed.
    #[serde(rename = "paused")]
    Paused,
    /// The run was cancelled before completion.
    #[serde(rename = "cancelled")]
    Cancelled,
    /// The workflow body failed or reached a cumulative resource ceiling.
    #[serde(rename = "error")]
    Error,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Kind of workflow progress line.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum WorkflowLogLineKind {
    /// A narrator log line.
    #[serde(rename = "log")]
    Log,
    /// A named workflow phase marker.
    #[serde(rename = "phase")]
    Phase,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Action the runtime selected for a durable workflow pause checkpoint.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum WorkflowPauseCheckpointAction {
    /// The checkpoint was committed by a prior paused attempt, so execution may continue.
    #[serde(rename = "continue")]
    Continue,
    /// This attempt claimed the checkpoint and must cooperatively stop.
    #[serde(rename = "pause")]
    Pause,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Derived lifecycle state of a workflow phase.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum WorkflowPhaseStatus {
    /// The phase has not been entered yet.
    #[serde(rename = "pending")]
    Pending,
    /// The phase is currently entered and accumulating active time.
    #[serde(rename = "active")]
    Active,
    /// The phase was entered and has since been closed.
    #[serde(rename = "completed")]
    Completed,
    /// The phase was never entered because a later phase was entered or the run reached a terminal state.
    #[serde(rename = "skipped")]
    Skipped,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Type of change represented by this file diff.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum WorkspaceDiffFileChangeType {
    /// The file was added.
    #[serde(rename = "added")]
    Added,
    /// The file was modified.
    #[serde(rename = "modified")]
    Modified,
    /// The file was deleted.
    #[serde(rename = "deleted")]
    Deleted,
    /// The file was renamed.
    #[serde(rename = "renamed")]
    Renamed,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Diff mode requested by the client.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum WorkspaceDiffMode {
    /// Return staged, unstaged, and untracked working tree changes.
    #[serde(rename = "unstaged")]
    Unstaged,
    /// Return changes compared with the default branch.
    #[serde(rename = "branch")]
    Branch,
    /// Return the cumulative diff of files Copilot changed this session (used in non-git workspaces).
    #[serde(rename = "session")]
    Session,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Allowed values for the `WorkspacesWorkspaceDetailsHostType` enumeration.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum WorkspacesWorkspaceDetailsHostType {
    /// Workspace repository is hosted on GitHub.
    #[serde(rename = "github")]
    GitHub,
    /// Workspace repository is hosted on Azure DevOps.
    #[serde(rename = "ado")]
    Ado,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

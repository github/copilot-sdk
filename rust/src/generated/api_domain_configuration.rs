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

/// Params to attach or detach an in-process ExtensionController delegate.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ConfigureSessionExtensionsParams {
    /// In-process ExtensionController delegate (CLI-only optimization). Marked internal: this field is excluded from the public SDK surface. The post-SDK extension surface exposes list/enable/disable/reload via dedicated RPCs served by the runtime.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) controller: Option<serde_json::Value>,
    /// Session to attach the extension controller delegate to.
    pub session_id: SessionId,
}

/// The declared shape of one feature flag, independent of any caller population.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct FeatureFlagMetadata {
    /// The population the flag is declared for before any environment or user-settings override is applied.
    #[doc(hidden)]
    pub(crate) availability: FeatureFlagAvailability,
    /// Whether the flag participates in the CAPI sanity suite.
    pub capi_sanity: bool,
    /// Text shown to a user choosing whether to turn the flag on in experimental mode. Absent when the flag is not user-facing.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub experimental_description: Option<String>,
    /// The online-experiment (ExP) flag this maps to, when the flag is assigned by an experiment rather than declared statically.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub exp_flag: Option<String>,
    /// Environment variable that overrides this flag's experiment assignment locally in the experiment service, not in `featureFlags.resolve`. Present exactly when `expFlag` is, and derived from it, so a caller reads a local override without reproducing the naming rule.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub exp_flag_env_var: Option<String>,
    /// The flag's name, matching the key it is listed under.
    pub name: String,
}

/// Every feature flag this runtime declares, with the shape each one was declared with.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct FeatureFlagsMetadataResult {
    /// Declared feature flags keyed by flag name.
    #[doc(hidden)]
    pub(crate) flags: HashMap<String, FeatureFlagMetadata>,
}

/// Caller population and overrides for static feature-flag resolution, independent of experiment assignments and active runtime state.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct FeatureFlagsResolveRequest {
    /// Resolve against this already-computed flag set rather than each flag's declared availability. Keyed by flag name.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub base_flags: Option<HashMap<String, bool>>,
    /// User settings to apply, including `enabledFeatureFlags`, legacy `feature_flags.enabled`, and supported convenience settings.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub config: Option<serde_json::Value>,
    /// Environment for direct feature-flag overrides and `COPILOT_CLI_ENABLED_FEATURE_FLAGS`. Does not apply `COPILOT_EXP_*` experiment assignments. Defaults to this runtime process's own environment.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub environment: Option<HashMap<String, String>>,
    /// Flags forced to a value after every other layer, keyed by flag name.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub flag_overrides: Option<HashMap<String, bool>>,
    /// Whether the caller is in an experimental population.
    pub is_experimental: bool,
    /// Whether the caller is GitHub or Microsoft staff.
    pub is_staff: bool,
    /// GitHub-staff membership. Omit when it was never recorded: a legacy staff account then counts as both staff populations and keeps every staff feature.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub is_staff_github: Option<bool>,
    /// Microsoft-staff membership. Omitted the same way as `isStaffGitHub`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub is_staff_microsoft: Option<bool>,
    /// Whether the caller belongs to the team population used by feature availability.
    pub is_team: bool,
}

/// Caller population and startup inputs to resolve named online-experiment gates for. Carries no account: the runtime resolves the assignment for the authentication it already holds.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct FeatureFlagsResolveStartupGatesRequest {
    /// User settings document whose `featureFlags` block is layered on top of the resolved set.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub config: Option<serde_json::Value>,
    /// Environment to read flag overrides from. Defaults to this runtime process's own environment.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub environment: Option<HashMap<String, String>>,
    /// When this installation first launched, in milliseconds since the Unix epoch. Experiments that segment on installation age read it. Omit when the first launch was never recorded.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub first_launch_at_millis: Option<f64>,
    /// The feature flags to resolve. A name this runtime does not declare is reported as an error rather than as a disabled flag.
    pub flags: Vec<String>,
    /// Whether the caller is in an experimental population.
    pub is_experimental: bool,
    /// Whether the caller is GitHub or Microsoft staff.
    pub is_staff: bool,
    /// GitHub-staff membership. Omit when it was never recorded: a legacy staff account then counts as both staff populations and keeps every staff feature.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub is_staff_github: Option<bool>,
    /// Microsoft-staff membership. Omitted the same way as `isStaffGitHub`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub is_staff_microsoft: Option<bool>,
    /// Whether the caller belongs to a team plan.
    pub is_team: bool,
    /// Whether the runtime may refresh the cached assignment over the network without an active session. The refresh never blocks this call: the answer comes from the cache either way, so a refresh only affects a later launch.
    pub refresh_without_session: bool,
    /// Whether streamer mode is on, which withholds the populations an experiment would otherwise segment on.
    pub streamer_mode: bool,
}

/// The verdict of the `forceLoginOrgs` enterprise sign-in pin for one credential. A managed device may pin Copilot to an approved set of GitHub organizations so a developer cannot sign in with a personal — or another enterprise's — account and escape every control that keys off the signed-in identity. A host must refuse to proceed when `allowed` is false; the remaining fields explain the refusal rather than qualifying it.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ForceLoginOrgsDecision {
    /// Whether the client may proceed. True whenever no pin is in force, so an unmanaged host is never blocked.
    pub allowed: bool,
    /// A stable kebab-case reason code when refused: `org-not-approved`, `unverifiable-credential`, `policy-undetermined`, `policy-invalid`, `not-signed-in`, or `membership-unknown`. Omitted when allowed. Treat an unrecognized code as a refusal rather than mapping it to an allow: a code this host has not been taught about still denied the credential.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub denial: Option<String>,
    /// Whether a pin applies at all. Distinguishes "allowed because unmanaged" from "allowed because the account is approved", which a diagnostics surface needs and which also decides whether a bring-your-own-key provider may serve inference.
    pub enforced: bool,
    /// The approved organization the account matched, when allowed under a pin.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub matched_org: Option<String>,
    /// A developer-facing explanation of the refusal. Omitted when allowed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub message: Option<String>,
    /// Error-level detail from managed-policy discovery — which file failed to load, which field failed validation. Carried on the decision rather than left to the log because a refusal can exit the process before a log sink exists, which would discard the only actionable diagnostic a misconfigured managed device produces.
    pub policy_diagnostics: Vec<String>,
    /// The approved organization logins, as configured. Enterprise configuration rather than user data, so it is safe to name in a message or a log line.
    pub required_orgs: Vec<String>,
}

/// Organization support information for end users.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ManagedCommunicationSupport {
    /// Absolute http or https URL of an organization-maintained help page, for example one that names who administers Copilot. Clients may link to it wherever they would otherwise tell users to contact their administrator. Open it only in an external browser.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub url: Option<String>,
}

/// Organization-specific information that clients surface to end users. Informational only; the runtime does not enforce it.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ManagedCommunicationSettings {
    /// Where end users find help for this organization's Copilot deployment.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub support: Option<ManagedCommunicationSupport>,
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
pub struct ManagedPermissionOperation {
    /// Operation to evaluate. Currently only url is supported.
    pub kind: ManagedPermissionOperationKind,
    /// Absolute HTTP(S) URL with a host. Invalid URLs reject the entire evaluation batch.
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
pub struct ManagedPermissionEvaluation {
    /// Original input operation associated with this verdict.
    pub operation: ManagedPermissionOperation,
    /// Managed-policy decision only; never bypasses user permissions or other security controls.
    pub verdict: ManagedPermissionVerdict,
}

/// Managed policy context and ordered operations for pure, sessionless evaluation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ManagedPermissionsEvaluateRequest {
    /// Trusted, reusable managed permission policy snapshot; no policy is discovered during evaluation.
    pub context: ManagedPermissionsContext,
    /// Operations to evaluate, preserving input order and duplicates. An empty batch is valid.
    pub operations: Vec<ManagedPermissionOperation>,
}

/// Ordered managed permission verdicts and the supplied policy's fail-closed posture.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ManagedPermissionsEvaluateResult {
    /// Managed permission policy could not be determined; every operation is denied.
    pub fail_closed: bool,
    /// One verdict per input operation, in the same order, including duplicates.
    pub results: Vec<ManagedPermissionEvaluation>,
}

/// Lock state and provenance of one managed setting.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ManagedSettingMeta {
    /// Whether users and repositories may choose a different value. `false` means policy locks the value.
    pub overridable: bool,
    /// Original managed value when the runtime adjusted it to a supported effective value. Omitted when no adjustment was needed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub requested: Option<String>,
    /// Channel that supplied this scalar value, matching a `layers[].source`: `device`, `server`, or `policyHelper`. These scalar defaults select one winning channel, not a mixed source. Treat unknown values as additional channels; more may be added.
    pub source: String,
}

/// A user settings document and the effective managed policy whose controlled entries should be stamped onto it.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ManagedSettingsApplyControlsParams {
    /// The effective managed policy, as composed by `managedSettings.resolveEffectiveValue`. Omit it on an unmanaged host: the settings then come back unchanged.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub managed: Option<serde_json::Value>,
    /// The user settings document to stamp. Entries the managed layer does not name are returned exactly as supplied.
    pub settings: serde_json::Value,
}

/// The user settings with the managed layer's controlled entries stamped on.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ManagedSettingsApplyControlsResult {
    /// The resulting settings document.
    pub settings_json: serde_json::Value,
}

/// One candidate channel; absent settings represents a channel that delivered no document.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ManagedSettingsComposeLayer {
    /// Candidate managed-settings document. Omit when the channel delivered none, as in resolve output.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub settings: Option<serde_json::Value>,
    /// The channel whose candidate document is being supplied.
    pub source: ManagedSettingsChannel,
}

/// Candidate managed-settings documents to merge without applying them.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ManagedSettingsComposeRequest {
    /// One entry per channel. `source` must be `device`, `server`, or `policyHelper`, each at most once (checked at runtime); order does not matter, because channel precedence is fixed. To preview documents from resolve output, map recognized source strings to ManagedSettingsChannel and copy their settings; generated resolve and compose layer types are distinct. Omitted settings means this channel delivered no document. Supplied documents must be valid within the preview limits; warnings are returned in diagnostics. Compose does not reproduce source-failure state or retained enforcement floors from resolve.
    pub layers: Vec<ManagedSettingsComposeLayer>,
}

/// One validation finding for a managed-settings document.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ManagedSettingsDiagnostic {
    /// Human-readable description of the finding.
    pub message: String,
    /// Dot-separated path of the offending setting, such as `autoTier.overridable`. Empty for the document as a whole.
    pub path: String,
    /// Whether the finding rejects the document.
    pub severity: ManagedSettingsDiagnosticSeverity,
}

/// One managed-settings channel and the document it delivered.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ManagedSettingsLayer {
    /// Validated managed-settings document this channel delivered. Absent when the channel delivered none.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub settings: Option<serde_json::Value>,
    /// Channel identifier: `device` (MDM, plist, registry, or managed file), `server` (account or organization policy), or `policyHelper` (session-local helper output, supported by compose). Treat unknown output values as additional channels; more may be added.
    pub source: String,
}

/// Per-key lock state and provenance for `ManagedSettingsValues`, with the same field names. Producers emit each typed key in values and meta together; both outer objects are omitted when no typed key is set.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ManagedSettingsMeta {
    /// Lock state and provenance of `values.autoTier`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auto_tier: Option<ManagedSettingMeta>,
    /// Provenance of `values.communication`. Always reported as not overridable: users cannot replace the organization's information.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub communication: Option<ManagedSettingMeta>,
    /// Lock state and provenance of `values.contextTier`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context_tier: Option<ManagedSettingMeta>,
    /// Lock state and provenance of `values.effortLevel`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub effort_level: Option<ManagedSettingMeta>,
    /// Lock state and provenance of values.fusionTier, including a legacy alias's original source. An unmanaged value imposes no tier lock.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub fusion_tier: Option<ManagedSettingMeta>,
    /// Lock state and provenance of `values.model`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model: Option<ManagedSettingMeta>,
}

/// Effective enterprise managed settings and contributing channels. Session events report applied policy; sessionless resolve reports an account/device snapshot, and compose reports a non-applying preview of candidate documents. Device values take precedence over server values, then the policy helper, per ordinary key, while permissions compose restrictively. Session-local SDK-client policy is included only in session results. Marked experimental while the managed-settings surface stabilizes.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ManagedSettingsResolvedData {
    /// Whether an explicit enterprise policy restriction disables bypass-permissions ("yolo") mode for this session. Deny-wins across layers; an unresolved policy does not force this on by itself.
    pub bypass_permissions_disabled: bool,
    /// Whether a session-local permissions layer injected by the SDK host was present
    #[serde(skip_serializing_if = "Option::is_none")]
    pub client_managed: Option<bool>,
    /// Whether an actual device MDM/plist/registry/file managed-settings layer was present
    pub device_managed: bool,
    /// Whether managed policy could not be determined (e.g. a failed server fetch) and unresolved-policy safeguards remain active. This does not by itself disable bypass-permissions; `bypassPermissionsDisabled` reports only an explicit policy restriction.
    pub fail_closed: bool,
    /// The setting keys under enterprise management in the effective managed settings (e.g. `model`, `enabledPlugins`, `permissions`). Empty when no managed settings are in force.
    pub managed_keys: Vec<String>,
    /// Whether at least two managed sources supplied permission allowlists, so enforcement intersects them and the flattened settings payload omits `permissions.allow`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub permissions_allow_intersected: Option<bool>,
    /// Reusable retained managed permission policy for managedSettings.permissions.evaluate, including source composition and session-local injection. Provided by live session snapshots; absent in older events. Refresh the context when managed policy changes.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub permissions_context: Option<ManagedPermissionsContext>,
    /// Explicit Policy Helper structured-failure and recheck capabilities supported by this runtime.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub policy_helper_capabilities: Option<PolicyHelperCapabilities>,
    /// The latest unresolved Policy Helper failure for this exact policy context. It remains present across retained-policy refresh failures and clears only after fresh success or authoritative removal.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub policy_helper_failure: Option<PolicyHelperFailure>,
    /// Whether the policy-helper managed-settings layer was present. The policy helper is the weakest channel: it fills keys no enterprise source set and can never replace one.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub policy_helper_managed: Option<bool>,
    /// Whether the effective sandbox policy forces the sandbox on *only* because managed policy could not be determined, rather than because the policy requires it. Lets clients tell a user whose `--no-sandbox` was overridden that the sandbox stayed on as a fail-closed fallback, instead of attributing it to an administrator who set no such policy.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sandbox_enabled_by_undetermined_policy: Option<bool>,
    /// Whether the server (account/org) managed-settings layer was present
    pub server_managed: bool,
    /// The effective (resolved) managed settings values, so clients can render exactly what is enforced. Absent when no managed policy is in force.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub settings: Option<serde_json::Value>,
    /// Channel summary: `server`, `device`, `client`, or `policyHelper` when exactly one channel contributed; `mixed` when multiple channels contributed; otherwise `none`. Consult the per-channel booleans for exact provenance.
    pub source: ManagedSettingsResolvedSource,
}

/// Typed effective values of managed settings. Each field mirrors the managed-settings schema key of the same name; more keys are added as they are typed.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ManagedSettingsValues {
    /// Managed Auto routing preference, used when the selected model is `auto`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auto_tier: Option<AutoTier>,
    /// Organization-specific information for clients to surface to end users. Present when policy sets `communication.support.url`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub communication: Option<ManagedCommunicationSettings>,
    /// Managed context-tier default for the managed concrete model.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context_tier: Option<ContextTier>,
    /// Managed reasoning-effort default for the managed concrete model. The runtime clamps it to an entitled effort when model availability is known.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub effort_level: Option<String>,
    /// Managed Fusion preference. Takes precedence over autoTier even when unmanaged, which means no managed tier and forbids legacy fallback. Legacy Auto values are mapped only when no managed source supplies fusionTier.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub fusion_tier: Option<ManagedFusionTier>,
    /// Managed default model identifier. When model availability was resolved, aliases and family names are projected to a concrete available model ID; otherwise the configured value is returned.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model: Option<String>,
}

/// The effective managed settings the runtime would enforce for the given documents, in the same shape `managedSettings.resolve` returns.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ManagedSettingsComposeResult {
    /// Warnings about ignored content, with paths prefixed by the channel name.
    pub diagnostics: Vec<ManagedSettingsDiagnostic>,
    /// Only the supplied channels, strongest first, with canonical documents. Empty canonical documents are represented as absent settings, as in live resolution.
    pub layers: Vec<ManagedSettingsLayer>,
    /// Per-key lock state and provenance for `values`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub meta: Option<ManagedSettingsMeta>,
    /// Effective managed settings, in the same shape as `session.managedSettings.get`.
    pub resolved: ManagedSettingsResolvedData,
    /// Typed effective values, as in `managedSettings.resolve`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub values: Option<ManagedSettingsValues>,
}

/// Inputs describing the host and account an ingestion should resolve managed policy for.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ManagedSettingsIngestParams {
    /// The authenticated identity to resolve policy for. Omitted before sign-in, in which case no server layer is fetched.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auth_info: Option<serde_json::Value>,
    /// Whether to ignore a cached server policy when reading, forcing the resolution to come from a live fetch.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub bypass_cache_read: Option<bool>,
    /// Name of the embedding client, reported to the managed-settings service as `client_id` so it can refuse to deliver a policy this client is too old to enforce. Omit it and the runtime reports a generic identity; the runtime version sent alongside is the gate that actually matters.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub client_name: Option<String>,
    /// A device layer supplied by the host instead of discovered from the machine. Pair it with `hasDeviceResponse` so an explicit absence is distinguishable from an omitted override.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub device_response: Option<serde_json::Value>,
    /// The host's environment variables, read for cache location and policy overrides. Supplied by the host rather than read from the runtime process so a command that selected its own configuration home is honored.
    pub environment: serde_json::Value,
    /// Whether `deviceResponse` is an override at all. True with a null `deviceResponse` asserts the device layer is empty; false discovers it from the machine.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub has_device_response: Option<bool>,
    /// The host's home directory, used to resolve cache and managed-file locations.
    pub home_directory: String,
    /// The host's platform, in Node's `process.platform` spelling, used to locate platform-specific cache and policy paths.
    pub platform: String,
    /// Whether a configured platform policy helper may be run to produce an enforced helper layer.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub policy_helper_enabled: Option<bool>,
    /// The policy-helper invocation reason. Defaults to sessionStart.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) policy_helper_reason: Option<ManagedSettingsPolicyHelperReason>,
    /// Directory the policy helper runs in when this ingestion is not owned by a registered session. A host resolving policy before its session exists must pass this, or a directory-sensitive helper judges the wrong repository.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub policy_helper_working_directory: Option<String>,
    /// Whether to run ingestion. Omit or set false to return an empty result without discovering or fetching policy.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub self_fetch: Option<bool>,
    /// How this ingestion may use the on-disk server-policy cache. Defaults to the runtime's own policy when omitted.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) server_policy_cache_mode: Option<ManagedSettingsServerPolicyCacheMode>,
    /// The session whose credential store and working directory the policy helper should use. Omit it when no session exists yet and the process-wide store applies.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_id: Option<SessionId>,
    /// User agent to send with the policy fetch.
    pub user_agent: String,
}

/// One diagnostic emitted while ingesting managed settings. Returned rather than logged by the runtime so the host decides where ingestion diagnostics go, which matters because an ingestion runs before a session exists to carry a log sink.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ManagedSettingsLogMessage {
    /// Severity of this diagnostic.
    #[doc(hidden)]
    pub(crate) level: ManagedSettingsLogLevel,
    /// Human-readable diagnostic text.
    pub message: String,
}

/// The managed policy layers an ingestion resolved, together with the per-layer outcome flags a host needs to decide how much to trust the result. The flags are reported rather than folded into the settings because a host must be able to distinguish an unmanaged device from a managed device whose policy could not be confirmed, and apply its fail-closed gates in the second case.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ManagedSettingsIngestResult {
    /// The device layer could not be read, so it is unconfirmed rather than known-absent.
    pub device_load_failed: bool,
    /// The device layer discovered from the machine, when one exists.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub device_response: Option<serde_json::Value>,
    /// The sandbox floor contributed by the device layer.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub device_sandbox_floor: Option<serde_json::Value>,
    /// Whether the device layer's sandbox intent could not be determined, so a consumer must not read its absence as permission to run unsandboxed.
    pub device_sandbox_undetermined: bool,
    /// A device-managed policy is known to exist.
    pub device_source_found: bool,
    /// Diagnostics emitted while resolving the layers, in the order they occurred.
    #[doc(hidden)]
    pub(crate) log_messages: Vec<ManagedSettingsLogMessage>,
    /// The effective managed permissions layer, when the resolved policy carries one.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub permissions: Option<serde_json::Value>,
    /// Which channel registered the policy helper that produced the helper layer.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) policy_helper_authority: Option<ManagedSettingsPolicyHelperAuthority>,
    /// A policy helper is configured for this host.
    pub policy_helper_configured: bool,
    /// The policy helper's failure must be treated as fail-closed rather than ignored.
    pub policy_helper_fail_closed: bool,
    /// Structured latest Policy Helper failure for this ingestion, when one occurred.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub policy_helper_failure: Option<PolicyHelperFailure>,
    /// The policy helper was configured but could not be run or its output could not be read.
    pub policy_helper_load_failed: bool,
    /// The layer produced by the platform policy helper, when one ran.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub policy_helper_response: Option<serde_json::Value>,
    /// A live fetch failed but a recent cached entry was served in its place, so `serverFetchFailed` stays false. Lets a consumer distinguish a clean fetch from a cache-backed fallback.
    pub server_cache_fallback: bool,
    /// Minimum client version requested by a successful response. Advisory only; the delivered policy remains active.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub server_client_update_requested: Option<String>,
    /// The server refused to deliver a policy because this client is too old to enforce it, carrying the user-facing sentence describing the refusal. No retry will help; the remedy is a client update. Gates that exist only because of managed policy should deny on this rather than falling back to their unmanaged default.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub server_client_update_required: Option<String>,
    /// The error that made the server fetch fail, when one did.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub server_fetch_error: Option<String>,
    /// The server fetch failed and no cached entry stood in for it, so the server layer is unconfirmed and fail-closed gates apply.
    pub server_fetch_failed: bool,
    /// The server fetch failed while a policy demanded a fresh confirmation, which is the strict posture an enterprise opted into rather than an ordinary transient failure.
    pub server_forced_refresh_failure: bool,
    /// The synthesized fail-closed permissions layer applied in place of a policy a refused client may not enforce.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub server_permissions_floor: Option<serde_json::Value>,
    /// How the server layer was resolved.
    #[doc(hidden)]
    pub(crate) server_resolution: ManagedSettingsServerPolicyResolution,
    /// The account's server-delivered policy, when one was resolved.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub server_response: Option<serde_json::Value>,
    /// The server fetch failed in a way a later retry could succeed at, as opposed to a refusal that no retry will change.
    pub server_retryable_failure: bool,
    /// The sandbox object from a cached server policy that a forced refresh declined to serve in full. Folded back into the effective sandbox layer so an outage during a forced refresh still synthesizes the fail-closed sandbox floor from the administrator's known intent.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub server_sandbox_floor: Option<serde_json::Value>,
    /// A server-managed policy is known to exist even though `serverResponse` is intentionally unset. Consumers should OR this with the presence of `serverResponse` when deciding whether the device is managed, so a fresh start during an outage still applies the fail-closed gates instead of treating the device as unmanaged.
    pub server_source_found: bool,
    /// No usable token was available, so no server fetch was attempted.
    pub server_token_unavailable: bool,
    /// The effective managed settings composed from every resolved layer. Omitted when no layer set anything, so an unmanaged host locks nothing.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub settings: Option<serde_json::Value>,
}

/// Inputs describing the host and account whose managed remote-control policy should be resolved. Mirrors `managedSettings.ingest`, and deliberately carries no retention decision: reading this gate never becomes this runtime process's startup policy.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ManagedSettingsLoadRemoteControlParams {
    /// The authenticated identity to resolve policy for. Omitted before sign-in, in which case no server layer is fetched.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auth_info: Option<serde_json::Value>,
    /// Name of the embedding client, reported to the managed-settings service as `client_id` so it can refuse to deliver a policy this client is too old to enforce. That refusal is exactly what turns into an explicit `disabled` below, so a host that omits this field forfeits the protection.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub client_name: Option<String>,
    /// A device layer supplied by the host instead of discovered from the machine. Pair it with `hasDeviceResponse` so an explicit absence is distinguishable from an omitted override.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub device_response: Option<serde_json::Value>,
    /// The host's environment variables, read for cache location and policy overrides.
    pub environment: serde_json::Value,
    /// Whether `deviceResponse` is an override at all. True with a null `deviceResponse` asserts the device layer is empty; false discovers it from the machine.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub has_device_response: Option<bool>,
    /// The host's home directory, used to resolve cache and managed-file locations.
    pub home_directory: String,
    /// The host's platform, in Node's `process.platform` spelling, used to locate platform-specific cache and policy paths.
    pub platform: String,
    /// Whether a configured platform policy helper may be run to produce an enforced helper layer.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub policy_helper_enabled: Option<bool>,
    /// The policy-helper invocation reason. Defaults to sessionStart.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) policy_helper_reason: Option<ManagedSettingsPolicyHelperReason>,
    /// Whether to run ingestion. Omit or set false to return an empty result without discovering or fetching policy.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub self_fetch: Option<bool>,
    /// User agent to send with the policy fetch.
    pub user_agent: String,
}

/// The managed remote-control policy in force for an account, if any. An absent policy means the account is unmanaged for this capability, which a host reads as allowed — so absence here is a positive statement that no policy applies, never a stand-in for a resolution that failed. A resolution that cannot be completed is rejected instead.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ManagedSettingsLoadRemoteControlResult {
    /// The account's `remoteControl` policy document. Absent when no managed policy governs remote control for this account. Reported as an explicit disabled policy, rather than as absence, when the service refused to deliver a policy this client is too old to enforce.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub remote_control: Option<serde_json::Value>,
}

/// The client-supplied managed settings to validate and normalize. Omitting them is valid and yields no policy.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ManagedSettingsParseClientValueParams {
    /// The managed settings a client is supplying for a session it is about to open. Omit them when the client is imposing no policy of its own.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub settings: Option<serde_json::Value>,
}

/// The accepted managed settings, reduced to the keys a client is allowed to set.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ManagedSettingsParseClientValueResult {
    /// The normalized settings. Omitted when the client supplied none. An accepted document that sets nothing comes back as an empty object, which is distinct from omission.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub settings_json: Option<serde_json::Value>,
}

/// The device-managed settings layer, with the sandbox floor and the load outcome a host needs to compose it into an effective policy.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ManagedSettingsReadDeviceResult {
    /// Whether discovery or validation failed. A host must treat this as "policy unknown" rather than "no policy": absent settings with this set false mean the device genuinely has none.
    pub load_failed: bool,
    /// Sandbox floor read from the discovered document before validation, so a document that is otherwise rejected can still raise the floor it declared.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sandbox_floor: Option<serde_json::Value>,
    /// Validated, canonical device-managed settings. Omitted when no device policy was discovered, when the discovered document failed validation, or when it validated to an empty object — an empty document carries no policy, and reporting it as present would outrank layers that do.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub settings: Option<serde_json::Value>,
}

/// Validated device-managed settings discovered before a session exists.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ManagedSettingsReadResult {
    /// Discovery or validation error text when managed settings could not be read safely.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error_message: Option<String>,
    /// Validated, canonical managed-settings JSON. Omitted when no managed settings were discovered or when discovered settings failed validation.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub settings_json: Option<serde_json::Value>,
}

/// The bypass-permissions state a host holds today, the account now signed in, and what the managed-policy resolution said.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ManagedSettingsResolveBypassDisableTransitionParams {
    /// The account signed in now. A `managed` disable pinned to a different account is lifted.
    pub active_identity: String,
    /// The account a `managed` disable is pinned to. Omit it when the current source is not `managed`; it is ignored for the other sources.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub current_managed_identity: Option<String>,
    /// Which layer currently disables bypass mode. Omit it when nothing does.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) current_source: Option<ManagedSettingsBypassDisableSource>,
    /// What the resolution said about bypass mode. Send `unknown` rather than omitting the field when there is no verdict.
    #[doc(hidden)]
    pub(crate) policy: ManagedSettingsBypassPolicyResolution,
}

/// The bypass-permissions state a host should hold after the resolution, and what it must do to get there.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ManagedSettingsResolveBypassDisableTransitionResult {
    /// The account the returned `managed` source is pinned to. Store it alongside `source`; the next call needs it to decide whether an account switch lifts the disable.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub managed_identity: Option<String>,
    /// The value to store in the host's own bypass-disabled flag. Omitted means leave it exactly as it is, which is not the same as being told to store `false`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub set_disabled_state: Option<bool>,
    /// Which layer disables bypass mode after this resolution. Omitted when none does, which is what a lifted disable looks like.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) source: Option<ManagedSettingsBypassDisableSource>,
    /// Whether a bypass-permissions session already running must be turned off. True only when this resolution newly disables bypass mode, so a host that is already compliant is not interrupted.
    pub suppress_runtime_allow_all: bool,
}

/// The credential and environment to evaluate the device-channel sign-in pin against.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ManagedSettingsResolveDeviceForceLoginOrgsGateParams {
    /// The authenticated identity to judge. Omitted when signed out, which is the case the device channel exists to serve: it can refuse a first sign-in before any credential exists.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auth_info: Option<serde_json::Value>,
    /// The host's environment variables. Read for the variables that would make an identity's membership snapshot forgeable, so a credential whose provenance cannot be trusted is refused rather than accepted.
    pub environment: serde_json::Value,
}

/// The managed-settings layers to compose into an effective policy. Every layer is optional; omitting all of them yields no effective policy.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ManagedSettingsResolveEffectiveValueParams {
    /// The device sandbox floor, folded into the device layer under `sandbox` before composition.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub device_sandbox_floor: Option<serde_json::Value>,
    /// The device layer: native MDM and managed-file settings describing the machine rather than the account.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mdm: Option<serde_json::Value>,
    /// The policy-helper layer, an enforced channel sourced from a platform policy helper process.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub policy_helper: Option<serde_json::Value>,
    /// The account's server-delivered org policy, as returned by a managed-settings fetch. Omitted before sign-in or when the fetch has not landed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub server: Option<serde_json::Value>,
}

/// The effective managed policy composed from the supplied layers.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ManagedSettingsResolveEffectiveValueResult {
    /// The composed effective managed settings. Omitted when no layer sets anything, so an unmanaged host locks nothing. Carries `sandboxAdvisoryFloorDeclined: true` when an advisory server force-on was dropped because this host cannot run the sandbox.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub settings_json: Option<serde_json::Value>,
}

/// Optional opaque account selection or GitHub token whose managed settings are resolved.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ManagedSettingsResolveRequest {
    /// Embedding client identity for server policy requests, as in session creation. Omit for the CLI identity.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub client_name: Option<String>,
    /// GitHub token to resolve instead of the current account. The call fails when the token cannot be resolved.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub git_hub_token: Option<String>,
    /// Opaque account identifier returned by `account.getAllUsers`. When omitted, the current account is used, or device policy only when no account is signed in.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub selection_id: Option<String>,
    /// Working directory used to run an organization policy helper. When omitted, sessionless resolution does not run the helper.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub working_directory: Option<String>,
}

/// Effective enterprise managed settings for an account, resolved without a session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ManagedSettingsResolveResult {
    /// Printable opaque identity of the account the settings were resolved for, suitable for comparison and storage, not an account selectionId. Absent when no account was available, in which case only device policy is reported.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub account: Option<String>,
    /// Warnings about unavailable policy sources or a failed refresh served from cache. A cached response is not proof of a successful live fetch; `resolved.failClosed` separately describes enforcement.
    pub diagnostics: Vec<ManagedSettingsDiagnostic>,
    /// Each managed-settings channel consulted, strongest first, with the validated document it delivered before merging. `resolved.settings` is the merged result. More channels may be added over time.
    pub layers: Vec<ManagedSettingsLayer>,
    /// Per-key lock state and provenance for the entries in `values`, using the same key names.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub meta: Option<ManagedSettingsMeta>,
    /// Reusable source-composed permission policy for managedSettings.permissions.evaluate. Unlike resolved.settings.permissions, this retains every source's allowlist and default-prompt semantics. Refresh through resolve when account/device policy changes. Absent on runtimes that do not support permission evaluation.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub permissions_context: Option<ManagedPermissionsContext>,
    /// Effective managed settings from the device and account (server) channels, in the same shape as `session.managedSettings.get`, excluding session-local injection.
    pub resolved: ManagedSettingsResolvedData,
    /// Typed effective values of managed settings, keyed like the managed-settings schema and already resolved across channels, with the `{ "overridable": ... }` wrapper removed. Present when policy sets at least one typed key. Keys not typed here are available in `resolved.settings`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub values: Option<ManagedSettingsValues>,
}

/// Inputs describing the host and account a startup policy resolution should run for. Mirrors `managedSettings.ingest`, and adds the decision of whether the resolved policy is retained for this runtime process.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ManagedSettingsResolveStartupPolicyParams {
    /// The authenticated identity to resolve policy for. Omitted before sign-in, in which case no server layer is fetched and the sign-in pin is evaluated against no credential.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auth_info: Option<serde_json::Value>,
    /// Whether to ignore a cached server policy when reading, forcing the resolution to come from a live fetch.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub bypass_cache_read: Option<bool>,
    /// Name of the embedding client, reported to the managed-settings service as `client_id` so it can refuse to deliver a policy this client is too old to enforce.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub client_name: Option<String>,
    /// A device layer supplied by the host instead of discovered from the machine. Pair it with `hasDeviceResponse` so an explicit absence is distinguishable from an omitted override.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub device_response: Option<serde_json::Value>,
    /// The host's environment variables, read for cache location, policy overrides, and the variables that would make a membership snapshot forgeable.
    pub environment: serde_json::Value,
    /// Whether `deviceResponse` is an override at all. True with a null `deviceResponse` asserts the device layer is empty; false discovers it from the machine.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub has_device_response: Option<bool>,
    /// The host's home directory, used to resolve cache and managed-file locations.
    pub home_directory: String,
    /// The host's platform, in Node's `process.platform` spelling, used to locate platform-specific cache and policy paths.
    pub platform: String,
    /// Whether a configured platform policy helper may be run to produce an enforced helper layer.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub policy_helper_enabled: Option<bool>,
    /// The policy-helper invocation reason. Defaults to sessionStart.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) policy_helper_reason: Option<ManagedSettingsPolicyHelperReason>,
    /// Directory the policy helper runs in when this resolution is not owned by a registered session. A host resolving policy before its session exists must pass this, or a directory-sensitive helper judges the wrong repository.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub policy_helper_working_directory: Option<String>,
    /// Whether to retain the resolved policy as this runtime process's startup policy, so later work in the process enforces it. Set false to resolve without side effects, which is what a protocol host wants before session creation: its sessions apply the complete policy through the normal lifecycle, and retaining here would let a readiness credential affect unrelated sessions. The returned verdict is identical either way — this decides only whether the process keeps it.
    pub retain_and_apply: bool,
    /// Whether to run ingestion. Omit or set false to return an empty result without discovering or fetching policy.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub self_fetch: Option<bool>,
    /// How this resolution may use the on-disk server-policy cache. Defaults to the runtime's own policy when omitted. Rejected when combined with `bypassCacheRead`, because the two express the same decision and accepting both would leave the caller unsure which one the runtime honored.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) server_policy_cache_mode: Option<ManagedSettingsServerPolicyCacheMode>,
    /// The session whose credential store, working directory and client kind the resolution should use. Omit it when no session exists yet and the process-wide store applies. A resolution whose session names a credential other than `authInfo` is returned but not retained, so a stale sign-in cannot apply policy for an account that has already been switched away from.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_id: Option<SessionId>,
    /// User agent to send with the policy fetch.
    pub user_agent: String,
}

/// The startup policy a resolution composed, the per-layer outcome flags a host needs to decide how much to trust it, and the enterprise sign-in verdict for the credential it ran against. The flags are reported rather than folded into the settings because a host must distinguish an unmanaged device from a managed device whose policy could not be confirmed, and apply its fail-closed gates in the second case.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ManagedSettingsResolveStartupPolicyResult {
    /// Advisory minimum client version from a successful server response.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub client_update_requested: Option<String>,
    /// The server refused to deliver a policy because this client is too old to enforce it, carrying the user-facing sentence describing the refusal. No retry will help; the remedy is a client update.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub client_update_required: Option<String>,
    /// The device layer could not be read, so it is unconfirmed rather than known-absent.
    pub device_load_failed: bool,
    /// The device layer discovered from the machine, when one exists.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub device_response: Option<serde_json::Value>,
    /// The sandbox floor contributed by the device layer.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub device_sandbox_floor: Option<serde_json::Value>,
    /// The enterprise sign-in verdict for the credential this resolution ran against, evaluated over the effective policy so a server-delivered pin is honored alongside a device one. Computed on every resolution, including one that was not retained, so a host that declines to apply policy still cannot proceed past a pin.
    #[doc(hidden)]
    pub(crate) force_login_orgs: ForceLoginOrgsDecision,
    /// A managed policy is known to apply to this host, from a discovered source or a resolved document.
    pub managed_policy_present: bool,
    /// The permissions layer specifically could not be confirmed, so permission gates apply their fail-closed posture.
    pub permission_policy_undetermined: bool,
    /// Structured latest Policy Helper failure for this startup resolution, when one occurred.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub policy_helper_failure: Option<PolicyHelperFailure>,
    /// The policy helper was configured but could not be run, or its output could not be read.
    pub policy_helper_load_failed: bool,
    /// The layer produced by the platform policy helper, when one ran.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub policy_helper_response: Option<serde_json::Value>,
    /// Some layer of the policy could not be confirmed, so its absence must not be read as permission. Folds in a failed server fetch as well as an unreadable device layer.
    pub policy_undetermined: bool,
    /// The sandbox was enabled because policy could not be confirmed rather than because an administrator asked for it. Lets a host explain a sandbox the user did not configure.
    pub sandbox_enabled_by_undetermined_policy: bool,
    /// The effective sandbox floor composed across every layer, which a host must apply rather than its own default.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sandbox_floor: Option<serde_json::Value>,
    /// The server fetch failed and no cached entry stood in for it, so the server layer is unconfirmed and fail-closed gates apply.
    pub server_fetch_failed: bool,
    /// How the current runtime resolved the server layer.
    #[doc(hidden)]
    pub(crate) server_resolution: ManagedSettingsServerPolicyResolution,
    /// The account's server-delivered policy, when one was resolved.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub server_response: Option<serde_json::Value>,
    /// Telemetry describing which layers answered and how, for the host to report alongside its own startup events.
    pub telemetry: serde_json::Value,
    /// Warnings raised while resolving, in the order they occurred.
    pub warnings: Vec<String>,
}

/// The managed layers to retain as this process's startup policy, with the outcome of fetching each one.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ManagedSettingsRetainPolicyParams {
    /// Whether the device load failed. Report it honestly, for the same reason as `serverFetchFailed`.
    pub device_load_failed: bool,
    /// The device/MDM policy layer.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub device_response: Option<serde_json::Value>,
    /// Sandbox floor declared by the device layer, carried separately so a document that failed validation can still raise the floor.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub device_sandbox_floor: Option<serde_json::Value>,
    /// Whether the device sandbox intent is unreadable independently of its other settings.
    pub device_sandbox_undetermined: bool,
    /// Whether a device policy source demonstrably exists. This, not a read error, is the evidence a device is managed, so a fail-closed policy gates on it.
    pub device_source_found: bool,
    /// Whether any managed policy source exists, independently of fetch failures.
    pub managed_policy_present: bool,
    /// Whether permission policy is undetermined. Sandbox-only uncertainty does not imply this flag.
    pub permission_policy_undetermined: bool,
    /// The policy helper's response: the third managed layer that a process owning its startup policy resolves. It is retained with the other two so that every ambient read in this process, including the mutation guards and the governed marketplace allowlist that the `plugins.*` handlers derive, sees the whole policy the process fetched. A session-owning process omits it, because it resolves a helper per session against that session's working directory.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub policy_helper_response: Option<serde_json::Value>,
    /// Preserve the retained server layer during a device-only resolution.
    pub preserve_server_state: bool,
    /// Whether uncertainty alone enabled the shared sandbox floor.
    pub sandbox_enabled_by_undetermined_policy: bool,
    /// Whether the shared sandbox policy is undetermined. Diagnostic errors alone do not imply this flag.
    pub sandbox_floor_undetermined: bool,
    /// Whether the server fetch failed. Report it honestly: true keeps the already-retained server layer rather than replacing it with an absent one.
    pub server_fetch_failed: bool,
    /// Whether a required fresh server-policy confirmation failed.
    pub server_forced_refresh_failure: bool,
    /// The resolved server policy. A known source can lack a response after a forced-refresh failure.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub server_response: Option<serde_json::Value>,
    /// Whether the server policy source exists, even when its response is unavailable.
    pub server_source_found: bool,
    /// Shared server/device sandbox floor, excluding session-local helper policy.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub shared_sandbox_floor: Option<serde_json::Value>,
}

/// The authoring JSON schema for managed settings recognized by this runtime.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ManagedSettingsSchemaResult {
    /// Version of the runtime that owns this schema.
    pub runtime_version: String,
    /// JSON schema (draft 2020-12) with descriptive shared `x-composition` annotations, not a complete runtime composition contract. Model, effortLevel, and contextTier remain coupled; use `managedSettings.compose` for the runtime's effective result.
    pub schema: serde_json::Value,
}

/// A candidate managed-settings document to validate without applying it.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ManagedSettingsValidateRequest {
    /// The document to validate: a JSON object, or a string containing the document's JSON text. Preview documents are limited to 1 MiB and 64 levels of nesting, a stricter resource limit than delivered-policy parsing; violations are returned as diagnostics.
    pub content: serde_json::Value,
    /// Channel the document is meant for (`device`, `server`, or `policyHelper`). Some keys are only honored in some channels; for example, a `policyHelper` registration is ignored in policy-helper output. When omitted, no channel-specific checks run.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub layer: Option<String>,
}

/// Result of validating a managed-settings document.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ManagedSettingsValidateResult {
    /// Errors that reject the document and warnings about content the runtime ignores.
    pub diagnostics: Vec<ManagedSettingsDiagnostic>,
    /// Canonical form of the document the runtime would apply, with unrecognized keys removed. Absent when the document is invalid.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub settings: Option<serde_json::Value>,
    /// Whether the runtime would accept the document within the preview resource limits. Always equals whether `settings` is present. An invalid document is rejected as a whole.
    pub valid: bool,
}

/// Result of a user-requested shell command.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserRequestedShellCommandResult {
    /// Error output when the execution failed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Process exit code, when available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub exit_code: Option<i64>,
    /// Captured command output
    pub output: String,
    /// Whether the command completed successfully
    pub success: bool,
    /// Tool call id emitted for the shell execution
    pub tool_call_id: String,
}

/// A single user setting's effective value alongside its default, so consumers can render settings left at their default.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserSettingMetadata {
    /// The centrally-known default for this setting (null when no default is registered).
    pub default: serde_json::Value,
    /// True when the user has not set an explicit value for this setting (i.e. it is left at its default). Reflects whether the user has overridden the key, not whether the effective value happens to equal the default — a key explicitly set to a value identical to the default still reports false.
    pub is_default: bool,
    /// The effective value: the user's value if set, otherwise the default.
    pub value: serde_json::Value,
}

/// A single top-level setting subtree to validate and write.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserSettingsApplyTopKeyRequest {
    /// Canonical path of the setting the caller is editing. Currently informational only.
    pub canonical_path: Vec<String>,
    /// Records that the user chose an update channel explicitly, so the one-time staff channel migration stops reapplying. Honored only for the `autoUpdatesChannel` key.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub complete_staff_update_channel_migration: Option<bool>,
    /// True to remove the top-level key instead of writing it.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub delete: Option<bool>,
    /// Top-level setting name to write.
    pub top_key: String,
    /// Value to write for the whole top-level key. Omitted when `delete` is true. Explicit null is preserved and validated as null.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub value: Option<serde_json::Value>,
}

/// One problem found while validating a setting value.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserSettingsValidationIssue {
    /// Human-readable description of the problem.
    pub message: String,
    /// Where in the settings tree the problem sits, as object keys and array indices. The first element is the top-level key; a whole-value problem is reported as that single key.
    pub path: Vec<serde_json::Value>,
}

/// Outcome of validating and writing one top-level setting subtree.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserSettingsApplyTopKeyResult {
    /// Why the write failed, present only when `status` is `write-error`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Problems found, present only when `status` is `validation-error`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub issues: Option<Vec<UserSettingsValidationIssue>>,
    /// `ok` when the value was written, `validation-error` when it was rejected before any write, `write-error` when validation passed but the write failed.
    pub status: String,
}

/// The overrides and host context to resolve effective runtime settings from.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct UserSettingsBuildRuntimeRequest {
    /// The environment to read settings-bearing variables from. Passed explicitly rather than read from the server process so a host can resolve settings for an environment it is about to create rather than the one it runs in.
    pub environment: HashMap<String, String>,
    /// Settings that take precedence over every document and environment layer, such as the values a host derived from its own command line. Keyed by setting name, with each value carried verbatim, so a host may pass keys this runtime does not recognize. Absent when the host contributes no overrides of its own.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub overrides: Option<HashMap<String, serde_json::Value>>,
    /// The platform to apply platform-specific defaults for, in the same spelling the host's own runtime reports.
    pub platform: String,
    /// The directory the settings are being resolved for. This selects the repository-scoped document, so a host resolving settings for a directory other than its own process working directory must pass that directory here.
    pub working_directory: String,
}

/// One atomic edit of a canonical setting in settings.json.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct UserSettingsEditRequest {
    /// Nonempty canonical path segments of the setting to edit, starting with its top-level key.
    pub canonical_path: Vec<String>,
    /// Configuration directory to edit. Omitted or empty resolves the server's own home, as in `user.settings.loadForConfigDir`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub config_dir: Option<String>,
    /// Set a scalar, add or remove a string list item, or remove the setting. The handler rejects a missing or incompatible value.
    #[doc(hidden)]
    pub(crate) operation: SettingsEditOperation,
    /// Required boolean, string or number for set, required string for add-item and remove-item, and forbidden for remove. The runtime rejects all other JSON values.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub value: Option<serde_json::Value>,
}

/// Selects which settings file paths to resolve.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserSettingsGetPathsRequest {
    /// Absolute path of a repository root. Supply it to also resolve that repository's two settings file paths; omit it to resolve only the user path.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repo_root: Option<String>,
}

/// On-disk locations of the settings files this runtime reads. A path is reported whether or not the file exists.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserSettingsGetPathsResult {
    /// The repository's local, untracked settings file, or null when no repository root was supplied.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repo_local_settings: Option<String>,
    /// The repository's checked-in settings file, or null when no repository root was supplied.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repo_settings: Option<String>,
    /// The user settings file: `settings.json` under the resolved Copilot home. This is the file `user.settings.get`, `user.settings.load`, and `user.settings.set` read and write.
    pub user_settings: String,
}

/// Per-key metadata for every known user setting in settings.json, including settings left at their default. Excludes repository- and enterprise-managed overrides.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserSettingsGetResult {
    /// Every known user setting keyed by setting name, each with its effective value, default, and whether it is at the default.
    pub settings: HashMap<String, UserSettingMetadata>,
}

/// Top-level user-setting keys grouped by who may govern them.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserSettingsGovernanceKeysResult {
    /// Top-level keys a repository may override through a Claude-compatible repository config.
    pub claude_repo: Vec<String>,
    /// Top-level keys an enterprise policy may fix through managed settings.
    pub managed: Vec<String>,
    /// Top-level keys a repository may override through `.github/copilot/settings.json` or `.github/copilot/settings.local.json`.
    pub repo: Vec<String>,
}

/// The filesystem and environment facts a user-settings file is resolved from. Passed explicitly rather than read from the runtime's own process, so a host whose configuration directory differs from its process environment resolves the file it means.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserSettingsHostContext {
    /// The Copilot configuration directory, when the host set one. Takes precedence over `COPILOT_HOME` and the home directory.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub config_dir: Option<String>,
    /// The host's environment variables. `COPILOT_HOME` is read from here, as are the variables a settings hook may expand.
    pub environment: serde_json::Value,
    /// The user's home directory, used when neither a configuration directory nor `COPILOT_HOME` is set.
    pub home_directory: String,
}

/// A repository to read settings from, and the user layer they are merged over.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct UserSettingsLoadAndMergeRepoRequest {
    /// Absolute path of the repository root whose settings to read. When this is the user's home directory, the Claude-compatible configuration is skipped, so a home-directory session does not inherit a personal config as if it were repository policy.
    pub repo_root: String,
    /// The user's own settings, keyed by setting name. The repository layer is merged over this; a repository may only add to the collection settings it is allowed to influence, so a user setting it does not name survives unchanged.
    pub user_settings: HashMap<String, serde_json::Value>,
}

/// The settings a consumer should act on inside a repository, together with any diagnostic raised while reading the repository layer.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct UserSettingsLoadAndMergeRepoResult {
    /// The merged settings document, keyed by setting name. Equals the supplied user layer when the repository declares no settings.
    pub settings: HashMap<String, serde_json::Value>,
    /// Why part of the repository document was ignored, as a single human-readable string joining every diagnostic with `; `. Null when it loaded cleanly. The settings that did load are still merged, so a consumer should surface this and continue rather than treat it as a failure.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub warning: Option<String>,
}

/// Selects the configuration directory whose user settings to read.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct UserSettingsLoadForConfigDirRequest {
    /// Copilot configuration directory to read the settings document from, taking precedence over the server's own `COPILOT_HOME` and default home. Omit it, or pass an empty string, to read the directory the server resolved for itself.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub config_dir: Option<String>,
}

/// Contents of settings.json as stored, for a caller about to rewrite it.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserSettingsLoadForEditResult {
    /// Parsed contents, present only when `status` is `ok`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub settings: Option<serde_json::Value>,
    /// `ok` when the file parsed, `missing` when there is no file yet, `invalid` when the file exists but will not parse.
    pub status: String,
}

/// Selects the repository whose checked-in settings to read.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserSettingsLoadRepoRequest {
    /// Absolute path of the repository root whose settings to read. When this is the user's home directory, the Claude-compatible configuration is skipped, so a home-directory session does not inherit a personal config as if it were repository policy.
    pub repo_root: String,
}

/// A repository's settings as stored, together with any diagnostic raised while reading them.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserSettingsLoadRepoResult {
    /// The repository's settings document, keyed by setting name, with each value carried verbatim. Null when the repository declares no settings.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub settings: Option<HashMap<String, serde_json::Value>>,
    /// Why part of the document was ignored, as a single human-readable string joining every diagnostic with `; `. Null when the document loaded cleanly. The settings that did load are still reported, so a consumer should surface this and continue rather than treat it as a failure.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub warning: Option<String>,
}

/// The user's stored settings, together with any diagnostic raised while reading them.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserSettingsLoadWithWarningResult {
    /// The stored user settings document, keyed by setting name, with each value carried verbatim. Empty when the file could not be read at all.
    pub settings: HashMap<String, serde_json::Value>,
    /// Why part of the document was ignored, phrased for display to the user. Null when the document loaded cleanly. The settings that did load are still reported, so a consumer may surface this and continue rather than treat it as a failure.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub warning: Option<String>,
}

/// The managed (enterprise policy) layer and the user layer to fold together. The field names carry the layer identity: nothing about a settings object identifies which layer produced it, so a generic base/override pair would let a caller transpose them and silently resolve the governance keys in the user's favour.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct UserSettingsMergeManagedAndUserParams {
    /// The managed layer. Its governance keys win, and the per-entry `enabledPlugins` and `extraKnownMarketplaces` control maps are stamped back from it after the union. Omitted when the host is unmanaged.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub managed: Option<serde_json::Value>,
    /// The user's own settings layer. Wins on every key the managed layer does not govern. Omitted when the user has no settings yet.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub user: Option<serde_json::Value>,
}

/// The user layer folded over the managed layer.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserSettingsMergeManagedAndUserResult {
    /// The merged settings. An empty object when neither layer was supplied, so an unmanaged host with no user settings governs nothing.
    pub settings_json: serde_json::Value,
}

/// The user layer and the repository layer to fold together.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct UserSettingsMergeRepoValueParams {
    /// The repository's checked-in settings layer.
    pub repo_settings: serde_json::Value,
    /// The user's settings, already resolved against any managed layer.
    pub user_settings: serde_json::Value,
}

/// The repository layer folded into the user layer.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserSettingsMergeRepoValueResult {
    /// The merged settings, with the per-entry managed control maps re-stamped from the runtime's ambient managed policy.
    pub settings_json: serde_json::Value,
}

/// Selects the configuration directory whose settings document to clean.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct UserSettingsMigrateObsoleteOnDiskRequest {
    /// The configuration directory holding the settings document to clean. A host that scopes a session to its own Copilot home passes that directory here; omit it to clean the document under the home the server resolved for itself. The home directory and environment are deliberately not caller-supplied: they decide only the settings path root and the two hook-parsing flags, and those describe the process doing the reading rather than the caller.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub config_dir: Option<String>,
}

/// The outcome of the one-time obsolete-settings cleanup.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct UserSettingsMigrateObsoleteOnDiskResult {
    /// What the cleanup did. Only `changed` rewrote the document. The cleaned settings are not returned here: the document is written before this replies, so `user.settings.load` reads it authoritatively.
    #[doc(hidden)]
    pub(crate) status: UserSettingsMigrateObsoleteOnDiskStatus,
}

/// Selects the configuration directory to run the one-time staff log-level migration against, and supplies the caller's record of whether it has already run.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct UserSettingsMigrateStaffLogLevelOnceRequest {
    /// When the caller last recorded this migration, as an ISO-8601 timestamp. Supplying it lets the runtime answer `current` without re-running the evaluation.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cached_completed_at: Option<String>,
    /// The configuration directory, home directory and environment to resolve the settings and global-state documents from. A host that scopes a session to its own Copilot home must pass that directory here, or the migration is evaluated against the server's own home instead.
    pub context: UserSettingsHostContext,
}

/// The outcome of the one-time staff log-level migration.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct UserSettingsMigrateStaffLogLevelOnceResult {
    /// When the migration was recorded as done, as an ISO-8601 timestamp. Absent only for `ineligible`, which records nothing.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub completed_at: Option<String>,
    /// The settings document as it stands after the migration, keyed by setting name. Present only for `refreshed`, and only when the document could be re-read cleanly.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub settings: Option<serde_json::Value>,
    /// What the migration did. Only `refreshed` can have changed the persisted settings.
    pub status: StaffLogLevelMigrationStatus,
}

/// The outcome of the one-time staff update-channel migration.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct UserSettingsMigrateStaffUpdateChannelOnceResult {
    /// When the migration was recorded as done, as an ISO-8601 timestamp. Absent only for `ineligible`, which records nothing.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub completed_at: Option<String>,
    /// What the migration did. Only `migrated` means the persisted settings changed.
    pub status: StaffUpdateChannelMigrationStatus,
}

/// Settings object whose key names should be canonicalized.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserSettingsNormalizeRequest {
    /// Free-form settings object, possibly using legacy or snake_case key names.
    pub settings: serde_json::Value,
}

/// Settings object with canonical key names.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserSettingsNormalizeResult {
    /// The same settings with every legacy or snake_case key renamed to its canonical spelling.
    pub settings: serde_json::Value,
}

/// The on-disk location of the user's settings document.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserSettingsPathResult {
    /// Absolute path to the settings file this runtime reads and writes. Normally `settings.json` under the Copilot configuration directory; an extensionless legacy `settings` file is reported instead when it exists and `settings.json` does not.
    pub path: String,
}

/// The settings layer a problem was found in.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserSettingsProblemLocation {
    /// Absolute path of the file backing this layer.
    pub file_path: String,
    /// Which layer this is: `user`, `repo`, or `local`.
    pub kind: String,
    /// The caller-supplied display label for the layer.
    pub label: String,
}

/// One setting that will not take effect, and why.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserSettingsProblem {
    /// Coarse grouping for the problem, for a caller that presents problems in sections.
    pub category: String,
    /// Why the setting will not take effect. Never contains the setting's value, which may be a token or another secret.
    pub explanation: String,
    /// Stable identifier for this problem within the reported set, so a caller can track a selection across refreshes.
    pub id: String,
    /// What is wrong in machine-readable form, for a caller that wants to branch rather than display.
    pub kind: String,
    /// The offending key as written in the file, when the problem is attributable to a single key. Absent when the whole file is at fault.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub raw_key: Option<String>,
    /// The settings layer the problem was found in.
    pub source: UserSettingsProblemLocation,
}

/// One already-loaded settings layer to diagnose.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserSettingsProblemSource {
    /// The layer's settings exactly as loaded, unnormalized. Empty when the file is missing or failed to parse.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub config: Option<HashMap<String, serde_json::Value>>,
    /// Absolute path of the file backing this layer.
    pub file_path: String,
    /// True when the file itself failed to parse, so its keys could not be read at all.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub invalid: Option<bool>,
    /// Which layer this is: `user`, `repo`, or `local`.
    pub kind: String,
    /// Caller-chosen display label for the layer, echoed back on each problem so a caller can group by layer without re-deriving its own naming.
    pub label: String,
}

/// The settings layers to diagnose.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserSettingsProblemsRequest {
    /// Layers to diagnose, in the precedence order the caller applies them.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sources: Option<Vec<UserSettingsProblemSource>>,
}

/// The settings that will not take effect across the given layers.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserSettingsProblemsResult {
    /// Each setting that will not take effect, and why. Empty when every layer is healthy.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub problems: Option<Vec<UserSettingsProblem>>,
}

/// Selects the configuration directory to run the one-time staff model-selection reset against, and supplies the caller's record of whether it has already run.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct UserSettingsResetStaffModelSelectionOnceRequest {
    /// The configuration directory, home directory and environment to resolve the settings and global-state documents from. A host that scopes a session to its own Copilot home must pass that directory here, or the reset is evaluated against the server's own home instead.
    pub context: UserSettingsHostContext,
    /// Whether the signed-in account is a staff account. The reset applies only to staff, so anything other than `true` returns `didReset: false` without touching the settings document.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub staff: Option<bool>,
    /// When the caller last recorded this reset, as an ISO-8601 timestamp. A timestamp at or after the cutoff means the reset already ran and is returned unchanged.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub staff_model_reset_at: Option<String>,
}

/// The outcome of the one-time staff model-selection reset.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct UserSettingsResetStaffModelSelectionOnceResult {
    /// The cutoff this reset is gated on, as an ISO-8601 timestamp. A recorded timestamp at or after it means the reset is already done.
    pub cutoff: String,
    /// Whether the persisted model selection was cleared. Only `true` means the settings document changed and the host should adopt the returned settings.
    pub did_reset: bool,
    /// The timestamp the host should now record, as an ISO-8601 timestamp. Carries the caller's own value back unchanged when nothing was reset.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reset_at: Option<String>,
    /// The settings document as it stands after the reset, keyed by setting name. Present only when `didReset` is true.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub settings: Option<serde_json::Value>,
}

/// One hook to disable or re-enable.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct UserSettingsSetHookDisabledRequest {
    /// True adds the key to `disabledHooks`, false removes it.
    pub disabled: bool,
    /// Durable content-hash key of the hook, as `hooks.loadResources` reports it.
    pub disable_key: String,
    /// Path of the settings.json to rewrite.
    pub settings_path: String,
}

/// Outcome of rewriting the `disabledHooks` setting.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct UserSettingsSetHookDisabledResult {
    /// Why the write failed. Absent when the status is `ok`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error_message: Option<String>,
    /// `ok` when the setting was written, `invalid` when settings.json could not be parsed and was left alone, `error` when the write itself failed.
    pub status: String,
}

/// Partial user settings to write to settings.json. Each top-level key is written individually, replacing the existing value; a key whose value is null is removed.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserSettingsSetRequest {
    /// Partial user settings to write, as a free-form object keyed by setting name
    pub settings: serde_json::Value,
}

/// Selects the configuration directory whose user settings document should be inspected for unrecognized top-level keys.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct UserSettingsUnknownKeysWarningRequest {
    /// The configuration directory, home directory and environment to resolve the settings document from. A host that scopes a session to its own Copilot home must pass that directory here, or the server's own home is inspected instead.
    pub context: UserSettingsHostContext,
}

/// The unrecognized top-level keys found in the user settings document, already phrased for display.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct UserSettingsUnknownKeysWarningResult {
    /// The warning to show the user, naming the unrecognized keys and the file they were found in. Absent when every top-level key is recognized, and also when the document is missing or could not be read, because an unknown-key list taken from a document the runtime could not load would name keys that may not be there.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub warning: Option<String>,
}

/// Names the user setting to merge into, and the partial object to merge.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct UserSettingsUpdateRequest {
    /// Copilot configuration directory to write the settings document in, taking precedence over the server's own `COPILOT_HOME` and default home. Omit it, or pass an empty string, to use the directory the server resolved for itself.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub config_dir: Option<String>,
    /// Top-level user-setting key to merge into, such as `extensions`.
    pub key: String,
    /// Single-component name for the settings document at `<configDir>/settings/<subDir>.json`; path separators, `.` and `..` are rejected. Omit it, or pass an empty string, to use the main settings document.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sub_dir: Option<String>,
    /// Value to store for `key`. When both the stored value and this value are objects, their fields are shallow-merged and fields this object does not name are preserved. Otherwise this value replaces the stored value.
    pub value: serde_json::Value,
}

/// The stored value of the user setting after the merge.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct UserSettingsUpdateResult {
    /// The key's stored value after the write: the merged object when both the stored value and the supplied value are objects, otherwise the supplied value.
    pub value: serde_json::Value,
}

/// A single top-level setting subtree to validate.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserSettingsValidateRequest {
    /// True to validate a removal of this key. The current settings file is not read.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub delete: Option<bool>,
    /// Top-level setting name to validate.
    pub top_key: String,
    /// Value to validate. Omitted when `delete` is true.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub value: Option<serde_json::Value>,
}

/// Outcome of validating one top-level setting subtree.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserSettingsValidateResult {
    /// Problems found, empty when `ok` is true.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub issues: Option<Vec<UserSettingsValidationIssue>>,
    /// True when the subtree is valid.
    pub ok: bool,
}

/// The whole content to store in the user settings file.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserSettingsWriteAllRequest {
    /// The settings object to store. It replaces the file's entire content, so a key absent here is removed.
    pub settings: serde_json::Value,
}

/// The on-disk location of the user's settings document.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserSettingsGetPathResult {
    /// Absolute path to the settings file this runtime reads and writes. Normally `settings.json` under the Copilot configuration directory; an extensionless legacy `settings` file is reported instead when it exists and `settings.json` does not.
    pub path: String,
}

/// The unrecognized top-level keys found in the user settings document, already phrased for display.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct UserSettingsGetUnknownKeysWarningResult {
    /// The warning to show the user, naming the unrecognized keys and the file they were found in. Absent when every top-level key is recognized, and also when the document is missing or could not be read, because an unknown-key list taken from a document the runtime could not load would name keys that may not be there.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub warning: Option<String>,
}

/// Top-level user-setting keys grouped by who may govern them.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserSettingsListGovernanceKeysResult {
    /// Top-level keys a repository may override through a Claude-compatible repository config.
    pub claude_repo: Vec<String>,
    /// Top-level keys an enterprise policy may fix through managed settings.
    pub managed: Vec<String>,
    /// Top-level keys a repository may override through `.github/copilot/settings.json` or `.github/copilot/settings.local.json`.
    pub repo: Vec<String>,
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
pub(crate) struct UserSettingsEditResult {
    /// Changed, unchanged or invalid existing file. Rejected input, validation failures and I/O errors remain RPC errors.
    #[doc(hidden)]
    pub(crate) status: SettingsEditStatus,
}

/// The settings that will not take effect across the given layers.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserSettingsDiagnoseProblemsResult {
    /// Each setting that will not take effect, and why. Empty when every layer is healthy.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub problems: Option<Vec<UserSettingsProblem>>,
}

/// Every feature flag this runtime declares, with the shape each one was declared with.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct FeatureFlagsListMetadataResult {
    /// Declared feature flags keyed by flag name.
    #[doc(hidden)]
    pub(crate) flags: HashMap<String, FeatureFlagMetadata>,
}

/// The verdict of the `forceLoginOrgs` enterprise sign-in pin for one credential. A managed device may pin Copilot to an approved set of GitHub organizations so a developer cannot sign in with a personal — or another enterprise's — account and escape every control that keys off the signed-in identity. A host must refuse to proceed when `allowed` is false; the remaining fields explain the refusal rather than qualifying it.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ManagedSettingsResolveDeviceForceLoginOrgsGateResult {
    /// Whether the client may proceed. True whenever no pin is in force, so an unmanaged host is never blocked.
    pub allowed: bool,
    /// A stable kebab-case reason code when refused: `org-not-approved`, `unverifiable-credential`, `policy-undetermined`, `policy-invalid`, `not-signed-in`, or `membership-unknown`. Omitted when allowed. Treat an unrecognized code as a refusal rather than mapping it to an allow: a code this host has not been taught about still denied the credential.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub denial: Option<String>,
    /// Whether a pin applies at all. Distinguishes "allowed because unmanaged" from "allowed because the account is approved", which a diagnostics surface needs and which also decides whether a bring-your-own-key provider may serve inference.
    pub enforced: bool,
    /// The approved organization the account matched, when allowed under a pin.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub matched_org: Option<String>,
    /// A developer-facing explanation of the refusal. Omitted when allowed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub message: Option<String>,
    /// Error-level detail from managed-policy discovery — which file failed to load, which field failed validation. Carried on the decision rather than left to the log because a refusal can exit the process before a log sink exists, which would discard the only actionable diagnostic a misconfigured managed device produces.
    pub policy_diagnostics: Vec<String>,
    /// The approved organization logins, as configured. Enterprise configuration rather than user data, so it is safe to name in a message or a log line.
    pub required_orgs: Vec<String>,
}

/// Ordered managed permission verdicts and the supplied policy's fail-closed posture.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ManagedSettingsPermissionsEvaluateResult {
    /// Managed permission policy could not be determined; every operation is denied.
    pub fail_closed: bool,
    /// One verdict per input operation, in the same order, including duplicates.
    pub results: Vec<ManagedPermissionEvaluation>,
}

/// The stored user settings document, keyed by setting name.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type UserSettingsLoadForConfigDirResult = HashMap<String, serde_json::Value>;

/// The effective runtime settings, keyed by setting name. Keys this runtime does not recognize are preserved rather than dropped, so a caller reading a document written by a newer runtime keeps every value.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type UserSettingsBuildRuntimeResult = HashMap<String, serde_json::Value>;

/// The resolved gates, keyed by flag name.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type FeatureFlagsResolveStartupGatesResult = HashMap<String, bool>;

/// The resolved feature flags, keyed by flag name.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type FeatureFlagsResolveResult = HashMap<String, bool>;

/// The population a feature flag is declared for, before overrides.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub(crate) enum FeatureFlagAvailability {
    /// Enabled for everyone, before any override.
    #[serde(rename = "on")]
    On,
    /// Enabled only for accounts on the owning team.
    #[serde(rename = "team")]
    Team,
    /// Enabled only for staff accounts that have also opted into experimental features.
    #[serde(rename = "staff-and-experimental")]
    StaffAndExperimental,
    /// Enabled for staff accounts and for accounts that opted into experimental features.
    #[serde(rename = "staff-or-experimental")]
    StaffOrExperimental,
    /// Enabled for any staff account, GitHub or Microsoft.
    #[serde(rename = "staff")]
    Staff,
    /// Enabled for GitHub staff accounts only.
    #[serde(rename = "staff-github")]
    StaffGitHub,
    /// Enabled for Microsoft staff accounts only.
    #[serde(rename = "staff-microsoft")]
    StaffMicrosoft,
    /// Enabled only for accounts that opted into experimental features.
    #[serde(rename = "experimental")]
    Experimental,
    /// Disabled for everyone, before any override.
    #[serde(rename = "off")]
    Off,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Administrator-configured Fusion preference, independent of execution support.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ManagedFusionTier {
    /// Use Auto Balance to balance capability, speed, and cost.
    #[serde(rename = "standard")]
    Standard,
    /// Use HydraFusion, falling back to Auto Intelligence when unavailable.
    #[serde(rename = "high")]
    High,
    /// Use HydraFusion Max to prioritize quality.
    #[serde(rename = "max")]
    Max,
    /// Leave tier selection unmanaged without falling back to legacy Auto policy.
    #[serde(rename = "unmanaged")]
    Unmanaged,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Operation to evaluate. Currently only url is supported.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ManagedPermissionOperationKind {
    #[serde(rename = "url")]
    #[default]
    Url,
}

/// Managed-policy verdict only: deny blocks the URL, ask requires approval, allow approves under managed policy, and unmanaged leaves the normal consumer permission flow in effect. No verdict bypasses other security controls.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ManagedPermissionVerdict {
    /// Managed policy prohibits the operation; do not offer approval.
    #[serde(rename = "deny")]
    Deny,
    /// Managed policy requires the consumer to obtain approval.
    #[serde(rename = "ask")]
    Ask,
    /// Managed policy allows the operation, subject to other security controls.
    #[serde(rename = "allow")]
    Allow,
    /// Managed policy does not decide the operation; use the consumer's normal permission flow.
    #[serde(rename = "unmanaged")]
    Unmanaged,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Which layer disabled bypass-permissions mode. The layer matters because only a server-delivered disable is pinned to an account and can later be lifted by signing in elsewhere.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub(crate) enum ManagedSettingsBypassDisableSource {
    /// The user turned bypass mode off themselves.
    #[serde(rename = "user")]
    User,
    /// The account's server-delivered org policy disabled it. Pinned to the account named by `currentManagedIdentity`.
    #[serde(rename = "managed")]
    Managed,
    /// The device/MDM layer disabled it. Outranks the account, so no server answer lifts it.
    #[serde(rename = "mdm")]
    Mdm,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// What the managed-policy resolution said about bypass-permissions mode. `unknown` is a third answer rather than a missing one, and the caller must send it explicitly when it has no verdict.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub(crate) enum ManagedSettingsBypassPolicyResolution {
    /// The policy resolved and does not disable bypass mode.
    #[serde(rename = "allowed")]
    Allowed,
    /// The policy resolved and disables bypass mode.
    #[serde(rename = "disabled")]
    Disabled,
    /// No verdict: the fetch failed, or the service declared a policy this build may not enforce. Whatever is in force is preserved, because reverting to permissive on an unreadable policy would widen access.
    #[serde(rename = "unknown")]
    UnknownValue,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// A channel accepted by managedSettings.compose.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ManagedSettingsChannel {
    /// Device policy, the strongest channel.
    #[serde(rename = "device")]
    Device,
    /// Account or organization policy.
    #[serde(rename = "server")]
    Server,
    /// Session-local helper output, the weakest channel.
    #[serde(rename = "policyHelper")]
    PolicyHelper,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Severity of a managed-settings validation finding.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ManagedSettingsDiagnosticSeverity {
    /// The runtime rejects the document.
    #[serde(rename = "error")]
    Error,
    /// The runtime accepts the document but ignores the flagged content.
    #[serde(rename = "warning")]
    Warning,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// The operation that requires a policy-helper response.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub(crate) enum ManagedSettingsPolicyHelperReason {
    /// Resolve policy for session startup.
    #[serde(rename = "sessionStart")]
    SessionStart,
    /// Resolve policy before plugin activation.
    #[serde(rename = "pluginActivation")]
    PluginActivation,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// How an ingestion may use the on-disk server-policy cache.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub(crate) enum ManagedSettingsServerPolicyCacheMode {
    /// Serve a recent cache entry before a live fetch.
    #[serde(rename = "preferFresh")]
    PreferFresh,
    /// Read cached policy without a network request.
    #[serde(rename = "cacheOnly")]
    CacheOnly,
    /// Serve a cached entry only as a fallback after a failed live fetch.
    #[serde(rename = "fallbackOnly")]
    FallbackOnly,
    /// Ignore the cache entirely and rely on the live fetch alone.
    #[serde(rename = "disabled")]
    Disabled,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Severity of a diagnostic emitted while ingesting managed settings.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub(crate) enum ManagedSettingsLogLevel {
    /// Detail useful only when diagnosing an ingestion.
    #[serde(rename = "debug")]
    Debug,
    /// Ordinary progress worth surfacing under a verbose flag.
    #[serde(rename = "info")]
    Info,
    /// A condition the operator should see but which did not stop the ingestion.
    #[serde(rename = "warning")]
    Warning,
    /// A failure that changed the outcome of the ingestion.
    #[serde(rename = "error")]
    Error,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Which channel registered the policy helper that produced a helper layer.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub(crate) enum ManagedSettingsPolicyHelperAuthority {
    /// The helper was registered by the device (MDM or managed file) layer.
    #[serde(rename = "device")]
    Device,
    /// The helper was registered by the account's server-delivered policy.
    #[serde(rename = "server")]
    Server,
    /// The helper was registered by the process-local environment fallback.
    #[serde(rename = "environment")]
    Environment,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// How the server layer of an ingestion was resolved.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub(crate) enum ManagedSettingsServerPolicyResolution {
    /// A live fetch succeeded.
    #[serde(rename = "live")]
    Live,
    /// A recent cached entry was served without a live fetch.
    #[serde(rename = "freshCache")]
    FreshCache,
    /// No cached policy or confirmed no-policy result exists.
    #[serde(rename = "cacheMiss")]
    CacheMiss,
    /// A live fetch failed and a cached entry was served in its place.
    #[serde(rename = "fallbackCache")]
    FallbackCache,
    /// No policy could be resolved and the host proceeds unmanaged.
    #[serde(rename = "failOpen")]
    FailOpen,
    /// No policy could be resolved and the host applies the fail-closed gates.
    #[serde(rename = "failClosed")]
    FailClosed,
    /// No fetch was attempted because no usable token was available.
    #[serde(rename = "skippedNoToken")]
    SkippedNoToken,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// What the one-time staff log-level migration did.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum StaffLogLevelMigrationStatus {
    /// The account is not a staff account, so the migration does not apply and nothing was recorded.
    #[serde(rename = "ineligible")]
    Ineligible,
    /// The migration already ran; the stored completion timestamp is returned unchanged.
    #[serde(rename = "current")]
    Current,
    /// The migration ran now. The persisted settings may have changed, so the host should adopt the returned settings when they are present.
    #[serde(rename = "refreshed")]
    Refreshed,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// What the one-time staff update-channel migration did.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum StaffUpdateChannelMigrationStatus {
    /// The account is not a staff account, so the migration does not apply and nothing was recorded.
    #[serde(rename = "ineligible")]
    Ineligible,
    /// The migration already ran within the cutoff window; the stored completion timestamp is returned unchanged.
    #[serde(rename = "current")]
    Current,
    /// The persisted update channel was changed to stable. Only this outcome requires the host to reload its configuration.
    #[serde(rename = "migrated")]
    Migrated,
    /// The persisted channel was already stable, so nothing changed, but the migration is now recorded as done.
    #[serde(rename = "completed")]
    Completed,
    /// The account is on the experimental channel, which the migration deliberately leaves alone, and the migration is recorded as done.
    #[serde(rename = "excluded-experimental")]
    ExcludedExperimental,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// What the one-time obsolete-settings cleanup did to the document.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub(crate) enum UserSettingsMigrateObsoleteOnDiskStatus {
    /// No settings document exists, so there was nothing to clean.
    #[serde(rename = "missing")]
    Missing,
    /// The document could not be parsed, so it was left untouched rather than rewritten from a partial read.
    #[serde(rename = "invalid")]
    Invalid,
    /// The document parsed and held nothing obsolete, so nothing was written.
    #[serde(rename = "unchanged")]
    Unchanged,
    /// Obsolete keys were removed and the document was rewritten.
    #[serde(rename = "changed")]
    Changed,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

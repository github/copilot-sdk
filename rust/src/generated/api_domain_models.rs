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

/// Default-tier text and image token prices reported for an image model, in AI credits per billing batch. Missing rates remain absent; these are not per-image quotes or actual billed usage.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ImageToolModelPricing {
    /// Number of tokens per billing batch.
    pub batch_size: i64,
    /// Distinct AI credits per batch of image-input tokens, when reported, including zero.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub image_input_price: Option<f64>,
    /// Distinct AI credits per batch of image-output tokens, when reported, including zero.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub image_output_price: Option<f64>,
    /// AI credits per batch of text-input tokens, including a reported zero.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub text_input_price: Option<f64>,
    /// AI credits per batch of text-output tokens, not generated image tokens.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub text_output_price: Option<f64>,
}

/// The runtime-selected image model and its account availability. Availability is independent of per-session image-generation enablement and does not override session policy or permissions.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ImageToolModel {
    /// Whether the resolved account can use this image model. This is not the session's enabled setting.
    pub availability: ImageToolModelAvailability,
    /// Image model identifier selected by the runtime.
    pub id: String,
    /// Whether this is the model the runtime currently uses for its native image tool.
    pub is_default: bool,
    /// Human-readable model name, supplied by the runtime when unavailable from the catalog.
    pub name: String,
    /// Catalog-reported token prices. Absent means prices were not reported.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub pricing: Option<ImageToolModelPricing>,
    /// Image operations supported by the model's available versioned endpoints.
    pub supported_operations: Vec<ImageToolModelOperation>,
}

/// Managed, repository, and CLI model overrides to overlay onto the session at startup.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelApplyStartupOverlayRequest {
    /// Effective default Auto routing preference from user and managed settings. Applies only to fresh sessions and never replaces a per-session selection.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auto_tier: Option<AutoTier>,
    /// Model explicitly selected by the CLI, when provided.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cli_model: Option<String>,
    /// Whether the overlay is being applied while resuming a deferred session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub deferred_resume: Option<bool>,
    /// Model required by device-managed policy, when configured.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub device_managed_model: Option<String>,
    /// Context tier paired with the effective organization-managed model. Applies only when that concrete managed model is selected; it is ignored for Auto and for CLI, resume, or user overrides.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub managed_context_tier: Option<String>,
    /// Reasoning effort paired with the effective organization-managed model. Applies only when that concrete managed model is selected; it is ignored for Auto and for CLI, resume, or user overrides.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub managed_reasoning_effort: Option<String>,
    /// Startup default model from the enterprise policy helper, when configured. Weakest of the managed sources: it applies only when neither device nor server policy names a model, and an explicit user selection still wins.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub policy_helper_model: Option<String>,
    /// Auto routing preference selected by repository settings, when configured. Applied only when the overlay selects the Auto model; beside a concrete model it stays dormant.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repo_auto_tier: Option<String>,
    /// Context tier selected by repository settings, when configured.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repo_context_tier: Option<String>,
    /// Model selected by repository settings, when configured.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repo_model: Option<String>,
    /// Provider owning the repository-selected model. Omitted repository provider identity retains legacy bare-model behavior.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repo_model_provider_id: Option<String>,
    /// Reasoning effort selected by repository settings, when configured.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repo_reasoning_effort: Option<String>,
    /// Model required by server-managed policy, when configured.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub server_managed_model: Option<String>,
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
pub(crate) struct ModelClearStartupSeedRequest {
    pub expected_model: String,
    /// Provider captured with the startup seed; omission matches only an unqualified seed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub expected_provider_id: Option<String>,
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
pub(crate) struct ModelClearStartupSeedResult {
    pub cleared: bool,
}

/// Host presentation state the reasoning-summaries policy depends on.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelGetReasoningSummariesRequest {
    /// The host's persisted `showReasoning` user setting, omitted when unset.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub persisted_show_reasoning: Option<bool>,
    /// Whether the host is currently showing reasoning summaries. Defaults to false.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub summaries_visible: Option<bool>,
}

/// Reasoning-summaries presentation policy for the session's current model.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelGetReasoningSummariesResult {
    /// Whether the supplied `persistedShowReasoning` setting only restates the model default. This reports a redundancy, not a standing instruction to delete the setting: it stays true for as long as the setting and the model default agree, including for a user who later chose that value deliberately. It exists for a one-time migration of a legacy setting written before the model default made it redundant, so a host that acts on it owns the cleanup and must gate it behind its own persisted `done` marker, delete only a setting whose value still matches the one it read, and never repeat the deletion on a later launch. Under an off-by-default reasoning-summaries experiment an explicit `showReasoning: true` opt-in also reads as redundant on a summary-producing model, so an ungated host would keep deleting that opt-in and hide summaries again on every launch.
    pub clear_redundant_setting: bool,
    /// Whether the session's reasoning-summaries experiment assignment has settled. False means this read does not wait for a pending assignment and reports the experiment-derived fields conservatively: `showHiddenHint` is false and `visibility` falls back to `persistedShowReasoning`, or is omitted when none was supplied. A host that wants the assigned answer re-reads until this is true.
    pub experiment_settled: bool,
    /// Whether the selected model shows reasoning summaries by default.
    pub model_default_shows_summaries: bool,
    /// False until the session has a model catalog and a selected model. This read never resolves the catalog itself, and the runtime emits no readiness notification, so repeating this call alone leaves `ready` false indefinitely on a session that has neither listed models nor run a turn. `session.model.list` is the prerequisite and the signal: it resolves and caches the catalog for the session, so a successful return means the next read is ready. The exception is a session that resolves models without a Copilot catalog, that is a BYOK registry or an alternate provider: `session.model.list` returns an empty list without caching a catalog, so `ready` stays false and this policy is unavailable there. While false, every model-policy field is false and `visibility` is omitted; `experimentSettled` is independent of readiness and still reports the assignment state.
    pub ready: bool,
    /// Whether turns on this session produce reasoning summaries, honouring an explicit session reasoning-summary override.
    pub session_shows_summaries: bool,
    /// Whether to show the hint that reasoning summaries are hidden. Always false while `experimentSettled` is false.
    pub show_hidden_hint: bool,
    /// Whether the selected model emits reasoning headers.
    pub show_reasoning_headers: bool,
    /// Default reasoning visibility, and only meaningful once `ready` is true: false when the session produces no summaries, else the persisted setting, else the experiment's default. While `ready` is true it is omitted only when all three are unavailable, that is when the session produces summaries, no `persistedShowReasoning` was supplied, and `experimentSettled` is false. While `ready` is false there is no model policy to report and this field is always omitted, so an absent value alone does not imply a pending experiment.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub visibility: Option<bool>,
}

/// List of Copilot models available to the resolved user, including capabilities and billing metadata.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelList {
    /// Ordered Auto routing preferences discovered for the resolved account. Present only
    /// when CAPI returned Auto metadata; same shape and semantics as `SessionModelList.auto`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auto: Option<AutoTierMetadata>,
    /// The runtime's default image model, separate from conversation models, with its availability for the resolved account. The default is reported even when unavailable; clients choose whether to display it. These entries must not be selected as chat models. Absent on runtimes that do not expose this catalog.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub image_models: Option<Vec<ImageToolModel>>,
    /// List of available models with full metadata
    pub models: Vec<Model>,
}

/// Optional listing options.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelListRequest {
    /// If true, bypasses the per-session model list cache and re-fetches from CAPI.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub skip_cache: Option<bool>,
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
pub struct ModelPickerPersistenceRequest {
    /// Whether context tier was explicitly selected and should be persisted.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context_tier_explicit: Option<bool>,
    /// Whether reasoning effort was explicitly selected and should be persisted.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_effort_explicit: Option<bool>,
    /// Filesystem and environment context used to resolve settings persistence.
    pub settings_context: ModelPickerSettingsContext,
}

/// Normalized model-provider adapter definitions available to the session, not discovered instances.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelProviderAdapterCatalog {
    /// Available provider adapters ordered by adapterId.
    pub providers: Vec<ModelProviderAdapterDescriptor>,
}

/// Provider configuration prepared from a discovered model. Preparing a plan changes nothing: it neither registers the model with the session nor writes durable configuration. To apply it, pass `provider` and `model` to `session.provider.add`, omitting whichever the dispositions report as already configured.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelProviderConfigurationPlan {
    /// Model definition prepared from the discovered model. Capability fields the provider did not report stay omitted rather than being asserted false.
    pub model: ProviderModelConfig,
    /// Whether `model` still needs to be registered. When `alreadyConfigured`, `selectionId` is already registered and the caller can select it without adding anything.
    pub model_disposition: ModelProviderConfigurationDisposition,
    /// Provider connection prepared from the instance's inference metadata. Carries no credential; supply one if the endpoint requires it.
    pub provider: NamedProviderConfig,
    /// Whether `provider` still needs to be registered. When `alreadyConfigured`, a provider with the same endpoint is already registered and `provider` restates it under its existing name; adding it again is rejected as a duplicate.
    pub provider_disposition: ModelProviderConfigurationDisposition,
    /// Provider-qualified selection id (`provider/id`) to pass to `switchTo` once the plan is applied.
    pub selection_id: String,
    /// Non-fatal warnings carried over from the discovered model, such as capabilities the provider did not report.
    pub warnings: Vec<ModelProviderWarning>,
}

/// Provider discovery parameters.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelProviderDiscoverRequest {
    /// Opaque adapter identity returned by `session.providers.getCatalog`.
    pub adapter_id: String,
    /// Provider-specific JSON input. Omission or null selects adapter defaults unless requiresInput is true. Non-null input is validated against the advertised Draft 7 schema when present; otherwise validation belongs to the adapter.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub input: Option<serde_json::Value>,
}

/// Provider instances found by a discovery operation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelProviderDiscoverResult {
    /// Discovered provider instances. Empty when passive default discovery finds no reachable provider.
    pub instances: Vec<ModelProviderInstance>,
    /// Typed operation outcome. Passive discovery can return `absent` with an empty instance list.
    pub outcome: ModelProviderOperationOutcome,
}

/// Provider status request parameters.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelProviderGetStatusRequest {
    /// Provider instance reference returned by discovery.
    pub instance: ModelProviderInstanceReference,
}

/// Provider model inventory request parameters.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelProviderModelsListRequest {
    /// Provider instance reference returned by discovery.
    pub instance: ModelProviderInstanceReference,
}

/// A discovered instance and one of its models to translate into provider configuration. Pass back the instance and model as returned by `session.providers.discover` and `session.providers.models.list`.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelProviderPrepareConfigurationRequest {
    /// The discovered instance that serves the model.
    pub instance: ModelProviderInstance,
    /// The discovered model to configure.
    pub model: DiscoveredModel,
}

/// Current health information for a provider instance.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelProviderStatus {
    /// Normalized provider instance.
    pub instance: ModelProviderInstance,
    /// Typed operation outcome.
    pub outcome: ModelProviderOperationOutcome,
    /// Open provider status value, such as `healthy`, `unreachable`, or `notInstalled`.
    pub status: String,
    /// Provider-reported version.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub version: Option<String>,
}

/// Host-supplied exact model selection IDs to allow for this running session. CAPI IDs are intersected with repository `.github/allowed_models.txt` policy; provider-qualified IDs remain exempt from repository-only policy but are restricted by this host list. Omit or pass null to clear the host restriction; an explicit empty or disjoint list is rejected. Validation and pre-selection fallback failures preserve the previous restriction. Failures after a fallback selection commits retain the new restriction and selected model; callers should inspect current session state after such an error.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelSetAllowedModelsRequest {
    /// Exact model IDs to permit, or null to clear the host restriction.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub allowed_models: Option<Vec<String>>,
}

/// The applied host allowlist and effective session model policy after intersection.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelSetAllowedModelsResult {
    /// Normalized host allowlist. Omitted when the host restriction was cleared, or when a relay client does not return the host policy.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub allowed_models: Option<Vec<String>>,
    /// Effective exact IDs or repository policy patterns after applying the host restriction. Omitted by relay clients that do not return the host policy.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub effective_allowed_models: Option<Vec<String>>,
    /// Effective deterministic fallback model, when the policy defines one.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub fallback_model: Option<String>,
    /// Selected session model after reconciling a now-disallowed concrete selection.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_id: Option<String>,
}

/// Reasoning effort level to apply to the currently selected model.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelSetReasoningEffortRequest {
    /// Reasoning effort level to apply to the currently selected model. The host is responsible for validating the value against the model's supported levels before calling.
    pub reasoning_effort: String,
}

/// Update the session's reasoning effort without changing the selected model. Use `switchTo` instead when you also need to change the model. The runtime stores the effort on the session and applies it to subsequent turns.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelSetReasoningEffortResult {
    /// Reasoning effort level recorded on the session after the update
    pub reasoning_effort: String,
}

/// Optional opaque account selection, compatibility GitHub token, or streamer-mode masking used to list models.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelsListRequest {
    /// GitHub token accepted for compatibility with existing SDK clients. When provided, resolves this token instead of using the current account.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub git_hub_token: Option<String>,
    /// Opaque account identifier returned by `account.getAllUsers`. When omitted, the current account is used.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub selection_id: Option<String>,
    /// When true, replaces the display names of preview and hidden models with `Hidden Model N`, and masks their promotion and data-retention text. Model ids are unchanged.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub streamer_mode: Option<bool>,
}

/// An Auto preference request for the session. This updates Auto configuration only; it does not change the selected model to `auto`.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelSwitchAutoTierRequest {
    /// Auto preference to activate when a future user turn using the `auto` model safely mints a replacement model and token pair. Pass null to return to provider-default Auto routing.
    pub auto_tier: Option<AutoTier>,
    /// Origin to record on the effective `session.model_change` event. Defaults to `sdk` when omitted.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source: Option<ModelChangeSource>,
}

/// Immediate acknowledgement and Auto preference snapshot after a switch request. This result never implies that a pending preference committed.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelSwitchAutoTierResult {
    /// Auto preference currently claimed by an in-progress activation. Null means the activation is returning to provider-default routing.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub activating_auto_tier: Option<AutoTier>,
    /// Auto preference currently committed for the session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub effective_auto_tier: Option<AutoTier>,
    /// Latest unclaimed Auto preference waiting for a future user turn.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub pending_auto_tier: Option<AutoTier>,
    /// Immediate request status. `pending` means accepted but not committed.
    pub status: ModelSwitchAutoTierStatus,
    /// Earlier unclaimed preference replaced by this request. This can be present with either status, including when selecting the effective preference cancels pending work.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub superseded_auto_tier: Option<AutoTier>,
}

/// Target model identifier and optional reasoning effort, summary, capability overrides, and context tier.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelSwitchToRequest {
    /// Optional Auto routing preference to stage atomically with selecting `auto`. Pass null to return to provider-default Auto routing. This field is rejected when `modelId` is not `auto`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auto_tier: Option<AutoTier>,
    /// Explicit response to a model-switch compaction preflight. Omit to request a confirmation projection when compaction is necessary.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub compaction_decision: Option<String>,
    /// Explicit context tier for the selected model. `"default"` / `"long_context"` apply the requested tier; omit this field to use normal model behavior with no explicit tier.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context_tier: Option<ContextTier>,
    /// When true, defer this switch (enqueue it) if another model change is already queued, even when no turn is active — so it drains last (FIFO) and wins over the already-queued change. Intended for genuine user-initiated model selections; internal restore/reapply switches omit it and apply immediately when no turn is active. When no other model change is queued this has no effect (a switch still applies immediately unless a turn is active).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub defer_if_model_change_queued: Option<bool>,
    /// Override individual model capabilities resolved by the runtime
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_capabilities: Option<ModelCapabilitiesOverride>,
    /// Settings scope used when persisting the selected model.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_change_scope: Option<String>,
    /// Model id to switch to, as returned by `list`. Include providerId to select an exact catalog entry when providers share the id. Without providerId, a bare id retains incumbent selection behavior; a registry-qualified id (`provider/id`, e.g. `acme/claude-sonnet`) targets a BYOK model.
    pub model_id: String,
    /// Optional settings context and explicit-override flags used to persist a picker selection.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub picker_persistence: Option<ModelPickerPersistenceRequest>,
    /// Reasoning effort level to use for the model. CAPI values are model-defined and validated against the selected model; BYOK providers may define additional values. "none" disables reasoning. Pass null to clear any session effort override and fall back to the model's default. When omitted, the session's current effort is kept.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_effort: Option<String>,
    /// Reasoning summary mode to request for supported model clients
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_summary: Option<ReasoningSummary>,
    /// Optional repository settings scope to persist after the switch commits.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repo_scope: Option<String>,
    /// Require the target to be currently available and enabled before applying the switch.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub require_available: Option<bool>,
    /// When true, evaluate context-window compaction policy before applying the switch, including effort-only and unchanged selections while idle. An effort-only change applies immediately without compaction when a turn is active, including when the turn starts during preflight; an explicit `compactionDecision: "cancel"` still cancels the change. Other switches retain preflight and may be deferred until the active turn ends.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub run_compaction_preflight: Option<bool>,
    /// Origin to record on the effective `session.model_change` event for trusted in-process calls. Transport SDK calls are always recorded as `sdk`, regardless of this value.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source: Option<ModelChangeSource>,
    /// Output verbosity level to request for supported models
    #[serde(skip_serializing_if = "Option::is_none")]
    pub verbosity: Option<Verbosity>,
}

/// Extensible [`ModelSwitchToRequest`], including inputs added after it was published.
///
/// Required inputs are [`ModelSwitchToOptions::new`] arguments; optional inputs have fluent setters.
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
pub struct ModelSwitchToOptions {
    #[serde(flatten)]
    legacy: ModelSwitchToRequest,
    #[serde(skip_serializing_if = "Option::is_none")]
    provider_id: Option<String>,
}

impl ModelSwitchToOptions {
    /// Creates options with the required inputs.
    pub fn new(model_id: impl Into<String>) -> Self {
        Self {
            legacy: ModelSwitchToRequest {
                model_id: model_id.into(),
                auto_tier: None,
                reasoning_effort: None,
                reasoning_summary: None,
                verbosity: None,
                model_capabilities: None,
                context_tier: None,
                source: None,
                defer_if_model_change_queued: None,
                compaction_decision: None,
                run_compaction_preflight: None,
                repo_scope: None,
                model_change_scope: None,
                require_available: None,
                picker_persistence: None,
            },
            provider_id: None,
        }
    }

    /// Optional Auto routing preference to stage atomically with selecting `auto`. Pass null to return to provider-default Auto routing. This field is rejected when `modelId` is not `auto`.
    pub fn auto_tier(mut self, value: AutoTier) -> Self {
        self.legacy.auto_tier = Some(value);
        self
    }

    /// Reasoning effort level to use for the model. CAPI values are model-defined and validated against the selected model; BYOK providers may define additional values. "none" disables reasoning. Pass null to clear any session effort override and fall back to the model's default. When omitted, the session's current effort is kept.
    pub fn reasoning_effort(mut self, value: impl Into<String>) -> Self {
        self.legacy.reasoning_effort = Some(value.into());
        self
    }

    /// Reasoning summary mode to request for supported model clients
    pub fn reasoning_summary(mut self, value: ReasoningSummary) -> Self {
        self.legacy.reasoning_summary = Some(value);
        self
    }

    /// Output verbosity level to request for supported models
    pub fn verbosity(mut self, value: Verbosity) -> Self {
        self.legacy.verbosity = Some(value);
        self
    }

    /// Override individual model capabilities resolved by the runtime
    pub fn model_capabilities(mut self, value: ModelCapabilitiesOverride) -> Self {
        self.legacy.model_capabilities = Some(value);
        self
    }

    /// Explicit context tier for the selected model. `"default"` / `"long_context"` apply the requested tier; omit this field to use normal model behavior with no explicit tier.
    pub fn context_tier(mut self, value: ContextTier) -> Self {
        self.legacy.context_tier = Some(value);
        self
    }

    /// Origin to record on the effective `session.model_change` event for trusted in-process calls. Transport SDK calls are always recorded as `sdk`, regardless of this value.
    pub fn source(mut self, value: ModelChangeSource) -> Self {
        self.legacy.source = Some(value);
        self
    }

    /// When true, defer this switch (enqueue it) if another model change is already queued, even when no turn is active — so it drains last (FIFO) and wins over the already-queued change. Intended for genuine user-initiated model selections; internal restore/reapply switches omit it and apply immediately when no turn is active. When no other model change is queued this has no effect (a switch still applies immediately unless a turn is active).
    pub fn defer_if_model_change_queued(mut self, value: bool) -> Self {
        self.legacy.defer_if_model_change_queued = Some(value);
        self
    }

    /// Explicit response to a model-switch compaction preflight. Omit to request a confirmation projection when compaction is necessary.
    pub fn compaction_decision(mut self, value: impl Into<String>) -> Self {
        self.legacy.compaction_decision = Some(value.into());
        self
    }

    /// When true, evaluate context-window compaction policy before applying the switch, including effort-only and unchanged selections while idle. An effort-only change applies immediately without compaction when a turn is active, including when the turn starts during preflight; an explicit `compactionDecision: "cancel"` still cancels the change. Other switches retain preflight and may be deferred until the active turn ends.
    pub fn run_compaction_preflight(mut self, value: bool) -> Self {
        self.legacy.run_compaction_preflight = Some(value);
        self
    }

    /// Optional repository settings scope to persist after the switch commits.
    pub fn repo_scope(mut self, value: impl Into<String>) -> Self {
        self.legacy.repo_scope = Some(value.into());
        self
    }

    /// Settings scope used when persisting the selected model.
    pub fn model_change_scope(mut self, value: impl Into<String>) -> Self {
        self.legacy.model_change_scope = Some(value.into());
        self
    }

    /// Require the target to be currently available and enabled before applying the switch.
    pub fn require_available(mut self, value: bool) -> Self {
        self.legacy.require_available = Some(value);
        self
    }

    /// Optional settings context and explicit-override flags used to persist a picker selection.
    pub fn picker_persistence(mut self, value: ModelPickerPersistenceRequest) -> Self {
        self.legacy.picker_persistence = Some(value);
        self
    }

    /// Provider id from the selected list entry's provider reference. Selects this exact provider/model pair; an unavailable pair fails rather than using another provider. Omit for deterministic legacy bare-model selection.
    pub fn provider_id(mut self, value: impl Into<String>) -> Self {
        self.provider_id = Some(value.into());
        self
    }
}

/// The model identifier active on the session after the switch.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelSwitchToResult {
    /// Compaction confirmation projection when status is confirmation_required
    #[serde(skip_serializing_if = "Option::is_none")]
    pub confirmation: Option<ModelSwitchConfirmation>,
    /// True when the switch was deferred (enqueued as a cancellable `/model` command) because a turn was active or another model change was already queued, rather than applied immediately. When true, the session's live model is unchanged until the queued change drains.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub deferred: Option<bool>,
    /// Deprecation warnings associated with the selected model or options.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub deprecation_warnings: Option<Vec<String>>,
    /// User-facing outcome message for the model switch.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub message: Option<String>,
    /// Currently active model identifier after the switch
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_id: Option<String>,
    /// Authoritative model and Auto preference state after an immediate switch. For deferred switches this remains the current state until the queued change drains.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_state: Option<CurrentModel>,
    /// Persistence failure encountered after applying the model switch.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub persistence_error: Option<String>,
    /// Stable queue item identifier when this request was enqueued. Remains present if the item drains before the response is returned.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub queue_id: Option<String>,
    /// Lifecycle result for the requested switch
    #[serde(skip_serializing_if = "Option::is_none")]
    pub status: Option<String>,
    /// User-facing warning produced while applying the model switch.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub warning: Option<String>,
}

/// The unit one model's published token prices are quoted in.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelTokenPriceUnit {
    /// Model ID the unit applies to, matching the id of a models.list entry.
    pub model_id: String,
    /// Unit this model's token prices are quoted in.
    pub unit: TokenPriceUnit,
}

/// Price units for every catalog model that publishes token prices.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelTokenPriceUnitList {
    /// One entry per model that publishes token prices, in catalog order. A model with no published prices is omitted.
    pub units: Vec<ModelTokenPriceUnit>,
}

/// BYOK providers and/or models to add to the session's registry at runtime. Both fields are optional; provide providers, models, or both.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ProviderAddRequest {
    /// BYOK model definitions to register. Each must reference a provider that is already registered or included in this same call. Selection ids (`provider/id`) must be unique across the registry.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub models: Option<Vec<ProviderModelConfig>>,
    /// Named BYOK provider connections to register, additive to any providers already in the registry. Each name must be unique across the registry and must not contain '/'.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub providers: Option<Vec<NamedProviderConfig>>,
}

/// The selectable model entries synthesized for the models added by this call.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ProviderAddResult {
    /// Synthesized selectable model entries for the newly added BYOK models, each under its provider-qualified selection id (`provider/id`). Empty when only providers were added.
    pub models: Vec<serde_json::Value>,
}

/// A snapshot of the provider endpoint the session is currently configured to talk to.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ProviderEndpoint {
    /// A credential the caller should use with this endpoint. Omitted only when the endpoint accepts unauthenticated requests.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub api_key: Option<String>,
    /// Base URL to pass to the LLM client library.
    pub base_url: String,
    /// HTTP headers the caller must include on every outbound request.
    pub headers: HashMap<String, String>,
    /// Short-lived, rotating credential the caller must send on every request, in addition to `apiKey` if one is present. Omitted when the endpoint does not require one.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_token: Option<ProviderSessionToken>,
    /// Transport to be used for provider requests.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub transport: Option<ProviderEndpointTransport>,
    /// Provider family. Matches the `type` field of a BYOK provider config.
    pub r#type: ProviderEndpointType,
    /// Wire API to be used, when required for the provider type.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub wire_api: Option<ProviderEndpointWireApi>,
}

/// Optional model identifier to scope the endpoint snapshot to.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ProviderGetEndpointRequest {
    /// Model identifier the caller intends to use against the returned endpoint. Used to pick the correct wire shape. Omit to use whichever model the session is currently using.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_id: Option<String>,
}

/// Authoritative BYOK provider and model registry snapshot to apply atomically to the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ProviderSyncRequest {
    /// BYOK model definition snapshot. Models absent from this list are removed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub models: Option<Vec<ProviderModelConfig>>,
    /// Named BYOK provider connection snapshot. Providers absent from this list are removed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub providers: Option<Vec<NamedProviderConfig>>,
}

/// The selectable model entries and selection ids synthesized for the synchronized BYOK models.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ProviderSyncResult {
    /// True when synchronization withdrew the selected host-managed model, leaving the session with no explicit selection, so ordinary model resolution picks the session default. Synchronization never promotes a surviving host model in its place: publishing a model offers it, and the choice of which model to use stays with the user.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_deselected: Option<bool>,
    /// Synthesized selectable model entries for the synchronized BYOK models.
    pub models: Vec<serde_json::Value>,
    /// Provider-qualified model selection ids present after synchronization.
    pub selection_ids: Vec<String>,
}

/// Asks the SDK client to acquire a bearer token for a BYOK provider whose config set `hasBearerTokenProvider: true`. Issued by the runtime before each outbound model request; the runtime does no caching, so this is sent once per request.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ProviderTokenAcquireRequest {
    /// Target session identifier
    pub session_id: SessionId,
    /// Name of the BYOK provider needing a token. For the legacy whole-session provider this is the implicit provider name; for named providers it is the configured provider name.
    pub provider_name: String,
}

/// A bearer token supplied by the SDK client for a BYOK provider. The runtime sets it as `Authorization: Bearer <token>` on the outbound request and does no caching; the SDK consumer owns token caching and refresh.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ProviderTokenAcquireResult {
    /// The bearer token value (without the `Bearer ` prefix).
    pub token: String,
}

/// Host-managed model selection ids to withdraw from the session's BYOK registry.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ProviderWithdrawRequest {
    /// Provider-qualified selection ids to withdraw. Ids that are not registered are ignored, so withdrawal is idempotent. A provider left with no models referencing it is removed too.
    pub models: Vec<String>,
}

/// What the withdrawal actually removed from the registry.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ProviderWithdrawResult {
    /// True when withdrawal removed the selected host-managed model, leaving the session with no explicit selection, so ordinary model resolution picks the session default. Withdrawal never promotes a surviving model in its place: the choice of which model to use stays with the user.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_deselected: Option<bool>,
    /// Providers removed because one of the withdrawn models was the last entry referencing them. A provider that merely has no models is not removed.
    pub providers_removed: Vec<String>,
    /// Selection ids that were registered and are now withdrawn. Excludes requested ids that were not present.
    pub withdrawn: Vec<String>,
}

/// List of Copilot models available to the resolved user, including capabilities and billing metadata.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelsListResult {
    /// Ordered Auto routing preferences discovered for the resolved account. Present only
    /// when CAPI returned Auto metadata; same shape and semantics as `SessionModelList.auto`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auto: Option<AutoTierMetadata>,
    /// The runtime's default image model, separate from conversation models, with its availability for the resolved account. The default is reported even when unavailable; clients choose whether to display it. These entries must not be selected as chat models. Absent on runtimes that do not expose this catalog.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub image_models: Option<Vec<ImageToolModel>>,
    /// List of available models with full metadata
    pub models: Vec<Model>,
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
pub struct ModelsGetBuiltInCatalogResult {
    /// Built-in model entries.
    pub models: Vec<BuiltInModelCatalogEntry>,
}

/// Price units for every catalog model that publishes token prices.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelsListTokenPriceUnitsResult {
    /// One entry per model that publishes token prices, in catalog order. A model with no published prices is omitted.
    pub units: Vec<ModelTokenPriceUnit>,
}

/// A bearer token supplied by the SDK client for a BYOK provider. The runtime sets it as `Authorization: Bearer <token>` on the outbound request and does no caching; the SDK consumer owns token caching and refresh.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ProviderTokenGetTokenResult {
    /// The bearer token value (without the `Bearer ` prefix).
    pub token: String,
}

/// Account availability of the runtime image model. Discovery errors are returned as errors, not converted to unavailable.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ImageToolModelAvailability {
    /// The account has access to a supported image operation.
    #[serde(rename = "available")]
    Available,
    /// The runtime default is not available to this account; enabling the tool cannot grant access.
    #[serde(rename = "unavailable")]
    Unavailable,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// A supported image-service operation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ImageToolModelOperation {
    /// Generate an image from a text prompt.
    #[serde(rename = "generate")]
    Generate,
    /// Generate or edit an image using explicitly selected image inputs.
    #[serde(rename = "edit")]
    Edit,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Unit the catalog publishes a model's token prices in. The numeric fields of ModelBillingTokenPrices carry no unit of their own, so a client that renders a price needs this to scale it.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum TokenPriceUnit {
    /// Prices are already quoted in AI Credits and need no conversion.
    #[serde(rename = "ai_credits")]
    AiCredits,
    /// Prices are quoted in nano-AIU; divide by 1,000,000,000 to get AI Credits.
    #[serde(rename = "nano_aiu")]
    NanoAiu,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

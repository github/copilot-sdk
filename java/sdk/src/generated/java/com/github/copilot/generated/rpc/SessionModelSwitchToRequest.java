/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import java.util.Objects;
import javax.annotation.processing.Generated;

/**
 * Target model identifier and optional reasoning effort, summary, capability overrides, and context tier.
 * <p>
 * Required inputs are constructor arguments. Optional inputs have fluent setters.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class SessionModelSwitchToRequest {

    /** Model id to switch to, as returned by `list`. Include providerId to select an exact catalog entry when providers share the id. Without providerId, a bare id retains incumbent selection behavior; a registry-qualified id (`provider/id`, e.g. `acme/claude-sonnet`) targets a BYOK model. */
    @JsonProperty("modelId")
    private final String modelId;

    /** Optional Auto routing preference to stage atomically with selecting `auto`. Pass null to return to provider-default Auto routing. This field is rejected when `modelId` is not `auto`. */
    @JsonProperty("autoTier")
    private AutoTier autoTier;

    /** Reasoning effort level to use for the model. CAPI values are model-defined and validated against the selected model; BYOK providers may define additional values. "none" disables reasoning. Pass null to clear any session effort override and fall back to the model's default. When omitted, the session's current effort is kept. */
    @JsonProperty("reasoningEffort")
    private String reasoningEffort;

    /** Reasoning summary mode to request for supported model clients */
    @JsonProperty("reasoningSummary")
    private ReasoningSummary reasoningSummary;

    /** Output verbosity level to request for supported models */
    @JsonProperty("verbosity")
    private Verbosity verbosity;

    /** Override individual model capabilities resolved by the runtime */
    @JsonProperty("modelCapabilities")
    private ModelCapabilitiesOverride modelCapabilities;

    /** Explicit context tier for the selected model. `"default"` / `"long_context"` apply the requested tier; omit this field to use normal model behavior with no explicit tier. */
    @JsonProperty("contextTier")
    private ContextTier contextTier;

    /** Origin to record on the effective `session.model_change` event for trusted in-process calls. Transport SDK calls are always recorded as `sdk`, regardless of this value. */
    @JsonProperty("source")
    private ModelChangeSource source;

    /** When true, defer this switch (enqueue it) if another model change is already queued, even when no turn is active — so it drains last (FIFO) and wins over the already-queued change. Intended for genuine user-initiated model selections; internal restore/reapply switches omit it and apply immediately when no turn is active. When no other model change is queued this has no effect (a switch still applies immediately unless a turn is active). */
    @JsonProperty("deferIfModelChangeQueued")
    private Boolean deferIfModelChangeQueued;

    /** Explicit response to a model-switch compaction preflight. Omit to request a confirmation projection when compaction is necessary. */
    @JsonProperty("compactionDecision")
    private String compactionDecision;

    /** When true, evaluate context-window compaction policy before applying the switch. */
    @JsonProperty("runCompactionPreflight")
    private Boolean runCompactionPreflight;

    /** Optional repository settings scope to persist after the switch commits. */
    @JsonProperty("repoScope")
    private String repoScope;

    /** Settings scope used when persisting the selected model. */
    @JsonProperty("modelChangeScope")
    private String modelChangeScope;

    /** Require the target to be currently available and enabled before applying the switch. */
    @JsonProperty("requireAvailable")
    private Boolean requireAvailable;

    /** Optional settings context and explicit-override flags used to persist a picker selection. */
    @JsonProperty("pickerPersistence")
    private ModelPickerPersistenceRequest pickerPersistence;

    /** Provider id from the selected list entry's provider reference. Selects this exact provider/model pair; an unavailable pair fails rather than using another provider. Omit for deterministic legacy bare-model selection. */
    @JsonProperty("providerId")
    private String providerId;

    /**
     * Creates a request with its required inputs.
     *
     * @param modelId Model id to switch to, as returned by `list`. Include providerId to select an exact catalog entry when providers share the id. Without providerId, a bare id retains incumbent selection behavior; a registry-qualified id (`provider/id`, e.g. `acme/claude-sonnet`) targets a BYOK model.
     */
    public SessionModelSwitchToRequest(String modelId) {
        this.modelId = Objects.requireNonNull(modelId, "modelId");
    }

    /**
     * Returns the {@code modelId} property.
     *
     * @return Model id to switch to, as returned by `list`. Include providerId to select an exact catalog entry when providers share the id. Without providerId, a bare id retains incumbent selection behavior; a registry-qualified id (`provider/id`, e.g. `acme/claude-sonnet`) targets a BYOK model.
     */
    public String getModelId() {
        return modelId;
    }

    /**
     * Returns the {@code autoTier} property.
     *
     * @return Optional Auto routing preference to stage atomically with selecting `auto`. Pass null to return to provider-default Auto routing. This field is rejected when `modelId` is not `auto`.
     */
    public AutoTier getAutoTier() {
        return autoTier;
    }

    /**
     * Returns the {@code reasoningEffort} property.
     *
     * @return Reasoning effort level to use for the model. CAPI values are model-defined and validated against the selected model; BYOK providers may define additional values. "none" disables reasoning. Pass null to clear any session effort override and fall back to the model's default. When omitted, the session's current effort is kept.
     */
    public String getReasoningEffort() {
        return reasoningEffort;
    }

    /**
     * Returns the {@code reasoningSummary} property.
     *
     * @return Reasoning summary mode to request for supported model clients
     */
    public ReasoningSummary getReasoningSummary() {
        return reasoningSummary;
    }

    /**
     * Returns the {@code verbosity} property.
     *
     * @return Output verbosity level to request for supported models
     */
    public Verbosity getVerbosity() {
        return verbosity;
    }

    /**
     * Returns the {@code modelCapabilities} property.
     *
     * @return Override individual model capabilities resolved by the runtime
     */
    public ModelCapabilitiesOverride getModelCapabilities() {
        return modelCapabilities;
    }

    /**
     * Returns the {@code contextTier} property.
     *
     * @return Explicit context tier for the selected model. `"default"` / `"long_context"` apply the requested tier; omit this field to use normal model behavior with no explicit tier.
     */
    public ContextTier getContextTier() {
        return contextTier;
    }

    /**
     * Returns the {@code source} property.
     *
     * @return Origin to record on the effective `session.model_change` event for trusted in-process calls. Transport SDK calls are always recorded as `sdk`, regardless of this value.
     */
    public ModelChangeSource getSource() {
        return source;
    }

    /**
     * Returns the {@code deferIfModelChangeQueued} property.
     *
     * @return When true, defer this switch (enqueue it) if another model change is already queued, even when no turn is active — so it drains last (FIFO) and wins over the already-queued change. Intended for genuine user-initiated model selections; internal restore/reapply switches omit it and apply immediately when no turn is active. When no other model change is queued this has no effect (a switch still applies immediately unless a turn is active).
     */
    public Boolean getDeferIfModelChangeQueued() {
        return deferIfModelChangeQueued;
    }

    /**
     * Returns the {@code compactionDecision} property.
     *
     * @return Explicit response to a model-switch compaction preflight. Omit to request a confirmation projection when compaction is necessary.
     */
    public String getCompactionDecision() {
        return compactionDecision;
    }

    /**
     * Returns the {@code runCompactionPreflight} property.
     *
     * @return When true, evaluate context-window compaction policy before applying the switch.
     */
    public Boolean getRunCompactionPreflight() {
        return runCompactionPreflight;
    }

    /**
     * Returns the {@code repoScope} property.
     *
     * @return Optional repository settings scope to persist after the switch commits.
     */
    public String getRepoScope() {
        return repoScope;
    }

    /**
     * Returns the {@code modelChangeScope} property.
     *
     * @return Settings scope used when persisting the selected model.
     */
    public String getModelChangeScope() {
        return modelChangeScope;
    }

    /**
     * Returns the {@code requireAvailable} property.
     *
     * @return Require the target to be currently available and enabled before applying the switch.
     */
    public Boolean getRequireAvailable() {
        return requireAvailable;
    }

    /**
     * Returns the {@code pickerPersistence} property.
     *
     * @return Optional settings context and explicit-override flags used to persist a picker selection.
     */
    public ModelPickerPersistenceRequest getPickerPersistence() {
        return pickerPersistence;
    }

    /**
     * Returns the {@code providerId} property.
     *
     * @return Provider id from the selected list entry's provider reference. Selects this exact provider/model pair; an unavailable pair fails rather than using another provider. Omit for deterministic legacy bare-model selection.
     */
    public String getProviderId() {
        return providerId;
    }

    /**
     * Sets the {@code autoTier} property.
     *
     * @param value Optional Auto routing preference to stage atomically with selecting `auto`. Pass null to return to provider-default Auto routing. This field is rejected when `modelId` is not `auto`.
     * @return this request
     */
    public SessionModelSwitchToRequest setAutoTier(AutoTier value) {
        this.autoTier = value;
        return this;
    }

    /**
     * Sets the {@code reasoningEffort} property.
     *
     * @param value Reasoning effort level to use for the model. CAPI values are model-defined and validated against the selected model; BYOK providers may define additional values. "none" disables reasoning. Pass null to clear any session effort override and fall back to the model's default. When omitted, the session's current effort is kept.
     * @return this request
     */
    public SessionModelSwitchToRequest setReasoningEffort(String value) {
        this.reasoningEffort = value;
        return this;
    }

    /**
     * Sets the {@code reasoningSummary} property.
     *
     * @param value Reasoning summary mode to request for supported model clients
     * @return this request
     */
    public SessionModelSwitchToRequest setReasoningSummary(ReasoningSummary value) {
        this.reasoningSummary = value;
        return this;
    }

    /**
     * Sets the {@code verbosity} property.
     *
     * @param value Output verbosity level to request for supported models
     * @return this request
     */
    public SessionModelSwitchToRequest setVerbosity(Verbosity value) {
        this.verbosity = value;
        return this;
    }

    /**
     * Sets the {@code modelCapabilities} property.
     *
     * @param value Override individual model capabilities resolved by the runtime
     * @return this request
     */
    public SessionModelSwitchToRequest setModelCapabilities(ModelCapabilitiesOverride value) {
        this.modelCapabilities = value;
        return this;
    }

    /**
     * Sets the {@code contextTier} property.
     *
     * @param value Explicit context tier for the selected model. `"default"` / `"long_context"` apply the requested tier; omit this field to use normal model behavior with no explicit tier.
     * @return this request
     */
    public SessionModelSwitchToRequest setContextTier(ContextTier value) {
        this.contextTier = value;
        return this;
    }

    /**
     * Sets the {@code source} property.
     *
     * @param value Origin to record on the effective `session.model_change` event for trusted in-process calls. Transport SDK calls are always recorded as `sdk`, regardless of this value.
     * @return this request
     */
    public SessionModelSwitchToRequest setSource(ModelChangeSource value) {
        this.source = value;
        return this;
    }

    /**
     * Sets the {@code deferIfModelChangeQueued} property.
     *
     * @param value When true, defer this switch (enqueue it) if another model change is already queued, even when no turn is active — so it drains last (FIFO) and wins over the already-queued change. Intended for genuine user-initiated model selections; internal restore/reapply switches omit it and apply immediately when no turn is active. When no other model change is queued this has no effect (a switch still applies immediately unless a turn is active).
     * @return this request
     */
    public SessionModelSwitchToRequest setDeferIfModelChangeQueued(Boolean value) {
        this.deferIfModelChangeQueued = value;
        return this;
    }

    /**
     * Sets the {@code compactionDecision} property.
     *
     * @param value Explicit response to a model-switch compaction preflight. Omit to request a confirmation projection when compaction is necessary.
     * @return this request
     */
    public SessionModelSwitchToRequest setCompactionDecision(String value) {
        this.compactionDecision = value;
        return this;
    }

    /**
     * Sets the {@code runCompactionPreflight} property.
     *
     * @param value When true, evaluate context-window compaction policy before applying the switch.
     * @return this request
     */
    public SessionModelSwitchToRequest setRunCompactionPreflight(Boolean value) {
        this.runCompactionPreflight = value;
        return this;
    }

    /**
     * Sets the {@code repoScope} property.
     *
     * @param value Optional repository settings scope to persist after the switch commits.
     * @return this request
     */
    public SessionModelSwitchToRequest setRepoScope(String value) {
        this.repoScope = value;
        return this;
    }

    /**
     * Sets the {@code modelChangeScope} property.
     *
     * @param value Settings scope used when persisting the selected model.
     * @return this request
     */
    public SessionModelSwitchToRequest setModelChangeScope(String value) {
        this.modelChangeScope = value;
        return this;
    }

    /**
     * Sets the {@code requireAvailable} property.
     *
     * @param value Require the target to be currently available and enabled before applying the switch.
     * @return this request
     */
    public SessionModelSwitchToRequest setRequireAvailable(Boolean value) {
        this.requireAvailable = value;
        return this;
    }

    /**
     * Sets the {@code pickerPersistence} property.
     *
     * @param value Optional settings context and explicit-override flags used to persist a picker selection.
     * @return this request
     */
    public SessionModelSwitchToRequest setPickerPersistence(ModelPickerPersistenceRequest value) {
        this.pickerPersistence = value;
        return this;
    }

    /**
     * Sets the {@code providerId} property.
     *
     * @param value Provider id from the selected list entry's provider reference. Selects this exact provider/model pair; an unavailable pair fails rather than using another provider. Omit for deterministic legacy bare-model selection.
     * @return this request
     */
    public SessionModelSwitchToRequest setProviderId(String value) {
        this.providerId = value;
        return this;
    }
}

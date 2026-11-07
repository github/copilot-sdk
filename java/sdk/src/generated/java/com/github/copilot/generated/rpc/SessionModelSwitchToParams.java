/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Target model identifier and optional reasoning effort, summary, capability overrides, and context tier.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = SessionModelSwitchToParams.$NullableDeserializer.class)
public record SessionModelSwitchToParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Model id to switch to, as returned by `list`. Include providerId to select an exact catalog entry when providers share the id. Without providerId, a bare id retains incumbent selection behavior; a registry-qualified id (`provider/id`, e.g. `acme/claude-sonnet`) targets a BYOK model. */
    @JsonProperty("modelId") String modelId,
    /** Optional Auto routing preference to stage atomically with selecting `auto`. Pass null to return to provider-default Auto routing. This field is rejected when `modelId` is not `auto`. */
    @JsonProperty("autoTier") AutoTier autoTier,
    /** Reasoning effort level to use for the model. CAPI values are model-defined and validated against the selected model; BYOK providers may define additional values. "none" disables reasoning. Pass null to clear any session effort override and fall back to the model's default. When omitted, the session's current effort is kept. */
    @JsonProperty("reasoningEffort") String reasoningEffort,
    /** Reasoning summary mode to request for supported model clients */
    @JsonProperty("reasoningSummary") ReasoningSummary reasoningSummary,
    /** Output verbosity level to request for supported models */
    @JsonProperty("verbosity") Verbosity verbosity,
    /** Override individual model capabilities resolved by the runtime */
    @JsonProperty("modelCapabilities") ModelCapabilitiesOverride modelCapabilities,
    /** Explicit context tier for the selected model. `"default"` / `"long_context"` apply the requested tier; omit this field to use normal model behavior with no explicit tier. */
    @JsonProperty("contextTier") ContextTier contextTier,
    /** Origin to record on the effective `session.model_change` event for trusted in-process calls. Transport SDK calls are always recorded as `sdk`, regardless of this value. */
    @JsonProperty("source") ModelChangeSource source,
    /** When true, defer this switch (enqueue it) if another model change is already queued, even when no turn is active — so it drains last (FIFO) and wins over the already-queued change. Intended for genuine user-initiated model selections; internal restore/reapply switches omit it and apply immediately when no turn is active. When no other model change is queued this has no effect (a switch still applies immediately unless a turn is active). */
    @JsonProperty("deferIfModelChangeQueued") Boolean deferIfModelChangeQueued,
    /** Explicit response to a model-switch compaction preflight. Omit to request a confirmation projection when compaction is necessary. */
    @JsonProperty("compactionDecision") String compactionDecision,
    /** When true, evaluate context-window compaction policy before applying the switch, including effort-only and unchanged selections while idle. An effort-only change applies immediately without compaction when a turn is active, including when the turn starts during preflight; an explicit `compactionDecision: "cancel"` still cancels the change. Other switches retain preflight and may be deferred until the active turn ends. */
    @JsonProperty("runCompactionPreflight") Boolean runCompactionPreflight,
    /** Optional repository settings scope to persist after the switch commits. */
    @JsonProperty("repoScope") String repoScope,
    /** Settings scope used when persisting the selected model. */
    @JsonProperty("modelChangeScope") String modelChangeScope,
    /** Require the target to be currently available and enabled before applying the switch. */
    @JsonProperty("requireAvailable") Boolean requireAvailable,
    /** Optional settings context and explicit-override flags used to persist a picker selection. */
    @JsonProperty("pickerPersistence") ModelPickerPersistenceRequest pickerPersistence,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionModelSwitchToParams { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionModelSwitchToParams(String sessionId, String modelId, AutoTier autoTier, String reasoningEffort, ReasoningSummary reasoningSummary, Verbosity verbosity, ModelCapabilitiesOverride modelCapabilities, ContextTier contextTier, ModelChangeSource source, Boolean deferIfModelChangeQueued, String compactionDecision, Boolean runCompactionPreflight, String repoScope, String modelChangeScope, Boolean requireAvailable, ModelPickerPersistenceRequest pickerPersistence) {
        this(sessionId, modelId, autoTier, reasoningEffort, reasoningSummary, verbosity, modelCapabilities, contextTier, source, deferIfModelChangeQueued, compactionDecision, runCompactionPreflight, repoScope, modelChangeScope, requireAvailable, pickerPersistence, java.util.Set.of());
    }

    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static final class Builder {
        private Builder() {}
        private String sessionId;
        private String modelId;
        private AutoTier autoTier;
        private boolean $presence2;
        private String reasoningEffort;
        private boolean $presence3;
        private ReasoningSummary reasoningSummary;
        private Verbosity verbosity;
        private ModelCapabilitiesOverride modelCapabilities;
        private ContextTier contextTier;
        private ModelChangeSource source;
        private Boolean deferIfModelChangeQueued;
        private String compactionDecision;
        private Boolean runCompactionPreflight;
        private String repoScope;
        private String modelChangeScope;
        private Boolean requireAvailable;
        private ModelPickerPersistenceRequest pickerPersistence;

        /**
         * Sets the {@code sessionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder sessionId(String value) {
            this.sessionId = value;
            return this;
        }

        /**
         * Sets the {@code modelId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder modelId(String value) {
            this.modelId = value;
            return this;
        }

        /**
         * Sets the {@code autoTier} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder autoTier(AutoTier value) {
            this.autoTier = value;
            this.$presence2 = true;
            return this;
        }

        /**
         * Sets the {@code reasoningEffort} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder reasoningEffort(String value) {
            this.reasoningEffort = value;
            this.$presence3 = true;
            return this;
        }

        /**
         * Sets the {@code reasoningSummary} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder reasoningSummary(ReasoningSummary value) {
            this.reasoningSummary = value;
            return this;
        }

        /**
         * Sets the {@code verbosity} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder verbosity(Verbosity value) {
            this.verbosity = value;
            return this;
        }

        /**
         * Sets the {@code modelCapabilities} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder modelCapabilities(ModelCapabilitiesOverride value) {
            this.modelCapabilities = value;
            return this;
        }

        /**
         * Sets the {@code contextTier} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder contextTier(ContextTier value) {
            this.contextTier = value;
            return this;
        }

        /**
         * Sets the {@code source} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder source(ModelChangeSource value) {
            this.source = value;
            return this;
        }

        /**
         * Sets the {@code deferIfModelChangeQueued} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder deferIfModelChangeQueued(Boolean value) {
            this.deferIfModelChangeQueued = value;
            return this;
        }

        /**
         * Sets the {@code compactionDecision} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder compactionDecision(String value) {
            this.compactionDecision = value;
            return this;
        }

        /**
         * Sets the {@code runCompactionPreflight} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder runCompactionPreflight(Boolean value) {
            this.runCompactionPreflight = value;
            return this;
        }

        /**
         * Sets the {@code repoScope} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder repoScope(String value) {
            this.repoScope = value;
            return this;
        }

        /**
         * Sets the {@code modelChangeScope} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder modelChangeScope(String value) {
            this.modelChangeScope = value;
            return this;
        }

        /**
         * Sets the {@code requireAvailable} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder requireAvailable(Boolean value) {
            this.requireAvailable = value;
            return this;
        }

        /**
         * Sets the {@code pickerPersistence} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder pickerPersistence(ModelPickerPersistenceRequest value) {
            this.pickerPersistence = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionModelSwitchToParams build() {
            if (modelId == null) throw new IllegalStateException("Missing required property: modelId");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence2 && autoTier == null) $explicitNulls.add("autoTier");
            if ($presence3 && reasoningEffort == null) $explicitNulls.add("reasoningEffort");
            return new SessionModelSwitchToParams(sessionId, modelId, autoTier, reasoningEffort, reasoningSummary, verbosity, modelCapabilities, contextTier, source, deferIfModelChangeQueued, compactionDecision, runCompactionPreflight, repoScope, modelChangeScope, requireAvailable, pickerPersistence, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("autoTier") && autoTier == null) values.put("autoTier", null);
        if ($explicitNulls.contains("reasoningEffort") && reasoningEffort == null) values.put("reasoningEffort", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<SessionModelSwitchToParams> {
        @Override
        public SessionModelSwitchToParams deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(SessionModelSwitchToParams.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("autoTier") && tree.get("autoTier").isNull()) $explicitNulls.add("autoTier");
            if (tree.has("reasoningEffort") && tree.get("reasoningEffort").isNull()) $explicitNulls.add("reasoningEffort");
            return new SessionModelSwitchToParams(readProperty(context, tree, "sessionId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "modelId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "autoTier", new com.fasterxml.jackson.core.type.TypeReference<AutoTier>() {}), readProperty(context, tree, "reasoningEffort", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "reasoningSummary", new com.fasterxml.jackson.core.type.TypeReference<ReasoningSummary>() {}), readProperty(context, tree, "verbosity", new com.fasterxml.jackson.core.type.TypeReference<Verbosity>() {}), readProperty(context, tree, "modelCapabilities", new com.fasterxml.jackson.core.type.TypeReference<ModelCapabilitiesOverride>() {}), readProperty(context, tree, "contextTier", new com.fasterxml.jackson.core.type.TypeReference<ContextTier>() {}), readProperty(context, tree, "source", new com.fasterxml.jackson.core.type.TypeReference<ModelChangeSource>() {}), readProperty(context, tree, "deferIfModelChangeQueued", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "compactionDecision", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "runCompactionPreflight", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "repoScope", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "modelChangeScope", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "requireAvailable", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "pickerPersistence", new com.fasterxml.jackson.core.type.TypeReference<ModelPickerPersistenceRequest>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}

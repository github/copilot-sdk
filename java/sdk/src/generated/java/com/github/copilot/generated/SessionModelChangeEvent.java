/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Session event "session.model_change". Model change details including previous and new model identifiers
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionModelChangeEvent extends SessionEvent {

    @Override
    public String getType() { return "session.model_change"; }

    @JsonProperty("data")
    private SessionModelChangeEventData data;

    public SessionModelChangeEventData getData() { return data; }
    public void setData(SessionModelChangeEventData data) { this.data = data; }

    /** Data payload for {@link SessionModelChangeEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = SessionModelChangeEventData.$NullableDeserializer.class)
    public record SessionModelChangeEventData(
        /** Model that was previously selected, if any */
        @JsonProperty("previousModel") String previousModel,
        /** Newly selected model identifier */
        @JsonProperty("newModel") String newModel,
        /** Provider selected for newModel. Omitted for legacy or unattributed selections; never inferred from a later selection. */
        @JsonProperty("providerId") String providerId,
        /** Provider of previousModel, when known. A provider-only change is a model selection change even when the model identifiers are equal. */
        @JsonProperty("previousProviderId") String previousProviderId,
        /** True when prior reasoning is incompatible with the selected provider routing even though model and provider identifiers are unchanged. Omitted for compatible or legacy selections. */
        @JsonProperty("reasoningInvalidated") Boolean reasoningInvalidated,
        /** Reasoning effort level before the model change, if applicable */
        @JsonProperty("previousReasoningEffort") String previousReasoningEffort,
        /** Reasoning effort level after the model change, if applicable */
        @JsonProperty("reasoningEffort") String reasoningEffort,
        /** Model that owns effort embedded in an authored model selection. Omitted for independent reasoning-effort overrides and legacy events. */
        @JsonProperty("reasoningEffortModel") String reasoningEffortModel,
        /** True when the reasoning effort is a managed-policy default bound to reasoningEffortModel. Omitted for agent-authored, user-authored, independent, and legacy effort. */
        @JsonProperty("reasoningEffortManaged") Boolean reasoningEffortManaged,
        /** Reasoning summary mode before the model change, if applicable */
        @JsonProperty("previousReasoningSummary") ReasoningSummary previousReasoningSummary,
        /** Reasoning summary mode after the model change, if applicable */
        @JsonProperty("reasoningSummary") ReasoningSummary reasoningSummary,
        /** Output verbosity level before the model change, if applicable */
        @JsonProperty("previousVerbosity") Verbosity previousVerbosity,
        /** Output verbosity level after the model change, if applicable */
        @JsonProperty("verbosity") Verbosity verbosity,
        /** Context tier after the model change; null explicitly clears a previously selected tier */
        @JsonProperty("contextTier") ContextTier contextTier,
        /** True when contextTier is a managed-policy default. Omitted for user-authored and legacy values. */
        @JsonProperty("contextTierManaged") Boolean contextTierManaged,
        /** Reason the change happened, when not user-initiated. `"rate_limit_auto_switch"` for changes triggered by the auto-mode-switch rate-limit recovery path, `"auto_mode_recovery"` when a prepared Auto route commits and a recovered inference is allowed to proceed before provider dispatch, or `"refusal_fallback"` when the active model declined a request (content refusal) and the runtime switched to the configured refusal-fallback model. UI clients can use this to render contextual copy. */
        @JsonProperty("cause") String cause,
        /** Origin of the effective model change, when known. */
        @JsonProperty("source") ModelChangeSource source,
        /** Previously committed Auto preference, when one was explicitly selected. */
        @JsonProperty("previousAutoTier") AutoTier previousAutoTier,
        /** Committed Auto preference after the model configuration change, when applicable. */
        @JsonProperty("autoTier") AutoTier autoTier,
        /** True when autoTier is a managed-policy default. Omitted for user-authored and legacy values. */
        @JsonProperty("autoTierManaged") Boolean autoTierManaged,
        @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
    ) {

        /**
         * Creates a record with the components it had before later optional fields were added.
         * @deprecated Use named construction via {@link #builder()}.
         * @param previousModel Model that was previously selected, if any
         * @param newModel Newly selected model identifier
         * @param previousReasoningEffort Reasoning effort level before the model change, if applicable
         * @param reasoningEffort Reasoning effort level after the model change, if applicable
         * @param reasoningEffortModel Model that owns effort embedded in an authored model selection. Omitted for independent reasoning-effort overrides and legacy events.
         * @param previousReasoningSummary Reasoning summary mode before the model change, if applicable
         * @param reasoningSummary Reasoning summary mode after the model change, if applicable
         * @param previousVerbosity Output verbosity level before the model change, if applicable
         * @param verbosity Output verbosity level after the model change, if applicable
         * @param contextTier Context tier after the model change; null explicitly clears a previously selected tier
         * @param cause Reason the change happened, when not user-initiated. `"rate_limit_auto_switch"` for changes triggered by the auto-mode-switch rate-limit recovery path, `"auto_mode_recovery"` when a prepared Auto route commits and a recovered inference is allowed to proceed before provider dispatch, or `"refusal_fallback"` when the active model declined a request (content refusal) and the runtime switched to the configured refusal-fallback model. UI clients can use this to render contextual copy.
         * @param source Origin of the effective model change, when known.
         * @param previousAutoTier Previously committed Auto preference, when one was explicitly selected.
         * @param autoTier Committed Auto preference after the model configuration change, when applicable.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionModelChangeEventData(
            String previousModel,
            String newModel,
            String previousReasoningEffort,
            String reasoningEffort,
            String reasoningEffortModel,
            ReasoningSummary previousReasoningSummary,
            ReasoningSummary reasoningSummary,
            Verbosity previousVerbosity,
            Verbosity verbosity,
            ContextTier contextTier,
            String cause,
            ModelChangeSource source,
            AutoTier previousAutoTier,
            AutoTier autoTier
        ) {
            this(previousModel, newModel, null, null, null, previousReasoningEffort, reasoningEffort, reasoningEffortModel, null, previousReasoningSummary, reasoningSummary, previousVerbosity, verbosity, contextTier, null, cause, source, previousAutoTier, autoTier, null);
        }

        /**
         * Retains the constructor published in Java SDK 1.0.28.
         * @deprecated Use named construction via {@link #builder()}.
         * @param previousModel Model that was previously selected, if any
         * @param newModel Newly selected model identifier
         * @param providerId Provider selected for newModel. Omitted for legacy or unattributed selections; never inferred from a later selection.
         * @param previousProviderId Provider of previousModel, when known. A provider-only change is a model selection change even when the model identifiers are equal.
         * @param previousReasoningEffort Reasoning effort level before the model change, if applicable
         * @param reasoningEffort Reasoning effort level after the model change, if applicable
         * @param reasoningEffortModel Model that owns effort embedded in an authored model selection. Omitted for independent reasoning-effort overrides and legacy events.
         * @param reasoningEffortManaged True when the reasoning effort is a managed-policy default bound to reasoningEffortModel. Omitted for agent-authored, user-authored, independent, and legacy effort.
         * @param previousReasoningSummary Reasoning summary mode before the model change, if applicable
         * @param reasoningSummary Reasoning summary mode after the model change, if applicable
         * @param previousVerbosity Output verbosity level before the model change, if applicable
         * @param verbosity Output verbosity level after the model change, if applicable
         * @param contextTier Context tier after the model change; null explicitly clears a previously selected tier
         * @param contextTierManaged True when contextTier is a managed-policy default. Omitted for user-authored and legacy values.
         * @param cause Reason the change happened, when not user-initiated. `"rate_limit_auto_switch"` for changes triggered by the auto-mode-switch rate-limit recovery path, `"auto_mode_recovery"` when a prepared Auto route commits and a recovered inference is allowed to proceed before provider dispatch, or `"refusal_fallback"` when the active model declined a request (content refusal) and the runtime switched to the configured refusal-fallback model. UI clients can use this to render contextual copy.
         * @param source Origin of the effective model change, when known.
         * @param previousAutoTier Previously committed Auto preference, when one was explicitly selected.
         * @param autoTier Committed Auto preference after the model configuration change, when applicable.
         * @param autoTierManaged True when autoTier is a managed-policy default. Omitted for user-authored and legacy values.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionModelChangeEventData(
            String previousModel,
            String newModel,
            String providerId,
            String previousProviderId,
            String previousReasoningEffort,
            String reasoningEffort,
            String reasoningEffortModel,
            Boolean reasoningEffortManaged,
            ReasoningSummary previousReasoningSummary,
            ReasoningSummary reasoningSummary,
            Verbosity previousVerbosity,
            Verbosity verbosity,
            ContextTier contextTier,
            Boolean contextTierManaged,
            String cause,
            ModelChangeSource source,
            AutoTier previousAutoTier,
            AutoTier autoTier,
            Boolean autoTierManaged
        ) {
            this(previousModel, newModel, providerId, previousProviderId, null, previousReasoningEffort, reasoningEffort, reasoningEffortModel, reasoningEffortManaged, previousReasoningSummary, reasoningSummary, previousVerbosity, verbosity, contextTier, contextTierManaged, cause, source, previousAutoTier, autoTier, autoTierManaged);
        }

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionModelChangeEventData { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

        /**
         * Creates a record with legacy null-as-omission semantics.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionModelChangeEventData(String previousModel, String newModel, String providerId, String previousProviderId, Boolean reasoningInvalidated, String previousReasoningEffort, String reasoningEffort, String reasoningEffortModel, Boolean reasoningEffortManaged, ReasoningSummary previousReasoningSummary, ReasoningSummary reasoningSummary, Verbosity previousVerbosity, Verbosity verbosity, ContextTier contextTier, Boolean contextTierManaged, String cause, ModelChangeSource source, AutoTier previousAutoTier, AutoTier autoTier, Boolean autoTierManaged) {
            this(previousModel, newModel, providerId, previousProviderId, reasoningInvalidated, previousReasoningEffort, reasoningEffort, reasoningEffortModel, reasoningEffortManaged, previousReasoningSummary, reasoningSummary, previousVerbosity, verbosity, contextTier, contextTierManaged, cause, source, previousAutoTier, autoTier, autoTierManaged, java.util.Set.of());
        }

        /**
         * Returns a named builder.
         * @return a new builder
         * @since 2.0.0
         */
        public static Builder builder() { return new Builder(); }

        /**
         * Named construction for this record.
         * @since 2.0.0
         */
        public static final class Builder {
            private Builder() {}
            private String previousModel;
            private String newModel;
            private String providerId;
            private String previousProviderId;
            private Boolean reasoningInvalidated;
            private String previousReasoningEffort;
            private String reasoningEffort;
            private boolean $presence6;
            private String reasoningEffortModel;
            private Boolean reasoningEffortManaged;
            private ReasoningSummary previousReasoningSummary;
            private ReasoningSummary reasoningSummary;
            private Verbosity previousVerbosity;
            private Verbosity verbosity;
            private ContextTier contextTier;
            private boolean $presence13;
            private Boolean contextTierManaged;
            private String cause;
            private ModelChangeSource source;
            private AutoTier previousAutoTier;
            private AutoTier autoTier;
            private boolean $presence18;
            private Boolean autoTierManaged;

            /**
             * Sets the {@code previousModel} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder previousModel(String value) {
                this.previousModel = value;
                return this;
            }

            /**
             * Sets the {@code newModel} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder newModel(String value) {
                this.newModel = value;
                return this;
            }

            /**
             * Sets the {@code providerId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder providerId(String value) {
                this.providerId = value;
                return this;
            }

            /**
             * Sets the {@code previousProviderId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder previousProviderId(String value) {
                this.previousProviderId = value;
                return this;
            }

            /**
             * Sets the {@code reasoningInvalidated} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder reasoningInvalidated(Boolean value) {
                this.reasoningInvalidated = value;
                return this;
            }

            /**
             * Sets the {@code previousReasoningEffort} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder previousReasoningEffort(String value) {
                this.previousReasoningEffort = value;
                return this;
            }

            /**
             * Sets the {@code reasoningEffort} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder reasoningEffort(String value) {
                this.reasoningEffort = value;
                this.$presence6 = true;
                return this;
            }

            /**
             * Sets the {@code reasoningEffortModel} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder reasoningEffortModel(String value) {
                this.reasoningEffortModel = value;
                return this;
            }

            /**
             * Sets the {@code reasoningEffortManaged} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder reasoningEffortManaged(Boolean value) {
                this.reasoningEffortManaged = value;
                return this;
            }

            /**
             * Sets the {@code previousReasoningSummary} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder previousReasoningSummary(ReasoningSummary value) {
                this.previousReasoningSummary = value;
                return this;
            }

            /**
             * Sets the {@code reasoningSummary} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder reasoningSummary(ReasoningSummary value) {
                this.reasoningSummary = value;
                return this;
            }

            /**
             * Sets the {@code previousVerbosity} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder previousVerbosity(Verbosity value) {
                this.previousVerbosity = value;
                return this;
            }

            /**
             * Sets the {@code verbosity} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder verbosity(Verbosity value) {
                this.verbosity = value;
                return this;
            }

            /**
             * Sets the {@code contextTier} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder contextTier(ContextTier value) {
                this.contextTier = value;
                this.$presence13 = true;
                return this;
            }

            /**
             * Sets the {@code contextTierManaged} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder contextTierManaged(Boolean value) {
                this.contextTierManaged = value;
                return this;
            }

            /**
             * Sets the {@code cause} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder cause(String value) {
                this.cause = value;
                return this;
            }

            /**
             * Sets the {@code source} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder source(ModelChangeSource value) {
                this.source = value;
                return this;
            }

            /**
             * Sets the {@code previousAutoTier} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder previousAutoTier(AutoTier value) {
                this.previousAutoTier = value;
                return this;
            }

            /**
             * Sets the {@code autoTier} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder autoTier(AutoTier value) {
                this.autoTier = value;
                this.$presence18 = true;
                return this;
            }

            /**
             * Sets the {@code autoTierManaged} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder autoTierManaged(Boolean value) {
                this.autoTierManaged = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionModelChangeEventData build() {
                if (newModel == null) throw new IllegalStateException("Missing required property: newModel");
                java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
                if ($presence6 && reasoningEffort == null) $explicitNulls.add("reasoningEffort");
                if ($presence13 && contextTier == null) $explicitNulls.add("contextTier");
                if ($presence18 && autoTier == null) $explicitNulls.add("autoTier");
                return new SessionModelChangeEventData(previousModel, newModel, providerId, previousProviderId, reasoningInvalidated, previousReasoningEffort, reasoningEffort, reasoningEffortModel, reasoningEffortManaged, previousReasoningSummary, reasoningSummary, previousVerbosity, verbosity, contextTier, contextTierManaged, cause, source, previousAutoTier, autoTier, autoTierManaged, $explicitNulls);
            }
        }

        @com.fasterxml.jackson.annotation.JsonAnyGetter
        @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
        public java.util.Map<String, Object> $nullableProperties() {
            java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
            if ($explicitNulls.contains("reasoningEffort") && reasoningEffort == null) values.put("reasoningEffort", null);
            if ($explicitNulls.contains("contextTier") && contextTier == null) values.put("contextTier", null);
            if ($explicitNulls.contains("autoTier") && autoTier == null) values.put("autoTier", null);
            return values;
        }

        public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<SessionModelChangeEventData> {
            @Override
            public SessionModelChangeEventData deserialize(com.fasterxml.jackson.core.JsonParser parser,
                    com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
                com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
                if (!tree.isObject()) return context.reportInputMismatch(SessionModelChangeEventData.class, "Expected a JSON object");
                java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
                if (tree.has("reasoningEffort") && tree.get("reasoningEffort").isNull()) $explicitNulls.add("reasoningEffort");
                if (tree.has("contextTier") && tree.get("contextTier").isNull()) $explicitNulls.add("contextTier");
                if (tree.has("autoTier") && tree.get("autoTier").isNull()) $explicitNulls.add("autoTier");
                return new SessionModelChangeEventData(readProperty(context, tree, "previousModel", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "newModel", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "providerId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "previousProviderId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "reasoningInvalidated", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "previousReasoningEffort", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "reasoningEffort", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "reasoningEffortModel", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "reasoningEffortManaged", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "previousReasoningSummary", new com.fasterxml.jackson.core.type.TypeReference<ReasoningSummary>() {}), readProperty(context, tree, "reasoningSummary", new com.fasterxml.jackson.core.type.TypeReference<ReasoningSummary>() {}), readProperty(context, tree, "previousVerbosity", new com.fasterxml.jackson.core.type.TypeReference<Verbosity>() {}), readProperty(context, tree, "verbosity", new com.fasterxml.jackson.core.type.TypeReference<Verbosity>() {}), readProperty(context, tree, "contextTier", new com.fasterxml.jackson.core.type.TypeReference<ContextTier>() {}), readProperty(context, tree, "contextTierManaged", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "cause", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "source", new com.fasterxml.jackson.core.type.TypeReference<ModelChangeSource>() {}), readProperty(context, tree, "previousAutoTier", new com.fasterxml.jackson.core.type.TypeReference<AutoTier>() {}), readProperty(context, tree, "autoTier", new com.fasterxml.jackson.core.type.TypeReference<AutoTier>() {}), readProperty(context, tree, "autoTierManaged", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), $explicitNulls);
            }

            private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                    com.fasterxml.jackson.databind.JsonNode tree, String name,
                    com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
                if (!tree.has(name)) return null;
                return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
            }
        }
    }
}

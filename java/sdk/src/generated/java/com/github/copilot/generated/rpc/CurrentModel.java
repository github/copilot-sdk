/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * The session's authoritative model snapshot. Auto preference fields are configuration for the virtual `auto` model and do not change the selected model identifier. The context tier reflects `Session.getContextTier()`, restored from the session journal on resume.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = CurrentModel.$NullableDeserializer.class)
public record CurrentModel(
    /** Currently active model identifier */
    @JsonProperty("modelId") String modelId,
    /** Provider selected for this model. Together with modelId, identifies a catalog entry even when providers serve the same model. Omitted for unattributed legacy selections. */
    @JsonProperty("providerId") String providerId,
    /** Captured base model to restore when leaving plan mode. Omitted outside plan mode or when no plan override has captured a base model. Persistent agent model requirements apply to this model rather than the temporary plan model. */
    @JsonProperty("planBaseModelId") String planBaseModelId,
    /** Provider of planBaseModelId, when that saved selection is provider-qualified. */
    @JsonProperty("planBaseProviderId") String planBaseProviderId,
    /** Reasoning effort level currently applied to the active model, when one is set. Reads `Session.getReasoningEffort()` synchronously after `getSelectedModel()` resolves so the two values are reported as a snapshot. */
    @JsonProperty("reasoningEffort") String reasoningEffort,
    /** Context tier for models that support multiple context-window sizes. */
    @JsonProperty("contextTier") ContextTier contextTier,
    /** Auto preference currently committed for the session. This can remain available while another model is selected so a later switch to `auto` can reuse it. */
    @JsonProperty("autoTier") AutoTier autoTier,
    /** Latest unclaimed Auto preference waiting for a future user turn. Null means the pending request is returning to provider-default routing. */
    @JsonProperty("pendingAutoTier") AutoTier pendingAutoTier,
    /** Auto preference currently claimed by an in-progress activation. Null means the activation is returning to provider-default routing. */
    @JsonProperty("activatingAutoTier") AutoTier activatingAutoTier,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public CurrentModel { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public CurrentModel(String modelId, String providerId, String planBaseModelId, String planBaseProviderId, String reasoningEffort, ContextTier contextTier, AutoTier autoTier, AutoTier pendingAutoTier, AutoTier activatingAutoTier) {
        this(modelId, providerId, planBaseModelId, planBaseProviderId, reasoningEffort, contextTier, autoTier, pendingAutoTier, activatingAutoTier, java.util.Set.of());
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
        private String modelId;
        private String providerId;
        private String planBaseModelId;
        private String planBaseProviderId;
        private String reasoningEffort;
        private ContextTier contextTier;
        private AutoTier autoTier;
        private AutoTier pendingAutoTier;
        private boolean $presence7;
        private AutoTier activatingAutoTier;
        private boolean $presence8;

        /**
         * Sets the {@code modelId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder modelId(String value) {
            this.modelId = value;
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
         * Sets the {@code planBaseModelId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder planBaseModelId(String value) {
            this.planBaseModelId = value;
            return this;
        }

        /**
         * Sets the {@code planBaseProviderId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder planBaseProviderId(String value) {
            this.planBaseProviderId = value;
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
            return this;
        }

        /**
         * Sets the {@code pendingAutoTier} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder pendingAutoTier(AutoTier value) {
            this.pendingAutoTier = value;
            this.$presence7 = true;
            return this;
        }

        /**
         * Sets the {@code activatingAutoTier} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder activatingAutoTier(AutoTier value) {
            this.activatingAutoTier = value;
            this.$presence8 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CurrentModel build() {
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence7 && pendingAutoTier == null) $explicitNulls.add("pendingAutoTier");
            if ($presence8 && activatingAutoTier == null) $explicitNulls.add("activatingAutoTier");
            return new CurrentModel(modelId, providerId, planBaseModelId, planBaseProviderId, reasoningEffort, contextTier, autoTier, pendingAutoTier, activatingAutoTier, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("pendingAutoTier") && pendingAutoTier == null) values.put("pendingAutoTier", null);
        if ($explicitNulls.contains("activatingAutoTier") && activatingAutoTier == null) values.put("activatingAutoTier", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<CurrentModel> {
        @Override
        public CurrentModel deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(CurrentModel.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("pendingAutoTier") && tree.get("pendingAutoTier").isNull()) $explicitNulls.add("pendingAutoTier");
            if (tree.has("activatingAutoTier") && tree.get("activatingAutoTier").isNull()) $explicitNulls.add("activatingAutoTier");
            return new CurrentModel(readProperty(context, tree, "modelId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "providerId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "planBaseModelId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "planBaseProviderId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "reasoningEffort", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "contextTier", new com.fasterxml.jackson.core.type.TypeReference<ContextTier>() {}), readProperty(context, tree, "autoTier", new com.fasterxml.jackson.core.type.TypeReference<AutoTier>() {}), readProperty(context, tree, "pendingAutoTier", new com.fasterxml.jackson.core.type.TypeReference<AutoTier>() {}), readProperty(context, tree, "activatingAutoTier", new com.fasterxml.jackson.core.type.TypeReference<AutoTier>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}

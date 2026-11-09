/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * A BYOK model definition referencing a named provider.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = ProviderModelConfig.$NullableDeserializer.class)
public record ProviderModelConfig(
    /** Provider-local model id, unique within its provider. The session-wide selection id (shown in the model list and passed to switchTo) is the provider-qualified `provider/id`. */
    @JsonProperty("id") String id,
    /** Name of the configured provider that serves this model. */
    @JsonProperty("provider") String provider,
    /** Forward-compatible persisted on-device Auto annotation. The live counterpart is metadata.autoLocal. */
    @JsonProperty("autoLocal") Object autoLocal,
    /** The model name sent to the provider API for inference. Defaults to `id`. */
    @JsonProperty("wireModel") String wireModel,
    /** Well-known base model id used for behavior/capability/config lookup. Defaults to `id`. */
    @JsonProperty("modelId") String modelId,
    /** Display name for model pickers. Defaults to the provider-qualified selection id (`provider/id`). */
    @JsonProperty("name") String name,
    /** Maximum prompt/input tokens for the model. */
    @JsonProperty("maxPromptTokens") Long maxPromptTokens,
    /** Maximum context window tokens for the model. */
    @JsonProperty("maxContextWindowTokens") Long maxContextWindowTokens,
    /** Maximum output tokens for the model. */
    @JsonProperty("maxOutputTokens") Long maxOutputTokens,
    /** Optional capability overrides (vision, tool_calls, reasoning, etc.). */
    @JsonProperty("capabilities") ModelCapabilitiesOverride capabilities,
    /** System-message configuration used when the runtime builds the standard prompt for this provider-qualified model, including general-purpose subagents. It uses the same object hierarchy as session-level systemMessage configuration, except transform actions are rejected because the current callback protocol is not model-scoped. When present, it overrides the session-wide configuration on those prompt paths. Selected custom-agent and specialized-subagent prompts remain authoritative. */
    @JsonProperty("systemMessage") Object systemMessage,
    /** Provider-published model metadata, preserved verbatim as the public Model.metadata object. */
    @JsonInclude(value = JsonInclude.Include.NON_NULL, content = JsonInclude.Include.ALWAYS)
    @JsonProperty("metadata") Map<String, Object> metadata,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ProviderModelConfig { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ProviderModelConfig(String id, String provider, Object autoLocal, String wireModel, String modelId, String name, Long maxPromptTokens, Long maxContextWindowTokens, Long maxOutputTokens, ModelCapabilitiesOverride capabilities, Object systemMessage, Map<String, Object> metadata) {
        this(id, provider, autoLocal, wireModel, modelId, name, maxPromptTokens, maxContextWindowTokens, maxOutputTokens, capabilities, systemMessage, metadata, java.util.Set.of());
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
        private String id;
        private String provider;
        private Object autoLocal;
        private boolean $presence2;
        private String wireModel;
        private String modelId;
        private String name;
        private Long maxPromptTokens;
        private Long maxContextWindowTokens;
        private Long maxOutputTokens;
        private ModelCapabilitiesOverride capabilities;
        private Object systemMessage;
        private Map<String, Object> metadata;

        /**
         * Sets the {@code id} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder id(String value) {
            this.id = value;
            return this;
        }

        /**
         * Sets the {@code provider} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder provider(String value) {
            this.provider = value;
            return this;
        }

        /**
         * Sets the {@code autoLocal} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder autoLocal(Object value) {
            this.autoLocal = value;
            this.$presence2 = true;
            return this;
        }

        /**
         * Sets the {@code wireModel} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder wireModel(String value) {
            this.wireModel = value;
            return this;
        }

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
         * Sets the {@code name} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder name(String value) {
            this.name = value;
            return this;
        }

        /**
         * Sets the {@code maxPromptTokens} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder maxPromptTokens(Long value) {
            this.maxPromptTokens = value;
            return this;
        }

        /**
         * Sets the {@code maxContextWindowTokens} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder maxContextWindowTokens(Long value) {
            this.maxContextWindowTokens = value;
            return this;
        }

        /**
         * Sets the {@code maxOutputTokens} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder maxOutputTokens(Long value) {
            this.maxOutputTokens = value;
            return this;
        }

        /**
         * Sets the {@code capabilities} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder capabilities(ModelCapabilitiesOverride value) {
            this.capabilities = value;
            return this;
        }

        /**
         * Sets the {@code systemMessage} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder systemMessage(Object value) {
            this.systemMessage = value;
            return this;
        }

        /**
         * Sets the {@code metadata} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder metadata(Map<String, Object> value) {
            this.metadata = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ProviderModelConfig build() {
            if (id == null) throw new IllegalStateException("Missing required property: id");
            if (provider == null) throw new IllegalStateException("Missing required property: provider");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence2 && autoLocal == null) $explicitNulls.add("autoLocal");
            return new ProviderModelConfig(id, provider, autoLocal, wireModel, modelId, name, maxPromptTokens, maxContextWindowTokens, maxOutputTokens, capabilities, systemMessage, metadata, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("autoLocal") && autoLocal == null) values.put("autoLocal", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<ProviderModelConfig> {
        @Override
        public ProviderModelConfig deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(ProviderModelConfig.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("autoLocal") && tree.get("autoLocal").isNull()) $explicitNulls.add("autoLocal");
            return new ProviderModelConfig(readProperty(context, tree, "id", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "provider", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "autoLocal", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "wireModel", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "modelId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "name", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "maxPromptTokens", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "maxContextWindowTokens", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "maxOutputTokens", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "capabilities", new com.fasterxml.jackson.core.type.TypeReference<ModelCapabilitiesOverride>() {}), readProperty(context, tree, "systemMessage", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "metadata", new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }

    /**
     * Creates a record with the components it had before later optional fields were added.
     * @deprecated Use named construction via {@link #builder()}.
     * @param id Provider-local model id, unique within its provider. The session-wide selection id (shown in the model list and passed to switchTo) is the provider-qualified `provider/id`.
     * @param provider Name of the configured provider that serves this model.
     * @param wireModel The model name sent to the provider API for inference. Defaults to `id`.
     * @param modelId Well-known base model id used for behavior/capability/config lookup. Defaults to `id`.
     * @param name Display name for model pickers. Defaults to the provider-qualified selection id (`provider/id`).
     * @param maxPromptTokens Maximum prompt/input tokens for the model.
     * @param maxContextWindowTokens Maximum context window tokens for the model.
     * @param maxOutputTokens Maximum output tokens for the model.
     * @param capabilities Optional capability overrides (vision, tool_calls, reasoning, etc.).
     * @param systemMessage System-message configuration used when the runtime builds the standard prompt for this provider-qualified model, including general-purpose subagents. It uses the same object hierarchy as session-level systemMessage configuration, except transform actions are rejected because the current callback protocol is not model-scoped. When present, it overrides the session-wide configuration on those prompt paths. Selected custom-agent and specialized-subagent prompts remain authoritative.
     * @param metadata Provider-published model metadata, preserved verbatim as the public Model.metadata object.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ProviderModelConfig(
        String id,
        String provider,
        String wireModel,
        String modelId,
        String name,
        Long maxPromptTokens,
        Long maxContextWindowTokens,
        Long maxOutputTokens,
        ModelCapabilitiesOverride capabilities,
        Object systemMessage,
        Map<String, Object> metadata
    ) {
        this(id, provider, null, wireModel, modelId, name, maxPromptTokens, maxContextWindowTokens, maxOutputTokens, capabilities, systemMessage, metadata);
    }
}

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
public record ProviderModelConfig(
    /** Provider-local model id, unique within its provider. The session-wide selection id (shown in the model list and passed to switchTo) is the provider-qualified `provider/id`. */
    @JsonProperty("id") String id,
    /** Name of the configured provider that serves this model. */
    @JsonProperty("provider") String provider,
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
    @JsonProperty("metadata") Map<String, Object> metadata
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ProviderModelConfig {}

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
            return new ProviderModelConfig(id, provider, wireModel, modelId, name, maxPromptTokens, maxContextWindowTokens, maxOutputTokens, capabilities, systemMessage, metadata);
        }
    }
}

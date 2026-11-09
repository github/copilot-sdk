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
 * Request to add a model to the server-owned saved provider configuration.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SavedProviderConfigurationAddModelRequest(
    /** Provider settings to create when this provider is not already saved. */
    @JsonProperty("provider") SavedProviderConfigurationProviderInput provider,
    /** Model to add to the provider. */
    @JsonProperty("model") ProviderModelConfig model,
    /** Optional replacement API key. Stored in the OS keychain and never returned. */
    @JsonProperty("credential") String credential
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SavedProviderConfigurationAddModelRequest {}

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
        private SavedProviderConfigurationProviderInput provider;
        private ProviderModelConfig model;
        private String credential;

        /**
         * Sets the {@code provider} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder provider(SavedProviderConfigurationProviderInput value) {
            this.provider = value;
            return this;
        }

        /**
         * Sets the {@code model} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder model(ProviderModelConfig value) {
            this.model = value;
            return this;
        }

        /**
         * Sets the {@code credential} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder credential(String value) {
            this.credential = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SavedProviderConfigurationAddModelRequest build() {
            if (provider == null) throw new IllegalStateException("Missing required property: provider");
            if (model == null) throw new IllegalStateException("Missing required property: model");
            return new SavedProviderConfigurationAddModelRequest(provider, model, credential);
        }
    }
}

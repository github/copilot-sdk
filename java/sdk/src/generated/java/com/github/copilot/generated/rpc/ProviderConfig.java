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
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Custom model-provider configuration (BYOK).
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProviderConfig(
    /** Provider type. Defaults to "openai" for generic OpenAI-compatible APIs. */
    @JsonProperty("type") ProviderConfigType type,
    /** Wire API format (openai/azure only). Defaults to "completions". */
    @JsonProperty("wireApi") ProviderConfigWireApi wireApi,
    /** Provider transport. Defaults to "http". */
    @JsonProperty("transport") ProviderConfigTransport transport,
    /** API endpoint URL. */
    @JsonProperty("baseUrl") String baseUrl,
    /** API key. Optional for local providers like Ollama. */
    @JsonProperty("apiKey") String apiKey,
    /** Command run by the runtime before each provider request to obtain a fresh API key. Credential-command failures fail the turn without sending a model request. Bearer authentication takes precedence. */
    @JsonProperty("apiKeyCommand") String apiKeyCommand,
    /** Bearer token for authentication. Sets the Authorization header directly. Takes precedence over apiKey when both are set. */
    @JsonProperty("bearerToken") String bearerToken,
    /** Azure-specific provider options. */
    @JsonProperty("azure") ProviderConfigAzure azure,
    /** Well-known model ID used for capability lookup. When set, agent behavior config and token limits are inferred from this model. */
    @JsonProperty("modelId") String modelId,
    /** Overrides for model capabilities when they cannot be inferred from modelId. */
    @JsonProperty("modelCapabilities") ModelCapabilitiesOverride modelCapabilities,
    /** Provider name used for model and telemetry attribution. */
    @JsonProperty("providerName") String providerName,
    /** The product serving the model, reported in telemetry as `model_provider`. Set it when `type` alone cannot identify the product, such as Ollama or LM Studio behind an OpenAI-compatible endpoint. Only affects telemetry. */
    @JsonProperty("modelProvider") ProviderConfigModelProvider modelProvider,
    /** The model identifier sent to the provider API for inference (the "wire" model), as opposed to modelId which is the well-known base. */
    @JsonProperty("wireModel") String wireModel,
    /** Maximum prompt/input tokens for the model. */
    @JsonProperty("maxPromptTokens") Double maxPromptTokens,
    /** Maximum context window tokens for the model. */
    @JsonProperty("maxContextWindowTokens") Double maxContextWindowTokens,
    /** Maximum output tokens for the model. */
    @JsonProperty("maxOutputTokens") Double maxOutputTokens,
    /** Custom HTTP headers to include in all outbound requests to the provider. */
    @JsonProperty("headers") Map<String, String> headers,
    /** When true, the SDK client supplies bearer tokens on demand: the runtime calls the client-session `providerToken.getToken` callback before each request and applies the returned token as an `Authorization: Bearer <token>` header. This is the bearer/OAuth scheme used by Azure AD / managed-identity tokens and provider OAuth access tokens (including Anthropic's), not a provider-specific API-key header such as Anthropic's `x-api-key`. The token-acquiring function itself stays on the SDK side and is never serialized; only this flag crosses the wire. When set alongside `apiKey`/`bearerToken`, the callback takes precedence: the runtime applies the token returned by `providerToken.getToken` as the `Authorization: Bearer` header for each request and does not send the static credential. */
    @JsonProperty("hasBearerTokenProvider") Boolean hasBearerTokenProvider
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ProviderConfig {}

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
        private ProviderConfigType type;
        private ProviderConfigWireApi wireApi;
        private ProviderConfigTransport transport;
        private String baseUrl;
        private String apiKey;
        private String apiKeyCommand;
        private String bearerToken;
        private ProviderConfigAzure azure;
        private String modelId;
        private ModelCapabilitiesOverride modelCapabilities;
        private String providerName;
        private ProviderConfigModelProvider modelProvider;
        private String wireModel;
        private Double maxPromptTokens;
        private Double maxContextWindowTokens;
        private Double maxOutputTokens;
        private Map<String, String> headers;
        private Boolean hasBearerTokenProvider;

        /**
         * Sets the {@code type} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder type(ProviderConfigType value) {
            this.type = value;
            return this;
        }

        /**
         * Sets the {@code wireApi} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder wireApi(ProviderConfigWireApi value) {
            this.wireApi = value;
            return this;
        }

        /**
         * Sets the {@code transport} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder transport(ProviderConfigTransport value) {
            this.transport = value;
            return this;
        }

        /**
         * Sets the {@code baseUrl} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder baseUrl(String value) {
            this.baseUrl = value;
            return this;
        }

        /**
         * Sets the {@code apiKey} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder apiKey(String value) {
            this.apiKey = value;
            return this;
        }

        /**
         * Sets the {@code apiKeyCommand} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder apiKeyCommand(String value) {
            this.apiKeyCommand = value;
            return this;
        }

        /**
         * Sets the {@code bearerToken} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder bearerToken(String value) {
            this.bearerToken = value;
            return this;
        }

        /**
         * Sets the {@code azure} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder azure(ProviderConfigAzure value) {
            this.azure = value;
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
         * Sets the {@code providerName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder providerName(String value) {
            this.providerName = value;
            return this;
        }

        /**
         * Sets the {@code modelProvider} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder modelProvider(ProviderConfigModelProvider value) {
            this.modelProvider = value;
            return this;
        }

        /**
         * Sets the {@code wireModel} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder wireModel(String value) {
            this.wireModel = value;
            return this;
        }

        /**
         * Sets the {@code maxPromptTokens} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder maxPromptTokens(Double value) {
            this.maxPromptTokens = value;
            return this;
        }

        /**
         * Sets the {@code maxContextWindowTokens} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder maxContextWindowTokens(Double value) {
            this.maxContextWindowTokens = value;
            return this;
        }

        /**
         * Sets the {@code maxOutputTokens} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder maxOutputTokens(Double value) {
            this.maxOutputTokens = value;
            return this;
        }

        /**
         * Sets the {@code headers} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder headers(Map<String, String> value) {
            this.headers = value;
            return this;
        }

        /**
         * Sets the {@code hasBearerTokenProvider} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder hasBearerTokenProvider(Boolean value) {
            this.hasBearerTokenProvider = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ProviderConfig build() {
            if (baseUrl == null) throw new IllegalStateException("Missing required property: baseUrl");
            return new ProviderConfig(type, wireApi, transport, baseUrl, apiKey, apiKeyCommand, bearerToken, azure, modelId, modelCapabilities, providerName, modelProvider, wireModel, maxPromptTokens, maxContextWindowTokens, maxOutputTokens, headers, hasBearerTokenProvider);
        }
    }
}

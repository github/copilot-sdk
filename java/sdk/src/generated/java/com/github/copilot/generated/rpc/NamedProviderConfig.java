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
 * External SDK input for a named custom model provider. Ingested by the native protocol boundary before host dispatch.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record NamedProviderConfig(
    /** Unique provider name used to qualify model selection IDs. */
    @JsonProperty("name") String name,
    /** Provider protocol family. */
    @JsonProperty("type") ProviderConfigType type,
    /** Wire API used to communicate with the provider. */
    @JsonProperty("wireApi") ProviderConfigWireApi wireApi,
    /** Transport used to communicate with the provider. */
    @JsonProperty("transport") ProviderConfigTransport transport,
    /** Base URL for provider API requests. */
    @JsonProperty("baseUrl") String baseUrl,
    /** Static API key used to authenticate provider requests. */
    @JsonProperty("apiKey") String apiKey,
    /** Static bearer token used to authenticate provider requests. */
    @JsonProperty("bearerToken") String bearerToken,
    /** Azure authentication configuration for the provider. */
    @JsonProperty("azure") ProviderConfigAzure azure,
    /** Additional HTTP headers included with provider requests. */
    @JsonProperty("headers") Map<String, String> headers,
    /** The product serving the provider's models, reported in telemetry as `model_provider`. Only affects telemetry. */
    @JsonProperty("modelProvider") ProviderConfigModelProvider modelProvider,
    /** Whether the host supplies bearer tokens dynamically. */
    @JsonProperty("hasBearerTokenProvider") Boolean hasBearerTokenProvider
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public NamedProviderConfig {}

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
        private String name;
        private ProviderConfigType type;
        private ProviderConfigWireApi wireApi;
        private ProviderConfigTransport transport;
        private String baseUrl;
        private String apiKey;
        private String bearerToken;
        private ProviderConfigAzure azure;
        private Map<String, String> headers;
        private ProviderConfigModelProvider modelProvider;
        private Boolean hasBearerTokenProvider;

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
         * Sets the {@code type} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
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
        public Builder apiKey(String value) {
            this.apiKey = value;
            return this;
        }

        /**
         * Sets the {@code bearerToken} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
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
        public Builder azure(ProviderConfigAzure value) {
            this.azure = value;
            return this;
        }

        /**
         * Sets the {@code headers} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder headers(Map<String, String> value) {
            this.headers = value;
            return this;
        }

        /**
         * Sets the {@code modelProvider} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder modelProvider(ProviderConfigModelProvider value) {
            this.modelProvider = value;
            return this;
        }

        /**
         * Sets the {@code hasBearerTokenProvider} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
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
        public NamedProviderConfig build() {
            if (name == null) throw new IllegalStateException("Missing required property: name");
            if (baseUrl == null) throw new IllegalStateException("Missing required property: baseUrl");
            return new NamedProviderConfig(name, type, wireApi, transport, baseUrl, apiKey, bearerToken, azure, headers, modelProvider, hasBearerTokenProvider);
        }
    }
}

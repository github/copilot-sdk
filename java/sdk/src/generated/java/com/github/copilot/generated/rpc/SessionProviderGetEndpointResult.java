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
 * A snapshot of the provider endpoint the session is currently configured to talk to.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionProviderGetEndpointResult(
    /** Provider family. Matches the `type` field of a BYOK provider config. */
    @JsonProperty("type") ProviderEndpointType type,
    /** Wire API to be used, when required for the provider type. */
    @JsonProperty("wireApi") ProviderEndpointWireApi wireApi,
    /** Transport to be used for provider requests. */
    @JsonProperty("transport") ProviderEndpointTransport transport,
    /** Base URL to pass to the LLM client library. */
    @JsonProperty("baseUrl") String baseUrl,
    /** A credential the caller should use with this endpoint. Omitted only when the endpoint accepts unauthenticated requests. */
    @JsonProperty("apiKey") String apiKey,
    /** HTTP headers the caller must include on every outbound request. */
    @JsonProperty("headers") Map<String, String> headers,
    /** Short-lived, rotating credential the caller must send on every request, in addition to `apiKey` if one is present. Omitted when the endpoint does not require one. */
    @JsonProperty("sessionToken") ProviderSessionToken sessionToken
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionProviderGetEndpointResult {}

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
        private ProviderEndpointType type;
        private ProviderEndpointWireApi wireApi;
        private ProviderEndpointTransport transport;
        private String baseUrl;
        private String apiKey;
        private Map<String, String> headers;
        private ProviderSessionToken sessionToken;

        /**
         * Sets the {@code type} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder type(ProviderEndpointType value) {
            this.type = value;
            return this;
        }

        /**
         * Sets the {@code wireApi} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder wireApi(ProviderEndpointWireApi value) {
            this.wireApi = value;
            return this;
        }

        /**
         * Sets the {@code transport} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder transport(ProviderEndpointTransport value) {
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
         * Sets the {@code sessionToken} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sessionToken(ProviderSessionToken value) {
            this.sessionToken = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionProviderGetEndpointResult build() {
            if (type == null) throw new IllegalStateException("Missing required property: type");
            if (baseUrl == null) throw new IllegalStateException("Missing required property: baseUrl");
            if (headers == null) throw new IllegalStateException("Missing required property: headers");
            return new SessionProviderGetEndpointResult(type, wireApi, transport, baseUrl, apiKey, headers, sessionToken);
        }
    }
}

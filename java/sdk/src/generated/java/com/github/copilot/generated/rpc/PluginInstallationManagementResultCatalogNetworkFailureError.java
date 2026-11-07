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
 * The runtime could not reach the catalog authority or retrieve a card. Covers being offline as well as transport-level failure.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class PluginInstallationManagementResultCatalogNetworkFailureError extends PluginInstallationManagementResult {

    @JsonProperty("kind")
    private final String kind = "network-failure";

    @Override
    public String getKind() { return kind; }

    /** Categorised failure, low cardinality so it can be aggregated without carrying a URL. */
    @JsonProperty("reason")
    private CatalogNetworkFailureReason reason;

    /** HTTP status code, when the failure was a rejected response. */
    @JsonProperty("statusCode")
    private Long statusCode;

    /** Bounded cooldown in seconds before another catalog request should be attempted, when the authority supplied a numeric Retry-After value or the runtime applied its documented fallback. */
    @JsonProperty("retryAfterSeconds")
    private Long retryAfterSeconds;

    /** Human-readable explanation, safe to surface. Never contains a query, URL, handle, or secret. */
    @JsonProperty("message")
    private String message;

    public CatalogNetworkFailureReason getReason() { return reason; }
    public void setReason(CatalogNetworkFailureReason reason) { this.reason = reason; }

    public Long getStatusCode() { return statusCode; }
    public void setStatusCode(Long statusCode) { this.statusCode = statusCode; }

    public Long getRetryAfterSeconds() { return retryAfterSeconds; }
    public void setRetryAfterSeconds(Long retryAfterSeconds) { this.retryAfterSeconds = retryAfterSeconds; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }


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
        private CatalogNetworkFailureReason reason;
        private Long statusCode;
        private Long retryAfterSeconds;
        private String message;

        /**
         * Sets the {@code reason} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder reason(CatalogNetworkFailureReason value) {
            this.reason = value;
            return this;
        }

        /**
         * Sets the {@code statusCode} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder statusCode(Long value) {
            this.statusCode = value;
            return this;
        }

        /**
         * Sets the {@code retryAfterSeconds} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder retryAfterSeconds(Long value) {
            this.retryAfterSeconds = value;
            return this;
        }

        /**
         * Sets the {@code message} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder message(String value) {
            this.message = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public PluginInstallationManagementResultCatalogNetworkFailureError build() {
            if (reason == null) throw new IllegalStateException("Missing required property: reason");
            if (message == null) throw new IllegalStateException("Missing required property: message");
            PluginInstallationManagementResultCatalogNetworkFailureError instance = new PluginInstallationManagementResultCatalogNetworkFailureError();
            instance.setReason(reason);
            instance.setStatusCode(statusCode);
            instance.setRetryAfterSeconds(retryAfterSeconds);
            instance.setMessage(message);
            return instance;
        }
    }
}

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
 * An optional catalog authentication exchange did not establish the caller's identity. Anonymous search remains supported; this refusal is reserved for an operation that cannot continue after the attempted exchange. It is distinct from `policy-rejected` and from a network failure, and the reason identifies the recovery action.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class CatalogResourceGeneratedFeedResultCatalogAuthenticationRequiredError extends CatalogResourceGeneratedFeedResult {

    @JsonProperty("kind")
    private final String kind = "authentication-required";

    @Override
    public String getKind() { return kind; }

    /** Why authentication failed. Only an expired credential justifies attempting a silent refresh; an absent or rejected credential requires sign-in. */
    @JsonProperty("reason")
    private CatalogAuthenticationRequiredReason reason;

    /** Human-readable explanation, safe to surface. Never contains a credential or token, nor a query, URL, handle, or secret. */
    @JsonProperty("message")
    private String message;

    public CatalogAuthenticationRequiredReason getReason() { return reason; }
    public void setReason(CatalogAuthenticationRequiredReason reason) { this.reason = reason; }

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
        private CatalogAuthenticationRequiredReason reason;
        private String message;

        /**
         * Sets the {@code reason} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder reason(CatalogAuthenticationRequiredReason value) {
            this.reason = value;
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
        public CatalogResourceGeneratedFeedResultCatalogAuthenticationRequiredError build() {
            if (reason == null) throw new IllegalStateException("Missing required property: reason");
            if (message == null) throw new IllegalStateException("Missing required property: message");
            CatalogResourceGeneratedFeedResultCatalogAuthenticationRequiredError instance = new CatalogResourceGeneratedFeedResultCatalogAuthenticationRequiredError();
            instance.setReason(reason);
            instance.setMessage(message);
            return instance;
        }
    }
}

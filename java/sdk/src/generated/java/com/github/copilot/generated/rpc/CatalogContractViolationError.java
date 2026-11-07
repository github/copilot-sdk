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
 * An upstream catalog response broke the wire contract. Most importantly, every result must carry exactly one of a URL or embedded data: a result carrying both, or neither, is refused here rather than being guessed at.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class CatalogContractViolationError extends CatalogSearchResult {

    @JsonProperty("kind")
    private final String kind = "contract-violation";

    @Override
    public String getKind() { return kind; }

    /** Which rule the response broke. */
    @JsonProperty("reason")
    private CatalogContractViolationReason reason;

    /** Human-readable explanation, safe to surface. Never echoes response content, nor a query, URL, handle, or secret. */
    @JsonProperty("message")
    private String message;

    public CatalogContractViolationReason getReason() { return reason; }
    public void setReason(CatalogContractViolationReason reason) { this.reason = reason; }

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
        private CatalogContractViolationReason reason;
        private String message;

        /**
         * Sets the {@code reason} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder reason(CatalogContractViolationReason value) {
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
        public CatalogContractViolationError build() {
            if (reason == null) throw new IllegalStateException("Missing required property: reason");
            if (message == null) throw new IllegalStateException("Missing required property: message");
            CatalogContractViolationError instance = new CatalogContractViolationError();
            instance.setReason(reason);
            instance.setMessage(message);
            return instance;
        }
    }
}

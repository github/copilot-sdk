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
 * Registry or enterprise policy refused the operation.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class CatalogResourceGeneratedFeedResultCatalogPolicyRejectedError extends CatalogResourceGeneratedFeedResult {

    @JsonProperty("kind")
    private final String kind = "policy-rejected";

    @Override
    public String getKind() { return kind; }

    /** Which authority produced the decision. */
    @JsonProperty("source")
    private McpPlanPolicySource source;

    /** Human-readable explanation, safe to surface. Never contains a query, URL, handle, or secret. */
    @JsonProperty("message")
    private String message;

    public McpPlanPolicySource getSource() { return source; }
    public void setSource(McpPlanPolicySource source) { this.source = source; }

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
        private McpPlanPolicySource source;
        private String message;

        /**
         * Sets the {@code source} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder source(McpPlanPolicySource value) {
            this.source = value;
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
        public CatalogResourceGeneratedFeedResultCatalogPolicyRejectedError build() {
            if (source == null) throw new IllegalStateException("Missing required property: source");
            if (message == null) throw new IllegalStateException("Missing required property: message");
            CatalogResourceGeneratedFeedResultCatalogPolicyRejectedError instance = new CatalogResourceGeneratedFeedResultCatalogPolicyRejectedError();
            instance.setSource(source);
            instance.setMessage(message);
            return instance;
        }
    }
}

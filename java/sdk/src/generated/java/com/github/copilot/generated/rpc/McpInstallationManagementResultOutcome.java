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
 * Variant {@code outcome} of {@link McpInstallationManagementResult}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class McpInstallationManagementResultOutcome extends McpInstallationManagementResult {

    @JsonProperty("kind")
    private final String kind = "outcome";

    @Override
    public String getKind() { return kind; }

    /** Observed management outcome. */
    @JsonProperty("outcome")
    private McpInstallationManagementOutcome outcome;

    /** Capabilities actually honoured for this request. */
    @JsonProperty("negotiated")
    private CatalogNegotiatedContract negotiated;

    public McpInstallationManagementOutcome getOutcome() { return outcome; }
    public void setOutcome(McpInstallationManagementOutcome outcome) { this.outcome = outcome; }

    public CatalogNegotiatedContract getNegotiated() { return negotiated; }
    public void setNegotiated(CatalogNegotiatedContract negotiated) { this.negotiated = negotiated; }


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
        private McpInstallationManagementOutcome outcome;
        private CatalogNegotiatedContract negotiated;

        /**
         * Sets the {@code outcome} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder outcome(McpInstallationManagementOutcome value) {
            this.outcome = value;
            return this;
        }

        /**
         * Sets the {@code negotiated} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder negotiated(CatalogNegotiatedContract value) {
            this.negotiated = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public McpInstallationManagementResultOutcome build() {
            if (outcome == null) throw new IllegalStateException("Missing required property: outcome");
            if (negotiated == null) throw new IllegalStateException("Missing required property: negotiated");
            McpInstallationManagementResultOutcome instance = new McpInstallationManagementResultOutcome();
            instance.setOutcome(outcome);
            instance.setNegotiated(negotiated);
            return instance;
        }
    }
}

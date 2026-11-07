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
 * Outcome of evaluating the planned server against registry and enterprise policy. Evaluation is read-only.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpPlanPolicyResult(
    /** What policy decided for this server. */
    @JsonProperty("decision") McpPlanPolicyDecision decision,
    /** Which authority produced the decision. */
    @JsonProperty("source") McpPlanPolicySource source,
    /** Human-readable explanation, safe to surface. Never contains a query, URL, handle, or secret. */
    @JsonProperty("reason") String reason
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public McpPlanPolicyResult {}

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
        private McpPlanPolicyDecision decision;
        private McpPlanPolicySource source;
        private String reason;

        /**
         * Sets the {@code decision} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder decision(McpPlanPolicyDecision value) {
            this.decision = value;
            return this;
        }

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
         * Sets the {@code reason} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder reason(String value) {
            this.reason = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public McpPlanPolicyResult build() {
            if (decision == null) throw new IllegalStateException("Missing required property: decision");
            if (source == null) throw new IllegalStateException("Missing required property: source");
            return new McpPlanPolicyResult(decision, source, reason);
        }
    }
}

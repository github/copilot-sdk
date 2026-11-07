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
 * Requested and resolved context configuration observed when a direct workflow agent launched.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkflowAgentContextObservation(
    /** Context-tier override explicitly supplied by the workflow call. */
    @JsonProperty("requestedTier") ContextTier requestedTier,
    /** Effective context-tier identifier reported by the initialized child session. Native sessions currently report `default` or `long_context`; remote participants may report another supported identifier. */
    @JsonProperty("resolvedTier") String resolvedTier,
    /** Effective prompt-token input budget resolved for the initialized child session. */
    @JsonProperty("promptTokenLimit") Long promptTokenLimit,
    /** Full context-window capacity resolved for the initialized child session. */
    @JsonProperty("maxContextWindowTokens") Long maxContextWindowTokens
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public WorkflowAgentContextObservation {}

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
        private ContextTier requestedTier;
        private String resolvedTier;
        private Long promptTokenLimit;
        private Long maxContextWindowTokens;

        /**
         * Sets the {@code requestedTier} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder requestedTier(ContextTier value) {
            this.requestedTier = value;
            return this;
        }

        /**
         * Sets the {@code resolvedTier} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder resolvedTier(String value) {
            this.resolvedTier = value;
            return this;
        }

        /**
         * Sets the {@code promptTokenLimit} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder promptTokenLimit(Long value) {
            this.promptTokenLimit = value;
            return this;
        }

        /**
         * Sets the {@code maxContextWindowTokens} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder maxContextWindowTokens(Long value) {
            this.maxContextWindowTokens = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public WorkflowAgentContextObservation build() {
            return new WorkflowAgentContextObservation(requestedTier, resolvedTier, promptTokenLimit, maxContextWindowTokens);
        }
    }
}

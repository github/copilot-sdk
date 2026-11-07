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
 * Public, persistence-independent projection of an autopilot objective.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AutopilotObjectiveState(
    /** Session-local objective identifier. */
    @JsonProperty("id") Long id,
    /** User-provided objective text. */
    @JsonProperty("objective") String objective,
    /** Current normalized lifecycle status. */
    @JsonProperty("status") AutopilotObjectiveStatus status,
    /** Number of objective turns started. */
    @JsonProperty("turnCount") Long turnCount,
    /** Optional reason the objective is paused. */
    @JsonProperty("pauseReason") String pauseReason,
    /** Optional summary recorded when the objective completed. */
    @JsonProperty("completionSummary") String completionSummary,
    /** Exact lifetime AI-credit consumption in non-negative integer nano-AIU, encoded as a decimal string. */
    @JsonProperty("creditCountNanoAiu") String creditCountNanoAiu,
    /** Current per-window consumption and optional cap, when a credit-tracking window is present. */
    @JsonProperty("creditLimit") AutopilotObjectiveCreditLimit creditLimit
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public AutopilotObjectiveState {}

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
        private Long id;
        private String objective;
        private AutopilotObjectiveStatus status;
        private Long turnCount;
        private String pauseReason;
        private String completionSummary;
        private String creditCountNanoAiu;
        private AutopilotObjectiveCreditLimit creditLimit;

        /**
         * Sets the {@code id} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder id(Long value) {
            this.id = value;
            return this;
        }

        /**
         * Sets the {@code objective} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder objective(String value) {
            this.objective = value;
            return this;
        }

        /**
         * Sets the {@code status} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder status(AutopilotObjectiveStatus value) {
            this.status = value;
            return this;
        }

        /**
         * Sets the {@code turnCount} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder turnCount(Long value) {
            this.turnCount = value;
            return this;
        }

        /**
         * Sets the {@code pauseReason} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder pauseReason(String value) {
            this.pauseReason = value;
            return this;
        }

        /**
         * Sets the {@code completionSummary} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder completionSummary(String value) {
            this.completionSummary = value;
            return this;
        }

        /**
         * Sets the {@code creditCountNanoAiu} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder creditCountNanoAiu(String value) {
            this.creditCountNanoAiu = value;
            return this;
        }

        /**
         * Sets the {@code creditLimit} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder creditLimit(AutopilotObjectiveCreditLimit value) {
            this.creditLimit = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AutopilotObjectiveState build() {
            if (id == null) throw new IllegalStateException("Missing required property: id");
            if (objective == null) throw new IllegalStateException("Missing required property: objective");
            if (status == null) throw new IllegalStateException("Missing required property: status");
            if (turnCount == null) throw new IllegalStateException("Missing required property: turnCount");
            if (creditCountNanoAiu == null) throw new IllegalStateException("Missing required property: creditCountNanoAiu");
            return new AutopilotObjectiveState(id, objective, status, turnCount, pauseReason, completionSummary, creditCountNanoAiu, creditLimit);
        }
    }
}

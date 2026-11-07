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
 * Durable lifecycle and timing for one workflow phase.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkflowPhaseObservation(
    /** Phase identifier. */
    @JsonProperty("id") String id,
    /** Zero-based declared phase ordinal, or null for an undeclared phase. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("ordinal") Long ordinal,
    /** Human-readable phase title. */
    @JsonProperty("title") String title,
    /** Optional human-readable phase detail. */
    @JsonProperty("detail") String detail,
    /** Derived lifecycle state of the phase. */
    @JsonProperty("status") WorkflowPhaseStatus status,
    /** Most recent run attempt that entered this phase, or `0` if the phase has never been entered. */
    @JsonProperty("lastEnteredRunAttempt") Long lastEnteredRunAttempt,
    /** Number of times execution entered this phase. */
    @JsonProperty("entryCount") Long entryCount,
    /** Epoch milliseconds when this phase first started; for a skipped phase, the synthetic skip timestamp (equal to `completedAt`). */
    @JsonProperty("startedAt") Long startedAt,
    /** Epoch milliseconds when this phase completed; for a skipped phase, the synthetic skip timestamp (equal to `startedAt`). */
    @JsonProperty("completedAt") Long completedAt,
    /** Completed active time accumulated by this phase in milliseconds. */
    @JsonProperty("accumulatedActiveMs") Long accumulatedActiveMs,
    /** Current live active time for this phase in milliseconds. */
    @JsonProperty("currentActiveMs") Long currentActiveMs,
    /** Total direct agents associated with this phase. */
    @JsonProperty("totalAgentCount") Long totalAgentCount,
    /** Direct agents in this phase that are currently live. */
    @JsonProperty("liveAgentCount") Long liveAgentCount
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public WorkflowPhaseObservation {}

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
        private String id;
        private Long ordinal;
        private boolean $presence1;
        private String title;
        private String detail;
        private WorkflowPhaseStatus status;
        private Long lastEnteredRunAttempt;
        private Long entryCount;
        private Long startedAt;
        private Long completedAt;
        private Long accumulatedActiveMs;
        private Long currentActiveMs;
        private Long totalAgentCount;
        private Long liveAgentCount;

        /**
         * Sets the {@code id} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder id(String value) {
            this.id = value;
            return this;
        }

        /**
         * Sets the {@code ordinal} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder ordinal(Long value) {
            this.ordinal = value;
            this.$presence1 = true;
            return this;
        }

        /**
         * Sets the {@code title} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder title(String value) {
            this.title = value;
            return this;
        }

        /**
         * Sets the {@code detail} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder detail(String value) {
            this.detail = value;
            return this;
        }

        /**
         * Sets the {@code status} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder status(WorkflowPhaseStatus value) {
            this.status = value;
            return this;
        }

        /**
         * Sets the {@code lastEnteredRunAttempt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder lastEnteredRunAttempt(Long value) {
            this.lastEnteredRunAttempt = value;
            return this;
        }

        /**
         * Sets the {@code entryCount} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder entryCount(Long value) {
            this.entryCount = value;
            return this;
        }

        /**
         * Sets the {@code startedAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder startedAt(Long value) {
            this.startedAt = value;
            return this;
        }

        /**
         * Sets the {@code completedAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder completedAt(Long value) {
            this.completedAt = value;
            return this;
        }

        /**
         * Sets the {@code accumulatedActiveMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder accumulatedActiveMs(Long value) {
            this.accumulatedActiveMs = value;
            return this;
        }

        /**
         * Sets the {@code currentActiveMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder currentActiveMs(Long value) {
            this.currentActiveMs = value;
            return this;
        }

        /**
         * Sets the {@code totalAgentCount} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder totalAgentCount(Long value) {
            this.totalAgentCount = value;
            return this;
        }

        /**
         * Sets the {@code liveAgentCount} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder liveAgentCount(Long value) {
            this.liveAgentCount = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public WorkflowPhaseObservation build() {
            if (id == null) throw new IllegalStateException("Missing required property: id");
            if (!$presence1) throw new IllegalStateException("Missing required property: ordinal");
            if (title == null) throw new IllegalStateException("Missing required property: title");
            if (status == null) throw new IllegalStateException("Missing required property: status");
            if (lastEnteredRunAttempt == null) throw new IllegalStateException("Missing required property: lastEnteredRunAttempt");
            if (entryCount == null) throw new IllegalStateException("Missing required property: entryCount");
            if (accumulatedActiveMs == null) throw new IllegalStateException("Missing required property: accumulatedActiveMs");
            if (currentActiveMs == null) throw new IllegalStateException("Missing required property: currentActiveMs");
            if (totalAgentCount == null) throw new IllegalStateException("Missing required property: totalAgentCount");
            if (liveAgentCount == null) throw new IllegalStateException("Missing required property: liveAgentCount");
            return new WorkflowPhaseObservation(id, ordinal, title, detail, status, lastEnteredRunAttempt, entryCount, startedAt, completedAt, accumulatedActiveMs, currentActiveMs, totalAgentCount, liveAgentCount);
        }
    }
}

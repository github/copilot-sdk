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
 * Durable workflow run summary with read-time live overlays.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkflowRunSummary(
    /** Workflow run identifier. */
    @JsonProperty("runId") String runId,
    /** Registered workflow name. */
    @JsonProperty("workflowName") String workflowName,
    /** Human-readable workflow description. */
    @JsonProperty("description") String description,
    /** Current workflow run status. */
    @JsonProperty("status") WorkflowRunStatus status,
    /** Monotonic durable run revision. */
    @JsonProperty("revision") Long revision,
    /** Epoch milliseconds when the run was created. */
    @JsonProperty("createdAt") Long createdAt,
    /** Epoch milliseconds when execution first started, or null before start. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("startedAt") Long startedAt,
    /** Epoch milliseconds when the durable run was last updated. */
    @JsonProperty("updatedAt") Long updatedAt,
    /** Epoch milliseconds when the run completed, or null while nonterminal. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("completedAt") Long completedAt,
    /** Current phase identity, or null before any phase is entered. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("currentPhase") WorkflowCurrentPhase currentPhase,
    /** Number of phases declared by the workflow. */
    @JsonProperty("declaredPhaseCount") Long declaredPhaseCount,
    /** Number of direct workflow agents currently live. */
    @JsonProperty("liveAgentCount") Long liveAgentCount,
    /** Total direct workflow agents spawned across all attempts. */
    @JsonProperty("totalSpawnedAgentCount") Long totalSpawnedAgentCount,
    /** Durable resource consumption. */
    @JsonProperty("consumed") WorkflowRunConsumed consumed,
    /** Resource ceilings declared by the workflow. */
    @JsonProperty("declaredLimits") WorkflowDeclaredLimits declaredLimits,
    /** Approved effective resource ceilings, or null until approved. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("approved") WorkflowDeclaredLimits approved,
    /** Epoch milliseconds when this live-overlay snapshot was observed. */
    @JsonProperty("observedAt") Long observedAt,
    /** Epoch milliseconds when the current active segment started, or null while inactive. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("activeSegmentStartedAt") Long activeSegmentStartedAt,
    /** Terminal run outcome, or null while nonterminal. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("terminal") WorkflowRunTerminal terminal,
    /** Whether the durable run state currently passes runtime resume eligibility checks. */
    @JsonProperty("canResume") Boolean canResume
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public WorkflowRunSummary {}

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
        private String runId;
        private String workflowName;
        private String description;
        private WorkflowRunStatus status;
        private Long revision;
        private Long createdAt;
        private Long startedAt;
        private boolean $presence6;
        private Long updatedAt;
        private Long completedAt;
        private boolean $presence8;
        private WorkflowCurrentPhase currentPhase;
        private boolean $presence9;
        private Long declaredPhaseCount;
        private Long liveAgentCount;
        private Long totalSpawnedAgentCount;
        private WorkflowRunConsumed consumed;
        private WorkflowDeclaredLimits declaredLimits;
        private WorkflowDeclaredLimits approved;
        private boolean $presence15;
        private Long observedAt;
        private Long activeSegmentStartedAt;
        private boolean $presence17;
        private WorkflowRunTerminal terminal;
        private boolean $presence18;
        private Boolean canResume;

        /**
         * Sets the {@code runId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder runId(String value) {
            this.runId = value;
            return this;
        }

        /**
         * Sets the {@code workflowName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder workflowName(String value) {
            this.workflowName = value;
            return this;
        }

        /**
         * Sets the {@code description} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder description(String value) {
            this.description = value;
            return this;
        }

        /**
         * Sets the {@code status} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder status(WorkflowRunStatus value) {
            this.status = value;
            return this;
        }

        /**
         * Sets the {@code revision} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder revision(Long value) {
            this.revision = value;
            return this;
        }

        /**
         * Sets the {@code createdAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder createdAt(Long value) {
            this.createdAt = value;
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
            this.$presence6 = true;
            return this;
        }

        /**
         * Sets the {@code updatedAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder updatedAt(Long value) {
            this.updatedAt = value;
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
            this.$presence8 = true;
            return this;
        }

        /**
         * Sets the {@code currentPhase} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder currentPhase(WorkflowCurrentPhase value) {
            this.currentPhase = value;
            this.$presence9 = true;
            return this;
        }

        /**
         * Sets the {@code declaredPhaseCount} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder declaredPhaseCount(Long value) {
            this.declaredPhaseCount = value;
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
         * Sets the {@code totalSpawnedAgentCount} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder totalSpawnedAgentCount(Long value) {
            this.totalSpawnedAgentCount = value;
            return this;
        }

        /**
         * Sets the {@code consumed} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder consumed(WorkflowRunConsumed value) {
            this.consumed = value;
            return this;
        }

        /**
         * Sets the {@code declaredLimits} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder declaredLimits(WorkflowDeclaredLimits value) {
            this.declaredLimits = value;
            return this;
        }

        /**
         * Sets the {@code approved} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder approved(WorkflowDeclaredLimits value) {
            this.approved = value;
            this.$presence15 = true;
            return this;
        }

        /**
         * Sets the {@code observedAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder observedAt(Long value) {
            this.observedAt = value;
            return this;
        }

        /**
         * Sets the {@code activeSegmentStartedAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder activeSegmentStartedAt(Long value) {
            this.activeSegmentStartedAt = value;
            this.$presence17 = true;
            return this;
        }

        /**
         * Sets the {@code terminal} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder terminal(WorkflowRunTerminal value) {
            this.terminal = value;
            this.$presence18 = true;
            return this;
        }

        /**
         * Sets the {@code canResume} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder canResume(Boolean value) {
            this.canResume = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public WorkflowRunSummary build() {
            if (runId == null) throw new IllegalStateException("Missing required property: runId");
            if (workflowName == null) throw new IllegalStateException("Missing required property: workflowName");
            if (description == null) throw new IllegalStateException("Missing required property: description");
            if (status == null) throw new IllegalStateException("Missing required property: status");
            if (revision == null) throw new IllegalStateException("Missing required property: revision");
            if (createdAt == null) throw new IllegalStateException("Missing required property: createdAt");
            if (!$presence6) throw new IllegalStateException("Missing required property: startedAt");
            if (updatedAt == null) throw new IllegalStateException("Missing required property: updatedAt");
            if (!$presence8) throw new IllegalStateException("Missing required property: completedAt");
            if (!$presence9) throw new IllegalStateException("Missing required property: currentPhase");
            if (declaredPhaseCount == null) throw new IllegalStateException("Missing required property: declaredPhaseCount");
            if (liveAgentCount == null) throw new IllegalStateException("Missing required property: liveAgentCount");
            if (totalSpawnedAgentCount == null) throw new IllegalStateException("Missing required property: totalSpawnedAgentCount");
            if (consumed == null) throw new IllegalStateException("Missing required property: consumed");
            if (declaredLimits == null) throw new IllegalStateException("Missing required property: declaredLimits");
            if (!$presence15) throw new IllegalStateException("Missing required property: approved");
            if (observedAt == null) throw new IllegalStateException("Missing required property: observedAt");
            if (!$presence17) throw new IllegalStateException("Missing required property: activeSegmentStartedAt");
            if (!$presence18) throw new IllegalStateException("Missing required property: terminal");
            if (canResume == null) throw new IllegalStateException("Missing required property: canResume");
            return new WorkflowRunSummary(runId, workflowName, description, status, revision, createdAt, startedAt, updatedAt, completedAt, currentPhase, declaredPhaseCount, liveAgentCount, totalSpawnedAgentCount, consumed, declaredLimits, approved, observedAt, activeSegmentStartedAt, terminal, canResume);
        }
    }
}

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
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Task completion notification with summary from the agent
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionToolsTaskCompleteEventDataResult(
    /** Summary of the completed task, provided by the agent */
    @JsonProperty("summary") String summary,
    /** Whether the task was accepted as complete. False when validation failed or completion was rejected or blocked by the reviewer */
    @JsonProperty("success") Boolean success,
    /** Semantic completion decision. Absent on legacy events and invalid tool calls */
    @JsonProperty("outcome") TaskCompletionOutcome outcome,
    /** Label-safe runtime rationale for the completion decision (e.g. a cancellation or pause/resume downgrade), when one applies. Reviewer-authored rationale is intentionally omitted here because this event has no IFC label channel; the reviewer's findings remain available through its own labeled sub-agent events */
    @JsonProperty("reason") String reason,
    /** Active autopilot objective ID evaluated by the completion reviewer */
    @JsonProperty("objectiveId") Long objectiveId,
    /** Permission-protected actions that remained blocked for this genuine-user-message epoch. Present on successful completion when the objective was completed without them and on blocked completion when one or more were required. */
    @JsonProperty("blockedActions") List<BlockedPermissionAction> blockedActions,
    /** Structured blocker details when outcome is blocked */
    @JsonProperty("blocker") TaskBlocker blocker
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionToolsTaskCompleteEventDataResult {}

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
        private String summary;
        private Boolean success;
        private TaskCompletionOutcome outcome;
        private String reason;
        private Long objectiveId;
        private List<BlockedPermissionAction> blockedActions;
        private TaskBlocker blocker;

        /**
         * Sets the {@code summary} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder summary(String value) {
            this.summary = value;
            return this;
        }

        /**
         * Sets the {@code success} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder success(Boolean value) {
            this.success = value;
            return this;
        }

        /**
         * Sets the {@code outcome} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder outcome(TaskCompletionOutcome value) {
            this.outcome = value;
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
         * Sets the {@code objectiveId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder objectiveId(Long value) {
            this.objectiveId = value;
            return this;
        }

        /**
         * Sets the {@code blockedActions} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder blockedActions(List<BlockedPermissionAction> value) {
            this.blockedActions = value;
            return this;
        }

        /**
         * Sets the {@code blocker} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder blocker(TaskBlocker value) {
            this.blocker = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionToolsTaskCompleteEventDataResult build() {
            return new SessionToolsTaskCompleteEventDataResult(summary, success, outcome, reason, objectiveId, blockedActions, blocker);
        }
    }

    /**
     * Creates a record with the components it had before later optional fields were added.
     * @deprecated Use named construction via {@link #builder()}.
     * @param summary Summary of the completed task, provided by the agent
     * @param success Whether the task was accepted as complete. False when validation failed or completion was rejected or blocked by the reviewer
     * @param outcome Semantic completion decision. Absent on legacy events and invalid tool calls
     * @param reason Label-safe runtime rationale for the completion decision (e.g. a cancellation or pause/resume downgrade), when one applies. Reviewer-authored rationale is intentionally omitted here because this event has no IFC label channel; the reviewer's findings remain available through its own labeled sub-agent events
     * @param objectiveId Active autopilot objective ID evaluated by the completion reviewer
     * @param blocker Structured blocker details when outcome is blocked
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionToolsTaskCompleteEventDataResult(
        String summary,
        Boolean success,
        TaskCompletionOutcome outcome,
        String reason,
        Long objectiveId,
        TaskBlocker blocker
    ) {
        this(summary, success, outcome, reason, objectiveId, null, blocker);
    }
}

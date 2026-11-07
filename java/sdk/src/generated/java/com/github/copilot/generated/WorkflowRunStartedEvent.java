/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Session event "workflow.run_started". Ephemeral signal that a workflow run attempt began executing.
 *
 * @apiNote This event type is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class WorkflowRunStartedEvent extends SessionEvent {

    @Override
    public String getType() { return "workflow.run_started"; }

    @JsonProperty("data")
    private WorkflowRunStartedEventData data;

    public WorkflowRunStartedEventData getData() { return data; }
    public void setData(WorkflowRunStartedEventData data) { this.data = data; }

    /** Data payload for {@link WorkflowRunStartedEvent}. */
    @CopilotExperimental
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record WorkflowRunStartedEventData(
        /** Identifier of the workflow run that started. */
        @JsonProperty("runId") String runId,
        /** Name of the workflow this run executes. Low cardinality by construction. */
        @JsonProperty("workflowName") String workflowName,
        /** Attempt number this start committed; a resumed run increments it. */
        @JsonProperty("attempt") Long attempt
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public WorkflowRunStartedEventData {}

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
            private Long attempt;

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
             * Sets the {@code attempt} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder attempt(Long value) {
                this.attempt = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public WorkflowRunStartedEventData build() {
                if (runId == null) throw new IllegalStateException("Missing required property: runId");
                if (workflowName == null) throw new IllegalStateException("Missing required property: workflowName");
                if (attempt == null) throw new IllegalStateException("Missing required property: attempt");
                return new WorkflowRunStartedEventData(runId, workflowName, attempt);
            }
        }
    }
}

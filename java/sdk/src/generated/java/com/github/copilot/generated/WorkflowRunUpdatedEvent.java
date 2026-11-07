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
 * Session event "workflow.run_updated". Ephemeral invalidation signal for a changed workflow run.
 *
 * @apiNote This event type is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class WorkflowRunUpdatedEvent extends SessionEvent {

    @Override
    public String getType() { return "workflow.run_updated"; }

    @JsonProperty("data")
    private WorkflowRunUpdatedEventData data;

    public WorkflowRunUpdatedEventData getData() { return data; }
    public void setData(WorkflowRunUpdatedEventData data) { this.data = data; }

    /** Data payload for {@link WorkflowRunUpdatedEvent}. */
    @CopilotExperimental
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record WorkflowRunUpdatedEventData(
        /** Workflow run identifier. */
        @JsonProperty("runId") String runId,
        /** Monotonic revision now available for the run. */
        @JsonProperty("revision") Long revision
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public WorkflowRunUpdatedEventData {}

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
            private Long revision;

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
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public WorkflowRunUpdatedEventData build() {
                if (runId == null) throw new IllegalStateException("Missing required property: runId");
                if (revision == null) throw new IllegalStateException("Missing required property: revision");
                return new WorkflowRunUpdatedEventData(runId, revision);
            }
        }
    }
}

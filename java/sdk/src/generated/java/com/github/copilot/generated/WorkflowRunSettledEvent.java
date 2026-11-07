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
 * Session event "workflow.run_settled". Ephemeral signal that a workflow run reached a terminal status.
 *
 * @apiNote This event type is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class WorkflowRunSettledEvent extends SessionEvent {

    @Override
    public String getType() { return "workflow.run_settled"; }

    @JsonProperty("data")
    private WorkflowRunSettledEventData data;

    public WorkflowRunSettledEventData getData() { return data; }
    public void setData(WorkflowRunSettledEventData data) { this.data = data; }

    /** Data payload for {@link WorkflowRunSettledEvent}. */
    @CopilotExperimental
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record WorkflowRunSettledEventData(
        /** Identifier of the workflow run that settled. */
        @JsonProperty("runId") String runId,
        /** Terminal status the run committed. */
        @JsonProperty("status") WorkflowRunSettledStatus status,
        /** Subagents this run consumed against its limits. */
        @JsonProperty("consumedSubagents") Long consumedSubagents,
        /** AI credits this run consumed, in nano-AIU. */
        @JsonProperty("consumedNanoAiu") Long consumedNanoAiu,
        /** Active milliseconds accumulated across every attempt of this run. */
        @JsonProperty("elapsedMs") Long elapsedMs,
        /** Typed failure class recorded on the run, when it failed with one (e.g. `workflow_limit_reached`). */
        @JsonProperty("failureType") String failureType
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public WorkflowRunSettledEventData {}

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
            private WorkflowRunSettledStatus status;
            private Long consumedSubagents;
            private Long consumedNanoAiu;
            private Long elapsedMs;
            private String failureType;

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
             * Sets the {@code status} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder status(WorkflowRunSettledStatus value) {
                this.status = value;
                return this;
            }

            /**
             * Sets the {@code consumedSubagents} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder consumedSubagents(Long value) {
                this.consumedSubagents = value;
                return this;
            }

            /**
             * Sets the {@code consumedNanoAiu} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder consumedNanoAiu(Long value) {
                this.consumedNanoAiu = value;
                return this;
            }

            /**
             * Sets the {@code elapsedMs} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder elapsedMs(Long value) {
                this.elapsedMs = value;
                return this;
            }

            /**
             * Sets the {@code failureType} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder failureType(String value) {
                this.failureType = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public WorkflowRunSettledEventData build() {
                if (runId == null) throw new IllegalStateException("Missing required property: runId");
                if (status == null) throw new IllegalStateException("Missing required property: status");
                if (consumedSubagents == null) throw new IllegalStateException("Missing required property: consumedSubagents");
                if (consumedNanoAiu == null) throw new IllegalStateException("Missing required property: consumedNanoAiu");
                if (elapsedMs == null) throw new IllegalStateException("Missing required property: elapsedMs");
                return new WorkflowRunSettledEventData(runId, status, consumedSubagents, consumedNanoAiu, elapsedMs, failureType);
            }
        }
    }
}

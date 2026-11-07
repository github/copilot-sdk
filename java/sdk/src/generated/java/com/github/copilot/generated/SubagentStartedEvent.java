/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Session event "subagent.started". Sub-agent startup details including parent tool call and agent information
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SubagentStartedEvent extends SessionEvent {

    @Override
    public String getType() { return "subagent.started"; }

    @JsonProperty("data")
    private SubagentStartedEventData data;

    public SubagentStartedEventData getData() { return data; }
    public void setData(SubagentStartedEventData data) { this.data = data; }

    /** Data payload for {@link SubagentStartedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SubagentStartedEventData(
        /** Tool call ID of the parent tool invocation that spawned this sub-agent */
        @JsonProperty("toolCallId") String toolCallId,
        /** Internal name of the sub-agent */
        @JsonProperty("agentName") String agentName,
        /** Human-readable display name of the sub-agent */
        @JsonProperty("agentDisplayName") String agentDisplayName,
        /** Description of what the sub-agent does */
        @JsonProperty("agentDescription") String agentDescription,
        /** Model the sub-agent will run with, when known at start. */
        @JsonProperty("model") String model,
        /** Where the model input for this sub-agent came from. Present when the task planner resolved the launch (the task tool and workflow agents); absent for sub-agents created through other runtime paths. */
        @JsonProperty("taskModelSource") SubagentTaskModelSource taskModelSource,
        /** Authority or runtime mechanism responsible for sub-agent model selection, when known at start. */
        @JsonProperty("modelSelectionSource") SubagentModelSelectionSource modelSelectionSource,
        /** Root id of the workflow run that spawned this sub-agent, when it was spawned by one. */
        @JsonProperty("workflowRunId") String workflowRunId,
        /** Task-registry ID of the spawning sub-agent. Absent when the root session spawned this child. */
        @JsonProperty("parentId") String parentId,
        /** Whether this sub-agent can be resumed. Currently always false. */
        @JsonProperty("resumable") Boolean resumable,
        /** Type of the sub-agent selected at spawn time. */
        @JsonProperty("agentType") String agentType,
        /** Whether the sub-agent runs synchronously or in the background. */
        @JsonProperty("executionMode") String executionMode
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SubagentStartedEventData {}

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
            private String toolCallId;
            private String agentName;
            private String agentDisplayName;
            private String agentDescription;
            private String model;
            private SubagentTaskModelSource taskModelSource;
            private SubagentModelSelectionSource modelSelectionSource;
            private String workflowRunId;
            private String parentId;
            private Boolean resumable;
            private String agentType;
            private String executionMode;

            /**
             * Sets the {@code toolCallId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder toolCallId(String value) {
                this.toolCallId = value;
                return this;
            }

            /**
             * Sets the {@code agentName} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder agentName(String value) {
                this.agentName = value;
                return this;
            }

            /**
             * Sets the {@code agentDisplayName} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder agentDisplayName(String value) {
                this.agentDisplayName = value;
                return this;
            }

            /**
             * Sets the {@code agentDescription} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder agentDescription(String value) {
                this.agentDescription = value;
                return this;
            }

            /**
             * Sets the {@code model} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder model(String value) {
                this.model = value;
                return this;
            }

            /**
             * Sets the {@code taskModelSource} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder taskModelSource(SubagentTaskModelSource value) {
                this.taskModelSource = value;
                return this;
            }

            /**
             * Sets the {@code modelSelectionSource} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder modelSelectionSource(SubagentModelSelectionSource value) {
                this.modelSelectionSource = value;
                return this;
            }

            /**
             * Sets the {@code workflowRunId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder workflowRunId(String value) {
                this.workflowRunId = value;
                return this;
            }

            /**
             * Sets the {@code parentId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder parentId(String value) {
                this.parentId = value;
                return this;
            }

            /**
             * Sets the {@code resumable} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder resumable(Boolean value) {
                this.resumable = value;
                return this;
            }

            /**
             * Sets the {@code agentType} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder agentType(String value) {
                this.agentType = value;
                return this;
            }

            /**
             * Sets the {@code executionMode} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder executionMode(String value) {
                this.executionMode = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SubagentStartedEventData build() {
                if (toolCallId == null) throw new IllegalStateException("Missing required property: toolCallId");
                if (agentName == null) throw new IllegalStateException("Missing required property: agentName");
                if (agentDisplayName == null) throw new IllegalStateException("Missing required property: agentDisplayName");
                if (agentDescription == null) throw new IllegalStateException("Missing required property: agentDescription");
                return new SubagentStartedEventData(toolCallId, agentName, agentDisplayName, agentDescription, model, taskModelSource, modelSelectionSource, workflowRunId, parentId, resumable, agentType, executionMode);
            }
        }
    }
}

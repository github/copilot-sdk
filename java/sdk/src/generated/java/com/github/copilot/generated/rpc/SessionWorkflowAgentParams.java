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
 * Parameters for one workflow-scoped subagent call.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionWorkflowAgentParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Workflow run identifier that owns the subagent. */
    @JsonProperty("workflowRunId") String workflowRunId,
    /** Opaque token identifying the current workflow execution attempt. */
    @JsonProperty("executionToken") String executionToken,
    /** Prompt to send to the subagent. */
    @JsonProperty("prompt") String prompt,
    /** Subagent execution options. */
    @JsonProperty("opts") WorkflowAgentOptions opts
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionWorkflowAgentParams {}

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
        private String sessionId;
        private String workflowRunId;
        private String executionToken;
        private String prompt;
        private WorkflowAgentOptions opts;

        /**
         * Sets the {@code sessionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder sessionId(String value) {
            this.sessionId = value;
            return this;
        }

        /**
         * Sets the {@code workflowRunId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder workflowRunId(String value) {
            this.workflowRunId = value;
            return this;
        }

        /**
         * Sets the {@code executionToken} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder executionToken(String value) {
            this.executionToken = value;
            return this;
        }

        /**
         * Sets the {@code prompt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder prompt(String value) {
            this.prompt = value;
            return this;
        }

        /**
         * Sets the {@code opts} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder opts(WorkflowAgentOptions value) {
            this.opts = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionWorkflowAgentParams build() {
            if (workflowRunId == null) throw new IllegalStateException("Missing required property: workflowRunId");
            if (executionToken == null) throw new IllegalStateException("Missing required property: executionToken");
            if (prompt == null) throw new IllegalStateException("Missing required property: prompt");
            if (opts == null) throw new IllegalStateException("Missing required property: opts");
            return new SessionWorkflowAgentParams(sessionId, workflowRunId, executionToken, prompt, opts);
        }
    }
}

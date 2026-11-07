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
 * Internal parameters for invoking a registered workflow from a tool.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record SessionWorkflowRunFromToolParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Registered workflow name. */
    @JsonProperty("name") String name,
    /** Workflow input value. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("args") Object args,
    /** Tool-originated workflow invocation options. */
    @JsonProperty("options") WorkflowToolRunOptions options,
    /** Opaque identifier of the originating tool call. */
    @JsonProperty("toolCallId") String toolCallId
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    SessionWorkflowRunFromToolParams {}

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
        private String name;
        private Object args;
        private boolean $presence2;
        private WorkflowToolRunOptions options;
        private String toolCallId;

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
         * Sets the {@code name} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder name(String value) {
            this.name = value;
            return this;
        }

        /**
         * Sets the {@code args} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder args(Object value) {
            this.args = value;
            this.$presence2 = true;
            return this;
        }

        /**
         * Sets the {@code options} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder options(WorkflowToolRunOptions value) {
            this.options = value;
            return this;
        }

        /**
         * Sets the {@code toolCallId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder toolCallId(String value) {
            this.toolCallId = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionWorkflowRunFromToolParams build() {
            if (name == null) throw new IllegalStateException("Missing required property: name");
            if (!$presence2) throw new IllegalStateException("Missing required property: args");
            return new SessionWorkflowRunFromToolParams(sessionId, name, args, options, toolCallId);
        }
    }
}

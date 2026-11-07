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
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Parameters for sending a user message to the session
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record SessionSendAndWaitParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** The user message text */
    @JsonProperty("prompt") String prompt,
    /** If provided, this is shown in the timeline instead of `prompt` */
    @JsonProperty("displayPrompt") String displayPrompt,
    /** Optional attachments (files, directories, selections, blobs, GitHub references) to include with the message */
    @JsonProperty("attachments") List<Object> attachments,
    /** How to deliver the message. `enqueue` (default) appends to the message queue. `immediate` interjects during an in-progress turn. */
    @JsonProperty("mode") SendMode mode,
    /** If true, adds the message to the front of the queue instead of the end */
    @JsonProperty("prepend") Boolean prepend,
    /** If false, this message will not trigger a Premium Request Unit charge. User messages default to billable. */
    @JsonProperty("billable") Boolean billable,
    /** If set, the request will fail if the named tool is not available when this message is among the user messages at the start of the current exchange */
    @JsonProperty("requiredTool") String requiredTool,
    /** Optional provenance tag copied to the resulting user.message event. Must be `user`, `system`, `command-<command-id>` for command-originated messages, `schedule-<numeric-id>` for scheduled prompts, or `agent-<agent-id>` for prompts sent by another agent. */
    @JsonProperty("source") String source,
    /** The UI mode the agent was in when this message was sent. Defaults to the session's current mode. */
    @JsonProperty("agentMode") SendAgentMode agentMode,
    /** Custom HTTP headers to include in outbound model requests for this turn. Merged with session-level provider headers; per-turn headers augment and overwrite session-level headers with the same key. */
    @JsonProperty("requestHeaders") Map<String, String> requestHeaders,
    /** Provider-native output format for this turn, including all tool-call iterations. Not inherited by later turns or subagents. Ordinary steering inherits the active format; specifying responseFormat with mode: immediate is an error, even while idle. Returned assistant content remains text; the runtime does not parse or validate it. Unsupported models or schemas produce provider errors. */
    @JsonProperty("responseFormat") SessionSendAndWaitParamsResponseFormat responseFormat,
    /** W3C Trace Context traceparent header for distributed tracing of this agent turn */
    @JsonProperty("traceparent") String traceparent,
    /** W3C Trace Context tracestate header for distributed tracing */
    @JsonProperty("tracestate") String tracestate,
    /** If true, await completion of the agentic loop for this message before returning. Defaults to false (fire-and-forget). When true, the result still contains the same `messageId`; the caller can rely on the agent having processed the message before the call resolves. Transport-dependent tail semantics: on a LOCAL (in-process) session the wait additionally blocks until the completed turn's event tail has been dispatched to this session's in-process subscribers, so a subsequent read of subscriber state already reflects the turn; on a REMOTE session the wait resolves once the loop completes and mirrored delivery follows over the wire. Callers that need the stronger local guarantee on remote sessions should await the event stream explicitly. */
    @JsonProperty("wait") Boolean wait_
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionSendAndWaitParamsResponseFormat(
        /** JSON Schema and provider options for the turn's output. */
        @JsonProperty("jsonSchema") JsonSchemaResponseFormat jsonSchema,
        /** Output format discriminator. Currently only json_schema is supported. */
        @JsonProperty("type") String type
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionSendAndWaitParamsResponseFormat {}

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
            private JsonSchemaResponseFormat jsonSchema;
            private String type;

            /**
             * Sets the {@code jsonSchema} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder jsonSchema(JsonSchemaResponseFormat value) {
                this.jsonSchema = value;
                return this;
            }

            /**
             * Sets the {@code type} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder type(String value) {
                this.type = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionSendAndWaitParamsResponseFormat build() {
                if (jsonSchema == null) throw new IllegalStateException("Missing required property: jsonSchema");
                if (type == null) throw new IllegalStateException("Missing required property: type");
                return new SessionSendAndWaitParamsResponseFormat(jsonSchema, type);
            }
        }
    }

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    SessionSendAndWaitParams {}

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
        private String prompt;
        private String displayPrompt;
        private List<Object> attachments;
        private SendMode mode;
        private Boolean prepend;
        private Boolean billable;
        private String requiredTool;
        private String source;
        private SendAgentMode agentMode;
        private Map<String, String> requestHeaders;
        private SessionSendAndWaitParamsResponseFormat responseFormat;
        private String traceparent;
        private String tracestate;
        private Boolean wait_;

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
         * Sets the {@code displayPrompt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder displayPrompt(String value) {
            this.displayPrompt = value;
            return this;
        }

        /**
         * Sets the {@code attachments} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder attachments(List<Object> value) {
            this.attachments = value;
            return this;
        }

        /**
         * Sets the {@code mode} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder mode(SendMode value) {
            this.mode = value;
            return this;
        }

        /**
         * Sets the {@code prepend} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder prepend(Boolean value) {
            this.prepend = value;
            return this;
        }

        /**
         * Sets the {@code billable} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder billable(Boolean value) {
            this.billable = value;
            return this;
        }

        /**
         * Sets the {@code requiredTool} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder requiredTool(String value) {
            this.requiredTool = value;
            return this;
        }

        /**
         * Sets the {@code source} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder source(String value) {
            this.source = value;
            return this;
        }

        /**
         * Sets the {@code agentMode} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder agentMode(SendAgentMode value) {
            this.agentMode = value;
            return this;
        }

        /**
         * Sets the {@code requestHeaders} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder requestHeaders(Map<String, String> value) {
            this.requestHeaders = value;
            return this;
        }

        /**
         * Sets the {@code responseFormat} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder responseFormat(SessionSendAndWaitParamsResponseFormat value) {
            this.responseFormat = value;
            return this;
        }

        /**
         * Sets the {@code traceparent} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder traceparent(String value) {
            this.traceparent = value;
            return this;
        }

        /**
         * Sets the {@code tracestate} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder tracestate(String value) {
            this.tracestate = value;
            return this;
        }

        /**
         * Sets the {@code wait} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder wait_(Boolean value) {
            this.wait_ = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionSendAndWaitParams build() {
            if (prompt == null) throw new IllegalStateException("Missing required property: prompt");
            return new SessionSendAndWaitParams(sessionId, prompt, displayPrompt, attachments, mode, prepend, billable, requiredTool, source, agentMode, requestHeaders, responseFormat, traceparent, tracestate, wait_);
        }
    }
}

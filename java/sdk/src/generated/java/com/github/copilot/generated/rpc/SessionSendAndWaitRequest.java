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
import java.util.Objects;
import javax.annotation.processing.Generated;

/**
 * Parameters for sending a user message to the session
 * <p>
 * Required inputs are constructor arguments. Optional inputs have fluent setters.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
final class SessionSendAndWaitRequest {

    /** The user message text */
    @JsonProperty("prompt")
    private final String prompt;

    /** If provided, this is shown in the timeline instead of `prompt` */
    @JsonProperty("displayPrompt")
    private String displayPrompt;

    /** Optional attachments (files, directories, selections, blobs, GitHub references) to include with the message */
    @JsonProperty("attachments")
    private List<Object> attachments;

    /** How to deliver the message. `enqueue` (default) appends to the message queue. `immediate` interjects during an in-progress turn. */
    @JsonProperty("mode")
    private SendMode mode;

    /** If true, adds the message to the front of the queue instead of the end */
    @JsonProperty("prepend")
    private Boolean prepend;

    /** If false, this message will not trigger a Premium Request Unit charge. User messages default to billable. */
    @JsonProperty("billable")
    private Boolean billable;

    /** If set, the request will fail if the named tool is not available when this message is among the user messages at the start of the current exchange */
    @JsonProperty("requiredTool")
    private String requiredTool;

    /** Optional provenance tag copied to the resulting user.message event. Must be `user`, `system`, `command-<command-id>` for command-originated messages, `schedule-<numeric-id>` for scheduled prompts, or `agent-<agent-id>` for prompts sent by another agent. */
    @JsonProperty("source")
    private String source;

    /** The UI mode the agent was in when this message was sent. Defaults to the session's current mode. */
    @JsonProperty("agentMode")
    private SendAgentMode agentMode;

    /** Custom HTTP headers to include in outbound model requests for this turn. Merged with session-level provider headers; per-turn headers augment and overwrite session-level headers with the same key. */
    @JsonProperty("requestHeaders")
    private Map<String, String> requestHeaders;

    /** Provider-native output format for this turn, including all tool-call iterations. Not inherited by later turns or subagents. Ordinary steering inherits the active format; specifying responseFormat with mode: immediate is an error, even while idle. Returned assistant content remains text; the runtime does not parse or validate it. Unsupported models or schemas produce provider errors. */
    @JsonProperty("responseFormat")
    private SessionSendAndWaitRequestResponseFormat responseFormat;

    /** W3C Trace Context traceparent header for distributed tracing of this agent turn */
    @JsonProperty("traceparent")
    private String traceparent;

    /** W3C Trace Context tracestate header for distributed tracing */
    @JsonProperty("tracestate")
    private String tracestate;

    /** If true, await completion of the agentic loop for this message before returning. Defaults to false (fire-and-forget). When true, the result still contains the same `messageId`; the caller can rely on the agent having processed the message before the call resolves. Transport-dependent tail semantics: on a LOCAL (in-process) session the wait additionally blocks until the completed turn's event tail has been dispatched to this session's in-process subscribers, so a subsequent read of subscriber state already reflects the turn; on a REMOTE session the wait resolves once the loop completes and mirrored delivery follows over the wire. Callers that need the stronger local guarantee on remote sessions should await the event stream explicitly. */
    @JsonProperty("wait")
    private Boolean wait_;

    /** Opaque caller-chosen ID for the turn this message starts. The runtime copies it unchanged into `params._meta["com.github.copilot/turnId"]` on every MCP `tools/call` the agent makes while serving that turn, including calls from sub-agents it spawns and from Autopilot continuations of it. Visible user steering (`mode: "immediate"`) that a running turn consumes hands it to a successor turn: the steering's value replaces the running turn's from the next model call onward, and steering without one clears it. System and hidden steering leave it unchanged. A turn started without one, including one the runtime starts itself, carries no turn ID. The `user.message` event for each message the send delivers echoes it as `clientTurnId` when the message is consumed, and is persisted like other events; the value stamped on MCP calls is held in memory only and is not restored on resume. */
    @JsonProperty("clientTurnId")
    private String clientTurnId;

    /**
     * Creates a request with its required inputs.
     *
     * @param prompt The user message text
     */
    public SessionSendAndWaitRequest(String prompt) {
        this.prompt = Objects.requireNonNull(prompt, "prompt");
    }

    /**
     * Returns the {@code prompt} property.
     *
     * @return The user message text
     */
    public String getPrompt() {
        return prompt;
    }

    /**
     * Returns the {@code displayPrompt} property.
     *
     * @return If provided, this is shown in the timeline instead of `prompt`
     */
    public String getDisplayPrompt() {
        return displayPrompt;
    }

    /**
     * Returns the {@code attachments} property.
     *
     * @return Optional attachments (files, directories, selections, blobs, GitHub references) to include with the message
     */
    public List<Object> getAttachments() {
        return attachments;
    }

    /**
     * Returns the {@code mode} property.
     *
     * @return How to deliver the message. `enqueue` (default) appends to the message queue. `immediate` interjects during an in-progress turn.
     */
    public SendMode getMode() {
        return mode;
    }

    /**
     * Returns the {@code prepend} property.
     *
     * @return If true, adds the message to the front of the queue instead of the end
     */
    public Boolean getPrepend() {
        return prepend;
    }

    /**
     * Returns the {@code billable} property.
     *
     * @return If false, this message will not trigger a Premium Request Unit charge. User messages default to billable.
     */
    public Boolean getBillable() {
        return billable;
    }

    /**
     * Returns the {@code requiredTool} property.
     *
     * @return If set, the request will fail if the named tool is not available when this message is among the user messages at the start of the current exchange
     */
    public String getRequiredTool() {
        return requiredTool;
    }

    /**
     * Returns the {@code source} property.
     *
     * @return Optional provenance tag copied to the resulting user.message event. Must be `user`, `system`, `command-<command-id>` for command-originated messages, `schedule-<numeric-id>` for scheduled prompts, or `agent-<agent-id>` for prompts sent by another agent.
     */
    public String getSource() {
        return source;
    }

    /**
     * Returns the {@code agentMode} property.
     *
     * @return The UI mode the agent was in when this message was sent. Defaults to the session's current mode.
     */
    public SendAgentMode getAgentMode() {
        return agentMode;
    }

    /**
     * Returns the {@code requestHeaders} property.
     *
     * @return Custom HTTP headers to include in outbound model requests for this turn. Merged with session-level provider headers; per-turn headers augment and overwrite session-level headers with the same key.
     */
    public Map<String, String> getRequestHeaders() {
        return requestHeaders;
    }

    /**
     * Returns the {@code responseFormat} property.
     *
     * @return Provider-native output format for this turn, including all tool-call iterations. Not inherited by later turns or subagents. Ordinary steering inherits the active format; specifying responseFormat with mode: immediate is an error, even while idle. Returned assistant content remains text; the runtime does not parse or validate it. Unsupported models or schemas produce provider errors.
     */
    public SessionSendAndWaitRequestResponseFormat getResponseFormat() {
        return responseFormat;
    }

    /**
     * Returns the {@code traceparent} property.
     *
     * @return W3C Trace Context traceparent header for distributed tracing of this agent turn
     */
    public String getTraceparent() {
        return traceparent;
    }

    /**
     * Returns the {@code tracestate} property.
     *
     * @return W3C Trace Context tracestate header for distributed tracing
     */
    public String getTracestate() {
        return tracestate;
    }

    /**
     * Returns the {@code wait} property.
     *
     * @return If true, await completion of the agentic loop for this message before returning. Defaults to false (fire-and-forget). When true, the result still contains the same `messageId`; the caller can rely on the agent having processed the message before the call resolves. Transport-dependent tail semantics: on a LOCAL (in-process) session the wait additionally blocks until the completed turn's event tail has been dispatched to this session's in-process subscribers, so a subsequent read of subscriber state already reflects the turn; on a REMOTE session the wait resolves once the loop completes and mirrored delivery follows over the wire. Callers that need the stronger local guarantee on remote sessions should await the event stream explicitly.
     */
    public Boolean getWait() {
        return wait_;
    }

    /**
     * Returns the {@code clientTurnId} property.
     *
     * @return Opaque caller-chosen ID for the turn this message starts. The runtime copies it unchanged into `params._meta["com.github.copilot/turnId"]` on every MCP `tools/call` the agent makes while serving that turn, including calls from sub-agents it spawns and from Autopilot continuations of it. Visible user steering (`mode: "immediate"`) that a running turn consumes hands it to a successor turn: the steering's value replaces the running turn's from the next model call onward, and steering without one clears it. System and hidden steering leave it unchanged. A turn started without one, including one the runtime starts itself, carries no turn ID. The `user.message` event for each message the send delivers echoes it as `clientTurnId` when the message is consumed, and is persisted like other events; the value stamped on MCP calls is held in memory only and is not restored on resume.
     */
    public String getClientTurnId() {
        return clientTurnId;
    }

    /**
     * Sets the {@code displayPrompt} property.
     *
     * @param value If provided, this is shown in the timeline instead of `prompt`
     * @return this request
     */
    public SessionSendAndWaitRequest setDisplayPrompt(String value) {
        this.displayPrompt = value;
        return this;
    }

    /**
     * Sets the {@code attachments} property.
     *
     * @param value Optional attachments (files, directories, selections, blobs, GitHub references) to include with the message
     * @return this request
     */
    public SessionSendAndWaitRequest setAttachments(List<Object> value) {
        this.attachments = value;
        return this;
    }

    /**
     * Sets the {@code mode} property.
     *
     * @param value How to deliver the message. `enqueue` (default) appends to the message queue. `immediate` interjects during an in-progress turn.
     * @return this request
     */
    public SessionSendAndWaitRequest setMode(SendMode value) {
        this.mode = value;
        return this;
    }

    /**
     * Sets the {@code prepend} property.
     *
     * @param value If true, adds the message to the front of the queue instead of the end
     * @return this request
     */
    public SessionSendAndWaitRequest setPrepend(Boolean value) {
        this.prepend = value;
        return this;
    }

    /**
     * Sets the {@code billable} property.
     *
     * @param value If false, this message will not trigger a Premium Request Unit charge. User messages default to billable.
     * @return this request
     */
    public SessionSendAndWaitRequest setBillable(Boolean value) {
        this.billable = value;
        return this;
    }

    /**
     * Sets the {@code requiredTool} property.
     *
     * @param value If set, the request will fail if the named tool is not available when this message is among the user messages at the start of the current exchange
     * @return this request
     */
    public SessionSendAndWaitRequest setRequiredTool(String value) {
        this.requiredTool = value;
        return this;
    }

    /**
     * Sets the {@code source} property.
     *
     * @param value Optional provenance tag copied to the resulting user.message event. Must be `user`, `system`, `command-<command-id>` for command-originated messages, `schedule-<numeric-id>` for scheduled prompts, or `agent-<agent-id>` for prompts sent by another agent.
     * @return this request
     */
    public SessionSendAndWaitRequest setSource(String value) {
        this.source = value;
        return this;
    }

    /**
     * Sets the {@code agentMode} property.
     *
     * @param value The UI mode the agent was in when this message was sent. Defaults to the session's current mode.
     * @return this request
     */
    public SessionSendAndWaitRequest setAgentMode(SendAgentMode value) {
        this.agentMode = value;
        return this;
    }

    /**
     * Sets the {@code requestHeaders} property.
     *
     * @param value Custom HTTP headers to include in outbound model requests for this turn. Merged with session-level provider headers; per-turn headers augment and overwrite session-level headers with the same key.
     * @return this request
     */
    public SessionSendAndWaitRequest setRequestHeaders(Map<String, String> value) {
        this.requestHeaders = value;
        return this;
    }

    /**
     * Sets the {@code responseFormat} property.
     *
     * @param value Provider-native output format for this turn, including all tool-call iterations. Not inherited by later turns or subagents. Ordinary steering inherits the active format; specifying responseFormat with mode: immediate is an error, even while idle. Returned assistant content remains text; the runtime does not parse or validate it. Unsupported models or schemas produce provider errors.
     * @return this request
     */
    public SessionSendAndWaitRequest setResponseFormat(SessionSendAndWaitRequestResponseFormat value) {
        this.responseFormat = value;
        return this;
    }

    /**
     * Sets the {@code traceparent} property.
     *
     * @param value W3C Trace Context traceparent header for distributed tracing of this agent turn
     * @return this request
     */
    public SessionSendAndWaitRequest setTraceparent(String value) {
        this.traceparent = value;
        return this;
    }

    /**
     * Sets the {@code tracestate} property.
     *
     * @param value W3C Trace Context tracestate header for distributed tracing
     * @return this request
     */
    public SessionSendAndWaitRequest setTracestate(String value) {
        this.tracestate = value;
        return this;
    }

    /**
     * Sets the {@code wait} property.
     *
     * @param value If true, await completion of the agentic loop for this message before returning. Defaults to false (fire-and-forget). When true, the result still contains the same `messageId`; the caller can rely on the agent having processed the message before the call resolves. Transport-dependent tail semantics: on a LOCAL (in-process) session the wait additionally blocks until the completed turn's event tail has been dispatched to this session's in-process subscribers, so a subsequent read of subscriber state already reflects the turn; on a REMOTE session the wait resolves once the loop completes and mirrored delivery follows over the wire. Callers that need the stronger local guarantee on remote sessions should await the event stream explicitly.
     * @return this request
     */
    public SessionSendAndWaitRequest setWait(Boolean value) {
        this.wait_ = value;
        return this;
    }

    /**
     * Sets the {@code clientTurnId} property.
     *
     * @param value Opaque caller-chosen ID for the turn this message starts. The runtime copies it unchanged into `params._meta["com.github.copilot/turnId"]` on every MCP `tools/call` the agent makes while serving that turn, including calls from sub-agents it spawns and from Autopilot continuations of it. Visible user steering (`mode: "immediate"`) that a running turn consumes hands it to a successor turn: the steering's value replaces the running turn's from the next model call onward, and steering without one clears it. System and hidden steering leave it unchanged. A turn started without one, including one the runtime starts itself, carries no turn ID. The `user.message` event for each message the send delivers echoes it as `clientTurnId` when the message is consumed, and is persisted like other events; the value stamped on MCP calls is held in memory only and is not restored on resume.
     * @return this request
     */
    public SessionSendAndWaitRequest setClientTurnId(String value) {
        this.clientTurnId = value;
        return this;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionSendAndWaitRequestResponseFormat(
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
        public SessionSendAndWaitRequestResponseFormat {}

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
            public SessionSendAndWaitRequestResponseFormat build() {
                if (jsonSchema == null) throw new IllegalStateException("Missing required property: jsonSchema");
                if (type == null) throw new IllegalStateException("Missing required property: type");
                return new SessionSendAndWaitRequestResponseFormat(jsonSchema, type);
            }
        }
    }
}

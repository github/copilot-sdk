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
 * Parameters for sending zero or more user messages to the session in a single turn. Remote-backed (Mission Control) sessions do not support this method and will return an error.
 * <p>
 * Required inputs are constructor arguments. Optional inputs have fluent setters.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class SessionSendMessagesRequest {

    /** The user messages to append to the conversation, in order, before running one agent loop. When the batch starts a run, its final message is the primary initiating message; earlier messages provide context, not separate runs or replies. May be empty, in which case a single turn runs over the existing history with no new user message or originatingMessageId. */
    @JsonProperty("messages")
    private final List<SendMessageItem> messages;

    /** How to deliver the messages. `enqueue` (default) appends to the message queue. `immediate` interjects during an in-progress turn. */
    @JsonProperty("mode")
    private SendMode mode;

    /** If true, adds the messages to the front of the queue instead of the end */
    @JsonProperty("prepend")
    private Boolean prepend;

    /** The UI mode the agent was in when these messages were sent. Defaults to the session's current mode. */
    @JsonProperty("agentMode")
    private SendAgentMode agentMode;

    /** Custom HTTP headers to include in outbound model requests for this turn. Merged with session-level provider headers; per-turn headers augment and overwrite session-level headers with the same key. */
    @JsonProperty("requestHeaders")
    private Map<String, String> requestHeaders;

    /** Provider-native output format for the whole turn, including an empty message batch and all tool-call iterations. Not inherited by later turns or subagents. Ordinary steering inherits the active format; specifying responseFormat with mode: immediate is an error, even while idle. Returned assistant content remains text; the runtime does not parse or validate it. Unsupported models or schemas produce provider errors. */
    @JsonProperty("responseFormat")
    private SessionSendMessagesRequestResponseFormat responseFormat;

    /** W3C Trace Context traceparent header for distributed tracing of this agent turn */
    @JsonProperty("traceparent")
    private String traceparent;

    /** W3C Trace Context tracestate header for distributed tracing */
    @JsonProperty("tracestate")
    private String tracestate;

    /** If true, await completion of the agentic loop for this turn before returning. Defaults to false (fire-and-forget). When true, the result still contains the same `messageIds`; the caller can rely on the agent having processed the messages before the call resolves. Transport-dependent tail semantics: on a LOCAL (in-process) session the wait additionally blocks until the completed turn's event tail has been dispatched to this session's in-process subscribers, so a subsequent read of subscriber state already reflects the turn; on a REMOTE session the wait resolves once the loop completes and mirrored delivery follows over the wire. Callers that need the stronger local guarantee on remote sessions should await the event stream explicitly. */
    @JsonProperty("wait")
    private Boolean wait_;

    /** Opaque caller-chosen ID for the turn this batch starts. The runtime copies it unchanged into `params._meta["com.github.copilot/turnId"]` on every MCP `tools/call` the agent makes while serving that turn, including calls from sub-agents it spawns and from Autopilot continuations of it. Visible user steering (`mode: "immediate"`) that a running turn consumes hands it to a successor turn: the steering's value replaces the running turn's from the next model call onward, and steering without one clears it. System and hidden steering leave it unchanged. A turn started without one, including one the runtime starts itself, carries no turn ID. The `user.message` event for each message the send delivers echoes it as `clientTurnId` when the message is consumed, and is persisted like other events; the value stamped on MCP calls is held in memory only and is not restored on resume. */
    @JsonProperty("clientTurnId")
    private String clientTurnId;

    /**
     * Creates a request with its required inputs.
     *
     * @param messages The user messages to append to the conversation, in order, before running one agent loop. When the batch starts a run, its final message is the primary initiating message; earlier messages provide context, not separate runs or replies. May be empty, in which case a single turn runs over the existing history with no new user message or originatingMessageId.
     */
    public SessionSendMessagesRequest(List<SendMessageItem> messages) {
        this.messages = Objects.requireNonNull(messages, "messages");
    }

    /**
     * Returns the {@code messages} property.
     *
     * @return The user messages to append to the conversation, in order, before running one agent loop. When the batch starts a run, its final message is the primary initiating message; earlier messages provide context, not separate runs or replies. May be empty, in which case a single turn runs over the existing history with no new user message or originatingMessageId.
     */
    public List<SendMessageItem> getMessages() {
        return messages;
    }

    /**
     * Returns the {@code mode} property.
     *
     * @return How to deliver the messages. `enqueue` (default) appends to the message queue. `immediate` interjects during an in-progress turn.
     */
    public SendMode getMode() {
        return mode;
    }

    /**
     * Returns the {@code prepend} property.
     *
     * @return If true, adds the messages to the front of the queue instead of the end
     */
    public Boolean getPrepend() {
        return prepend;
    }

    /**
     * Returns the {@code agentMode} property.
     *
     * @return The UI mode the agent was in when these messages were sent. Defaults to the session's current mode.
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
     * @return Provider-native output format for the whole turn, including an empty message batch and all tool-call iterations. Not inherited by later turns or subagents. Ordinary steering inherits the active format; specifying responseFormat with mode: immediate is an error, even while idle. Returned assistant content remains text; the runtime does not parse or validate it. Unsupported models or schemas produce provider errors.
     */
    public SessionSendMessagesRequestResponseFormat getResponseFormat() {
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
     * @return If true, await completion of the agentic loop for this turn before returning. Defaults to false (fire-and-forget). When true, the result still contains the same `messageIds`; the caller can rely on the agent having processed the messages before the call resolves. Transport-dependent tail semantics: on a LOCAL (in-process) session the wait additionally blocks until the completed turn's event tail has been dispatched to this session's in-process subscribers, so a subsequent read of subscriber state already reflects the turn; on a REMOTE session the wait resolves once the loop completes and mirrored delivery follows over the wire. Callers that need the stronger local guarantee on remote sessions should await the event stream explicitly.
     */
    public Boolean getWait() {
        return wait_;
    }

    /**
     * Returns the {@code clientTurnId} property.
     *
     * @return Opaque caller-chosen ID for the turn this batch starts. The runtime copies it unchanged into `params._meta["com.github.copilot/turnId"]` on every MCP `tools/call` the agent makes while serving that turn, including calls from sub-agents it spawns and from Autopilot continuations of it. Visible user steering (`mode: "immediate"`) that a running turn consumes hands it to a successor turn: the steering's value replaces the running turn's from the next model call onward, and steering without one clears it. System and hidden steering leave it unchanged. A turn started without one, including one the runtime starts itself, carries no turn ID. The `user.message` event for each message the send delivers echoes it as `clientTurnId` when the message is consumed, and is persisted like other events; the value stamped on MCP calls is held in memory only and is not restored on resume.
     */
    public String getClientTurnId() {
        return clientTurnId;
    }

    /**
     * Sets the {@code mode} property.
     *
     * @param value How to deliver the messages. `enqueue` (default) appends to the message queue. `immediate` interjects during an in-progress turn.
     * @return this request
     */
    public SessionSendMessagesRequest setMode(SendMode value) {
        this.mode = value;
        return this;
    }

    /**
     * Sets the {@code prepend} property.
     *
     * @param value If true, adds the messages to the front of the queue instead of the end
     * @return this request
     */
    public SessionSendMessagesRequest setPrepend(Boolean value) {
        this.prepend = value;
        return this;
    }

    /**
     * Sets the {@code agentMode} property.
     *
     * @param value The UI mode the agent was in when these messages were sent. Defaults to the session's current mode.
     * @return this request
     */
    public SessionSendMessagesRequest setAgentMode(SendAgentMode value) {
        this.agentMode = value;
        return this;
    }

    /**
     * Sets the {@code requestHeaders} property.
     *
     * @param value Custom HTTP headers to include in outbound model requests for this turn. Merged with session-level provider headers; per-turn headers augment and overwrite session-level headers with the same key.
     * @return this request
     */
    public SessionSendMessagesRequest setRequestHeaders(Map<String, String> value) {
        this.requestHeaders = value;
        return this;
    }

    /**
     * Sets the {@code responseFormat} property.
     *
     * @param value Provider-native output format for the whole turn, including an empty message batch and all tool-call iterations. Not inherited by later turns or subagents. Ordinary steering inherits the active format; specifying responseFormat with mode: immediate is an error, even while idle. Returned assistant content remains text; the runtime does not parse or validate it. Unsupported models or schemas produce provider errors.
     * @return this request
     */
    public SessionSendMessagesRequest setResponseFormat(SessionSendMessagesRequestResponseFormat value) {
        this.responseFormat = value;
        return this;
    }

    /**
     * Sets the {@code traceparent} property.
     *
     * @param value W3C Trace Context traceparent header for distributed tracing of this agent turn
     * @return this request
     */
    public SessionSendMessagesRequest setTraceparent(String value) {
        this.traceparent = value;
        return this;
    }

    /**
     * Sets the {@code tracestate} property.
     *
     * @param value W3C Trace Context tracestate header for distributed tracing
     * @return this request
     */
    public SessionSendMessagesRequest setTracestate(String value) {
        this.tracestate = value;
        return this;
    }

    /**
     * Sets the {@code wait} property.
     *
     * @param value If true, await completion of the agentic loop for this turn before returning. Defaults to false (fire-and-forget). When true, the result still contains the same `messageIds`; the caller can rely on the agent having processed the messages before the call resolves. Transport-dependent tail semantics: on a LOCAL (in-process) session the wait additionally blocks until the completed turn's event tail has been dispatched to this session's in-process subscribers, so a subsequent read of subscriber state already reflects the turn; on a REMOTE session the wait resolves once the loop completes and mirrored delivery follows over the wire. Callers that need the stronger local guarantee on remote sessions should await the event stream explicitly.
     * @return this request
     */
    public SessionSendMessagesRequest setWait(Boolean value) {
        this.wait_ = value;
        return this;
    }

    /**
     * Sets the {@code clientTurnId} property.
     *
     * @param value Opaque caller-chosen ID for the turn this batch starts. The runtime copies it unchanged into `params._meta["com.github.copilot/turnId"]` on every MCP `tools/call` the agent makes while serving that turn, including calls from sub-agents it spawns and from Autopilot continuations of it. Visible user steering (`mode: "immediate"`) that a running turn consumes hands it to a successor turn: the steering's value replaces the running turn's from the next model call onward, and steering without one clears it. System and hidden steering leave it unchanged. A turn started without one, including one the runtime starts itself, carries no turn ID. The `user.message` event for each message the send delivers echoes it as `clientTurnId` when the message is consumed, and is persisted like other events; the value stamped on MCP calls is held in memory only and is not restored on resume.
     * @return this request
     */
    public SessionSendMessagesRequest setClientTurnId(String value) {
        this.clientTurnId = value;
        return this;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionSendMessagesRequestResponseFormat(
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
        public SessionSendMessagesRequestResponseFormat {}

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
            public SessionSendMessagesRequestResponseFormat build() {
                if (jsonSchema == null) throw new IllegalStateException("Missing required property: jsonSchema");
                if (type == null) throw new IllegalStateException("Missing required property: type");
                return new SessionSendMessagesRequestResponseFormat(jsonSchema, type);
            }
        }
    }
}

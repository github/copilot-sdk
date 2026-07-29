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
 * Parameters for sending zero or more user messages to the session in a single turn. Remote-backed (Mission Control) sessions do not support this method and will return an error.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionSendMessagesParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** The user messages to append to the conversation, in order. May be empty, in which case a single turn runs over the existing history with no new user message. */
    @JsonProperty("messages") List<SendMessageItem> messages,
    /** How to deliver the messages. `enqueue` (default) appends to the message queue. `immediate` interjects during an in-progress turn. */
    @JsonProperty("mode") SendMode mode,
    /** If true, adds the messages to the front of the queue instead of the end */
    @JsonProperty("prepend") Boolean prepend,
    /** The UI mode the agent was in when these messages were sent. Defaults to the session's current mode. */
    @JsonProperty("agentMode") SendAgentMode agentMode,
    /** Custom HTTP headers to include in outbound model requests for this turn. Merged with session-level provider headers; per-turn headers augment and overwrite session-level headers with the same key. */
    @JsonProperty("requestHeaders") Map<String, String> requestHeaders,
    /** W3C Trace Context traceparent header for distributed tracing of this agent turn */
    @JsonProperty("traceparent") String traceparent,
    /** W3C Trace Context tracestate header for distributed tracing */
    @JsonProperty("tracestate") String tracestate,
    /** If true, await completion of the agentic loop for this turn before returning. Defaults to false (fire-and-forget). When true, the result still contains the same `messageIds`; the caller can rely on the agent having processed the messages before the call resolves. */
    @JsonProperty("wait") Boolean wait_
) {
}

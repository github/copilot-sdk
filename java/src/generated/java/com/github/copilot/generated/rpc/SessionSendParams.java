/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Parameters for sending a user message to the session
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionSendParams(
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
    /** Optional provenance tag copied to the resulting user.message event. Supported values are `system`, `command-*`, and `schedule-*`. */
    @JsonProperty("source") Object source,
    /** The UI mode the agent was in when this message was sent. Defaults to the session's current mode. */
    @JsonProperty("agentMode") SendAgentMode agentMode,
    /** Custom HTTP headers to include in outbound model requests for this turn. Merged with session-level provider headers; per-turn headers augment and overwrite session-level headers with the same key. */
    @JsonProperty("requestHeaders") Map<String, String> requestHeaders,
    /** W3C Trace Context traceparent header for distributed tracing of this agent turn */
    @JsonProperty("traceparent") String traceparent,
    /** W3C Trace Context tracestate header for distributed tracing */
    @JsonProperty("tracestate") String tracestate,
    /** If true, await completion of the agentic loop for this message before returning. Defaults to false (fire-and-forget). When true, the result still contains the same `messageId`; the caller can rely on the agent having processed the message before the call resolves. */
    @JsonProperty("wait") Boolean wait_
) {
}

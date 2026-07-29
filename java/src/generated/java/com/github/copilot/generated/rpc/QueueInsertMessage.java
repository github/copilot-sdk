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
 * Serializable message fields accepted by queue.insertAt.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record QueueInsertMessage(
    /** The user message text. */
    @JsonProperty("prompt") String prompt,
    /** Optional user-facing display text. */
    @JsonProperty("displayPrompt") String displayPrompt,
    /** Optional attachments for the message. */
    @JsonProperty("attachments") List<Object> attachments,
    /** Optional explicit agent mode. When omitted, the session's current mode is assigned. */
    @JsonProperty("agentMode") SendAgentMode agentMode,
    /** Optional provenance source. `system` is rejected: it would hide the inserted row from `pendingItems` and make it unaddressable while still executing, so inserted items must stay visible. */
    @JsonProperty("source") String source,
    /** Whether the message is billable. */
    @JsonProperty("billable") Boolean billable,
    /** Required tool name for the turn, when any. */
    @JsonProperty("requiredTool") String requiredTool,
    /** Per-turn request headers. */
    @JsonProperty("requestHeaders") Map<String, String> requestHeaders,
    /** Accepted for SendOptions compatibility but ignored; inserted items always use queued delivery semantics. */
    @JsonProperty("mode") SendMode mode,
    /** Accepted for SendOptions compatibility but ignored; the requested public position controls placement. */
    @JsonProperty("prepend") Boolean prepend,
    /** Accepted for SendOptions compatibility but ignored; insertion scheduling is controlled by the queue drain state. */
    @JsonProperty("wait") Boolean wait_,
    /** Accepted for internal SendOptions compatibility but ignored; delivery is derived from current session activity. */
    @JsonProperty("delivery") String delivery
) {
}

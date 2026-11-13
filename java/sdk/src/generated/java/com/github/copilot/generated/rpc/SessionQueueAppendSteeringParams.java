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
import javax.annotation.processing.Generated;

/**
 * Append to one pending steering message without changing its identity or delivery position.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionQueueAppendSteeringParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Message identity returned by send, not the queue item id. Only unclaimed user steering messages are eligible. */
    @JsonProperty("messageId") String messageId,
    /** Expected current prompt, including any previous appends. The runtime applies plan-mode normalization before comparing and refuses a changed message. */
    @JsonProperty("expectedPrompt") String expectedPrompt,
    /** Mode captured at submission. Only steering messages in the same mode may be combined. */
    @JsonProperty("agentMode") SendAgentMode agentMode,
    /** Text to append after a blank line. */
    @JsonProperty("prompt") String prompt,
    /** Display text to append to the existing preview after a blank line. */
    @JsonProperty("displayPrompt") String displayPrompt,
    /** Attachments to add after the message's existing attachments. An empty list preserves the existing attachments. */
    @JsonProperty("attachments") List<Object> attachments
) {
}

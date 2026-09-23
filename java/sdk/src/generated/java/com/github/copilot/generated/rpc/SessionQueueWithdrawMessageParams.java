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
 * Conditional withdrawal of a single user message, before the runtime claims it for delivery.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionQueueWithdrawMessageParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Message identity returned by send, not the queue item id. Batch messages are not eligible. */
    @JsonProperty("messageId") String messageId,
    /** The prompt originally sent. A message edited since submission is not withdrawn, so an obsolete draft cannot replace the edit. */
    @JsonProperty("expectedPrompt") String expectedPrompt
) {
}

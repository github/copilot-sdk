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
 * Parameters for clearing the conversation and seeding the window that replaces it.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionHistoryClearContextParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** First user message of the fresh context window. Required: a cleared window holding only system and developer messages is not a conversation a model can answer, so every clear seeds the window it creates. Delivered by the enclosing turn driver once the agentic loop exits, which is why the call must be made from inside a tool handler. */
    @JsonProperty("prompt") String prompt
) {
}

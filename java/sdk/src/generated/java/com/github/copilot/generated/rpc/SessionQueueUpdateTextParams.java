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
 * Parameters for editing a single queued message.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionQueueUpdateTextParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Stable opaque ID of the queued item to edit. */
    @JsonProperty("id") String id,
    /** Replacement prompt sent to the model. */
    @JsonProperty("prompt") String prompt,
    /** Optional replacement prompt displayed to the user. */
    @JsonProperty("displayPrompt") String displayPrompt
) {
}

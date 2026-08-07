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
 * Register a relative-interval scheduled prompt.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionScheduleAddParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Human-readable interval such as `30s`, `5m`, or `2h`. */
    @JsonProperty("interval") String interval,
    /** Prompt text to enqueue when the schedule fires. */
    @JsonProperty("prompt") String prompt,
    /** Whether the schedule should re-arm after each tick. Defaults to true. */
    @JsonProperty("recurring") Boolean recurring,
    /** Optional display-only prompt label. */
    @JsonProperty("displayPrompt") String displayPrompt
) {
}

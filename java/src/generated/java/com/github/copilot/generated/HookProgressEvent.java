/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Session event "hook.progress". Ephemeral progress update from a running hook process
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class HookProgressEvent extends SessionEvent {

    @Override
    public String getType() { return "hook.progress"; }

    @JsonProperty("data")
    private HookProgressEventData data;

    public HookProgressEventData getData() { return data; }
    public void setData(HookProgressEventData data) { this.data = data; }

    /** Data payload for {@link HookProgressEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record HookProgressEventData(
        /** Human-readable progress message from the hook process */
        @JsonProperty("message") String message
    ) {
    }
}

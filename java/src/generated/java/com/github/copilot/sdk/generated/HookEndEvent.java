/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * The {@code hook.end} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class HookEndEvent extends SessionEvent {

    @Override
    public String getType() { return "hook.end"; }

    @JsonProperty("data")
    private HookEndEventData data;

    public HookEndEventData getData() { return data; }
    public void setData(HookEndEventData data) { this.data = data; }

    /** Data payload for {@link HookEndEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record HookEndEventData(
        /** Identifier matching the corresponding hook.start event */
        @JsonProperty("hookInvocationId") String hookInvocationId,
        /** Type of hook that was invoked (e.g., "preToolUse", "postToolUse", "sessionStart") */
        @JsonProperty("hookType") String hookType,
        /** Output data produced by the hook */
        @JsonProperty("output") Object output,
        /** Whether the hook completed successfully */
        @JsonProperty("success") Boolean success,
        /** Error details when the hook failed */
        @JsonProperty("error") HookEndError error
    ) {
    }
}

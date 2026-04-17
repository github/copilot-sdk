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
 * The {@code hook.start} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class HookStartEvent extends SessionEvent {

    @Override
    public String getType() { return "hook.start"; }

    @JsonProperty("data")
    private HookStartEventData data;

    public HookStartEventData getData() { return data; }
    public void setData(HookStartEventData data) { this.data = data; }

    /** Data payload for {@link HookStartEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record HookStartEventData(
        /** Unique identifier for this hook invocation */
        @JsonProperty("hookInvocationId") String hookInvocationId,
        /** Type of hook being invoked (e.g., "preToolUse", "postToolUse", "sessionStart") */
        @JsonProperty("hookType") String hookType,
        /** Input data passed to the hook */
        @JsonProperty("input") Object input
    ) {
    }
}

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
 * The {@code permission.completed} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class PermissionCompletedEvent extends SessionEvent {

    @Override
    public String getType() { return "permission.completed"; }

    @JsonProperty("data")
    private PermissionCompletedEventData data;

    public PermissionCompletedEventData getData() { return data; }
    public void setData(PermissionCompletedEventData data) { this.data = data; }

    /** Data payload for {@link PermissionCompletedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PermissionCompletedEventData(
        /** Request ID of the resolved permission request; clients should dismiss any UI for this request */
        @JsonProperty("requestId") String requestId,
        /** Optional tool call ID associated with this permission prompt; clients may use it to correlate UI created from tool-scoped prompts */
        @JsonProperty("toolCallId") String toolCallId,
        /** The result of the permission request */
        @JsonProperty("result") PermissionCompletedResult result
    ) {
    }
}

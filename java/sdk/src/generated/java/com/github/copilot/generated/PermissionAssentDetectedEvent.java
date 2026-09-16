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
 * Session event "permission.assentDetected". Records that deterministic text recognition found likely assent in the human turn immediately following a root Autopilot permission request that was blocked because no interactive response was available. This event grants no authority; its model-facing projection only suggests retrying the unchanged operation.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class PermissionAssentDetectedEvent extends SessionEvent {

    @Override
    public String getType() { return "permission.assentDetected"; }

    @JsonProperty("data")
    private PermissionAssentDetectedEventData data;

    public PermissionAssentDetectedEventData getData() { return data; }
    public void setData(PermissionAssentDetectedEventData data) { this.data = data; }

    /** Data payload for {@link PermissionAssentDetectedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PermissionAssentDetectedEventData(
        /** Permission request the likely assent may refer to. The runtime derives this from the preceding durable blocker; the human message and extraction model do not choose it. */
        @JsonProperty("requestId") String requestId,
        /** Human turn whose text triggered the deterministic assent recognizer. */
        @JsonProperty("turnIndex") Long turnIndex
    ) {
    }
}

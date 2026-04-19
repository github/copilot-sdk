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
 * The {@code permission.requested} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class PermissionRequestedEvent extends SessionEvent {

    @Override
    public String getType() { return "permission.requested"; }

    @JsonProperty("data")
    private PermissionRequestedEventData data;

    public PermissionRequestedEventData getData() { return data; }
    public void setData(PermissionRequestedEventData data) { this.data = data; }

    /** Data payload for {@link PermissionRequestedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PermissionRequestedEventData(
        /** Unique identifier for this permission request; used to respond via session.respondToPermission() */
        @JsonProperty("requestId") String requestId,
        /** Details of the permission being requested */
        @JsonProperty("permissionRequest") Object permissionRequest,
        /** When true, this permission was already resolved by a permissionRequest hook and requires no client action */
        @JsonProperty("resolvedByHook") Boolean resolvedByHook
    ) {
    }
}

/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: permission.completed
 * <p>
 * Broadcast when a pending permission request has been resolved by a client
 * (protocol v3).
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class PermissionCompletedEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private PermissionCompletedData data;

    @Override
    public String getType() {
        return "permission.completed";
    }

    public PermissionCompletedData getData() {
        return data;
    }

    public void setData(PermissionCompletedData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PermissionCompletedData(@JsonProperty("requestId") String requestId,
            @JsonProperty("result") PermissionCompletedResult result) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record PermissionCompletedResult(@JsonProperty("kind") String kind) {
        }
    }
}

/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.sdk.json.PermissionRequest;

/**
 * Event: permission.requested
 * <p>
 * Broadcast when the CLI needs a client to handle a permission request
 * (protocol v3). Clients that have a permission handler should respond via
 * {@code session.permissions.handlePendingPermissionRequest}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class PermissionRequestedEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private PermissionRequestedData data;

    @Override
    public String getType() {
        return "permission.requested";
    }

    public PermissionRequestedData getData() {
        return data;
    }

    public void setData(PermissionRequestedData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PermissionRequestedData(@JsonProperty("requestId") String requestId,
            @JsonProperty("permissionRequest") PermissionRequest permissionRequest) {
    }
}

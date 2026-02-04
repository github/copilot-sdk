/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response from session.setForeground RPC call.
 * <p>
 * This is only available when connecting to a server running in TUI+server mode
 * (--ui-server).
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SetForegroundSessionResponse {

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("error")
    private String error;

    /**
     * Whether the operation was successful.
     *
     * @return true if successful
     */
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    /**
     * Gets the error message if the operation failed.
     *
     * @return the error message, or null if successful
     */
    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}

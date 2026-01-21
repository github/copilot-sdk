/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Internal response object from deleting a session.
 * <p>
 * This is a low-level class for JSON-RPC communication containing the result of
 * a session deletion operation.
 *
 * @see com.github.copilot.sdk.CopilotClient#deleteSession(String)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class DeleteSessionResponse {

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("error")
    private String error;

    /**
     * Returns whether the deletion was successful.
     *
     * @return {@code true} if the session was deleted successfully
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Sets whether the deletion was successful.
     *
     * @param success
     *            {@code true} if successful
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }

    /**
     * Gets the error message if the deletion failed.
     *
     * @return the error message, or {@code null} if successful
     */
    public String getError() {
        return error;
    }

    /**
     * Sets the error message.
     *
     * @param error
     *            the error message
     */
    public void setError(String error) {
        this.error = error;
    }
}

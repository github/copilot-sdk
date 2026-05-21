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
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DeleteSessionResponse(
        /** Whether the deletion was successful. */
        @JsonProperty("success") boolean success,
        /** The error message, or {@code null} if successful. */
        @JsonProperty("error") String error) {
}

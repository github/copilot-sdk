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
public record SetForegroundSessionResponse(
        /** Whether the operation was successful. */
        @JsonProperty("success") boolean success,
        /** The error message, or null if successful. */
        @JsonProperty("error") String error) {
}

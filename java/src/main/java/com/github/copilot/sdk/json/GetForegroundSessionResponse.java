/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response from session.getForeground RPC call.
 * <p>
 * This is only available when connecting to a server running in TUI+server mode
 * (--ui-server).
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GetForegroundSessionResponse(
        /** The session ID currently displayed in the TUI, or null if none. */
        @JsonProperty("sessionId") String sessionId,
        /** The workspace path of the foreground session, or null. */
        @JsonProperty("workspacePath") String workspacePath) {
}

/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request body for session.setForeground RPC call.
 * <p>
 * Using an explicit record type (rather than an ad-hoc map) ensures correct
 * JSON serialization in all execution environments.
 *
 * @since 1.0.0
 */
public record SetForegroundSessionRequest(
        /** The session ID to bring to the foreground. */
        @JsonProperty("sessionId") String sessionId) {
}

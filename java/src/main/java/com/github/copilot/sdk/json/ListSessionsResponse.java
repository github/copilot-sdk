/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Internal response object from listing sessions.
 * <p>
 * This is a low-level class for JSON-RPC communication containing the list of
 * available sessions.
 *
 * @see com.github.copilot.sdk.CopilotClient#listSessions()
 * @see SessionMetadata
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ListSessionsResponse(
        /** The list of session metadata. */
        @JsonProperty("sessions") List<SessionMetadata> sessions) {
}

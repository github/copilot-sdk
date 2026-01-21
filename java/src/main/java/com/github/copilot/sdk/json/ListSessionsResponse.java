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
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ListSessionsResponse {

    @JsonProperty("sessions")
    private List<SessionMetadata> sessions;

    /**
     * Gets the list of sessions.
     *
     * @return the list of session metadata
     */
    public List<SessionMetadata> getSessions() {
        return sessions;
    }

    /**
     * Sets the list of sessions.
     *
     * @param sessions
     *            the list of session metadata
     */
    public void setSessions(List<SessionMetadata> sessions) {
        this.sessions = sessions;
    }
}

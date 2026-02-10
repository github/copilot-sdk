package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Internal response object from creating a session.
 *
 * @param sessionId
 *            the session ID assigned by the server
 * @param workspacePath
 *            the workspace path, or {@code null} if infinite sessions are
 *            disabled
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreateSessionResponse(@JsonProperty("sessionId") String sessionId,
        @JsonProperty("workspacePath") String workspacePath) {
}

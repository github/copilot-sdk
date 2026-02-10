package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Internal response object from resuming a session.
 *
 * @param sessionId
 *            the session ID
 * @param workspacePath
 *            the workspace path, or {@code null} if infinite sessions are
 *            disabled
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResumeSessionResponse(@JsonProperty("sessionId") String sessionId,
        @JsonProperty("workspacePath") String workspacePath) {
}

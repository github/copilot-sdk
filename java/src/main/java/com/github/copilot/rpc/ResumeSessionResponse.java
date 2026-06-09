package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.generated.rpc.OpenCanvasInstance;
import java.util.List;

/**
 * Internal response object from resuming a session.
 *
 * @param sessionId
 *            the session ID
 * @param workspacePath
 *            the workspace path, or {@code null} if infinite sessions are
 *            disabled
 * @param capabilities
 *            the capabilities reported by the host, or {@code null}
 * @param openCanvases
 *            the canvas instances open for the session, or {@code null}
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResumeSessionResponse(@JsonProperty("sessionId") String sessionId,
        @JsonProperty("workspacePath") String workspacePath,
        @JsonProperty("capabilities") SessionCapabilities capabilities,
        @JsonProperty("openCanvases") List<OpenCanvasInstance> openCanvases) {
}

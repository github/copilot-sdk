package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.generated.rpc.OpenCanvasInstance;
import com.github.copilot.generated.rpc.SessionPermissionsSetModeResult;
import java.util.List;

/**
 * Internal response object from creating a session.
 * <p>
 * The {@code openCanvases} component was added in 1.0.1.
 *
 * @param sessionId
 *            the session ID assigned by the server
 * @param workspacePath
 *            the workspace path, or {@code null} if infinite sessions are
 *            disabled
 * @param capabilities
 *            the capabilities reported by the host, or {@code null}
 * @param openCanvases
 *            the canvas instances open for the session, or {@code null} (since
 *            1.0.1)
 * @param permissionModeResult
 *            the outcome of the requested initial permission mode, or
 *            {@code null} when none was requested
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreateSessionResponse(@JsonProperty("sessionId") String sessionId,
        @JsonProperty("workspacePath") String workspacePath,
        @JsonProperty("capabilities") SessionCapabilities capabilities,
        @JsonProperty("openCanvases") List<OpenCanvasInstance> openCanvases,
        @JsonProperty("permissionModeResult") SessionPermissionsSetModeResult permissionModeResult) {
    /**
     * Constructs a response without a permission-mode outcome.
     *
     * @param sessionId
     *            the session ID
     * @param workspacePath
     *            the workspace path, or {@code null}
     * @param capabilities
     *            the host capabilities, or {@code null}
     * @param openCanvases
     *            the open canvas instances, or {@code null}
     */
    public CreateSessionResponse(String sessionId, String workspacePath, SessionCapabilities capabilities,
            List<OpenCanvasInstance> openCanvases) {
        this(sessionId, workspacePath, capabilities, openCanvases, null);
    }
}

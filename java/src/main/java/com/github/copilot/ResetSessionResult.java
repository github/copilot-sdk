/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

/**
 * Result returned by
 * {@link CopilotSession#resetAsync(com.github.copilot.rpc.SessionConfig)}.
 *
 * @param previousSessionId
 *            the session ID that was closed and replaced
 * @param session
 *            the fresh session created from the supplied reset configuration
 */
public record ResetSessionResult(String previousSessionId, CopilotSession session) {
}

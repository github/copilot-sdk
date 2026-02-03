/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import java.util.concurrent.CompletableFuture;

/**
 * Handler for session-end hooks.
 * <p>
 * This handler is invoked when a session ends, allowing you to perform cleanup
 * or logging.
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * SessionEndHandler handler = (input, invocation) -> {
 *     System.out.println("Session ended: " + input.getReason());
 *     return CompletableFuture.completedFuture(
 *         new SessionEndHookOutput()
 *             .setSessionSummary("Session completed successfully")
 *     );
 * };
 * }</pre>
 *
 * @since 1.0.7
 */
@FunctionalInterface
public interface SessionEndHandler {

    /**
     * Handles a session end event.
     *
     * @param input
     *            the hook input containing session end details
     * @param invocation
     *            metadata about the hook invocation
     * @return a future that resolves with the hook output, or {@code null} to
     *         proceed without modification
     */
    CompletableFuture<SessionEndHookOutput> handle(SessionEndHookInput input, HookInvocation invocation);
}

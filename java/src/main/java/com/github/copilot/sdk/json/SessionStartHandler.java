/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import java.util.concurrent.CompletableFuture;

/**
 * Handler for session-start hooks.
 * <p>
 * This handler is invoked when a session starts, allowing you to perform
 * initialization or modify the session configuration.
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * SessionStartHandler handler = (input, invocation) -> {
 * 	System.out.println("Session started from: " + input.source());
 * 	return CompletableFuture.completedFuture(new SessionStartHookOutput("Custom initialization context", null));
 * };
 * }</pre>
 *
 * @since 1.0.7
 */
@FunctionalInterface
public interface SessionStartHandler {

    /**
     * Handles a session start event.
     *
     * @param input
     *            the hook input containing session start details
     * @param invocation
     *            metadata about the hook invocation
     * @return a future that resolves with the hook output, or {@code null} to
     *         proceed without modification
     */
    CompletableFuture<SessionStartHookOutput> handle(SessionStartHookInput input, HookInvocation invocation);
}

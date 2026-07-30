/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.concurrent.CompletableFuture;

/**
 * Handler for agent-stop hooks.
 *
 * @since 1.0.9
 */
@FunctionalInterface
public interface AgentStopHandler {

    /**
     * Handles an agent-stop hook invocation.
     *
     * @param input
     *            the hook input
     * @param invocation
     *            context information about the invocation
     * @return a future that resolves with the hook output, or {@code null} to let
     *         the agent stop
     */
    CompletableFuture<AgentStopHookOutput> handle(AgentStopHookInput input, HookInvocation invocation);
}

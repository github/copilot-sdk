/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.concurrent.CompletableFuture;

/**
 * Handler invoked after a subagent's turn.
 */
@FunctionalInterface
public interface SubagentStopHandler {

    /**
     * Handles a subagent-stop hook invocation.
     *
     * @param input
     *            the hook input
     * @param invocation
     *            context about the invocation
     * @return a block decision or replacement response, or {@code null} to allow
     *         the stop
     */
    CompletableFuture<SubagentStopHookOutput> handle(SubagentStopHookInput input, HookInvocation invocation);
}

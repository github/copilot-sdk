/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.concurrent.CompletableFuture;

/**
 * Handler invoked before a subagent's first turn.
 */
@FunctionalInterface
public interface SubagentStartHandler {

    /**
     * Handles a subagent-start hook invocation.
     *
     * @param input
     *            the hook input
     * @param invocation
     *            context about the invocation
     * @return context to prepend to the subagent's prompt, or {@code null}
     */
    CompletableFuture<SubagentStartHookOutput> handle(SubagentStartHookInput input, HookInvocation invocation);
}

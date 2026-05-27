/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.concurrent.CompletableFuture;

/**
 * Handler for post-tool-use-failure hooks.
 * <p>
 * This hook is called after a tool execution whose result was a failure.
 * {@link PostToolUseHandler} only fires for successful tool executions;
 * register this handler in addition to observe failed tool calls.
 *
 * @since 1.3.0
 */
@FunctionalInterface
public interface PostToolUseFailureHandler {

    /**
     * Handles a post-tool-use-failure hook invocation.
     *
     * @param input
     *            the hook input containing tool name, arguments, and error message
     * @param invocation
     *            context information about the invocation
     * @return a future that resolves with the hook output, or {@code null} to use
     *         defaults
     */
    CompletableFuture<PostToolUseFailureHookOutput> handle(PostToolUseFailureHookInput input,
            HookInvocation invocation);
}

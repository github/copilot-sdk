/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import java.util.concurrent.CompletableFuture;

/**
 * Handler for post-tool-use hooks.
 * <p>
 * This hook is called after a tool has been executed, allowing you to:
 * <ul>
 * <li>Inspect or modify tool results</li>
 * <li>Add additional context for the model</li>
 * <li>Suppress output</li>
 * </ul>
 *
 * @since 1.0.6
 */
@FunctionalInterface
public interface PostToolUseHandler {

    /**
     * Handles a post-tool-use hook invocation.
     *
     * @param input
     *            the hook input containing tool name, arguments, and result
     * @param invocation
     *            context information about the invocation
     * @return a future that resolves with the hook output, or {@code null} to use
     *         defaults
     */
    CompletableFuture<PostToolUseHookOutput> handle(PostToolUseHookInput input, HookInvocation invocation);
}

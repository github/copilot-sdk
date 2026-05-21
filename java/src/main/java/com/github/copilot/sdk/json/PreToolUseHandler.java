/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import java.util.concurrent.CompletableFuture;

/**
 * Handler for pre-tool-use hooks.
 * <p>
 * This hook is called before a tool is executed, allowing you to:
 * <ul>
 * <li>Approve or deny tool execution</li>
 * <li>Modify tool arguments</li>
 * <li>Add additional context for the model</li>
 * </ul>
 *
 * @since 1.0.6
 */
@FunctionalInterface
public interface PreToolUseHandler {

    /**
     * Handles a pre-tool-use hook invocation.
     *
     * @param input
     *            the hook input containing tool name and arguments
     * @param invocation
     *            context information about the invocation
     * @return a future that resolves with the hook output, or {@code null} to use
     *         defaults
     */
    CompletableFuture<PreToolUseHookOutput> handle(PreToolUseHookInput input, HookInvocation invocation);
}

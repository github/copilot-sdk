/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import java.util.concurrent.CompletableFuture;

/**
 * Handler for user-prompt-submitted hooks.
 * <p>
 * This handler is invoked when the user submits a prompt, allowing you to
 * intercept and modify the prompt before it is processed.
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * UserPromptSubmittedHandler handler = (input, invocation) -> {
 * 	System.out.println("User submitted: " + input.getPrompt());
 * 	// Optionally modify the prompt
 * 	return CompletableFuture.completedFuture(
 * 			new UserPromptSubmittedHookOutput().setModifiedPrompt(input.getPrompt() + " (enhanced)"));
 * };
 * }</pre>
 *
 * @since 1.0.7
 */
@FunctionalInterface
public interface UserPromptSubmittedHandler {

    /**
     * Handles a user prompt submission event.
     *
     * @param input
     *            the hook input containing the prompt details
     * @param invocation
     *            metadata about the hook invocation
     * @return a future that resolves with the hook output, or {@code null} to
     *         proceed without modification
     */
    CompletableFuture<UserPromptSubmittedHookOutput> handle(UserPromptSubmittedHookInput input,
            HookInvocation invocation);
}

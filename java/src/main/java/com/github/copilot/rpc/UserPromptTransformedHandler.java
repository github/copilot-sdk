/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.concurrent.CompletableFuture;

/**
 * Handler for user-prompt-transformed hooks.
 *
 * @since 1.0.11
 */
@FunctionalInterface
public interface UserPromptTransformedHandler {

    /**
     * Handles a transformed user prompt before it is stored or sent to the model.
     *
     * @param input
     *            the hook input
     * @param invocation
     *            metadata about the hook invocation
     * @return a future resolving to the hook output, or {@code null}
     */
    CompletableFuture<UserPromptTransformedHookOutput> handle(UserPromptTransformedHookInput input,
            HookInvocation invocation);
}

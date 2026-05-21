/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import java.util.concurrent.CompletableFuture;

/**
 * Functional interface for handling slash-command executions.
 * <p>
 * Implement this interface to define the behavior of a registered slash
 * command. The handler is invoked when the user executes the command in the CLI
 * TUI.
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * CommandHandler deployHandler = context -> {
 * 	System.out.println("Deploying with args: " + context.getArgs());
 * 	// perform deployment...
 * 	return CompletableFuture.completedFuture(null);
 * };
 * }</pre>
 *
 * @see CommandDefinition
 * @since 1.0.0
 */
@FunctionalInterface
public interface CommandHandler {

    /**
     * Handles a slash-command execution.
     *
     * @param context
     *            the command context containing session ID, command text, and
     *            arguments
     * @return a future that completes when the command handling is done
     */
    CompletableFuture<Void> handle(CommandContext context);
}

/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

/**
 * Defines a slash command that users can invoke from the CLI TUI.
 * <p>
 * Register commands via {@link SessionConfig#setCommands(java.util.List)} or
 * {@link ResumeSessionConfig#setCommands(java.util.List)}. Each command appears
 * as {@code /name} in the CLI TUI.
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * var config = new SessionConfig().setCommands(List.of(
 * 		new CommandDefinition().setName("deploy").setDescription("Deploy the application").setHandler(context -> {
 * 			System.out.println("Deploying: " + context.getArgs());
 * 			return CompletableFuture.completedFuture(null);
 * 		})));
 * }</pre>
 *
 * @see CommandHandler
 * @see CommandContext
 * @since 1.0.0
 */
public class CommandDefinition {

    private String name;
    private String description;
    private CommandHandler handler;

    /**
     * Gets the command name (without leading {@code /}).
     *
     * @return the command name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the command name (without leading {@code /}).
     * <p>
     * For example, {@code "deploy"} registers the {@code /deploy} command.
     *
     * @param name
     *            the command name
     * @return this instance for method chaining
     */
    public CommandDefinition setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Gets the human-readable description shown in the command completion UI.
     *
     * @return the description, or {@code null} if not set
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the human-readable description shown in the command completion UI.
     *
     * @param description
     *            the description
     * @return this instance for method chaining
     */
    public CommandDefinition setDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * Gets the handler invoked when the command is executed.
     *
     * @return the command handler
     */
    public CommandHandler getHandler() {
        return handler;
    }

    /**
     * Sets the handler invoked when the command is executed.
     *
     * @param handler
     *            the command handler
     * @return this instance for method chaining
     */
    public CommandDefinition setHandler(CommandHandler handler) {
        this.handler = handler;
        return this;
    }
}

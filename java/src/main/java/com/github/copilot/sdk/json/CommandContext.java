/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

/**
 * Context passed to a {@link CommandHandler} when a slash command is executed.
 *
 * @since 1.0.0
 */
public class CommandContext {

    private String sessionId;
    private String command;
    private String commandName;
    private String args;

    /** Gets the session ID where the command was invoked. @return the session ID */
    public String getSessionId() {
        return sessionId;
    }

    /** Sets the session ID. @param sessionId the session ID @return this */
    public CommandContext setSessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    /**
     * Gets the full command text (e.g., {@code /deploy production}).
     *
     * @return the full command text
     */
    public String getCommand() {
        return command;
    }

    /** Sets the full command text. @param command the command text @return this */
    public CommandContext setCommand(String command) {
        this.command = command;
        return this;
    }

    /**
     * Gets the command name without the leading {@code /}.
     *
     * @return the command name
     */
    public String getCommandName() {
        return commandName;
    }

    /** Sets the command name. @param commandName the command name @return this */
    public CommandContext setCommandName(String commandName) {
        this.commandName = commandName;
        return this;
    }

    /**
     * Gets the raw argument string after the command name.
     *
     * @return the argument string
     */
    public String getArgs() {
        return args;
    }

    /** Sets the argument string. @param args the argument string @return this */
    public CommandContext setArgs(String args) {
        this.args = args;
        return this;
    }
}

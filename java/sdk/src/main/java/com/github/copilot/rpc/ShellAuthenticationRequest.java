/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

/**
 * A password or other answer that a shell command run by the agent asks for,
 * for example {@code sudo -A}, {@code ssh}, or {@code git} over HTTPS. The
 * answer goes only to the program that asked: it is not shown to the model and
 * is not recorded in session events.
 */
public class ShellAuthenticationRequest {

    private String requestId;
    private String program;
    private String prompt;
    private String inputKind;
    private String command;
    private String cwd;

    /**
     * Gets the request ID. A cancellation for the request carries the same ID.
     *
     * @return the request ID
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * Sets the request ID.
     *
     * @param requestId
     *            the request ID
     * @return this instance for method chaining
     */
    public ShellAuthenticationRequest setRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    /**
     * Gets the program that asked: {@code sudo}, {@code ssh}, or {@code git}.
     *
     * @return the program
     */
    public String getProgram() {
        return program;
    }

    /**
     * Sets the program that asked.
     *
     * @param program
     *            the program
     * @return this instance for method chaining
     */
    public ShellAuthenticationRequest setProgram(String program) {
        this.program = program;
        return this;
    }

    /**
     * Gets the prompt text the program supplied.
     *
     * @return the prompt
     */
    public String getPrompt() {
        return prompt;
    }

    /**
     * Sets the prompt text.
     *
     * @param prompt
     *            the prompt
     * @return this instance for method chaining
     */
    public ShellAuthenticationRequest setPrompt(String prompt) {
        this.prompt = prompt;
        return this;
    }

    /**
     * Gets how to present the request: {@code secret} for a password or passphrase
     * (mask the input), {@code text} for a visible answer such as a user name, or
     * {@code confirm} for a yes or no question such as an ssh host-key check.
     *
     * @return the input kind
     */
    public String getInputKind() {
        return inputKind;
    }

    /**
     * Sets the input kind.
     *
     * @param inputKind
     *            {@code secret}, {@code text}, or {@code confirm}
     * @return this instance for method chaining
     */
    public ShellAuthenticationRequest setInputKind(String inputKind) {
        this.inputKind = inputKind;
        return this;
    }

    /**
     * Gets the shell command that triggered the prompt.
     *
     * @return the command
     */
    public String getCommand() {
        return command;
    }

    /**
     * Sets the shell command that triggered the prompt.
     *
     * @param command
     *            the command
     * @return this instance for method chaining
     */
    public ShellAuthenticationRequest setCommand(String command) {
        this.command = command;
        return this;
    }

    /**
     * Gets the command's working directory.
     *
     * @return the working directory
     */
    public String getCwd() {
        return cwd;
    }

    /**
     * Sets the command's working directory.
     *
     * @param cwd
     *            the working directory
     * @return this instance for method chaining
     */
    public ShellAuthenticationRequest setCwd(String cwd) {
        this.cwd = cwd;
        return this;
    }
}

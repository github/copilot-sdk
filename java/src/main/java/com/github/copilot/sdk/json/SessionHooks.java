/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

/**
 * Hook handlers configuration for a session.
 * <p>
 * Hooks allow you to intercept and modify various session events including tool
 * execution, user prompts, and session lifecycle events.
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * var hooks = new SessionHooks().setOnPreToolUse((input, invocation) -> {
 * 	System.out.println("Tool being called: " + input.getToolName());
 * 	return CompletableFuture.completedFuture(new PreToolUseHookOutput().setPermissionDecision("allow"));
 * }).setOnPostToolUse((input, invocation) -> {
 * 	System.out.println("Tool result: " + input.getToolResult());
 * 	return CompletableFuture.completedFuture(null);
 * }).setOnUserPromptSubmitted((input, invocation) -> {
 * 	System.out.println("User prompt: " + input.getPrompt());
 * 	return CompletableFuture.completedFuture(null);
 * }).setOnSessionStart((input, invocation) -> {
 * 	System.out.println("Session started: " + input.getSource());
 * 	return CompletableFuture.completedFuture(null);
 * }).setOnSessionEnd((input, invocation) -> {
 * 	System.out.println("Session ended: " + input.getReason());
 * 	return CompletableFuture.completedFuture(null);
 * });
 *
 * var session = client.createSession(new SessionConfig().setHooks(hooks)).get();
 * }</pre>
 *
 * @since 1.0.6
 */
public class SessionHooks {

    private PreToolUseHandler onPreToolUse;
    private PostToolUseHandler onPostToolUse;
    private UserPromptSubmittedHandler onUserPromptSubmitted;
    private SessionStartHandler onSessionStart;
    private SessionEndHandler onSessionEnd;

    /**
     * Gets the pre-tool-use handler.
     *
     * @return the handler, or {@code null} if not set
     */
    public PreToolUseHandler getOnPreToolUse() {
        return onPreToolUse;
    }

    /**
     * Sets the handler called before a tool is executed.
     *
     * @param onPreToolUse
     *            the handler
     * @return this instance for method chaining
     */
    public SessionHooks setOnPreToolUse(PreToolUseHandler onPreToolUse) {
        this.onPreToolUse = onPreToolUse;
        return this;
    }

    /**
     * Gets the post-tool-use handler.
     *
     * @return the handler, or {@code null} if not set
     */
    public PostToolUseHandler getOnPostToolUse() {
        return onPostToolUse;
    }

    /**
     * Sets the handler called after a tool has been executed.
     *
     * @param onPostToolUse
     *            the handler
     * @return this instance for method chaining
     */
    public SessionHooks setOnPostToolUse(PostToolUseHandler onPostToolUse) {
        this.onPostToolUse = onPostToolUse;
        return this;
    }

    /**
     * Gets the user-prompt-submitted handler.
     *
     * @return the handler, or {@code null} if not set
     * @since 1.0.7
     */
    public UserPromptSubmittedHandler getOnUserPromptSubmitted() {
        return onUserPromptSubmitted;
    }

    /**
     * Sets the handler called when the user submits a prompt.
     *
     * @param onUserPromptSubmitted
     *            the handler
     * @return this instance for method chaining
     * @since 1.0.7
     */
    public SessionHooks setOnUserPromptSubmitted(UserPromptSubmittedHandler onUserPromptSubmitted) {
        this.onUserPromptSubmitted = onUserPromptSubmitted;
        return this;
    }

    /**
     * Gets the session-start handler.
     *
     * @return the handler, or {@code null} if not set
     * @since 1.0.7
     */
    public SessionStartHandler getOnSessionStart() {
        return onSessionStart;
    }

    /**
     * Sets the handler called when a session starts.
     *
     * @param onSessionStart
     *            the handler
     * @return this instance for method chaining
     * @since 1.0.7
     */
    public SessionHooks setOnSessionStart(SessionStartHandler onSessionStart) {
        this.onSessionStart = onSessionStart;
        return this;
    }

    /**
     * Gets the session-end handler.
     *
     * @return the handler, or {@code null} if not set
     * @since 1.0.7
     */
    public SessionEndHandler getOnSessionEnd() {
        return onSessionEnd;
    }

    /**
     * Sets the handler called when a session ends.
     *
     * @param onSessionEnd
     *            the handler
     * @return this instance for method chaining
     * @since 1.0.7
     */
    public SessionHooks setOnSessionEnd(SessionEndHandler onSessionEnd) {
        this.onSessionEnd = onSessionEnd;
        return this;
    }

    /**
     * Returns whether any hooks are registered.
     *
     * @return {@code true} if at least one hook handler is set
     */
    public boolean hasHooks() {
        return onPreToolUse != null || onPostToolUse != null || onUserPromptSubmitted != null || onSessionStart != null
                || onSessionEnd != null;
    }
}

/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

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
 * 	return CompletableFuture.completedFuture(PreToolUseHookOutput.allow());
 * }).setOnPostToolUse((input, invocation) -> {
 * 	System.out.println("Tool result: " + input.getToolResult());
 * 	return CompletableFuture.completedFuture(null);
 * }).setOnUserPromptSubmitted((input, invocation) -> {
 * 	System.out.println("User prompt: " + input.prompt());
 * 	return CompletableFuture.completedFuture(null);
 * }).setOnSessionStart((input, invocation) -> {
 * 	System.out.println("Session started: " + input.source());
 * 	return CompletableFuture.completedFuture(null);
 * }).setOnSessionEnd((input, invocation) -> {
 * 	System.out.println("Session ended: " + input.reason());
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
    private PreMcpToolCallHandler onPreMcpToolCall;
    private PostToolUseHandler onPostToolUse;
    private PostToolUseFailureHandler onPostToolUseFailure;
    private UserPromptSubmittedHandler onUserPromptSubmitted;
    private UserPromptTransformedHandler onUserPromptTransformed;
    private SessionStartHandler onSessionStart;
    private SessionEndHandler onSessionEnd;
    private AgentStopHandler onAgentStop;

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
     * Gets the pre-MCP-tool-call handler.
     *
     * @return the handler, or {@code null} if not set
     * @since 1.0.8
     */
    public PreMcpToolCallHandler getOnPreMcpToolCall() {
        return onPreMcpToolCall;
    }

    /**
     * Sets the handler called before an MCP tool call is dispatched to an MCP
     * server.
     *
     * @param onPreMcpToolCall
     *            the handler
     * @return this instance for method chaining
     * @since 1.0.8
     */
    public SessionHooks setOnPreMcpToolCall(PreMcpToolCallHandler onPreMcpToolCall) {
        this.onPreMcpToolCall = onPreMcpToolCall;
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
     * Gets the post-tool-use-failure handler.
     *
     * @return the handler, or {@code null} if not set
     * @since 1.3.0
     */
    public PostToolUseFailureHandler getOnPostToolUseFailure() {
        return onPostToolUseFailure;
    }

    /**
     * Sets the handler called after a tool execution whose result was a failure.
     * <p>
     * {@link #getOnPostToolUse()} only fires for successful tool executions;
     * register this handler in addition to observe failed tool calls.
     *
     * @param onPostToolUseFailure
     *            the handler
     * @return this instance for method chaining
     * @since 1.3.0
     */
    public SessionHooks setOnPostToolUseFailure(PostToolUseFailureHandler onPostToolUseFailure) {
        this.onPostToolUseFailure = onPostToolUseFailure;
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
     * Gets the user-prompt-transformed handler.
     *
     * @return the handler, or {@code null} if not set
     * @since 1.0.11
     */
    public UserPromptTransformedHandler getOnUserPromptTransformed() {
        return onUserPromptTransformed;
    }

    /**
     * Sets the handler called after the runtime transforms a submitted prompt.
     *
     * @param onUserPromptTransformed
     *            the handler
     * @return this instance for method chaining
     * @since 1.0.11
     */
    public SessionHooks setOnUserPromptTransformed(UserPromptTransformedHandler onUserPromptTransformed) {
        this.onUserPromptTransformed = onUserPromptTransformed;
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
     * Gets the agent-stop handler.
     *
     * @return the handler, or {@code null} if not set
     * @since 1.0.9
     */
    public AgentStopHandler getOnAgentStop() {
        return onAgentStop;
    }

    /**
     * Sets the handler called when the top-level agent reaches a natural stop.
     *
     * @param onAgentStop
     *            the handler
     * @return this instance for method chaining
     * @since 1.0.9
     */
    public SessionHooks setOnAgentStop(AgentStopHandler onAgentStop) {
        this.onAgentStop = onAgentStop;
        return this;
    }

    /**
     * Returns whether any hooks are registered.
     *
     * @return {@code true} if at least one hook handler is set
     */
    public boolean hasHooks() {
        return onPreToolUse != null || onPreMcpToolCall != null || onPostToolUse != null || onPostToolUseFailure != null
                || onUserPromptSubmitted != null || onUserPromptTransformed != null || onSessionStart != null
                || onSessionEnd != null || onAgentStop != null;
    }
}

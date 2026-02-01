/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

/**
 * Hook handlers configuration for a session.
 * <p>
 * Hooks allow you to intercept and modify tool execution behavior. Currently
 * supports pre-tool-use and post-tool-use hooks.
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
     * Returns whether any hooks are registered.
     *
     * @return {@code true} if at least one hook handler is set
     */
    public boolean hasHooks() {
        return onPreToolUse != null || onPostToolUse != null;
    }
}

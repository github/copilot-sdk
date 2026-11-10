/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Context for a shell authentication request invocation.
 */
public class ShellAuthenticationInvocation {

    private String sessionId;
    private CompletionStage<Void> cancellation = new CompletableFuture<Void>().minimalCompletionStage();

    /**
     * Gets the ID of the session whose command asked.
     *
     * @return the session ID
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Sets the session ID.
     *
     * @param sessionId
     *            the session ID
     * @return this instance for method chaining
     */
    public ShellAuthenticationInvocation setSessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    /**
     * Gets a stage that completes when the answer is no longer needed, for example
     * because the command ended, the prompt timed out, or the session ended or
     * switched to autopilot. Close the input when it completes.
     *
     * @return the cancellation stage
     */
    public CompletionStage<Void> getCancellation() {
        return cancellation;
    }

    /**
     * Sets the cancellation stage.
     *
     * @param cancellation
     *            a stage that completes when the answer is no longer needed
     * @return this instance for method chaining
     */
    public ShellAuthenticationInvocation setCancellation(CompletionStage<Void> cancellation) {
        this.cancellation = cancellation;
        return this;
    }
}

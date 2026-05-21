/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import java.util.concurrent.CompletableFuture;

/**
 * Handler for user input requests from the agent.
 * <p>
 * Implement this interface to handle user input requests when the agent uses
 * the ask_user tool.
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * UserInputHandler handler = (request, invocation) -> {
 * 	System.out.println("Agent asks: " + request.getQuestion());
 * 	String answer = readUserInput(); // your input method
 * 	return CompletableFuture.completedFuture(new UserInputResponse().setAnswer(answer).setWasFreeform(true));
 * };
 *
 * var session = client.createSession(new SessionConfig().setOnUserInputRequest(handler)).get();
 * }</pre>
 *
 * @since 1.0.6
 */
@FunctionalInterface
public interface UserInputHandler {

    /**
     * Handles a user input request from the agent.
     *
     * @param request
     *            the user input request containing the question and optional
     *            choices
     * @param invocation
     *            context information about the invocation
     * @return a future that resolves with the user's response
     */
    CompletableFuture<UserInputResponse> handle(UserInputRequest request, UserInputInvocation invocation);
}

/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.concurrent.CompletableFuture;

/**
 * Handler for passwords and other answers that shell commands run by the agent
 * ask for, for example {@code sudo -A}, {@code ssh}, or {@code git} over HTTPS.
 * <p>
 * The answer goes only to the program that asked: it is not shown to the model
 * and is not recorded in session events. An exception, a {@code null} or
 * exceptionally completed future, a response other than
 * {@link ShellAuthenticationResponse#accept(String)}, or an answer after
 * {@link ShellAuthenticationInvocation#getCancellation()} completes cancels the
 * prompt, so the program that asked fails instead of receiving a wrong value.
 * When the runtime no longer needs the answer, the SDK also cancels the
 * returned future.
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * ShellAuthenticationHandler handler = (request, invocation) -> {
 * 	var dialog = showDialog(request.getProgram() + " asks: " + request.getPrompt(),
 * 			"secret".equals(request.getInputKind()));
 * 	invocation.getCancellation().thenRun(dialog::close);
 * 	return dialog.answer()
 * 			.thenApply(answer -> answer == null
 * 					? ShellAuthenticationResponse.cancel()
 * 					: ShellAuthenticationResponse.accept(answer));
 * };
 *
 * var session = client.createSession(new SessionConfig().setOnShellAuthenticationRequest(handler)).get();
 * }</pre>
 */
@FunctionalInterface
public interface ShellAuthenticationHandler {

    /**
     * Handles a shell authentication request from a shell command the agent runs.
     *
     * @param request
     *            the request, with the program that asked and its prompt
     * @param invocation
     *            context information about the invocation
     * @return a future that resolves with the answer, or a cancellation
     */
    CompletableFuture<ShellAuthenticationResponse> handle(ShellAuthenticationRequest request,
            ShellAuthenticationInvocation invocation);
}

/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import java.util.concurrent.CompletableFuture;

/**
 * Functional interface for handling elicitation requests from the server.
 * <p>
 * Register an elicitation handler via
 * {@link SessionConfig#setOnElicitationRequest(ElicitationHandler)} or
 * {@link ResumeSessionConfig#setOnElicitationRequest(ElicitationHandler)}. When
 * provided, the server routes elicitation requests to this handler and reports
 * elicitation as a supported capability.
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * ElicitationHandler handler = context -> {
 * 	// Show the form to the user and collect responses
 * 	Map<String, Object> formValues = showForm(context.getMessage(), context.getRequestedSchema());
 * 	return CompletableFuture.completedFuture(
 * 			new ElicitationResult().setAction(ElicitationResultAction.ACCEPT).setContent(formValues));
 * };
 * }</pre>
 *
 * @see ElicitationContext
 * @see ElicitationResult
 * @since 1.0.0
 */
@FunctionalInterface
public interface ElicitationHandler {

    /**
     * Handles an elicitation request from the server.
     *
     * @param context
     *            the elicitation context containing the message, schema, and mode
     * @return a future that resolves with the elicitation result
     */
    CompletableFuture<ElicitationResult> handle(ElicitationContext context);
}

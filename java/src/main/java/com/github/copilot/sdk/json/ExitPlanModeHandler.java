/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import java.util.concurrent.CompletableFuture;

/**
 * Handler for exit-plan-mode requests from the agent.
 * <p>
 * Register an exit-plan-mode handler via
 * {@link SessionConfig#setOnExitPlanMode(ExitPlanModeHandler)} or
 * {@link ResumeSessionConfig#setOnExitPlanMode(ExitPlanModeHandler)}. When
 * provided, the server routes {@code exitPlanMode.request} callbacks to this
 * handler.
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * ExitPlanModeHandler handler = (request, invocation) -> {
 * 	// Review the plan and decide whether to approve
 * 	return CompletableFuture
 * 			.completedFuture(new ExitPlanModeResult().setApproved(true).setSelectedAction("interactive"));
 * };
 *
 * var session = client.createSession(new SessionConfig().setOnExitPlanMode(handler)).get();
 * }</pre>
 *
 * @see ExitPlanModeRequest
 * @see ExitPlanModeResult
 * @since 1.0.8
 */
@FunctionalInterface
public interface ExitPlanModeHandler {

    /**
     * Handles an exit-plan-mode request from the agent.
     *
     * @param request
     *            the exit-plan-mode request containing the summary, plan content,
     *            and available actions
     * @param invocation
     *            context information about the invocation
     * @return a future that resolves with the user's decision
     */
    CompletableFuture<ExitPlanModeResult> handle(ExitPlanModeRequest request, ExitPlanModeInvocation invocation);
}

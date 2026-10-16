/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.concurrent.CompletableFuture;

import com.github.copilot.CopilotExperimental;
import com.github.copilot.generated.rpc.InstallationConfirmationRequest;
import com.github.copilot.generated.rpc.InstallationDecision;

/**
 * Handles connection-global installation confirmation requests from the
 * runtime.
 * <p>
 * Match the request's operation and policy session against the exact original
 * action on this connection before presenting the review. Refuse unknown
 * actions or incomplete reviews. The SDK echoes the original challenge and
 * review fingerprint, so the handler returns only an explicit decision.
 *
 * @apiNote This type is experimental and may change in a future version.
 * @since 1.0.0
 */
@FunctionalInterface
@CopilotExperimental
public interface InstallationConfirmationHandler {

    /**
     * Handles one installation confirmation review.
     *
     * @param request
     *            the generated request from the runtime
     * @param context
     *            cancellation signal for this review
     * @return a future that resolves with the user's explicit decision
     */
    CompletableFuture<InstallationDecision> confirm(InstallationConfirmationRequest request,
            InstallationConfirmationContext context);
}

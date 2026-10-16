/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import com.github.copilot.CopilotExperimental;

/**
 * Cancellation signal for one installation confirmation request.
 * <p>
 * This signal retires the human review. It does not cancel outbound
 * installation or OAuth RPCs.
 *
 * @apiNote This type is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
public final class InstallationConfirmationContext {

    private final CompletableFuture<Void> cancelled;

    /**
     * Creates an installation confirmation context.
     *
     * @param cancelled
     *            completed when the runtime cancels this review or the original
     *            connection closes
     */
    public InstallationConfirmationContext(CompletableFuture<Void> cancelled) {
        this.cancelled = Objects.requireNonNull(cancelled, "cancelled must not be null");
    }

    /**
     * Gets the cancellation signal for this review.
     * <p>
     * This future completes when the runtime retires this review request, including
     * through JSON-RPC {@code $/cancelRequest}, runtime-enforced expiry, or loss of
     * the original connection.
     * <p>
     * Completing the returned future does not affect SDK state.
     *
     * @return the cancellation signal
     */
    public CompletableFuture<Void> getCancelled() {
        return cancelled.copy();
    }
}

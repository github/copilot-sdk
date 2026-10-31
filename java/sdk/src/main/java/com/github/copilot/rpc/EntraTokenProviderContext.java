/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import com.github.copilot.CopilotExperimental;

/** Cancellation lifetime of one host-owned Entra acquisition. */
@CopilotExperimental
public final class EntraTokenProviderContext {
    private final CompletableFuture<Void> cancelled;

    /**
     * @param cancelled
     *            signal completed when this acquisition is retired
     */
    public EntraTokenProviderContext(CompletableFuture<Void> cancelled) {
        this.cancelled = Objects.requireNonNull(cancelled, "cancelled must not be null");
    }

    /**
     * @return a signal completed when the request or its original connection ends;
     *         completing this copy does not change SDK state
     */
    public CompletableFuture<Void> getCancelled() {
        return cancelled.copy();
    }
}

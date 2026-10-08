/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import com.github.copilot.generated.rpc.HostDisposeParams;
import com.github.copilot.generated.rpc.HostListSessionsParams;
import com.github.copilot.generated.rpc.HostListSessionsResult;
import com.github.copilot.generated.rpc.HostPublishSessionParams;
import com.github.copilot.generated.rpc.HostPublishSessionResult;
import com.github.copilot.generated.rpc.HostStartResult;
import com.github.copilot.generated.rpc.ServerHostApi;
import java.util.concurrent.CompletableFuture;

/**
 * A ready, connection-owned AHP listener. The handle remains bound to its
 * original transport across client reconnects. Disposal never deletes
 * application sessions.
 */
@CopilotExperimental
public final class AhpHost implements AutoCloseable {
    private final HostStartResult info;
    private final ServerHostApi rpc;

    AhpHost(HostStartResult info, ServerHostApi rpc) {
        this.info = info;
        this.rpc = rpc;
    }

    /**
     * Gets the listener identity.
     *
     * @return the listener identity
     */
    public String getHostId() {
        return info.hostId();
    }

    /**
     * Gets the bound address.
     *
     * @return the actual bound WebSocket URL, or {@code null} without a local
     *         server
     */
    public String getUrl() {
        return info.url();
    }

    /**
     * Gets the secret token.
     *
     * @return the token, or {@code null} without an authenticated local server
     */
    public String getToken() {
        return info.token();
    }

    /**
     * Gets the legacy process ID.
     *
     * @return the process ID, or {@code null} for in-process hosting
     */
    public Long getPid() {
        return info.pid();
    }

    /**
     * Gets the GitHub Mission Control environment identity.
     *
     * @return the environment ID, or {@code null} without a GitHub environment
     */
    public String getEnvironmentId() {
        return info.environmentId();
    }

    /**
     * Stops the listener and joins runtime cleanup. Every call reaches the runtime,
     * including concurrent and repeated disposal calls.
     *
     * @return completion of listener cleanup
     */
    public CompletableFuture<Void> dispose() {
        return rpc.dispose(new HostDisposeParams(info.hostId()));
    }

    /**
     * Durably advertises an exact attached session in the compute-scoped catalog,
     * without invoking a factory or transferring session ownership.
     *
     * @param sessionId
     *            resident session identity
     * @return the published AHP session identity
     */
    public CompletableFuture<HostPublishSessionResult> publishSession(String sessionId) {
        return rpc.publishSession(new HostPublishSessionParams(info.hostId(), sessionId));
    }

    /**
     * Lists all live and dormant catalog sessions advertised by this host.
     *
     * @return the advertised host sessions
     */
    public CompletableFuture<HostListSessionsResult> listSessions() {
        return rpc.listSessions(new HostListSessionsParams(info.hostId()));
    }

    /** Disposes the listener and waits for acknowledged cleanup. */
    @Override
    public void close() {
        dispose().join();
    }
}

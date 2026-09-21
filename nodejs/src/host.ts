/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import type { HostExitedNotification, HostStartRequest, HostStartResult } from "./generated/rpc.js";

/** The runtime's report that a supervised AHP host has stopped. */
export type CopilotHostExit = HostExitedNotification;

/** Options for a runtime-supervised AHP listener. @experimental */
export type CopilotHostOptions = Omit<HostStartRequest, "hostId">;

/**
 * A connection-owned AHP listener supervised by the runtime.
 *
 * The runtime, not this SDK, launches and reaps copilotd-lite. Disconnecting
 * the owning client also stops the host; reconnecting does not reclaim it.
 * Stopping a host does not delete its underlying sessions.
 *
 * @experimental
 */
export class CopilotHost {
    readonly hostId: string;
    readonly url: string;
    /** Bearer token required by the listener. Treat this value as a secret. */
    readonly token: string;
    /** Process ID of the runtime's child, not an SDK-owned process. */
    readonly pid: number;
    /**
     * Resolves when the runtime reports termination, or the owner connection
     * is lost. Inspect `reason` and `error` for unexpected failures. On owner
     * connection loss, runtime cleanup proceeds independently; this promise
     * cannot acknowledge child reaping over a disconnected transport.
     */
    readonly closed: Promise<CopilotHostExit>;
    private disposePromise?: Promise<void>;
    private stopped = false;

    /** @internal */
    constructor(
        info: HostStartResult,
        closed: Promise<CopilotHostExit>,
        private readonly disposeHost: () => Promise<void>
    ) {
        this.hostId = info.hostId;
        this.url = info.url;
        this.token = info.token;
        this.pid = info.pid;
        this.closed = closed;
        void closed.then(() => {
            this.stopped = true;
        });
    }

    /** Stop the listener and await runtime-owned child cleanup. Idempotent. */
    dispose(): Promise<void> {
        if (this.stopped) {
            return Promise.resolve();
        }
        this.disposePromise ??= this.disposeHost().catch((error: unknown) => {
            this.disposePromise = undefined;
            throw error;
        });
        return this.disposePromise;
    }

    async [Symbol.asyncDispose](): Promise<void> {
        await this.dispose();
    }
}

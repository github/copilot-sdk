/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import type { HostExitedNotification, HostStartResult } from "./generated/rpc.js";

/**
 * A runtime exit report, or an owner disconnect that cannot acknowledge reaping.
 * @experimental
 */
export type AhpHostExit = HostExitedNotification;

/** Options for a runtime-supervised AHP listener. @experimental */
export interface AhpHostOptions {
    /** Listener hostname. The runtime defaults to 127.0.0.1. */
    hostname?: string;
    /** Listener port. Omitted or zero asks the runtime for an available port. */
    port?: number;
    /** Connection token. The runtime generates one when required and omitted. */
    token?: string;
    /** Whether the listener requires token authentication. */
    requireConnectionToken?: boolean;
    /** Called at most once. This callback is local and is never sent to the runtime. */
    onExit?: (exit: AhpHostExit) => void;
}

/**
 * A connection-owned AHP listener supervised by the runtime.
 *
 * The runtime, not this SDK, launches and reaps copilotd-lite. Disconnecting
 * the owning client also stops the host; reconnecting does not reclaim it.
 * Stopping a host does not delete its underlying sessions.
 * Its durable AHP catalog is shared by successive lite hosts in the runtime's
 * effective Copilot home; a concurrent lite host for that catalog is rejected.
 *
 * @experimental
 */
export class AhpHost {
    readonly hostId: string;
    readonly url: string;
    /** Connection token, when required by the listener. Treat this value as a secret. */
    readonly token: string | undefined;
    /** Process ID of the runtime's child, not an SDK-owned process. */
    readonly pid: number;

    /** @internal */
    constructor(
        info: HostStartResult,
        private readonly disposeHost: () => Promise<void>
    ) {
        this.hostId = info.hostId;
        this.url = info.url;
        this.token = info.token;
        this.pid = info.pid;
    }

    /** Ask the runtime to stop the listener and await its cleanup, on every call. */
    dispose(): Promise<void> {
        return this.disposeHost();
    }

    async [Symbol.asyncDispose](): Promise<void> {
        await this.dispose();
    }
}

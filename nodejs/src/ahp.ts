import { randomUUID } from "node:crypto";
import { setMaxListeners } from "node:events";
import type { MessageConnection } from "vscode-jsonrpc/node.js";

const MAX_BYTES = 8 * 1024 * 1024;
const MAX_PENDING_MESSAGES = 64;
const DEADLINE_MS = 10_000;

/** Application-owned transport. The SDK forwards opaque AHP JSON text. */
export interface AhpTransport {
    send(jsonText: string, signal: AbortSignal): Promise<void>;
    close(error?: Error): void | Promise<void>;
}

/** One physical transport connection, accepted synchronously before runtime setup completes. */
export interface AhpConnection {
    /** Resolves after bounded runtime admission, not after the AHP operation completes. */
    receive(text: string): Promise<void>;
    receiveChunk(bytes: Uint8Array, options: { endOfMessage: boolean }): Promise<void>;
    end(): Promise<void>;
    /** Resolves on normal closure and rejects on transport/protocol failure. */
    readonly closed: Promise<void>;
}

/** A runtime AHP endpoint with an application-owned listener and authentication policy. */
export interface AhpEndpoint {
    acceptConnection(transport: AhpTransport): AhpConnection;
    dispose(): Promise<void>;
}

function asError(error: unknown): Error {
    return error instanceof Error ? error : new Error(String(error));
}

function bounded<T>(work: Promise<T>, signal?: AbortSignal): Promise<T> {
    return new Promise<T>((resolve, reject) => {
        const finish = (callback: () => void) => {
            clearTimeout(timer);
            signal?.removeEventListener("abort", abort);
            callback();
        };
        const abort = () =>
            finish(() => reject(signal?.reason ?? new Error("AHP connection closed")));
        const timer = setTimeout(
            () => finish(() => reject(new Error("AHP transport timed out"))),
            DEADLINE_MS
        );
        signal?.addEventListener("abort", abort, { once: true });
        if (signal?.aborted) abort();
        work.then(
            (value) => finish(() => resolve(value)),
            (error) => finish(() => reject(error))
        );
    });
}

export class AhpEndpointImpl implements AhpEndpoint {
    readonly connections = new Map<string, AhpConnectionImpl>();
    readonly id = randomUUID();
    private active = true;
    private disposal?: Promise<void>;
    private readonly lifetime = new AbortController();

    constructor(
        private readonly rpc: MessageConnection,
        private readonly remove: () => void
    ) {}

    async initialize(): Promise<void> {
        const creation = this.request("ahp.createEndpoint");
        void creation.then(
            () => {
                if (!this.active) void bounded(this.request("ahp.disposeEndpoint")).catch(() => {});
            },
            () => {}
        );
        try {
            await bounded(creation, this.lifetime.signal);
        } catch (error) {
            this.retire(asError(error));
            throw error;
        }
    }

    async request(method: string, connectionId?: string, message?: string): Promise<unknown> {
        return this.rpc.sendRequest(method, {
            endpointId: this.id,
            ...(connectionId === undefined ? {} : { connectionId }),
            ...(message === undefined ? {} : { message }),
        });
    }

    acceptConnection(transport: AhpTransport): AhpConnection {
        if (!this.active) throw new Error("AHP endpoint is disposed");
        const connection = new AhpConnectionImpl(this, transport);
        this.connections.set(connection.id, connection);
        connection.open();
        return connection;
    }

    retire(error?: Error): void {
        if (!this.active) return;
        this.active = false;
        this.remove();
        this.lifetime.abort(error ?? new Error("AHP endpoint disposed"));
        const connections = [...this.connections.values()];
        this.connections.clear();
        for (const connection of connections) connection.retire(error);
    }

    dispose(): Promise<void> {
        if (this.disposal) return this.disposal;
        if (!this.active) return Promise.resolve();
        this.retire();
        this.disposal = bounded(this.request("ahp.disposeEndpoint")).then(() => {});
        return this.disposal;
    }
}

class AhpConnectionImpl implements AhpConnection {
    readonly id = randomUUID();
    readonly closed: Promise<void>;
    private resolveClosed!: () => void;
    private rejectClosed!: (error: Error) => void;
    private transport?: AhpTransport;
    private readonly lifetime = new AbortController();
    private opening!: Promise<unknown>;
    private ending?: Promise<void>;
    private remoteClosing?: Promise<unknown>;
    private closeWork: Promise<void> = Promise.resolve();
    private inbound: Promise<void> = Promise.resolve();
    private outbound: Promise<void> = Promise.resolve();
    private incomingBytes = 0;
    private outgoingBytes = 0;
    private incomingMessages = 0;
    private outgoingMessages = 0;
    private chunks = new Uint8Array(0);
    private chunkBytes = 0;
    private fragmenting = false;

    constructor(
        private readonly endpoint: AhpEndpointImpl,
        transport: AhpTransport
    ) {
        this.transport = transport;
        setMaxListeners(2 * MAX_PENDING_MESSAGES + 8, this.lifetime.signal);
        this.closed = new Promise<void>((resolve, reject) => {
            this.resolveClosed = resolve;
            this.rejectClosed = reject;
        });
        void this.closed.catch(() => {});
    }

    open(): void {
        this.opening = this.endpoint.request("ahp.openConnection", this.id);
        // A socket can disappear before open is acknowledged. Never reattach it.
        void this.opening.then(
            () => {
                if (this.lifetime.signal.aborted) {
                    // An earlier close may have completed before the concurrent
                    // open registered remotely, so this must be a fresh request.
                    void bounded(this.endpoint.request("ahp.closeConnection", this.id)).catch(
                        () => {}
                    );
                }
            },
            () => {}
        );
        void bounded(this.opening, this.lifetime.signal).catch((error) => {
            if (!this.lifetime.signal.aborted) this.fail(asError(error));
        });
    }

    private assertActive(): void {
        if (this.lifetime.signal.aborted) throw this.lifetime.signal.reason;
    }

    async receive(text: string): Promise<void> {
        this.assertActive();
        if (this.fragmenting) throw new Error("An AHP fragmented message is still in progress");
        const bytes = Buffer.byteLength(text, "utf8");
        if (
            this.incomingBytes + bytes > MAX_BYTES ||
            this.incomingMessages >= MAX_PENDING_MESSAGES
        ) {
            const error = new Error(
                "AHP incoming messages exceed the 8 MiB / 64 pending message limit"
            );
            this.fail(error);
            throw error;
        }
        this.incomingBytes += bytes;
        this.incomingMessages++;
        const work = this.inbound.then(async () => {
            await bounded(this.opening, this.lifetime.signal);
            this.assertActive();
            await bounded(
                this.endpoint.request("ahp.receive", this.id, text),
                this.lifetime.signal
            );
        });
        const admitted = bounded(work, this.lifetime.signal);
        this.inbound = admitted.catch(() => {});
        try {
            await admitted;
        } catch (error) {
            this.fail(asError(error));
            throw error;
        } finally {
            if (!this.lifetime.signal.aborted) {
                this.incomingBytes -= bytes;
                this.incomingMessages--;
            }
        }
    }

    async receiveChunk(bytes: Uint8Array, options: { endOfMessage: boolean }): Promise<void> {
        this.assertActive();
        if (this.incomingBytes + this.chunkBytes + bytes.byteLength > MAX_BYTES) {
            const error = new Error("AHP incoming messages exceed the 8 MiB limit");
            this.fail(error);
            throw error;
        }
        const length = this.chunkBytes + bytes.byteLength;
        if (length > this.chunks.byteLength) {
            const grown = new Uint8Array(
                Math.min(MAX_BYTES, Math.max(length, this.chunks.byteLength * 2, 1024))
            );
            grown.set(this.chunks.subarray(0, this.chunkBytes));
            this.chunks = grown;
        }
        this.chunks.set(bytes, this.chunkBytes);
        this.chunkBytes += bytes.byteLength;
        this.fragmenting = !options.endOfMessage;
        if (!options.endOfMessage) return;
        const message = this.chunks.subarray(0, this.chunkBytes);
        this.chunks = new Uint8Array(0);
        this.chunkBytes = 0;
        try {
            await this.receive(new TextDecoder("utf-8", { fatal: true }).decode(message));
        } catch (error) {
            this.fail(asError(error));
            throw error;
        }
    }

    async send(message: string): Promise<void> {
        this.assertActive();
        const bytes = Buffer.byteLength(message, "utf8");
        if (
            this.outgoingBytes + bytes > MAX_BYTES ||
            this.outgoingMessages >= MAX_PENDING_MESSAGES
        ) {
            const error = new Error(
                "AHP outgoing messages exceed the 8 MiB / 64 pending message limit"
            );
            this.fail(error);
            throw error;
        }
        this.outgoingBytes += bytes;
        this.outgoingMessages++;
        const work = this.outbound.then(async () => {
            this.assertActive();
            await bounded(
                this.transport!.send(message, this.lifetime.signal),
                this.lifetime.signal
            );
        });
        const sent = bounded(work, this.lifetime.signal);
        this.outbound = sent.catch(() => {});
        try {
            await sent;
        } catch (error) {
            this.fail(asError(error));
            throw error;
        } finally {
            if (!this.lifetime.signal.aborted) {
                this.outgoingBytes -= bytes;
                this.outgoingMessages--;
            }
        }
    }

    retire(error?: Error): void {
        if (this.lifetime.signal.aborted) return;
        const transport = this.transport;
        this.transport = undefined;
        this.endpoint.connections.delete(this.id);
        this.chunks = new Uint8Array(0);
        this.fragmenting = false;
        this.chunkBytes = this.incomingBytes = this.outgoingBytes = 0;
        this.incomingMessages = this.outgoingMessages = 0;
        this.lifetime.abort(error ?? new Error("AHP connection closed"));
        if (error) this.rejectClosed(error);
        try {
            this.closeWork = bounded(Promise.resolve(transport?.close(error)));
        } catch (closeError) {
            this.closeWork = Promise.reject(closeError);
        }
        if (!error) void this.closeWork.then(this.resolveClosed, this.rejectClosed);
        void this.closeWork.catch(() => {});
    }

    private fail(error: Error): void {
        if (this.lifetime.signal.aborted) return;
        this.retire(error);
        void this.end().catch(() => {});
    }

    private closeRemote(): Promise<unknown> {
        return (this.remoteClosing ??= this.endpoint.request("ahp.closeConnection", this.id));
    }

    end(): Promise<void> {
        if (this.ending) return this.ending;
        this.retire();
        this.ending = Promise.all([this.closeWork, bounded(this.closeRemote())]).then(() => {});
        return this.ending;
    }
}

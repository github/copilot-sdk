/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { AsyncLocalStorage } from "node:async_hooks";
import {
    type CancellationToken,
    type Disposable,
    type MessageConnection,
    ResponseError,
} from "vscode-jsonrpc/node.js";
import type {
    AhpCreateSessionRequest as WireCreateSessionRequest,
    AhpResumeSessionRequest as WireResumeSessionRequest,
    AhpListSessionsRequest as WireListSessionsRequest,
    AhpMessageRequest as WireMessageRequest,
    AhpSessionControlRequest as WireSessionControlRequest,
    AhpConnectionClosedNotification,
    AhpEndpointClosedNotification,
    AhpEndpointRef,
    AhpRegisterEndpointResult,
    AhpOpenConnectionResult,
} from "./generated/rpc.js";

/** A session returned by application policy; a CopilotSession also satisfies this interface. */
export interface AhpSessionIdentity {
    sessionId: string;
}

/** Application callback context. Cancellation never disconnects the owning SDK session. */
export interface AhpCallbackContext {
    signal: AbortSignal;
}

export interface AhpCreateSessionRequest {
    connectionId: string;
    requestedSessionId: string;
    workingDirectory?: string;
    model?: string;
    config?: unknown;
}

export interface AhpResumeSessionRequest {
    connectionId: string;
    sessionId: string;
}

export interface AhpSessionControlRequest {
    sessionId: string;
    kind: string;
    payload: unknown;
}

export interface AhpSessionControlResult {
    applied: boolean;
    reason?: string;
    result?: unknown;
}

/** Local policy functions are never serialized. Omitted callbacks use native runtime defaults. */
export interface AhpEndpointOptions {
    allowSessionCreation?: boolean;
    capabilities?: unknown;
    onCreateSession?: (
        request: AhpCreateSessionRequest,
        context: AhpCallbackContext
    ) => AhpSessionIdentity | Promise<AhpSessionIdentity>;
    onResumeSession?: (
        request: AhpResumeSessionRequest,
        context: AhpCallbackContext
    ) => AhpSessionIdentity | Promise<AhpSessionIdentity>;
    /**
     * Authorizes sessions for this endpoint. The native endpoint enforces this
     * set for subscriptions, history, actions, disposal, and root notifications.
     */
    onListSessions?: (
        context: AhpCallbackContext
    ) => readonly AhpSessionIdentity[] | Promise<readonly AhpSessionIdentity[]>;
    onSessionControl?: (
        request: AhpSessionControlRequest,
        context: AhpCallbackContext
    ) => AhpSessionControlResult | Promise<AhpSessionControlResult>;
    /**
     * Tighter local limits per direction, including the currently in-flight message.
     * Values cannot exceed 1 MiB per message, 128 messages, or 8 MiB buffered.
     */
    limits?: {
        maxMessageBytes?: number;
        maxQueuedMessages?: number;
        maxBufferedBytes?: number;
    };
}

export interface AhpConnectionOptions {
    /**
     * Deliver one opaque AHP message. Calls are serialized for this connection
     * only. Resolve after transport delivery; the native delivery ACK waits for it.
     */
    onMessage: (message: string) => void | Promise<void>;
    onClose?: (error?: Error) => void;
}

export interface AhpConnection {
    readonly id: string;
    /** Resolves on runtime admission, not on the eventual AHP response. */
    send(message: string): Promise<void>;
    close(): Promise<void>;
}

export interface AhpEndpoint {
    readonly id: string;
    openConnection(options: AhpConnectionOptions): Promise<AhpConnection>;
    refreshExposure(): Promise<void>;
    setCapabilities(capabilities: unknown): Promise<void>;
    dispose(): Promise<void>;
}

const callbackScope = new AsyncLocalStorage<boolean>();
const closedError = () => new Error("AHP endpoint or connection is closed");
const asError = (error: unknown) => (error instanceof Error ? error : new Error(String(error)));
const MAX_LIMITS = Object.freeze({
    maxMessageBytes: 1024 * 1024,
    maxQueuedMessages: 128,
    maxBufferedBytes: 8 * 1024 * 1024,
});

function assertNotCallback(): void {
    if (callbackScope.getStore()) {
        throw new Error("Recursive AHP endpoint operations from an AHP callback are not supported");
    }
}

type CallbackRequest =
    | { method: "createSession"; params: WireCreateSessionRequest }
    | { method: "resumeSession"; params: WireResumeSessionRequest }
    | { method: "listSessions"; params: WireListSessionsRequest }
    | { method: "sessionControl"; params: WireSessionControlRequest };

/** @internal Opaque bridge over the client's existing runtime connection. */
export class AhpEndpointRegistry {
    private endpoints = new Map<string, Endpoint>();
    private subscriptions: Disposable[] = [];
    private lifetime = new AbortController();

    constructor(private readonly rpc: MessageConnection) {
        this.registerCallback<WireCreateSessionRequest>("createSession", (params) => ({
            method: "createSession",
            params,
        }));
        this.registerCallback<WireResumeSessionRequest>("resumeSession", (params) => ({
            method: "resumeSession",
            params,
        }));
        this.registerCallback<WireListSessionsRequest>("listSessions", (params) => ({
            method: "listSessions",
            params,
        }));
        this.registerCallback<WireSessionControlRequest>("sessionControl", (params) => ({
            method: "sessionControl",
            params,
        }));
        this.subscriptions.push(
            rpc.onRequest("ahp.message", (params: WireMessageRequest, token: CancellationToken) => {
                const connection = this.endpoints
                    .get(params.endpointId)
                    ?.connections.get(params.connectionId);
                if (!connection) throw closedError();
                return connection.receive(params.message, token);
            }),
            rpc.onNotification(
                "ahp.connectionClosed",
                (params: AhpConnectionClosedNotification) => {
                    this.endpoints
                        .get(params.endpointId)
                        ?.connections.get(params.connectionId)
                        ?.finish(params.error === undefined ? undefined : new Error(params.error));
                }
            ),
            rpc.onNotification("ahp.endpointClosed", (params: AhpEndpointClosedNotification) => {
                this.endpoints
                    .get(params.endpointId)
                    ?.finish(params.error === undefined ? undefined : new Error(params.error));
            }),
            rpc.onClose(() => this.close(new Error("Runtime connection closed"))),
            rpc.onDispose(() => this.close(new Error("Runtime connection disposed")))
        );
    }

    private registerCallback<T extends AhpEndpointRef>(
        method: CallbackRequest["method"],
        request: (params: T) => CallbackRequest
    ): void {
        this.subscriptions.push(
            this.rpc.onRequest(`ahp.${method}`, (params: T, token: CancellationToken) => {
                const endpoint = this.endpoints.get(params.endpointId);
                if (!endpoint) throw closedError();
                return endpoint.invoke(request(params), token);
            })
        );
    }

    get closed(): boolean {
        return this.lifetime.signal.aborted;
    }

    async request<T>(method: string, params: unknown): Promise<T> {
        if (this.closed) throw closedError();
        try {
            return await this.rpc.sendRequest<T>(`ahp.${method}`, params);
        } catch (error) {
            if (error instanceof ResponseError && error.code === -32601) {
                throw new Error(
                    "This runtime does not support native AHP endpoints. Use a matching runtime with ahp.* support.",
                    { cause: error }
                );
            }
            throw error;
        }
    }

    async create(options: AhpEndpointOptions): Promise<AhpEndpoint> {
        assertNotCallback();
        const limits = {
            maxMessageBytes: options.limits?.maxMessageBytes ?? MAX_LIMITS.maxMessageBytes,
            maxQueuedMessages: options.limits?.maxQueuedMessages ?? MAX_LIMITS.maxQueuedMessages,
            maxBufferedBytes: options.limits?.maxBufferedBytes ?? MAX_LIMITS.maxBufferedBytes,
        };
        for (const key of ["maxMessageBytes", "maxQueuedMessages", "maxBufferedBytes"] as const) {
            const value = limits[key];
            if (!Number.isSafeInteger(value) || value <= 0) {
                throw new Error("AHP limits must be positive safe integers");
            }
            if (value > MAX_LIMITS[key]) {
                throw new Error(`AHP ${key} cannot exceed ${MAX_LIMITS[key]}`);
            }
        }
        const { endpointId } = await this.request<AhpRegisterEndpointResult>("registerEndpoint", {
            callbacks: {
                createSession: !!options.onCreateSession,
                resumeSession: !!options.onResumeSession,
                listSessions: !!options.onListSessions,
                sessionControl: !!options.onSessionControl,
            },
            ...(options.allowSessionCreation === undefined
                ? {}
                : { allowSessionCreation: options.allowSessionCreation }),
            ...(options.capabilities === undefined ? {} : { capabilities: options.capabilities }),
        });
        if (this.closed) throw closedError();
        const endpoint = new Endpoint(endpointId, this, options, limits, () =>
            this.endpoints.delete(endpointId)
        );
        this.endpoints.set(endpointId, endpoint);
        return endpoint;
    }

    close(error: Error): void {
        if (this.closed) return;
        this.lifetime.abort(error);
        for (const endpoint of this.endpoints.values()) endpoint.finish(error);
        for (const subscription of this.subscriptions) subscription.dispose();
        this.subscriptions = [];
    }
}

type Limits = Required<NonNullable<AhpEndpointOptions["limits"]>>;

class Endpoint implements AhpEndpoint {
    readonly connections = new Map<string, LogicalConnection>();
    private lifetime = new AbortController();
    private callbacks = new Map<AbortController, string | undefined>();
    private disposing?: Promise<void>;

    constructor(
        readonly id: string,
        private readonly registry: AhpEndpointRegistry,
        private readonly options: AhpEndpointOptions,
        readonly limits: Limits,
        private readonly remove: () => void
    ) {}

    get closed(): boolean {
        return this.lifetime.signal.aborted;
    }

    request<T>(method: string, params: object = {}): Promise<T> {
        assertNotCallback();
        if (this.closed) return Promise.reject(closedError());
        return this.registry.request<T>(method, { endpointId: this.id, ...params });
    }

    async openConnection(options: AhpConnectionOptions): Promise<AhpConnection> {
        const { connectionId } = await this.request<AhpOpenConnectionResult>("openConnection");
        if (this.closed) throw closedError();
        const connection = new LogicalConnection(connectionId, this, options);
        this.connections.set(connectionId, connection);
        return connection;
    }

    refreshExposure(): Promise<void> {
        return this.request("refreshExposure");
    }

    setCapabilities(capabilities: unknown): Promise<void> {
        return this.request("setCapabilities", { capabilities });
    }

    dispose(): Promise<void> {
        assertNotCallback();
        if (this.disposing) return this.disposing;
        if (this.closed) return Promise.resolve();
        const disposal = this.registry.request<void>("disposeEndpoint", { endpointId: this.id });
        this.finish();
        this.disposing = disposal;
        return disposal;
    }

    finish(error?: Error): void {
        if (this.closed) return;
        this.lifetime.abort(error ?? closedError());
        for (const controller of this.callbacks.keys()) controller.abort(error ?? closedError());
        for (const connection of this.connections.values()) connection.finish(error);
        this.remove();
    }

    connectionFinished(id: string): void {
        this.connections.delete(id);
        for (const [controller, connectionId] of this.callbacks) {
            if (connectionId === id) controller.abort(closedError());
        }
    }

    async invoke(request: CallbackRequest, token: CancellationToken): Promise<unknown> {
        if (this.closed) throw closedError();
        if (
            (request.method === "createSession" || request.method === "resumeSession") &&
            !this.connections.has(request.params.connectionId)
        ) {
            throw closedError();
        }
        const controller = new AbortController();
        this.callbacks.set(
            controller,
            "connectionId" in request.params ? request.params.connectionId : undefined
        );
        const subscription = token.onCancellationRequested(() => controller.abort());
        if (token.isCancellationRequested) controller.abort();
        const context = { signal: controller.signal };
        let abortListener: (() => void) | undefined;
        try {
            const cancelled = new Promise<never>((_, reject) => {
                abortListener = () => reject(new ResponseError(-32800, "AHP callback cancelled"));
                controller.signal.addEventListener("abort", abortListener, { once: true });
                if (controller.signal.aborted) abortListener();
            });
            const result = callbackScope.run(true, async () => {
                controller.signal.throwIfAborted();
                switch (request.method) {
                    case "createSession": {
                        if (!this.options.onCreateSession) throw new Error("No create callback");
                        const { endpointId: _endpointId, ...params } = request.params;
                        const session = await this.options.onCreateSession(params, context);
                        return { sessionId: session.sessionId };
                    }
                    case "resumeSession": {
                        if (!this.options.onResumeSession) throw new Error("No resume callback");
                        const { endpointId: _endpointId, ...params } = request.params;
                        const session = await this.options.onResumeSession(params, context);
                        return { sessionId: session.sessionId };
                    }
                    case "listSessions": {
                        if (!this.options.onListSessions) throw new Error("No list callback");
                        const sessions = await this.options.onListSessions(context);
                        return { sessionIds: sessions.map((session) => session.sessionId) };
                    }
                    case "sessionControl": {
                        if (!this.options.onSessionControl) throw new Error("No control callback");
                        const { endpointId: _endpointId, ...params } = request.params;
                        return this.options.onSessionControl(params, context);
                    }
                }
            });
            return await Promise.race([cancelled, result]);
        } finally {
            subscription.dispose();
            if (abortListener) controller.signal.removeEventListener("abort", abortListener);
            this.callbacks.delete(controller);
        }
    }
}

interface InputMessage {
    message: string;
    bytes: number;
    resolve: () => void;
    reject: (error: Error) => void;
}

interface OutputMessage {
    message: string;
    bytes: number;
    resolve: (value: null) => void;
    reject: (error: Error) => void;
    cancellation?: Disposable;
}

class LogicalConnection implements AhpConnection {
    private closed = false;
    private closing?: Promise<void>;
    private input: InputMessage[] = [];
    private output: OutputMessage[] = [];
    private inputBytes = 0;
    private outputBytes = 0;
    private sending = false;
    private delivering = false;

    constructor(
        readonly id: string,
        private readonly endpoint: Endpoint,
        private readonly options: AhpConnectionOptions
    ) {}

    private check(message: string, count: number, bytes: number): number {
        if (typeof message !== "string") throw new Error("AHP messages must be strings");
        const size = Buffer.byteLength(message, "utf8");
        const limits = this.endpoint.limits;
        if (size > limits.maxMessageBytes) throw new Error("AHP message size limit exceeded");
        if (count >= limits.maxQueuedMessages || bytes + size > limits.maxBufferedBytes) {
            throw new Error("AHP connection buffer limit exceeded");
        }
        return size;
    }

    async send(message: string): Promise<void> {
        assertNotCallback();
        if (this.closed) throw closedError();
        let bytes: number;
        try {
            bytes = this.check(message, this.input.length, this.inputBytes);
        } catch (error) {
            this.fail(asError(error));
            throw error;
        }
        return new Promise<void>((resolve, reject) => {
            this.input.push({ message, bytes, resolve, reject });
            this.inputBytes += bytes;
            void this.drainInput();
        });
    }

    private async drainInput(): Promise<void> {
        if (this.sending) return;
        this.sending = true;
        try {
            while (!this.closed && this.input.length) {
                const item = this.input[0];
                try {
                    await this.endpoint.request("send", {
                        connectionId: this.id,
                        message: item.message,
                    });
                } catch (error) {
                    this.fail(asError(error));
                    return;
                }
                if (this.closed) return;
                this.input.shift();
                this.inputBytes -= item.bytes;
                item.resolve();
            }
        } finally {
            this.sending = false;
        }
    }

    async receive(message: string, token: CancellationToken): Promise<null> {
        if (this.closed) throw closedError();
        let bytes: number;
        try {
            bytes = this.check(message, this.output.length, this.outputBytes);
        } catch (error) {
            this.fail(asError(error));
            throw error;
        }
        return new Promise<null>((resolve, reject) => {
            const item: OutputMessage = { message, bytes, resolve, reject };
            this.output.push(item);
            this.outputBytes += bytes;
            const cancel = () => {
                this.fail(new ResponseError(-32800, "AHP output delivery cancelled"));
            };
            item.cancellation = token.onCancellationRequested(cancel);
            if (token.isCancellationRequested) cancel();
            void this.drainOutput();
        });
    }

    private async drainOutput(): Promise<void> {
        if (this.delivering) return;
        this.delivering = true;
        try {
            while (!this.closed && this.output.length) {
                const item = this.output[0];
                try {
                    await this.options.onMessage(item.message);
                } catch (error) {
                    this.fail(asError(error));
                    return;
                }
                if (this.closed) return;
                this.output.shift();
                this.outputBytes -= item.bytes;
                item.cancellation?.dispose();
                // This acknowledges physical delivery, not the opaque AHP request.
                item.resolve(null);
            }
        } finally {
            this.delivering = false;
        }
    }

    private fail(error: Error): void {
        if (this.closed) return;
        const cleanup = this.endpoint.request<void>("closeConnection", { connectionId: this.id });
        this.finish(error);
        // A failed transport cleanup is observable by an explicit close(), while
        // onClose already reports the original delivery/admission failure.
        this.closing = cleanup;
        void cleanup.catch((cleanupError: unknown) => {
            console.error("Failed to close native AHP connection", cleanupError);
        });
    }

    close(): Promise<void> {
        assertNotCallback();
        if (this.closing) return this.closing;
        if (this.closed) return Promise.resolve();
        this.closing = this.endpoint.request("closeConnection", { connectionId: this.id });
        this.finish();
        return this.closing;
    }

    finish(error?: Error): void {
        if (this.closed) return;
        this.closed = true;
        for (const item of this.input) item.reject(error ?? closedError());
        for (const item of this.output) {
            item.cancellation?.dispose();
            item.reject(error ?? closedError());
        }
        this.input = [];
        this.output = [];
        this.inputBytes = this.outputBytes = 0;
        this.endpoint.connectionFinished(this.id);
        try {
            this.options.onClose?.(error);
        } catch (callbackError) {
            // onClose is a terminal notification, not a request; report a broken
            // observer without interrupting other connections' shutdown.
            console.error("AHP onClose callback failed", callbackError);
        }
    }
}

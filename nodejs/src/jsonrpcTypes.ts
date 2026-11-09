/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// Keep published declarations independent of vscode-jsonrpc's exports-only type resolution.
import { CancellationTokenSource } from "vscode-jsonrpc/node";

export interface Disposable {
    dispose(): void;
}

export interface CancellationToken {
    readonly isCancellationRequested: boolean;
    readonly onCancellationRequested: <T>(
        listener: (event: T) => unknown,
        thisArgs?: unknown,
        disposables?: Disposable[]
    ) => Disposable;
}

export interface MessageConnection {
    sendRequest<R>(method: string, params?: unknown, token?: CancellationToken): Promise<R>;
    onRequest<P, R>(
        method: string,
        handler: (params: P, token: CancellationToken) => R | Promise<R>
    ): Disposable;
    onNotification<P>(method: string, handler: (params: P) => void | Promise<void>): Disposable;
    onClose(listener: () => void): Disposable;
    onDispose(listener: () => void): Disposable;
}

/** Sends a cancellable read without retaining the caller's abort listener after completion. */
export async function sendRequestWithSignal<R>(
    connection: MessageConnection,
    method: string,
    params: unknown,
    signal?: AbortSignal
): Promise<R> {
    if (!signal) return connection.sendRequest(method, params);
    signal.throwIfAborted();
    const cancellation = new CancellationTokenSource();
    const abort = () => cancellation.cancel();
    signal.addEventListener("abort", abort, { once: true });
    try {
        return await connection.sendRequest(method, params, cancellation.token);
    } finally {
        signal.removeEventListener("abort", abort);
        cancellation.dispose();
    }
}

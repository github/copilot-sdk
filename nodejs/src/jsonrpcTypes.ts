/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// Keep published declarations independent of vscode-jsonrpc's exports-only type resolution.
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
    sendRequest<R>(method: string, params?: unknown): Promise<R>;
    onRequest<P, R>(
        method: string,
        handler: (params: P, token: CancellationToken) => R | Promise<R>
    ): Disposable;
    onNotification<P>(method: string, handler: (params: P) => void | Promise<void>): Disposable;
    onClose(listener: () => void): Disposable;
    onDispose(listener: () => void): Disposable;
}

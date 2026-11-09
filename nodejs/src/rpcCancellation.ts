/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { CancellationTokenSource } from "vscode-jsonrpc/node";
import type { MessageConnection } from "./jsonrpcTypes.js";

/** Bridges a caller's abort signal to standard JSON-RPC request cancellation. */
export async function sendCancellableRequest<R>(
    connection: MessageConnection,
    method: string,
    params: unknown,
    signal?: AbortSignal
): Promise<R> {
    signal?.throwIfAborted();
    if (!signal) return connection.sendRequest<R>(method, params);

    const cancellation = new CancellationTokenSource();
    const abort = () => cancellation.cancel();
    signal.addEventListener("abort", abort, { once: true });
    try {
        return await connection.sendRequest<R>(method, params, cancellation.token);
    } finally {
        signal.removeEventListener("abort", abort);
        cancellation.dispose();
    }
}

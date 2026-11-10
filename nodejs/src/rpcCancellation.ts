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
    signal?: AbortSignal,
    rejectOnAbort = false
): Promise<R> {
    signal?.throwIfAborted();
    if (!signal) return connection.sendRequest<R>(method, params);

    const cancellation = new CancellationTokenSource();
    let rejectAbort: ((reason: unknown) => void) | undefined;
    const aborted = rejectOnAbort
        ? new Promise<never>((_, reject) => {
              rejectAbort = reject;
          })
        : undefined;
    const abort = () => {
        cancellation.cancel();
        rejectAbort?.(signal.reason);
    };
    signal.addEventListener("abort", abort, { once: true });
    try {
        const request = connection.sendRequest<R>(method, params, cancellation.token);
        return await (aborted ? Promise.race([request, aborted]) : request);
    } finally {
        signal.removeEventListener("abort", abort);
        cancellation.dispose();
    }
}

/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import {
    CancellationTokenSource,
    ResponseError,
    type CancellationToken,
    type Disposable,
    type MessageConnection,
} from "vscode-jsonrpc/node.js";
import type { EntraTokenProvider, EntraTokenProviderArgs } from "./types.js";

export function registerEntraTokenProvider(
    connection: MessageConnection,
    provider: EntraTokenProvider
): void {
    const closed = new CancellationTokenSource();
    const connectionClosed = closed.token;
    const close = connection.onClose(() => closed.cancel());
    const dispose = connection.onDispose(() => {
        closed.cancel();
        closed.dispose();
        close.dispose();
        dispose.dispose();
    });
    connection.onRequest(
        "entraToken.getToken",
        async ({ request }: { request: EntraTokenProviderArgs }, token: CancellationToken) => {
            const combined = new CancellationTokenSource();
            const subscriptions: Disposable[] = [];
            const checkCancellation = () => {
                if (connectionClosed.isCancellationRequested || token.isCancellationRequested) {
                    combined.cancel();
                    throw new ResponseError(-32800, "Entra token acquisition cancelled");
                }
            };
            try {
                const cancelled = new Promise<never>((_resolve, reject) => {
                    const cancel = () => {
                        combined.cancel();
                        reject(new ResponseError(-32800, "Entra token acquisition cancelled"));
                    };
                    subscriptions.push(
                        token.onCancellationRequested(cancel),
                        connectionClosed.onCancellationRequested(cancel)
                    );
                });
                checkCancellation();
                const result = await Promise.race([
                    Promise.resolve().then(() => {
                        checkCancellation();
                        return provider(request, combined.token);
                    }),
                    cancelled,
                ]);
                checkCancellation();
                if (result === undefined || result === null) {
                    throw new ResponseError(-32603, "Entra token provider failed");
                }
                return result;
            } catch {
                checkCancellation();
                throw new ResponseError(-32603, "Entra token provider failed");
            } finally {
                for (const subscription of subscriptions) subscription.dispose();
                combined.dispose();
            }
        }
    );
}

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
import type {
    InstallationConfirmationRequest,
    InstallationDecision,
    InstallationsConfirmResult,
} from "./generated/rpc.js";

/** The `installations.confirm` receiver, invoked with the transport's request cancellation. */
export interface InstallationConfirmationReceiver {
    confirm(
        request: InstallationConfirmationRequest,
        token: CancellationToken
    ): Promise<InstallationsConfirmResult>;
}

/**
 * Collects an explicit human decision for the complete runtime review.
 * Match operationId and policySessionId to the exact action registered by the host
 * before displaying it. Refuse unknown actions or incomplete reviews; never infer
 * authority from the current session or a global pending slot.
 *
 * The SDK echoes the original challenge and fingerprint. Separately spawned UI
 * work must observe the token to retire itself when the request or connection ends.
 * Registering this callback does not enable installation capabilities.
 * @experimental
 */
export type InstallationConfirmationHandler = (
    request: InstallationConfirmationRequest,
    token: CancellationToken
) => InstallationDecision | Promise<InstallationDecision>;

export function createInstallationConfirmationAdapter(
    connection: MessageConnection,
    handler: InstallationConfirmationHandler
): InstallationConfirmationReceiver {
    const closed = new CancellationTokenSource();
    const closeSubscription = connection.onClose(() => closed.cancel());
    const disposeSubscription = connection.onDispose(() => {
        closed.cancel();
        closed.dispose();
        closeSubscription.dispose();
        disposeSubscription.dispose();
    });

    return {
        async confirm(request, token) {
            if (!token) {
                throw new Error("Installation confirmation request cancellation is unavailable");
            }
            const { confirmationId, reviewFingerprint } = request;
            const combined = new CancellationTokenSource();
            const subscriptions: Disposable[] = [];
            const cancelRequest = () => combined.cancel();
            const cancelConnection = () => combined.cancel();
            const checkCancellation = () => {
                if (closed.token.isCancellationRequested) {
                    throw new Error("Installation confirmation connection closed");
                }
                if (token.isCancellationRequested) {
                    throw new ResponseError(-32800, "Installation confirmation request cancelled");
                }
            };
            try {
                subscriptions.push(
                    token.onCancellationRequested(cancelRequest),
                    closed.token.onCancellationRequested(cancelConnection)
                );
                if (token.isCancellationRequested || closed.token.isCancellationRequested) {
                    combined.cancel();
                }
                checkCancellation();
                const cancelled = new Promise<never>((_resolve, reject) => {
                    subscriptions.push(
                        token.onCancellationRequested(() =>
                            reject(
                                new ResponseError(
                                    -32800,
                                    "Installation confirmation request cancelled"
                                )
                            )
                        ),
                        closed.token.onCancellationRequested(() =>
                            reject(new Error("Installation confirmation connection closed"))
                        )
                    );
                });
                const decision = await Promise.race([
                    Promise.resolve().then(() => {
                        checkCancellation();
                        return handler(request, combined.token);
                    }),
                    cancelled,
                ]);
                checkCancellation();
                if (decision !== "confirm" && decision !== "decline" && decision !== "cancel") {
                    throw new Error("Invalid installation confirmation decision");
                }
                return { confirmationId, reviewFingerprint, decision };
            } finally {
                for (const subscription of subscriptions) subscription.dispose();
                combined.dispose();
            }
        },
    };
}

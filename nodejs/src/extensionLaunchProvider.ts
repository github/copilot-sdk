/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import {
    CancellationTokenSource,
    ResponseError,
    type CancellationToken,
    type Disposable,
} from "vscode-jsonrpc/node.js";
import type {
    ExtensionLaunchProviderHandler,
    ExtensionLaunchProviderRegistrationResult,
    ExtensionLaunchProviderResolveRequest,
    ExtensionLaunchProviderResolveResult,
} from "./generated/rpc.js";

function cancelled(): ResponseError {
    return new ResponseError(-32800, "Extension launch provider request cancelled");
}

async function withCancellation<T>(run: () => Promise<T>, token: CancellationToken): Promise<T> {
    if (token.isCancellationRequested) {
        throw cancelled();
    }
    let subscription: Disposable | undefined;
    const cancellation = new Promise<never>((_, reject) => {
        subscription = token.onCancellationRequested(() => reject(cancelled()));
    });
    try {
        // Invoke synchronously, but turn throws into promises before observing both race inputs.
        const operation = (async () => run())();
        const result = await Promise.race([operation, cancellation]);
        if (token.isCancellationRequested) {
            throw cancelled();
        }
        return result;
    } finally {
        subscription?.dispose();
    }
}

/** One launch-provider registration and its outstanding requests on a single connection. */
export class ExtensionLaunchProviderConnection {
    private readonly lifetime = new CancellationTokenSource();
    private registration?: Promise<ExtensionLaunchProviderRegistrationResult>;
    private registered = false;

    readonly handler: ExtensionLaunchProviderHandler = {
        resolve: (params, token) => this.resolve(params, token),
    };

    constructor(
        private readonly provider: ExtensionLaunchProviderHandler,
        private readonly registerProvider: () => Promise<ExtensionLaunchProviderRegistrationResult>
    ) {}

    async register(): Promise<ExtensionLaunchProviderRegistrationResult> {
        if (this.lifetime.token.isCancellationRequested) {
            throw cancelled();
        }
        this.registration ??= withCancellation(async () => {
            const result = await this.registerProvider();
            if (result?.contractVersion !== 1) {
                throw new Error(
                    "Extension launch provider requires contract version 1; the runtime did not acknowledge it."
                );
            }
            if (this.lifetime.token.isCancellationRequested) {
                throw cancelled();
            }
            this.registered = true;
            return result;
        }, this.lifetime.token);
        return this.registration;
    }

    dispose(): void {
        // Materialize the lazy token before cancelling, and make teardown reentrant.
        if (this.lifetime.token.isCancellationRequested) {
            return;
        }
        this.lifetime.cancel();
        this.lifetime.dispose();
    }

    private async resolve(
        params: ExtensionLaunchProviderResolveRequest,
        token?: CancellationToken
    ): Promise<ExtensionLaunchProviderResolveResult> {
        if (!this.registered) {
            throw new Error("Extension launch provider contract has not been acknowledged");
        }
        const request = new CancellationTokenSource();
        const requestToken = request.token;
        const cancelRequest = () => {
            if (!requestToken.isCancellationRequested) {
                request.cancel();
            }
        };
        const connectionSubscription = this.lifetime.token.onCancellationRequested(cancelRequest);
        const requestSubscription = token?.onCancellationRequested(cancelRequest);
        if (this.lifetime.token.isCancellationRequested || token?.isCancellationRequested) {
            cancelRequest();
        }
        try {
            return await withCancellation(
                () => this.provider.resolve(params, requestToken),
                requestToken
            );
        } finally {
            connectionSubscription.dispose();
            requestSubscription?.dispose();
            request.dispose();
        }
    }
}

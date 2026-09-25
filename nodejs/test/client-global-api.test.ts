// Copyright (c) Microsoft Corporation. All rights reserved.

import { PassThrough } from "node:stream";
import { describe, expect, it, onTestFinished, vi } from "vitest";
import {
    CancellationTokenSource,
    createMessageConnection,
    StreamMessageReader,
    StreamMessageWriter,
    type CancellationToken,
} from "vscode-jsonrpc/node.js";
import {
    registerClientGlobalApiHandlers,
    type ClientGlobalApiHandlers,
    type ExtensionLaunchProviderResolveRequest,
    type ExtensionLaunchProviderResolveResult,
} from "../src/generated/rpc.js";

function connect(handlers: ClientGlobalApiHandlers) {
    const outbound = new PassThrough();
    const inbound = new PassThrough();
    const client = createMessageConnection(
        new StreamMessageReader(inbound),
        new StreamMessageWriter(outbound)
    );
    const server = createMessageConnection(
        new StreamMessageReader(outbound),
        new StreamMessageWriter(inbound)
    );
    onTestFinished(() => {
        client.dispose();
        server.dispose();
        inbound.destroy();
        outbound.destroy();
    });
    registerClientGlobalApiHandlers(client, handlers);
    client.listen();
    server.listen();
    return server;
}

const request: ExtensionLaunchProviderResolveRequest = {
    id: "project:extension",
    modulePath: "/extensions/example/index.js",
    name: "Example",
    source: "project",
};

describe("client-global API transport", () => {
    it("awaits a handler and returns its result without a session", async () => {
        let release!: (result: ExtensionLaunchProviderResolveResult) => void;
        const result = new Promise<ExtensionLaunchProviderResolveResult>((resolve) => {
            release = resolve;
        });
        const handler = vi.fn(async () => result);
        const server = connect({ extensionLaunchProvider: { resolve: handler } });
        const response = server.sendRequest("extensionLaunchProvider.resolve", request);
        const completed = vi.fn();
        void response.then(completed);

        await vi.waitFor(() =>
            expect(handler).toHaveBeenCalledWith(
                request,
                expect.objectContaining({ isCancellationRequested: false })
            )
        );
        expect(completed).not.toHaveBeenCalled();
        const launch = { executable: "/app/extension-host", args: ["example"], env: {} };
        release({ launch });

        await expect(response).resolves.toEqual({ launch });
        expect(handler).toHaveBeenCalledTimes(1);
    });

    it("keeps global handlers and responses on their original connection", async () => {
        const firstHandler = vi.fn(async () => ({
            launch: { executable: "/first/host", args: [], env: {} },
        }));
        const secondHandler = vi.fn(async () => ({
            launch: { executable: "/second/host", args: [], env: {} },
        }));
        const first = connect({ extensionLaunchProvider: { resolve: firstHandler } });
        const second = connect({ extensionLaunchProvider: { resolve: secondHandler } });

        const responses = await Promise.all([
            first.sendRequest("extensionLaunchProvider.resolve", request),
            second.sendRequest("extensionLaunchProvider.resolve", request),
        ]);

        expect(responses).toEqual([
            { launch: { executable: "/first/host", args: [], env: {} } },
            { launch: { executable: "/second/host", args: [], env: {} } },
        ]);
        expect(firstHandler).toHaveBeenCalledTimes(1);
        expect(secondHandler).toHaveBeenCalledTimes(1);
    });

    it("continues dispatching requests while a global handler is pending", async () => {
        let release!: (result: ExtensionLaunchProviderResolveResult) => void;
        const result = new Promise<ExtensionLaunchProviderResolveResult>((resolve) => {
            release = resolve;
        });
        const handler = vi.fn(async () => result);
        const tokenHandler = vi.fn(async () => ({ kind: "cancelled" as const }));
        const server = connect({
            extensionLaunchProvider: { resolve: handler },
            gitHubToken: { getToken: tokenHandler },
        });
        const pending = server.sendRequest("extensionLaunchProvider.resolve", request);
        await vi.waitFor(() => expect(handler).toHaveBeenCalledOnce());

        await expect(
            server.sendRequest("gitHubToken.getToken", {
                registrationId: "registration",
                host: "github.com",
                reason: "initial",
            })
        ).resolves.toEqual({ kind: "cancelled" });

        release({});
        await expect(pending).resolves.toEqual({});
    });

    it("returns an error when no global handler is registered", async () => {
        const server = connect({});

        await expect(
            server.sendRequest("extensionLaunchProvider.resolve", request)
        ).rejects.toThrow("No extensionLaunchProvider client-global handler registered");
    });

    it("propagates handler failure without substituting a successful result", async () => {
        const server = connect({
            extensionLaunchProvider: {
                resolve: async () => {
                    throw new Error("Host review unavailable");
                },
            },
        });

        await expect(
            server.sendRequest("extensionLaunchProvider.resolve", request)
        ).rejects.toThrow("Host review unavailable");
    });

    it("forwards real request cancellation to the pending global handler", async () => {
        const cancellation = new CancellationTokenSource();
        onTestFinished(() => cancellation.dispose());
        let observed: CancellationToken | undefined;
        let started!: () => void;
        const entered = new Promise<void>((resolve) => {
            started = resolve;
        });
        const server = connect({
            extensionLaunchProvider: {
                resolve: async (_params, token?: CancellationToken) => {
                    observed = token;
                    started();
                    if (token && !token.isCancellationRequested) {
                        await new Promise<void>((resolve) => {
                            const subscription = token.onCancellationRequested(() => {
                                subscription.dispose();
                                resolve();
                            });
                        });
                    }
                    return {};
                },
            },
        });
        const response = server.sendRequest(
            "extensionLaunchProvider.resolve",
            request,
            cancellation.token
        );

        try {
            await entered;
            expect(observed).toBeDefined();
            expect(observed?.isCancellationRequested).toBe(false);
        } finally {
            cancellation.cancel();
            await expect(response).resolves.toEqual({});
        }
        expect(observed?.isCancellationRequested).toBe(true);
    });
});

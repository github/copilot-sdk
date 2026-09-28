// Copyright (c) Microsoft Corporation. All rights reserved.

import { once } from "node:events";
import { readFileSync } from "node:fs";
import { createServer, type Socket } from "node:net";
import { describe, expect, it, onTestFinished, vi } from "vitest";
import {
    CancellationTokenSource,
    createMessageConnection,
    StreamMessageReader,
    StreamMessageWriter,
    type CancellationToken,
    type MessageConnection,
} from "vscode-jsonrpc/node.js";
import {
    CopilotClient,
    RuntimeConnection,
    type ExtensionLaunchProvider,
    type InstallationConfirmationHandler,
    type InstallationConfirmationRequest,
    type InstallationDecision,
} from "../src/index.js";
import { createInstallationConfirmationAdapter } from "../src/installationConfirmation.js";
import type {
    CatalogClientContract,
    CatalogNegotiatedContract,
    InstallationReview,
    McpInstallationManagementResult,
    McpInstallationResult,
    McpPrepareInstallRequest,
} from "../src/generated/rpc.js";

const capabilities: CatalogNegotiatedContract["grantedCapabilities"] = [
    "catalog-search-credential-required",
    "catalog-search-session-bound",
    "mcp-confirmed-remote-installation",
];
const contract: CatalogClientContract = {
    protocolVersion: 3,
    requiredCapabilities: capabilities,
};
const negotiated: CatalogNegotiatedContract = {
    runtimeProtocolVersion: 3,
    grantedCapabilities: capabilities,
};
const prepareRequest: McpPrepareInstallRequest = {
    contract,
    planHandle: "bound-plan",
    choiceId: "remote-choice",
    policySessionId: "original-session",
    inputs: [],
    secrets: [],
    source: {
        kind: "url",
        mediaType: "application/mcp-server-card+json",
        url: "https://example.test/card",
    },
    secretStorage: "keychain",
};

function request(operation: string): InstallationConfirmationRequest {
    const value: InstallationConfirmationRequest = JSON.parse(
        readFileSync(
            new URL("../../rust/tests/fixtures/installation_confirmation.json", import.meta.url),
            "utf8"
        )
    );
    return {
        ...value,
        operationId: operation,
        confirmationId: `challenge-${operation}`,
        reviewFingerprint: `fingerprint-${operation}`,
    };
}

function deferred<T>() {
    let resolve!: (value: T) => void;
    const promise = new Promise<T>((complete) => {
        resolve = complete;
    });
    return { promise, resolve };
}

async function connect(
    handler?: InstallationConfirmationHandler,
    extensionLaunchProvider?: ExtensionLaunchProvider
) {
    const accepted = deferred<{ connection: MessageConnection; socket: Socket }>();
    const server = createServer((socket) => {
        const connection = createMessageConnection(
            new StreamMessageReader(socket),
            new StreamMessageWriter(socket)
        );
        connection.onRequest("connect", () => ({
            ok: true,
            protocolVersion: 3,
            version: "confirmation-transport-test",
        }));
        connection.onRequest("registerExtensionLaunchProvider", () => ({}));
        connection.listen();
        accepted.resolve({ connection, socket });
    });
    server.listen(0, "127.0.0.1");
    await once(server, "listening");
    const address = server.address();
    if (!address || typeof address === "string") throw new Error("Missing test listener");
    const client = new CopilotClient({
        connection: RuntimeConnection.forUri(`127.0.0.1:${address.port}`),
        installationConfirmationHandler: handler,
        extensionLaunchProvider,
    });
    onTestFinished(async () => {
        await client.forceStop();
        const peer = await accepted.promise;
        peer.connection.dispose();
        peer.socket.destroy();
        await new Promise<void>((resolve) => server.close(() => resolve()));
    });
    await client.start();
    return { client, ...(await accepted.promise) };
}

describe("installation confirmation on the actual client transport", () => {
    it("keeps MCP and Skill confirmation reviews strongly typed", () => {
        const mcp = request("typed-mcp");
        if (mcp.review.resource !== "mcp") throw new Error("expected MCP review");
        expect(mcp.review.review.action).toBe("install");

        const skillReview: InstallationReview = {
            resource: "skill",
            review: {
                action: "install",
                name: "demo",
                description: "Demo Skill",
                catalogue: {
                    resourceId: "123",
                    displayName: "Demo Skill",
                    description: "Demo Skill",
                    publisher: "Octo",
                    source: "https://agentfinder.github.com",
                },
                source: {
                    resourceId: "123",
                    catalogRevisionId: "1",
                    repositoryId: "42",
                    repository: "octo/demo",
                    revision: "a".repeat(40),
                    root: "",
                    descriptorDigest: `sha256:${"1".repeat(64)}`,
                    bundleDigest: `sha256:${"2".repeat(64)}`,
                },
                target: {
                    scope: "personal",
                    displayLabel: "~/.copilot/skills/demo",
                    relativePath: "skills/demo",
                    diagnosticsAbsolutePath: "/home/skills/demo",
                },
                installsDisabled: true,
                files: [
                    {
                        path: "SKILL.md",
                        sizeBytes: 12,
                        mediaType: "text/markdown",
                        executable: false,
                        digest: `sha256:${"3".repeat(64)}`,
                    },
                ],
                totalBytes: 12,
                entrypointPath: "SKILL.md",
                entrypointContent: "---\nname: demo\n---",
            },
        };
        const skill: InstallationConfirmationRequest = {
            ...mcp,
            review: skillReview,
        };
        if (skill.review.resource !== "skill") throw new Error("expected Skill review");
        expect(skill.review.review.name).toBe("demo");
    });

    it("forwards all eight typed installation methods and preserves unavailable results", async () => {
        const { client, connection } = await connect();
        const refusal = {
            kind: "outcome",
            negotiated,
            outcome: { kind: "refused", reason: "lifecycle-unavailable" },
        } satisfies McpInstallationManagementResult & McpInstallationResult;
        const apply = {
            contract,
            operationId: "prepared-operation",
            policySessionId: "original-session",
        };
        const removal = {
            contract,
            installationId: "owned-installation",
            policySessionId: "original-session",
        };
        const applyRemoval = {
            contract,
            planHandle: "removal-plan",
            policySessionId: "original-session",
        };
        const inventory = { contract, policySessionId: "original-session" };
        const control = { contract, operationId: "prepared-operation" };
        const calls = [
            {
                method: "mcp.prepareInstall",
                params: prepareRequest,
                invoke: () => client.rpc.mcp.prepareInstall(prepareRequest),
            },
            {
                method: "mcp.applyInstall",
                params: apply,
                invoke: () => client.rpc.mcp.applyInstall(apply),
            },
            {
                method: "mcp.planUninstall",
                params: removal,
                invoke: () => client.rpc.mcp.planUninstall(removal),
            },
            {
                method: "mcp.applyUninstall",
                params: applyRemoval,
                invoke: () => client.rpc.mcp.applyUninstall(applyRemoval),
            },
            {
                method: "mcp.installations.list",
                params: inventory,
                invoke: () => client.rpc.mcp.installations.list(inventory),
            },
            {
                method: "mcp.installations.recover",
                params: inventory,
                invoke: () => client.rpc.mcp.installations.recover(inventory),
            },
            {
                method: "mcp.installations.status",
                params: control,
                invoke: () => client.rpc.mcp.installations.status(control),
            },
            {
                method: "mcp.installations.cancel",
                params: control,
                invoke: () => client.rpc.mcp.installations.cancel(control),
            },
        ];
        for (const call of calls) {
            const received = vi.fn((params: unknown) => {
                expect(params).toEqual(call.params);
                return refusal;
            });
            connection.onRequest(call.method, received);
            await expect(call.invoke()).resolves.toEqual(refusal);
            expect(received).toHaveBeenCalledOnce();
        }
    });

    it("registers prepared IDs before apply and refuses late A without approving successor B", async () => {
        const known = new Map<string, string>();
        const handler = vi.fn<InstallationConfirmationHandler>((incoming) => {
            if (known.get(incoming.operationId) !== incoming.policySessionId) {
                throw new Error("Unknown original operation");
            }
            return "decline";
        });
        const { client, connection } = await connect(handler);
        connection.onRequest(
            "mcp.prepareInstall",
            (params: McpPrepareInstallRequest) =>
                ({
                    kind: "outcome",
                    negotiated,
                    outcome: {
                        kind: "install-prepared",
                        operation: {
                            operationId: `operation-${params.planHandle}`,
                            expiresAtEpochMs: 42,
                        },
                    },
                }) satisfies McpInstallationManagementResult
        );
        const prepare = async (planHandle: string) => {
            const result = await client.rpc.mcp.prepareInstall({ ...prepareRequest, planHandle });
            if (result.kind !== "outcome" || result.outcome.kind !== "install-prepared") {
                throw new Error("Expected a prepared operation");
            }
            expect(result.outcome.operation.expiresAtEpochMs).toBe(42);
            const id = result.outcome.operation.operationId;
            known.set(id, prepareRequest.policySessionId);
            return id;
        };
        const a = await prepare("a");
        expect(handler).not.toHaveBeenCalled();
        connection.onRequest("mcp.installations.cancel", (params: { operationId: string }) => {
            expect(params).toEqual({ contract, operationId: a });
            return {
                kind: "outcome",
                negotiated,
                outcome: {
                    kind: "operation",
                    operation: {
                        phase: "completed",
                        operationId: a,
                        cancellationRequested: true,
                        outcome: { kind: "cancelled", operationId: a },
                    },
                },
            } satisfies McpInstallationManagementResult;
        });
        await client.rpc.mcp.installations.cancel({ contract, operationId: a });
        known.delete(a);
        const b = await prepare("b");
        expect(b).not.toBe(a);
        expect(handler).not.toHaveBeenCalled();
        await expect(connection.sendRequest("installations.confirm", request(a))).rejects.toThrow(
            "Unknown original operation"
        );
        connection.onRequest("mcp.applyInstall", async (params: { operationId: string }) => {
            expect(params).toEqual({
                contract,
                operationId: b,
                policySessionId: "original-session",
            });
            const response = await connection.sendRequest("installations.confirm", request(b));
            expect(response).toEqual({
                confirmationId: `challenge-${b}`,
                reviewFingerprint: `fingerprint-${b}`,
                decision: "decline",
            });
            return {
                kind: "outcome",
                negotiated,
                outcome: { kind: "declined", operationId: b },
            } satisfies McpInstallationResult;
        });
        await expect(
            client.rpc.mcp.applyInstall({
                contract,
                operationId: b,
                policySessionId: "original-session",
            })
        ).resolves.toMatchObject({ outcome: { kind: "declined", operationId: b } });
        expect(handler).toHaveBeenCalledTimes(2);
    });

    it("retains prepared OAuth identity and original session through login and cancellation", async () => {
        const { client, connection } = await connect();
        connection.onRequest("session.create", () => ({ sessionId: "original-session" }));
        const session = await client.createSession({
            sessionId: "original-session",
            onPermissionRequest: () => ({ kind: "approved" }),
        });
        const identity = { serverName: "example", expectedInstallationId: "owned-installation" };
        const loginId = "runtime-issued-login";
        connection.onRequest("session.mcp.oauth.prepareLogin", (params: unknown) => {
            expect(params).toEqual({
                sessionId: "original-session",
                ...identity,
                forceReauth: true,
            });
            return { loginId, expiresAt: "2026-09-24T15:00:00Z" };
        });
        const prepared = await session.rpc.mcp.oauth.prepareLogin({
            ...identity,
            forceReauth: true,
        });
        expect(prepared.loginId).toBe(loginId);
        const started = deferred<void>();
        const completed = deferred<{ loginId: string; status: "cancelled" }>();
        connection.onRequest("session.mcp.oauth.login", (params: unknown) => {
            expect(params).toEqual({
                sessionId: "original-session",
                ...identity,
                loginId: prepared.loginId,
            });
            started.resolve();
            return completed.promise;
        });
        const login = session.rpc.mcp.oauth.login({ ...identity, loginId: prepared.loginId });
        await started.promise;
        connection.onRequest("session.mcp.oauth.cancelLogin", (params: unknown) => {
            expect(params).toEqual({
                sessionId: "original-session",
                expectedInstallationId: identity.expectedInstallationId,
                loginId: prepared.loginId,
            });
            completed.resolve({ loginId, status: "cancelled" });
            return { cancelled: true };
        });
        await expect(
            session.rpc.mcp.oauth.cancelLogin({
                expectedInstallationId: identity.expectedInstallationId,
                loginId: prepared.loginId,
            })
        ).resolves.toEqual({ cancelled: true });
        await expect(login).resolves.toEqual({ loginId, status: "cancelled" });
    });

    it("presents typed full review and echoes the original challenge and fingerprint", async () => {
        const handler = vi.fn<InstallationConfirmationHandler>(
            async (incoming, token): Promise<InstallationDecision> => {
                expect(token.isCancellationRequested).toBe(false);
                expect(incoming).toEqual(request("a"));
                expect(incoming.review.review.action).toBe("install");
                if (incoming.review.review.action !== "install")
                    throw new Error("Unexpected review");
                expect(incoming.review.review.effectiveConfiguration).toEqual({
                    transport: "streamable-http",
                    url: "https://example.test/mcp",
                    headers: { "X-Region": "eu" },
                    tools: ["*"],
                });
                incoming.confirmationId = "mutated-by-handler";
                incoming.reviewFingerprint = "mutated-by-handler";
                return "confirm";
            }
        );
        const { connection } = await connect(handler);

        await expect(
            connection.sendRequest("installations.confirm", request("a"))
        ).resolves.toEqual({
            confirmationId: "challenge-a",
            reviewFingerprint: "fingerprint-a",
            decision: "confirm",
        });
        expect(handler).toHaveBeenCalledOnce();
    });

    it("keeps concurrent same-session reviews independent and allows out-of-order decisions", async () => {
        const a = deferred<InstallationDecision>();
        const b = deferred<InstallationDecision>();
        const handler = vi.fn<InstallationConfirmationHandler>((incoming) =>
            incoming.operationId === "a" ? a.promise : b.promise
        );
        const { connection } = await connect(handler);
        const first = connection.sendRequest("installations.confirm", request("a"));
        const second = connection.sendRequest("installations.confirm", request("b"));
        await vi.waitFor(() => expect(handler).toHaveBeenCalledTimes(2));
        await expect(
            connection.sendRequest("gitHubToken.getToken", {
                registrationId: "unknown",
                host: "github.com",
                reason: "initial",
            })
        ).rejects.toThrow("No GitHub token provider");

        b.resolve("decline");
        await expect(second).resolves.toEqual({
            confirmationId: "challenge-b",
            reviewFingerprint: "fingerprint-b",
            decision: "decline",
        });
        a.resolve("confirm");
        await expect(first).resolves.toMatchObject({ confirmationId: "challenge-a" });
    });

    it("reaches and cancels a confirmation queued behind a blocked global callback", async () => {
        const release = deferred<void>();
        const resolveEntered = deferred<void>();
        const extensionLaunchProvider: ExtensionLaunchProvider = {
            resolve: async () => {
                resolveEntered.resolve();
                await release.promise;
                return {};
            },
        };
        let token: CancellationToken | undefined;
        const pending = deferred<InstallationDecision>();
        const { connection } = await connect((_request, incoming) => {
            token = incoming;
            return pending.promise;
        }, extensionLaunchProvider);
        const cancellation = new CancellationTokenSource();
        onTestFinished(() => cancellation.dispose());

        const blocked = connection.sendRequest("extensionLaunchProvider.resolve", {
            id: "project:blocked",
            modulePath: "/extensions/blocked/index.js",
            name: "Blocked",
            source: "project",
        });
        await resolveEntered.promise;
        const confirmation = connection.sendRequest(
            "installations.confirm",
            request("behind-blocked-callback"),
            cancellation.token
        );
        const cancelled = expect(confirmation).rejects.toMatchObject({ code: -32800 });
        await vi.waitFor(() => expect(token).toBeDefined());
        cancellation.cancel();
        await cancelled;
        expect(token?.isCancellationRequested).toBe(true);

        release.resolve();
        await expect(blocked).resolves.toEqual({});
        pending.resolve("confirm");
    });

    it("retires cancelled A before a late decision and does not rebind it to successor B", async () => {
        const cancellation = new CancellationTokenSource();
        onTestFinished(() => cancellation.dispose());
        const a = deferred<InstallationDecision>();
        const tokens = new Map<string, CancellationToken>();
        const handler: InstallationConfirmationHandler = (incoming, token) => {
            tokens.set(incoming.operationId, token);
            return incoming.operationId === "a" ? a.promise : "confirm";
        };
        const { connection } = await connect(handler);
        const first = connection.sendRequest(
            "installations.confirm",
            request("a"),
            cancellation.token
        );
        const cancelled = expect(first).rejects.toMatchObject({ code: -32800 });
        await vi.waitFor(() => expect(tokens.has("a")).toBe(true));
        cancellation.cancel();
        await cancelled;
        expect(tokens.get("a")?.isCancellationRequested).toBe(true);
        a.resolve("confirm");
        await expect(
            connection.sendRequest("installations.confirm", request("b"))
        ).resolves.toMatchObject({
            confirmationId: "challenge-b",
            reviewFingerprint: "fingerprint-b",
        });
        expect(tokens.get("b")?.isCancellationRequested).toBe(false);
    });

    it("rejects when cancellation and a decision are ready together", async () => {
        const cancellation = new CancellationTokenSource();
        onTestFinished(() => cancellation.dispose());
        const decision = deferred<InstallationDecision>();
        let token: CancellationToken | undefined;
        const disposable = { dispose() {} };
        const adapter = createInstallationConfirmationAdapter(
            {
                onClose: () => disposable,
                onDispose: () => disposable,
            } as unknown as MessageConnection,
            (_request, incoming) => {
                token = incoming;
                return decision.promise;
            }
        );
        const confirmation = adapter.confirm(request("simultaneous"), cancellation.token);
        const cancelled = expect(confirmation).rejects.toMatchObject({ code: -32800 });
        await vi.waitFor(() => expect(token).toBeDefined());
        decision.resolve("confirm");
        cancellation.cancel();
        await cancelled;
        expect(token?.isCancellationRequested).toBe(true);
    });

    it("connection closure cancels all outstanding reviews on the original connection", async () => {
        const pending = deferred<InstallationDecision>();
        const tokens = new Map<string, CancellationToken>();
        const first = await connect((incoming, token) => {
            tokens.set(incoming.operationId, token);
            return pending.promise;
        });
        const second = await connect(() => "decline");
        const firstResponse = first.connection.sendRequest("installations.confirm", request("a"));
        const secondResponse = first.connection.sendRequest("installations.confirm", request("b"));
        const firstClosed = expect(firstResponse).rejects.toThrow();
        const secondClosed = expect(secondResponse).rejects.toThrow();
        await vi.waitFor(() => expect(tokens.size).toBe(2));
        first.socket.destroy();
        await vi.waitFor(() => {
            expect(tokens.get("a")?.isCancellationRequested).toBe(true);
            expect(tokens.get("b")?.isCancellationRequested).toBe(true);
        });
        first.connection.dispose();
        await firstClosed;
        await secondClosed;
        pending.resolve("confirm");
        await expect(
            second.connection.sendRequest("installations.confirm", request("c"))
        ).resolves.toMatchObject({
            confirmationId: "challenge-c",
            decision: "decline",
        });
    });

    it("refuses missing handlers and propagates explicit handler refusal", async () => {
        const missing = await connect();
        await expect(
            missing.connection.sendRequest("installations.confirm", request("a"))
        ).rejects.toThrow("No installations client-global handler registered");
        const refusing = await connect(() => {
            throw new Error("Unknown original operation or incomplete review");
        });
        await expect(
            refusing.connection.sendRequest("installations.confirm", request("a"))
        ).rejects.toThrow("Unknown original operation or incomplete review");
    });

    it("does not infer optional legacy session authority", async () => {
        const incoming = request("legacy");
        delete incoming.policySessionId;
        const handler = vi.fn<InstallationConfirmationHandler>((params) => {
            expect(params.policySessionId).toBeUndefined();
            return "decline";
        });
        const { connection } = await connect(handler);
        await expect(
            connection.sendRequest("installations.confirm", incoming)
        ).resolves.toMatchObject({
            decision: "decline",
        });
    });
});

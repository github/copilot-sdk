/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it, vi } from "vitest";
import type { MessageConnection } from "vscode-jsonrpc";

import { createServerRpc } from "../src/generated/rpc.js";
import type {
    AuthIdentityMetadata,
    ConnectorSessionAccount,
    ConnectorSessionAccountResult,
} from "../src/index.js";
import { CopilotSession } from "../src/session.js";

describe("client Connector discovery RPC", () => {
    it("lists Connectors without creating a session", async () => {
        const sendRequest = vi.fn(async (method: string) => {
            if (method === "connectors.getCapabilities") {
                return {
                    apiVersion: 1,
                    availability: "enabled",
                    opaqueAccountSelection: true,
                    conditionalCache: true,
                };
            }
            if (method === "connectors.getAccounts") {
                return {
                    availability: "enabled",
                    accounts: [
                        {
                            accountId: "account-1",
                            authInfo: {
                                type: "user",
                                host: "https://github.com",
                                login: "octocat",
                            },
                        },
                    ],
                };
            }
            return {
                accountId: "account-1",
                revision: 1,
                refreshedAtMs: 1,
                connectors: [
                    {
                        name: "calendar",
                        displayName: "Calendar",
                        status: "connected",
                    },
                ],
            };
        });
        const rpc = createServerRpc({
            sendRequest,
        } as unknown as MessageConnection);

        expect(await rpc.connectors.getCapabilities()).toMatchObject({
            apiVersion: 1,
            availability: "enabled",
        });
        expect(await rpc.connectors.getAccounts()).toEqual({
            availability: "enabled",
            accounts: [
                {
                    accountId: "account-1",
                    authInfo: {
                        type: "user",
                        host: "https://github.com",
                        login: "octocat",
                    },
                },
            ],
        });
        expect(await rpc.connectors.list({ accountId: "account-1" })).toMatchObject({
            revision: 1,
        });
        expect(await rpc.connectors.refresh({ accountId: "account-1" })).toMatchObject({
            revision: 1,
        });

        expect(sendRequest.mock.calls).toEqual([
            ["connectors.getCapabilities", {}],
            ["connectors.getAccounts", {}],
            ["connectors.list", { accountId: "account-1" }],
            ["connectors.refresh", { accountId: "account-1" }],
        ]);
    });
});

describe("session Connector RPC", () => {
    it("exposes the host-owned experimental Connector lifecycle", async () => {
        const sendRequest = vi.fn(async (method: string) => {
            if (method === "session.connectors.getCapabilities") {
                return {
                    apiVersion: 1,
                    availability: "enabled",
                    consentContinuation: true,
                    opaqueAccountSelection: true,
                    maxPollAttempts: 30,
                    maxPollIntervalMs: 2_000,
                    maxDeadlineMs: 60_000,
                };
            }
            if (method === "session.connectors.list" || method === "session.connectors.refresh") {
                return { revision: 1, refreshedAtMs: 1, connectors: [] };
            }
            if (method === "session.connectors.connect") {
                return {
                    kind: "consent_required",
                    consentUrl: "https://example.com/consent",
                    continuationId: "continuation-1",
                };
            }
            if (
                method === "session.connectors.reconnect" ||
                method === "session.connectors.continueConnection"
            ) {
                return { kind: "pending", continuationId: "continuation-1" };
            }
            if (method === "session.connectors.disconnect") {
                return { disconnected: true, status: connectorStatus() };
            }
            return connectorStatus();
        });
        const session = new CopilotSession("session-1", {
            sendRequest,
        } as unknown as MessageConnection);

        expect(await session.rpc.connectors.getCapabilities()).toMatchObject({
            apiVersion: 1,
            availability: "enabled",
        });
        expect(await session.rpc.connectors.getStatus()).toEqual(connectorStatus());
        expect(await session.rpc.connectors.list({ accountId: "account-1" })).toMatchObject({
            revision: 1,
        });
        expect(await session.rpc.connectors.refresh({ accountId: "account-1" })).toMatchObject({
            revision: 1,
        });
        expect(
            await session.rpc.connectors.connect({
                accountId: "account-1",
                connectorName: "calendar",
            })
        ).toMatchObject({ kind: "consent_required" });
        expect(
            await session.rpc.connectors.reconnect({
                accountId: "account-1",
                connectorName: "calendar",
            })
        ).toMatchObject({ kind: "pending" });
        expect(
            await session.rpc.connectors.continueConnection({
                continuationId: "continuation-1",
                maxAttempts: 3,
                pollIntervalMs: 100,
                deadlineMs: 1_000,
            })
        ).toMatchObject({ kind: "pending" });
        expect(
            await session.rpc.connectors.disconnect({
                accountId: "account-1",
                connectorName: "calendar",
            })
        ).toMatchObject({ disconnected: true });
        expect(
            await session.rpc.connectors.reconcile({
                accountId: "account-1",
                refreshCatalog: true,
            })
        ).toEqual(connectorStatus());

        expect(sendRequest.mock.calls).toEqual([
            ["session.connectors.getCapabilities", { sessionId: "session-1" }],
            ["session.connectors.getStatus", { sessionId: "session-1" }],
            ["session.connectors.list", { sessionId: "session-1", accountId: "account-1" }],
            ["session.connectors.refresh", { sessionId: "session-1", accountId: "account-1" }],
            [
                "session.connectors.connect",
                {
                    sessionId: "session-1",
                    accountId: "account-1",
                    connectorName: "calendar",
                },
            ],
            [
                "session.connectors.reconnect",
                {
                    sessionId: "session-1",
                    accountId: "account-1",
                    connectorName: "calendar",
                },
            ],
            [
                "session.connectors.continueConnection",
                {
                    sessionId: "session-1",
                    continuationId: "continuation-1",
                    maxAttempts: 3,
                    pollIntervalMs: 100,
                    deadlineMs: 1_000,
                },
            ],
            [
                "session.connectors.disconnect",
                {
                    sessionId: "session-1",
                    accountId: "account-1",
                    connectorName: "calendar",
                },
            ],
            [
                "session.connectors.reconcile",
                {
                    sessionId: "session-1",
                    accountId: "account-1",
                    refreshCatalog: true,
                },
            ],
        ]);
    });

    it("preserves unknown continuation outcomes without treating them as connected", async () => {
        const futureResult = {
            kind: "future_outcome",
            continuationId: "continuation-1",
            status: connectorStatus(),
        };
        const sendRequest = vi.fn().mockResolvedValue(futureResult);
        const session = new CopilotSession("session-1", {
            sendRequest,
        } as unknown as MessageConnection);

        const result = await session.rpc.connectors.continueConnection({
            continuationId: "continuation-1",
            maxAttempts: 1,
            pollIntervalMs: 0,
            deadlineMs: 1_000,
        });

        expect(result).toEqual(futureResult);
        expect(result.kind).not.toBe("connected");
    });

    it("returns the session account or null and sends an exact targeted reconcile request", async () => {
        const authInfo: AuthIdentityMetadata = {
            type: "token",
            host: "github.com",
            login: "alice",
        };
        const account: ConnectorSessionAccount = { accountId: "session-account-1", authInfo };
        const sendRequest = vi
            .fn()
            .mockResolvedValueOnce({
                ...connectorCapabilities(),
                sessionAccountSelection: true,
                targetedReconcile: true,
            })
            .mockResolvedValueOnce(account)
            .mockResolvedValueOnce(null)
            .mockResolvedValueOnce(connectorStatus());
        const session = new CopilotSession("session-1", {
            sendRequest,
        } as unknown as MessageConnection);

        const capabilities = await session.rpc.connectors.getCapabilities();
        expect(capabilities.availability).toBe("enabled");
        expect(capabilities.sessionAccountSelection).toBe(true);
        expect(capabilities.targetedReconcile).toBe(true);

        const selected: ConnectorSessionAccountResult = await session.rpc.connectors.getAccount();
        expect(selected).toEqual(account);
        expect(Object.keys(selected!.authInfo).sort()).toEqual(["host", "login", "type"]);
        const unavailable: ConnectorSessionAccountResult =
            await session.rpc.connectors.getAccount();
        expect(unavailable).toBeNull();
        expect(
            await session.rpc.connectors.reconcile({
                accountId: account.accountId,
                refreshCatalog: true,
                forceConnectorName: "calendar",
            })
        ).toEqual(connectorStatus());

        expect(sendRequest.mock.calls).toEqual([
            ["session.connectors.getCapabilities", { sessionId: "session-1" }],
            ["session.connectors.getAccount", { sessionId: "session-1" }],
            ["session.connectors.getAccount", { sessionId: "session-1" }],
            [
                "session.connectors.reconcile",
                {
                    sessionId: "session-1",
                    accountId: "session-account-1",
                    refreshCatalog: true,
                    forceConnectorName: "calendar",
                },
            ],
        ]);
    });

    it.each([undefined, false])(
        "lets hosts avoid unsupported session-account calls when capabilities are %s",
        async (supported) => {
            const sendRequest = vi.fn().mockResolvedValue({
                ...connectorCapabilities(),
                ...(supported === undefined
                    ? {}
                    : { sessionAccountSelection: supported, targetedReconcile: supported }),
            });
            const session = new CopilotSession("session-1", {
                sendRequest,
            } as unknown as MessageConnection);

            const capabilities = await session.rpc.connectors.getCapabilities();
            if (
                capabilities.availability === "enabled" &&
                capabilities.sessionAccountSelection === true
            ) {
                await session.rpc.connectors.getAccount();
            }
            if (
                capabilities.availability === "enabled" &&
                capabilities.targetedReconcile === true
            ) {
                await session.rpc.connectors.reconcile({
                    accountId: "account-1",
                    forceConnectorName: "calendar",
                });
            }

            expect(sendRequest.mock.calls).toEqual([
                ["session.connectors.getCapabilities", { sessionId: "session-1" }],
            ]);
        }
    );

    it("preserves optional catalog presentation metadata without requiring it", async () => {
        const entry = {
            name: "calendar",
            displayName: "Calendar",
            status: "connected",
            runtimeServerIds: ["connector-calendar"],
        };
        const decorated = {
            ...entry,
            logo: "https://example.com/calendar.svg",
            tier: "standard",
            releaseTag: "preview",
        };
        const sendRequest = vi
            .fn()
            .mockResolvedValueOnce({ connectors: [decorated], revision: 1, refreshedAtMs: 1 })
            .mockResolvedValueOnce({ connectors: [entry], revision: 2, refreshedAtMs: 2 });
        const session = new CopilotSession("session-1", {
            sendRequest,
        } as unknown as MessageConnection);

        const catalog = await session.rpc.connectors.list({ accountId: "account-1" });
        expect(catalog.connectors[0]).toEqual(decorated);
        const legacy = await session.rpc.connectors.refresh({ accountId: "account-1" });
        expect(legacy.connectors[0]).toEqual(entry);
    });
});

function connectorCapabilities() {
    return {
        apiVersion: 1,
        availability: "enabled",
        consentContinuation: true,
        opaqueAccountSelection: true,
        maxPollAttempts: 30,
        maxPollIntervalMs: 2_000,
        maxDeadlineMs: 60_000,
    };
}

function connectorStatus() {
    return {
        apiVersion: 1,
        availability: "enabled" as const,
        runtimeServers: [],
        pendingConnections: 0,
    };
}

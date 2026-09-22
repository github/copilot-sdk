import { describe, expect, it, vi } from "vitest";
import type { MessageConnection } from "vscode-jsonrpc";

import { CopilotSession } from "../src/session.js";

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
});

function connectorStatus() {
    return {
        apiVersion: 1,
        availability: "enabled" as const,
        runtimeServers: [],
        pendingConnections: 0,
    };
}

/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import type { MessageConnection } from "vscode-jsonrpc/node.js";
import { describe, expect, it, vi } from "vitest";
import {
    createServerRpc,
    type CatalogClientContract,
    type CatalogSearchRequest,
    type McpPlanInstallRequest,
    type McpPlanInstallSource,
} from "../src/generated/rpc.js";

// Requests that declare x-legacy-parameters keep one object argument in TypeScript:
// later inputs are optional properties, so existing calls are unchanged.
const contract: CatalogClientContract = {
    protocolVersion: 3,
    requiredCapabilities: ["mcp-install-planning"],
};
const source: McpPlanInstallSource = {
    kind: "candidate",
    candidateHandle: "candidate",
    searchId: "search",
};

function recordingRpc() {
    const sendRequest = vi.fn(async () => ({ kind: "unavailable", message: "offline" }));
    const rpc = createServerRpc({ sendRequest } as unknown as MessageConnection);
    return { rpc, sendRequest };
}

describe("x-legacy-parameters in TypeScript", () => {
    it("keeps existing object calls unchanged", async () => {
        const { rpc, sendRequest } = recordingRpc();
        const plan: McpPlanInstallRequest = { contract, source, scope: "user" };
        const search: CatalogSearchRequest = { contract, query: "catalogue query", limit: 4 };

        await rpc.mcp.planInstall(plan);
        await rpc.catalog.search(search);

        expect(sendRequest.mock.calls).toEqual([
            ["mcp.planInstall", { contract, source, scope: "user" }],
            ["catalog.search", { contract, query: "catalogue query", limit: 4 }],
        ]);
    });

    it("sends added optional inputs through the same method", async () => {
        const { rpc, sendRequest } = recordingRpc();

        await rpc.mcp.planInstall({ contract, source, policySessionId: "session" });
        await rpc.catalog.search({
            contract,
            query: "catalogue query",
            policySessionId: "session",
        });

        expect(sendRequest.mock.calls).toEqual([
            ["mcp.planInstall", { contract, source, policySessionId: "session" }],
            ["catalog.search", { contract, query: "catalogue query", policySessionId: "session" }],
        ]);
    });
});

import { describe, expect, it, vi } from "vitest";
import type { MessageConnection } from "vscode-jsonrpc/node.js";
import {
    createServerRpc,
    type CatalogSearchRequest,
    type CatalogSearchResult,
} from "../src/generated/rpc.js";

describe("catalog search generated types", () => {
    it("preserves candidate variants, handles, refusals, and inert embedded sources", async () => {
        const success = {
            kind: "succeeded",
            searchId: "search-1",
            candidates: [
                {
                    kind: "mcp-server",
                    handle: "mcp-handle",
                    handleExpiresAt: "2026-09-02T12:00:00Z",
                    mediaType: "application/mcp-server-card+json",
                    installability: "installable",
                    displayName: "Example MCP",
                    source: { kind: "url", url: "https://example.com/mcp.json" },
                    provenance: {
                        authority: "example.com",
                        observedAt: "2026-09-02T11:00:00Z",
                        mediaType: "application/mcp-server-card+json",
                    },
                },
                {
                    kind: "ai-skill",
                    handle: "skill-handle",
                    handleExpiresAt: "2026-09-02T12:00:00Z",
                    mediaType: "application/ai-skill",
                    installability: "not-installable-kind",
                    displayName: "Example skill",
                    source: { kind: "embedded" },
                    provenance: {
                        authority: "example.com",
                        observedAt: "2026-09-02T11:00:00Z",
                        mediaType: "application/ai-skill",
                    },
                },
            ],
            truncated: false,
            negotiated: {
                runtimeProtocolVersion: 1,
                grantedCapabilities: ["mcp-server-card", "ai-skill-discovery"],
            },
        } satisfies CatalogSearchResult;
        const refusal = {
            kind: "unsupported-kind",
            message: "AI skills are unavailable",
            requestedKinds: ["ai-skill"],
            supportedKinds: ["mcp-server"],
        } satisfies CatalogSearchResult;
        const sendRequest = vi.fn().mockResolvedValueOnce(success).mockResolvedValueOnce(refusal);
        const connection = { sendRequest } as unknown as MessageConnection;
        const request = {
            contract: { protocolVersion: 1, requiredCapabilities: ["mcp-server-card"] },
            query: "example",
        } satisfies CatalogSearchRequest;

        const result = await createServerRpc(connection).catalog.search(request);
        expect(result.kind).toBe("succeeded");
        if (result.kind !== "succeeded") throw new Error("expected successful catalogue search");
        expect(result.candidates.map((candidate) => candidate.kind)).toEqual([
            "mcp-server",
            "ai-skill",
        ]);
        expect(result.candidates.map((candidate) => candidate.handle)).toEqual([
            "mcp-handle",
            "skill-handle",
        ]);
        expect(result.candidates[1].source).toEqual({ kind: "embedded" });

        const rejected = await createServerRpc(connection).catalog.search(request);
        expect(rejected.kind).toBe("unsupported-kind");
    });
});

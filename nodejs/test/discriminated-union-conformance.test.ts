import { PassThrough } from "node:stream";

import {
    createMessageConnection,
    StreamMessageReader,
    StreamMessageWriter,
} from "vscode-jsonrpc/node.js";
import { describe, expect, it, onTestFinished } from "vitest";

import {
    type CatalogSearchRequest,
    type CatalogSearchResult,
    createServerRpc,
} from "../src/generated/rpc.js";

const OPAQUE_MCP_HANDLE = "opaque:mcp/01-do-not-parse";
const OPAQUE_SKILL_HANDLE = "opaque:skill/02-do-not-parse";

function successResult(): CatalogSearchResult {
    return {
        kind: "succeeded",
        searchId: "search-01",
        candidates: [
            {
                kind: "mcp-server",
                handle: OPAQUE_MCP_HANDLE,
                handleExpiresAt: "2026-09-02T12:00:00Z",
                mediaType: "application/mcp-server-card+json",
                installability: "installable",
                displayName: "Example MCP",
                source: { kind: "url", url: "https://catalog.example/mcp.json" },
                provenance: {
                    authority: "catalog.example",
                    observedAt: "2026-09-02T11:00:00Z",
                    mediaType: "application/mcp-server-card+json",
                },
            },
            {
                kind: "ai-skill",
                handle: OPAQUE_SKILL_HANDLE,
                handleExpiresAt: "2026-09-02T12:00:00Z",
                mediaType: "application/ai-skill",
                installability: "not-installable-kind",
                displayName: "Example skill",
                source: { kind: "embedded" },
                provenance: {
                    authority: "catalog.example",
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
    };
}

function successWireResult(): unknown {
    const result = successResult();
    if (result.kind !== "succeeded") throw new Error("Expected a successful search.");
    return {
        ...result,
        rawCard: { secret: "must-not-survive" },
        candidates: result.candidates.map((candidate) => ({
            ...candidate,
            card: { secret: "must-not-survive" },
            cardData: { secret: "must-not-survive" },
            rawCard: { secret: "must-not-survive" },
            source: {
                ...candidate.source,
                rawCard: { secret: "must-not-survive" },
            },
        })),
    };
}

function invalidWireResult(kind: string): unknown {
    const result = structuredClone(successWireResult()) as {
        kind?: string;
        candidates: Array<{ kind?: string; source: { kind?: string } }>;
    };
    switch (kind) {
        case "unknown-result":
            result.kind = "future-result";
            return result;
        case "missing-result":
            delete result.kind;
            return result;
        case "unknown-candidate":
            result.candidates[0].kind = "future-candidate";
            return result;
        case "missing-candidate":
            delete result.candidates[0].kind;
            return result;
        case "unknown-source":
            result.candidates[0].source.kind = "future-source";
            return result;
        case "missing-source":
            delete result.candidates[0].source.kind;
            return result;
        default:
            throw new Error(`Unknown invalid result fixture: ${kind}`);
    }
}

describe("closed discriminated union conformance", () => {
    it("transports typed candidates, refusals, and failures unchanged", async () => {
        const clientToServer = new PassThrough();
        const serverToClient = new PassThrough();
        const client = createMessageConnection(
            new StreamMessageReader(serverToClient),
            new StreamMessageWriter(clientToServer)
        );
        const server = createMessageConnection(
            new StreamMessageReader(clientToServer),
            new StreamMessageWriter(serverToClient)
        );
        onTestFinished(() => {
            client.dispose();
            server.dispose();
        });

        server.onRequest("catalog.search", (params: CatalogSearchRequest) => {
            if (params.query === "authentication") {
                return {
                    kind: "authentication-required",
                    reason: "no-credential",
                    message: "Sign in is required.",
                } satisfies CatalogSearchResult;
            }
            if (params.query === "network") {
                return {
                    kind: "network-failure",
                    reason: "timeout",
                    retryAfterSeconds: 30,
                    message: "The catalogue timed out.",
                } satisfies CatalogSearchResult;
            }
            if (params.query.startsWith("invalid:")) {
                return invalidWireResult(params.query.slice("invalid:".length));
            }
            return successWireResult();
        });
        client.listen();
        server.listen();

        const rpc = createServerRpc(client);
        const request = {
            contract: { protocolVersion: 1, requiredCapabilities: [] },
            query: "success",
        };
        const success = await rpc.catalog.search(request);
        expect(success.kind).toBe("succeeded");
        if (success.kind !== "succeeded") throw new Error("Expected a successful search.");

        expect(success.candidates.map((candidate) => candidate.kind)).toEqual([
            "mcp-server",
            "ai-skill",
        ]);
        expect(success.candidates.map((candidate) => candidate.handle)).toEqual([
            OPAQUE_MCP_HANDLE,
            OPAQUE_SKILL_HANDLE,
        ]);
        const encodedCandidates = JSON.parse(JSON.stringify(success)).candidates as Array<
            Record<string, unknown>
        >;
        expect(success).not.toHaveProperty("rawCard");
        for (const candidate of encodedCandidates) {
            expect(candidate).not.toHaveProperty("card");
            expect(candidate).not.toHaveProperty("cardData");
            expect(candidate).not.toHaveProperty("rawCard");
            expect(candidate.source).not.toHaveProperty("rawCard");
        }

        await expect(
            rpc.catalog.search({ ...request, query: "authentication" })
        ).resolves.toMatchObject({
            kind: "authentication-required",
            reason: "no-credential",
        });
        await expect(rpc.catalog.search({ ...request, query: "network" })).resolves.toMatchObject({
            kind: "network-failure",
            reason: "timeout",
            retryAfterSeconds: 30,
        });

        for (const invalid of [
            "unknown-result",
            "missing-result",
            "unknown-candidate",
            "missing-candidate",
            "unknown-source",
            "missing-source",
        ]) {
            await expect(
                rpc.catalog.search({ ...request, query: `invalid:${invalid}` })
            ).rejects.toThrow(/unknown or missing kind discriminator/);
        }
    });
});

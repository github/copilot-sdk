import { describe, expect, it } from "vitest";

import { renderRpcTypes, renderRpcWrappers } from "../../java/scripts/codegen/java.ts";

function rpcSource(files: Map<string, string>, name: string): string {
    const source = files.get(
        `sdk/src/generated/java/com/github/copilot/generated/rpc/${name}.java`
    );
    expect(source).toBeDefined();
    return source!;
}

describe("java RPC codegen visibility", () => {
    const schema = {
        server: {
            publicOnly: {
                get: {
                    rpcMethod: "publicOnly.get",
                    params: { $ref: "#/definitions/PublicOnlyRequest" },
                    result: { $ref: "#/definitions/PublicOnlyResult" },
                },
            },
            mixed: {
                visible: {
                    rpcMethod: "mixed.visible",
                    params: null,
                    result: { $ref: "#/definitions/SharedResult" },
                },
                hidden: {
                    rpcMethod: "mixed.hidden",
                    visibility: "internal",
                    params: { $ref: "#/definitions/InternalOnlyRequest" },
                    result: { $ref: "#/definitions/InternalOnlyResult" },
                },
            },
            internalOnly: {
                hidden: {
                    rpcMethod: "internalOnly.hidden",
                    visibility: "internal",
                    params: { $ref: "#/definitions/InternalOnlyRequest" },
                    result: { $ref: "#/definitions/InternalOnlyResult" },
                },
            },
        },
        definitions: {
            PublicOnlyRequest: {
                type: "object",
                properties: { value: { type: "string" } },
            },
            PublicOnlyResult: {
                type: "object",
                properties: { shared: { $ref: "#/definitions/SharedResult" } },
            },
            SharedResult: {
                type: "object",
                properties: { ok: { type: "boolean" } },
            },
            InternalOnlyRequest: {
                type: "object",
                properties: { secret: { type: "string" } },
            },
            InternalOnlyResult: {
                type: "object",
                properties: { secret: { type: "string" } },
            },
        },
    } satisfies Parameters<typeof renderRpcTypes>[0];

    it("keeps internal-only RPC types package-private", async () => {
        const files = await renderRpcTypes(schema, {});

        expect(rpcSource(files, "PublicOnlyGetParams")).toContain(
            "public record PublicOnlyGetParams("
        );
        expect(rpcSource(files, "SharedResult")).toContain("public record SharedResult(");
        expect(rpcSource(files, "MixedHiddenParams")).toContain("record MixedHiddenParams(");
        expect(rpcSource(files, "MixedHiddenParams")).not.toContain(
            "public record MixedHiddenParams("
        );
        expect(rpcSource(files, "InternalOnlyResult")).toContain("record InternalOnlyResult(");
        expect(rpcSource(files, "InternalOnlyResult")).not.toContain(
            "public record InternalOnlyResult("
        );
    });

    it("does not expose internal methods or namespaces through public Java members", async () => {
        const files = await renderRpcWrappers(schema);

        expect(rpcSource(files, "ServerRpc")).toContain(
            "public final ServerPublicOnlyApi publicOnly;"
        );
        expect(rpcSource(files, "ServerRpc")).toContain("public final ServerMixedApi mixed;");
        expect(rpcSource(files, "ServerRpc")).toContain(
            "final ServerInternalOnlyApi internalOnly;"
        );
        expect(rpcSource(files, "ServerRpc")).not.toContain(
            "public final ServerInternalOnlyApi internalOnly;"
        );

        expect(rpcSource(files, "ServerMixedApi")).toContain("public final class ServerMixedApi");
        expect(rpcSource(files, "ServerMixedApi")).toContain(
            "public CompletableFuture<MixedVisibleResult> visible()"
        );
        expect(rpcSource(files, "ServerMixedApi")).toContain(
            "CompletableFuture<InternalOnlyResult> hidden(MixedHiddenParams params)"
        );
        expect(rpcSource(files, "ServerMixedApi")).not.toContain(
            "public CompletableFuture<InternalOnlyResult> hidden"
        );

        expect(rpcSource(files, "ServerInternalOnlyApi")).toContain(
            "final class ServerInternalOnlyApi"
        );
        expect(rpcSource(files, "ServerInternalOnlyApi")).not.toContain(
            "public final class ServerInternalOnlyApi"
        );
    });

    it("reuses a shared result definition for internal methods", async () => {
        const sharedResultSchema = {
            server: {
                globalState: {
                    load: {
                        rpcMethod: "globalState.load",
                        visibility: "internal",
                        params: null,
                        result: { $ref: "#/definitions/GlobalStateLoadResult" },
                    },
                    loadForConfigDir: {
                        rpcMethod: "globalState.loadForConfigDir",
                        visibility: "internal",
                        params: null,
                        result: { $ref: "#/definitions/GlobalStateLoadResult" },
                    },
                },
            },
            definitions: {
                GlobalStateLoadResult: {
                    type: "object",
                    properties: { staff: { type: "boolean" } },
                },
            },
        } satisfies Parameters<typeof renderRpcTypes>[0];
        const types = await renderRpcTypes(sharedResultSchema, {});
        const wrappers = await renderRpcWrappers(sharedResultSchema);

        expect(rpcSource(wrappers, "ServerGlobalStateApi")).toContain(
            "CompletableFuture<GlobalStateLoadResult> loadForConfigDir()"
        );
        expect(rpcSource(wrappers, "ServerGlobalStateApi")).toContain(
            "CompletableFuture<GlobalStateLoadResult> load()"
        );
        expect(rpcSource(types, "GlobalStateLoadResult")).not.toContain(
            "public record GlobalStateLoadResult("
        );
        expect(
            types.has(
                "sdk/src/generated/java/com/github/copilot/generated/rpc/GlobalStateLoadForConfigDirResult.java"
            )
        ).toBe(false);
    });
});

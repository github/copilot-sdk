import assert from "node:assert/strict";
import { test } from "node:test";
import { generateRpcCode } from "../csharp.js";

test("omits internal in-process-only properties from generated RPC classes", () => {
    const schema = (visibility?: string) => ({
        session: {
            getApi: {
                rpcMethod: "session.getApi",
                params: null,
                result: {
                    type: "object" as const,
                    title: "SessionApi",
                    properties: {
                        handle: { visibility, "x-opaque-in-process": true },
                    },
                },
            },
        },
    });
    const code = generateRpcCode(schema("internal"));

    assert.doesNotMatch(code, /"handle"/);
    assert.throws(() => generateRpcCode(schema()), /x-opaque-in-process properties must have visibility "internal"/);
});

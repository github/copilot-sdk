/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import assert from "node:assert/strict";
import { test } from "node:test";
import type { JSONSchema7 } from "json-schema";
import { generateRpcCode } from "./csharp.js";

for (const keyword of ["anyOf", "oneOf"] as const) {
    test(`C# RPC preserves named single-variant ${keyword} objects`, () => {
        const responseFormat: JSONSchema7 = {
            title: "ResponseFormat",
            description: "A provider-native output format.",
            [keyword]: [{
                type: "object",
                properties: {
                    type: { type: "string", const: "json_schema" },
                    jsonSchema: { $ref: "#/definitions/JsonSchemaResponseFormat" },
                },
                required: ["type", "jsonSchema"],
            }],
        };
        const code = generateRpcCode({
            session: {
                send: {
                    rpcMethod: "session.send",
                    params: {
                        type: "object",
                        title: "SendRequest",
                        properties: {
                            responseFormat: { $ref: "#/definitions/ResponseFormat" },
                            requiredFormat: { $ref: "#/definitions/ResponseFormat" },
                        },
                        required: ["requiredFormat"],
                    },
                },
            },
            definitions: {
                ResponseFormat: responseFormat,
                JsonSchemaResponseFormat: {
                    type: "object",
                    properties: {
                        name: { type: "string" },
                        schema: { "x-opaque-json": true } as JSONSchema7,
                        strict: { type: "boolean" },
                    },
                    required: ["name", "schema"],
                },
            },
        });

        assert.match(code, /public sealed class ResponseFormat\b/);
        assert.match(code, /A provider-native output format\./);
        assert.match(code, /public ResponseFormat\? ResponseFormat/);
        assert.match(code, /public ResponseFormat RequiredFormat/);
        assert.match(code, /public JsonSchemaResponseFormat JsonSchema/);
        assert.match(code, /public JsonElement Schema/);
        assert.match(code, /public bool\? Strict/);
        assert.equal(code.match(/public sealed class ResponseFormat\b/g)?.length, 1);
    });
}

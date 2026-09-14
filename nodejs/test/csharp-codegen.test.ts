import type { JSONSchema7 } from "json-schema";
import { describe, expect, it } from "vitest";

import { generateRpcCode } from "../../scripts/codegen/csharp.ts";

describe("C# RPC codegen", () => {
    it.each(["anyOf", "oneOf"] as const)("preserves named single-variant %s objects", (keyword) => {
        const responseFormat: JSONSchema7 = {
            title: "ResponseFormat",
            description: "A provider-native output format.",
            [keyword]: [
                {
                    type: "object",
                    properties: {
                        type: { type: "string", const: "json_schema" },
                        jsonSchema: { $ref: "#/definitions/JsonSchemaResponseFormat" },
                    },
                    required: ["type", "jsonSchema"],
                },
            ],
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

        expect(code).toContain("public sealed class ResponseFormat");
        expect(code).toContain("A provider-native output format.");
        expect(code).toContain("public ResponseFormat? ResponseFormat");
        expect(code).toContain("public ResponseFormat RequiredFormat");
        expect(code).toContain("public JsonSchemaResponseFormat JsonSchema");
        expect(code).toContain("public JsonElement Schema");
        expect(code).toContain("public bool? Strict");
        expect(code.match(/public sealed class ResponseFormat\b/g)).toHaveLength(1);
    });

    it.each(["anyOf", "oneOf"] as const)(
        "preserves referenced enum discriminators and nested %s unions",
        (keyword) => {
            const code = generateRpcCode({
                session: {
                    configure: {
                        rpcMethod: "session.configure",
                        stability: "experimental",
                        params: {
                            type: "object",
                            properties: {
                                systemMessage: { $ref: "#/definitions/SystemMessage" },
                                requiredMessage: { $ref: "#/definitions/SystemMessage" },
                            },
                            required: ["requiredMessage"],
                        },
                    },
                },
                definitions: {
                    SystemMessage: {
                        description: "System message configuration.",
                        [keyword]: [
                            { $ref: "#/definitions/AppendConfig" },
                            { $ref: "#/definitions/ReplaceConfig" },
                            { $ref: "#/definitions/CustomizeConfig" },
                        ],
                    },
                    AppendMode: { type: "string", enum: ["append"] },
                    ReplaceMode: { type: "string", enum: ["replace"] },
                    CustomizeMode: { type: "string", enum: ["customize"] },
                    AppendConfig: {
                        type: "object",
                        properties: {
                            mode: { $ref: "#/definitions/AppendMode" },
                            content: { type: "string" },
                        },
                    },
                    ReplaceConfig: {
                        type: "object",
                        properties: {
                            mode: { $ref: "#/definitions/ReplaceMode" },
                            content: { type: "string" },
                        },
                        required: ["mode", "content"],
                    },
                    CustomizeConfig: {
                        type: "object",
                        properties: {
                            mode: { $ref: "#/definitions/CustomizeMode" },
                            content: { type: "string" },
                            sections: {
                                type: "object",
                                additionalProperties: {
                                    $ref: "#/definitions/SectionOverride",
                                },
                            },
                        },
                        required: ["mode"],
                    },
                    SectionOverride: {
                        [keyword]: [
                            { $ref: "#/definitions/StaticSectionOverride" },
                            { $ref: "#/definitions/MarkerSectionOverride" },
                        ],
                    },
                    StaticSectionAction: {
                        type: "string",
                        enum: ["replace", "remove", "append", "prepend"],
                    },
                    StaticSectionOverride: {
                        type: "object",
                        properties: {
                            action: { $ref: "#/definitions/StaticSectionAction" },
                            content: { type: "string" },
                        },
                        required: ["action"],
                    },
                    MarkerSectionOverride: {
                        [keyword]: ["transform", "preserve"].map((action) => ({
                            type: "object",
                            properties: { action: { type: "string", const: action } },
                            required: ["action"],
                        })),
                    },
                },
            });

            expect(code).toContain("public sealed partial class SystemMessage");
            expect(code).toContain(
                "[Experimental(Diagnostics.Experimental)]\n[JsonConverter(typeof(Converter))]\npublic sealed partial class SystemMessage"
            );
            expect(code).toContain("System message configuration.");
            expect(code).toContain("public SystemMessage? SystemMessage");
            expect(code).toContain("public SystemMessage RequiredMessage");
            expect(code.match(/public sealed partial class SystemMessage\b/g)).toHaveLength(1);
            expect(code).toContain("public AppendConfig? AppendConfig { get; }");
            expect(code).toContain("public ReplaceConfig? ReplaceConfig { get; }");
            expect(code).toContain("public CustomizeConfig? CustomizeConfig { get; }");
            expect(code).toContain("public AppendMode? Mode");
            expect(code).toContain("public ReplaceMode Mode");
            expect(code).toContain("public IDictionary<string, SectionOverride>? Sections");
            expect(code).toContain("public StaticSectionOverride? StaticSectionOverride { get; }");
            expect(code).toContain("public MarkerSectionOverride? MarkerSectionOverride { get; }");
            expect(code).toContain("public StaticSectionAction Action");
            expect(code).toContain(
                '!element.TryGetProperty("mode", out _) || (element.TryGetProperty("mode", out _)'
            );
            for (const mode of ["append", "replace", "customize"]) {
                expect(code).toContain(`element.GetProperty("mode").GetString() == "${mode}"`);
            }
            for (const action of [
                "replace",
                "remove",
                "append",
                "prepend",
                "transform",
                "preserve",
            ]) {
                expect(code).toContain(`element.GetProperty("action").GetString() == "${action}"`);
            }
            expect(code).toContain('element.GetProperty("mode").ValueKind == JsonValueKind.String');
            expect(code).not.toContain("catch (JsonException)");
            for (const type of [
                "SystemMessage",
                "AppendConfig",
                "ReplaceConfig",
                "CustomizeConfig",
                "SectionOverride",
                "StaticSectionOverride",
                "MarkerSectionOverride",
            ]) {
                expect(code).toContain(`[JsonSerializable(typeof(${type}))]`);
            }
            expect(code).toContain(
                "JsonSerializer.Deserialize(element, RpcJsonContext.Default.AppendConfig)"
            );
            expect(code).toContain(
                "JsonSerializer.Serialize(writer, appendConfig, RpcJsonContext.Default.AppendConfig)"
            );
        }
    );
});

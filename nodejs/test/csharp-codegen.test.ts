import type { JSONSchema7 } from "json-schema";
import { describe, expect, it } from "vitest";

import { generateRpcCode, generateSessionEventsCode } from "../../scripts/codegen/csharp.ts";
import type { ApiSchema, RpcMethod } from "../../scripts/codegen/utils.ts";
import { legacyRequestSchema } from "./legacy-parameters-fixture.ts";

function legacySignatureFixture(scope: "server" | "session", legacy: unknown) {
    const params: JSONSchema7 & { "x-legacy-parameters"?: unknown } = {
        title: "SampleRequest",
        type: "object",
        properties: {
            ...(scope === "session" ? { sessionId: { type: "string" as const } } : {}),
            name: { type: "string" },
            config: { "x-opaque-json": true } as JSONSchema7,
            expectedIdentity: { type: "string" },
        },
        required: scope === "session" ? ["sessionId", "name"] : ["name"],
        additionalProperties: false,
    };
    if (legacy !== undefined) params["x-legacy-parameters"] = legacy;
    const methods: Record<string, RpcMethod> = {
        run: {
            rpcMethod: `${scope === "session" ? "session." : ""}sample.run`,
            params,
            result: null,
        },
    };
    const schema: ApiSchema = { [scope]: { sample: methods } };
    return { schema, params, methods };
}

describe("C# additive request entry compatibility", () => {
    it.each(["server", "session"] as const)(
        "preserves the exact legacy %s signature and adds a same-name request overload",
        (scope) => {
            const { schema } = legacySignatureFixture(scope, ["name", "config"]);
            const code = generateRpcCode(schema);
            expect(code).toContain(
                "RunAsync(string name, object? config = null, CancellationToken cancellationToken = default)"
            );
            expect(code).toContain(
                "RunAsync(SampleRequest request, CancellationToken cancellationToken = default)"
            );
            expect(code).not.toContain("WithRequestAsync");
            expect(code).toContain("public sealed class SampleRequest\n");
            expect(code).toContain("public required string Name { get; set; }");
            expect(code).toContain("ArgumentNullException.ThrowIfNull(request);");
            expect(code).toContain("ArgumentNullException.ThrowIfNull(request.Name);");
            expect(code).toContain("Config = CopilotClient.ToJsonElementForWire(config)");
            expect(code).not.toContain("ExpectedIdentity = expectedIdentity");
            if (scope === "session") {
                expect(code).toContain("internal sealed class SampleRequestWithSession\n");
                expect(code).toContain(
                    "new SampleRequestWithSession { SessionId = _session.SessionId, Name = request.Name, Config = request.Config, ExpectedIdentity = request.ExpectedIdentity }"
                );
                const publicRequest = code.match(
                    /public sealed class SampleRequest\n\{[\s\S]*?\n\}/
                )?.[0];
                expect(publicRequest).toBeDefined();
                expect(publicRequest).not.toContain("SessionId");
                expect(code).toContain("[JsonSerializable(typeof(SampleRequestWithSession))]");
            } else {
                expect(code).toContain('(_rpc, "sample.run", [request], cancellationToken)');
            }
        }
    );

    it.each(["server", "session"] as const)(
        "leaves unmarked %s method output unchanged",
        (scope) => {
            const { schema, methods, params } = legacySignatureFixture(scope, undefined);
            methods.control = {
                ...methods.run,
                rpcMethod: methods.run.rpcMethod.replace(/run$/, "control"),
                params: { ...params, title: "ControlRequest" },
            };
            const before = generateRpcCode(schema);
            params["x-legacy-parameters"] = ["name", "config"];
            const after = generateRpcCode(schema);
            const method = /    public async Task ControlAsync\([\s\S]*?\n    \}/;
            const request = /internal sealed class ControlRequest\n\{[\s\S]*?\n\}/;
            expect(before.match(method)?.[0]).toBeDefined();
            expect(after.match(method)?.[0]).toBe(before.match(method)?.[0]);
            expect(before.match(request)?.[0]).toBeDefined();
            expect(after.match(request)?.[0]).toBe(before.match(request)?.[0]);
            expect(before).not.toContain("WithRequestAsync");
            expect(after).not.toContain("ControlWithRequestAsync");
        }
    );

    it.each([
        ["not an array", "name"],
        ["non-string", ["name", 1]],
        ["duplicate", ["name", "name"]],
        ["unknown", ["name", "missing"]],
        ["session envelope", ["sessionId", "name"]],
        ["missing required", ["config"]],
        ["optional before required", ["config", "name"]],
        ["no new arguments", ["name", "config", "expectedIdentity"]],
    ])("rejects invalid legacy metadata: %s", (_reason, legacy) => {
        const { schema } = legacySignatureFixture("session", legacy);
        expect(() => generateRpcCode(schema)).toThrow("Invalid x-legacy-parameters");
    });

    it("rejects nullable request metadata rather than changing the existing request entry", () => {
        const { schema, methods, params } = legacySignatureFixture("session", ["name", "config"]);
        methods.run.params = { anyOf: [params, { type: "null" }] };
        expect(() => generateRpcCode(schema)).toThrow("nullable requests");
    });

    it("keeps both overloads unchanged across two successive optional additions", () => {
        const signatures = (additions: 1 | 2) => {
            const code = generateRpcCode(legacyRequestSchema(additions).schema);
            return {
                code,
                overloads: code.match(/public async Task<[A-Za-z]+> PlanAsync\([^)]*\)/g),
            };
        };
        const one = signatures(1);
        const two = signatures(2);
        expect(one.overloads).toEqual([
            "public async Task<SamplePlanResult> PlanAsync(string contract, string source, string? scope = null, CancellationToken cancellationToken = default)",
            "public async Task<SamplePlanResult> PlanAsync(SamplePlanRequest request, CancellationToken cancellationToken = default)",
        ]);
        expect(two.overloads).toEqual(one.overloads);
        expect(one.code).not.toContain("TraceId");
        expect(two.code).toContain("public string? TraceId { get; set; }");
        expect(two.code).toContain("public required string Contract { get; set; }");
    });
});

describe("C# root event payload unions", () => {
    it.each(["anyOf", "oneOf"] as const)("preserves referenced %s payload variants", (keyword) => {
        const code = generateSessionEventsCode({
            definitions: {
                Payload: {
                    [keyword]: ["status", "startup", "server_error", "incremental"].map((kind) => ({
                        type: "object",
                        properties: { kind: { const: kind }, value: { type: "string" } },
                        required: ["kind", "value"],
                    })),
                },
                SessionEvent: {
                    anyOf: [
                        {
                            type: "object",
                            properties: {
                                type: { const: "sample.union" },
                                data: { $ref: "#/definitions/Payload" },
                            },
                            required: ["type", "data"],
                        },
                        {
                            type: "object",
                            properties: {
                                type: { const: "sample.empty" },
                                data: { type: "object", properties: {} },
                            },
                            required: ["type", "data"],
                        },
                    ],
                },
            },
        });
        expect(code).toContain("public required SampleUnionData Data");
        expect(code).toContain('TypeDiscriminatorPropertyName = "kind"');
        for (const suffix of ["Status", "Startup", "ServerError", "Incremental"]) {
            expect(code).toContain(
                `public sealed partial class SampleUnionData${suffix} : SampleUnionData`
            );
            expect(code).toContain(`[JsonSerializable(typeof(SampleUnionData${suffix}))]`);
        }
        expect(code).toContain("public required string Value");
        expect(code).toContain("public sealed partial class SampleEmptyData");
        expect(code).not.toContain("public sealed partial class SampleUnionData { }");
    });
});

describe("C# RPC codegen", () => {
    it.each(["uninstall", "update"])(
        "separates the session wire envelope from the shared plugins %s request",
        (method) => {
            const title = `Plugins${method === "uninstall" ? "Uninstall" : "Update"}Request`;
            const params: JSONSchema7 = {
                title,
                type: "object",
                properties: {
                    name: { type: "string" },
                    directSourceId: { type: ["string", "null"] },
                    mode: { type: "string", enum: ["local", "global"] },
                },
                required: ["name"],
                additionalProperties: false,
            };
            const code = generateRpcCode({
                definitions: { [title]: params },
                server: {
                    plugins: {
                        [method]: {
                            rpcMethod: `plugins.${method}`,
                            params: { $ref: `#/definitions/${title}` },
                        },
                    },
                },
                session: {
                    plugins: {
                        [method]: {
                            rpcMethod: `session.plugins.${method}`,
                            params: {
                                ...params,
                                properties: {
                                    sessionId: { type: "string" },
                                    ...params.properties,
                                },
                                required: ["sessionId", "name"],
                            },
                        },
                    },
                },
            });

            expect(code).toContain(`internal sealed class ${title}\n`);
            expect(code).toContain(`internal sealed class ${title}WithSession\n`);
            expect(code).toContain(
                `new ${title}WithSession { SessionId = _session.SessionId, Name = name, DirectSourceId = directSourceId, Mode = mode }`
            );
            expect(code).toContain(
                `new ${title} { Name = name, DirectSourceId = directSourceId, Mode = mode }`
            );
            expect(code).toContain(
                `string name, string? directSourceId = null, ${title}Mode? mode = null, CancellationToken cancellationToken = default`
            );
            expect(code).not.toContain(`${title}WithSessionMode`);
            expect(code).toContain(`[JsonSerializable(typeof(${title}))]`);
            expect(code).toContain(`[JsonSerializable(typeof(${title}WithSession))]`);
        }
    );

    it("still rejects incompatible schemas sharing a request title", () => {
        expect(() =>
            generateRpcCode({
                server: {
                    first: {
                        rpcMethod: "first",
                        params: {
                            title: "SharedRequest",
                            type: "object",
                            properties: { name: { type: "string" } },
                        },
                    },
                    second: {
                        rpcMethod: "second",
                        params: {
                            title: "SharedRequest",
                            type: "object",
                            properties: { count: { type: "integer" } },
                        },
                    },
                },
            })
        ).toThrow('Conflicting RPC class name "SharedRequest"');
    });

    it("does not hide non-envelope differences in a shared session request", () => {
        const params: JSONSchema7 = {
            title: "SharedRequest",
            type: "object",
            properties: { name: { type: "string" } },
            required: ["name"],
        };
        expect(() =>
            generateRpcCode({
                definitions: { SharedRequest: params },
                server: {
                    configure: { rpcMethod: "configure", params },
                },
                session: {
                    configure: {
                        rpcMethod: "session.configure",
                        params: {
                            ...params,
                            properties: {
                                sessionId: { type: "string" },
                                name: { type: "integer" },
                            },
                            required: ["sessionId", "name"],
                        },
                    },
                },
            })
        ).toThrow('Conflicting RPC class name "SharedRequest"');
    });

    it("preserves nullable public requests and their separate session wire type", () => {
        const code = generateRpcCode({
            session: {
                configure: {
                    rpcMethod: "session.configure",
                    params: {
                        anyOf: [
                            { type: "null" },
                            {
                                title: "ConfigureRequest",
                                type: "object",
                                properties: {
                                    sessionId: { type: "string" },
                                    mode: { type: "string", enum: ["local", "global"] },
                                },
                                required: ["sessionId"],
                            },
                        ],
                    },
                },
            },
        });
        expect(code).toContain("public sealed class ConfigureRequest\n");
        expect(code).toContain("internal sealed class ConfigureRequestWithSession\n");
        expect(code).toContain("ConfigureAsync(ConfigureRequest? request = null,");
        expect(code).toContain(
            "new ConfigureRequestWithSession { SessionId = _session.SessionId, Mode = request?.Mode }"
        );
        expect(code).not.toContain("ConfigureRequestWithSessionMode");
    });

    it("preserves session-only wire type names used by handwritten SDK code", () => {
        const params: JSONSchema7 = {
            title: "ModelSwitchToRequest",
            type: "object",
            properties: { modelId: { type: "string" } },
            required: ["modelId"],
        };
        const code = generateRpcCode({
            definitions: { ModelSwitchToRequest: params },
            session: {
                model: {
                    switchTo: {
                        rpcMethod: "session.model.switchTo",
                        params: {
                            ...params,
                            properties: { sessionId: { type: "string" }, ...params.properties },
                            required: ["sessionId", "modelId"],
                        },
                    },
                },
            },
        });
        expect(code).toContain("internal sealed class ModelSwitchToRequest\n");
        expect(code).toContain(
            "new ModelSwitchToRequest { SessionId = _session.SessionId, ModelId = modelId }"
        );
        expect(code).not.toContain("ModelSwitchToRequestWithSession");
    });

    it.each([
        ["anyOf", false],
        ["anyOf", true],
        ["oneOf", false],
        ["oneOf", true],
    ] as const)(
        "preserves the %s hierarchy when adding a variant (nullable: %s)",
        (keyword, nullable) => {
            const jsonSchemaVariant: JSONSchema7 = {
                type: "object",
                properties: {
                    type: { type: "string", const: "json_schema" },
                    jsonSchema: { $ref: "#/definitions/JsonSchemaResponseFormat" },
                },
                required: ["type", "jsonSchema"],
            };
            const nullVariants: JSONSchema7[] = nullable ? [{ type: "null" }] : [];
            const responseFormat: JSONSchema7 = {
                title: "ResponseFormat",
                description: "A provider-native output format.",
                [keyword]: [jsonSchemaVariant, ...nullVariants],
            };
            const schema: ApiSchema = {
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
            };
            const code = generateRpcCode(schema);
            const futureCode = generateRpcCode({
                ...schema,
                definitions: {
                    ...schema.definitions,
                    ResponseFormat: {
                        ...responseFormat,
                        [keyword]: [
                            jsonSchemaVariant,
                            {
                                type: "object",
                                properties: { type: { type: "string", const: "text" } },
                                required: ["type"],
                            },
                            ...nullVariants,
                        ],
                    },
                },
            });

            for (const generated of [code, futureCode]) {
                expect(generated).toContain("public partial class ResponseFormat\n");
                expect(generated).toContain("A provider-native output format.");
                expect(generated).toContain(
                    '[JsonPolymorphic(\n    TypeDiscriminatorPropertyName = "type",'
                );
                expect(generated).toContain(
                    '[JsonDerivedType(typeof(ResponseFormatJsonSchema), "json_schema")]'
                );
                expect(generated).toContain(
                    "public partial class ResponseFormatJsonSchema : ResponseFormat"
                );
                expect(generated).toContain('public override string Type => "json_schema";');
                expect(generated).toContain("public ResponseFormat? ResponseFormat");
                expect(generated).toContain(
                    `public ResponseFormat${nullable ? "?" : ""} RequiredFormat`
                );
                expect(generated).toContain("public required JsonSchemaResponseFormat JsonSchema");
                expect(generated).toContain("public JsonElement Schema");
                expect(generated).toContain("public bool? Strict");
                expect(generated).toContain("[JsonSerializable(typeof(ResponseFormat))]");
                expect(generated.match(/public partial class ResponseFormat\b/g)).toHaveLength(1);
            }
            for (const name of ["ResponseFormat", "ResponseFormatJsonSchema"]) {
                const declaration = new RegExp(
                    `public partial class ${name}\\b[^\\n]*\\n\\{[\\s\\S]*?\\n\\}`
                );
                expect(code.match(declaration)?.[0]).toBe(futureCode.match(declaration)?.[0]);
            }
            expect(futureCode).toContain('[JsonDerivedType(typeof(ResponseFormatText), "text")]');
            expect(futureCode).toContain(
                "public partial class ResponseFormatText : ResponseFormat"
            );
        }
    );

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
                "[Experimental(global::GitHub.Copilot.Diagnostics.Experimental)]\n[JsonConverter(typeof(Converter))]\npublic sealed partial class SystemMessage"
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

import type { JSONSchema7 } from "json-schema";
import { describe, expect, it } from "vitest";

import { generateSessionEventsCode as generateCSharpSessionEventsCode } from "../../scripts/codegen/csharp.ts";
import { generateGoSessionEventsCode } from "../../scripts/codegen/go.ts";
import { generatePythonSessionEventsCode } from "../../scripts/codegen/python.ts";
import { generateSessionEventsCode as generateRustSessionEventsCode } from "../../scripts/codegen/rust.ts";

describe("session event codegen", () => {
    it("maps special schema formats to the expected Python types", () => {
        const schema: JSONSchema7 = {
            definitions: {
                SessionEvent: {
                    anyOf: [
                        {
                            type: "object",
                            required: ["type", "data"],
                            properties: {
                                type: { const: "session.synthetic" },
                                data: {
                                    type: "object",
                                    required: [
                                        "at",
                                        "identifier",
                                        "duration",
                                        "integerDuration",
                                        "uri",
                                        "pattern",
                                        "payload",
                                        "encoded",
                                        "count",
                                    ],
                                    properties: {
                                        at: { type: "string", format: "date-time" },
                                        identifier: { type: "string", format: "uuid" },
                                        duration: { type: "number", format: "duration" },
                                        integerDuration: { type: "integer", format: "duration" },
                                        optionalDuration: {
                                            type: ["number", "null"],
                                            format: "duration",
                                        },
                                        action: {
                                            type: "string",
                                            enum: ["store", "vote"],
                                            default: "store",
                                        },
                                        summary: { type: "string", default: "" },
                                        uri: { type: "string", format: "uri" },
                                        pattern: { type: "string", format: "regex" },
                                        payload: { type: "string", format: "byte" },
                                        encoded: { type: "string", contentEncoding: "base64" },
                                        count: { type: "integer" },
                                    },
                                },
                            },
                        },
                    ],
                },
            },
        };

        const code = generatePythonSessionEventsCode(schema);

        expect(code).toContain("from datetime import datetime, timedelta");
        expect(code).toContain("at: datetime");
        expect(code).toContain("identifier: UUID");
        expect(code).toContain("duration: timedelta");
        expect(code).toContain("integer_duration: timedelta");
        expect(code).toContain("optional_duration: timedelta | None = None");
        expect(code).toContain('duration = from_timedelta(obj.get("duration"))');
        expect(code).toContain('result["duration"] = to_timedelta(self.duration)');
        expect(code).toContain(
            'result["integerDuration"] = to_timedelta_int(self.integer_duration)'
        );
        expect(code).toContain("def to_timedelta_int(x: timedelta) -> int:");
        expect(code).toContain(
            'action = from_union([from_none, lambda x: parse_enum(SessionSyntheticDataAction, x)], obj.get("action"))'
        );
        expect(code).toContain('summary = from_union([from_none, from_str], obj.get("summary"))');
        expect(code).not.toContain('obj.get("action", "store")');
        expect(code).not.toContain('obj.get("summary", "")');
        expect(code).toContain("uri: str");
        expect(code).toContain("pattern: str");
        expect(code).toContain("payload: str");
        expect(code).toContain("encoded: str");
        expect(code).toContain("count: int");
    });

    it("strips Ms suffixes from duration member names while preserving JSON names", () => {
        const schema: JSONSchema7 = {
            definitions: {
                SessionEvent: {
                    anyOf: [
                        {
                            type: "object",
                            required: ["type", "data"],
                            properties: {
                                type: { const: "session.synthetic" },
                                data: {
                                    type: "object",
                                    required: ["durationMs", "integerDurationMs", "URLMs"],
                                    properties: {
                                        durationMs: { type: "number", format: "duration" },
                                        integerDurationMs: { type: "integer", format: "duration" },
                                        optionalDurationMs: {
                                            type: ["number", "null"],
                                            format: "duration",
                                        },
                                        nullableDurationMs: {
                                            anyOf: [
                                                { type: "number", format: "duration" },
                                                { type: "null" },
                                            ],
                                        },
                                        URLMs: { type: "number", format: "duration" },
                                    },
                                },
                            },
                        },
                    ],
                },
            },
        };

        const pythonCode = generatePythonSessionEventsCode(schema);

        expect(pythonCode).toContain("duration: timedelta");
        expect(pythonCode).toContain("integer_duration: timedelta");
        expect(pythonCode).toContain("optional_duration: timedelta | None = None");
        expect(pythonCode).toContain("nullable_duration: timedelta | None = None");
        expect(pythonCode).toContain("urlms: timedelta");
        expect(pythonCode).toContain('duration = from_timedelta(obj.get("durationMs"))');
        expect(pythonCode).toContain('result["durationMs"] = to_timedelta(self.duration)');
        expect(pythonCode).toContain(
            'integer_duration = from_timedelta(obj.get("integerDurationMs"))'
        );
        expect(pythonCode).toContain(
            'result["integerDurationMs"] = to_timedelta_int(self.integer_duration)'
        );
        expect(pythonCode).toContain(
            'optional_duration = from_union([from_none, from_timedelta], obj.get("optionalDurationMs"))'
        );
        expect(pythonCode).toContain(
            'result["optionalDurationMs"] = from_union([from_none, to_timedelta], self.optional_duration)'
        );
        expect(pythonCode).toContain(
            'nullable_duration = from_union([from_none, from_timedelta], obj.get("nullableDurationMs"))'
        );
        expect(pythonCode).toContain(
            'result["nullableDurationMs"] = from_union([from_none, to_timedelta], self.nullable_duration)'
        );
        expect(pythonCode).toContain('urlms = from_timedelta(obj.get("URLMs"))');
        expect(pythonCode).toContain('result["URLMs"] = to_timedelta(self.urlms)');

        const csharpCode = generateCSharpSessionEventsCode(schema);

        expect(csharpCode).toContain(
            '[JsonPropertyName("durationMs")]\n    public required TimeSpan Duration { get; set; }'
        );
        expect(csharpCode).toContain(
            '[JsonPropertyName("integerDurationMs")]\n    public required TimeSpan IntegerDuration { get; set; }'
        );
        expect(csharpCode).toContain(
            '[JsonPropertyName("optionalDurationMs")]\n    public TimeSpan? OptionalDuration { get; set; }'
        );
        expect(csharpCode).toContain(
            '[JsonPropertyName("nullableDurationMs")]\n    public TimeSpan? NullableDuration { get; set; }'
        );
        expect(csharpCode).toContain(
            '[JsonPropertyName("URLMs")]\n    public required TimeSpan URLMs { get; set; }'
        );
    });

    it("collapses redundant callable wrapper lambdas", () => {
        const schema: JSONSchema7 = {
            definitions: {
                SessionEvent: {
                    anyOf: [
                        {
                            type: "object",
                            required: ["type", "data"],
                            properties: {
                                type: { const: "session.synthetic" },
                                data: {
                                    type: "object",
                                    properties: {
                                        summary: { type: "string" },
                                        tags: {
                                            type: "array",
                                            items: { type: "string" },
                                        },
                                        context: {
                                            type: "object",
                                            properties: {
                                                gitRoot: { type: "string" },
                                            },
                                        },
                                    },
                                },
                            },
                        },
                    ],
                },
            },
        };

        const code = generatePythonSessionEventsCode(schema);

        expect(code).toContain('summary = from_union([from_none, from_str], obj.get("summary"))');
        expect(code).toContain(
            'tags = from_union([from_none, lambda x: from_list(from_str, x)], obj.get("tags"))'
        );
        expect(code).toContain(
            'context = from_union([from_none, SessionSyntheticDataContext.from_dict], obj.get("context"))'
        );
        expect(code).not.toContain("lambda x: from_str(x)");
        expect(code).not.toContain("lambda x: SessionSyntheticDataContext.from_dict(x)");
        expect(code).not.toContain("from_list(lambda x: from_str(x), x)");
    });

    it("preserves key shortened nested type names", () => {
        const schema: JSONSchema7 = {
            definitions: {
                SessionEvent: {
                    anyOf: [
                        {
                            type: "object",
                            required: ["type", "data"],
                            properties: {
                                type: { const: "permission.requested" },
                                data: {
                                    type: "object",
                                    required: ["requestId", "permissionRequest"],
                                    properties: {
                                        requestId: { type: "string" },
                                        permissionRequest: {
                                            anyOf: [
                                                {
                                                    type: "object",
                                                    required: [
                                                        "kind",
                                                        "fullCommandText",
                                                        "intention",
                                                        "commands",
                                                        "possiblePaths",
                                                        "possibleUrls",
                                                        "hasWriteFileRedirection",
                                                        "canOfferSessionApproval",
                                                    ],
                                                    properties: {
                                                        kind: { const: "shell", type: "string" },
                                                        fullCommandText: { type: "string" },
                                                        intention: { type: "string" },
                                                        commands: {
                                                            type: "array",
                                                            items: {
                                                                type: "object",
                                                                required: [
                                                                    "identifier",
                                                                    "readOnly",
                                                                ],
                                                                properties: {
                                                                    identifier: { type: "string" },
                                                                    readOnly: { type: "boolean" },
                                                                },
                                                            },
                                                        },
                                                        possiblePaths: {
                                                            type: "array",
                                                            items: { type: "string" },
                                                        },
                                                        possibleUrls: {
                                                            type: "array",
                                                            items: {
                                                                type: "object",
                                                                required: ["url"],
                                                                properties: {
                                                                    url: { type: "string" },
                                                                },
                                                            },
                                                        },
                                                        hasWriteFileRedirection: {
                                                            type: "boolean",
                                                        },
                                                        canOfferSessionApproval: {
                                                            type: "boolean",
                                                        },
                                                    },
                                                },
                                                {
                                                    type: "object",
                                                    required: ["kind", "fact"],
                                                    properties: {
                                                        kind: { const: "memory", type: "string" },
                                                        fact: { type: "string" },
                                                        action: {
                                                            type: "string",
                                                            enum: ["store", "vote"],
                                                            default: "store",
                                                        },
                                                        direction: {
                                                            type: "string",
                                                            enum: ["upvote", "downvote"],
                                                        },
                                                    },
                                                },
                                            ],
                                        },
                                    },
                                },
                            },
                        },
                        {
                            type: "object",
                            required: ["type", "data"],
                            properties: {
                                type: { const: "elicitation.requested" },
                                data: {
                                    type: "object",
                                    properties: {
                                        requestedSchema: {
                                            type: "object",
                                            required: ["type", "properties"],
                                            properties: {
                                                type: { const: "object", type: "string" },
                                                properties: {
                                                    type: "object",
                                                    additionalProperties: {},
                                                },
                                            },
                                        },
                                        mode: {
                                            type: "string",
                                            enum: ["form", "url"],
                                        },
                                    },
                                },
                            },
                        },
                        {
                            type: "object",
                            required: ["type", "data"],
                            properties: {
                                type: { const: "capabilities.changed" },
                                data: {
                                    type: "object",
                                    properties: {
                                        ui: {
                                            type: "object",
                                            properties: {
                                                elicitation: { type: "boolean" },
                                            },
                                        },
                                    },
                                },
                            },
                        },
                    ],
                },
            },
        };

        const code = generatePythonSessionEventsCode(schema);

        expect(code).toContain("class PermissionRequest:");
        expect(code).toContain("class PermissionRequestShellCommand:");
        expect(code).toContain("class PermissionRequestShellPossibleURL:");
        expect(code).toContain("class PermissionRequestMemoryAction(Enum):");
        expect(code).toContain("class PermissionRequestMemoryDirection(Enum):");
        expect(code).toContain("class ElicitationRequestedSchema:");
        expect(code).toContain("class ElicitationRequestedMode(Enum):");
        expect(code).toContain("class CapabilitiesChangedUI:");
        expect(code).not.toContain("class PermissionRequestedDataPermissionRequest:");
        expect(code).not.toContain("class ElicitationRequestedDataRequestedSchema:");
        expect(code).not.toContain("class CapabilitiesChangedDataUi:");
    });

    it("keeps distinct enum types even when they share the same values", () => {
        const schema: JSONSchema7 = {
            definitions: {
                SessionEvent: {
                    anyOf: [
                        {
                            type: "object",
                            required: ["type", "data"],
                            properties: {
                                type: { const: "assistant.message" },
                                data: {
                                    type: "object",
                                    properties: {
                                        toolRequests: {
                                            type: "array",
                                            items: {
                                                type: "object",
                                                required: ["toolCallId", "name", "type"],
                                                properties: {
                                                    toolCallId: { type: "string" },
                                                    name: { type: "string" },
                                                    type: {
                                                        type: "string",
                                                        enum: ["function", "custom"],
                                                    },
                                                },
                                            },
                                        },
                                    },
                                },
                            },
                        },
                        {
                            type: "object",
                            required: ["type", "data"],
                            properties: {
                                type: { const: "session.import_legacy" },
                                data: {
                                    type: "object",
                                    properties: {
                                        legacySession: {
                                            type: "object",
                                            properties: {
                                                chatMessages: {
                                                    type: "array",
                                                    items: {
                                                        type: "object",
                                                        properties: {
                                                            toolCalls: {
                                                                type: "array",
                                                                items: {
                                                                    type: "object",
                                                                    properties: {
                                                                        type: {
                                                                            type: "string",
                                                                            enum: [
                                                                                "function",
                                                                                "custom",
                                                                            ],
                                                                        },
                                                                    },
                                                                },
                                                            },
                                                        },
                                                    },
                                                },
                                            },
                                        },
                                    },
                                },
                            },
                        },
                    ],
                },
            },
        };

        const code = generatePythonSessionEventsCode(schema);

        expect(code).toContain("class AssistantMessageToolRequestType(Enum):");
        expect(code).toContain("type: AssistantMessageToolRequestType");
        expect(code).toContain("parse_enum(AssistantMessageToolRequestType,");
        expect(code).toContain(
            "class SessionImportLegacyDataLegacySessionChatMessagesItemToolCallsItemType(Enum):"
        );
    });
});

describe("enum value description codegen", () => {
    const schema: JSONSchema7 = {
        definitions: {
            SessionEvent: {
                anyOf: [
                    {
                        type: "object",
                        required: ["type", "data"],
                        properties: {
                            type: { const: "session.synthetic" },
                            data: {
                                type: "object",
                                required: ["mode", "fallback"],
                                properties: {
                                    mode: {
                                        type: "string",
                                        enum: ["alpha", "beta"],
                                        title: "SyntheticMode",
                                        description: "Synthetic mode.",
                                        "x-enumDescriptions": {
                                            alpha: "Use alpha mode.",
                                        },
                                    },
                                    fallback: {
                                        type: "string",
                                        enum: ["plain"],
                                        title: "FallbackMode",
                                    },
                                },
                            },
                        },
                    },
                ],
            },
        },
    };

    it("emits Python comments for described enum values", () => {
        const code = generatePythonSessionEventsCode(schema);

        expect(code).toContain("class SyntheticMode(Enum):");
        expect(code).toContain('    # Use alpha mode.\n    ALPHA = "alpha"');
        expect(code).toContain('    BETA = "beta"');
    });

    it("emits C# XML docs for described enum values and keeps fallback docs", () => {
        const code = generateCSharpSessionEventsCode(schema);

        expect(code).toContain("public readonly struct SyntheticMode");
        expect(code).toContain(
            "    /// <summary>Use alpha mode.</summary>\n    public static SyntheticMode Alpha"
        );
        expect(code).toContain(
            "    /// <summary>Gets the <c>plain</c> value.</summary>\n    public static FallbackMode Plain"
        );
    });

    it("emits Go comments for described enum values", () => {
        const code = generateGoSessionEventsCode(schema, "rpc").typeCode;

        expect(code).toContain("type SyntheticMode string");
        expect(code).toContain(
            '\t// Use alpha mode.\n\tSyntheticModeAlpha SyntheticMode = "alpha"'
        );
        expect(code).toContain('\tSyntheticModeBeta SyntheticMode = "beta"');
    });

    it("emits Rust docs for described enum values", () => {
        const code = generateRustSessionEventsCode(schema);

        expect(code).toContain("pub enum SyntheticMode {");
        expect(code).toContain(
            '    /// Use alpha mode.\n    #[serde(rename = "alpha")]\n    Alpha,'
        );
        expect(code).toContain('    #[serde(rename = "beta")]\n    Beta,');
    });
});

describe("csharp session event codegen", () => {
    it("emits regular expression attributes for regex format properties with patterns", () => {
        const schema: JSONSchema7 = {
            definitions: {
                SessionEvent: {
                    anyOf: [
                        {
                            type: "object",
                            required: ["type", "data"],
                            properties: {
                                type: { const: "session.synthetic" },
                                data: {
                                    type: "object",
                                    required: ["pattern"],
                                    properties: {
                                        pattern: {
                                            type: "string",
                                            format: "regex",
                                            pattern: "^foo\\d+$",
                                        },
                                    },
                                },
                            },
                        },
                    ],
                },
            },
        };

        const code = generateCSharpSessionEventsCode(schema);

        expect(code).toContain(`    [StringSyntax(StringSyntaxAttribute.Regex)]
    [RegularExpression("^foo\\\\d+$")]
    [JsonPropertyName("pattern")]`);
        expect(code.split(`[RegularExpression("^foo\\\\d+$")]`)).toHaveLength(2);
    });
});

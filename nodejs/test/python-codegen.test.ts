import type { JSONSchema7 } from "json-schema";
import { describe, expect, it } from "vitest";

import { generatePythonSessionEventsCode } from "../../scripts/codegen/python.ts";

describe("python session event codegen", () => {
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
            'action = from_union([from_none, lambda x: parse_enum(SessionSyntheticDataAction, x)], obj.get("action", "store"))'
        );
        expect(code).toContain(
            'summary = from_union([from_none, lambda x: from_str(x)], obj.get("summary", ""))'
        );
        expect(code).toContain("uri: str");
        expect(code).toContain("pattern: str");
        expect(code).toContain("payload: str");
        expect(code).toContain("encoded: str");
        expect(code).toContain("count: int");
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
                                                                required: ["identifier", "readOnly"],
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
                                                                properties: { url: { type: "string" } },
                                                            },
                                                        },
                                                        hasWriteFileRedirection: { type: "boolean" },
                                                        canOfferSessionApproval: { type: "boolean" },
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

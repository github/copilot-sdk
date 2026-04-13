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
});

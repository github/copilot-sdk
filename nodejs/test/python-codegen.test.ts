import { describe, expect, it } from "vitest";

import { generatePythonSessionEventsCode } from "../../scripts/codegen/python.ts";

describe("Python root event payload unions", () => {
    it.each(["anyOf", "oneOf"] as const)("preserves referenced %s payload variants", (keyword) => {
        const code = generatePythonSessionEventsCode({
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
        expect(code).toContain("class SampleUnionData:");
        expect(code).toContain("kind: SampleUnionDataKind");
        expect(code).toContain("value: str");
        for (const kind of ["status", "startup", "server_error", "incremental"]) {
            expect(code).toContain(`= "${kind}"`);
        }
        expect(code).toContain('result["value"] = from_str(self.value)');
        expect(code).toContain("data = SampleUnionData.from_dict(data_obj)");
        expect(code).toContain("return SampleEmptyData()");
    });
});

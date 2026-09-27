import { describe, expect, it } from "vitest";

import {
    applyPythonLegacyParameters,
    generatePythonSessionEventsCode,
} from "../../scripts/codegen/python.ts";
import { legacyRequestSchema } from "./legacy-parameters-fixture.ts";

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

function pythonRequestSnippet(additions: 1 | 2): string {
    const traceField = additions >= 2 ? "    trace_id: str | None = None\n\n" : "";
    const traceLoad =
        additions >= 2
            ? '        trace_id = from_union([from_str, from_none], obj.get("traceId"))\n'
            : "";
    const traceArg = additions >= 2 ? ", trace_id" : "";
    return [
        "@dataclass",
        "class SamplePlanRequest:",
        "    contract: str",
        "",
        "    source: str",
        "",
        "    policy_session_id: str | None = None",
        "",
        "    scope: str | None = None",
        "",
        traceField + "    @staticmethod",
        "    def from_dict(obj: Any) -> 'SamplePlanRequest':",
        "        assert isinstance(obj, dict)",
        '        contract = from_str(obj.get("contract"))',
        '        source = from_str(obj.get("source"))',
        '        policy_session_id = from_union([from_str, from_none], obj.get("policySessionId"))',
        '        scope = from_union([from_str, from_none], obj.get("scope"))',
        traceLoad +
            `        return SamplePlanRequest(contract, source, policy_session_id, scope${traceArg})`,
        "",
    ].join("\n");
}

describe("Python x-legacy-parameters", () => {
    it.each([1, 2] as const)(
        "keeps legacy fields positional and makes %i added field(s) keyword-only",
        (additions) => {
            const { params } = legacyRequestSchema(additions);
            const code = applyPythonLegacyParameters(pythonRequestSnippet(additions), {
                SamplePlanRequest: params,
            });
            expect(code).toContain("    contract: str\n");
            expect(code).toContain("    source: str\n");
            expect(code).toContain("    scope: str | None = None\n");
            expect(code).toContain(
                "    policy_session_id: str | None = field(default=None, kw_only=True)"
            );
            if (additions === 2) {
                expect(code).toContain(
                    "    trace_id: str | None = field(default=None, kw_only=True)"
                );
                expect(code).toContain(
                    "return SamplePlanRequest(contract, source, scope, policy_session_id=policy_session_id, trace_id=trace_id)"
                );
            } else {
                expect(code).toContain(
                    "return SamplePlanRequest(contract, source, scope, policy_session_id=policy_session_id)"
                );
            }
        }
    );

    it("leaves unmarked dataclasses unchanged", () => {
        const snippet = pythonRequestSnippet(1);
        expect(
            applyPythonLegacyParameters(snippet, {
                SamplePlanRequest: legacyRequestSchema(0).params,
            })
        ).toBe(snippet);
    });
});

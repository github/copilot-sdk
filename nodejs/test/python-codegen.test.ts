import { readFileSync } from "node:fs";
import { describe, expect, it } from "vitest";

import {
    appendLastPythonRpcConstructorFields,
    applyPythonLegacyParameters,
    emitMethod,
    generatePythonSessionEventsCode,
    isPythonObjectResultSchema,
    pythonAppendLastFieldsPresentIn,
} from "../../scripts/codegen/python.ts";
import { legacyRequestSchema } from "./legacy-parameters-fixture.ts";

describe("Python referenced RPC results", () => {
    it("uses record deserialization for nullable object references, but not enums", () => {
        const definitions = {
            definitions: {
                Account: {
                    type: "object" as const,
                    properties: { accountId: { type: "string" as const } },
                    required: ["accountId"],
                },
                AccountResult: {
                    anyOf: [{ $ref: "#/definitions/Account" }, { type: "null" as const }],
                },
                Mode: { type: "string" as const, enum: ["token"] },
            },
        };

        expect(isPythonObjectResultSchema({ $ref: "#/definitions/Account" }, definitions)).toBe(
            true
        );
        expect(
            isPythonObjectResultSchema({ $ref: "#/definitions/AccountResult" }, definitions)
        ).toBe(true);
        expect(isPythonObjectResultSchema({ $ref: "#/definitions/Mode" }, definitions)).toBe(false);
    });
});

describe("Python RPC projection compatibility", () => {
    const code = readFileSync(
        new URL("../../python/copilot/generated/rpc.py", import.meta.url),
        "utf8"
    );

    it("preserves the existing Workflow checkpoint result API", () => {
        const result = "SessionWorkflowPauseAtCheckpointResult";
        expect(code).toContain(`class ${result}:`);
        expect(code).toContain(`-> ${result}:`);
        expect(code).toContain(`return ${result}.from_dict(`);
        expect(code).toContain(`"${result}",`);
    });

    it("does not restore the retired Factory checkpoint aliases", () => {
        expect(code).not.toContain("SessionFactoryPauseAtCheckpointResult");
        expect(code).not.toContain('"session.factory.pauseAtCheckpoint"');
    });

    it("keeps named connection-scoped host callback results", () => {
        expect(code).toContain(
            "async def materialize_session(self, params: HostSessionCreateCallback) -> HostSessionCreateResult:"
        );
        expect(code).toContain(
            "async def register_session(self, params: HostRegisterSessionRequest) -> HostPublishSessionResult:"
        );
        expect(code).toContain("class HostEmptyResult:");
        expect(code).toContain(
            "async def shutdown(self, params: HostEmptyResult) -> HostEmptyResult:"
        );
        expect(code).toContain(
            'return HostEmptyResult.from_dict(await self._client.request("host.dispose"'
        );
        expect(code).not.toContain("dict.from_dict");
    });
});

it("deserializes open empty-object results as dictionaries", () => {
    const lines: string[] = [];
    emitMethod(
        lines,
        "dispose",
        {
            rpcMethod: "host.dispose",
            params: {
                type: "object",
                properties: { hostId: { type: "string" } },
                required: ["hostId"],
            },
            result: { type: "object", properties: {} },
        },
        false,
        (name) => (name === "HostDisposeResult" ? "dict" : name)
    );
    const code = lines.join("\n");
    expect(code).toContain('return dict(await self._client.request("host.dispose"');
    expect(code).not.toContain("dict.from_dict");
});

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

describe("Python append-last RPC fields", () => {
    const fields = [["Request", "addedField"]] as const;
    const request = (docstring: string) =>
        [
            "@dataclass",
            "class Request:",
            `    """${docstring}"""`,
            "",
            "    added_field: str | None = None",
            "    stable_field: str | None = None",
            "",
            "    @staticmethod",
            "    def from_dict(obj: Any) -> 'Request':",
            '        added_field = from_str(obj.get("addedField"))',
            '        stable_field = from_str(obj.get("stableField"))',
            "        return Request(added_field, stable_field)",
            "",
        ].join("\n");

    it("reorders a dataclass that is the last block in the file", () => {
        const updated = appendLastPythonRpcConstructorFields(request("Request."), fields);

        expect(updated.indexOf("stable_field: str")).toBeLessThan(
            updated.indexOf("added_field: str")
        );
        expect(updated).toContain("return Request(stable_field, added_field)");
    });

    it("does not end the dataclass block at a capital Z before its constructor", () => {
        const updated = appendLastPythonRpcConstructorFields(
            `${request("Zone request.")}\n@dataclass\nclass Next:\n    pass\n`,
            fields
        );

        expect(updated.indexOf("stable_field: str")).toBeLessThan(
            updated.indexOf("added_field: str")
        );
        expect(updated).toContain("return Request(stable_field, added_field)");
        expect(updated).toContain("class Next:");
    });

    it("fails generation instead of skipping a missing dataclass", () => {
        expect(() =>
            appendLastPythonRpcConstructorFields("@dataclass\nclass Other:\n    pass\n", fields)
        ).toThrow("Missing dataclass Request");
    });
});

describe("Python append-last fields for a selected schema", () => {
    const fields = [["Request", "addedField"]] as const;

    it("keeps an entry whose property the schema declares", () => {
        expect(
            pythonAppendLastFieldsPresentIn(
                { Request: { type: "object", properties: { addedField: { type: "string" } } } },
                fields
            )
        ).toEqual(fields);
    });

    it("includes fields marked append-last by the contract schema", () => {
        expect(
            pythonAppendLastFieldsPresentIn({
                Response: {
                    type: "object",
                    properties: {
                        stableField: { type: "string" },
                        addedField: {
                            type: "string",
                            "x-copilot-sdk-append-last": true,
                        },
                    },
                },
            })
        ).toContainEqual(["Response", "addedField"]);
    });

    it("skips an entry for a legacy request or a schema without the definition", () => {
        expect(
            pythonAppendLastFieldsPresentIn(
                { Request: { type: "object", properties: { stableField: { type: "string" } } } },
                fields
            )
        ).toEqual([]);
        expect(pythonAppendLastFieldsPresentIn({}, fields)).toEqual([]);
    });
});

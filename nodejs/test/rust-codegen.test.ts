import type { ApiSchema } from "../../scripts/codegen/utils.ts";
import type { JSONSchema7 } from "json-schema";
import { readFileSync } from "node:fs";
import { describe, expect, it } from "vitest";

import {
    generateApiTypesCode,
    generateSessionEventsCode,
    isRustCodegenEntrypoint,
} from "../../scripts/codegen/rust.ts";

describe("Rust codegen entrypoint", () => {
    it("matches Windows paths case-insensitively", () => {
        expect(
            isRustCodegenEntrypoint(
                "C:\\a\\copilot-agent-runtime\\src\\sdk\\scripts\\codegen\\rust.ts",
                "c:\\a\\copilot-agent-runtime\\src\\sdk\\scripts\\codegen\\rust.ts",
                "win32"
            )
        ).toBe(true);
    });
});

describe("Rust API type codegen", () => {
    it("retains named action unions inside single-variant reference wrappers without titles", () => {
        const code = generateApiTypesCode({
            definitions: {
                Request: {
                    type: "object",
                    required: ["review"],
                    properties: { review: { $ref: "#/definitions/Review" } },
                },
                Review: {
                    anyOf: [
                        {
                            type: "object",
                            required: ["resource", "review"],
                            properties: {
                                resource: { type: "string", const: "mcp" },
                                review: { $ref: "#/definitions/ActionReview" },
                            },
                        },
                    ],
                },
                ActionReview: {
                    anyOf: ["install", "uninstall"].map((action) => ({
                        type: "object",
                        required: ["action", "identity"],
                        properties: {
                            action: { type: "string", const: action },
                            identity: { type: "string" },
                        },
                    })),
                },
            },
        } as ApiSchema);

        expect(code).toContain("pub review: Review,");
        expect(code).toContain("pub review: ActionReview,");
        expect(code).toContain(`pub enum ActionReview {
    Install(ActionReviewInstall),
    Uninstall(ActionReviewUninstall),
}`);
        expect(code).not.toContain("RequestReview");
        expect(code).not.toContain("ActionReviewValue");
        expect(code).not.toContain("serde_json::Value,");
        expect(code).toContain(`#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Request`);
    });

    it("preserves the published MCP transport choice schema as a typed union", () => {
        // Exact selected definitions from CLI 1.0.89-1 api.schema.json:
        // sha256 a445b552b6ecef536b89f3d08cc73b6fbbe8fe0e503daae8974578529d62bc83.
        const schema = JSON.parse(
            readFileSync(
                new URL("./fixtures/mcp-plan-transport-choice.schema.json", import.meta.url),
                "utf8"
            )
        ) as ApiSchema;
        schema.definitions!.Plan = {
            type: "object",
            required: ["transportChoices"],
            properties: {
                transportChoices: {
                    type: "array",
                    items: { $ref: "#/definitions/McpPlanTransportChoice" },
                },
            },
        };
        const code = generateApiTypesCode(schema);

        expect(code).toContain(`#[serde(untagged)]
pub enum McpPlanTransportChoice {
    Package(McpPlanTransportChoicePackage),
    Remote(McpPlanTransportChoiceRemote),
}`);
        expect(code).toContain("pub transport_choices: Vec<McpPlanTransportChoice>,");
        expect(code).toContain("pub required_values: Vec<McpPlanRequiredValue>,");
        expect(code).toContain("pub secret_placeholders: Vec<McpPlanSecretPlaceholder>,");
        expect(code).toContain(
            'deserialize_with = "McpPlanTransportChoicePackage::deserialize_install_method"'
        );
        expect(code).toContain(
            'deserialize_with = "McpPlanTransportChoiceRemote::deserialize_install_method"'
        );
        expect(code).toContain('if value != "package"');
        expect(code).toContain('if value != "remote"');
        expect(code).not.toContain("Vec<serde_json::Value>");
    });

    it.each(["anyOf", "oneOf"] as const)(
        "supports arbitrary required enum-reference discriminators in %s unions",
        (keyword) => {
            const code = generateApiTypesCode({
                definitions: {
                    Mode: { type: "string", enum: ["first", "second"] },
                    Choice: {
                        title: "Choice",
                        [keyword]: ["first", "second"].map((value) => ({
                            type: "object",
                            required: ["mode", "value"],
                            properties: {
                                mode: { $ref: "#/definitions/Mode", const: value },
                                value: { type: "string" },
                            },
                        })),
                    },
                    Container: {
                        type: "object",
                        required: ["choice"],
                        properties: { choice: { $ref: "#/definitions/Choice" } },
                    },
                },
            } as ApiSchema);

            expect(code).toContain(`#[serde(untagged)]
pub enum Choice {
    First(ChoiceFirst),
    Second(ChoiceSecond),
}`);
            expect(code).toContain(`#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Container`);
            expect(code).toContain("pub choice: Choice,");
        }
    );

    it.each(["optional", "duplicate", "missing", "number", "inline", "outside-enum"] as const)(
        "retains the JSON fallback for %s discriminators without a supported union",
        (invalid) => {
            const variants: JSONSchema7[] = ["first", "second"].map((value) => ({
                type: "object",
                required: invalid === "optional" ? ["value"] : ["mode", "value"],
                properties: {
                    mode:
                        invalid === "missing"
                            ? { type: "string" }
                            : invalid === "number"
                              ? { type: "integer", const: value === "first" ? 1 : 2 }
                              : invalid === "inline"
                                ? { type: "string", const: value }
                                : {
                                      $ref: "#/definitions/Mode",
                                      const: invalid === "duplicate" ? "same" : value,
                                  },
                    value: { type: "string" },
                },
            }));
            const code = generateApiTypesCode({
                definitions: {
                    Mode: {
                        type: "string",
                        enum: invalid === "outside-enum" ? ["different"] : ["first", "second"],
                    },
                    Container: {
                        type: "object",
                        required: ["choice"],
                        properties: { choice: { anyOf: variants } },
                    },
                },
            } as ApiSchema);

            expect(code).toContain("pub choice: serde_json::Value,");
            expect(code).not.toContain("pub enum ContainerChoice");
        }
    );

    it.each(["anyOf", "oneOf"] as const)(
        "preserves raw optional metadata when newly recognising referenced %s discriminators",
        (keyword) => {
            const code = generateApiTypesCode({
                definitions: {
                    Status: { type: "string", enum: ["known", "unavailable"] },
                    Snapshot: {
                        title: "Snapshot",
                        [keyword]: ["known", "unavailable"].map((status) => ({
                            type: "object",
                            required: ["status"],
                            properties: {
                                status: { $ref: "#/definitions/Status", const: status },
                            },
                        })),
                    },
                    Container: {
                        type: "object",
                        required: ["requiredSnapshot"],
                        properties: {
                            requiredSnapshot: { $ref: "#/definitions/Snapshot" },
                            optionalSnapshot: { $ref: "#/definitions/Snapshot" },
                        },
                    },
                },
            } as ApiSchema);

            expect(code).toContain("pub required_snapshot: Snapshot,");
            expect(code).toContain("pub optional_snapshot: Option<serde_json::Value>,");
            expect(code).not.toContain("pub optional_snapshot: Option<Snapshot>,");
        }
    );

    it.each([true, false])(
        "validates a reference's sibling constant without changing its enum (required: %s)",
        (required) => {
            const code = generateApiTypesCode({
                definitions: {
                    CandidateKind: {
                        type: "string",
                        enum: ["mcp-server", "ai-skill", "unknown"],
                    },
                    Candidate: {
                        type: "object",
                        required: required ? ["kind", "ordinaryKind"] : ["ordinaryKind"],
                        properties: {
                            kind: {
                                $ref: "#/definitions/CandidateKind",
                                const: "ai-skill",
                            },
                            ordinaryKind: { $ref: "#/definitions/CandidateKind" },
                        },
                    },
                },
            } as ApiSchema);

            expect(code).toContain(
                `#[serde(${required ? "" : "default, "}deserialize_with = "Candidate::deserialize_kind")]`
            );
            expect(code).toContain(
                `pub kind: ${required ? "CandidateKind" : "Option<CandidateKind>"},`
            );
            expect(code).toContain("pub ordinary_kind: CandidateKind,");
            expect(code).not.toContain("Candidate::deserialize_ordinary_kind");
            expect(code).toContain(`if value != "ai-skill" {`);
            expect(code).toContain(`&["ai-skill"]`);
            expect(code).toContain(
                "<CandidateKind>::deserialize(serde::de::value::StringDeserializer::<D::Error>::new(value))"
            );
            expect(code.match(/fn deserialize_/g)).toHaveLength(1);
            if (!required) {
                expect(code).toContain("Option::<String>::deserialize(deserializer)?");
                expect(code).toContain("return Ok(None);");
                expect(code).toContain(".map(Some)");
            }
            expect(code).toContain(`#[serde(rename = "mcp-server")]
    McpServer,`);
            expect(code).toContain(`#[serde(rename = "ai-skill")]
    AiSkill,`);
            expect(code).toContain(`#[serde(rename = "unknown")]
    UnknownValue,`);
            expect(code).toContain(`#[default]
    #[serde(other)]
    Unknown,`);
        }
    );

    it("separates adjacent constant helpers before formatting", () => {
        const property: JSONSchema7 = { $ref: "#/definitions/Kind", const: "ai-skill" };
        const code = generateApiTypesCode({
            definitions: {
                Kind: { type: "string", enum: ["ai-skill", "mcp-server"] },
                Candidate: {
                    type: "object",
                    required: ["first", "second"],
                    properties: { first: property, second: { ...property } },
                },
            },
        } as ApiSchema);

        expect(code).toContain("    }\n\n    fn deserialize_second");
    });

    it("distinguishes a protocol-defined unknown value from the forward-compatible fallback", () => {
        const code = generateApiTypesCode({
            definitions: {
                CatalogTrustEligibility: {
                    type: "string",
                    enum: ["default", "expanded", "hidden", "unknown"],
                },
            },
        } as ApiSchema);

        expect(code).toContain(`#[serde(rename = "unknown")]
    UnknownValue,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,`);
    });

    it("publicly re-exports API types moved into the shared session-events schema", () => {
        const code = generateApiTypesCode({
            definitions: {
                PermissionDecision: {
                    type: "object",
                    required: ["source"],
                    properties: {
                        source: {
                            $ref: "session-events.schema.json#/definitions/PermissionDecisionSource",
                        },
                    },
                },
            },
        } as ApiSchema);

        expect(code).toContain("pub use super::session_events::{PermissionDecisionSource};");
        expect(code).toContain("use crate::types::{RequestId, SessionId};");
    });

    it("emits the enqueue result union discriminated by queued", () => {
        const code = generateApiTypesCode({
            definitions: {
                AcceptedEnqueueCommandResult: {
                    type: "object",
                    required: ["queued", "queueId"],
                    properties: {
                        queued: { type: "boolean", const: true },
                        queueId: { type: "string" },
                    },
                },
                UnsupportedEnqueueCommandResult: {
                    type: "object",
                    required: ["queued"],
                    properties: {
                        queued: { type: "boolean", const: false },
                        queueId: { type: ["string", "null"] },
                    },
                },
                EnqueueCommandResult: {
                    anyOf: [
                        { $ref: "#/definitions/AcceptedEnqueueCommandResult" },
                        { $ref: "#/definitions/UnsupportedEnqueueCommandResult" },
                    ],
                    title: "EnqueueCommandResult",
                },
            },
        } as ApiSchema);

        expect(code).toContain("#[serde(untagged)]");
        expect(code).toContain("pub enum EnqueueCommandResult {");
        expect(code).toContain("True(AcceptedEnqueueCommandResult),");
        expect(code).toContain("False(UnsupportedEnqueueCommandResult),");
        expect(code).toContain(
            'deserialize_with = "AcceptedEnqueueCommandResult::deserialize_queued", serialize_with = "AcceptedEnqueueCommandResult::serialize_queued"'
        );
        expect(code).toContain(
            'deserialize_with = "UnsupportedEnqueueCommandResult::deserialize_queued", serialize_with = "UnsupportedEnqueueCommandResult::serialize_queued"'
        );
        expect(code).toContain('serde::de::Error::custom("expected true")');
        expect(code).toContain('serde::ser::Error::custom("expected true")');
        expect(code).toContain('serde::de::Error::custom("expected false")');
        expect(code).toContain('serde::ser::Error::custom("expected false")');
        expect(code).toContain("if !value {");
        expect(code).toContain("if !*value {");
        expect(code).toContain("if value {");
        expect(code).toContain("if *value {");
        expect(code).not.toMatch(/\bvalue != (?:true|false)\b/);
        expect(code).not.toMatch(/\*value != (?:true|false)\b/);
    });

    it.each([
        ["unknown", "unknown_value"],
        ["unknown_value", "unknown"],
        ["not-called", "not_called"],
    ])("rejects colliding wire values %s and %s", (first, second) => {
        expect(() =>
            generateApiTypesCode({
                definitions: {
                    Collision: { type: "string", enum: [first, second] },
                },
            } as ApiSchema)
        ).toThrow("is not unique");
    });

    it("keeps the fallback name reserved for wire values other than exact lowercase unknown", () => {
        expect(() =>
            generateApiTypesCode({
                definitions: {
                    Collision: { type: "string", enum: ["Unknown"] },
                },
            } as ApiSchema)
        ).toThrow('Generated Rust enum variant identifier "Unknown" is not unique');
    });
});

describe("Rust session event codegen", () => {
    function eventSchema(
        data: JSONSchema7,
        definitions: JSONSchema7["definitions"] = {}
    ): JSONSchema7 {
        return {
            definitions: {
                ...definitions,
                SessionEvent: {
                    anyOf: [
                        {
                            type: "object",
                            required: ["type", "data"],
                            properties: {
                                type: { const: "session.search" },
                                data,
                            },
                        },
                    ],
                },
            },
        };
    }

    it.each([
        ["anyOf", false],
        ["anyOf", true],
        ["oneOf", false],
        ["oneOf", true],
    ] as const)("preserves root %s event unions (referenced: %s)", (keyword, referenced) => {
        const payload: JSONSchema7 = {
            ...(referenced ? { title: "SearchPayload" } : {}),
            [keyword]: [
                {
                    type: "object",
                    required: ["kind", "ready"],
                    properties: {
                        kind: { type: "string", const: "status" },
                        ready: { type: "boolean" },
                    },
                },
                {
                    type: "object",
                    required: ["kind", "durationMs"],
                    properties: {
                        kind: { type: "string", const: "startup" },
                        durationMs: { type: "number" },
                    },
                },
            ],
        };
        const code = generateSessionEventsCode(
            eventSchema(
                referenced ? { $ref: "#/definitions/SearchPayload" } : payload,
                referenced ? { SearchPayload: payload } : {}
            )
        );
        const name = referenced ? "SearchPayload" : "SessionSearchData";

        expect(code).toContain("SessionSearch(SessionSearchData),");
        expect(code).toContain(`#[serde(untagged)]
pub enum ${name} {
    Status(${name}Status),
    Startup(${name}Startup),
}`);
        expect(code).toContain("pub ready: bool,");
        expect(code).toContain("pub duration_ms: f64,");
        expect(code).not.toContain("pub struct SessionSearchData {");
        if (referenced) {
            expect(code).toContain("pub type SessionSearchData = SearchPayload;");
        } else {
            expect(code).not.toContain("pub type SessionSearchData = SessionSearchData;");
        }
    });

    it.each(["anyOf", "oneOf"] as const)(
        "preserves unrepresentable root %s event payloads as raw JSON",
        (keyword) => {
            const code = generateSessionEventsCode(
                eventSchema({ [keyword]: [{ type: "string" }, { type: "number" }] })
            );

            expect(code).toContain("pub type SessionSearchData = serde_json::Value;");
            expect(code).toContain("SessionSearch(SessionSearchData),");
            expect(code).not.toContain("pub struct SessionSearchData");
        }
    );

    it("keeps ordinary and empty object event payloads as structs", () => {
        const ordinary = generateSessionEventsCode(
            eventSchema({
                type: "object",
                required: ["message"],
                properties: { message: { type: "string" } },
            })
        );
        const empty = generateSessionEventsCode(eventSchema({ type: "object", properties: {} }));

        expect(ordinary).toContain("pub struct SessionSearchData {\n    pub message: String,\n}");
        expect(empty).toContain("pub struct SessionSearchData {\n}");
        expect(ordinary).not.toContain("pub type SessionSearchData");
        expect(empty).not.toContain("pub type SessionSearchData");
    });

    it.each(["reasonCode", "judgeStatus", "evaluationStage"])(
        "preserves explicit unknown values in nested approval %s enums",
        (property) => {
            const code = generateSessionEventsCode({
                definitions: {
                    SessionEvent: {
                        anyOf: [
                            {
                                type: "object",
                                required: ["type", "data"],
                                properties: {
                                    type: { const: "permission.completed" },
                                    data: {
                                        type: "object",
                                        properties: {
                                            approval: {
                                                type: "object",
                                                properties: {
                                                    [property]: {
                                                        type: "string",
                                                        enum: ["unknown", "inherited"],
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
            });

            expect(code).toContain(`#[serde(rename = "unknown")]
    UnknownValue,`);
            expect(code).toContain(`#[serde(rename = "inherited")]
    Inherited,`);
            expect(code).toContain(`/// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,`);
        }
    );
});

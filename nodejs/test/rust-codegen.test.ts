import type { ApiSchema } from "../../scripts/codegen/utils.ts";
import type { JSONSchema7 } from "json-schema";
import { describe, expect, it } from "vitest";

import { generateApiTypesCode, generateSessionEventsCode } from "../../scripts/codegen/rust.ts";

describe("Rust API type codegen", () => {
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

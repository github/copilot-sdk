import type { ApiSchema } from "../../scripts/codegen/utils.ts";
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
});

describe("Rust session event codegen", () => {
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

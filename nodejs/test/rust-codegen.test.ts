import type { ApiSchema } from "../../scripts/codegen/utils.ts";
import { describe, expect, it } from "vitest";

import { generateApiTypesCode } from "../../scripts/codegen/rust.ts";

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
});

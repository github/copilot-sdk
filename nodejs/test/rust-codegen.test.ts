import type { ApiSchema } from "../../scripts/codegen/utils.ts";
import type { JSONSchema7 } from "json-schema";
import { readFileSync } from "node:fs";
import { describe, expect, it } from "vitest";

import {
    generateApiTypesCode,
    generateRpcCode,
    generateSessionEventsCode,
    isRustCodegenEntrypoint,
    RELEASED_RUST_STRING_ENUM_COLLISIONS,
} from "../../scripts/codegen/rust.ts";
import { legacyRequestSchema } from "./legacy-parameters-fixture.ts";

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
    it.each([
        ["anyOf", true],
        ["oneOf", true],
        ["anyOf", false],
        ["oneOf", false],
    ] as const)(
        "retains directly resolved %s property names as aliases once (nullable: %s)",
        (keyword, nullable) => {
            const code = generateApiTypesCode({
                definitions: {
                    Payload: {
                        [keyword]: [
                            {
                                type: "object",
                                required: ["value", "mode"],
                                properties: {
                                    value: { type: "string" },
                                    mode: { type: "string", enum: ["first", "second"] },
                                },
                            },
                            ...(nullable ? [{ type: "null" }] : []),
                        ],
                    },
                    Container: {
                        type: "object",
                        required: ["value", "items", "itemsItem"],
                        properties: {
                            value: { $ref: "#/definitions/Payload" },
                            items: {
                                type: "array",
                                items: { $ref: "#/definitions/Payload" },
                            },
                            itemsItem: { $ref: "#/definitions/Payload" },
                        },
                    },
                },
            } as ApiSchema);

            expect(code).toContain("pub type ContainerValue = Payload;");
            expect(code.match(/pub type ContainerItemsItem = Payload;/g)).toHaveLength(1);
            expect(code).toContain(`pub value: ${nullable ? "Option<Payload>" : "Payload"},`);
            expect(code).toContain(`pub items: Vec<${nullable ? "Option<Payload>" : "Payload"}>,`);
            expect(code).toContain("pub enum PayloadMode");
            expect(code.match(/pub type ContainerValueMode = PayloadMode;/g)).toHaveLength(1);
            expect(code.match(/pub type ContainerItemsItemMode = PayloadMode;/g)).toHaveLength(1);
        }
    );

    it("keeps nested names an inline projection emitted when a reference alias replaces it", () => {
        const code = generateApiTypesCode({
            definitions: {
                Attribution: {
                    anyOf: [
                        {
                            type: "object",
                            required: ["source", "entries", "categories"],
                            properties: {
                                source: { type: "string", enum: ["runtime", "host"] },
                                entries: {
                                    type: "array",
                                    items: {
                                        type: "object",
                                        properties: { id: { type: "string" } },
                                    },
                                },
                                categories: {
                                    type: "object",
                                    properties: { system: { type: "integer" } },
                                },
                            },
                        },
                    ],
                },
                FirstResult: {
                    type: "object",
                    required: ["attribution"],
                    properties: { attribution: { $ref: "#/definitions/Attribution" } },
                },
                SecondResult: {
                    type: "object",
                    required: ["attribution"],
                    properties: { attribution: { $ref: "#/definitions/Attribution" } },
                },
            },
        } as ApiSchema);

        for (const owner of ["FirstResultAttribution", "SecondResultAttribution"]) {
            expect(code).toContain(`pub type ${owner} = Attribution;`);
            expect(code).toContain(`pub type ${owner}Source = AttributionSource;`);
            expect(code).toContain(`pub type ${owner}EntriesItem = AttributionEntriesItem;`);
            expect(code).toContain(`pub type ${owner}Categories = AttributionCategories;`);
        }
        expect(code).toMatch(
            /#\[doc\(hidden\)\]\n#\[deprecated\]\npub type FirstResultAttributionSource = AttributionSource;/
        );
    });

    it("keeps an x-legacy-untyped field raw while emitting its typed union", () => {
        const choice = {
            title: "Choice",
            oneOf: ["package", "remote"].map((kind) => ({
                type: "object",
                required: ["kind"],
                properties: { kind: { type: "string", const: kind } },
            })),
        };
        const schema = (legacy: unknown) =>
            ({
                definitions: {
                    Choice: choice,
                    Plan: {
                        type: "object",
                        required: ["choices"],
                        properties: {
                            choices: {
                                type: "array",
                                items: { $ref: "#/definitions/Choice" },
                                ...(legacy === undefined ? {} : { "x-legacy-untyped": legacy }),
                            },
                        },
                    },
                },
            }) as ApiSchema;

        const typed = generateApiTypesCode(schema(undefined));
        expect(typed).toContain("pub choices: Vec<Choice>,");
        const legacy = generateApiTypesCode(schema(true));
        expect(legacy).toContain("pub choices: Vec<serde_json::Value>,");
        expect(legacy).toContain("pub enum Choice");
        expect(() => generateApiTypesCode(schema("yes"))).toThrow(/x-legacy-untyped must be true/);
    });

    it.each([
        [
            "nullable array",
            { anyOf: [{ type: "array", items: { type: "string" } }, { type: "null" }] },
        ],
        ["array reference", { $ref: "#/definitions/Items" }],
    ])("refuses x-legacy-untyped on a %s instead of silently narrowing it", (_label, property) => {
        expect(() =>
            generateApiTypesCode({
                definitions: {
                    Items: { type: "array", items: { type: "string" } },
                    Plan: {
                        type: "object",
                        properties: { values: { ...property, "x-legacy-untyped": true } },
                    },
                },
            } as ApiSchema)
        ).toThrow(/x-legacy-untyped supports only a plain array or object property/);
    });

    it("emits nested compatibility names independently of definition order", () => {
        const target = {
            anyOf: [
                {
                    type: "object",
                    required: ["source"],
                    properties: { source: { type: "string", enum: ["runtime", "host"] } },
                },
            ],
        };
        const owner = {
            type: "object",
            required: ["attribution"],
            properties: { attribution: { $ref: "#/definitions/Attribution" } },
        };
        for (const definitions of [
            { Owner: owner, Attribution: target },
            { Attribution: target, Owner: owner },
        ]) {
            const code = generateApiTypesCode({ definitions } as ApiSchema);
            expect(
                code.match(/pub type OwnerAttributionSource = AttributionSource;/g)
            ).toHaveLength(1);
        }
    });

    it("pins the exact nested compatibility names generated for the committed API schema", () => {
        const generated = readFileSync(
            new URL("../../rust/src/generated/api_types.rs", import.meta.url),
            "utf8"
        );
        const names = [
            ...generated.matchAll(
                /\/\/\/ Compatibility name for \[`\w+`\]\.\n(?:\/\/\/.*\n|#\[.*\]\n)*pub type (\w+)/g
            ),
        ].map((match) => match[1]);
        expect(names.sort()).toEqual([
            "DiagnosticsReadResultEntriesItemSource",
            "InstallationConfirmationRequestReviewResource",
            "MetadataContextAttributionResultContextAttributionCategories",
            "MetadataContextAttributionResultContextAttributionCompactions",
            "MetadataContextAttributionResultContextAttributionEntriesItem",
            "SendMessagesRequestResponseFormatType",
            "SendRequestResponseFormatType",
            "SessionDiagnosticsReadResultEntriesItemSource",
            "SessionMetadataGetContextAttributionResultContextAttributionCategories",
            "SessionMetadataGetContextAttributionResultContextAttributionCompactions",
            "SessionMetadataGetContextAttributionResultContextAttributionEntriesItem",
        ]);
    });

    it.each([
        ["before", "object"],
        ["after", "object"],
        ["before", "enum"],
        ["after", "enum"],
        ["before", "array"],
        ["after", "array"],
    ] as const)(
        "refuses a direct alias colliding with a type emitted %s it (%s)",
        (order, kind) => {
            const collision = {
                ContainerValue:
                    kind === "enum"
                        ? { type: "string", enum: ["existing"] }
                        : kind === "array"
                          ? { type: "array", items: { type: "string" } }
                          : { type: "object", properties: { id: { type: "string" } } },
            };
            const schema = {
                definitions: {
                    Payload: {
                        anyOf: [
                            { type: "object", properties: { value: { type: "string" } } },
                            { type: "null" },
                        ],
                    },
                    ...(order === "before" ? collision : {}),
                    Container: {
                        type: "object",
                        properties: { value: { $ref: "#/definitions/Payload" } },
                    },
                    ...(order === "after" ? collision : {}),
                },
            } as ApiSchema;

            expect(() => generateApiTypesCode(schema)).toThrow(/ContainerValue.*collid/);
        }
    );

    it("refuses different direct targets for the same property fallback name", () => {
        const nullable = {
            anyOf: [
                { type: "object", properties: { value: { type: "string" } } },
                { type: "null" },
            ],
        };
        const schema = {
            definitions: {
                First: nullable,
                Second: nullable,
                Container: {
                    type: "object",
                    properties: {
                        items: { type: "array", items: { $ref: "#/definitions/First" } },
                        itemsItem: { $ref: "#/definitions/Second" },
                    },
                },
            },
        } as ApiSchema;

        expect(() => generateApiTypesCode(schema)).toThrow(/ContainerItemsItem.*First.*Second/);
    });

    it.each(["anyOf", "oneOf"] as const)(
        "keeps required phase results typed through %s references",
        (keyword) => {
            const code = generateApiTypesCode({
                definitions: {
                    Operation: {
                        [keyword]: ["prepared", "cancelled"].map((phase) => ({
                            type: "object",
                            required: ["phase", "operationId"],
                            properties: {
                                phase: { type: "string", const: phase },
                                operationId: { type: "string" },
                            },
                        })),
                    },
                    Result: {
                        type: "object",
                        required: ["operation"],
                        properties: { operation: { $ref: "#/definitions/Operation" } },
                    },
                },
            } as ApiSchema);

            expect(code).toContain("pub operation: Operation,");
            expect(code).toContain(`#[serde(untagged)]
pub enum Operation {
    Prepared(OperationPrepared),
    Cancelled(OperationCancelled),
}`);
            expect(code).toContain("pub phase: OperationPreparedPhase,");
            expect(code).toContain("pub phase: OperationCancelledPhase,");
            expect(code).not.toContain("#[serde(other)]");
            expect(code).not.toContain("pub operation: serde_json::Value,");
        }
    );

    it.each(["anyOf", "oneOf", "type"] as const)(
        "retains nullability through %s object references and reference chains",
        (keyword) => {
            const payload: JSONSchema7 = {
                type: "object",
                required: ["tokenCount"],
                properties: { tokenCount: { type: "integer" } },
            };
            const nullable: JSONSchema7 =
                keyword === "type"
                    ? { ...payload, type: ["object", "null"] }
                    : { [keyword]: [payload, { type: "null" }] };
            const code = generateApiTypesCode({
                definitions: {
                    NullablePayload: nullable,
                    Alias: { $ref: "#/definitions/NullablePayload" },
                    TransitiveAlias: { $ref: "#/definitions/Alias" },
                    Container: {
                        type: "object",
                        required: ["direct", "transitive", "items"],
                        properties: {
                            direct: { $ref: "#/definitions/NullablePayload" },
                            transitive: { $ref: "#/definitions/TransitiveAlias" },
                            optional: { $ref: "#/definitions/NullablePayload" },
                            items: {
                                type: "array",
                                items: { $ref: "#/definitions/NullablePayload" },
                            },
                        },
                    },
                },
            } as ApiSchema);

            const directType = keyword === "type" ? "ContainerDirect" : "NullablePayload";
            const transitiveType = keyword === "type" ? "ContainerTransitive" : "TransitiveAlias";
            const optionalType = keyword === "type" ? "ContainerOptional" : "NullablePayload";
            const itemType = keyword === "type" ? "ContainerItemsItem" : "NullablePayload";
            expect(code).toContain(`pub direct: Option<${directType}>,`);
            expect(code).toContain(`pub transitive: Option<${transitiveType}>,`);
            expect(code).toContain(`pub optional: Option<${optionalType}>,`);
            expect(code).toContain(`pub items: Vec<Option<${itemType}>>,`);
            expect(code).not.toContain("Option<Option<");
            expect(code).toContain(`pub struct ${directType} {
    pub token_count: i64,
}`);
            expect(code).toContain(`pub struct ${transitiveType} {
    pub token_count: i64,
}`);
            expect(code).toContain(`pub struct Container {
    pub direct: Option<${directType}>,`);
            expect(code).toContain(`#[serde(skip_serializing_if = "Option::is_none")]
    pub optional: Option<${optionalType}>,`);
        }
    );

    it.each(["anyOf", "oneOf", "allOf"] as const)(
        "retains nullability when a referenced %s wrapper points to another nullable definition",
        (keyword) => {
            const code = generateApiTypesCode({
                definitions: {
                    NullablePayload: {
                        anyOf: [
                            {
                                type: "object",
                                required: ["value"],
                                properties: { value: { type: "string" } },
                            },
                            { type: "null" },
                        ],
                    },
                    Wrapper: { [keyword]: [{ $ref: "#/definitions/NullablePayload" }] },
                    NonNullableWrapper: {
                        allOf: [{ $ref: "#/definitions/NullablePayload" }, { type: "object" }],
                    },
                    Container: {
                        type: "object",
                        required: ["payload", "nonNullable"],
                        properties: {
                            payload: { $ref: "#/definitions/Wrapper" },
                            nonNullable: { $ref: "#/definitions/NonNullableWrapper" },
                        },
                    },
                },
            } as ApiSchema);

            expect(code).toContain("pub payload: Option<Wrapper>,");
            expect(code).toContain("pub non_nullable: NonNullableWrapper,");
        }
    );

    it.each(["anyOf", "oneOf"] as const)(
        "keeps null in a referenced multi-variant %s discriminated union",
        (keyword) => {
            const code = generateApiTypesCode({
                definitions: {
                    Outcome: {
                        title: "Outcome",
                        [keyword]: [
                            ...["ready", "pending"].map((kind) => ({
                                type: "object",
                                required: ["kind"],
                                properties: { kind: { type: "string", const: kind } },
                            })),
                            { type: "null" },
                        ],
                    },
                    Container: {
                        type: "object",
                        required: ["outcome"],
                        properties: { outcome: { $ref: "#/definitions/Outcome" } },
                    },
                },
            } as ApiSchema);

            expect(code).toContain("pub outcome: Option<Outcome>,");
            expect(code).toContain("pub enum Outcome {");
        }
    );

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
        expect(code).toContain("pub type RequestReview = Review;");
        expect(code).not.toContain("pub struct RequestReview");
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

describe("Rust x-legacy-parameters", () => {
    const render = (additions: 0 | 1 | 2, scope: "server" | "session" = "server") => {
        const { schema } = legacyRequestSchema(additions, scope);
        const types = generateApiTypesCode(schema);
        const rpc = generateRpcCode(schema);
        const block = (code: string, pattern: RegExp) => code.match(pattern)?.[0];
        return {
            types,
            rpc,
            request: block(types, /pub struct SamplePlanRequest \{[\s\S]*?\n\}/),
            constructor: block(types, /pub fn new\([^)]*\) -> Self/),
            plan: block(rpc, /pub async fn plan\([^)]*\)[^{]*/),
            options: block(rpc, /pub async fn plan_with_options\([^)]*\)[^{]*/),
        };
    };

    it("leaves unmarked requests unchanged", () => {
        const original = render(0);
        expect(original.types).not.toContain("SamplePlanOptions");
        expect(original.rpc).not.toContain("plan_with_options");
    });

    it("freezes the published struct and method and adds private-field options", () => {
        const original = render(0);
        const once = render(1);
        expect(once.request).toBe(original.request);
        expect(once.plan).toBe(original.plan);
        expect(once.types).toContain(
            "pub struct SamplePlanOptions {\n    #[serde(flatten)]\n    legacy: SamplePlanRequest,"
        );
        expect(once.types).toMatch(
            /#\[serde\(skip_serializing_if = "Option::is_none"\)\]\n    policy_session_id: Option<String>,/
        );
        expect(once.constructor).toBe(
            "pub fn new(contract: impl Into<String>, source: impl Into<String>) -> Self"
        );
        expect(once.types).toContain(
            "legacy: SamplePlanRequest { contract: contract.into(), source: source.into(), scope: None },"
        );
        expect(once.types).toContain(
            "pub fn scope(mut self, value: impl Into<String>) -> Self {\n        self.legacy.scope = Some(value.into());"
        );
        expect(once.types).toContain(
            "pub fn policy_session_id(mut self, value: impl Into<String>) -> Self {\n        self.policy_session_id = Some(value.into());"
        );
        expect(once.options).toContain("params: SamplePlanOptions");
        const wireCalls = once.rpc.match(/rpc_methods::SAMPLE_PLAN/g) ?? [];
        expect(wireCalls).toHaveLength(2);
        expect(once.types).not.toContain("non_exhaustive");
    });

    it("keeps both entry points unchanged across a second optional addition", () => {
        const once = render(1);
        const twice = render(2);
        expect(twice.request).toBe(once.request);
        expect(twice.constructor).toBe(once.constructor);
        expect(twice.plan).toBe(once.plan);
        expect(twice.options).toBe(once.options);
        expect(twice.types).toContain(
            "pub fn trace_id(mut self, value: impl Into<String>) -> Self {"
        );
        expect(twice.types).toContain("            trace_id: None,");
    });

    it("keeps the session id injected by session-scoped wrappers", () => {
        const once = render(1, "session");
        expect(once.request).toBe(render(0, "session").request);
        expect(once.request).not.toContain("session_id");
        const body = once.rpc.slice(once.rpc.indexOf("pub async fn plan_with_options"));
        expect(body).toMatch(
            /wire_params\["sessionId"\] = serde_json::Value::String\(self\.session\.id\(\)\.to_string\(\)\);/
        );
    });

    it("derives Default for options without required inputs", () => {
        const { schema, params } = legacyRequestSchema(1);
        params.required = [];
        const types = generateApiTypesCode(schema);
        expect(types).toContain("pub fn new() -> Self");
        expect(types).toContain(
            "impl Default for SamplePlanOptions {\n    fn default() -> Self {\n        Self::new()"
        );
        expect(render(1).types).not.toContain("impl Default for SamplePlanOptions");
    });

    it("rejects optional requests", () => {
        const { schema, params } = legacyRequestSchema(1);
        const method = (schema.server as Record<string, Record<string, { params: unknown }>>).sample
            .plan;
        method.params = { anyOf: [{ not: {} }, params] };
        expect(() => generateApiTypesCode(schema)).toThrow(
            "Invalid x-legacy-parameters for sample.plan: optional requests cannot declare legacy parameters"
        );
    });

    it("rejects metadata that omits a required input", () => {
        const { schema, params } = legacyRequestSchema(1);
        (params as Record<string, unknown>)["x-legacy-parameters"] = ["contract", "scope"];
        expect(() => generateApiTypesCode(schema)).toThrow(
            "Invalid x-legacy-parameters for sample.plan: required property source must be a legacy parameter"
        );
    });

    it("gives each titled const discriminator its own Rust type", () => {
        const code = generateApiTypesCode({
            definitions: {
                Review: {
                    anyOf: ["install", "uninstall"].map((action) => ({
                        type: "object",
                        required: ["action"],
                        properties: {
                            action: { type: "string", const: action, title: "ReviewAction" },
                        },
                    })),
                },
            },
        } as ApiSchema);

        expect(code).toMatch(
            /pub enum Review\w*InstallAction \{\n(?:.*\n)*?\s+#\[serde\(rename = "install"\)\]/
        );
        expect(code).toMatch(
            /pub enum Review\w*UninstallAction \{\n(?:.*\n)*?\s+#\[serde\(rename = "uninstall"\)\]/
        );
        expect(code).not.toContain("pub enum ReviewAction");
    });

    it("refuses to reuse a string enum name for a different value set", () => {
        expect(() =>
            generateApiTypesCode({
                definitions: {
                    Kind: { type: "string", enum: ["a", "b"] },
                    Owner: {
                        type: "object",
                        required: ["kind", "mode"],
                        properties: {
                            kind: { $ref: "#/definitions/Kind" },
                            mode: { type: "string", enum: ["c"], title: "Kind" },
                        },
                    },
                },
            } as ApiSchema)
        ).toThrow(/Rust string enum Kind is requested for different values/);
    });

    it("keeps every const discriminator of the committed API schema distinct in Rust", () => {
        const schema = JSON.parse(
            readFileSync(new URL("../../../../generated/api.schema.json", import.meta.url), "utf8")
        ) as ApiSchema;
        // Generation throws on any new collision; only the pinned released ones are reused.
        const code = generateApiTypesCode(schema);
        expect([...RELEASED_RUST_STRING_ENUM_COLLISIONS].sort()).toEqual([
            "AttachmentGitHubReferenceType",
            "PushAttachmentGitHubReferenceType",
        ]);

        const enumValues = new Map<string, string[]>();
        for (const match of code.matchAll(/^pub enum (\w+) \{\n([\s\S]*?)^\}/gm)) {
            enumValues.set(
                match[1],
                [...match[2].matchAll(/rename = "([^"]+)"/g)].map((m) => m[1])
            );
        }
        const unions: Array<{ owner: string; variants: unknown[] }> = [];
        const visit = (node: unknown, owner: string): void => {
            if (typeof node !== "object" || node === null) return;
            const record = node as Record<string, unknown>;
            for (const key of ["anyOf", "oneOf"] as const) {
                if (Array.isArray(record[key]))
                    unions.push({ owner, variants: record[key] as unknown[] });
            }
            for (const [key, value] of Object.entries(record)) visit(value, `${owner}/${key}`);
        };
        visit(schema, "api.schema.json");

        let checked = 0;
        for (const { owner, variants } of unions) {
            const discriminators = variants.map((variant) => {
                const properties = (variant as JSONSchema7).properties ?? {};
                return Object.entries(properties).filter(
                    ([name, prop]) =>
                        typeof prop === "object" &&
                        typeof prop.const === "string" &&
                        (variant as JSONSchema7).required?.includes(name) &&
                        !prop.$ref
                );
            });
            for (const [name] of discriminators[0] ?? []) {
                const values = discriminators.map(
                    (entries) => entries.find(([key]) => key === name)?.[1].const
                );
                if (
                    values.some((value) => typeof value !== "string") ||
                    new Set(values).size !== values.length
                )
                    continue;
                const titles = new Set(
                    discriminators.map(
                        (entries) =>
                            (entries.find(([key]) => key === name)?.[1] as JSONSchema7).title
                    )
                );
                if (titles.size !== 1 || [...titles][0] === undefined) continue;
                // Shared-title literals must each map to an enum accepting exactly that value.
                for (const value of values as string[]) {
                    const matching = [...enumValues].filter(
                        ([, accepted]) => accepted.length === 1 && accepted[0] === value
                    );
                    expect(matching.length, `${owner}.${name} = ${value}`).toBeGreaterThan(0);
                }
                checked += 1;
            }
        }
        expect(checked).toBeGreaterThan(0);
    });
});

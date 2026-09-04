import type { JSONSchema7 } from "json-schema";
import { describe, expect, it } from "vitest";

import { collectNestedDiscriminatedUnionTypeNames } from "../../java/scripts/codegen/java.ts";
import {
    analyseDiscriminatedUnion,
    analyseNestedClosedUnionResult,
    schemaDiscriminatorValueKey,
} from "../../scripts/codegen/schema-unions.ts";
import { createRpcResultProjectionBundle } from "../../scripts/codegen/typescript.ts";
import type { DefinitionCollections } from "../../scripts/codegen/utils.ts";

const definitions: Record<string, JSONSchema7> = {
    SyntheticResult: {
        anyOf: [
            { $ref: "#/definitions/SyntheticSucceeded" },
            { $ref: "#/definitions/SyntheticFailed" },
        ],
    },
    SyntheticSucceeded: {
        type: "object",
        additionalProperties: false,
        required: ["kind", "choices"],
        properties: {
            kind: { const: "succeeded" },
            choices: {
                type: "array",
                items: { $ref: "#/definitions/SyntheticChoice" },
            },
        },
    },
    SyntheticFailed: {
        type: "object",
        additionalProperties: false,
        required: ["kind", "message"],
        properties: {
            kind: { const: "failed" },
            message: { type: "string" },
        },
    },
    SyntheticChoice: {
        anyOf: [{ $ref: "#/definitions/SyntheticAlpha" }, { $ref: "#/definitions/SyntheticBeta" }],
    },
    SyntheticAlpha: {
        type: "object",
        additionalProperties: false,
        required: ["kind", "source"],
        properties: {
            kind: { const: "alpha" },
            source: { $ref: "#/definitions/SyntheticSource" },
        },
    },
    SyntheticBeta: {
        type: "object",
        additionalProperties: false,
        required: ["kind", "source"],
        properties: {
            kind: { const: "beta" },
            source: { $ref: "#/definitions/SyntheticSource" },
        },
    },
    SyntheticSource: {
        oneOf: [
            { $ref: "#/definitions/SyntheticInlineSource" },
            { $ref: "#/definitions/SyntheticUrlSource" },
        ],
    },
    SyntheticInlineSource: {
        type: "object",
        additionalProperties: false,
        required: ["kind"],
        properties: {
            kind: { const: "inline" },
        },
    },
    SyntheticUrlSource: {
        type: "object",
        additionalProperties: false,
        required: ["kind", "url"],
        properties: {
            kind: { const: "url" },
            url: { type: "string" },
        },
    },
};

const collections: DefinitionCollections = { definitions, $defs: {} };
const resolveVariant = (schema: JSONSchema7): JSONSchema7 | undefined => {
    const name = schema.$ref?.match(/^#\/definitions\/([^/]+)$/)?.[1];
    return name ? definitions[name] : schema;
};

describe("schema-driven discriminated union codegen", () => {
    it("classifies a closed synthetic union without relying on domain names", () => {
        const analysis = analyseDiscriminatedUnion(definitions.SyntheticChoice, resolveVariant);

        expect(analysis?.property).toBe("kind");
        expect(analysis?.unknownVariantPolicy).toBe("reject");
        expect(analysis?.mapping.map(({ value }) => value)).toEqual(["alpha", "beta"]);
    });

    it("preserves fallback only when a variant explicitly permits extra properties", () => {
        const openDefinitions = structuredClone(definitions);
        openDefinitions.SyntheticBeta.additionalProperties = true;
        const analysis = analyseDiscriminatedUnion(openDefinitions.SyntheticChoice, (schema) => {
            const name = schema.$ref?.match(/^#\/definitions\/([^/]+)$/)?.[1];
            return name ? openDefinitions[name] : schema;
        });

        expect(analysis?.unknownVariantPolicy).toBe("preserve");
    });

    it("builds nested runtime projections for an equivalent synthetic result", () => {
        const projection = createRpcResultProjectionBundle(
            { $ref: "#/definitions/SyntheticResult" },
            collections
        );

        expect(projection?.root).toEqual({
            kind: "ref",
            name: "SyntheticResult",
        });
        expect(projection?.definitions.SyntheticChoice).toMatchObject({
            kind: "union",
            discriminator: "kind",
            variants: {
                [schemaDiscriminatorValueKey("alpha")]: {
                    kind: "ref",
                    name: "SyntheticAlpha",
                },
                [schemaDiscriminatorValueKey("beta")]: {
                    kind: "ref",
                    name: "SyntheticBeta",
                },
            },
        });
        expect(projection?.definitions.SyntheticSource).toMatchObject({
            kind: "union",
            discriminator: "kind",
        });
    });

    it("promotes every nested synthetic union for Java generation", () => {
        expect(
            analyseNestedClosedUnionResult(
                { $ref: "#/definitions/SyntheticResult" },
                definitions
            )?.unionDefinitionNames
        ).toEqual(
            new Set(["SyntheticResult", "SyntheticChoice", "SyntheticSource"])
        );
        expect(
            collectNestedDiscriminatedUnionTypeNames(
                { $ref: "#/definitions/SyntheticResult" },
                definitions
            )
        ).toEqual(new Set(["SyntheticChoice", "SyntheticSource"]));
        expect(
            collectNestedDiscriminatedUnionTypeNames(
                { $ref: "#/definitions/SyntheticChoice" },
                definitions
            )
        ).toEqual(new Set(["SyntheticSource"]));
    });
});

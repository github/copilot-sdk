import type { JSONSchema7 } from "json-schema";
import { compile } from "json-schema-to-typescript";
import { describe, expect, it } from "vitest";

import {
    assertNoPublicInternalReferences,
    filterPublicSessionEventVariants,
    normalizeSchemaForTypeScript,
} from "../../scripts/codegen/typescript.ts";
import type { DefinitionCollections } from "../../scripts/codegen/utils.ts";

describe("typescript schema codegen", () => {
    it("emits JSDoc comments for described enum values", async () => {
        const schema: JSONSchema7 = {
            title: "SyntheticOptions",
            type: "object",
            additionalProperties: false,
            properties: {
                namedMode: {
                    title: "SyntheticMode",
                    type: "string",
                    enum: ["alpha", "beta"],
                    description: "Synthetic mode.",
                    "x-enumDescriptions": {
                        alpha: "Use alpha mode.",
                    },
                },
                inlineMode: {
                    type: "string",
                    enum: ["direct", "indirect"],
                    description: "Inline mode.",
                    "x-enumDescriptions": {
                        direct: "Use a direct value.",
                    },
                },
            },
            required: ["namedMode", "inlineMode"],
        };

        const code = await compile(normalizeSchemaForTypeScript(schema), "SyntheticOptions", {
            bannerComment: "",
            style: { semi: true, singleQuote: false },
            additionalProperties: false,
        });

        expect(code).toContain(
            'export type SyntheticMode = /** Use alpha mode. */ "alpha" | "beta";'
        );
        expect(code).toContain('inlineMode: /** Use a direct value. */ "direct" | "indirect";');
    });
});

describe("filterPublicSessionEventVariants", () => {
    const makeCollections = (defs: Record<string, JSONSchema7>): DefinitionCollections => ({
        definitions: defs,
        $defs: {},
    });

    it("keeps public union arms", () => {
        const defs = {
            PublicEvent: { type: "object" as const, properties: { type: { const: "pub" } } },
        };
        const variants: JSONSchema7[] = [{ $ref: "#/definitions/PublicEvent" }];
        const { publicVariants, excludedDefinitionNames } = filterPublicSessionEventVariants(
            variants,
            makeCollections(defs)
        );
        expect(publicVariants).toHaveLength(1);
        expect(excludedDefinitionNames.size).toBe(0);
    });

    it("excludes arms whose arm object is marked visibility:internal", () => {
        const defs = {
            InternalEvent: {
                type: "object" as const,
                visibility: "internal",
                properties: { type: { const: "internal.evt" } },
            } as JSONSchema7 & { visibility: string },
        };
        const variants: JSONSchema7[] = [
            { $ref: "#/definitions/InternalEvent", visibility: "internal" } as JSONSchema7 & {
                visibility: string;
            },
        ];
        const { publicVariants, excludedDefinitionNames } = filterPublicSessionEventVariants(
            variants,
            makeCollections(defs)
        );
        expect(publicVariants).toHaveLength(0);
        expect(excludedDefinitionNames.has("InternalEvent")).toBe(true);
    });

    it("excludes arms whose resolved definition is marked visibility:internal", () => {
        const defs = {
            InternalEvent: {
                type: "object" as const,
                visibility: "internal",
                properties: { type: { const: "internal.evt" } },
            } as JSONSchema7 & { visibility: string },
        };
        // arm object itself is NOT marked, but the resolved definition is
        const variants: JSONSchema7[] = [{ $ref: "#/definitions/InternalEvent" }];
        const { publicVariants, excludedDefinitionNames } = filterPublicSessionEventVariants(
            variants,
            makeCollections(defs)
        );
        expect(publicVariants).toHaveLength(0);
        expect(excludedDefinitionNames.has("InternalEvent")).toBe(true);
    });

    it("excludes arms whose internal data sub-property is the only internal marker (legacy pattern)", () => {
        // Event types that carry a `data: InternalData` field — the `data` property is what is
        // internal, not the event wrapper type itself.
        const defs = {
            InternalData: {
                type: "object" as const,
                visibility: "internal",
            } as JSONSchema7 & { visibility: string },
            WrapperEvent: {
                type: "object" as const,
                properties: {
                    type: { const: "wrapper.evt" },
                    data: { $ref: "#/definitions/InternalData" },
                },
            },
        };
        const variants: JSONSchema7[] = [{ $ref: "#/definitions/WrapperEvent" }];
        const { publicVariants, excludedDefinitionNames } = filterPublicSessionEventVariants(
            variants,
            makeCollections(defs)
        );
        expect(publicVariants).toHaveLength(0);
        expect(excludedDefinitionNames.has("WrapperEvent")).toBe(true);
        expect(excludedDefinitionNames.has("InternalData")).toBe(true);
    });
});

describe("assertNoPublicInternalReferences", () => {
    it("passes when all declarations are public and do not reference internal types", () => {
        const ts = `
export interface Foo {
  bar: string;
}
export type Bar = "a" | "b";
`;
        expect(() => assertNoPublicInternalReferences(ts, new Set(["Hidden"]))).not.toThrow();
    });

    it("passes when the only reference is from an @internal-tagged declaration", () => {
        const ts = `
/** @internal */
export interface Hidden {
  x: number;
}
/** @internal */
export interface AlsoInternal {
  h: Hidden;
}
export interface Public {
  y: string;
}
`;
        expect(() => assertNoPublicInternalReferences(ts, new Set(["Hidden"]))).not.toThrow();
    });

    it("passes when the reference is inside an @internal-tagged member of a public type", () => {
        const ts = `
/** @internal */
export interface Hidden {
  x: number;
}
export interface Public {
  /**
   * Some field.
   * @internal
   */
  secret?: Hidden;
  visible: string;
}
`;
        expect(() => assertNoPublicInternalReferences(ts, new Set(["Hidden"]))).not.toThrow();
    });

    it("throws when a public declaration references an internal type directly", () => {
        const ts = `
/** @internal */
export interface Hidden {
  x: number;
}
export type Event = PublicEvent | Hidden;
`;
        expect(() => assertNoPublicInternalReferences(ts, new Set(["Hidden"]))).toThrow(
            /Event \(public\) references internal type Hidden/
        );
    });

    it("throws when a public interface member references an internal type", () => {
        const ts = `
/** @internal */
export interface Hidden {
  x: number;
}
export interface Public {
  value: Hidden;
}
`;
        expect(() => assertNoPublicInternalReferences(ts, new Set(["Hidden"]))).toThrow(
            /Public \(public\) references internal type Hidden/
        );
    });

    it("does not count JSDoc comment text as a code reference", () => {
        // The auto-generated JSDoc says 'via the definition "Hidden"' but that is not a
        // real TypeScript type reference — it must not trigger the validator.
        const ts = `
/** @internal */
export interface Hidden {
  x: number;
}
export interface Preceding {
  y: string;
}
/**
 * This interface was referenced by something.
 * via the definition "Hidden".
 */
export interface Following {
  z: string;
}
`;
        expect(() => assertNoPublicInternalReferences(ts, new Set(["Hidden"]))).not.toThrow();
    });

    it("does not count inline object-shaped @internal members as public references", () => {
        const ts = `
/** @internal */
export interface Hidden {
  x: number;
}
export interface Public {
  /**
   * Some field.
   * @internal
   */
  secret?: {
    [k: string]: Hidden | undefined;
  };
  visible: string;
}
`;
        expect(() => assertNoPublicInternalReferences(ts, new Set(["Hidden"]))).not.toThrow();
    });

    it("does not count function body references as public type references", () => {
        const ts = `
/** @internal */
export interface Hidden {
  x: number;
}
/** @internal */
export function doInternal(connection: unknown): void {
  connection.onRequest("x", async (params: Hidden) => { return params; });
}
export function doPublic(connection: unknown): void {
  connection.onRequest("x", async (params: Hidden) => { return params; });
}
`;
        // function body references are stripped — only signature matters
        expect(() => assertNoPublicInternalReferences(ts, new Set(["Hidden"]))).not.toThrow();
    });
});

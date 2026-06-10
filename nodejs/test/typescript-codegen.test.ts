import type { JSONSchema7 } from "json-schema";
import { compile } from "json-schema-to-typescript";
import { describe, expect, it } from "vitest";

import { normalizeSchemaForTypeScript } from "../../scripts/codegen/typescript.ts";

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

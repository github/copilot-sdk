import { expect, it } from "vitest";

import { generateRpcCode } from "../../scripts/codegen/csharp.ts";

function schemaWithProperties(properties: Record<string, unknown>) {
    return {
        server: {
            test: {
                rpcMethod: "test",
                params: null,
                result: {
                    type: "object" as const,
                    title: "TestResult",
                    properties,
                },
            },
        },
    };
}

it("omits only untyped internal properties from C# RPC types", () => {
    const code = generateRpcCode(
        schemaWithProperties({
            publicValue: { type: "string" },
            typedInternal: { type: "string", visibility: "internal" },
            untypedInternal: { visibility: "internal" },
        })
    );

    expect(code).toContain("public string? PublicValue");
    expect(code).toContain("internal string? TypedInternal");
    expect(code).not.toContain("UntypedInternal");
    expect(() =>
        generateRpcCode(
            schemaWithProperties({
                untypedPublic: {},
            })
        )
    ).toThrow(/cannot map schema to an idiomatic C# type/);
});

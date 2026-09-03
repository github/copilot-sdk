import { describe, expect, it } from "vitest";

import { emitClientSessionApiRegistration as emitGoClientSessionApiRegistration } from "../../scripts/codegen/go.ts";
import { emitClientSessionApiRegistration as emitPythonClientSessionApiRegistration } from "../../scripts/codegen/python.ts";
import { emitClientSessionApiRegistration as emitTypeScriptClientSessionApiRegistration } from "../../scripts/codegen/typescript.ts";

const clientSessionSchema: Record<string, unknown> = {
    mixed: {
        visible: {
            rpcMethod: "mixed.visible",
            params: {
                type: "object",
                title: "VisibleRequest",
                properties: {
                    sessionId: { type: "string" },
                },
                required: ["sessionId"],
            },
            result: {
                type: "object",
                title: "VisibleResult",
                properties: {},
            },
        },
        secret: {
            rpcMethod: "mixed.secret",
            visibility: "internal",
            params: {
                $ref: "#/definitions/InternalRequest",
            },
            result: {
                $ref: "#/definitions/InternalResult",
            },
        },
    },
    internalOnly: {
        hidden: {
            rpcMethod: "internalOnly.hidden",
            visibility: "internal",
            params: {
                $ref: "#/definitions/InternalRequest",
            },
            result: {
                $ref: "#/definitions/InternalResult",
            },
        },
    },
};

function expectOnlyPublicClientSessionHandlers(code: string): void {
    expect(code).toContain("mixed.visible");
    expect(code).not.toContain("mixed.secret");
    expect(code).not.toContain("internalOnly.hidden");
    expect(code).not.toContain("InternalRequest");
    expect(code).not.toContain("InternalResult");
}

describe("client-session API codegen", () => {
    it("excludes internal methods from TypeScript handlers", () => {
        const code = emitTypeScriptClientSessionApiRegistration(clientSessionSchema).join("\n");

        expectOnlyPublicClientSessionHandlers(code);
        expect(code).not.toContain("InternalOnlyHandler");
    });

    it("excludes internal methods from Go handlers", () => {
        const lines: string[] = [];
        emitGoClientSessionApiRegistration(lines, clientSessionSchema, (name) => name, new Map());
        const code = lines.join("\n");

        expectOnlyPublicClientSessionHandlers(code);
        expect(code).not.toContain("InternalOnlyHandler");
    });

    it("excludes internal methods from Python handlers", () => {
        const lines: string[] = [];
        emitPythonClientSessionApiRegistration(lines, clientSessionSchema, (name) => name);
        const code = lines.join("\n");

        expectOnlyPublicClientSessionHandlers(code);
        expect(code).not.toContain("InternalOnlyHandler");
    });
});

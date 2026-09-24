import { describe, expect, it } from "vitest";

import { emitClientSessionApiRegistration as emitGoClientSessionApiRegistration } from "../../scripts/codegen/go.ts";
import { emitClientSessionApiRegistration as emitPythonClientSessionApiRegistration } from "../../scripts/codegen/python.ts";
import {
    emitClientGlobalApiRegistration as emitTypeScriptClientGlobalApiRegistration,
    emitClientSessionApiRegistration as emitTypeScriptClientSessionApiRegistration,
} from "../../scripts/codegen/typescript.ts";

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

const allInternalClientSessionSchema: Record<string, unknown> = {
    internalOnly: clientSessionSchema.internalOnly,
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
        const allInternalCode = emitTypeScriptClientSessionApiRegistration(
            allInternalClientSessionSchema
        ).join("\n");

        expectOnlyPublicClientSessionHandlers(code);
        expect(code).not.toContain("InternalOnlyHandler");
        expect(allInternalCode).toContain("export interface ClientSessionApiHandlers {");
        expect(allInternalCode).toContain("export function registerClientSessionApiHandlers(");
        expect(allInternalCode).not.toContain("InternalOnlyHandler");
    });

    describe("client-global API codegen", () => {
        it("preserves request cancellation without changing notification handlers", () => {
            const code = emitTypeScriptClientGlobalApiRegistration({
                callbacks: {
                    withParams: {
                        rpcMethod: "callbacks.withParams",
                        params: {
                            type: "object",
                            title: "CallbackRequest",
                            properties: { id: { type: "string" } },
                        },
                        result: { type: "object", title: "CallbackResult", properties: {} },
                    },
                    withoutParams: {
                        rpcMethod: "callbacks.withoutParams",
                        result: { type: "object", title: "CallbackResult", properties: {} },
                    },
                    notified: {
                        rpcMethod: "callbacks.notified",
                        notification: true,
                        params: {
                            type: "object",
                            title: "CallbackRequest",
                            properties: { id: { type: "string" } },
                        },
                    },
                },
            }).join("\n");

            expect(code).toContain(
                "withParams(params: CallbackRequest, token?: CancellationToken)"
            );
            expect(code).toContain("withoutParams(token?: CancellationToken)");
            expect(code).toContain("return handler.withParams(params, token)");
            expect(code).toContain("return handler.withoutParams(token)");
            expect(code).toContain("notified(params: CallbackRequest): Promise<void>");
            expect(code).toContain("await handler.notified(params)");
            expect(code).not.toContain("handler.notified(params, token)");
        });

        it("keeps internal methods out of global registration", () => {
            const code = emitTypeScriptClientGlobalApiRegistration(clientSessionSchema).join("\n");
            expectOnlyPublicClientSessionHandlers(code);
            expect(code).not.toContain("InternalOnlyHandler");
        });
    });

    it("excludes internal methods from Go handlers", () => {
        const lines: string[] = [];
        emitGoClientSessionApiRegistration(lines, clientSessionSchema, (name) => name, new Map());
        const code = lines.join("\n");
        const allInternalLines: string[] = [];
        emitGoClientSessionApiRegistration(
            allInternalLines,
            allInternalClientSessionSchema,
            (name) => name,
            new Map()
        );
        const allInternalCode = allInternalLines.join("\n");

        expectOnlyPublicClientSessionHandlers(code);
        expect(code).not.toContain("InternalOnlyHandler");
        expect(allInternalCode).toContain("type ClientSessionAPIHandlers struct {");
        expect(allInternalCode).toContain("func RegisterClientSessionAPIHandlers(");
        expect(allInternalCode).not.toContain("InternalOnlyHandler");
        expect(allInternalCode).not.toContain("clientSessionHandlerError");
    });

    it("excludes internal methods from Python handlers", () => {
        const lines: string[] = [];
        emitPythonClientSessionApiRegistration(lines, clientSessionSchema, (name) => name);
        const code = lines.join("\n");
        const allInternalLines: string[] = [];
        emitPythonClientSessionApiRegistration(
            allInternalLines,
            allInternalClientSessionSchema,
            (name) => name
        );
        const allInternalCode = allInternalLines.join("\n");

        expectOnlyPublicClientSessionHandlers(code);
        expect(code).not.toContain("InternalOnlyHandler");
        expect(allInternalCode).toContain("class ClientSessionApiHandlers:");
        expect(allInternalCode).toContain("def register_client_session_api_handlers(");
        expect(allInternalCode).not.toContain("InternalOnlyHandler");
    });
});

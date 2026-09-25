import { PassThrough } from "node:stream";
import { runInNewContext } from "node:vm";
import { ModuleKind, transpileModule } from "typescript";
import { describe, expect, it, onTestFinished } from "vitest";
import {
    CancellationTokenSource,
    createMessageConnection,
    StreamMessageReader,
    StreamMessageWriter,
    type CancellationToken,
    type MessageConnection,
} from "vscode-jsonrpc/node.js";

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
        it.each([true, false])(
            "forwards real framed cancellation through emitted registration (params: %s)",
            async (hasParams) => {
                const code = emitTypeScriptClientGlobalApiRegistration({
                    callbacks: {
                        review: {
                            rpcMethod: "callbacks.review",
                            ...(hasParams
                                ? {
                                      params: {
                                          type: "object",
                                          title: "ReviewRequest",
                                          properties: { operationId: { type: "string" } },
                                          required: ["operationId"],
                                      },
                                  }
                                : {}),
                            result: { type: "object", title: "ReviewResult" },
                        },
                    },
                }).join("\n");
                const generated: {
                    registerClientGlobalApiHandlers?: (
                        connection: MessageConnection,
                        handlers: Record<string, unknown>
                    ) => void;
                } = {};
                runInNewContext(
                    transpileModule(code, { compilerOptions: { module: ModuleKind.CommonJS } })
                        .outputText,
                    { exports: generated }
                );
                if (!generated.registerClientGlobalApiHandlers) {
                    throw new Error("Generated global registration is missing");
                }

                const inbound = new PassThrough();
                const outbound = new PassThrough();
                const client = createMessageConnection(
                    new StreamMessageReader(inbound),
                    new StreamMessageWriter(outbound)
                );
                const server = createMessageConnection(
                    new StreamMessageReader(outbound),
                    new StreamMessageWriter(inbound)
                );
                const cancellation = new CancellationTokenSource();
                onTestFinished(() => {
                    cancellation.cancel();
                    cancellation.dispose();
                    client.dispose();
                    server.dispose();
                    inbound.destroy();
                    outbound.destroy();
                });
                let received!: () => void;
                const entered = new Promise<void>((resolve) => {
                    received = resolve;
                });
                let observed: CancellationToken | undefined;
                const review = async (token?: CancellationToken) => {
                    observed = token;
                    received();
                    if (!token) throw new Error("Missing original request cancellation");
                    if (!token.isCancellationRequested) {
                        await new Promise<void>((resolve) => {
                            const subscription = token.onCancellationRequested(() => {
                                subscription.dispose();
                                resolve();
                            });
                        });
                    }
                    return { decision: "cancel" };
                };
                generated.registerClientGlobalApiHandlers(client, {
                    callbacks: {
                        review: hasParams
                            ? (_params: unknown, token?: CancellationToken) => review(token)
                            : review,
                    },
                });
                client.listen();
                server.listen();
                const response = hasParams
                    ? server.sendRequest(
                          "callbacks.review",
                          { operationId: "original" },
                          cancellation.token
                      )
                    : server.sendRequest("callbacks.review", cancellation.token);
                const result = response.then(
                    (value: unknown) => ({ value }),
                    (error: unknown) => ({ error })
                );
                await entered;
                const initiallyCancelled = observed?.isCancellationRequested;
                cancellation.cancel();
                expect(await result).toEqual({ value: { decision: "cancel" } });
                expect(initiallyCancelled).toBe(false);
                expect(observed?.isCancellationRequested).toBe(true);
            }
        );

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

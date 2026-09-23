/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { approveAll, CanvasError, createCanvas } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

describe("Canvas RPC", async () => {
    const openCalls: Array<{ canvasId: string; instanceId: string; input?: unknown }> = [];
    const closeCalls: Array<{ canvasId: string; instanceId: string }> = [];
    const actionCalls: Array<{
        canvasId: string;
        instanceId: string;
        actionName: string;
        input?: unknown;
    }> = [];

    const counter = createCanvas({
        id: "counter",
        displayName: "Counter",
        description: "A simple counter canvas for e2e testing",
        inputSchema: {
            type: "object",
            properties: { startValue: { type: "number" } },
        },
        actions: [
            {
                name: "increment",
                description: "Increment the counter",
                inputSchema: {
                    type: "object",
                    properties: { amount: { type: "number" } },
                },
                handler: (ctx) => {
                    actionCalls.push({
                        canvasId: ctx.canvasId,
                        instanceId: ctx.instanceId,
                        actionName: ctx.actionName,
                        input: ctx.input,
                    });
                    return { newValue: 42 };
                },
            },
        ],
        open: (ctx) => {
            openCalls.push({
                canvasId: ctx.canvasId,
                instanceId: ctx.instanceId,
                input: ctx.input,
            });
            return {
                url: "https://example.test/counter",
                title: "Counter Canvas",
                status: "ready",
            };
        },
        onClose: (ctx) => {
            closeCalls.push({
                canvasId: ctx.canvasId,
                instanceId: ctx.instanceId,
            });
        },
    });

    const { copilotClient: client } = await createSdkTestContext();

    it("discovers declared canvases via session.canvas.list", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            canvases: [counter],
        });

        const result = await session.rpc.canvas.list();
        expect(result.canvases).toHaveLength(1);
        expect(result.canvases[0]).toMatchObject({
            canvasId: "counter",
            displayName: "Counter",
            description: "A simple counter canvas for e2e testing",
        });

        await session.disconnect();
    });

    it("opens a canvas instance via session.canvas.open round-trip", async () => {
        openCalls.length = 0;

        const session = await client.createSession({
            onPermissionRequest: approveAll,
            canvases: [counter],
        });

        const result = await session.rpc.canvas.open({
            canvasId: "counter",
            instanceId: "counter-1",
            input: { startValue: 10 },
        });

        expect(result.url).toBe("https://example.test/counter");
        expect(result.title).toBe("Counter Canvas");
        expect(openCalls).toHaveLength(1);
        expect(openCalls[0]).toMatchObject({
            canvasId: "counter",
            instanceId: "counter-1",
            input: { startValue: 10 },
        });

        // Verify it appears in the open list
        const openList = await session.rpc.canvas.listOpen();
        expect(openList.openCanvases).toHaveLength(1);
        expect(openList.openCanvases[0]).toMatchObject({
            canvasId: "counter",
            instanceId: "counter-1",
        });

        await session.disconnect();
    });

    it("invokes an action on an open canvas instance", async () => {
        openCalls.length = 0;
        actionCalls.length = 0;

        const session = await client.createSession({
            onPermissionRequest: approveAll,
            canvases: [counter],
        });

        await session.rpc.canvas.open({
            canvasId: "counter",
            instanceId: "counter-2",
            input: {},
        });

        const result = await session.rpc.canvas.action.invoke({
            instanceId: "counter-2",
            actionName: "increment",
            input: { amount: 5 },
        });

        expect(result.result).toEqual({ newValue: 42 });
        expect(actionCalls).toHaveLength(1);
        expect(actionCalls[0]).toMatchObject({
            canvasId: "counter",
            instanceId: "counter-2",
            actionName: "increment",
            input: { amount: 5 },
        });

        await session.disconnect();
    });

    it("closes an open canvas instance via session.canvas.close", async () => {
        openCalls.length = 0;
        closeCalls.length = 0;

        const session = await client.createSession({
            onPermissionRequest: approveAll,
            canvases: [counter],
        });

        await session.rpc.canvas.open({
            canvasId: "counter",
            instanceId: "counter-3",
            input: {},
        });
        expect(closeCalls).toHaveLength(0);

        await session.rpc.canvas.close({ instanceId: "counter-3" });
        expect(closeCalls).toHaveLength(1);
        expect(closeCalls[0]).toMatchObject({
            canvasId: "counter",
            instanceId: "counter-3",
        });

        // Verify it's no longer in the open list
        const openList = await session.rpc.canvas.listOpen();
        expect(openList.openCanvases).toHaveLength(0);

        await session.disconnect();
    });

    it.each(["open", "action", "close"] as const)(
        "preserves structured canvas errors from %s handlers",
        async (operation) => {
            const errorCanvas = createCanvas({
                id: `error-${operation}`,
                displayName: `Error ${operation}`,
                description: `Throws a structured error from ${operation}.`,
                actions: [
                    {
                        name: "fail",
                        handler: () => {
                            if (operation === "action") {
                                throw new CanvasError(
                                    `scenario_canvas_${operation}`,
                                    `scenario ${operation} failed`
                                );
                            }
                            return null;
                        },
                    },
                ],
                open: () => {
                    if (operation === "open") {
                        throw new CanvasError(
                            `scenario_canvas_${operation}`,
                            `scenario ${operation} failed`
                        );
                    }
                    return { url: "https://example.test/error-canvas" };
                },
                onClose: () => {
                    if (operation === "close") {
                        throw new CanvasError(
                            `scenario_canvas_${operation}`,
                            `scenario ${operation} failed`
                        );
                    }
                },
            });
            const session = await client.createSession({
                onPermissionRequest: approveAll,
                canvases: [errorCanvas],
            });

            try {
                const open = () =>
                    session.rpc.canvas.open({
                        canvasId: `error-${operation}`,
                        instanceId: `error-${operation}-1`,
                    });
                if (operation === "open") {
                    await expect(open()).rejects.toSatisfy((error: unknown) => {
                        expect(error).toMatchObject({ code: -32603 });
                        expect(String(error)).toContain("scenario open failed");
                        return true;
                    });
                } else {
                    await open();
                }

                const action = () =>
                    session.rpc.canvas.action.invoke({
                        instanceId: `error-${operation}-1`,
                        actionName: "fail",
                    });
                const close = () =>
                    session.rpc.canvas.close({ instanceId: `error-${operation}-1` });
                if (operation === "action") {
                    await expect(action()).rejects.toSatisfy((error: unknown) => {
                        expect(error).toMatchObject({ code: -32603 });
                        expect(String(error)).toContain("scenario action failed");
                        return true;
                    });
                } else if (operation === "close") {
                    await expect(close()).resolves.toBeNull();
                }

                const callback =
                    operation === "open"
                        ? session.clientSessionApis.canvas!.open({
                              sessionId: session.sessionId,
                              extensionId: "typescript-sdk-tests",
                              canvasId: `error-${operation}`,
                              instanceId: `error-${operation}-callback`,
                          })
                        : operation === "action"
                          ? session.clientSessionApis.canvas!.invoke({
                                sessionId: session.sessionId,
                                extensionId: "typescript-sdk-tests",
                                canvasId: `error-${operation}`,
                                instanceId: `error-${operation}-1`,
                                actionName: "fail",
                            })
                          : session.clientSessionApis.canvas!.close({
                                sessionId: session.sessionId,
                                extensionId: "typescript-sdk-tests",
                                canvasId: `error-${operation}`,
                                instanceId: `error-${operation}-1`,
                            });
                await expect(callback).rejects.toMatchObject({
                    code: -32603,
                    message: `scenario ${operation} failed`,
                    data: {
                        code: `scenario_canvas_${operation}`,
                        message: `scenario ${operation} failed`,
                    },
                });
            } finally {
                await session.disconnect();
            }
        }
    );
});

/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { approveAll, createCanvas } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext";

// E2E coverage for the canvas SDK ↔ runtime loop. The host-side
// `session.rpc.canvas.{open,close,invokeAction}` RPCs drive the runtime to
// dispatch `canvas.open` / `canvas.close` / `canvas.action.invoke` back to the
// declaring provider (us). These tests do not involve CAPI, so their
// snapshots are empty (`conversations: []`).
describe("Canvas E2E", async () => {
    const { copilotClient: client } = await createSdkTestContext();

    function makeCounter(record: {
        open?: { instanceId: string; canvasId: string; input?: unknown }[];
        close?: { instanceId: string; canvasId: string }[];
        action?: { actionName: string; instanceId: string; input?: unknown }[];
    }) {
        return createCanvas({
            id: "counter",
            displayName: "Counter",
            description: "A test counter canvas",
            actions: [
                {
                    name: "increment",
                    description: "Increment the counter",
                    handler: ({ actionName, instanceId, input }) => {
                        record.action?.push({ actionName, instanceId, input });
                        return { ok: true, actionName, input };
                    },
                },
            ],
            open: ({ instanceId, canvasId, input }) => {
                record.open?.push({ instanceId, canvasId, input });
                return { url: `https://example.test/${instanceId}` };
            },
            onClose: ({ instanceId, canvasId }) => {
                record.close?.push({ instanceId, canvasId });
            },
        });
    }

    it("dispatches canvas.open to the provider handler", async () => {
        const opens: { instanceId: string; canvasId: string; input?: unknown }[] = [];
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            canvases: [makeCounter({ open: opens })],
            requestCanvasRenderer: true,
            extensionInfo: { source: "github-app", name: "counter-provider" },
        });

        try {
            const result = await session.rpc.canvas.open({
                canvasId: "counter",
                instanceId: "counter-1",
                input: { seed: 7 },
            });

            expect(opens).toEqual([
                { instanceId: "counter-1", canvasId: "counter", input: { seed: 7 } },
            ]);
            expect(result).toMatchObject({
                instanceId: "counter-1",
                canvasId: "counter",
                url: "https://example.test/counter-1",
                availability: "ready",
            });
        } finally {
            await session.disconnect();
        }
    });

    it("dispatches canvas.action.invoke to the per-action handler", async () => {
        const actions: { actionName: string; instanceId: string; input?: unknown }[] = [];
        const opens: { instanceId: string; canvasId: string; input?: unknown }[] = [];
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            canvases: [makeCounter({ open: opens, action: actions })],
            requestCanvasRenderer: true,
            extensionInfo: { source: "github-app", name: "counter-provider" },
        });

        try {
            await session.rpc.canvas.open({ canvasId: "counter", instanceId: "counter-2" });

            const result = await session.rpc.canvas.invokeAction({
                canvasId: "counter",
                instanceId: "counter-2",
                actionName: "increment",
                input: { amount: 3 },
            });

            expect(actions).toEqual([
                {
                    actionName: "increment",
                    instanceId: "counter-2",
                    input: { amount: 3 },
                },
            ]);
            expect(result).toEqual({
                result: { ok: true, actionName: "increment", input: { amount: 3 } },
            });
        } finally {
            await session.disconnect();
        }
    });

    it("dispatches canvas.close to the provider onClose handler", async () => {
        const closes: { instanceId: string; canvasId: string }[] = [];
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            canvases: [makeCounter({ close: closes })],
            requestCanvasRenderer: true,
            extensionInfo: { source: "github-app", name: "counter-provider" },
        });

        try {
            await session.rpc.canvas.open({ canvasId: "counter", instanceId: "counter-3" });
            await session.rpc.canvas.close({ canvasId: "counter", instanceId: "counter-3" });

            // onClose is fire-and-forget on the runtime side; allow a microtask flush.
            await new Promise((r) => setTimeout(r, 50));

            expect(closes).toEqual([{ instanceId: "counter-3", canvasId: "counter" }]);
        } finally {
            await session.disconnect();
        }
    });

    it("rejects invokeAction for an action the canvas did not declare", async () => {
        // The Node `createCanvas` API requires every declared action to ship
        // with a `handler`, so the `canvas_action_no_handler` SDK-internal
        // error is unreachable via the runtime path here — it's covered by
        // unit tests in `client.test.ts`. The runtime, however, pre-validates
        // action names against the declaration before dispatching, and that
        // user-visible rejection is what we exercise end-to-end.
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            canvases: [makeCounter({})],
            requestCanvasRenderer: true,
            extensionInfo: { source: "github-app", name: "counter-provider" },
        });

        try {
            await session.rpc.canvas.open({ canvasId: "counter", instanceId: "counter-4" });

            await expect(
                session.rpc.canvas.invokeAction({
                    canvasId: "counter",
                    instanceId: "counter-4",
                    actionName: "ghost",
                    input: {},
                })
            ).rejects.toThrow(/Unknown action "ghost"/);
        } finally {
            await session.disconnect();
        }
    });

    it("seeds openCanvases on resume from the runtime resume response", async () => {
        // Open a canvas in session A, then resume into a fresh session view
        // and assert the resumed view's openCanvases() reflects the live
        // instance reported by the runtime.
        const sessionA = await client.createSession({
            onPermissionRequest: approveAll,
            canvases: [makeCounter({})],
            requestCanvasRenderer: true,
            extensionInfo: { source: "github-app", name: "counter-provider" },
        });

        try {
            await sessionA.rpc.canvas.open({
                canvasId: "counter",
                instanceId: "counter-resume",
                input: { initial: true },
            });

            const resumed = await client.resumeSession(sessionA.sessionId, {
                onPermissionRequest: approveAll,
                canvases: [makeCounter({})],
                requestCanvasRenderer: true,
                extensionInfo: { source: "github-app", name: "counter-provider" },
            });

            try {
                const seeded = resumed.openCanvases;
                expect(seeded.length).toBeGreaterThan(0);
                const match = seeded.find((c) => c.instanceId === "counter-resume");
                expect(match).toBeDefined();
                expect(match?.canvasId).toBe("counter");
            } finally {
                await resumed.disconnect();
            }
        } finally {
            await sessionA.disconnect();
        }
    });
});

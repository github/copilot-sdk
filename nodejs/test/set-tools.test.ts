/* eslint-disable @typescript-eslint/no-explicit-any */
import { describe, expect, it, vi } from "vitest";
import { z } from "zod";
import { CopilotSession } from "../src/session.js";
import { defineTool } from "../src/types.js";

type Deferred = { resolve: (value?: unknown) => void; reject: (error: unknown) => void };

/**
 * A session whose `session.tools.set` requests stay pending until the test
 * settles them, and whose other requests succeed immediately.
 */
function sessionWithPendingToolsSet() {
    const pending: Deferred[] = [];
    const sendRequest = vi.fn((method: string) =>
        method === "session.tools.set"
            ? new Promise((resolve, reject) => pending.push({ resolve, reject }))
            : Promise.resolve(undefined)
    );
    const session = new CopilotSession("session-1", { sendRequest } as never);
    const toolsSetCalls = () =>
        sendRequest.mock.calls.filter(([method]) => method === "session.tools.set");
    return { session, sendRequest, pending, toolsSetCalls };
}

function labelTool(name: string, handler: () => string) {
    return defineTool(name, {
        description: `${name} tool`,
        parameters: z.object({}),
        handler,
    });
}

async function requestTool(session: CopilotSession, requestId: string, toolName: string) {
    (session as any)._handleBroadcastEvent({
        type: "external_tool.requested",
        data: {
            requestId,
            sessionId: "session-1",
            toolCallId: `call-${requestId}`,
            toolName,
            arguments: {},
        },
    });
    await new Promise((resolve) => setImmediate(resolve));
}

describe("CopilotSession.setTools", () => {
    it("sends the complete tool set and switches handlers once the runtime accepts", async () => {
        const { session, pending, toolsSetCalls } = sessionWithPendingToolsSet();
        const oldHandler = vi.fn(() => "old");
        const newHandler = vi.fn(() => "new");
        session.registerTools([labelTool("old_tool", oldHandler)]);

        const replacement = session.setTools([
            labelTool("new_tool", newHandler),
            defineTool("declared_tool", { description: "Serviced by another client" }),
        ]);
        await vi.waitFor(() => expect(toolsSetCalls()).toHaveLength(1));
        expect(toolsSetCalls()[0][1]).toEqual({
            sessionId: "session-1",
            tools: [
                expect.objectContaining({
                    name: "new_tool",
                    description: "new_tool tool",
                    parameters: expect.objectContaining({ type: "object" }),
                }),
                expect.objectContaining({
                    name: "declared_tool",
                    description: "Serviced by another client",
                }),
            ],
        });

        // Until the runtime accepts, the previous handlers stay in effect.
        await requestTool(session, "before-new", "new_tool");
        await requestTool(session, "before-old", "old_tool");
        expect(newHandler).not.toHaveBeenCalled();
        expect(oldHandler).toHaveBeenCalledTimes(1);

        pending[0].resolve({});
        await replacement;

        await requestTool(session, "after-new", "new_tool");
        await requestTool(session, "after-old", "old_tool");
        await requestTool(session, "after-declared", "declared_tool");
        expect(newHandler).toHaveBeenCalledTimes(1);
        expect(oldHandler).toHaveBeenCalledTimes(1);
    });

    it("keeps the previous handlers when the runtime rejects the replacement", async () => {
        const { session, pending } = sessionWithPendingToolsSet();
        const oldHandler = vi.fn(() => "old");
        const newHandler = vi.fn(() => "new");
        session.registerTools([labelTool("old_tool", oldHandler)]);

        const replacement = session.setTools([labelTool("new_tool", newHandler)]);
        await vi.waitFor(() => expect(pending).toHaveLength(1));
        pending[0].reject(new Error("External tool name clash: new_tool"));
        await expect(replacement).rejects.toThrow("External tool name clash");

        await requestTool(session, "rejected-new", "new_tool");
        await requestTool(session, "kept-old", "old_tool");
        expect(newHandler).not.toHaveBeenCalled();
        expect(oldHandler).toHaveBeenCalledTimes(1);
    });

    it("removes this client's tools when given an empty set", async () => {
        const { session, pending, toolsSetCalls } = sessionWithPendingToolsSet();
        const oldHandler = vi.fn(() => "old");
        session.registerTools([labelTool("old_tool", oldHandler)]);

        const replacement = session.setTools([]);
        await vi.waitFor(() => expect(pending).toHaveLength(1));
        expect(toolsSetCalls()[0][1]).toEqual({ sessionId: "session-1", tools: [] });
        pending[0].resolve({});
        await replacement;

        await requestTool(session, "removed", "old_tool");
        expect(oldHandler).not.toHaveBeenCalled();
    });

    it("applies concurrent replacements one at a time, in call order", async () => {
        const { session, pending, toolsSetCalls } = sessionWithPendingToolsSet();
        const firstHandler = vi.fn(() => "first");
        const secondHandler = vi.fn(() => "second");

        const first = session.setTools([labelTool("first_tool", firstHandler)]);
        const second = session.setTools([labelTool("second_tool", secondHandler)]);
        await vi.waitFor(() => expect(pending).toHaveLength(1));
        await new Promise((resolve) => setImmediate(resolve));
        expect(toolsSetCalls()).toHaveLength(1);

        pending[0].resolve({});
        await first;
        await vi.waitFor(() => expect(pending).toHaveLength(2));
        expect(toolsSetCalls()[1][1]).toMatchObject({ tools: [{ name: "second_tool" }] });
        pending[1].resolve({});
        await second;

        await requestTool(session, "first", "first_tool");
        await requestTool(session, "second", "second_tool");
        expect(firstHandler).not.toHaveBeenCalled();
        expect(secondHandler).toHaveBeenCalledTimes(1);
    });

    it("still applies a later replacement after an earlier one is rejected", async () => {
        const { session, pending } = sessionWithPendingToolsSet();
        const secondHandler = vi.fn(() => "second");

        const first = session.setTools([labelTool("bad.name", () => "bad")]);
        const second = session.setTools([labelTool("second_tool", secondHandler)]);
        await vi.waitFor(() => expect(pending).toHaveLength(1));
        pending[0].reject(new Error("contains invalid characters"));
        await expect(first).rejects.toThrow("invalid characters");
        await vi.waitFor(() => expect(pending).toHaveLength(2));
        pending[1].resolve({});
        await second;

        await requestTool(session, "second", "second_tool");
        expect(secondHandler).toHaveBeenCalledTimes(1);
    });
});

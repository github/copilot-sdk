/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { randomUUID } from "crypto";
import { describe, expect, it } from "vitest";
import { approveAll } from "../../src/index.js";
import type { SessionEvent } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

describe("Session-scoped RPC", async () => {
    const { copilotClient: client } = await createSdkTestContext();

    async function assertImplementedFailure(
        action: () => Promise<unknown>,
        method: string
    ): Promise<void> {
        await expect(action()).rejects.toSatisfy((err: unknown) => {
            const text = err instanceof Error ? `${err.message}\n${err.stack ?? ""}` : String(err);
            expect(text.toLowerCase()).not.toContain(`unhandled method ${method.toLowerCase()}`);
            return true;
        });
    }

    function getConversationMessages(events: SessionEvent[]): { role: string; content: string }[] {
        const messages: { role: string; content: string }[] = [];
        for (const evt of events) {
            if (evt.type === "user.message") {
                messages.push({ role: "user", content: evt.data.content });
            } else if (evt.type === "assistant.message") {
                messages.push({ role: "assistant", content: evt.data.content });
            }
        }
        return messages;
    }

    it("should call session rpc model getcurrent", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            model: "claude-sonnet-4.5",
        });

        const result = await session.rpc.model.getCurrent();
        expect(result.modelId).toBeTruthy();

        await session.disconnect();
    });

    it("should call session rpc model switchto", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            model: "claude-sonnet-4.5",
        });

        const before = await session.rpc.model.getCurrent();
        expect(before.modelId).toBeTruthy();

        const result = await session.rpc.model.switchTo({
            modelId: "gpt-4.1",
            reasoningEffort: "high",
        });
        const after = await session.rpc.model.getCurrent();

        expect(result.modelId).toBe("gpt-4.1");
        expect(after.modelId).toBe(before.modelId);

        await session.disconnect();
    });

    it("should get and set session mode", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });

        const initial = await session.rpc.mode.get();
        expect(initial).toBe("interactive");

        await session.rpc.mode.set({ mode: "plan" });
        expect(await session.rpc.mode.get()).toBe("plan");

        await session.rpc.mode.set({ mode: "interactive" });
        expect(await session.rpc.mode.get()).toBe("interactive");

        await session.disconnect();
    });

    it("should read update and delete plan", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });

        const initial = await session.rpc.plan.read();
        expect(initial.exists).toBe(false);
        expect(initial.content).toBeFalsy();

        const planContent = "# Test Plan\n\n- Step 1\n- Step 2";
        await session.rpc.plan.update({ content: planContent });

        const afterUpdate = await session.rpc.plan.read();
        expect(afterUpdate.exists).toBe(true);
        expect(afterUpdate.content).toBe(planContent);

        await session.rpc.plan.delete();

        const afterDelete = await session.rpc.plan.read();
        expect(afterDelete.exists).toBe(false);
        expect(afterDelete.content).toBeFalsy();

        await session.disconnect();
    });

    it("should call workspace file rpc methods", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });

        const initial = await session.rpc.workspaces.listFiles();
        expect(initial.files).toBeDefined();

        await session.rpc.workspaces.createFile({
            path: "test.txt",
            content: "Hello, workspace!",
        });

        const afterCreate = await session.rpc.workspaces.listFiles();
        expect(afterCreate.files).toContain("test.txt");

        const file = await session.rpc.workspaces.readFile({ path: "test.txt" });
        expect(file.content).toBe("Hello, workspace!");

        const workspace = await session.rpc.workspaces.getWorkspace();
        expect(workspace.workspace).toBeDefined();
        expect(workspace.workspace.id).toBeTruthy();

        await session.disconnect();
    });

    it("should get and set session metadata", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });

        await session.rpc.name.set({ name: "SDK test session" });
        const name = await session.rpc.name.get();
        expect(name.name).toBe("SDK test session");

        const sources = await session.rpc.instructions.getSources();
        expect(sources.sources).toBeDefined();

        await session.disconnect();
    });

    it("should fork session with persisted messages", async () => {
        const sourcePrompt = "Say FORK_SOURCE_ALPHA exactly.";
        const forkPrompt = "Now say FORK_CHILD_BETA exactly.";

        const session = await client.createSession({ onPermissionRequest: approveAll });

        const initialAnswer = await session.sendAndWait({ prompt: sourcePrompt });
        expect(initialAnswer?.data.content ?? "").toContain("FORK_SOURCE_ALPHA");

        const sourceConversation = getConversationMessages(await session.getMessages());
        expect(
            sourceConversation.some((m) => m.role === "user" && m.content === sourcePrompt)
        ).toBe(true);
        expect(
            sourceConversation.some(
                (m) => m.role === "assistant" && m.content.includes("FORK_SOURCE_ALPHA")
            )
        ).toBe(true);

        const fork = await client.rpc.sessions.fork({ sessionId: session.sessionId });
        expect(fork.sessionId).toBeTruthy();
        expect(fork.sessionId).not.toBe(session.sessionId);

        const forkedSession = await client.resumeSession(fork.sessionId, {
            onPermissionRequest: approveAll,
        });
        const forkedConversation = getConversationMessages(await forkedSession.getMessages());
        expect(forkedConversation.slice(0, sourceConversation.length)).toEqual(sourceConversation);

        const forkAnswer = await forkedSession.sendAndWait({ prompt: forkPrompt });
        expect(forkAnswer?.data.content ?? "").toContain("FORK_CHILD_BETA");

        const sourceAfterFork = getConversationMessages(await session.getMessages());
        expect(sourceAfterFork.some((m) => m.content === forkPrompt)).toBe(false);

        const forkAfterPrompt = getConversationMessages(await forkedSession.getMessages());
        expect(forkAfterPrompt.some((m) => m.role === "user" && m.content === forkPrompt)).toBe(
            true
        );
        expect(
            forkAfterPrompt.some(
                (m) => m.role === "assistant" && m.content.includes("FORK_CHILD_BETA")
            )
        ).toBe(true);

        await forkedSession.disconnect();
        await session.disconnect();
    });

    it("should report error when forking session without persisted events", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });

        await expect(client.rpc.sessions.fork({ sessionId: session.sessionId })).rejects.toSatisfy(
            (err: unknown) => {
                const text =
                    err instanceof Error ? `${err.message}\n${err.stack ?? ""}` : String(err);
                expect(text.toLowerCase()).toContain("not found or has no persisted events");
                expect(text.toLowerCase()).not.toContain("unhandled method sessions.fork");
                return true;
            }
        );

        await session.disconnect();
    });

    it("should fork session to event id excluding boundary event", async () => {
        const firstPrompt = "Say FORK_BOUNDARY_FIRST exactly.";
        const secondPrompt = "Say FORK_BOUNDARY_SECOND exactly.";

        const session = await client.createSession({ onPermissionRequest: approveAll });
        try {
            await session.sendAndWait({ prompt: firstPrompt });
            await session.sendAndWait({ prompt: secondPrompt });

            const sourceEvents = await session.getMessages();
            const secondUserEvent = sourceEvents.find(
                (event) => event.type === "user.message" && event.data.content === secondPrompt
            );
            expect(secondUserEvent).toBeDefined();
            const boundaryEventId = secondUserEvent!.id;

            const fork = await client.rpc.sessions.fork({
                sessionId: session.sessionId,
                toEventId: boundaryEventId,
            });
            expect(fork.sessionId.trim()).toBeTruthy();
            expect(fork.sessionId).not.toBe(session.sessionId);

            const forkedSession = await client.resumeSession(fork.sessionId, {
                onPermissionRequest: approveAll,
            });
            try {
                const forkedEvents = await forkedSession.getMessages();
                expect(forkedEvents.some((event) => event.id === boundaryEventId)).toBe(false);

                const forkedConversation = getConversationMessages(forkedEvents);
                expect(
                    forkedConversation.some((m) => m.role === "user" && m.content === firstPrompt)
                ).toBe(true);
                expect(
                    forkedConversation.some((m) => m.role === "user" && m.content === secondPrompt)
                ).toBe(false);
            } finally {
                await forkedSession.disconnect();
            }
        } finally {
            await session.disconnect();
        }
    });

    it("should report error when forking session to unknown event id", async () => {
        const sourcePrompt = "Say FORK_UNKNOWN_EVENT_OK exactly.";
        const session = await client.createSession({ onPermissionRequest: approveAll });
        try {
            await session.sendAndWait({ prompt: sourcePrompt });

            const bogusEventId = randomUUID();
            await expect(
                client.rpc.sessions.fork({
                    sessionId: session.sessionId,
                    toEventId: bogusEventId,
                })
            ).rejects.toSatisfy((err: unknown) => {
                const text =
                    err instanceof Error ? `${err.message}\n${err.stack ?? ""}` : String(err);
                expect(text.toLowerCase()).toContain(`event ${bogusEventId} not found`);
                expect(text.toLowerCase()).not.toContain("unhandled method sessions.fork");
                return true;
            });
        } finally {
            await session.disconnect();
        }
    });

    it("should call session usage and permission rpcs", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });

        const metrics = await session.rpc.usage.getMetrics();
        expect(metrics.sessionStartTime).toBeGreaterThan(0);
        if (metrics.totalNanoAiu !== undefined && metrics.totalNanoAiu !== null) {
            expect(metrics.totalNanoAiu).toBeGreaterThanOrEqual(0);
        }
        if (metrics.tokenDetails) {
            for (const detail of Object.values(metrics.tokenDetails)) {
                expect(detail.tokenCount).toBeGreaterThanOrEqual(0);
            }
        }
        for (const modelMetric of Object.values(metrics.modelMetrics)) {
            if (modelMetric.totalNanoAiu !== undefined && modelMetric.totalNanoAiu !== null) {
                expect(modelMetric.totalNanoAiu).toBeGreaterThanOrEqual(0);
            }
            if (modelMetric.tokenDetails) {
                for (const detail of Object.values(modelMetric.tokenDetails)) {
                    expect(detail.tokenCount).toBeGreaterThanOrEqual(0);
                }
            }
        }

        try {
            const approve = await session.rpc.permissions.setApproveAll({ enabled: true });
            expect(approve.success).toBe(true);

            const reset = await session.rpc.permissions.resetSessionApprovals();
            expect(reset.success).toBe(true);
        } finally {
            await session.rpc.permissions.setApproveAll({ enabled: false });
        }

        await session.disconnect();
    });

    it("should report implemented errors for unsupported session rpc paths", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });

        await assertImplementedFailure(
            () => session.rpc.history.truncate({ eventId: "missing-event" }),
            "session.history.truncate"
        );

        await assertImplementedFailure(
            () => session.rpc.mcp.oauth.login({ serverName: "missing-server" }),
            "session.mcp.oauth.login"
        );

        await session.disconnect();
    });

    it("should compact session history after messages", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });

        await session.sendAndWait({ prompt: "What is 2+2?" });

        const result = await session.rpc.history.compact();
        expect(result).toBeDefined();

        await session.disconnect();
    });
});

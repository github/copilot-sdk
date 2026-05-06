/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { randomUUID } from "crypto";
import { describe, expect, it } from "vitest";
import { approveAll } from "../../src/index.js";
import type { CopilotSession, SessionEvent } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

const EVENT_TIMEOUT_MS = 30_000;

function waitForEvent<T extends SessionEvent>(
    session: CopilotSession,
    predicate: (event: SessionEvent) => event is T,
    description: string,
    timeoutMs = EVENT_TIMEOUT_MS
): Promise<T> {
    return new Promise<T>((resolve, reject) => {
        let unsubscribe: () => void = () => {};
        const timer = setTimeout(() => {
            unsubscribe();
            reject(new Error(`Timed out waiting for ${description}`));
        }, timeoutMs);

        unsubscribe = session.on((event) => {
            if (predicate(event)) {
                clearTimeout(timer);
                unsubscribe();
                resolve(event);
            } else if (event.type === "session.error") {
                clearTimeout(timer);
                unsubscribe();
                reject(new Error(`${event.data.message}\n${event.data.stack ?? ""}`));
            }
        });
    });
}

describe("Session RPC event side effects", async () => {
    const { copilotClient: client } = await createSdkTestContext();

    it("should emit mode changed event when mode set", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        try {
            const modeChanged = waitForEvent(
                session,
                (event): event is Extract<SessionEvent, { type: "session.mode_changed" }> =>
                    event.type === "session.mode_changed" &&
                    event.data.newMode === "plan" &&
                    event.data.previousMode === "interactive",
                "session.mode_changed event for interactive to plan"
            );

            await session.rpc.mode.set({ mode: "plan" });

            const event = await modeChanged;
            expect(event.data.newMode).toBe("plan");
            expect(event.data.previousMode).toBe("interactive");
        } finally {
            await session.disconnect();
        }
    });

    it("should emit plan changed event for update and delete", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        try {
            const created = waitForEvent(
                session,
                (event): event is Extract<SessionEvent, { type: "session.plan_changed" }> =>
                    event.type === "session.plan_changed" && event.data.operation === "create",
                "session.plan_changed create event"
            );
            await session.rpc.plan.update({ content: "# Test plan\n- item" });
            expect((await created).data.operation).toBe("create");

            const deleted = waitForEvent(
                session,
                (event): event is Extract<SessionEvent, { type: "session.plan_changed" }> =>
                    event.type === "session.plan_changed" && event.data.operation === "delete",
                "session.plan_changed delete event"
            );
            await session.rpc.plan.delete();
            expect((await deleted).data.operation).toBe("delete");
        } finally {
            await session.disconnect();
        }
    });

    it("should emit plan changed update operation on second update", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        try {
            await session.rpc.plan.update({ content: "# initial" });

            const updated = waitForEvent(
                session,
                (event): event is Extract<SessionEvent, { type: "session.plan_changed" }> =>
                    event.type === "session.plan_changed" && event.data.operation === "update",
                "session.plan_changed update event"
            );
            await session.rpc.plan.update({ content: "# updated content" });

            expect((await updated).data.operation).toBe("update");
        } finally {
            await session.disconnect();
        }
    });

    it("should emit workspace file changed event when file created", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        try {
            const path = `side-effect-${randomUUID()}.txt`;
            const changed = waitForEvent(
                session,
                (
                    event
                ): event is Extract<SessionEvent, { type: "session.workspace_file_changed" }> =>
                    event.type === "session.workspace_file_changed" && event.data.path === path,
                `session.workspace_file_changed event for ${path}`
            );

            await session.rpc.workspaces.createFile({ path, content: "hello" });

            const event = await changed;
            expect(event.data.path).toBe(path);
            expect(["create", "update"]).toContain(event.data.operation);
        } finally {
            await session.disconnect();
        }
    });

    it("should emit title changed event when name set", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        try {
            const title = `Renamed-${randomUUID()}`;
            const titleChanged = waitForEvent(
                session,
                (event): event is Extract<SessionEvent, { type: "session.title_changed" }> =>
                    event.type === "session.title_changed" && event.data.title === title,
                "session.title_changed event after name.set"
            );

            await session.rpc.name.set({ name: title });

            expect((await titleChanged).data.title).toBe(title);
        } finally {
            await session.disconnect();
        }
    });

    it("should emit snapshot rewind event and remove events on truncate", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        try {
            await session.sendAndWait({ prompt: "Say SNAPSHOT_REWIND_TARGET exactly." });

            const messages = await session.getMessages();
            const userEvent = messages.find((event) => event.type === "user.message");
            expect(userEvent).toBeDefined();
            const targetEventId = userEvent!.id;

            const rewind = waitForEvent(
                session,
                (event): event is Extract<SessionEvent, { type: "session.snapshot_rewind" }> =>
                    event.type === "session.snapshot_rewind" &&
                    event.data.upToEventId.toLowerCase() === targetEventId.toLowerCase(),
                "session.snapshot_rewind event after truncate"
            );

            const truncateResult = await session.rpc.history.truncate({ eventId: targetEventId });
            expect(truncateResult.eventsRemoved).toBeGreaterThanOrEqual(1);

            const rewindEvent = await rewind;
            expect(rewindEvent.data.eventsRemoved).toBe(truncateResult.eventsRemoved);
            expect(rewindEvent.data.upToEventId.toLowerCase()).toBe(targetEventId.toLowerCase());

            const messagesAfter = await session.getMessages();
            expect(messagesAfter.some((event) => event.id === targetEventId)).toBe(false);
        } finally {
            await session.disconnect();
        }
    });

    it("should allow session use after truncate", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        try {
            await session.sendAndWait({ prompt: "Say SNAPSHOT_REWIND_TARGET exactly." });

            const messages = await session.getMessages();
            const userEvent = messages.find((event) => event.type === "user.message");
            expect(userEvent).toBeDefined();

            const truncateResult = await session.rpc.history.truncate({ eventId: userEvent!.id });
            expect(truncateResult.eventsRemoved).toBeGreaterThanOrEqual(1);

            const mode = await session.rpc.mode.get();
            expect(["interactive", "plan", "autopilot"]).toContain(mode);
            const workspace = await session.rpc.workspaces.getWorkspace();
            expect(workspace.workspace).toBeDefined();
        } finally {
            await session.disconnect();
        }
    });
});

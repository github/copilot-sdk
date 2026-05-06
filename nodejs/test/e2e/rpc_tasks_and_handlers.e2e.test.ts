/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { approveAll } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

describe("Session tasks RPC and pending handlers", async () => {
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

    it("should list task state and return false for missing task operations", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });

        const tasks = await session.rpc.tasks.list();
        expect(tasks.tasks).toBeDefined();
        expect(tasks.tasks).toEqual([]);

        const promote = await session.rpc.tasks.promoteToBackground({ taskId: "missing-task" });
        expect(promote.promoted).toBe(false);

        const cancel = await session.rpc.tasks.cancel({ taskId: "missing-task" });
        expect(cancel.cancelled).toBe(false);

        const remove = await session.rpc.tasks.remove({ taskId: "missing-task" });
        expect(remove.removed).toBe(false);

        await session.disconnect();
    }, 60_000);

    it("should report implemented error for missing task agent type", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });

        await assertImplementedFailure(
            () =>
                session.rpc.tasks.startAgent({
                    agentType: "missing-agent-type",
                    prompt: "Say hi",
                    name: "sdk-test-task",
                }),
            "session.tasks.startAgent"
        );

        await session.disconnect();
    });

    it("should return expected results for missing pending handler requestIds", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });

        const tool = await session.rpc.tools.handlePendingToolCall({
            requestId: "missing-tool-request",
            result: "tool result",
        });
        expect(tool.success).toBe(false);

        const command = await session.rpc.commands.handlePendingCommand({
            requestId: "missing-command-request",
            error: "command error",
        });
        expect(command.success).toBe(true);

        const elicitation = await session.rpc.ui.handlePendingElicitation({
            requestId: "missing-elicitation-request",
            result: { action: "cancel" },
        });
        expect(elicitation.success).toBe(false);

        const permission = await session.rpc.permissions.handlePendingPermissionRequest({
            requestId: "missing-permission-request",
            result: { kind: "reject", feedback: "not approved" },
        });
        expect(permission.success).toBe(false);

        const permanent = await session.rpc.permissions.handlePendingPermissionRequest({
            requestId: "missing-permanent-permission-request",
            result: { kind: "approve-permanently", domain: "example.com" },
        });
        expect(permanent.success).toBe(false);

        await session.disconnect();
    });
});

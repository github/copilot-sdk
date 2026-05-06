/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import * as fs from "fs";
import * as os from "os";
import * as path from "path";
import { describe, expect, it } from "vitest";
import { z } from "zod";
import { approveAll, defineTool } from "../../src/index.js";
import type { CopilotSession, SessionEvent } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

describe("Shell and fleet RPC", async () => {
    const { copilotClient: client, workDir } = await createSdkTestContext();

    function createWriteFileCommand(markerPath: string, marker: string): string {
        if (os.platform() === "win32") {
            return `powershell -NoLogo -NoProfile -Command "Set-Content -LiteralPath '${markerPath}' -Value '${marker}'"`;
        }
        return `sh -c "printf '%s' '${marker}' > '${markerPath}'"`;
    }

    async function waitForFileText(
        filePath: string,
        expected: string,
        timeoutMs = 30_000
    ): Promise<void> {
        const deadline = Date.now() + timeoutMs;
        while (Date.now() < deadline) {
            if (fs.existsSync(filePath)) {
                const content = fs.readFileSync(filePath, "utf8");
                if (content.includes(expected)) {
                    return;
                }
            }
            await new Promise((resolve) => setTimeout(resolve, 100));
        }
        throw new Error(
            `Timed out waiting for shell command to write '${expected}' to '${filePath}'.`
        );
    }

    async function waitForMessages(
        session: CopilotSession,
        predicate: (events: SessionEvent[]) => boolean,
        timeoutMs = 120_000
    ): Promise<SessionEvent[]> {
        // Fleet-mode tasks do not emit session.idle on completion, so polling the
        // session message list is the simplest way to wait for a satisfying state.
        const deadline = Date.now() + timeoutMs;
        while (Date.now() < deadline) {
            const messages = await session.getMessages();
            if (predicate(messages)) {
                return messages;
            }
            await new Promise((resolve) => setTimeout(resolve, 250));
        }
        throw new Error("Timed out waiting for fleet-mode assistant reply to satisfy predicate.");
    }

    it("should execute shell command", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        const markerPath = path.join(
            workDir,
            `shell-rpc-${Date.now()}-${Math.random().toString(36).slice(2)}.txt`
        );
        const marker = "copilot-sdk-shell-rpc";

        const result = await session.rpc.shell.exec({
            command: createWriteFileCommand(markerPath, marker),
            cwd: workDir,
        });

        expect(result.processId).toBeTruthy();
        await waitForFileText(markerPath, marker);

        await session.disconnect();
    });

    it("should kill shell process", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        const command =
            os.platform() === "win32"
                ? `powershell -NoLogo -NoProfile -Command "Start-Sleep -Seconds 30"`
                : "sleep 30";

        // On Windows, terminating the shell wrapper can briefly leave grandchildren alive.
        // Keep this command outside the fixture workspace so cleanup is not blocked by cwd handles.
        const execResult = await session.rpc.shell.exec({ command, cwd: os.tmpdir() });
        expect(execResult.processId).toBeTruthy();

        const killResult = await session.rpc.shell.kill({ processId: execResult.processId });
        expect(killResult.killed).toBe(true);

        await session.disconnect();
    });

    it("should start fleet and complete custom tool task", { timeout: 180_000 }, async () => {
        const markerPath = path.join(
            workDir,
            `fleet-rpc-${Date.now()}-${Math.random().toString(36).slice(2)}.txt`
        );
        const marker = "copilot-sdk-fleet-rpc";
        const toolName = "record_fleet_completion";

        const recordFleetCompletion = defineTool(toolName, {
            description: "Records completion of the fleet validation task.",
            parameters: z.object({ content: z.string() }),
            handler: ({ content }) => {
                fs.writeFileSync(markerPath, content);
                return content;
            },
        });

        const session = await client.createSession({
            onPermissionRequest: approveAll,
            tools: [recordFleetCompletion],
        });

        const prompt = `Use the ${toolName} tool with content '${marker}', then report that the fleet task is complete.`;

        const result = await session.rpc.fleet.start({ prompt });
        expect(result.started).toBe(true);

        await waitForFileText(markerPath, marker);

        const messages = await waitForMessages(session, (events) =>
            events.some(
                (e) =>
                    e.type === "assistant.message" &&
                    (e.data.content ?? "").toLowerCase().includes("fleet task")
            )
        );

        const userMessages = messages.filter((m) => m.type === "user.message");
        expect(userMessages.some((m) => m.data.content.includes(prompt))).toBe(true);

        const toolStarts = messages.filter((m) => m.type === "tool.execution_start");
        expect(toolStarts.some((m) => m.data.toolName === toolName)).toBe(true);

        const toolCompletes = messages.filter((m) => m.type === "tool.execution_complete");
        expect(
            toolCompletes.some(
                (m) =>
                    m.data.success === true &&
                    typeof m.data.result?.content === "string" &&
                    m.data.result.content.includes(marker)
            )
        ).toBe(true);

        const assistantMessages = messages.filter((m) => m.type === "assistant.message");
        expect(
            assistantMessages.some((m) =>
                (m.data.content ?? "").toLowerCase().includes("fleet task")
            )
        ).toBe(true);

        await session.disconnect();
    });
});

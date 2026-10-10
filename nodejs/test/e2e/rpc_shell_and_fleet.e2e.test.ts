/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import * as fs from "fs";
import * as os from "os";
import * as path from "path";
import { randomUUID } from "node:crypto";
import { describe, expect, it } from "vitest";
import { z } from "zod";
import { approveAll, defineTool } from "../../src/index.js";
import type { CopilotSession, SessionEvent } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";
import { waitForCondition, waitForFileText } from "./harness/sdkTestHelper.js";

describe("Shell and fleet RPC", async () => {
    const { copilotClient: client, workDir } = await createSdkTestContext();

    function createWriteFileCommand(markerPath: string, marker: string): string {
        if (os.platform() === "win32") {
            return `echo ${marker}>"${markerPath}"`;
        }
        return `sh -c "printf '%s' '${marker}' > '${markerPath}'"`;
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
            const messages = await session.getEvents();
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
            command: createWriteFileCommand(path.basename(markerPath), marker),
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

    it("should honor custom cwd for shell exec", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        const cwd = path.join(workDir, `shell-cwd-${randomUUID()}`);
        fs.mkdirSync(cwd);
        try {
            const result = await session.rpc.shell.exec({
                command: createWriteFileCommand("marker.txt", "custom-cwd"),
                cwd,
            });
            expect(result.processId).toBeTruthy();
            await waitForFileText(path.join(cwd, "marker.txt"), "custom-cwd");
        } finally {
            await session.disconnect();
        }
    });

    it("should return false when killing an unknown shell process", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        try {
            expect(
                (await session.rpc.shell.kill({ processId: `unknown-${randomUUID()}` })).killed
            ).toBe(false);
        } finally {
            await session.disconnect();
        }
    });

    it.each(["SIGTERM", "SIGKILL"] as const)(
        "should release shell process after %s",
        async (signal) => {
            const session = await client.createSession({ onPermissionRequest: approveAll });
            const command =
                os.platform() === "win32"
                    ? 'powershell -NoLogo -NoProfile -Command "Start-Sleep -Seconds 60"'
                    : "sleep 60";
            try {
                const result = await session.rpc.shell.exec({ command, cwd: workDir });
                expect(result.processId).toBeTruthy();
                expect(
                    (await session.rpc.shell.kill({ processId: result.processId, signal })).killed
                ).toBe(true);
                await waitForCondition(
                    async () =>
                        !(await session.rpc.shell.kill({ processId: result.processId })).killed,
                    { timeoutMessage: `Process ${result.processId} remains after ${signal}` }
                );
            } finally {
                await session.disconnect();
            }
        }
    );

    it(
        "should stop a timed-out shell command before its final marker",
        { timeout: 60_000 },
        async () => {
            const session = await client.createSession({ onPermissionRequest: approveAll });
            const started = path.join(workDir, `shell-started-${randomUUID()}.txt`);
            const completed = path.join(workDir, `shell-completed-${randomUUID()}.txt`);
            const timeout = os.platform() === "win32" ? 10_000 : 1_000;
            const command =
                os.platform() === "win32"
                    ? `echo started>"${started}" & ping.exe -n 31 127.0.0.1 >nul & echo completed>"${completed}"`
                    : `printf started > '${started}'; sleep 30; printf completed > '${completed}'`;
            try {
                const result = await session.rpc.shell.exec({ command, cwd: workDir, timeout });
                expect(result.processId).toBeTruthy();
                await waitForCondition(() => fs.existsSync(started), {
                    timeoutMessage: `Timed-out shell command did not start: ${started}`,
                });
                // The start marker acknowledges the phase before the deadline is observed.
                await new Promise((resolve) => setTimeout(resolve, timeout + 2_000));
                expect((await session.rpc.shell.kill({ processId: result.processId })).killed).toBe(
                    false
                );
                expect(fs.existsSync(completed)).toBe(false);
            } finally {
                await session.disconnect();
            }
        }
    );

    it.skipIf(os.platform() === "win32")(
        "should release post-root-exit shell pipes and rewind admission at timeout",
        async ({ onTestFailed }) => {
            const cwd = fs.mkdtempSync(path.join(workDir, "post-exit-shell-"));
            let phase = "creating session";
            let rootPid: number | undefined;
            let rootExited = false;
            let lastRewindState:
                | Awaited<ReturnType<CopilotSession["rpc"]["history"]["listRewindPoints"]>>
                | undefined;
            onTestFailed(() => {
                console.error("Post-root-exit shell timeout diagnostic:", {
                    phase,
                    rootPid,
                    rootExited,
                    lastRewindState,
                    descendantReady: fs.existsSync(path.join(cwd, "descendant.ready")),
                    terminated: fs.existsSync(path.join(cwd, "terminated")),
                });
            });
            const session = await client.createSession({
                enableFileChangeTracking: true,
                onPermissionRequest: approveAll,
            });
            let processId: string | undefined;
            function processExists(pid: number): boolean {
                try {
                    process.kill(pid, 0);
                    return true;
                } catch (error) {
                    if ((error as NodeJS.ErrnoException).code === "ESRCH") {
                        return false;
                    }
                    throw error;
                }
            }
            try {
                phase = "waiting for rewind tracking initialization";
                await waitForCondition(
                    async () => {
                        lastRewindState = await session.rpc.history.listRewindPoints();
                        return lastRewindState.fileChangeTrackingEnabled;
                    },
                    { timeoutMessage: "Rewind tracking did not initialize" }
                );
                phase = "launching post-exit shell";
                // Reader teardown must not SIGPIPE the fixture before its TERM trap records termination.
                const result = await session.rpc.shell.exec({
                    command:
                        `sh -c 'trap "" PIPE; trap "printf terminated > terminated; exit 0" TERM; ` +
                        `printf ready > descendant.ready; sleep 60' & ` +
                        `echo $$ > root.pending; mv root.pending root.pid; exit 7`,
                    cwd,
                    timeout: 10_000,
                });
                processId = result.processId;
                expect(processId).toBeTruthy();
                phase = "waiting for root reap and descendant readiness";
                await waitForCondition(
                    () => {
                        if (
                            !fs.existsSync(path.join(cwd, "root.pid")) ||
                            !fs.existsSync(path.join(cwd, "descendant.ready"))
                        ) {
                            return false;
                        }
                        rootPid = Number(fs.readFileSync(path.join(cwd, "root.pid"), "utf8"));
                        expect(Number.isInteger(rootPid) && rootPid > 1).toBe(true);
                        rootExited = !processExists(rootPid);
                        return rootExited;
                    },
                    { timeoutMessage: "Fixture root did not exit with a ready pipe holder" }
                );
                phase = "checking retained rewind lease";
                expect(fs.existsSync(path.join(cwd, "terminated"))).toBe(false);
                lastRewindState = await session.rpc.history.listRewindPoints();
                expect(lastRewindState.unavailableReason).toBe("session-busy");

                // Neither poll sends a kill: the configured timeout must end the
                // descendant and release the production sink's operation lease.
                phase = "waiting for rewind admission after timeout";
                await waitForCondition(
                    async () => {
                        lastRewindState = await session.rpc.history.listRewindPoints();
                        return (
                            lastRewindState.fileChangeTrackingEnabled &&
                            lastRewindState.unavailableReason === undefined
                        );
                    },
                    { timeoutMessage: "Post-exit timeout left rewind admission session-busy" }
                );
                phase = "waiting for automatic descendant termination";
                await waitForFileText(path.join(cwd, "terminated"), "terminated");
                phase = "checking registry cleanup";
                expect((await session.rpc.shell.kill({ processId })).killed).toBe(false);
            } finally {
                try {
                    if (processId) {
                        await session.rpc.shell.kill({ processId, signal: "SIGKILL" });
                    }
                } finally {
                    await session.disconnect();
                }
            }
        }
    );

    it("should accept a missing shell command and clean up after it exits", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        const marker = path.join(workDir, `shell-missing-${randomUUID()}.txt`);
        const missingCommand = `not-a-command-${randomUUID()}`;
        const command =
            os.platform() === "win32"
                ? `${missingCommand} & echo done>"${marker}" & exit /b 1`
                : `${missingCommand}; code=$?; printf done > '${marker}'; exit $code`;
        try {
            const result = await session.rpc.shell.exec({ command, cwd: workDir });
            expect(result.processId).toBeTruthy();
            await waitForFileText(marker, "done");
            // The shell may still be flushing stderr when the final marker is written.
            await new Promise((resolve) => setTimeout(resolve, 1_000));
            expect((await session.rpc.shell.kill({ processId: result.processId })).killed).toBe(
                false
            );
        } finally {
            await session.disconnect();
        }
    });

    it.each(["stderr", "large-stdout"] as const)(
        "should clean up shell process after %s output",
        async (outputKind) => {
            const session = await client.createSession({ onPermissionRequest: approveAll });
            const marker = path.join(workDir, `shell-${outputKind}-${randomUUID()}.txt`);
            const phase = `${marker}.phase`;
            const recordPhase = (value: string) =>
                `[IO.File]::WriteAllText('${phase}', '${value}')`;
            // Publish only after Set-Content has closed its exclusive Windows writer.
            const publishMarker =
                `${recordPhase("output-written")}; ` +
                `Set-Content -LiteralPath '${marker}.pending' -Value done; ` +
                `${recordPhase("marker-written")}; ` +
                `Move-Item -LiteralPath '${marker}.pending' -Destination '${marker}'; ` +
                recordPhase("marker-published");
            const command =
                outputKind === "stderr"
                    ? os.platform() === "win32"
                        ? `powershell -NoLogo -NoProfile -Command "${recordPhase("started")}; [Console]::Error.WriteLine('boom'); ${publishMarker}; exit 2"`
                        : `echo boom 1>&2; printf done > '${marker}'; exit 2`
                    : os.platform() === "win32"
                      ? `powershell -NoLogo -NoProfile -Command "${recordPhase("started")}; Write-Host ('x' * 71680); ${publishMarker}"`
                      : `printf '%71680s' '' | tr ' ' '='; printf done > '${marker}'`;
            try {
                const result = await session.rpc.shell.exec({ command, cwd: workDir });
                expect(result.processId).toBeTruthy();
                await waitForFileText(
                    marker,
                    "done",
                    30_000,
                    os.platform() === "win32" ? [phase, `${marker}.pending`] : []
                );
                // The process map is updated after the output stream closes.
                await new Promise((resolve) => setTimeout(resolve, 2_000));
                expect((await session.rpc.shell.kill({ processId: result.processId })).killed).toBe(
                    false
                );
            } finally {
                await session.disconnect();
            }
        },
        60_000
    );

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

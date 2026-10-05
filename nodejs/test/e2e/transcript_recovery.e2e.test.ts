/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { randomUUID } from "node:crypto";
import { mkdir, readFile, writeFile } from "node:fs/promises";
import { join } from "node:path";
import { describe, expect, it } from "vitest";
import { approveAll, type TranscriptRecovery } from "../../src/index.js";
import { createSdkTestContext, isInProcessTransport } from "./harness/sdkTestContext.js";

describe.skipIf(isInProcessTransport)("Transcript recovery through the public SDK", async () => {
    const { createClient, workDir } = await createSdkTestContext();

    it.each([
        ["empty", "torn final record"],
        ["copilot-cli", "torn final record"],
        ["empty", "forward-compatible record before session.start"],
        ["copilot-cli", "forward-compatible record before session.start"],
    ] as const)(
        "uses permissive defaults in %s mode for a %s and supports explicit rejection",
        { timeout: 90_000 },
        async (mode, damage) => {
            // Stop the log-writing client before fixture afterEach removes workDir.
            await using client = createClient({ mode, baseDirectory: workDir });

            const sessionId = randomUUID();
            const sessionDir = join(workDir, "session-state", sessionId);
            const eventsPath = join(sessionDir, "events.jsonl");
            const timestamp = "2024-01-02T03:04:05.000Z";
            const intact = `${JSON.stringify({
                id: randomUUID(),
                parentId: null,
                type: "session.start",
                timestamp,
                data: {
                    sessionId,
                    version: 1,
                    producer: "copilot-agent",
                    copilotVersion: "0.0.353",
                    startTime: timestamp,
                },
            })}\n`;
            await mkdir(sessionDir, { recursive: true });
            await writeFile(eventsPath, intact);
            await writeFile(
                join(sessionDir, "workspace.yaml"),
                JSON.stringify({
                    id: sessionId,
                    cwd: workDir,
                    summary_count: 0,
                    created_at: timestamp,
                    updated_at: timestamp,
                })
            );
            const tornTail = damage === "torn final record";
            const damaged = tornTail
                ? intact + '{"type":"user.message","data":'
                : `${JSON.stringify({ id: randomUUID(), type: "newer.event", data: {} })}\n${intact}`;
            const expectedRecovery = {
                invalidLineNumbers: tornTail ? [2] : [],
                sessionStartMoved: !tornTail,
            };
            await writeFile(eventsPath, damaged);

            await expect(
                client.resumeSession(sessionId, {
                    availableTools: ["builtin:ask_user"],
                    onPermissionRequest: approveAll,
                    allowTranscriptRecovery: false,
                })
            ).rejects.toMatchObject({
                code: -32075,
                data: expectedRecovery,
            });
            expect(await readFile(eventsPath, "utf8")).toBe(damaged);

            const resumed = await client.resumeSession(sessionId, {
                availableTools: ["builtin:ask_user"],
                onPermissionRequest: approveAll,
            });
            const recovery: TranscriptRecovery | undefined = resumed.transcriptRecovery;
            expect(recovery).toMatchObject(expectedRecovery);
            expect(recovery?.plannedBackupPath).toContain("events.jsonl.backup-before-recovery-");
            await resumed.disconnect();
        }
    );
});

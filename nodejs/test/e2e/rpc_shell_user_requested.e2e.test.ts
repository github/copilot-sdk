/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { randomUUID } from "node:crypto";
import { existsSync, rmSync } from "node:fs";
import { join } from "node:path";
import { describe, expect, it } from "vitest";
import { approveAll } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";
import { waitForCondition } from "./harness/sdkTestHelper.js";

describe("User-requested shell RPC", async () => {
    const { copilotClient: client, homeDir } = await createSdkTestContext();

    function compactUuid(): string {
        return randomUUID().replace(/-/g, "");
    }

    function quotePowerShell(value: string): string {
        return `'${value.replace(/'/g, "''")}'`;
    }

    function quoteSh(value: string): string {
        return `'${value.replace(/'/g, "'\\''")}'`;
    }

    function createMarkerThenSleepCommand(markerPath: string, seconds: number): string {
        if (process.platform === "win32") {
            return `Set-Content -LiteralPath ${quotePowerShell(markerPath)} -Value 'running'; Start-Sleep -Seconds ${seconds}`;
        }
        return `echo running > ${quoteSh(markerPath)}; sleep ${seconds}`;
    }

    async function waitForFileExists(filePath: string): Promise<void> {
        await waitForCondition(() => existsSync(filePath), {
            timeoutMs: 30_000,
            intervalMs: 100,
            timeoutMessage: `Timed out waiting for the shell command to create '${filePath}'.`,
        });
    }

    async function withTimeout<T>(
        promise: Promise<T>,
        timeoutMs: number,
        message: string
    ): Promise<T> {
        let timeout: ReturnType<typeof setTimeout> | undefined;
        try {
            return await Promise.race([
                promise,
                new Promise<never>((_, reject) => {
                    timeout = setTimeout(() => reject(new Error(message)), timeoutMs);
                }),
            ]);
        } finally {
            if (timeout) {
                clearTimeout(timeout);
            }
        }
    }

    function tryDeleteFile(filePath: string): void {
        try {
            rmSync(filePath, { force: true });
        } catch {
            // Best-effort cleanup.
        }
    }

    it("should execute user requested shell command", { timeout: 120_000 }, async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        try {
            const marker = `copilotusershell${compactUuid()}`;
            const requestId = `req-${compactUuid()}`;

            const result = await session.rpc.shell.executeUserRequested({
                requestId,
                command: `echo ${marker}`,
            });

            expect(result.success).toBe(true);
            expect(result.exitCode).toBe(0);
            expect(result.output).toContain(marker);
            expect(result.toolCallId).toBeTruthy();
        } finally {
            await session.disconnect();
        }
    });

    it("should cancel user requested shell command", { timeout: 120_000 }, async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        const markerPath = join(homeDir, `shell-cancel-${compactUuid()}.txt`);
        let executeTask:
            | Promise<Awaited<ReturnType<typeof session.rpc.shell.executeUserRequested>>>
            | undefined;
        let executeSettled = false;
        try {
            const missing = await session.rpc.shell.cancelUserRequested({
                requestId: `missing-${compactUuid()}`,
            });
            expect(missing.cancelled).toBe(false);

            const requestId = `req-${compactUuid()}`;
            executeTask = session.rpc.shell.executeUserRequested({
                requestId,
                command: createMarkerThenSleepCommand(markerPath, 60),
            });
            executeTask
                .finally(() => {
                    executeSettled = true;
                })
                .catch(() => {});
            executeTask.catch(() => {});

            await waitForFileExists(markerPath);

            await waitForCondition(
                async () => (await session.rpc.shell.cancelUserRequested({ requestId })).cancelled,
                {
                    timeoutMs: 15_000,
                    intervalMs: 100,
                    timeoutMessage:
                        "Timed out waiting for the user-requested shell command to become cancellable.",
                }
            );

            const result = await withTimeout(
                executeTask,
                30_000,
                "Timed out waiting for cancelled shell command to finish."
            );
            expect(result.success).toBe(false);
        } finally {
            if (executeTask && !executeSettled) {
                await withTimeout(
                    executeTask,
                    30_000,
                    "Timed out draining cancelled shell command."
                ).catch(() => {});
            }
            tryDeleteFile(markerPath);
            await session.disconnect();
        }
    });
});

/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { existsSync, mkdtempSync, readFileSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it, onTestFinished } from "vitest";
import { CopilotClient, RuntimeConnection } from "../src/index.js";

const fixture = fileURLToPath(
    new URL("../../test/harness/stdio-shutdown-runtime.cjs", import.meta.url)
);

describe("owned stdio shutdown", () => {
    it.each([
        "stop",
        "dispose",
        "force",
        "fallback",
        "start-failure",
        "force-during-stop",
    ] as const)(
        "%s preserves the owned-process cleanup contract",
        async (mode) => {
            const directory = mkdtempSync(join(tmpdir(), "copilot-node-shutdown-"));
            const marker = join(directory, "telemetry.jsonl");
            const pidFile = join(directory, "runtime.pid");
            const client = new CopilotClient({
                connection: RuntimeConnection.forStdio({
                    path: process.execPath,
                    args: [
                        fixture,
                        marker,
                        mode === "force-during-stop" ? "fallback" : mode,
                        pidFile,
                    ],
                }),
                useLoggedInUser: false,
            });
            onTestFinished(async () => {
                await client.forceStop();
                rmSync(directory, {
                    recursive: true,
                    force: true,
                    maxRetries: 10,
                    retryDelay: 100,
                });
            });

            if (mode === "start-failure") {
                await expect(client.start()).rejects.toThrow(/protocol version/i);
            } else {
                await client.start();
                const started = performance.now();
                if (mode === "force") {
                    await client.forceStop();
                    expect(performance.now() - started).toBeLessThan(10_000);
                } else if (mode === "force-during-stop") {
                    const stopping = client.stop();
                    await expect.poll(() => existsSync(marker), { timeout: 5000 }).toBe(true);
                    await client.forceStop();
                    expect(await stopping).toEqual([]);
                    expect(performance.now() - started).toBeLessThan(10_000);
                } else if (mode === "dispose") {
                    await client[Symbol.asyncDispose]();
                } else {
                    expect(await client.stop()).toEqual([]);
                }
                if (mode === "fallback") {
                    expect(performance.now() - started).toBeGreaterThanOrEqual(10_000);
                }
            }

            const pid = Number(readFileSync(pidFile, "utf8"));
            const hasExited = () => {
                try {
                    process.kill(pid, 0);
                    return false;
                } catch (error) {
                    if ((error as NodeJS.ErrnoException).code !== "ESRCH") throw error;
                    return true;
                }
            };
            if (mode === "force" || mode === "start-failure") {
                // Force-stop sends the kill signal without waiting to reap the child.
                await expect.poll(hasExited, { timeout: 5000 }).toBe(true);
                expect(existsSync(marker)).toBe(false);
            } else {
                expect(hasExited()).toBe(true);
                expect(readFileSync(marker, "utf8")).toBe('{"type":"span"}\n');
            }
            expect(await client.stop()).toEqual([]);
            await client[Symbol.asyncDispose]();
        },
        40_000
    );
});

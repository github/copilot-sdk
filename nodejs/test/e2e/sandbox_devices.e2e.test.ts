/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { readFile, writeFile } from "node:fs/promises";
import { join } from "node:path";
import { describe, it } from "vitest";
import type { PermissionRequest } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

const TEST_NAME = "does not offer a sandbox bypass for standard device paths";

describe("Sandbox devices", async () => {
    if (process.platform !== "darwin") {
        // The SDK sandbox suite currently has a backend only on macOS.
        it.skip(TEST_NAME, () => undefined);
        return;
    }

    const { copilotClient: client, workDir } = await createSdkTestContext({
        copilotClientOptions: {
            env: { COPILOT_CLI_ENABLED_FEATURE_FLAGS: "SANDBOX" },
        },
    });

    it(TEST_NAME, { timeout: 180_000 }, async ({ expect }) => {
        const denialText = "cat: /dev/stdin: Operation not permitted\n";
        await writeFile(join(workDir, "denial.txt"), denialText);
        const bypassRequests: PermissionRequest[] = [];
        const shellResults: string[] = [];
        const shellCalls = new Set<string>();
        const session = await client.createSession({
            onPermissionRequest: (request) => {
                if ("requestSandboxBypass" in request && request.requestSandboxBypass) {
                    bypassRequests.push(request);
                    return { kind: "reject" };
                }
                return { kind: "approve-once" };
            },
        });
        try {
            const update = await session.rpc.options.update({
                sandboxConfig: {
                    enabled: true,
                    allowBypass: true,
                    addCurrentWorkingDirectory: true,
                },
            });
            expect(update.success).toBe(true);
            session.on((event) => {
                if (event.type === "tool.execution_start" && event.data.toolName === "bash") {
                    shellCalls.add(event.data.toolCallId);
                } else if (
                    event.type === "tool.execution_complete" &&
                    shellCalls.has(event.data.toolCallId) &&
                    event.data.success
                ) {
                    shellResults.push(event.data.result?.content ?? "");
                }
            });

            const message = await session.sendAndWait(
                {
                    prompt:
                        "Use the bash tool exactly once to run this exact command: " +
                        "`head -c 16 /dev/urandom > entropy.bin && " +
                        "cat /dev/stdin < denial.txt > copy.txt && cat copy.txt`. " +
                        "Do not request a sandbox bypass. After the tool returns, " +
                        "reply with exactly DEVICE_COMMAND_COMPLETE.",
                },
                120_000
            );

            expect(message?.data.content).toContain("DEVICE_COMMAND_COMPLETE");
            expect(shellCalls.size).toBe(1);
            expect(shellResults).toHaveLength(1);
            expect(shellResults[0]).toContain(denialText.trim());
            expect(shellResults[0]).toContain("exit code 0");
            expect(await readFile(join(workDir, "entropy.bin"))).toHaveLength(16);
            expect(await readFile(join(workDir, "copy.txt"), "utf8")).toBe(denialText);
            expect(bypassRequests).toEqual([]);
        } finally {
            await session.disconnect();
        }
    });
});

/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { mkdir, writeFile } from "fs/promises";
import { join } from "path";
import { describe, expect, it } from "vitest";
import type { PermissionRequest } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

const SEND_TIMEOUT_MS = 120_000;
const TEST_TIMEOUT_MS = 180_000;

describe("Sandbox bypass", () => {
    // The Windows backend requires BaseContainer, which is unavailable on the SDK's Windows runners.
    it.skipIf(process.platform === "win32")(
        "approves a blocked search and executes it outside the sandbox",
        async () => {
            const { copilotClient: client, workDir } = await createSdkTestContext({
                copilotClientOptions: {
                    env: { COPILOT_CLI_ENABLED_FEATURE_FLAGS: "SANDBOX" },
                },
            });
            const vaultDir = join(workDir, "vault");
            await mkdir(vaultDir, { recursive: true });
            await writeFile(join(vaultDir, "notes.txt"), "OUTSIDE_MATCH_LINE bypass-approved\n");

            const permissionRequests: PermissionRequest[] = [];
            let bypassedSearchCompleted = false;
            const session = await client.createSession({
                onPermissionRequest: (request) => {
                    permissionRequests.push(request);
                    return { kind: "approve-once" };
                },
            });
            const update = await session.rpc.options.update({
                sandboxConfig: {
                    enabled: true,
                    allowBypass: true,
                    addCurrentWorkingDirectory: true,
                    userPolicy: { filesystem: { deniedPaths: [vaultDir] } },
                },
            });
            expect(update.success).toBe(true);
            session.on((event) => {
                if (
                    event.type === "tool.execution_complete" &&
                    event.data.toolName === "grep" &&
                    event.data.success &&
                    event.data.sandboxed === false
                ) {
                    bypassedSearchCompleted = true;
                }
            });

            const message = await session.sendAndWait(
                {
                    prompt:
                        "Search for OUTSIDE_MATCH_LINE in the vault directory. " +
                        "After the search succeeds, reply with exactly SANDBOX_BYPASS_APPROVED.",
                },
                SEND_TIMEOUT_MS
            );

            expect(message?.data.content).toContain("SANDBOX_BYPASS_APPROVED");
            expect(
                permissionRequests.some(
                    (request) =>
                        "requestSandboxBypass" in request && request.requestSandboxBypass === true
                )
            ).toBe(true);
            expect(bypassedSearchCompleted).toBe(true);

            await session.disconnect();
        },
        TEST_TIMEOUT_MS
    );
});

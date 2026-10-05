/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { existsSync } from "node:fs";
import { readFile, stat } from "node:fs/promises";
import { join } from "node:path";
import { describe, it } from "vitest";
import { approveAll } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

const TEST_NAME = "creates missing denied directories on the host before a sandboxed shell runs";

describe("Sandbox filesystem", async () => {
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

    it(TEST_NAME, { timeout: 120_000 }, async ({ expect }) => {
        const denied = join(workDir, "vault", ".env");
        expect(existsSync(denied)).toBe(false);
        const session = await client.createSession({ onPermissionRequest: approveAll });
        try {
            const update = await session.rpc.options.update({
                sandboxConfig: {
                    enabled: true,
                    allowBypass: false,
                    addCurrentWorkingDirectory: true,
                    userPolicy: {
                        filesystem: { deniedPaths: ["./vault/.env"] },
                        network: { allowOutbound: false },
                    },
                },
            });
            expect(update.success).toBe(true);
            const result = await session.rpc.shell.executeUserRequested({
                requestId: "denied-directory",
                command:
                    "echo control > control.txt && " +
                    "if echo blocked > vault/.env/probe; then echo SANDBOX_WROTE; " +
                    "else echo SANDBOX_DENIED; fi",
            });
            expect(result.success, result.output).toBe(true);
            expect(result.exitCode, result.output).toBe(0);
            expect(result.output).toContain("SANDBOX_DENIED");
            expect(result.output).not.toContain("SANDBOX_WROTE");
            expect((await stat(denied)).isDirectory()).toBe(true);
            expect(existsSync(join(denied, "probe"))).toBe(false);
            expect((await readFile(join(workDir, "control.txt"), "utf8")).trim()).toBe("control");
        } finally {
            await session.disconnect();
        }
    });
});

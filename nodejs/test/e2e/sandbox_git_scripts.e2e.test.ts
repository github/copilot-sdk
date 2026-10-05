/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { execFileSync } from "node:child_process";
import { readFile, writeFile } from "node:fs/promises";
import { join } from "node:path";
import { describe, it } from "vitest";
import { approveAll } from "../../src/index.js";
import { createSdkTestContext, DEFAULT_GITHUB_TOKEN } from "./harness/sdkTestContext.js";

const TEST_NAME = "gives Python Git subprocesses masked credentials and observes auth changes";

describe("Sandbox Git scripts", async () => {
    if (process.platform !== "darwin") {
        // The SDK sandbox suite currently has a backend only on macOS.
        it.skip(TEST_NAME, () => undefined);
        return;
    }

    const { copilotClient: client, workDir } = await createSdkTestContext({
        copilotClientOptions: {
            gitHubToken: DEFAULT_GITHUB_TOKEN,
            env: {
                COPILOT_CLI_ENABLED_FEATURE_FLAGS: "SANDBOX",
                GH_TOKEN: DEFAULT_GITHUB_TOKEN,
                GITHUB_TOKEN: DEFAULT_GITHUB_TOKEN,
                GITHUB_COPILOT_GITHUB_TOKEN: DEFAULT_GITHUB_TOKEN,
                GIT_CONFIG_GLOBAL: "/dev/null",
                GIT_CONFIG_NOSYSTEM: "1",
                GIT_CONFIG_COUNT: "0",
            },
        },
    });

    it(TEST_NAME, { timeout: 120_000 }, async ({ expect }) => {
        execFileSync("git", ["init", "--quiet"], { cwd: workDir, stdio: "pipe" });
        execFileSync(
            "git",
            ["remote", "add", "origin", "git@github.com:example/private-repo.git"],
            { cwd: workDir, stdio: "pipe" }
        );
        await writeFile(
            join(workDir, "inspect_git.py"),
            [
                "import pathlib, subprocess",
                "config = subprocess.check_output(['git', 'config', '--list'], text=True)",
                "pathlib.Path('child-git-config.txt').write_text(config)",
                "remote = subprocess.check_output(['git', 'remote', 'get-url', 'origin'], text=True)",
                "pathlib.Path('child-git-remote.txt').write_text(remote)",
                "print('GIT_SCRIPT_COMPLETE')",
                "",
            ].join("\n")
        );

        const session = await client.createSession({ onPermissionRequest: approveAll });
        try {
            let previousHeader: string | undefined;
            for (const [index, enabled] of [true, false, true].entries()) {
                const update = await session.rpc.options.update({
                    sandboxConfig: {
                        enabled: true,
                        addCurrentWorkingDirectory: true,
                        auth: { git: enabled, gh: false },
                        userPolicy: { network: { allowOutbound: false } },
                    },
                });
                expect(update.success).toBe(true);
                const result = await session.rpc.shell.executeUserRequested({
                    requestId: `git-script-${index}`,
                    command: "python3 inspect_git.py",
                });
                expect(result.success, result.output).toBe(true);
                expect(result.exitCode, result.output).toBe(0);
                expect(result.output).toContain("GIT_SCRIPT_COMPLETE");

                // Inspect raw child-written bytes, not redacted tool output.
                const config = await readFile(join(workDir, "child-git-config.txt"), "utf8");
                expect(config).not.toContain(DEFAULT_GITHUB_TOKEN);
                expect(config).not.toContain(
                    Buffer.from(`x-access-token:${DEFAULT_GITHUB_TOKEN}`).toString("base64")
                );
                const headers = config
                    .split("\n")
                    .filter((line) =>
                        line.startsWith("http.https://github.com/.extraheader=Authorization: ")
                    );
                if (enabled) {
                    expect(headers).toHaveLength(1);
                    expect(headers[0]).toMatch(/=Authorization: copilot_mask_/);
                    expect(headers[0]).not.toBe(previousHeader);
                    previousHeader = headers[0];
                } else {
                    expect(headers).toEqual([]);
                }
                expect((await readFile(join(workDir, "child-git-remote.txt"), "utf8")).trim()).toBe(
                    enabled
                        ? "https://github.com/example/private-repo.git"
                        : "git@github.com:example/private-repo.git"
                );
            }
        } finally {
            await session.disconnect();
        }
    });
});

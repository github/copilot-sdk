/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { execFileSync } from "node:child_process";
import { readFile, writeFile } from "node:fs/promises";
import { join } from "node:path";
import { describe, it } from "vitest";
import { approveAll } from "../../src/index.js";
import { createSdkTestContext, DEFAULT_GITHUB_TOKEN } from "./harness/sdkTestContext.js";

const TEST_NAME =
    "preserves gh settings and aliases in sandboxed scripts without copying credentials";

describe("Sandbox gh configuration", async () => {
    if (process.platform !== "darwin") {
        // The SDK sandbox suite currently has a backend only on macOS.
        it.skip(TEST_NAME, () => undefined);
        return;
    }

    const {
        copilotClient: client,
        homeDir,
        workDir,
    } = await createSdkTestContext({
        copilotClientOptions: {
            gitHubToken: DEFAULT_GITHUB_TOKEN,
            env: {
                COPILOT_CLI_ENABLED_FEATURE_FLAGS: "SANDBOX",
                GH_TOKEN: DEFAULT_GITHUB_TOKEN,
                GITHUB_TOKEN: DEFAULT_GITHUB_TOKEN,
                GITHUB_COPILOT_GITHUB_TOKEN: DEFAULT_GITHUB_TOKEN,
                GIT_CONFIG_GLOBAL: "/dev/null",
                GIT_CONFIG_NOSYSTEM: "1",
            },
        },
    });

    it(TEST_NAME, { timeout: 120_000 }, async ({ expect }) => {
        execFileSync("git", ["init", "--quiet"], { cwd: workDir, stdio: "pipe" });
        const token = `ghp_${"a".repeat(36)}`;
        const source = join(homeDir, "config.yml");
        const config = (editor: string) =>
            [
                'version: "1"',
                `editor: ${editor}`,
                "pager:",
                "browser:",
                "http_unix_socket:",
                "aliases:",
                `  sandbox-check: ${JSON.stringify(
                    `!case "$GITHUB_TOKEN" in copilot_mask_*) printf '%s\\n' GH_ALIAS_AVAILABLE ;; *) exit 73 ;; esac`
                )}`,
                "  token: auth token",
                "  who: auth token",
                `  embedded-secret: ${JSON.stringify(`!echo ${token}`)}`,
                "hosts:",
                "  github.com:",
                `    oauth_token: ${token}`,
                "",
            ].join("\n");
        await writeFile(source, config("sandbox-editor-one"));
        await writeFile(join(homeDir, "hosts.yml"), `github.com:\n  oauth_token: ${token}\n`);
        await writeFile(
            join(workDir, "release.sh"),
            [
                "set -eu",
                'case "$GH_TOKEN" in copilot_mask_*) ;; *) exit 71 ;; esac',
                'test ! -e "$GH_CONFIG_DIR/hosts.yml"',
                `if cat ${JSON.stringify(join(homeDir, "hosts.yml"))} >/dev/null 2>&1; then exit 72; fi`,
                'cp "$GH_CONFIG_DIR/config.yml" copied-config.yml',
                'printf "%s" "$GH_CONFIG_DIR" > copied-config-dir.txt',
                "gh sandbox-check",
                'test -z "$(gh config get http_unix_socket)"',
                "gh alias list",
                "gh config get editor",
                "printf '%s\\n' GH_SCRIPT_COMPLETE",
                "",
            ].join("\n")
        );

        const session = await client.createSession({ onPermissionRequest: approveAll });
        try {
            const update = await session.rpc.options.update({
                sandboxConfig: {
                    enabled: true,
                    addCurrentWorkingDirectory: true,
                    auth: { git: false, gh: true },
                    userPolicy: {
                        filesystem: { deniedPaths: [homeDir] },
                        network: { allowOutbound: false },
                    },
                },
            });
            expect(update.success).toBe(true);

            let previousDirectory: string | undefined;
            for (const editor of ["sandbox-editor-one", "sandbox-editor-two"]) {
                const original = config(editor);
                await writeFile(source, original);
                const result = await session.rpc.shell.executeUserRequested({
                    requestId: editor,
                    command: "sh release.sh",
                });
                expect(result.success, result.output).toBe(true);
                expect(result.exitCode, result.output).toBe(0);
                expect(result.output).toContain("GH_ALIAS_AVAILABLE");
                expect(result.output).toContain("token: auth token");
                expect(result.output).toContain("who: auth token");
                expect(result.output).toContain(editor);
                expect(result.output).toContain("GH_SCRIPT_COMPLETE");

                // Read raw child-written bytes, not secret-filtered tool output.
                const copied = await readFile(join(workDir, "copied-config.yml"), "utf8");
                expect(copied).not.toContain(token);
                expect(copied).not.toContain("oauth_token");
                expect(copied).not.toContain("hosts:");
                expect(copied).toContain("sandbox-check");
                expect(await readFile(source, "utf8")).toBe(original);
                const directory = await readFile(join(workDir, "copied-config-dir.txt"), "utf8");
                expect(directory).not.toBe(homeDir);
                expect(directory).not.toBe(previousDirectory);
                previousDirectory = directory;

                const direct = await session.rpc.shell.executeUserRequested({
                    requestId: `${editor}-direct-alias`,
                    command: "gh sandbox-check",
                });
                expect(direct.success, direct.output).toBe(true);
                expect(direct.exitCode, direct.output).toBe(0);
                expect(direct.output).toContain("GH_ALIAS_AVAILABLE");
            }
        } finally {
            await session.disconnect();
        }
    });
});

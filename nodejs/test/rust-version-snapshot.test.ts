/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { spawnSync } from "node:child_process";
import { mkdtempSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { describe, expect, it, onTestFinished } from "vitest";

// Windows needs Git Bash's path conversion, not the WSL bash executable.
const bash =
    process.platform === "win32"
        ? join(process.env.ProgramFiles ?? "C:\\Program Files", "Git", "bin", "bash.exe")
        : "bash";

describe.each(["snapshot-bundled-cli-version.sh", "snapshot-bundled-in-process-version.sh"])(
    "%s",
    (scriptName) => {
        it.each(["copilot-snapshot-", "copilot sdk's snapshot-"])(
            "reads the version under %s",
            (prefix) => {
                const directory = mkdtempSync(join(tmpdir(), prefix));
                onTestFinished(() => rmSync(directory, { recursive: true, force: true }));
                const packageFile = join(directory, "package.json");
                writeFileSync(packageFile, JSON.stringify({ copilotCliVersion: "1.2.3" }));

                const script = readFileSync(
                    new URL(`../../rust/scripts/${scriptName}`, import.meta.url),
                    "utf8"
                );
                const versionAssignment = script.match(/^VERSION=.*$/m)?.[0];
                expect(versionAssignment).toBeDefined();

                const result = spawnSync(
                    bash,
                    [
                        "-euc",
                        [
                            process.platform === "win32"
                                ? 'PACKAGE_FILE="$(cygpath -u "$1")"'
                                : 'PACKAGE_FILE="$1"',
                            versionAssignment,
                            'printf "%s" "$VERSION"',
                        ].join("\n"),
                        "version-test",
                        packageFile,
                    ],
                    { encoding: "utf8", timeout: 10000 }
                );

                expect(result.error).toBeUndefined();
                expect(result.status, result.stderr).toBe(0);
                expect(result.stdout).toBe("1.2.3");
            }
        );
    }
);

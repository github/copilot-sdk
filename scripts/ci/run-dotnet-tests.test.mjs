/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

const script = fileURLToPath(new URL("./run-dotnet-tests.sh", import.meta.url));

function fromBashPath(value) {
    if (process.platform === "win32" && /^\/[a-z]\//i.test(value)) {
        return `${value[1].toUpperCase()}:${value.slice(2)}`;
    }
    return value;
}

await test("runs all .NET tests and forwards only explicit filter and runtime", (t) => {
    const root = fs.mkdtempSync(path.join(os.tmpdir(), "sdk-dotnet-test-"));
    t.after(() => fs.rmSync(root, { recursive: true, force: true }));
    const argumentsPath = path.join(root, "arguments");
    const workingDirectoryPath = path.join(root, "working-directory");
    const fakeDotnet = path.join(root, "dotnet");
    fs.writeFileSync(
        fakeDotnet,
        `#!/usr/bin/env bash\nprintf '%s\\n' "$@" > "$ARGUMENTS_PATH"\nprintf '%s\\n' "$PWD" > "$WORKING_DIRECTORY_PATH"\n`,
    );
    fs.chmodSync(fakeDotnet, 0o755);

    for (const [filter, runtime] of [
        ["", ""],
        ["E2EBackend!=CapiOnly", "win-x64"],
    ]) {
        const result = spawnSync("bash", [script], {
            encoding: "utf8",
            env: {
                ...process.env,
                ARGUMENTS_PATH: argumentsPath,
                DOTNET_TEST_FILTER: filter,
                DOTNET_TEST_RUNTIME: runtime,
                PATH: `${root}${path.delimiter}${process.env.PATH ?? ""}`,
                WORKING_DIRECTORY_PATH: workingDirectoryPath,
            },
        });

        assert.equal(result.status, 0, result.stderr);
        assert.equal(
            fs.realpathSync(fromBashPath(fs.readFileSync(workingDirectoryPath, "utf8").trim())),
            fs.realpathSync(fileURLToPath(new URL("../../dotnet", import.meta.url))),
        );
        const arguments_ = fs.readFileSync(argumentsPath, "utf8").trim().split("\n");
        assert.deepEqual(arguments_.slice(0, 3), ["test", "test/GitHub.Copilot.SDK.Test.csproj", "--no-build"]);
        assert.equal(arguments_.includes("--filter"), filter !== "");
        if (filter) {
            assert.equal(arguments_[arguments_.indexOf("--filter") + 1], filter);
        }
        assert.equal(arguments_.includes("--runtime"), runtime !== "");
        if (runtime) {
            assert.equal(arguments_[arguments_.indexOf("--runtime") + 1], runtime);
        }
    }
});

await test("documents its environment and rejects positional arguments", () => {
    const help = spawnSync("bash", [script, "--help"], { encoding: "utf8" });
    assert.equal(help.status, 0, help.stderr);
    assert.match(help.stdout, /DOTNET_TEST_FILTER/);
    assert.match(help.stdout, /DOTNET_TEST_RUNTIME/);

    const invalid = spawnSync("bash", [script, "unexpected"], { encoding: "utf8" });
    assert.equal(invalid.status, 2);
    assert.match(invalid.stderr, /Usage: run-dotnet-tests\.sh/);
});

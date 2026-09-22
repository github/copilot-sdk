/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/** Verifies .NET test shard selection and filter composition. */

import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

const script = fileURLToPath(new URL("./run-dotnet-tests.sh", import.meta.url));

await test("constructs a focused filter for named .NET shards", (t) => {
    const root = fs.mkdtempSync(path.join(os.tmpdir(), "sdk-dotnet-shard-"));
    t.after(() => fs.rmSync(root, { recursive: true, force: true }));
    const argumentsPath = path.join(root, "arguments");
    const workingDirectoryPath = path.join(root, "working-directory");
    const fakeDotnet = path.join(root, "dotnet");
    fs.writeFileSync(
        fakeDotnet,
        `#!/usr/bin/env bash\nprintf '%s\\n' "$@" > "$ARGUMENTS_PATH"\nprintf '%s\\n' "$PWD" > "$WORKING_DIRECTORY_PATH"\n`,
    );
    fs.chmodSync(fakeDotnet, 0o755);

    const result = spawnSync("bash", [script], {
        encoding: "utf8",
        env: {
            ...process.env,
            ARGUMENTS_PATH: argumentsPath,
            DOTNET_TEST_FILTER: "E2EBackend!=CapiOnly",
            DOTNET_TEST_SHARD: "2b-rpc-agent",
            PATH: `${root}${path.delimiter}${process.env.PATH ?? ""}`,
            WORKING_DIRECTORY_PATH: workingDirectoryPath,
        },
    });

    assert.equal(result.status, 0, result.stderr);
    assert.equal(
        fs.realpathSync(fs.readFileSync(workingDirectoryPath, "utf8").trim()),
        fs.realpathSync(fileURLToPath(new URL("../../dotnet", import.meta.url))),
    );
    const arguments_ = fs.readFileSync(argumentsPath, "utf8").trim().split("\n");
    assert.deepEqual(arguments_.slice(0, 3), [
        "test",
        "test/GitHub.Copilot.SDK.Test.csproj",
        "--no-build",
    ]);
    const filterIndex = arguments_.indexOf("--filter");
    assert.notEqual(filterIndex, -1);
    assert.equal(
        arguments_[filterIndex + 1],
        "(E2EBackend!=CapiOnly)&(FullyQualifiedName~GitHub.Copilot.Test.E2E.RpcAgentE2ETests)",
    );
});

await test("rejects unknown .NET shards before invoking dotnet", () => {
    const result = spawnSync("bash", [script], {
        encoding: "utf8",
        env: { ...process.env, DOTNET_TEST_SHARD: "unknown" },
    });

    assert.equal(result.status, 2);
    assert.match(result.stderr, /Unknown \.NET test shard: unknown/);
});

await test("constructs the extensions shard without initial buckets", (t) => {
    const root = fs.mkdtempSync(path.join(os.tmpdir(), "sdk-dotnet-extensions-"));
    t.after(() => fs.rmSync(root, { recursive: true, force: true }));
    const argumentsPath = path.join(root, "arguments");
    const fakeDotnet = path.join(root, "dotnet");
    fs.writeFileSync(fakeDotnet, `#!/usr/bin/env bash\nprintf '%s\\n' "$@" > "$ARGUMENTS_PATH"\n`);
    fs.chmodSync(fakeDotnet, 0o755);

    const result = spawnSync("bash", [script], {
        encoding: "utf8",
        env: {
            ...process.env,
            ARGUMENTS_PATH: argumentsPath,
            DOTNET_TEST_SHARD: "extensions",
            PATH: `${root}${path.delimiter}${process.env.PATH ?? ""}`,
        },
    });

    assert.equal(result.status, 0, result.stderr);
    const arguments_ = fs.readFileSync(argumentsPath, "utf8").trim().split("\n");
    const filterIndex = arguments_.indexOf("--filter");
    assert.equal(
        arguments_[filterIndex + 1],
        "(FullyQualifiedName~GitHub.Copilot.Test.E2E.RpcExtensionsLoadedE2ETests)",
    );
});

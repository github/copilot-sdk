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

await test("runtime artifact tests preserve the enclosing job's environment and artifacts", (t) => {
    const root = fs.mkdtempSync(path.join(os.tmpdir(), "sdk-runtime-artifact-environment-"));
    t.after(() => fs.rmSync(root, { recursive: true, force: true }));
    const environmentFile = path.join(root, "environment");
    const original = "EXISTING_JOB_SETTING=preserved\n";
    fs.writeFileSync(environmentFile, original);
    const outputDirectory = path.join(root, "job-runtime");
    fs.mkdirSync(outputDirectory);
    fs.writeFileSync(path.join(outputDirectory, "existing-artifact"), "preserved");
    const environment = {
        ...process.env,
        GITHUB_ENV: environmentFile,
        COPILOT_RUNTIME_OUTPUT_DIRECTORY: outputDirectory,
    };
    // Start an independent runner rather than inheriting this test worker's context.
    delete environment.NODE_TEST_CONTEXT;

    const result = spawnSync(
        process.execPath,
        ["--test", fileURLToPath(new URL("./runtime-artifact.test.mjs", import.meta.url))],
        {
            encoding: "utf8",
            env: environment,
        },
    );

    assert.equal(result.status, 0, result.stdout + result.stderr);
    assert.match(result.stdout, /stages all same-checkout runtime inputs/);
    assert.equal(fs.readFileSync(environmentFile, "utf8"), original);
    assert.deepEqual(fs.readdirSync(outputDirectory), ["existing-artifact"]);
    assert.equal(fs.readFileSync(path.join(outputDirectory, "existing-artifact"), "utf8"), "preserved");
});

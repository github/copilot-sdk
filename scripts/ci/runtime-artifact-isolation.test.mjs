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

await test("runtime artifact fixtures do not modify the inherited Actions environment file", (t) => {
    const root = fs.mkdtempSync(path.join(os.tmpdir(), "sdk-runtime-artifact-isolation-"));
    t.after(() => fs.rmSync(root, { recursive: true, force: true }));
    const environmentFile = path.join(root, "environment");
    const originalContents = "EXISTING_VALUE=preserved\n";
    fs.writeFileSync(environmentFile, originalContents);

    const result = spawnSync(
        process.execPath,
        [fileURLToPath(new URL("./runtime-artifact.test.mjs", import.meta.url))],
        {
            encoding: "utf8",
            env: { ...process.env, GITHUB_ENV: environmentFile },
            timeout: 30_000,
        },
    );

    assert.equal(result.error, undefined);
    assert.equal(result.status, 0, result.stdout + result.stderr);
    assert.equal(fs.readFileSync(environmentFile, "utf8"), originalContents);
});

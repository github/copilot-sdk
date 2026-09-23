/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import assert from "node:assert/strict";
import test from "node:test";

import { getTaskSteps } from "../run-tasks.mjs";

test("leaves generated Rust formatting to the generator", () => {
    const rustSteps = getTaskSteps("generate", "rust");
    assert.deepEqual(rustSteps, [
        {
            language: "rust",
            cwd: "scripts/codegen",
            executable: "npm",
            args: ["run", "generate:rust"],
        },
    ]);
    assert.deepEqual(
        getTaskSteps("generate").filter((step) => step.language === "rust"),
        rustSteps,
    );
});

test("retains the explicit SDK Rust formatting command", () => {
    assert.deepEqual(getTaskSteps("format", "rust"), [
        {
            language: "rust",
            cwd: "rust",
            executable: "cargo",
            args: ["+nightly-2026-04-14", "fmt", "--all", "--", "--config-path", ".rustfmt.nightly.toml"],
        },
    ]);
});

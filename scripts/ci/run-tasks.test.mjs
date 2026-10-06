/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import assert from "node:assert/strict";
import childProcess from "node:child_process";
import fs from "node:fs";
import { syncBuiltinESMExports } from "node:module";
import path from "node:path";
import test from "node:test";
import { pathToFileURL } from "node:url";

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

test("default build selects the existing Node and Rust build steps in order", () => {
    assert.deepEqual(getTaskSteps("build:default"), [
        ...getTaskSteps("build", "nodejs"),
        ...getTaskSteps("build", "rust"),
    ]);
    assert.throws(() => getTaskSteps("build:default", "java"), /Unknown SDK task/);
});

test("checkout default build prepares both projections once without rebuilding the CLI", async (t) => {
    const { runTasks, preparations, calls, sdkRoot, root } = await taskFixture(t, true);

    runTasks("build:default", undefined, { environment: {}, runtimeSource: "checkout" });

    assert.deepEqual(preparations, [{ languages: ["nodejs", "rust"], runtimeRoot: root, sdkRoot }]);
    assert.deepEqual(calls, [
        { executable: "npm", args: ["ci", "--ignore-scripts", "--include=dev"], cwd: path.join(sdkRoot, "nodejs") },
        { executable: "npm", args: ["run", "build"], cwd: path.join(sdkRoot, "nodejs") },
        {
            executable: process.platform === "win32" ? "pnpm.cmd" : "pnpm",
            args: ["bazel", "build", "--action_env=PATH", "//src/sdk/rust:github-copilot-sdk"],
            cwd: root,
        },
    ]);

    calls.length = 0;
    runTasks("build:default", undefined, { environment: {}, runtimeSource: "checkout" });
    assert.equal(preparations.length, 2);
    assert.equal(calls.length, 2);
    assert.deepEqual(calls[0].args, ["run", "build"]);
});

test("standalone default build retains the all-features Cargo build", async (t) => {
    const { runTasks, preparations, calls, sdkRoot } = await taskFixture(t, false);

    runTasks("build:default", undefined, { environment: { COPILOT_RUNTIME_SOURCE: "published" } });

    assert.deepEqual(preparations, []);
    assert.deepEqual(calls, [
        { executable: "npm", args: ["ci", "--ignore-scripts", "--include=dev"], cwd: path.join(sdkRoot, "nodejs") },
        { executable: "npm", args: ["run", "build"], cwd: path.join(sdkRoot, "nodejs") },
        { executable: "cargo", args: ["build", "--all-features"], cwd: path.join(sdkRoot, "rust") },
    ]);
});

for (const language of ["nodejs", "rust"]) {
    test(`checkout ${language} build still prepares only its selected projection`, async (t) => {
        const { runTasks, preparations, calls, sdkRoot, root } = await taskFixture(t, true);

        runTasks("build", language, { environment: {}, runtimeSource: "checkout" });

        assert.deepEqual(preparations, [{ languages: [language], runtimeRoot: root, sdkRoot }]);
        assert.equal(calls.length, language === "nodejs" ? 2 : 1);
        assert.ok(calls.every(({ cwd }) => cwd === (language === "nodejs" ? path.join(sdkRoot, language) : root)));
    });
}

for (const checkout of [true, false]) {
    test(`Java docs install once through the Maven wrapper before validation (${checkout ? "checkout" : "standalone"})`, async (t) => {
        const { runTasks, preparations, calls, sdkRoot } = await taskFixture(t, checkout);

        runTasks("docs", "java", { environment: {}, runtimeSource: checkout ? "checkout" : "published" });

        assert.deepEqual(preparations, []);
        assert.deepEqual(calls, [
            { executable: "npm", args: ["run", "extract"], cwd: path.join(sdkRoot, "scripts/docs-validation") },
            {
                executable: process.platform === "win32" ? "mvnw.cmd" : "./mvnw",
                args: ["install", "-Dmaven.test.skip=true", "-Dskip.test.harness=true", "-Dcopilot.native.skip.download=true"],
                cwd: path.join(sdkRoot, "java"),
            },
            { executable: "npm", args: ["run", "validate:java"], cwd: path.join(sdkRoot, "scripts/docs-validation") },
        ]);
    });
}

async function taskFixture(t, checkout) {
    const root = fs.mkdtempSync(path.join(import.meta.dirname, ".run-tasks-"));
    t.after(() => fs.rmSync(root, { recursive: true, force: true }));
    const sdkRoot = path.join(root, checkout ? "src/sdk" : "sdk");
    const scripts = path.join(sdkRoot, "scripts");
    fs.mkdirSync(scripts, { recursive: true });
    for (const name of ["run-tasks.mjs", "runtime-layout.mjs", "install-dependencies.mjs"]) {
        fs.copyFileSync(new URL(`../${name}`, import.meta.url), path.join(scripts, name));
    }
    // Observe the prerequisite boundary without invoking Bazel or changing real generated files.
    fs.writeFileSync(
        path.join(scripts, "build-prerequisites.mjs"),
        `export const preparations = [];
export function prepareSdkSources(options) { preparations.push(options); }
export function bazelActionEnvironmentArgument() { return "--action_env=PATH"; }
`,
    );
    fs.mkdirSync(path.join(sdkRoot, "nodejs"));
    fs.writeFileSync(path.join(sdkRoot, "nodejs/package.json"), "{}");
    fs.writeFileSync(path.join(sdkRoot, "nodejs/package-lock.json"), "{}");
    if (checkout) {
        fs.mkdirSync(path.join(root, "script"));
        fs.writeFileSync(path.join(root, "script/sea-build.ts"), "");
    }
    const calls = [];
    const spawn = t.mock.method(childProcess, "spawnSync", (executable, args, { cwd }) => {
        calls.push({ executable, args, cwd });
        return { status: 0 };
    });
    syncBuiltinESMExports();
    t.after(() => {
        spawn.mock.restore();
        syncBuiltinESMExports();
    });
    const { runTasks } = await import(pathToFileURL(path.join(scripts, "run-tasks.mjs")));
    const { preparations } = await import(pathToFileURL(path.join(scripts, "build-prerequisites.mjs")));
    return { runTasks, preparations, calls, sdkRoot, root };
}

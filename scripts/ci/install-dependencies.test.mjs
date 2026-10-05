/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import { createHash } from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import test from "node:test";

import { installNpmDependencies } from "../install-dependencies.mjs";
import { npmInvocation } from "../build-prerequisites.mjs";

test("installs once and fingerprints the install arguments and manifests", (t) => {
    const { directory, stampFile } = fixture(t);
    const calls = [];
    const runNpm = (args, cwd) => calls.push({ args, cwd });

    assert.equal(installNpmDependencies(directory, runNpm), true);
    assert.deepEqual(calls, [{ args: ["ci", "--ignore-scripts", "--include=dev"], cwd: directory }]);
    const expectedFingerprint = createHash("sha256")
        .update(JSON.stringify(["ci", "--ignore-scripts", "--include=dev"]))
        .update(fs.readFileSync(path.join(directory, "package-lock.json")))
        .update(fs.readFileSync(path.join(directory, "package.json")))
        .digest("hex");
    assert.equal(fs.readFileSync(stampFile, "utf8"), `${expectedFingerprint}\n`);

    assert.equal(installNpmDependencies(directory, runNpm), false);
    assert.equal(calls.length, 1);
});

for (const file of ["package.json", "package-lock.json"]) {
    test(`reinstalls when ${file} changes`, (t) => {
        const { directory, stampFile } = fixture(t);
        const calls = [];
        const runNpm = (args) => calls.push(args);
        installNpmDependencies(directory, runNpm);
        const previousStamp = fs.readFileSync(stampFile, "utf8");
        fs.appendFileSync(path.join(directory, file), "\n");

        assert.equal(installNpmDependencies(directory, runNpm), true);
        assert.equal(calls.length, 2);
        assert.notEqual(fs.readFileSync(stampFile, "utf8"), previousStamp);
    });
}

for (const missing of ["stamp", "node_modules"]) {
    test(`reinstalls when ${missing} is missing`, (t) => {
        const { directory, stampFile } = fixture(t);
        installNpmDependencies(directory, () => {});
        fs.rmSync(missing === "stamp" ? stampFile : path.dirname(stampFile), { recursive: true });

        const calls = [];
        assert.equal(
            installNpmDependencies(directory, (args) => calls.push(args)),
            true,
        );
        assert.deepEqual(calls, [["ci", "--ignore-scripts", "--include=dev"]]);
        assert.equal(fs.existsSync(stampFile), true);
    });
}

test("removes the old stamp before installing and leaves it absent after failure", (t) => {
    const { directory, stampFile } = fixture(t);
    installNpmDependencies(directory, () => {});
    fs.appendFileSync(path.join(directory, "package-lock.json"), "\n");
    const failure = new Error("npm ci failed");
    let attempts = 0;

    assert.throws(
        () =>
            installNpmDependencies(directory, () => {
                attempts++;
                assert.equal(fs.existsSync(stampFile), false);
                throw failure;
            }),
        (error) => error === failure,
    );
    assert.equal(attempts, 1);
    assert.equal(fs.existsSync(stampFile), false);
    assert.equal(
        installNpmDependencies(directory, () => {}),
        true,
    );
    assert.equal(fs.existsSync(stampFile), true);
});

test("surfaces unexpected stamp read errors without attempting an install", (t) => {
    const { directory, stampFile } = fixture(t);
    fs.mkdirSync(stampFile, { recursive: true });
    let installed = false;

    assert.throws(
        () =>
            installNpmDependencies(directory, () => {
                installed = true;
            }),
        { code: "EISDIR" },
    );
    assert.equal(installed, false);
});

for (const environment of [{ NODE_ENV: "production" }, { npm_config_omit: "dev" }]) {
    test(`installs build tools despite ${JSON.stringify(environment)} and reuses them on a normal retry`, (t) => {
        const { directory, runNpm, buildTool } = npmFixture(t);

        assert.equal(
            installNpmDependencies(directory, (args, cwd) => runNpm(args, cwd, environment)),
            true,
        );
        assert.equal(fs.existsSync(buildTool), true);
        assert.equal(installNpmDependencies(directory, runNpm), false);
        assert.equal(fs.existsSync(buildTool), true);
    });
}

test("repairs a production-only install recorded by the old manifest-only stamp", (t) => {
    const { directory, stampFile, runNpm, buildTool } = npmFixture(t);
    runNpm(["ci", "--ignore-scripts"], directory, { NODE_ENV: "production" });
    assert.equal(fs.existsSync(buildTool), false);
    const legacyFingerprint = createHash("sha256")
        .update(fs.readFileSync(path.join(directory, "package-lock.json")))
        .update(fs.readFileSync(path.join(directory, "package.json")))
        .digest("hex");
    fs.mkdirSync(path.dirname(stampFile), { recursive: true });
    fs.writeFileSync(stampFile, `${legacyFingerprint}\n`);

    assert.equal(installNpmDependencies(directory, runNpm), true);
    assert.equal(fs.existsSync(buildTool), true);
    assert.equal(installNpmDependencies(directory, runNpm), false);
});

function npmFixture(t) {
    const fixturePaths = fixture(t);
    const { directory } = fixturePaths;
    fs.mkdirSync(path.join(directory, "build-tool"));
    fs.writeFileSync(
        path.join(directory, "build-tool/package.json"),
        JSON.stringify({ name: "fixture-build-tool", version: "1.0.0" }),
    );
    fs.writeFileSync(
        path.join(directory, "package.json"),
        JSON.stringify({ name: "fixture", devDependencies: { "fixture-build-tool": "file:./build-tool" } }),
    );
    const environment = Object.fromEntries(
        Object.entries(process.env).filter(([name]) => name !== "NODE_ENV" && !/^npm_config_/i.test(name)),
    );
    const userConfig = path.join(directory, "user.npmrc");
    const globalConfig = path.join(directory, "global.npmrc");
    fs.writeFileSync(userConfig, "");
    fs.writeFileSync(globalConfig, "");
    const invocation = npmInvocation();
    const runNpm = (args, cwd, overrides = {}) => {
        const result = spawnSync(
            invocation.command,
            [
                ...invocation.args,
                ...args,
                "--offline",
                "--no-audit",
                "--no-fund",
                `--userconfig=${userConfig}`,
                `--globalconfig=${globalConfig}`,
                `--cache=${path.join(directory, "cache")}`,
            ],
            { cwd, env: { ...environment, ...overrides }, encoding: "utf8", timeout: 30_000 },
        );
        assert.equal(result.status, 0, result.error?.message ?? `${result.stdout}\n${result.stderr}`);
    };
    runNpm(["install", "--package-lock-only", "--ignore-scripts"], directory);
    return {
        ...fixturePaths,
        runNpm,
        buildTool: path.join(directory, "node_modules/fixture-build-tool/package.json"),
    };
}

function fixture(t) {
    const directory = fs.mkdtempSync(path.join(import.meta.dirname, ".install-dependencies-"));
    t.after(() => fs.rmSync(directory, { recursive: true, force: true }));
    fs.writeFileSync(path.join(directory, "package.json"), '{"name":"fixture"}\n');
    fs.writeFileSync(path.join(directory, "package-lock.json"), '{"lockfileVersion":3}\n');
    return { directory, stampFile: path.join(directory, "node_modules/.copilot-sdk-install-stamp") };
}

/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/** Verifies runtime artifact selection, packaging, restoration, and schema staging. */

import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

import { prepareRuntimeArtifact, restoreRuntimeArtifact, stageRuntimeSchemas } from "./runtime-artifact.mjs";

await test("stages all same-checkout runtime inputs", (t) => {
    const root = fs.mkdtempSync(path.join(os.tmpdir(), "sdk-runtime-artifact-"));
    t.after(() => fs.rmSync(root, { recursive: true, force: true }));

    const runtimeRoot = path.join(root, "runtime");
    const sdkRoot = path.join(runtimeRoot, "src", "sdk");
    const outputDirectory = path.join(root, "output");
    const environmentFile = path.join(root, "environment");
    const target = `${process.platform}-${process.arch}`;
    const executableName = process.platform === "win32" ? "copilot.exe" : "copilot";

    const wrapperName = process.platform === "win32" ? "copilot-runtime.exe" : "copilot-runtime";
    writeFixture(path.join(runtimeRoot, "dist-cli", "index.js"), "entry point");
    writeFixture(path.join(runtimeRoot, "dist-cli", "app.js"), "legacy entry point");
    writeFixture(path.join(runtimeRoot, "dist-cli", "runtime-asset"), "asset");
    writeFixture(path.join(runtimeRoot, "dist-cli", "copilot-sdk", "index.js"), "sdk entry point");
    writeFixture(path.join(runtimeRoot, "dist-cli", "copilot-sdk", "extension.js"), "sdk extension entry point");
    writeFixture(path.join(runtimeRoot, "dist-bin", target, executableName), "executable");
    writeFixture(path.join(runtimeRoot, "dist-cli", "prebuilds", target, "runtime.node"), "runtime");
    writeFixture(path.join(runtimeRoot, "dist-cli", "prebuilds", target, wrapperName), "wrapper");
    fs.mkdirSync(path.join(sdkRoot, "rust"), { recursive: true });

    const values = prepareRuntimeArtifact({ runtimeRoot, sdkRoot, outputDirectory, environmentFile });

    assert.equal(fs.readFileSync(values.COPILOT_RUNTIME_BINARY_PATH, "utf8"), "executable");
    assert.equal(fs.readFileSync(values.COPILOT_RUNTIME_LIBRARY_PATH, "utf8"), "runtime");
    assert.equal(fs.readFileSync(values.COPILOT_CLI_PATH, "utf8"), "wrapper");
    assert.equal(fs.readFileSync(values.COPILOT_LEGACY_CLI_PATH, "utf8"), "legacy entry point");
    assert.equal(
        fs.readFileSync(path.join(values.COPILOT_EXTENSION_SDK_PATH, "extension.js"), "utf8"),
        "sdk extension entry point",
    );
    assert.equal(fs.readFileSync(path.join(outputDirectory, "package", "runtime-asset"), "utf8"), "asset");
    assert.match(fs.readFileSync(environmentFile, "utf8"), /^COPILOT_CLI_PATH=.*$/m);
    assert.match(fs.readFileSync(environmentFile, "utf8"), /^COPILOT_LEGACY_CLI_PATH=.*$/m);
    assert.match(fs.readFileSync(environmentFile, "utf8"), /^COPILOT_SKIP_CLI_DOWNLOAD=1$/m);
    assert.match(fs.readFileSync(environmentFile, "utf8"), /^COPILOT_EXTENSION_SDK_PATH=.*$/m);

    assert.equal(fs.statSync(values.BUNDLED_CLI_CACHE_DIR).isDirectory(), true);
    assert.equal(fs.existsSync(path.join(sdkRoot, "rust/cli-version.txt")), false);
    assert.equal(fs.existsSync(path.join(sdkRoot, "rust/cli-version-in-process.txt")), false);
    assert.match(values.COPILOT_CLI_RELEASE_SHA256, /^[a-f0-9]{64}$/);

    const tarCommand = process.platform === "win32" ? "tar.exe" : "tar";
    const inProcessContents = spawnSync(
        tarCommand,
        ["-tzf", path.join(values.BUNDLED_CLI_CACHE_DIR, `v0.0.0-sdk-ci-github-copilot-0.0.0-sdk-ci-${target}.tgz`)],
        { encoding: "utf8" },
    );
    assert.equal(inProcessContents.status, 0, inProcessContents.stderr);
    assert.match(inProcessContents.stdout, new RegExp(`package/prebuilds/${target}/runtime\\.node`));
    assert.doesNotMatch(inProcessContents.stdout, new RegExp(`^package/${executableName}$`, "m"));
    assert.doesNotMatch(inProcessContents.stdout, /^package\/app\.js$/m);
});

await test("preserves complete checked-in Rust release pins", (t) => {
    const root = fs.mkdtempSync(path.join(os.tmpdir(), "sdk-runtime-artifact-pins-"));
    t.after(() => fs.rmSync(root, { recursive: true, force: true }));

    const runtimeRoot = path.join(root, "runtime");
    const sdkRoot = path.join(runtimeRoot, "src", "sdk");
    const target = `${process.platform}-${process.arch}`;
    writeRuntimeFixture(runtimeRoot, target);
    const rustDirectory = path.join(sdkRoot, "rust");
    fs.mkdirSync(rustDirectory, { recursive: true });
    const releasePin = "version=1.2.3\nrelease-platform=" + "a".repeat(64) + "\n";
    fs.writeFileSync(path.join(rustDirectory, "cli-version.txt"), releasePin);
    fs.writeFileSync(path.join(rustDirectory, "cli-version-in-process.txt"), releasePin);

    prepareRuntimeArtifact({
        runtimeRoot,
        sdkRoot,
        target,
        outputDirectory: path.join(root, "output"),
        environmentFile: undefined,
    });

    assert.equal(fs.readFileSync(path.join(rustDirectory, "cli-version.txt"), "utf8"), releasePin);
    assert.equal(fs.readFileSync(path.join(rustDirectory, "cli-version-in-process.txt"), "utf8"), releasePin);
});

await test("selects an explicit musl target without falling back to the GNU artifact", (t) => {
    const root = fs.mkdtempSync(path.join(os.tmpdir(), "sdk-runtime-artifact-musl-"));
    t.after(() => fs.rmSync(root, { recursive: true, force: true }));

    const runtimeRoot = path.join(root, "runtime");
    const sdkRoot = path.join(runtimeRoot, "src", "sdk");
    const outputDirectory = path.join(root, "output");
    const target = "linuxmusl-arm64";
    writeRuntimeFixture(runtimeRoot, target);
    fs.mkdirSync(path.join(sdkRoot, "rust"), { recursive: true });

    const values = prepareRuntimeArtifact({
        runtimeRoot,
        sdkRoot,
        target,
        outputDirectory,
        environmentFile: undefined,
    });

    assert.match(values.COPILOT_CLI_PATH, /prebuilds\/linuxmusl-arm64\/copilot-runtime$/);
    assert.match(values.COPILOT_CLI_RELEASE_TARBALL, /linuxmusl-arm64\.tgz$/);
    assert.equal(
        fs.existsSync(
            path.join(values.BUNDLED_CLI_CACHE_DIR, "v0.0.0-sdk-ci-copilot-linuxmusl-arm64.tar.gz"),
        ),
        true,
    );
    assert.equal(
        fs.existsSync(path.join(values.BUNDLED_CLI_CACHE_DIR, "v0.0.0-sdk-ci-copilot-linux-arm64.tar.gz")),
        false,
    );
});

await test("rejects a GNU artifact when a musl target is requested", (t) => {
    const root = fs.mkdtempSync(path.join(os.tmpdir(), "sdk-runtime-artifact-mismatch-"));
    t.after(() => fs.rmSync(root, { recursive: true, force: true }));

    const runtimeRoot = path.join(root, "runtime");
    writeRuntimeFixture(runtimeRoot, "linux-arm64");

    assert.throws(
        () =>
            prepareRuntimeArtifact({
                runtimeRoot,
                sdkRoot: path.join(runtimeRoot, "src/sdk"),
                target: "linuxmusl-arm64",
                outputDirectory: path.join(root, "out"),
                environmentFile: undefined,
            }),
        /dist-bin[/\\]linuxmusl-arm64[/\\]copilot/,
    );
});

await test("accepts explicit target and output routing through the CLI environment", (t) => {
    const root = fs.mkdtempSync(path.join(os.tmpdir(), "sdk-runtime-artifact-cli-"));
    t.after(() => fs.rmSync(root, { recursive: true, force: true }));

    const runtimeRoot = path.join(root, "runtime");
    const sdkRoot = path.join(runtimeRoot, "src/sdk");
    const script = path.join(sdkRoot, "scripts/ci/runtime-artifact.mjs");
    const outputDirectory = path.join(root, "musl-output");
    const environmentFile = path.join(root, "musl-environment");
    fs.mkdirSync(path.dirname(script), { recursive: true });
    fs.copyFileSync(fileURLToPath(new URL("./runtime-artifact.mjs", import.meta.url)), script);
    fs.mkdirSync(path.join(sdkRoot, "rust"), { recursive: true });
    writeRuntimeFixture(runtimeRoot, "linuxmusl-arm64");

    const result = spawnSync(process.execPath, [script, "prepare"], {
        encoding: "utf8",
        env: {
            ...process.env,
            COPILOT_RUNTIME_OUTPUT_DIRECTORY: outputDirectory,
            COPILOT_RUNTIME_TARGET: "linuxmusl-arm64",
            GITHUB_ENV: environmentFile,
        },
    });

    assert.equal(result.status, 0, result.stderr);
    assert.match(
        fs.readFileSync(environmentFile, "utf8"),
        new RegExp(
            `^COPILOT_CLI_PATH=${escapeRegExp(path.join(outputDirectory, "package/prebuilds/linuxmusl-arm64/copilot-runtime"))}$`,
            "m",
        ),
    );
});

await test("selects a complete runtime schema directory for generators", (t) => {
    const root = fs.mkdtempSync(path.join(os.tmpdir(), "sdk-runtime-schemas-"));
    t.after(() => fs.rmSync(root, { recursive: true, force: true }));
    writeFixture(path.join(root, "generated/session-events.schema.json"), '{"title":"events"}');
    writeFixture(path.join(root, "generated/api.schema.json"), '{"title":"api"}');

    const environmentFile = path.join(root, "environment");
    const schemaDirectory = stageRuntimeSchemas({
        environmentFile,
        schemaDirectory: path.join(root, "generated"),
    });

    assert.equal(schemaDirectory, path.join(root, "generated"));
    assert.match(
        fs.readFileSync(environmentFile, "utf8"),
        new RegExp(`^COPILOT_CLI_SCHEMA_DIR=${escapeRegExp(schemaDirectory)}$`, "m"),
    );
    assert.match(fs.readFileSync(environmentFile, "utf8"), /^COPILOT_RUNTIME_SOURCE=checkout$/m);
});

await test("rejects incomplete or invalid runtime schemas without staging partial inputs", (t) => {
    const root = fs.mkdtempSync(path.join(os.tmpdir(), "sdk-runtime-schemas-invalid-"));
    t.after(() => fs.rmSync(root, { recursive: true, force: true }));
    const schemaDirectory = path.join(root, "generated");
    const environmentFile = path.join(root, "environment");
    writeFixture(path.join(schemaDirectory, "api.schema.json"), "{}");

    assert.throws(
        () => stageRuntimeSchemas({ environmentFile, schemaDirectory }),
        /session-events\.schema\.json/,
    );
    assert.equal(fs.statSync(environmentFile, { throwIfNoEntry: false }), undefined);

    writeFixture(path.join(schemaDirectory, "session-events.schema.json"), "not json");
    assert.throws(() => stageRuntimeSchemas({ environmentFile, schemaDirectory }), /Invalid runtime schema/);
    assert.equal(fs.statSync(environmentFile, { throwIfNoEntry: false }), undefined);
});

await test("rejects an incomplete runtime artifact", (t) => {
    const root = fs.mkdtempSync(path.join(os.tmpdir(), "sdk-runtime-artifact-"));
    t.after(() => fs.rmSync(root, { recursive: true, force: true }));

    assert.throws(
        () =>
            prepareRuntimeArtifact({
                runtimeRoot: root,
                sdkRoot: path.join(root, "src/sdk"),
                outputDirectory: path.join(root, "out"),
            }),
        /CLI executable not found/,
    );
});

await test("prepares artifacts from an exported SDK layout", (t) => {
    const root = fs.mkdtempSync(path.join(os.tmpdir(), "sdk-exported-layout-"));
    t.after(() => fs.rmSync(root, { recursive: true, force: true }));
    const script = path.join(root, "scripts/ci/runtime-artifact.mjs");
    fs.mkdirSync(path.dirname(script), { recursive: true });
    fs.copyFileSync(fileURLToPath(new URL("./runtime-artifact.mjs", import.meta.url)), script);
    fs.mkdirSync(path.join(root, "rust"), { recursive: true });
    writeRuntimeFixture(root, `${process.platform}-${process.arch}`);

    const result = spawnSync(process.execPath, [script, "prepare"], {
        encoding: "utf8",
        env: { ...process.env, GITHUB_ENV: path.join(root, "environment") },
    });

    assert.equal(result.status, 0, result.stderr);
});

await test("restores executable bits only in directories present in the artifact", (t) => {
    const root = fs.mkdtempSync(path.join(os.tmpdir(), "sdk-restored-artifact-"));
    t.after(() => fs.rmSync(root, { recursive: true, force: true }));
    const wrapper = path.join(root, "dist-cli/prebuilds/linux-arm64/copilot-runtime");
    writeFixture(wrapper, "wrapper");
    fs.chmodSync(wrapper, 0o644);

    restoreRuntimeArtifact({ runtimeRoot: root });

    assert.notEqual(fs.statSync(wrapper).mode & 0o111, 0);
});

/** @param {string} filePath @param {string} contents */
function writeFixture(filePath, contents) {
    fs.mkdirSync(path.dirname(filePath), { recursive: true });
    fs.writeFileSync(filePath, contents);
}

/** @param {string} runtimeRoot @param {string} target */
function writeRuntimeFixture(runtimeRoot, target) {
    writeFixture(path.join(runtimeRoot, "dist-cli/index.js"), "entry point");
    writeFixture(path.join(runtimeRoot, "dist-cli/app.js"), "legacy entry point");
    writeFixture(path.join(runtimeRoot, "dist-cli/copilot-sdk/index.js"), "sdk entry point");
    writeFixture(path.join(runtimeRoot, "dist-cli/copilot-sdk/extension.js"), "sdk extension entry point");
    writeFixture(path.join(runtimeRoot, "dist-bin", target, "copilot"), target);
    writeFixture(path.join(runtimeRoot, "dist-cli/prebuilds", target, "runtime.node"), target);
    writeFixture(path.join(runtimeRoot, "dist-cli/prebuilds", target, "copilot-runtime"), target);
}

/** @param {string} value */
function escapeRegExp(value) {
    return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

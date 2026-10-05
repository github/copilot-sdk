/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import assert from "node:assert/strict";
import childProcess from "node:child_process";
import { spawnSync } from "node:child_process";
import fs from "node:fs";
import { syncBuiltinESMExports } from "node:module";
import os from "node:os";
import path from "node:path";
import test from "node:test";

import {
    archiveExtractionInvocation,
    bazelActionEnvironmentArgument,
    bazelInvocationEnvironment,
    installCodegenDependencies,
    npmInvocation,
    parseBazelInfoPath,
    syncGeneratedArchive,
    syncSchemas,
} from "./build-prerequisites.mjs";

test("extracts archives by basename so Windows drive letters are not parsed as remote hosts", () => {
    assert.deepEqual(
        archiveExtractionInvocation("C:\\build\\nodejs.tar", "C:\\temp\\generated", "win32"),
        {
            args: ["-xf", "nodejs.tar", "-C", "C:\\temp\\generated"],
            cwd: "C:\\build",
        },
    );
    assert.deepEqual(archiveExtractionInvocation("/build/nodejs.tar", "/tmp/generated", "linux"), {
        args: ["-xf", "nodejs.tar", "-C", "/tmp/generated"],
        cwd: "/build",
    });
});

test("passes Bazel's normalized tool PATH through the invocation environment", () => {
    const systemBin = path.join(path.sep, "tools", "bin");
    const environment = {
        PATH: [
            path.join(path.sep, "workspace", "src", "sdk", "node_modules", ".bin"),
            systemBin,
            path.join(path.sep, "workspace", "node_modules", ".bin"),
            systemBin,
            path.join(path.sep, "npm", "node-gyp-bin"),
        ].join(path.delimiter),
    };

    assert.equal(bazelInvocationEnvironment(environment).PATH, systemBin);
    assert.equal(bazelActionEnvironmentArgument(), "--action_env=PATH");
});

test("uses only the final Bazel info path when preceding output is noisy", () => {
    const bazelBin = path.resolve("bazel-bin");

    assert.equal(
        parseBazelInfoPath(`Verifying lockfile...\nDependencies ready\n${bazelBin}\n`),
        bazelBin,
    );
});

test("rejects Bazel info output without an absolute path", () => {
    assert.throws(
        () => parseBazelInfoPath("Verifying lockfile...\nrelative/bazel-bin\n"),
        /Unable to resolve Bazel output path/,
    );
});

test("runs npm through the Windows command interpreter", () => {
    assert.deepEqual(npmInvocation("win32", { ComSpec: "C:\\Windows\\System32\\cmd.exe" }), {
        command: "C:\\Windows\\System32\\cmd.exe",
        args: ["/d", "/s", "/c", "npm.cmd"],
    });
});

test("installs shared codegen dependencies once and Java dependencies only when selected", (t) => {
    const sdkRoot = fs.mkdtempSync(path.join(import.meta.dirname, ".codegen-dependencies-"));
    t.after(() => fs.rmSync(sdkRoot, { recursive: true, force: true }));
    for (const directory of ["scripts/codegen", "java/scripts/codegen"]) {
        writeFile(path.join(sdkRoot, directory, "package.json"), "{}");
        writeFile(path.join(sdkRoot, directory, "package-lock.json"), "{}");
    }
    const calls = [];
    const spawn = t.mock.method(childProcess, "spawnSync", (command, args, { cwd }) => {
        calls.push({ command, args, cwd });
        return { status: 0 };
    });
    syncBuiltinESMExports();
    t.after(() => {
        spawn.mock.restore();
        syncBuiltinESMExports();
    });
    const invocation = npmInvocation();
    const shared = path.join(sdkRoot, "scripts/codegen");
    const java = path.join(sdkRoot, "java/scripts/codegen");

    installCodegenDependencies(["nodejs", "rust"], sdkRoot);
    installCodegenDependencies(["nodejs"], sdkRoot);
    assert.deepEqual(calls, [
        {
            command: invocation.command,
            args: [...invocation.args, "ci", "--ignore-scripts", "--include=dev"],
            cwd: shared,
        },
    ]);
    assert.equal(fs.existsSync(path.join(java, "node_modules")), false);

    installCodegenDependencies(["java"], sdkRoot);
    installCodegenDependencies(["java"], sdkRoot);
    assert.equal(calls.length, 2);
    assert.deepEqual(calls[1], {
        command: invocation.command,
        args: [...invocation.args, "ci", "--ignore-scripts", "--include=dev"],
        cwd: java,
    });
});

test("syncs generated projections once without rewriting unchanged files", (t) => {
    const root = createGitFixture(t);
    const sdkRoot = path.join(root, "src/sdk");
    const stagedRoot = path.join(root, "staged");
    const generatedPath = "nodejs/src/generated/rpc.ts";
    writeFile(path.join(stagedRoot, generatedPath), "generated\n");
    const archivePath = path.join(root, "projection.tar");
    run(tarCommand(), ["-cf", archivePath, "-C", stagedRoot, "."], root);

    assert.equal(
        syncGeneratedArchive({
            archivePath,
            generatedRoots: [generatedPath],
            language: "nodejs",
            runtimeRoot: root,
            sdkRoot,
        }),
        true,
    );
    const destination = path.join(sdkRoot, generatedPath);
    const firstModifiedTime = fs.statSync(destination).mtimeMs;
    assert.equal(
        syncGeneratedArchive({
            archivePath,
            generatedRoots: [generatedPath],
            language: "nodejs",
            runtimeRoot: root,
            sdkRoot,
        }),
        false,
    );
    assert.equal(fs.statSync(destination).mtimeMs, firstModifiedTime);
});

test("refuses to overwrite modified generated projections", (t) => {
    const root = createGitFixture(t);
    const sdkRoot = path.join(root, "src/sdk");
    const generatedPath = "nodejs/src/generated/rpc.ts";
    writeFile(path.join(sdkRoot, generatedPath), "tracked\n");
    run("git", ["add", "."], root);
    run("git", ["commit", "-m", "fixture"], root);
    fs.writeFileSync(path.join(sdkRoot, generatedPath), "local edit\n");

    const stagedRoot = path.join(root, "staged");
    writeFile(path.join(stagedRoot, generatedPath), "new generated output\n");
    const archivePath = path.join(root, "projection.tar");
    run(tarCommand(), ["-cf", archivePath, "-C", stagedRoot, "."], root);

    assert.throws(
        () =>
            syncGeneratedArchive({
                archivePath,
                generatedRoots: [generatedPath],
                language: "nodejs",
                runtimeRoot: root,
                sdkRoot,
            }),
        /Refusing to overwrite modified generated SDK outputs/,
    );
    assert.equal(fs.readFileSync(path.join(sdkRoot, generatedPath), "utf8"), "local edit\n");
});

test("rejects empty generator archives without deleting existing projections", (t) => {
    const root = createGitFixture(t);
    const sdkRoot = path.join(root, "src/sdk");
    const generatedPath = "nodejs/src/generated/rpc.ts";
    writeFile(path.join(sdkRoot, generatedPath), "tracked\n");
    run("git", ["add", "."], root);
    run("git", ["commit", "-m", "fixture"], root);
    const stagedRoot = path.join(root, "staged");
    fs.mkdirSync(stagedRoot);
    const archivePath = path.join(root, "projection.tar");
    run(tarCommand(), ["-cf", archivePath, "-C", stagedRoot, "."], root);

    assert.throws(
        () =>
            syncGeneratedArchive({
                archivePath,
                generatedRoots: ["nodejs/src/generated"],
                language: "nodejs",
                runtimeRoot: root,
                sdkRoot,
            }),
        /No nodejs generated outputs/,
    );
    assert.equal(fs.readFileSync(path.join(sdkRoot, generatedPath), "utf8"), "tracked\n");
});

test("replaces prior generated projections while preserving manual edits", (t) => {
    const root = createGitFixture(t);
    const sdkRoot = path.join(root, "src/sdk");
    const generatedPath = "nodejs/src/generated/rpc.ts";
    const destination = path.join(sdkRoot, generatedPath);
    writeFile(destination, "committed\n");
    run("git", ["add", "."], root);
    run("git", ["commit", "-m", "fixture"], root);

    const previousRoot = path.join(root, "previous");
    writeFile(path.join(previousRoot, generatedPath), "previous generated output\n");
    const previousArchivePath = path.join(root, "previous.tar");
    run(tarCommand(), ["-cf", previousArchivePath, "-C", previousRoot, "."], root);
    fs.copyFileSync(path.join(previousRoot, generatedPath), destination);

    const stagedRoot = path.join(root, "staged");
    writeFile(path.join(stagedRoot, generatedPath), "new generated output\n");
    const archivePath = path.join(root, "projection.tar");
    run(tarCommand(), ["-cf", archivePath, "-C", stagedRoot, "."], root);

    assert.equal(
        syncGeneratedArchive({
            archivePath,
            generatedRoots: [generatedPath],
            language: "nodejs",
            previousArchivePath,
            runtimeRoot: root,
            sdkRoot,
        }),
        true,
    );
    assert.equal(fs.readFileSync(destination, "utf8"), "new generated output\n");
});

test("syncs Bazel schemas without rewriting equal tracked files", (t) => {
    const root = createGitFixture(t);
    const schemaOutputDirectory = path.join(root, "bazel-schemas");
    for (const fileName of ["api.schema.json", "session-events.schema.json"]) {
        writeFile(path.join(schemaOutputDirectory, fileName), `{"title":"${fileName}"}`);
    }

    assert.equal(syncSchemas({ runtimeRoot: root, schemaOutputDirectory }), true);
    const apiPath = path.join(root, "generated/api.schema.json");
    const firstModifiedTime = fs.statSync(apiPath).mtimeMs;
    assert.equal(syncSchemas({ runtimeRoot: root, schemaOutputDirectory }), false);
    assert.equal(fs.statSync(apiPath).mtimeMs, firstModifiedTime);
});

test("replaces schemas that match the prior Bazel output", (t) => {
    const root = createGitFixture(t);
    for (const fileName of ["api.schema.json", "session-events.schema.json"]) {
        writeFile(path.join(root, "generated", fileName), `{"title":"committed ${fileName}"}`);
    }
    run("git", ["add", "."], root);
    run("git", ["commit", "-m", "fixture"], root);

    const previousSchemaDirectory = path.join(root, "previous-schemas");
    const schemaOutputDirectory = path.join(root, "bazel-schemas");
    for (const fileName of ["api.schema.json", "session-events.schema.json"]) {
        writeFile(path.join(previousSchemaDirectory, fileName), `{"title":"previous ${fileName}"}`);
        fs.copyFileSync(
            path.join(previousSchemaDirectory, fileName),
            path.join(root, "generated", fileName),
        );
        writeFile(path.join(schemaOutputDirectory, fileName), `{"title":"new ${fileName}"}`);
    }

    assert.equal(
        syncSchemas({ previousSchemaDirectory, runtimeRoot: root, schemaOutputDirectory }),
        true,
    );
    assert.equal(
        fs.readFileSync(path.join(root, "generated/api.schema.json"), "utf8"),
        '{"title":"new api.schema.json"}',
    );
});

test("syncs new and obsolete Go outputs without changing handwritten neighbors", (t) => {
    const root = createGitFixture(t);
    const sdkRoot = path.join(root, "src/sdk");
    writeFile(path.join(sdkRoot, "go/rpc/zobsolete.go"), "obsolete");
    writeFile(path.join(sdkRoot, "go/rpc/handwritten.go"), "handwritten");
    run("git", ["add", "."], root);
    run("git", ["commit", "-m", "fixture"], root);
    const staged = path.join(root, "staged");
    writeFile(path.join(staged, "go/rpc/znew.go"), "new");
    const archivePath = path.join(root, "projection.tar");
    run(tarCommand(), ["-cf", archivePath, "-C", staged, "."], root);
    syncGeneratedArchive({
        archivePath,
        generatedRoots: ["go/z*.go", "go/rpc/z*.go"],
        language: "go",
        runtimeRoot: root,
        sdkRoot,
    });
    assert.equal(fs.existsSync(path.join(sdkRoot, "go/rpc/zobsolete.go")), false);
    assert.equal(fs.readFileSync(path.join(sdkRoot, "go/rpc/znew.go"), "utf8"), "new");
    assert.equal(fs.readFileSync(path.join(sdkRoot, "go/rpc/handwritten.go"), "utf8"), "handwritten");
});

test("preserves an edited Python initializer on a first generation without a cached archive", (t) => {
    const root = createGitFixture(t);
    const sdkRoot = path.join(root, "src/sdk");
    const initializer = "python/copilot/generated/__init__.py";
    writeFile(path.join(sdkRoot, initializer), "handwritten");
    writeFile(path.join(sdkRoot, "python/copilot/generated/obsolete.py"), "obsolete");
    run("git", ["add", "."], root);
    run("git", ["commit", "-m", "fixture"], root);
    writeFile(path.join(sdkRoot, initializer), "local handwritten edit");
    const staged = path.join(root, "staged");
    writeFile(path.join(staged, "python/copilot/generated/rpc.py"), "generated");
    const archivePath = path.join(root, "projection.tar");
    run(tarCommand(), ["-cf", archivePath, "-C", staged, "."], root);
    syncGeneratedArchive({
        archivePath,
        generatedRoots: ["python/copilot/generated"],
        preservedFiles: [initializer],
        language: "python",
        runtimeRoot: root,
        sdkRoot,
    });
    assert.equal(fs.readFileSync(path.join(sdkRoot, initializer), "utf8"), "local handwritten edit");
    assert.equal(fs.existsSync(path.join(sdkRoot, "python/copilot/generated/obsolete.py")), false);
});

test("rejects archive outputs outside the declared generation roots", (t) => {
    const root = createGitFixture(t);
    const staged = path.join(root, "staged");
    writeFile(path.join(staged, "nodejs/src/new-output.ts"), "generated");
    const archivePath = path.join(root, "projection.tar");
    run(tarCommand(), ["-cf", archivePath, "-C", staged, "."], root);
    assert.throws(
        () =>
            syncGeneratedArchive({
                archivePath,
                generatedRoots: ["nodejs/src/generated"],
                language: "nodejs",
                runtimeRoot: root,
                sdkRoot: path.join(root, "src/sdk"),
            }),
        /Undeclared nodejs generated output: nodejs\/src\/new-output.ts/,
    );
});

function createGitFixture(t) {
    const root = fs.mkdtempSync(path.join(os.tmpdir(), "copilot-sdk-build-prerequisites-"));
    t.after(() => fs.rmSync(root, { recursive: true, force: true }));
    run("git", ["init", "--quiet"], root);
    run("git", ["config", "user.name", "SDK Test"], root);
    run("git", ["config", "user.email", "sdk-test@example.com"], root);
    return root;
}

function writeFile(filePath, contents) {
    fs.mkdirSync(path.dirname(filePath), { recursive: true });
    fs.writeFileSync(filePath, contents);
}

function run(command, args, cwd) {
    const result = spawnSync(command, args, { cwd, encoding: "utf8" });
    assert.equal(result.status, 0, result.stderr);
}

function tarCommand() {
    return process.platform === "win32" ? "tar.exe" : "tar";
}

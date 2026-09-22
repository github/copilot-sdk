/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { spawnSync } from "node:child_process";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";

const SCHEMA_FILES = ["api.schema.json", "session-events.schema.json"];
const GENERATED_ROOTS = {
    nodejs: ["nodejs/src/generated/rpc.ts", "nodejs/src/generated/session-events.ts"],
    python: ["python/copilot/generated/rpc.py", "python/copilot/generated/session_events.py"],
    go: [
        "go/rpc/zrpc.go",
        "go/rpc/zrpc_encoding.go",
        "go/rpc/zsession_encoding.go",
        "go/rpc/zsession_events.go",
        "go/zsession_events.go",
    ],
    dotnet: ["dotnet/src/Generated/Rpc.cs", "dotnet/src/Generated/SessionEvents.cs"],
    java: ["java/sdk/src/generated/java/com/github/copilot/generated"],
    rust: [
        "rust/src/generated/api_types.rs",
        "rust/src/generated/mod.rs",
        "rust/src/generated/rpc.rs",
        "rust/src/generated/session_events.rs",
    ],
};

export function prepareSdkSources({ languages, runtimeRoot, sdkRoot }) {
    const selectedLanguages = [...new Set(languages)];
    const labels = selectedLanguages.map((language) => `//src/sdk:${language}_projection`);
    installCodegenDependencies(selectedLanguages, sdkRoot);
    const bazelBin = parseBazelInfoPath(runBazel(["info", "bazel-bin"], runtimeRoot, true));
    const previousOutputs = fs.mkdtempSync(path.join(os.tmpdir(), "copilot-sdk-previous-"));
    try {
        copyExistingDirectory(
            path.join(bazelBin, "src/native/schema-codegen/generated"),
            path.join(previousOutputs, "schemas"),
        );
        for (const language of selectedLanguages) {
            copyFileIfPresent(
                path.join(bazelBin, `src/sdk/projections/${language}.tar`),
                path.join(previousOutputs, `${language}.tar`),
            );
        }

        runBazel(["build", bazelActionEnvironmentArgument(), ...labels], runtimeRoot);
        syncSchemas({
            previousSchemaDirectory: path.join(previousOutputs, "schemas"),
            runtimeRoot,
            schemaOutputDirectory: path.join(bazelBin, "src/native/schema-codegen/generated"),
        });
        for (const language of selectedLanguages) {
            syncGeneratedArchive({
                archivePath: path.join(bazelBin, `src/sdk/projections/${language}.tar`),
                generatedRoots: GENERATED_ROOTS[language],
                language,
                previousArchivePath: path.join(previousOutputs, `${language}.tar`),
                runtimeRoot,
                sdkRoot,
            });
        }
    } finally {
        fs.rmSync(previousOutputs, { recursive: true, force: true });
    }
}

export function parseBazelInfoPath(output) {
    const candidate = output
        .split(/\r?\n/)
        .map((line) => line.trim())
        .filter(Boolean)
        .at(-1);
    if (!candidate || !path.isAbsolute(candidate)) {
        throw new Error(`Unable to resolve Bazel output path from:\n${output.trim()}`);
    }
    return candidate;
}

export function npmInvocation(platform = process.platform, environment = process.env) {
    if (platform === "win32") {
        return {
            command: environment.ComSpec ?? "cmd.exe",
            args: ["/d", "/s", "/c", "npm.cmd"],
        };
    }
    return { command: "npm", args: [] };
}

export function bazelInvocationEnvironment(environment = process.env) {
    const pathEntries = (environment.PATH ?? "")
        .split(path.delimiter)
        .filter(
            (entry) =>
                entry &&
                !entry.includes(`${path.sep}node_modules${path.sep}.bin`) &&
                !entry.includes(`${path.sep}node-gyp-bin`),
        );
    return {
        ...environment,
        PATH: [...new Set(pathEntries)].join(path.delimiter),
    };
}

export function bazelActionEnvironmentArgument() {
    return "--action_env=PATH";
}

function installCodegenDependencies(languages, sdkRoot) {
    runNpm(["ci", "--ignore-scripts"], path.join(sdkRoot, "scripts/codegen"));
    if (languages.includes("java")) {
        runNpm(["ci", "--ignore-scripts"], path.join(sdkRoot, "java/scripts/codegen"));
    }
}

export function syncSchemas({ previousSchemaDirectory, runtimeRoot, schemaOutputDirectory }) {
    const changes = SCHEMA_FILES.filter(
        (fileName) =>
            !filesEqual(
                path.join(schemaOutputDirectory, fileName),
                path.join(runtimeRoot, "generated", fileName),
            ),
    );
    if (changes.length === 0) {
        return false;
    }
    const safelyGenerated = changes.every((fileName) => {
        const destination = path.join(runtimeRoot, "generated", fileName);
        return (
            !fs.statSync(destination, { throwIfNoEntry: false }) ||
            (previousSchemaDirectory &&
                filesEqual(path.join(previousSchemaDirectory, fileName), destination))
        );
    });
    if (!safelyGenerated) {
        assertClean(runtimeRoot, changes.map((fileName) => path.join("generated", fileName)));
    }
    for (const fileName of changes) {
        copyFileIfChanged(
            path.join(schemaOutputDirectory, fileName),
            path.join(runtimeRoot, "generated", fileName),
        );
    }
    return true;
}

export function syncGeneratedArchive({
    archivePath,
    generatedRoots,
    language,
    previousArchivePath,
    runtimeRoot,
    sdkRoot,
}) {
    const stagingDirectory = fs.mkdtempSync(path.join(os.tmpdir(), `copilot-sdk-${language}-`));
    try {
        const extraction = archiveExtractionInvocation(archivePath, stagingDirectory);
        run(tarCommand(), extraction.args, extraction.cwd);
        if (rootsEqual(stagingDirectory, sdkRoot, generatedRoots)) {
            return false;
        }
        if (!archiveCanReplaceRoots(previousArchivePath, sdkRoot, generatedRoots, runtimeRoot)) {
            assertClean(runtimeRoot, generatedRoots.map((root) => path.join("src/sdk", root)));
        }
        for (const root of generatedRoots) {
            const source = path.join(stagingDirectory, root);
            const destination = path.join(sdkRoot, root);
            if (fs.statSync(source, { throwIfNoEntry: false })?.isDirectory()) {
                fs.rmSync(destination, { recursive: true, force: true });
                fs.cpSync(source, destination, { recursive: true });
            } else {
                fs.mkdirSync(path.dirname(destination), { recursive: true });
                fs.copyFileSync(source, destination);
                fs.chmodSync(destination, 0o644);
            }
        }
        return true;
    } finally {
        fs.rmSync(stagingDirectory, { recursive: true, force: true });
    }
}

function archiveCanReplaceRoots(archivePath, sdkRoot, generatedRoots, runtimeRoot) {
    if (!archivePath || !fs.statSync(archivePath, { throwIfNoEntry: false })?.isFile()) {
        return generatedRoots.every(
            (root) => !fs.statSync(path.join(sdkRoot, root), { throwIfNoEntry: false }),
        );
    }
    const stagingDirectory = fs.mkdtempSync(path.join(os.tmpdir(), "copilot-sdk-previous-archive-"));
    try {
        const extraction = archiveExtractionInvocation(archivePath, stagingDirectory);
        run(tarCommand(), extraction.args, extraction.cwd);
        return generatedRoots.every((root) => {
            const destination = path.join(sdkRoot, root);
            return (
                !fs.statSync(destination, { throwIfNoEntry: false }) ||
                pathsEqual(path.join(stagingDirectory, root), destination)
            );
        });
    } finally {
        fs.rmSync(stagingDirectory, { recursive: true, force: true });
    }
}

function rootsEqual(leftRoot, rightRoot, roots) {
    return roots.every((root) => pathsEqual(path.join(leftRoot, root), path.join(rightRoot, root)));
}

function pathsEqual(left, right) {
    const leftStat = fs.statSync(left, { throwIfNoEntry: false });
    const rightStat = fs.statSync(right, { throwIfNoEntry: false });
    if (!leftStat || !rightStat || leftStat.isDirectory() !== rightStat.isDirectory()) {
        return false;
    }
    if (leftStat.isFile()) {
        return filesEqual(left, right);
    }
    const leftEntries = fs.readdirSync(left).sort();
    const rightEntries = fs.readdirSync(right).sort();
    return (
        leftEntries.length === rightEntries.length &&
        leftEntries.every((entry, index) => entry === rightEntries[index] && pathsEqual(path.join(left, entry), path.join(right, entry)))
    );
}

function filesEqual(left, right) {
    const leftStat = fs.statSync(left, { throwIfNoEntry: false });
    const rightStat = fs.statSync(right, { throwIfNoEntry: false });
    return (
        !!leftStat &&
        !!rightStat &&
        leftStat.size === rightStat.size &&
        fs.readFileSync(left).equals(fs.readFileSync(right))
    );
}

function copyFileIfChanged(source, destination) {
    if (filesEqual(source, destination)) {
        return;
    }
    fs.mkdirSync(path.dirname(destination), { recursive: true });
    fs.copyFileSync(source, destination);
    fs.chmodSync(destination, 0o644);
}

function copyExistingDirectory(source, destination) {
    if (fs.statSync(source, { throwIfNoEntry: false })?.isDirectory()) {
        fs.cpSync(source, destination, { recursive: true });
    }
}

function copyFileIfPresent(source, destination) {
    if (fs.statSync(source, { throwIfNoEntry: false })?.isFile()) {
        fs.copyFileSync(source, destination);
    }
}

function assertClean(runtimeRoot, paths) {
    const result = spawnSync("git", ["status", "--porcelain", "--", ...paths], {
        cwd: runtimeRoot,
        encoding: "utf8",
    });
    if (result.error) {
        throw result.error;
    }
    if (result.status !== 0) {
        throw new Error(`git status failed with exit code ${result.status}: ${result.stderr.trim()}`);
    }
    if (result.stdout.trim()) {
        throw new Error(
            `Refusing to overwrite modified generated SDK outputs:\n${result.stdout.trim()}\nCommit, stash, or revert them before rebuilding.`,
        );
    }
}

function runBazel(args, runtimeRoot, capture = false) {
    return run(
        process.execPath,
        ["--experimental-strip-types", path.join(runtimeRoot, "script/bazel.ts"), ...args],
        runtimeRoot,
        capture,
        bazelInvocationEnvironment(),
    );
}

function runNpm(args, cwd) {
    const invocation = npmInvocation();
    run(invocation.command, [...invocation.args, ...args], cwd);
}

function tarCommand() {
    return process.platform === "win32" ? "tar.exe" : "tar";
}

export function archiveExtractionInvocation(archivePath, destination, platform = process.platform) {
    const pathApi = platform === "win32" ? path.win32 : path;
    return {
        args: ["-xf", pathApi.basename(archivePath), "-C", destination],
        cwd: pathApi.dirname(archivePath),
    };
}

function run(command, args, cwd, capture = false, environment = process.env) {
    const result = spawnSync(command, args, {
        cwd,
        encoding: capture ? "utf8" : undefined,
        env: environment,
        stdio: capture ? ["ignore", "pipe", "pipe"] : "inherit",
    });
    if (result.error) {
        throw result.error;
    }
    if (result.status !== 0) {
        throw new Error(
            `${command} ${args.join(" ")} exited with status ${result.status}${capture ? `: ${result.stderr.trim()}` : ""}`,
        );
    }
    return capture ? result.stdout : "";
}

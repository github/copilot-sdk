/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// Provides portable aggregate and per-language SDK tasks without requiring Just.

import { readFileSync, readdirSync, statSync } from "node:fs";
import { dirname, join, relative, resolve } from "node:path";
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import { bazelActionEnvironmentArgument, prepareSdkSources } from "./build-prerequisites.mjs";
import { findRuntimeRoot, getRuntimeCliPaths } from "./runtime-layout.mjs";

const sdkRoot = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const languages = ["nodejs", "python", "go", "dotnet", "java", "rust"];
const maven = process.platform === "win32" ? "mvnw.cmd" : "./mvnw";

const command = (cwd, executable, args) => ({ args, cwd, executable });

const tasks = {
    build: {
        nodejs: [
            command("nodejs", "npm", ["ci", "--ignore-scripts"]),
            command("nodejs", "npm", ["run", "build"]),
        ],
        python: [
            command("python", "uv", ["sync", "--all-extras", "--dev"]),
            command("python", "uv", ["build"]),
        ],
        go: [command("go", "go", ["build", "./..."]), command("go/samples", "go", ["build", "./..."])],
        dotnet: [command("dotnet", "dotnet", ["build", "src/GitHub.Copilot.SDK.csproj"])],
        java: [command("java", maven, ["clean", "package", "-DskipTests"])],
        rust: [command("rust", "cargo", ["build", "--all-features"])],
    },
    test: {
        nodejs: [
            command("test/harness", "npm", ["test"]),
            command("scripts/corrections", "npm", ["test"]),
            command("nodejs", "npm", ["test"]),
        ],
        python: [command("python", "uv", ["run", "pytest"])],
        go: [command("go", "go", ["test", "./..."])],
        dotnet: [command("dotnet", "dotnet", ["test", "test/GitHub.Copilot.SDK.Test.csproj"])],
        java: [command("java", maven, ["clean", "verify"])],
        rust: [command("rust", "cargo", ["test", "--features", "test-support"])],
    },
    lint: {
        nodejs: [command("nodejs", "npm", ["run", "lint"])],
        python: [command("python", "uv", ["run", "ruff", "check", "."])],
        go: [command("go", "golangci-lint", ["run", "./..."])],
        dotnet: [command("dotnet", "dotnet", ["format", "GitHub.Copilot.SDK.slnx", "--verify-no-changes"])],
        java: [command("java", maven, ["spotless:check"])],
        rust: [
            command("rust", "cargo", [
                "clippy",
                "--all-targets",
                "--features",
                "test-support",
                "--",
                "--no-deps",
                "-D",
                "warnings",
                "-D",
                "clippy::unwrap_used",
                "-D",
                "clippy::disallowed_macros",
                "-D",
                "clippy::await_holding_invalid_type",
            ]),
        ],
    },
    format: {
        nodejs: [command("nodejs", "npm", ["run", "format"])],
        python: [command("python", "uv", ["run", "ruff", "format", "."])],
        go: [{ kind: "gofmt", write: true }],
        dotnet: [command("dotnet", "dotnet", ["format", "GitHub.Copilot.SDK.slnx"])],
        java: [command("java", maven, ["spotless:apply"])],
        rust: [
            command("rust", "cargo", [
                "+nightly-2026-04-14",
                "fmt",
                "--all",
                "--",
                "--config-path",
                ".rustfmt.nightly.toml",
            ]),
        ],
    },
    "format:check": {
        nodejs: [command("nodejs", "npm", ["run", "format:check"])],
        python: [command("python", "uv", ["run", "ruff", "format", "--check", "."])],
        go: [{ kind: "gofmt", write: false }],
        dotnet: [command("dotnet", "dotnet", ["format", "GitHub.Copilot.SDK.slnx", "--verify-no-changes"])],
        java: [command("java", maven, ["spotless:check"])],
        rust: [
            command("rust", "cargo", [
                "+nightly-2026-04-14",
                "fmt",
                "--all",
                "--",
                "--config-path",
                ".rustfmt.nightly.toml",
                "--check",
            ]),
        ],
    },
    check: {
        nodejs: [
            command("nodejs", "npm", ["run", "format:check"]),
            command("nodejs", "npm", ["run", "lint"]),
            command("nodejs", "npm", ["run", "typecheck"]),
        ],
        python: [
            command("python", "uv", ["run", "ruff", "format", "--check", "."]),
            command("python", "uv", ["run", "ruff", "check", "."]),
            command("python", "uv", ["run", "ty", "check", "copilot"]),
        ],
        go: [{ kind: "gofmt", write: false }, command("go", "golangci-lint", ["run", "./..."])],
        dotnet: [
            command("dotnet", "dotnet", ["format", "GitHub.Copilot.SDK.slnx", "--verify-no-changes"]),
            command("dotnet", "dotnet", ["build", "GitHub.Copilot.SDK.slnx"]),
        ],
        java: [command("java", maven, ["spotless:check"]), command("java", maven, ["verify"])],
        rust: [],
    },
    generate: {
        nodejs: [command("scripts/codegen", "npm", ["run", "generate:ts"])],
        python: [command("scripts/codegen", "npm", ["run", "generate:python"])],
        go: [command("scripts/codegen", "npm", ["run", "generate:go"])],
        dotnet: [command("scripts/codegen", "npm", ["run", "generate:csharp"])],
        java: [command("java", maven, ["generate-sources", "-Pcodegen"])],
        rust: [command("scripts/codegen", "npm", ["run", "generate:rust"])],
    },
    docs: {
        nodejs: [command("scripts/docs-validation", "npm", ["run", "validate:ts"])],
        python: [command("scripts/docs-validation", "npm", ["run", "validate:py"])],
        go: [command("scripts/docs-validation", "npm", ["run", "validate:go"])],
        dotnet: [command("scripts/docs-validation", "npm", ["run", "validate:cs"])],
        java: [command("scripts/docs-validation", "npm", ["run", "validate:java"])],
    },
};

tasks["test:default"] = {
    nodejs: [command("nodejs", "npm", ["run", "test:unit"])],
    rust: tasks.test.rust,
};

tasks.check.rust = [...tasks["format:check"].rust, ...tasks.lint.rust];

function collectGoFiles(directory) {
    const files = [];
    for (const entry of readdirSync(directory, { withFileTypes: true })) {
        if (entry.isDirectory()) {
            if (entry.name !== "generated") {
                files.push(...collectGoFiles(join(directory, entry.name)));
            }
        } else if (entry.isFile() && entry.name.endsWith(".go")) {
            files.push(join(directory, entry.name));
        }
    }
    return files;
}

function runCommand(step, environment = process.env) {
    const cwd = resolve(sdkRoot, step.cwd);
    console.log(`\n> ${relative(sdkRoot, cwd) || "."}: ${step.executable} ${step.args.join(" ")}`);
    const result = spawnSync(step.executable, step.args, {
        cwd,
        shell: process.platform === "win32",
        stdio: "inherit",
        env: environment,
    });
    if (result.error) {
        throw new Error(
            `Failed to run ${step.executable} in ${relative(sdkRoot, cwd) || "."}: ${result.error.message}`,
            { cause: result.error },
        );
    }
    if (result.status !== 0) {
        process.exit(result.status ?? 1);
    }
}

function runGoFormat(write) {
    const goRoot = resolve(sdkRoot, "go");
    const files = collectGoFiles(goRoot);
    const args = write ? ["-w", ...files] : ["-l", ...files];
    const result = spawnSync("gofmt", args, {
        cwd: goRoot,
        encoding: "utf8",
        shell: process.platform === "win32",
    });
    if (result.error) {
        throw result.error;
    }
    if (result.status !== 0) {
        process.stderr.write(result.stderr);
        process.exit(result.status ?? 1);
    }
    if (!write && result.stdout.trim()) {
        process.stderr.write(`The following Go files need formatting:\n${result.stdout}`);
        process.exit(1);
    }
}

export function getTaskSteps(verb, language) {
    if (typeof verb !== "string" || !(verb in tasks) || (language && !(language in tasks[verb]))) {
        throw new Error(`Unknown SDK task: ${verb}${language ? `:${language}` : ""}`);
    }
    const selectedLanguages = language ? [language] : Object.keys(tasks[verb]);
    return selectedLanguages.flatMap((selectedLanguage) =>
        (tasks[verb][selectedLanguage] ?? []).map((step) => ({
            language: selectedLanguage,
            ...step,
        })),
    );
}

export function resolveTaskEnvironment({
    environment = process.env,
    requireCli = false,
    requireLegacyCli = false,
    requireSchemas = false,
    runtimeSource,
    schemaDirectory,
    sdkDirectory = sdkRoot,
} = {}) {
    const runtimeRoot = findRuntimeRoot(sdkDirectory);
    const resolvedRuntimeSource =
        runtimeSource ?? environment.COPILOT_RUNTIME_SOURCE ?? (runtimeRoot ? "checkout" : undefined);
    const selectedSchemaDirectory =
        schemaDirectory ??
        environment.COPILOT_CLI_SCHEMA_DIR ??
        (runtimeRoot && requireSchemas ? join(runtimeRoot, "generated") : undefined);
    const { cliPath, legacyCliPath } = getRuntimeCliPaths(runtimeRoot, environment);
    if (!selectedSchemaDirectory && resolvedRuntimeSource !== "checkout") {
        return environment;
    }
    let resolvedSchemaDirectory;
    if (selectedSchemaDirectory) {
        resolvedSchemaDirectory = resolve(sdkDirectory, selectedSchemaDirectory);
        for (const schemaName of ["api.schema.json", "session-events.schema.json"]) {
            const schemaPath = join(resolvedSchemaDirectory, schemaName);
            let contents;
            try {
                contents = readFileSync(schemaPath, "utf8");
            } catch (error) {
                throw new Error(`Runtime schema not found at ${schemaPath}`, { cause: error });
            }
            try {
                JSON.parse(contents);
            } catch (error) {
                throw new Error(`Runtime schema is invalid JSON at ${schemaPath}`, { cause: error });
            }
        }
    }
    if (requireCli) {
        assertRuntimeFile(cliPath, "Runtime CLI");
    }
    if (requireLegacyCli) {
        assertRuntimeFile(legacyCliPath, "Legacy runtime CLI");
    }
    return {
        ...environment,
        ...(resolvedSchemaDirectory ? { COPILOT_CLI_SCHEMA_DIR: resolvedSchemaDirectory } : {}),
        ...(cliPath ? { COPILOT_CLI_PATH: resolve(cliPath) } : {}),
        ...(legacyCliPath ? { COPILOT_LEGACY_CLI_PATH: resolve(legacyCliPath) } : {}),
        COPILOT_SKIP_CLI_DOWNLOAD: "1",
        COPILOT_RUNTIME_SOURCE: "checkout",
        CopilotSkipCliDownload: "true",
    };
}

function assertRuntimeFile(filePath, description) {
    if (!filePath) {
        throw new Error(`${description} path is required for runtime SDK tests`);
    }
    try {
        if (!statSync(filePath).isFile()) {
            throw new Error();
        }
    } catch (error) {
        throw new Error(`${description} not found at ${filePath}; run pnpm run build:cli first`, { cause: error });
    }
}

export function runTasks(verb, language, options = {}) {
    const runtimeRoot = findRuntimeRoot(options.sdkDirectory ?? sdkRoot);
    const runtimeSource =
        options.runtimeSource ??
        options.environment?.COPILOT_RUNTIME_SOURCE ??
        process.env.COPILOT_RUNTIME_SOURCE ??
        (runtimeRoot ? "checkout" : undefined);
    let environment;
    if (
        runtimeSource === "checkout" &&
        runtimeRoot &&
        ["build", "generate", "test", "test:default"].includes(verb)
    ) {
        const selectedLanguages = language ? [language] : Object.keys(tasks[verb]);
        prepareSdkSources({ languages: selectedLanguages, runtimeRoot, sdkRoot });
        if (verb === "generate") {
            return;
        }
        if (
            verb === "test" ||
            verb === "test:default" ||
            (verb === "build" && (!language || language === "java"))
        ) {
            environment = buildRuntimeForSdkTests({
                environment: options.environment ?? process.env,
                language,
                runtimeRoot,
                sdkDirectory: options.sdkDirectory ?? sdkRoot,
            });
        }
    }
    environment ??= resolveTaskEnvironment({
        ...options,
        requireCli: verb === "test" || verb === "test:default",
        requireLegacyCli: (verb === "test" || verb === "test:default") && (!language || language === "nodejs"),
        requireSchemas: verb === "generate",
    });
    const resolvedRuntimeSource = environment.COPILOT_RUNTIME_SOURCE;
    if (verb === "docs") {
        runCommand(command("scripts/docs-validation", "npm", ["run", "extract"]), environment);
    }
    for (const step of getTaskSteps(verb, language)) {
        if (step.kind === "gofmt") {
            runGoFormat(step.write);
        } else {
            if (resolvedRuntimeSource === "checkout" && verb === "build" && step.language === "rust") {
                runRootCommand(
                    runtimeRoot,
                    "pnpm",
                    ["bazel", "build", bazelActionEnvironmentArgument(environment), "//src/sdk/rust:github-copilot-sdk"],
                    environment,
                );
            } else {
                runCommand(step, environment);
            }
        }
    }
}

export function buildRuntimeForSdkTests({
    buildRuntime = (runtimeRoot, environment) =>
        runRootCommand(runtimeRoot, "pnpm", ["run", "build:cli"], environment),
    environment = process.env,
    language,
    runtimeRoot,
    sdkDirectory = sdkRoot,
}) {
    const localEnvironment = { ...environment };
    for (const name of [
        "COPILOT_CLI_PATH",
        "COPILOT_BUILD_RUNTIME_BIN",
        "COPILOT_LEGACY_CLI_PATH",
        "COPILOT_NAPI_ADDONS_PREBUILT",
        "COPILOT_RUNTIME_TARGET",
        "CLI_TARGET",
    ]) {
        delete localEnvironment[name];
    }
    buildRuntime(runtimeRoot, localEnvironment);
    return resolveTaskEnvironment({
        environment: localEnvironment,
        requireCli: true,
        requireLegacyCli: !language || language === "nodejs",
        runtimeSource: "checkout",
        sdkDirectory,
    });
}

function runRootCommand(cwd, executable, args, environment = process.env) {
    const commandName = process.platform === "win32" && executable === "pnpm" ? "pnpm.cmd" : executable;
    console.log(`\n> ${relative(sdkRoot, cwd)}: ${commandName} ${args.join(" ")}`);
    const result = spawnSync(commandName, args, {
        cwd,
        env: environment,
        shell: process.platform === "win32",
        stdio: "inherit",
    });
    if (result.error) {
        throw new Error(`Failed to run ${commandName}: ${result.error.message}`, { cause: result.error });
    }
    if (result.status !== 0) {
        process.exit(result.status ?? 1);
    }
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
    const args = process.argv.slice(2);
    const verb = args.shift();
    const language = args[0] && !args[0].startsWith("--") ? args.shift() : undefined;
    let runtimeSource;
    let schemaDirectory;
    while (args.length > 0) {
        const option = args.shift();
        const value = args.shift();
        if (!value || (option !== "--runtime-source" && option !== "--schema-dir")) {
            console.error(
                `Usage: node scripts/run-tasks.mjs <${Object.keys(tasks).join("|")}> [${languages.join("|")}] [--runtime-source <checkout>] [--schema-dir <path>]`,
            );
            process.exit(2);
        }
        if (option === "--runtime-source") {
            runtimeSource = value;
        } else {
            schemaDirectory = value;
        }
    }
    if (runtimeSource && runtimeSource !== "checkout") {
        console.error(
            `Usage: node scripts/run-tasks.mjs <${Object.keys(tasks).join("|")}> [${languages.join("|")}] [--runtime-source <checkout>] [--schema-dir <path>]`,
        );
        process.exit(2);
    }
    try {
        runTasks(verb, language, { runtimeSource, schemaDirectory });
    } catch (error) {
        if (error instanceof Error && error.message.startsWith("Unknown SDK task:")) {
            console.error(
                `Usage: node scripts/run-tasks.mjs <${Object.keys(tasks).join("|")}> [${languages.join("|")}] [--runtime-source <checkout>] [--schema-dir <path>]`,
            );
            process.exit(2);
        }
        throw error;
    }
}

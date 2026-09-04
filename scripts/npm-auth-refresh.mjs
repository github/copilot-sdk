/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { spawnSync } from "node:child_process";
import { writeFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";

export const azureFeedLocalRegistry =
    "https://pkgs.dev.azure.com/devdiv/_packaging/copilot-canary@Local/npm/registry/";
export const cfsRegistry = "https://packagefeedproxy.microsoft.io/npm/";
export const credentialProviderRegistry =
    "https://pkgs.dev.azure.com/artifacts-public/23934c1b-a3b5-4b70-9dd3-d1bef4cc72a0/_packaging/AzureArtifacts/npm/registry/";

export function getProjectNpmrcPaths(scriptUrl = import.meta.url) {
    const repositoryRoot = path.resolve(path.dirname(fileURLToPath(scriptUrl)), "..");
    return [
        path.join(repositoryRoot, "nodejs", ".npmrc"),
        path.join(repositoryRoot, "test", "harness", ".npmrc"),
        path.join(repositoryRoot, "java", "scripts", "codegen", ".npmrc"),
    ];
}

export function buildProjectNpmConfig() {
    return `@github:registry=${azureFeedLocalRegistry}\n`;
}

export function writeProjectNpmConfigs(npmrcPaths) {
    const config = buildProjectNpmConfig();
    for (const npmrcPath of npmrcPaths) {
        writeFileSync(npmrcPath, config, "utf8");
    }
}

export function getAuthCommands(platform, npmrcPath) {
    if (platform === "win32") {
        return [
            {
                command: "npm.cmd",
                args: ["install", "--global", "vsts-npm-auth@0.43.0", `--registry=${cfsRegistry}`],
            },
            {
                command: "vsts-npm-auth.cmd",
                args: ["-config", npmrcPath, "-Force", "-ReadOnly"],
            },
        ];
    }

    return [
        {
            command: "npm",
            args: [
                "install",
                "--global",
                "@microsoft/artifacts-npm-credprovider@1.1.3",
                `--registry=${credentialProviderRegistry}`,
                `--@microsoft:registry=${credentialProviderRegistry}`,
            ],
        },
        {
            command: "artifacts-npm-credprovider",
            args: ["-c", npmrcPath],
        },
    ];
}

export function getCommandInvocation(platform, command, args, commandInterpreter = "cmd.exe") {
    if (platform === "win32") {
        return {
            command: commandInterpreter,
            args: ["/d", "/s", "/c", command, ...args],
        };
    }

    return { command, args };
}

export function runCommand(
    command,
    args,
    platform = process.platform,
    commandInterpreter = process.env.ComSpec ?? "cmd.exe"
) {
    const invocation = getCommandInvocation(platform, command, args, commandInterpreter);
    const result = spawnSync(invocation.command, invocation.args, {
        stdio: "inherit",
    });
    if (result.error) {
        throw result.error;
    }
    if (result.status !== 0) {
        const outcome =
            result.status === null
                ? `terminated by signal ${result.signal ?? "unknown"}`
                : `exited with code ${result.status}`;
        throw new Error(`${command} ${outcome}`);
    }
}

export function refreshNpmAuthentication(
    platform = process.platform,
    npmrcPaths = getProjectNpmrcPaths(),
    writer = writeProjectNpmConfigs,
    runner = runCommand
) {
    writer(npmrcPaths);
    for (const { command, args } of getAuthCommands(platform, npmrcPaths[0])) {
        runner(command, args, platform);
    }
}

function usage() {
    console.log(`Usage: npm run auth:refresh

Generate scoped project .npmrc files for the copilot-canary @Local view, then
refresh Azure Artifacts credentials in the user-level npm configuration.`);
}

export function main(args = process.argv.slice(2), refresh = refreshNpmAuthentication) {
    if (args.length === 1 && (args[0] === "--help" || args[0] === "-h")) {
        usage();
        return 0;
    }

    if (args.length !== 1 || args[0] !== "--run") {
        usage();
        return 1;
    }

    refresh();
    return 0;
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
    try {
        process.exitCode = main();
    } catch (error) {
        console.error(error);
        process.exitCode = 1;
    }
}

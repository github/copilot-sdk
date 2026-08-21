import { spawnSync } from "node:child_process";
import { mkdtemp, mkdir, readFile, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import path from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";

import { afterEach, describe, expect, it, vi } from "vitest";

import {
    azureFeedLocalRegistry,
    buildProjectNpmConfig,
    cfsRegistry,
    credentialProviderRegistry,
    getAuthCommands,
    getCommandInvocation,
    getProjectNpmrcPaths,
    main,
    refreshNpmAuthentication,
    runCommand,
    writeProjectNpmConfigs,
} from "../../scripts/npm-auth-refresh.mjs";

const scriptPath = fileURLToPath(new URL("../../scripts/npm-auth-refresh.mjs", import.meta.url));
const temporaryDirectories: string[] = [];

async function createTemporaryNpmrcPaths(): Promise<string[]> {
    const repositoryRoot = await mkdtemp(path.join(tmpdir(), "copilot-sdk-npm-auth-"));
    temporaryDirectories.push(repositoryRoot);

    const directories = [
        path.join(repositoryRoot, "nodejs"),
        path.join(repositoryRoot, "test", "harness"),
        path.join(repositoryRoot, "java", "scripts", "codegen"),
    ];
    await Promise.all(directories.map((directory) => mkdir(directory, { recursive: true })));
    return directories.map((directory) => path.join(directory, ".npmrc"));
}

afterEach(async () => {
    await Promise.all(
        temporaryDirectories.splice(0).map((directory) => rm(directory, { recursive: true }))
    );
});

describe("local npm authentication refresh", () => {
    it.each(["--help", "-h"])("prints help successfully for %s", (flag) => {
        const result = spawnSync(process.execPath, [scriptPath, flag], {
            encoding: "utf8",
        });

        expect(result.status).toBe(0);
        expect(result.stdout).toContain("Usage: npm run auth:refresh");
    });

    it.each([[[]], [["--refresh"]], [["--run", "unexpected"]]])(
        "requires the explicit --run argument for %j",
        (args) => {
            const result = spawnSync(process.execPath, [scriptPath, ...args], {
                encoding: "utf8",
            });

            expect(result.status).toBe(1);
            expect(result.stdout).toContain("Usage: npm run auth:refresh");
        }
    );

    it("runs authentication only for --run", () => {
        const refresh = vi.fn();

        expect(main(["--run"], refresh)).toBe(0);
        expect(refresh).toHaveBeenCalledOnce();
    });

    it("resolves all project configs from the script URL", () => {
        const repositoryRoot = path.resolve(path.dirname(scriptPath), "..");
        expect(getProjectNpmrcPaths(pathToFileURL(scriptPath).href)).toEqual([
            path.join(repositoryRoot, "nodejs", ".npmrc"),
            path.join(repositoryRoot, "test", "harness", ".npmrc"),
            path.join(repositoryRoot, "java", "scripts", "codegen", ".npmrc"),
        ]);
    });

    it("writes only the scoped registry to all three project configs", async () => {
        const npmrcPaths = await createTemporaryNpmrcPaths();

        writeProjectNpmConfigs(npmrcPaths);

        const expected = `@github:registry=${azureFeedLocalRegistry}\n`;
        await Promise.all(
            npmrcPaths.map(async (npmrcPath) => {
                await expect(readFile(npmrcPath, "utf8")).resolves.toBe(expected);
            })
        );
        expect(buildProjectNpmConfig()).not.toMatch(/^registry=/m);
        expect(buildProjectNpmConfig()).not.toMatch(/(?:_auth|token|password)/i);
    });

    it("authenticates once using the nodejs config", () => {
        const npmrcPaths = [
            "C:\\repo\\nodejs\\.npmrc",
            "C:\\repo\\test\\harness\\.npmrc",
            "C:\\repo\\java\\scripts\\codegen\\.npmrc",
        ];
        const writer = vi.fn();
        const runner = vi.fn();

        refreshNpmAuthentication("win32", npmrcPaths, writer, runner);

        expect(writer).toHaveBeenCalledOnce();
        expect(writer).toHaveBeenCalledWith(npmrcPaths);
        expect(runner).toHaveBeenCalledTimes(2);
        expect(runner).toHaveBeenLastCalledWith(
            "vsts-npm-auth.cmd",
            ["-config", npmrcPaths[0], "-Force", "-ReadOnly"],
            "win32"
        );
    });

    it("uses vsts-npm-auth on Windows", () => {
        expect(getAuthCommands("win32", "C:\\repo\\nodejs\\.npmrc")).toEqual([
            {
                command: "npm.cmd",
                args: ["install", "--global", "vsts-npm-auth@0.43.0", `--registry=${cfsRegistry}`],
            },
            {
                command: "vsts-npm-auth.cmd",
                args: ["-config", "C:\\repo\\nodejs\\.npmrc", "-Force", "-ReadOnly"],
            },
        ]);
    });

    it("launches Windows command shims through the command interpreter", () => {
        expect(
            getCommandInvocation(
                "win32",
                "npm.cmd",
                ["--version"],
                "C:\\Windows\\System32\\cmd.exe"
            )
        ).toEqual({
            command: "C:\\Windows\\System32\\cmd.exe",
            args: ["/d", "/s", "/c", "npm.cmd", "--version"],
        });
    });

    it("surfaces command spawn errors", () => {
        expect(() =>
            runCommand(path.join(tmpdir(), "copilot-sdk-command-does-not-exist"), [], "linux")
        ).toThrow();
    });

    it("surfaces nonzero command exit statuses", () => {
        expect(() => runCommand(process.execPath, ["-e", "process.exit(7)"], "linux")).toThrow(
            "exited with code 7"
        );
    });

    it.each(["linux", "darwin"])("uses the Azure credential provider on %s", (platform) => {
        expect(getAuthCommands(platform, "/repo/nodejs/.npmrc")).toEqual([
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
                args: ["-c", "/repo/nodejs/.npmrc"],
            },
        ]);
        expect(getCommandInvocation(platform, "npm", ["--version"])).toEqual({
            command: "npm",
            args: ["--version"],
        });
    });
});

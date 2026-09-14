import { spawnSync } from "node:child_process";
import { mkdtemp, mkdir, readFile, rm, writeFile } from "node:fs/promises";
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

vi.mock("node:child_process", async (importOriginal) => {
    const original = await importOriginal<typeof import("node:child_process")>();
    return { ...original, spawnSync: vi.fn(original.spawnSync) };
});

const scriptPath = fileURLToPath(new URL("../../scripts/npm-auth-refresh.mjs", import.meta.url));
const temporaryDirectories: string[] = [];
const successfulSpawn: ReturnType<typeof spawnSync> = {
    pid: 1,
    output: [],
    stdout: Buffer.alloc(0),
    stderr: Buffer.alloc(0),
    status: 0,
    signal: null,
};

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
    vi.mocked(spawnSync).mockReset();
    vi.unstubAllEnvs();
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

    it.each(["\n", "\r\n"])(
        "preserves unrelated config content using %j line endings",
        async (newline) => {
            const npmrcPaths = await createTemporaryNpmrcPaths();
            const unrelatedLines = [
                "; Local npm settings",
                "registry=https://registry.npmjs.org/",
                "@other:registry=https://example.com/npm/",
                "# @github:registry=https://example.com/commented/",
                "strict-ssl=true",
            ];
            const existingConfig = [
                ...unrelatedLines.slice(0, 2),
                "@github:registry=https://example.com/old/",
                ...unrelatedLines.slice(2),
                "\t@github:registry = https://example.com/duplicate/",
            ].join(newline);
            await Promise.all(npmrcPaths.map((npmrcPath) => writeFile(npmrcPath, existingConfig)));

            const expected = [
                ...unrelatedLines,
                `@github:registry=${azureFeedLocalRegistry}`,
                "",
            ].join(newline);
            for (let refresh = 0; refresh < 2; refresh++) {
                writeProjectNpmConfigs(npmrcPaths);
                await Promise.all(
                    npmrcPaths.map(async (npmrcPath) => {
                        await expect(readFile(npmrcPath, "utf8")).resolves.toBe(expected);
                    })
                );
            }
        }
    );

    it.each(["strict-ssl=true", "strict-ssl=true\n", "strict-ssl=true\r\n"])(
        "adds the scoped registry without joining existing settings for %j",
        (existingConfig) => {
            const newline = existingConfig.includes("\r\n") ? "\r\n" : "\n";
            const expected = `strict-ssl=true${newline}@github:registry=${azureFeedLocalRegistry}${newline}`;

            expect(buildProjectNpmConfig(existingConfig)).toBe(expected);
            expect(buildProjectNpmConfig(expected)).toBe(expected);
        }
    );

    it("surfaces errors reading existing configs", async () => {
        const npmrcPaths = await createTemporaryNpmrcPaths();
        await mkdir(npmrcPaths[0]);

        expect(() => writeProjectNpmConfigs(npmrcPaths)).toThrow();
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
            ["-config", ".npmrc", "-Force", "-ReadOnly"],
            "win32",
            "C:\\repo\\nodejs"
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
                args: ["-config", ".npmrc", "-Force", "-ReadOnly"],
                cwd: "C:\\repo\\nodejs",
            },
        ]);
    });

    it("launches Windows command shims through the command interpreter", () => {
        expect(getCommandInvocation("win32", "npm.cmd", ["--version"])).toEqual({
            command: "cmd.exe",
            args: ["/d", "/s", "/c", "npm.cmd", "--version"],
        });
    });

    it.each([
        "C:\\repo with spaces\\nodejs\\.npmrc",
        "C:\\repo&other\\nodejs\\.npmrc",
        "C:\\repo%TEMP%^!()\\nodejs\\.npmrc",
    ])("ignores ComSpec and keeps the config path out of shell arguments for %s", (npmrcPath) => {
        vi.stubEnv("ComSpec", "C:\\untrusted\\not-cmd.exe");
        vi.mocked(spawnSync).mockReturnValueOnce(successfulSpawn);
        const { command, args, cwd } = getAuthCommands("win32", npmrcPath)[1];

        runCommand(command, args, "win32", cwd);

        expect(spawnSync).toHaveBeenCalledExactlyOnceWith(
            "cmd.exe",
            ["/d", "/s", "/c", "vsts-npm-auth.cmd", "-config", ".npmrc", "-Force", "-ReadOnly"],
            { stdio: "inherit", cwd: path.win32.dirname(npmrcPath) }
        );
    });

    it("surfaces command spawn errors", () => {
        const error = new Error("Unable to spawn command");
        vi.mocked(spawnSync).mockReturnValueOnce({ ...successfulSpawn, status: null, error });

        expect(() => runCommand("npm", ["--version"], "linux")).toThrow(error);
    });

    it("surfaces nonzero command exit statuses", () => {
        vi.mocked(spawnSync).mockReturnValueOnce({ ...successfulSpawn, status: 7 });

        expect(() => runCommand("npm", ["--version"], "linux")).toThrow("exited with code 7");
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
                args: ["-f", "-c", ".npmrc"],
                cwd: "/repo/nodejs",
            },
        ]);
        expect(getCommandInvocation(platform, "npm", ["--version"])).toEqual({
            command: "npm",
            args: ["--version"],
        });
    });
});

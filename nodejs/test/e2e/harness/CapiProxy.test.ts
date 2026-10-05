/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { spawn } from "child_process";
import { once } from "node:events";
import { existsSync } from "node:fs";
import { copyFile, mkdtemp, readFile, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { dirname, join } from "node:path";
import { createInterface } from "node:readline";
import { beforeEach, describe, expect, inject, it, onTestFinished, vi } from "vitest";
import { CapiProxy } from "./CapiProxy";
import { CAPI_PROXY_BUNDLE } from "./proxyBundleContext";
import { hasChildExited, stopChildProcess, waitForChildExit } from "./sdkTestHelper";

vi.mock("child_process", async (importOriginal) => {
    const actual = await importOriginal<typeof import("child_process")>();
    return { ...actual, spawn: vi.fn(actual.spawn) };
});

vi.mock("./sdkTestHelper", async (importOriginal) => {
    const actual = await importOriginal<typeof import("./sdkTestHelper")>();
    return {
        ...actual,
        stopChildProcess: vi.fn(actual.stopChildProcess),
        waitForChildExit: vi.fn(actual.waitForChildExit),
    };
});

const realSpawn = vi.mocked(spawn).getMockImplementation()!;
const realWaitForChildExit = vi.mocked(waitForChildExit).getMockImplementation()!;

async function startOwnedProxy(): Promise<CapiProxy> {
    const proxy = new CapiProxy();
    const starting = proxy.start();
    onTestFinished(async () => {
        await Promise.allSettled([starting]);
        await proxy.stop(true);
    });
    await starting;
    return proxy;
}

describe("bundled CAPI proxy", () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it("launches the shared bundle directly and waits for a clean shutdown", async () => {
        const proxy = await startOwnedProxy();
        const serverPath = inject(CAPI_PROXY_BUNDLE);
        expect(serverPath).toMatch(/server\.mjs$/);
        expect(existsSync(serverPath)).toBe(true);
        expect(spawn).toHaveBeenCalledExactlyOnceWith(process.execPath, [serverPath], {
            stdio: ["ignore", "pipe", "inherit"],
            windowsHide: true,
        });
        const child = vi.mocked(spawn).mock.results[0].value;
        expect(child.exitCode).toBeNull();
        expect(proxy.getProxyEnv()).toMatchObject({
            HTTPS_PROXY: expect.stringMatching(/^http:\/\/127\.0\.0\.1:/),
            NODE_EXTRA_CA_CERTS: expect.any(String),
        });
        expect(existsSync(proxy.getProxyEnv().NODE_EXTRA_CA_CERTS)).toBe(true);

        const proxyUrl = proxy.url;
        await proxy.stop(true);
        expect(child.exitCode).toBe(0);
        expect(child.signalCode).toBeNull();
        expect(() => proxy.url).toThrow("has not been started");
        await expect(fetch(proxyUrl)).rejects.toThrow();
    });

    it("shares only the bundle, not authentication state, ports, or certificates", async () => {
        const [first, second] = await Promise.all([startOwnedProxy(), startOwnedProxy()]);
        expect(first.url).not.toBe(second.url);
        expect(first.getProxyEnv().HTTPS_PROXY).not.toBe(second.getProxyEnv().HTTPS_PROXY);
        expect(first.getProxyEnv().NODE_EXTRA_CA_CERTS).not.toBe(
            second.getProxyEnv().NODE_EXTRA_CA_CERTS
        );
        expect(vi.mocked(spawn).mock.calls.map((call) => call[1])).toEqual([
            [inject(CAPI_PROXY_BUNDLE)],
            [inject(CAPI_PROXY_BUNDLE)],
        ]);

        await first.setCopilotUserByToken("isolated-token", { login: "first-user" });
        await second.setCopilotUserByToken("isolated-token", { login: "second-user" });
        for (const [proxy, login] of [
            [first, "first-user"],
            [second, "second-user"],
        ] as const) {
            const response = await fetch(`${proxy.url}/copilot_internal/user`, {
                headers: { Authorization: "Bearer isolated-token" },
            });
            expect(response.ok).toBe(true);
            expect(await response.json()).toMatchObject({ login });
        }
    });

    it("runs the self-contained bundle from a path containing spaces", async () => {
        const directory = await mkdtemp(join(tmpdir(), "sdk proxy bundle "));
        const proxy = new CapiProxy();
        let starting: Promise<string> | undefined;
        onTestFinished(async () => {
            await Promise.allSettled(starting ? [starting] : []);
            try {
                await proxy.stop(true);
            } finally {
                await rm(directory, { recursive: true, force: true });
            }
        });
        const serverPath = join(directory, "server with spaces.mjs");
        await copyFile(inject(CAPI_PROXY_BUNDLE), serverPath);
        vi.mocked(spawn).mockImplementationOnce((command, _args, options) =>
            realSpawn(command, [serverPath], options)
        );
        starting = proxy.start();
        await starting;
        await proxy.setCopilotUserByToken("spaces-token", { login: "spaces-user" });
        const response = await fetch(`${proxy.url}/copilot_internal/user`, {
            headers: { Authorization: "Bearer spaces-token" },
        });
        expect(await response.json()).toMatchObject({ login: "spaces-user" });
    });

    it("reports early startup exit and its output", async () => {
        vi.mocked(spawn).mockImplementationOnce((_command, _args, options) =>
            realSpawn(
                process.execPath,
                ["-e", 'console.log("startup failed"); process.exit(23);'],
                options
            )
        );
        await expect(startOwnedProxy()).rejects.toThrow(
            "Proxy exited before startup with code 23: startup failed"
        );
        expect(vi.mocked(spawn).mock.results[0].value.exitCode).toBe(23);
    });

    it("terminates the owned child when startup metadata is invalid", async () => {
        vi.mocked(spawn).mockImplementationOnce((_command, _args, options) =>
            realSpawn(
                process.execPath,
                [
                    "-e",
                    'console.log("Listening: http://127.0.0.1:1 {}"); setInterval(() => {}, 1000);',
                ],
                options
            )
        );
        await expect(startOwnedProxy()).rejects.toThrow("missing CONNECT proxy details");
        const child = vi.mocked(spawn).mock.results[0].value;
        expect(child.exitCode !== null || child.signalCode !== null).toBe(true);
    });

    it("reports spawn failures without waiting for an impossible exit", async () => {
        vi.mocked(spawn).mockImplementationOnce(() =>
            realSpawn(join(dirname(inject(CAPI_PROXY_BUNDLE)), "missing-node-executable"), [], {})
        );
        await expect(startOwnedProxy()).rejects.toMatchObject({ code: "ENOENT" });
    });

    it("retains a child after startup cleanup fails so shutdown can retry", async () => {
        const cleanupError = new Error("Child process did not exit after SIGKILL");
        vi.mocked(stopChildProcess).mockRejectedValueOnce(cleanupError);
        vi.mocked(spawn).mockImplementationOnce((_command, _args, options) =>
            realSpawn(
                process.execPath,
                [
                    "-e",
                    'console.log("Listening: http://127.0.0.1:1 {}"); setInterval(() => {}, 1000);',
                ],
                options
            )
        );
        const proxy = new CapiProxy();
        const starting = proxy.start();
        const child = vi.mocked(spawn).mock.results[0].value;
        onTestFinished(async () => {
            await Promise.allSettled([starting]);
            try {
                await proxy.stop(true);
            } finally {
                await stopChildProcess(child);
            }
        });

        await expect(starting).rejects.toMatchObject({
            message: "Proxy startup and cleanup failed",
            errors: [
                expect.objectContaining({
                    message: expect.stringContaining("missing CONNECT proxy details"),
                }),
                cleanupError,
            ],
        });
        expect(hasChildExited(child)).toBe(false);
        await expect(proxy.start()).rejects.toThrow("already been started");
        expect(spawn).toHaveBeenCalledTimes(1);

        await proxy.stop(true);
        expect(hasChildExited(child)).toBe(true);
    });

    it("retains a running proxy after shutdown cleanup fails so shutdown can retry", async () => {
        const proxy = new CapiProxy();
        const starting = proxy.start();
        const child = vi.mocked(spawn).mock.results[0].value;
        const fetchMock = vi.spyOn(globalThis, "fetch");
        onTestFinished(async () => {
            fetchMock.mockRestore();
            await Promise.allSettled([starting]);
            try {
                await proxy.stop(true);
            } finally {
                await stopChildProcess(child);
            }
        });
        const proxyUrl = await starting;
        const proxyEnv = proxy.getProxyEnv();
        const shutdownError = new Error("Shutdown request failed");
        const cleanupError = new Error("Child process did not exit after SIGKILL");
        fetchMock.mockRejectedValueOnce(shutdownError);
        vi.mocked(stopChildProcess).mockRejectedValueOnce(cleanupError);

        await expect(proxy.stop(true)).rejects.toMatchObject({
            message: "Proxy shutdown and cleanup failed",
            errors: [shutdownError, cleanupError],
        });
        expect(hasChildExited(child)).toBe(false);
        expect(proxy.url).toBe(proxyUrl);
        expect(proxy.getProxyEnv()).toEqual(proxyEnv);
        await expect(proxy.start()).rejects.toThrow("already been started");
        expect(spawn).toHaveBeenCalledTimes(1);

        await proxy.stop(true);
        expect(child.exitCode).toBe(0);
        expect(child.signalCode).toBeNull();
        expect(() => proxy.url).toThrow("has not been started");
        expect(proxy.getProxyEnv()).toEqual({});
        await expect(fetch(proxyUrl)).rejects.toThrow();
    });

    it("allows an acknowledged capture flush to finish beyond the old shutdown cutoff", async () => {
        const directory = await mkdtemp(join(tmpdir(), "sdk proxy flush "));
        const capturePath = join(directory, "capture.yaml");
        vi.mocked(spawn).mockImplementationOnce((_command, _args, options) =>
            realSpawn(
                process.execPath,
                [
                    "-e",
                    `
const { createServer } = require("node:http");
const { writeFile } = require("node:fs/promises");
let releaseFlush;
const flushGate = new Promise(resolve => { releaseFlush = resolve; });
const server = createServer(async (request, response) => {
    if (request.url === "/stop") {
        response.end();
        await writeFile(process.argv[1], "partial capture");
        console.log("Capture flush blocked");
        await flushGate;
        await writeFile(process.argv[1], "complete capture");
        server.close();
        process.exit(0);
    } else if (request.url === "/release") {
        response.end();
        releaseFlush();
    }
});
server.listen(0, "127.0.0.1", () => {
    const url = "http://127.0.0.1:" + server.address().port;
    console.log("Listening: " + url + " " + JSON.stringify({
        connectProxyUrl: url,
        caFilePath: "unused.pem"
    }));
});
`,
                    capturePath,
                ],
                options
            )
        );
        const proxy = new CapiProxy();
        const starting = proxy.start();
        const child = vi.mocked(spawn).mock.results[0].value;
        let outputLines: ReturnType<typeof createInterface> | undefined;
        let stopping: Promise<unknown> | undefined;
        onTestFinished(async () => {
            vi.useRealTimers();
            outputLines?.close();
            try {
                await stopChildProcess(child);
                await Promise.allSettled([starting, ...(stopping ? [stopping] : [])]);
            } finally {
                await rm(directory, { recursive: true, force: true });
            }
        });
        const proxyUrl = await starting;
        // Startup closes its reader and pauses stdout; attach the flush reader afterward.
        outputLines = createInterface({ input: child.stdout! });
        const flushBlocked = once(outputLines, "line");
        let resolveExitWait!: () => void;
        const exitWaitStarted = new Promise<void>((resolve) => {
            resolveExitWait = resolve;
        });
        vi.mocked(waitForChildExit).mockImplementationOnce((process, timeoutMs) => {
            vi.useFakeTimers({ toFake: ["setTimeout", "clearTimeout"] });
            const waiting = realWaitForChildExit(process, timeoutMs);
            resolveExitWait();
            return waiting;
        });
        const kill = vi.spyOn(child, "kill");
        let shutdownSettled = false;
        stopping = proxy.stop().then(
            () => {
                shutdownSettled = true;
            },
            (error: unknown) => {
                shutdownSettled = true;
                return error;
            }
        );
        await exitWaitStarted;
        const [output] = await flushBlocked;
        expect(String(output)).toContain("Capture flush blocked");
        expect(await readFile(capturePath, "utf8")).toBe("partial capture");

        await vi.advanceTimersByTimeAsync(10_000);
        expect(shutdownSettled).toBe(false);
        expect(kill).not.toHaveBeenCalled();
        expect(hasChildExited(child)).toBe(false);

        vi.useRealTimers();
        expect((await fetch(`${proxyUrl}/release`)).ok).toBe(true);
        expect(await stopping).toBeUndefined();
        expect(await readFile(capturePath, "utf8")).toBe("complete capture");
        expect(child.exitCode).toBe(0);
        expect(child.signalCode).toBeNull();
    });
});

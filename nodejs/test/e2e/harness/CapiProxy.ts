/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { spawn, type ChildProcess } from "child_process";
import { createInterface } from "readline";
import { expect, inject } from "vitest";
import type { CapturedRequest, ReplayBackend } from "../../../../test/harness/replayingCapiProxy";
import type {
    CopilotUserResponse,
    MemoryApiStub,
    ParsedHttpExchange,
} from "../../../../test/harness/replayingCapiProxy";
import { isCI } from "./sdkTestContext";
import { CAPI_PROXY_BUNDLE } from "./proxyBundleContext";
import { hasChildExited, stopChildProcess, waitForChildExit } from "./sdkTestHelper";
import { testBackend } from "./testBackend";

const NO_PROXY = "127.0.0.1,localhost,::1";

interface ProxyStartupInfo {
    capiProxyUrl: string;
    connectProxyUrl?: string;
    caFilePath?: string;
}

// Manages a child process that acts as a replaying proxy to the underlying AI endpoints
export class CapiProxy {
    private proxyUrl: string | undefined;
    private startupInfo: ProxyStartupInfo | undefined;
    private serverProcess: ChildProcess | undefined;

    /**
     * Returns the URL of the running proxy. Throws if the proxy has not been started.
     */
    get url(): string {
        if (!this.proxyUrl) {
            throw new Error("CapiProxy has not been started; call start() first.");
        }
        return this.proxyUrl;
    }

    async start(): Promise<string> {
        if (this.serverProcess) {
            throw new Error("CapiProxy has already been started.");
        }
        const serverPath = inject(CAPI_PROXY_BUNDLE);
        if (!serverPath) {
            throw new Error("CapiProxy bundle is missing; enable the SDK Vitest global setup.");
        }
        const serverProcess = spawn(process.execPath, [serverPath], {
            stdio: ["ignore", "pipe", "inherit"],
            windowsHide: true,
        });
        this.serverProcess = serverProcess;

        try {
            this.startupInfo = await new Promise<ProxyStartupInfo>((resolve, reject) => {
                const stdout = serverProcess.stdout!;
                const lines: string[] = [];
                const lineReader = createInterface({ input: stdout });
                const cleanup = () => {
                    lineReader.off("line", onLine);
                    serverProcess.off("exit", onExit);
                    serverProcess.off("error", onError);
                    lineReader.close();
                    // Closing readline pauses stdout even when another reader still owns it.
                    stdout.resume();
                };
                const onLine = (line: string) => {
                    lines.push(line);
                    try {
                        const info = tryParseStartupInfo(line);
                        if (!info) {
                            return;
                        }
                        cleanup();
                        resolve(info);
                    } catch (error) {
                        cleanup();
                        reject(error);
                    }
                };
                const onExit = (code: number | null) => {
                    cleanup();
                    reject(
                        new Error(
                            `Proxy exited before startup with code ${code}: ${lines.join("\n")}`
                        )
                    );
                };
                const onError = (error: Error) => {
                    cleanup();
                    reject(error);
                };
                lineReader.on("line", onLine);
                serverProcess.once("exit", onExit);
                serverProcess.once("error", onError);
            });
        } catch (error) {
            try {
                // A failed spawn has no PID and cannot emit an exit event.
                if (serverProcess.pid !== undefined) {
                    await stopChildProcess(serverProcess);
                }
            } catch (cleanupError) {
                throw new AggregateError([error, cleanupError], "Proxy startup and cleanup failed");
            } finally {
                if (serverProcess.pid === undefined || hasChildExited(serverProcess)) {
                    this.serverProcess = undefined;
                }
            }
            throw error;
        }
        this.proxyUrl = this.startupInfo.capiProxyUrl;

        return this.proxyUrl;
    }

    getProxyEnv(): Record<string, string> {
        if (!this.startupInfo?.connectProxyUrl || !this.startupInfo.caFilePath) {
            return {};
        }

        return {
            HTTP_PROXY: this.startupInfo.connectProxyUrl,
            HTTPS_PROXY: this.startupInfo.connectProxyUrl,
            http_proxy: this.startupInfo.connectProxyUrl,
            https_proxy: this.startupInfo.connectProxyUrl,
            NO_PROXY,
            no_proxy: NO_PROXY,
            NODE_EXTRA_CA_CERTS: this.startupInfo.caFilePath,
            SSL_CERT_FILE: this.startupInfo.caFilePath,
            REQUESTS_CA_BUNDLE: this.startupInfo.caFilePath,
            CURL_CA_BUNDLE: this.startupInfo.caFilePath,
            GIT_SSL_CAINFO: this.startupInfo.caFilePath,
            GH_TOKEN: "",
            GH_ENTERPRISE_TOKEN: "",
            GITHUB_ENTERPRISE_TOKEN: "",

            // In CI we never want it to make real network requests, so there should be no need for auth
            // But when running locally you have to be able to generate snapshots and that does require real auth,
            // so you should set GH_TOKEN and we need to pass it through into the test app.
            ...(isCI ? { GITHUB_TOKEN: "" } : undefined),
        };
    }

    async updateConfig(config: {
        filePath: string;
        workDir: string;
        backend?: ReplayBackend;
        testInfo?: { file: string; line?: number };
        modelNames?: Record<string, string>;
    }): Promise<void> {
        const response = await fetch(`${this.proxyUrl}/config`, {
            method: "POST",
            headers: { "content-type": "application/json" },
            body: JSON.stringify({ backend: testBackend, ...config }),
        });
        expect(response.ok).toBe(true);
    }

    async getExchanges(): Promise<ParsedHttpExchange[]> {
        const response = await fetch(`${this.proxyUrl}/exchanges`, { method: "GET" });
        return (await response.json()) as ParsedHttpExchange[];
    }

    async getRequests(): Promise<CapturedRequest[]> {
        const response = await fetch(`${this.proxyUrl}/requests`, { method: "GET" });
        return (await response.json()) as CapturedRequest[];
    }

    async stop(skipWritingCache?: boolean): Promise<void> {
        const serverProcess = this.serverProcess;
        if (!serverProcess) {
            return;
        }
        try {
            if (!this.proxyUrl) {
                await stopChildProcess(serverProcess);
                return;
            }
            const url = skipWritingCache
                ? `${this.proxyUrl}/stop?skipWritingCache=true`
                : `${this.proxyUrl}/stop`;
            const response = await fetch(url, { method: "POST" });
            expect(response.ok).toBe(true);
            // /stop acknowledges before captures are flushed; do not interrupt that write.
            await waitForChildExit(serverProcess);
            if (serverProcess.exitCode !== 0 || serverProcess.signalCode !== null) {
                throw new Error(
                    `Proxy exited with code ${serverProcess.exitCode}, signal ${serverProcess.signalCode}`
                );
            }
        } catch (error) {
            try {
                await stopChildProcess(serverProcess);
            } catch (cleanupError) {
                throw new AggregateError(
                    [error, cleanupError],
                    "Proxy shutdown and cleanup failed"
                );
            }
            throw error;
        } finally {
            if (serverProcess.pid === undefined || hasChildExited(serverProcess)) {
                this.serverProcess = undefined;
                this.proxyUrl = undefined;
                this.startupInfo = undefined;
            }
        }
    }

    /**
     * Register a per-token response for the `/copilot_internal/user` endpoint.
     * When a request with `Authorization: Bearer <token>` arrives at the proxy,
     * the matching response is returned.
     */
    async setCopilotUserByToken(token: string, response: CopilotUserResponse): Promise<void> {
        const res = await fetch(`${this.proxyUrl}/copilot-user-config`, {
            method: "POST",
            headers: { "content-type": "application/json" },
            body: JSON.stringify({ token, response }),
        });
        expect(res.ok).toBe(true);
    }

    async setMemoryApiStub(stub: MemoryApiStub): Promise<void> {
        const response = await fetch(`${this.proxyUrl}/memory-api-config`, {
            method: "POST",
            headers: { "content-type": "application/json" },
            body: JSON.stringify(stub),
        });
        expect(response.ok).toBe(true);
    }
}

function tryParseStartupInfo(line: string): ProxyStartupInfo | undefined {
    if (!line) {
        return undefined;
    }

    const match = line.match(/Listening: (http:\/\/[^\s]+)\s+(\{.*\})$/);
    if (!match) {
        if (!line.includes("Listening: ")) {
            return undefined;
        }
        throw new Error(`Unexpected proxy output: ${line}`);
    }

    const metadata = JSON.parse(match[2]) as Partial<ProxyStartupInfo>;
    if (!metadata.connectProxyUrl || !metadata.caFilePath) {
        throw new Error(`Proxy startup metadata missing CONNECT proxy details: ${line}`);
    }
    return {
        capiProxyUrl: match[1],
        connectProxyUrl: metadata.connectProxyUrl,
        caFilePath: metadata.caFilePath,
    };
}

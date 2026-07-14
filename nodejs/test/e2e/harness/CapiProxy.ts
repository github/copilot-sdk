import { ChildProcess, spawn } from "child_process";
import { resolve } from "path";
import { createInterface } from "readline";
import { expect } from "vitest";
import {
    CopilotUserResponse,
    ParsedHttpExchange,
} from "../../../../test/harness/replayingCapiProxy";
import { isCI } from "./sdkTestContext";

const HARNESS_SERVER_PATH = resolve(__dirname, "../../../../test/harness/server.ts");
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
        const serverProcess = spawn("npx", ["tsx", HARNESS_SERVER_PATH], {
            stdio: ["ignore", "pipe", "inherit"],
            shell: true,
        });
        this.serverProcess = serverProcess;

        this.startupInfo = await new Promise<ProxyStartupInfo>((resolve, reject) => {
            const stdout = serverProcess.stdout!;
            const lines: string[] = [];
            const lineReader = createInterface({ input: stdout });
            const cleanup = () => {
                lineReader.off("line", onLine);
                serverProcess.off("exit", onExit);
                lineReader.close();
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
                    new Error(`Proxy exited before startup with code ${code}: ${lines.join("\n")}`)
                );
            };
            lineReader.on("line", onLine);
            serverProcess.once("exit", onExit);
        });
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
        testInfo?: { file: string; line?: number };
    }): Promise<void> {
        const response = await fetch(`${this.proxyUrl}/config`, {
            method: "POST",
            headers: { "content-type": "application/json" },
            body: JSON.stringify(config),
        });
        expect(response.ok).toBe(true);
    }

    async getExchanges(): Promise<ParsedHttpExchange[]> {
        const response = await fetch(`${this.proxyUrl}/exchanges`, { method: "GET" });
        return await response.json();
    }

    async stop(skipWritingCache?: boolean): Promise<void> {
        const process = this.serverProcess;
        if (!process) {
            return;
        }

        const url = skipWritingCache
            ? `${this.proxyUrl}/stop?skipWritingCache=true`
            : `${this.proxyUrl}/stop`;
        try {
            const controller = new AbortController();
            const timeout = setTimeout(() => controller.abort(), 5000);
            try {
                const response = await fetch(url, { method: "POST", signal: controller.signal });
                expect(response.ok).toBe(true);
            } finally {
                clearTimeout(timeout);
            }
        } catch {
            // Best-effort graceful stop; process termination below is the hard guarantee.
        }

        if (!(await waitForProcessExit(process, 5000))) {
            await terminateProcessTree(process);
            await waitForProcessExit(process, 5000);
        }
        this.serverProcess = undefined;
        this.proxyUrl = undefined;
        this.startupInfo = undefined;
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

}

// Wait for proxy child exit so teardown doesn't leave hanging harness processes.
async function waitForProcessExit(process: ChildProcess, timeoutMs: number): Promise<boolean> {
    if (process.exitCode !== null || process.signalCode !== null) {
        return true;
    }
    return await new Promise<boolean>((resolve) => {
        const onExit = () => {
            clearTimeout(timer);
            process.off("exit", onExit);
            resolve(true);
        };
        const timer = setTimeout(() => {
            process.off("exit", onExit);
            resolve(false);
        }, timeoutMs);
        process.once("exit", onExit);
    });
}

async function terminateProcessTree(child: ChildProcess): Promise<void> {
    if (child.exitCode !== null || child.signalCode !== null) {
        return;
    }
    const pid = child.pid;
    if (!pid) {
        return;
    }

    if (process.platform === "win32") {
        await new Promise<void>((resolve) => {
            const taskkill = spawn("taskkill", ["/PID", String(pid), "/T", "/F"], {
                stdio: "ignore",
            });
            taskkill.once("error", () => resolve());
            taskkill.once("exit", () => resolve());
        });
        return;
    }

    try {
        child.kill();
    } catch {
        // Process may have already exited between wait timeout and kill attempt.
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

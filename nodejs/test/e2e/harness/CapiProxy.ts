import { spawn } from "child_process";
import { resolve } from "path";
import { expect } from "vitest";
import {
    CopilotUserResponse,
    ParsedHttpExchange,
} from "../../../../test/harness/replayingCapiProxy";

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

        this.startupInfo = await new Promise<ProxyStartupInfo>((resolve, reject) => {
            let output = "";
            const cleanup = () => {
                serverProcess.stdout!.off("data", onData);
                serverProcess.off("exit", onExit);
            };
            const onData = (chunk: Buffer) => {
                output += chunk.toString();
                const info = tryParseStartupInfo(output);
                if (info) {
                    cleanup();
                    resolve(info);
                }
            };
            const onExit = (code: number | null) => {
                cleanup();
                reject(new Error(`Proxy exited before startup with code ${code}: ${output}`));
            };
            serverProcess.stdout!.on("data", onData);
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
            GITHUB_TOKEN: "",
            GH_ENTERPRISE_TOKEN: "",
            GITHUB_ENTERPRISE_TOKEN: "",
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
        const url = skipWritingCache
            ? `${this.proxyUrl}/stop?skipWritingCache=true`
            : `${this.proxyUrl}/stop`;
        const response = await fetch(url, { method: "POST" });
        expect(response.ok).toBe(true);
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

function tryParseStartupInfo(output: string): ProxyStartupInfo | undefined {
    const line = output.split(/\r?\n/).find((candidate) => candidate.includes("Listening: "));
    if (!line) {
        return undefined;
    }

    const match = line.match(/Listening: (http:\/\/[^\s]+)(?:\s+(\{.*\}))?/);
    if (!match) {
        throw new Error(`Unexpected proxy output: ${line}`);
    }

    const metadata = match[2] ? (JSON.parse(match[2]) as Partial<ProxyStartupInfo>) : {};
    return {
        capiProxyUrl: match[1],
        connectProxyUrl: metadata.connectProxyUrl,
        caFilePath: metadata.caFilePath,
    };
}

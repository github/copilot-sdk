import { spawn } from "child_process";
import { resolve } from "path";
import { expect } from "vitest";
import {
    CopilotUserResponse,
    ParsedHttpExchange,
} from "../../../../test/harness/replayingCapiProxy";

const HARNESS_SERVER_PATH = resolve(__dirname, "../../../../test/harness/server.ts");

// Manages a child process that acts as a replaying proxy to the underlying AI endpoints
export class CapiProxy {
    private proxyUrl: string | undefined;

    async start(): Promise<string> {
        const serverProcess = spawn("npx", ["tsx", HARNESS_SERVER_PATH], {
            stdio: ["ignore", "pipe", "inherit"],
            shell: true,
        });

        this.proxyUrl = await new Promise<string>((resolve) => {
            serverProcess.stdout!.once("data", (chunk: Buffer) => {
                const match = chunk.toString().match(/Listening: (http:\/\/[^\s]+)/);
                resolve(match![1]);
            });
        });

        return this.proxyUrl;
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

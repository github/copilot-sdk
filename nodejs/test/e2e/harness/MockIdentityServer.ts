/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { ChildProcess, spawn } from "child_process";
import { resolve } from "path";
import { createInterface } from "readline";
import type {
    MockIdentityEndpointInfo,
    RecordedIdentityRequest,
} from "../../../../test/harness/mockIdentityEndpoint";

const ENTRYPOINT_PATH = resolve(__dirname, "../../../../test/harness/mockIdentityServer.ts");

/**
 * Spawns the shared mock managed identity endpoint (`test/harness/mockIdentityServer.ts`)
 * as a child process and exposes its endpoint + control URLs. This is the
 * Node reference consumer of the shared endpoint; other SDK languages spawn the
 * same entrypoint and parse its `Listening:` banner the same way.
 */
export class MockIdentityServer {
    private process: ChildProcess | undefined;
    private info: MockIdentityEndpointInfo | undefined;

    async start(): Promise<MockIdentityEndpointInfo> {
        const child = spawn("npx", ["tsx", ENTRYPOINT_PATH], {
            stdio: ["ignore", "pipe", "inherit"],
            shell: true,
        });
        this.process = child;

        this.info = await new Promise<MockIdentityEndpointInfo>((resolveInfo, reject) => {
            const reader = createInterface({ input: child.stdout! });
            const lines: string[] = [];
            const cleanup = () => {
                reader.off("line", onLine);
                child.off("exit", onExit);
                reader.close();
            };
            const onLine = (line: string) => {
                lines.push(line);
                const match = line.match(/^Listening: (\{.*\})$/);
                if (!match) {
                    return;
                }
                try {
                    const info = JSON.parse(match[1]) as MockIdentityEndpointInfo;
                    cleanup();
                    resolveInfo(info);
                } catch (error) {
                    cleanup();
                    reject(error);
                }
            };
            const onExit = (code: number | null) => {
                cleanup();
                reject(
                    new Error(
                        `Mock identity server exited before startup with code ${code}: ${lines.join("\n")}`
                    )
                );
            };
            reader.on("line", onLine);
            child.once("exit", onExit);
        });

        return this.info;
    }

    /** URL to assign to the `IDENTITY_ENDPOINT` env var. */
    get endpoint(): string {
        return this.requireInfo().endpoint;
    }

    /** Secret to assign to the `IDENTITY_HEADER` env var. */
    get header(): string {
        return this.requireInfo().header;
    }

    /** Fake bearer token the runtime injects (`Authorization: Bearer <token>`). */
    get token(): string {
        return this.requireInfo().token;
    }

    /** Token requests recorded by the endpoint so far, in arrival order. */
    async getRecordedRequests(): Promise<RecordedIdentityRequest[]> {
        const response = await fetch(this.requireInfo().recordedUrl);
        return (await response.json()) as RecordedIdentityRequest[];
    }

    /** Clears the endpoint's recorded token requests. */
    async reset(): Promise<void> {
        await fetch(this.requireInfo().resetUrl, { method: "POST" });
    }

    async stop(): Promise<void> {
        const child = this.process;
        if (!child) {
            return;
        }
        this.process = undefined;
        const exited = new Promise<void>((resolveExit) => child.once("exit", () => resolveExit()));
        try {
            await fetch(this.requireInfo().stopUrl, { method: "POST" });
        } catch {
            // Endpoint may already be gone; fall back to killing the process.
        }
        const killTimer = setTimeout(() => child.kill(), 2_000);
        await exited;
        clearTimeout(killTimer);
    }

    private requireInfo(): MockIdentityEndpointInfo {
        if (!this.info) {
            throw new Error("MockIdentityServer has not been started; call start() first.");
        }
        return this.info;
    }
}

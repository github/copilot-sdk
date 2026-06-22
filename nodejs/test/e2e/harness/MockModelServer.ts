/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { ChildProcess, spawn } from "child_process";
import { resolve } from "path";
import { createInterface } from "readline";
import type {
    MockModelEndpointInfo,
    RecordedModelRequest,
} from "../../../../test/harness/mockModelEndpoint";

const ENTRYPOINT_PATH = resolve(__dirname, "../../../../test/harness/mockModelServer.ts");

/**
 * Spawns the shared mock BYOK model endpoint (`test/harness/mockModelServer.ts`)
 * as a child process and exposes its base + control URLs. This is the Node
 * reference consumer of the shared endpoint; other SDK languages spawn the same
 * entrypoint and parse its `Listening:` banner the same way.
 */
export class MockModelServer {
    private process: ChildProcess | undefined;
    private info: MockModelEndpointInfo | undefined;

    async start(): Promise<MockModelEndpointInfo> {
        const child = spawn("npx", ["tsx", ENTRYPOINT_PATH], {
            stdio: ["ignore", "pipe", "inherit"],
            shell: true,
        });
        this.process = child;

        this.info = await new Promise<MockModelEndpointInfo>((resolveInfo, reject) => {
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
                    const info = JSON.parse(match[1]) as MockModelEndpointInfo;
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
                        `Mock model server exited before startup with code ${code}: ${lines.join("\n")}`
                    )
                );
            };
            reader.on("line", onLine);
            child.once("exit", onExit);
        });

        return this.info;
    }

    /** Base URL to assign as the BYOK provider's `baseUrl`. */
    get baseUrl(): string {
        return this.requireInfo().baseUrl;
    }

    /** Inference requests recorded by the endpoint so far, in arrival order. */
    async getRecordedRequests(): Promise<RecordedModelRequest[]> {
        const response = await fetch(this.requireInfo().recordedUrl);
        return (await response.json()) as RecordedModelRequest[];
    }

    /** Clears the endpoint's recorded inference requests. */
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

    private requireInfo(): MockModelEndpointInfo {
        if (!this.info) {
            throw new Error("MockModelServer has not been started; call start() first.");
        }
        return this.info;
    }
}

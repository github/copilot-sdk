/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { randomUUID } from "node:crypto";
import { describe, expect, it } from "vitest";
import { CopilotClient, RuntimeConnection } from "../../src/index.js";
import { createSdkTestContext, DEFAULT_GITHUB_TOKEN } from "./harness/sdkTestContext.js";
import { formatError } from "./harness/sdkTestHelper.js";

describe("Server-scoped remote-control RPC", async () => {
    const { env, workDir } = await createSdkTestContext();

    function createDedicatedClient(): CopilotClient {
        return new CopilotClient({
            workingDirectory: workDir,
            env,
            logLevel: "error",
            connection: RuntimeConnection.forStdio({ path: process.env.COPILOT_CLI_PATH }),
            gitHubToken: DEFAULT_GITHUB_TOKEN,
        });
    }

    async function forceStop(client: CopilotClient): Promise<void> {
        try {
            await client.forceStop();
        } catch {
            // Runtime may already be gone.
        }
    }

    function uniqueSessionId(prefix: string): string {
        return `${prefix}-${randomUUID().replace(/-/g, "")}`;
    }

    it("should report remote control status as off", { timeout: 120_000 }, async () => {
        const client = createDedicatedClient();
        try {
            await client.start();

            const result = await client.rpc.sessions.getRemoteControlStatus();

            expect(result.status.state).toBe("off");
        } finally {
            await forceStop(client);
        }
    });

    it("should treat set steering as no op when off", { timeout: 120_000 }, async () => {
        const client = createDedicatedClient();
        try {
            await client.start();

            const result = await client.rpc.sessions.setRemoteControlSteering({ enabled: false });

            expect(result.status.state).toBe("off");
        } finally {
            await forceStop(client);
        }
    });

    it("should report not stopped when remote control is off", { timeout: 120_000 }, async () => {
        const client = createDedicatedClient();
        try {
            await client.start();

            const result = await client.rpc.sessions.stopRemoteControl({});

            expect(result.stopped).toBe(false);
            expect(result.status.state).toBe("off");
        } finally {
            await forceStop(client);
        }
    });

    it("should reject transfer when off with compare and swap", { timeout: 120_000 }, async () => {
        const client = createDedicatedClient();
        try {
            await client.start();

            const result = await client.rpc.sessions.transferRemoteControl({
                toSessionId: uniqueSessionId("rc-to"),
                expectedFromSessionId: uniqueSessionId("rc-from"),
            });

            expect(result.transferred).toBe(false);
            expect(result.status.state).toBe("off");
        } finally {
            await forceStop(client);
        }
    });

    it(
        "should reach runtime when starting remote control for unknown session",
        { timeout: 120_000 },
        async () => {
            const client = createDedicatedClient();
            try {
                await client.start();

                await expect(
                    client.rpc.sessions.startRemoteControl({
                        sessionId: uniqueSessionId("missing-session"),
                        config: {
                            remote: false,
                            explicit: false,
                            silent: true,
                            steerable: false,
                        },
                    })
                ).rejects.toSatisfy((error: unknown) => {
                    const message = formatError(error);
                    expect(message.toLowerCase()).not.toContain("unhandled method");
                    expect(
                        message.toLowerCase().includes("session") ||
                            message.toLowerCase().includes("remote")
                    ).toBe(true);
                    return true;
                });
            } finally {
                try {
                    await client.rpc.sessions.stopRemoteControl({ force: true });
                } catch {
                    // Best-effort reset.
                }
                await forceStop(client);
            }
        }
    );
});

/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it, afterAll } from "vitest";
import { z } from "zod";
import { CopilotClient, defineTool, approveAll } from "../../src/index.js";
import type { SessionEvent } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext";

describe("Multi-client broadcast", async () => {
    // Use TCP mode so a second client can connect to the same CLI process
    const tcpConnectionToken = "multi-client-test-token";
    const ctx = await createSdkTestContext({
        useStdio: false,
        copilotClientOptions: { tcpConnectionToken },
    });
    const client1 = ctx.copilotClient;

    // Trigger connection so we can read the port
    const initSession = await client1.createSession({ onPermissionRequest: approveAll });
    await initSession.disconnect();

    const actualPort = (client1 as unknown as { actualPort: number }).actualPort;
    let client2 = new CopilotClient({ cliUrl: `localhost:${actualPort}`, tcpConnectionToken });
    const EVENT_TIMEOUT_MS = 30_000;

    afterAll(async () => {
        await client2.stop();
    });

    async function withTimeout<T>(promise: Promise<T>, ms: number, label: string): Promise<T> {
        let timer: ReturnType<typeof setTimeout> | undefined;
        try {
            return await Promise.race([
                promise,
                new Promise<T>((_, reject) => {
                    timer = setTimeout(() => reject(new Error(`Timeout: ${label}`)), ms);
                }),
            ]);
        } finally {
            if (timer) clearTimeout(timer);
        }
    }

    function waitForEvent(
        session: { on: (handler: (event: SessionEvent) => void) => () => void },
        type: SessionEvent["type"],
        label: string
    ): Promise<SessionEvent> {
        return withTimeout(
            new Promise<SessionEvent>((resolve) => {
                const unsub = session.on((event) => {
                    if (event.type === type) {
                        unsub();
                        resolve(event);
                    }
                });
            }),
            EVENT_TIMEOUT_MS,
            label
        );
    }

    it("both clients see tool request and completion events", async () => {
        const tool = defineTool("magic_number", {
            description: "Returns a magic number",
            parameters: z.object({
                seed: z.string().describe("A seed value"),
            }),
            handler: ({ seed }) => `MAGIC_${seed}_42`,
        });

        // Client 1 creates a session with a custom tool
        const session1 = await client1.createSession({
            onPermissionRequest: approveAll,
            tools: [tool],
        });

        // Client 2 resumes with NO tools — should not overwrite client 1's tools
        const session2 = await client2.resumeSession(session1.sessionId, {
            onPermissionRequest: approveAll,
        });

        // Set up event waiters BEFORE sending the prompt to avoid race conditions
        const client1RequestedP = waitForEvent(
            session1,
            "external_tool.requested",
            "client1 external_tool.requested"
        );
        const client2RequestedP = waitForEvent(
            session2,
            "external_tool.requested",
            "client2 external_tool.requested"
        );
        const client1CompletedP = waitForEvent(
            session1,
            "external_tool.completed",
            "client1 external_tool.completed"
        );
        const client2CompletedP = waitForEvent(
            session2,
            "external_tool.completed",
            "client2 external_tool.completed"
        );

        // Send a prompt that triggers the custom tool
        const response = await session1.sendAndWait({
            prompt: "Use the magic_number tool with seed 'hello' and tell me the result",
        });

        // The response should contain the tool's output
        expect(response?.data.content).toContain("MAGIC_hello_42");

        // Wait for all broadcast events to arrive on both clients
        await expect(
            Promise.all([
                client1RequestedP,
                client2RequestedP,
                client1CompletedP,
                client2CompletedP,
            ])
        ).resolves.toBeDefined();

        await session2.disconnect();
    });

    it("one client approves permission and both see the result", async () => {
        const client1PermissionRequests: unknown[] = [];

        // Client 1 creates a session and manually approves permission requests
        const session1 = await client1.createSession({
            onPermissionRequest: (request) => {
                client1PermissionRequests.push(request);
                return { kind: "approve-once" as const };
            },
        });

        // Client 2 observes the permission request but leaves the decision to client 1.
        const session2 = await client2.resumeSession(session1.sessionId, {
            onPermissionRequest: () => ({ kind: "no-result" as const }),
        });

        const client1PermRequestedP = waitForEvent(
            session1,
            "permission.requested",
            "client1 permission.requested"
        );
        const client2PermRequestedP = waitForEvent(
            session2,
            "permission.requested",
            "client2 permission.requested"
        );
        const client1PermCompletedP = waitForEvent(
            session1,
            "permission.completed",
            "client1 permission.completed"
        );
        const client2PermCompletedP = waitForEvent(
            session2,
            "permission.completed",
            "client2 permission.completed"
        );

        // Send a prompt that triggers a write operation (requires permission)
        const response = await session1.sendAndWait({
            prompt: "Create a file called hello.txt containing the text 'hello world'",
        });

        expect(response?.data.content).toBeTruthy();

        // Client 1 should have handled the permission request
        expect(client1PermissionRequests.length).toBeGreaterThan(0);

        // Both clients should have seen permission.requested events
        await client1PermRequestedP;
        await client2PermRequestedP;

        // Both clients should have seen permission.completed events with approved result
        const client1PermCompleted = await client1PermCompletedP;
        const client2PermCompleted = await client2PermCompletedP;
        for (const event of [client1PermCompleted, client2PermCompleted]) {
            expect(event.type).toBe("permission.completed");
            if (event.type !== "permission.completed") continue;
            expect(event.data.result.kind).toBe("approved");
        }

        await session2.disconnect();
    });

    it("one client rejects permission and both see the result", async () => {
        // Client 1 creates a session and denies all permission requests
        const session1 = await client1.createSession({
            onPermissionRequest: () => ({ kind: "reject" as const }),
        });

        // Client 2 observes the permission request but leaves the decision to client 1.
        const session2 = await client2.resumeSession(session1.sessionId, {
            onPermissionRequest: () => ({ kind: "no-result" as const }),
        });

        const client1PermRequestedP = waitForEvent(
            session1,
            "permission.requested",
            "client1 permission.requested"
        );
        const client2PermRequestedP = waitForEvent(
            session2,
            "permission.requested",
            "client2 permission.requested"
        );
        const client1PermCompletedP = waitForEvent(
            session1,
            "permission.completed",
            "client1 permission.completed"
        );
        const client2PermCompletedP = waitForEvent(
            session2,
            "permission.completed",
            "client2 permission.completed"
        );

        // Ask the agent to write a file (requires permission)
        const { writeFile } = await import("fs/promises");
        const { join } = await import("path");
        const testFile = join(ctx.workDir, "protected.txt");
        await writeFile(testFile, "protected content");

        await session1.sendAndWait({
            prompt: "Edit protected.txt and replace 'protected' with 'hacked'.",
        });

        // Verify the file was NOT modified (permission was denied)
        const { readFile } = await import("fs/promises");
        const content = await readFile(testFile, "utf-8");
        expect(content).toBe("protected content");

        // Both clients should have seen permission.requested and permission.completed
        await client1PermRequestedP;
        await client2PermRequestedP;

        // Both clients should see the denial in the completed event
        const client1PermCompleted = await client1PermCompletedP;
        const client2PermCompleted = await client2PermCompletedP;
        for (const event of [client1PermCompleted, client2PermCompleted]) {
            expect(event.type).toBe("permission.completed");
            if (event.type !== "permission.completed") continue;
            expect(event.data.result.kind).toBe("denied-interactively-by-user");
        }

        await session2.disconnect();
    });

    it(
        "two clients register different tools and agent uses both",
        { timeout: 90_000 },
        async () => {
            const toolA = defineTool("city_lookup", {
                description: "Returns a city name for a given country code",
                parameters: z.object({
                    countryCode: z.string().describe("A two-letter country code"),
                }),
                handler: ({ countryCode }) => `CITY_FOR_${countryCode}`,
            });

            const toolB = defineTool("currency_lookup", {
                description: "Returns a currency for a given country code",
                parameters: z.object({
                    countryCode: z.string().describe("A two-letter country code"),
                }),
                handler: ({ countryCode }) => `CURRENCY_FOR_${countryCode}`,
            });

            // Client 1 creates a session with tool A
            const session1 = await client1.createSession({
                onPermissionRequest: approveAll,
                tools: [toolA],
            });

            // Client 2 resumes with tool B (different tool, union should have both)
            const session2 = await client2.resumeSession(session1.sessionId, {
                onPermissionRequest: approveAll,
                tools: [toolB],
            });

            // Send prompts sequentially to avoid nondeterministic tool_call ordering
            const response1 = await session1.sendAndWait({
                prompt: "Use the city_lookup tool with countryCode 'US' and tell me the result.",
            });
            expect(response1?.data.content).toContain("CITY_FOR_US");

            const response2 = await session1.sendAndWait({
                prompt: "Now use the currency_lookup tool with countryCode 'US' and tell me the result.",
            });
            expect(response2?.data.content).toContain("CURRENCY_FOR_US");

            await session2.disconnect();
        }
    );

    it("disconnecting client removes its tools", { timeout: 90_000 }, async () => {
        const toolA = defineTool("stable_tool", {
            description: "A tool that persists across disconnects",
            parameters: z.object({ input: z.string() }),
            handler: ({ input }) => `STABLE_${input}`,
        });

        const toolB = defineTool("ephemeral_tool", {
            description: "A tool that will disappear when its client disconnects",
            parameters: z.object({ input: z.string() }),
            handler: ({ input }) => `EPHEMERAL_${input}`,
        });

        // Client 1 creates a session with stable_tool
        const session1 = await client1.createSession({
            onPermissionRequest: approveAll,
            tools: [toolA],
        });

        // Client 2 resumes with ephemeral_tool
        await client2.resumeSession(session1.sessionId, {
            onPermissionRequest: approveAll,
            tools: [toolB],
        });

        // Verify both tools work before disconnect (sequential to avoid nondeterministic tool_call ordering)
        const stableResponse = await session1.sendAndWait({
            prompt: "Use the stable_tool with input 'test1' and tell me the result.",
        });
        expect(stableResponse?.data.content).toContain("STABLE_test1");

        const ephemeralResponse = await session1.sendAndWait({
            prompt: "Use the ephemeral_tool with input 'test2' and tell me the result.",
        });
        expect(ephemeralResponse?.data.content).toContain("EPHEMERAL_test2");

        // Disconnect client 2 without destroying the shared session.
        // Suppress "Connection is disposed" rejections that occur when the server
        // broadcasts events (e.g. tool_changed_notice) to the now-dead connection.
        const suppressDisposed = (reason: unknown) => {
            if (reason instanceof Error && reason.message.includes("Connection is disposed")) {
                return;
            }
            throw reason;
        };
        process.on("unhandledRejection", suppressDisposed);
        await client2.forceStop();

        // Give the server time to process the connection close and remove tools
        await new Promise((resolve) => setTimeout(resolve, 500));
        process.removeListener("unhandledRejection", suppressDisposed);

        // Recreate client2 for cleanup in afterAll (but don't rejoin the session)
        client2 = new CopilotClient({ cliUrl: `localhost:${actualPort}`, tcpConnectionToken });

        // Now only stable_tool should be available
        const afterResponse = await session1.sendAndWait({
            prompt: "Use the stable_tool with input 'still_here'. Also try using ephemeral_tool if it is available.",
        });
        expect(afterResponse?.data.content).toContain("STABLE_still_here");
        // ephemeral_tool should NOT have produced a result
        expect(afterResponse?.data.content).not.toContain("EPHEMERAL_");
    });
});

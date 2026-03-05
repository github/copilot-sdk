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
    const ctx = await createSdkTestContext({ useStdio: false });
    const client1 = ctx.copilotClient;

    // Trigger connection so we can read the port
    const initSession = await client1.createSession({ onPermissionRequest: approveAll });
    await initSession.destroy();

    const actualPort = (client1 as unknown as { actualPort: number }).actualPort;
    const client2 = new CopilotClient({ cliUrl: `localhost:${actualPort}` });

    afterAll(async () => {
        await client2.stop();
    });

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

        // Client 2 resumes the same session (separate TCP connection, own handlers)
        const session2 = await client2.resumeSession(session1.sessionId, {
            onPermissionRequest: approveAll,
            tools: [tool],
        });

        // Track events seen by each client
        const client1Events: SessionEvent[] = [];
        const client2Events: SessionEvent[] = [];

        session1.on((event) => client1Events.push(event));
        session2.on((event) => client2Events.push(event));

        // Send a prompt that triggers the custom tool
        const response = await session1.sendAndWait({
            prompt: "Use the magic_number tool with seed 'hello' and tell me the result",
        });

        // The response should contain the tool's output
        expect(response?.data.content).toContain("MAGIC_hello_42");

        // Both clients should have seen the external_tool.requested event
        const client1ToolRequested = client1Events.filter(
            (e) => e.type === "external_tool.requested"
        );
        const client2ToolRequested = client2Events.filter(
            (e) => e.type === "external_tool.requested"
        );
        expect(client1ToolRequested.length).toBeGreaterThan(0);
        expect(client2ToolRequested.length).toBeGreaterThan(0);

        // Both clients should have seen the external_tool.completed event
        const client1ToolCompleted = client1Events.filter(
            (e) => e.type === "external_tool.completed"
        );
        const client2ToolCompleted = client2Events.filter(
            (e) => e.type === "external_tool.completed"
        );
        expect(client1ToolCompleted.length).toBeGreaterThan(0);
        expect(client2ToolCompleted.length).toBeGreaterThan(0);

        await session2.destroy();
    });

    it("one client approves permission and both see the result", async () => {
        const client1PermissionRequests: unknown[] = [];

        // Client 1 creates a session and manually approves permission requests
        const session1 = await client1.createSession({
            onPermissionRequest: (request) => {
                client1PermissionRequests.push(request);
                return { kind: "approved" as const };
            },
        });

        // Client 2 resumes the same session — no permission handler needed,
        // it just observes the broadcast events
        const session2 = await client2.resumeSession(session1.sessionId, {
            onPermissionRequest: approveAll,
        });

        // Track events seen by each client
        const client1Events: SessionEvent[] = [];
        const client2Events: SessionEvent[] = [];

        session1.on((event) => client1Events.push(event));
        session2.on((event) => client2Events.push(event));

        // Send a prompt that triggers a write operation (requires permission)
        const response = await session1.sendAndWait({
            prompt: "Create a file called hello.txt containing the text 'hello world'",
        });

        expect(response?.data.content).toBeTruthy();

        // Client 1 should have handled the permission request
        expect(client1PermissionRequests.length).toBeGreaterThan(0);

        // Both clients should have seen permission.requested events
        const client1PermRequested = client1Events.filter(
            (e) => e.type === "permission.requested"
        );
        const client2PermRequested = client2Events.filter(
            (e) => e.type === "permission.requested"
        );
        expect(client1PermRequested.length).toBeGreaterThan(0);
        expect(client2PermRequested.length).toBeGreaterThan(0);

        // Both clients should have seen permission.completed events
        const client1PermCompleted = client1Events.filter(
            (e) => e.type === "permission.completed"
        );
        const client2PermCompleted = client2Events.filter(
            (e) => e.type === "permission.completed"
        );
        expect(client1PermCompleted.length).toBeGreaterThan(0);
        expect(client2PermCompleted.length).toBeGreaterThan(0);

        await session2.destroy();
    });
});

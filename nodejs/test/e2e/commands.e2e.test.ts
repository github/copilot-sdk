/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { afterAll, describe, expect, it } from "vitest";
import { CopilotClient, approveAll } from "../../src/index.js";
import type { SessionEvent } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

describe("Commands", async () => {
    // Use TCP mode so a second client can connect to the same CLI process
    const tcpConnectionToken = "commands-test-token";
    const ctx = await createSdkTestContext({
        useStdio: false,
        copilotClientOptions: { tcpConnectionToken },
    });
    const client1 = ctx.copilotClient;

    // Trigger connection so we can read the port
    const initSession = await client1.createSession({ onPermissionRequest: approveAll });
    await initSession.disconnect();

    const { actualPort } = client1 as unknown as { actualPort: number };
    const client2 = new CopilotClient({ cliUrl: `localhost:${actualPort}`, tcpConnectionToken });

    afterAll(async () => {
        await client2.stop();
    });

    it(
        "client receives commands.changed when another client joins with commands",
        { timeout: 20_000 },
        async () => {
            const session1 = await client1.createSession({
                onPermissionRequest: approveAll,
            });

            type CommandsChangedEvent = Extract<SessionEvent, { type: "commands.changed" }>;

            // Wait for the commands.changed event deterministically
            const commandsChangedPromise = new Promise<CommandsChangedEvent>((resolve) => {
                session1.on((event) => {
                    if (event.type === "commands.changed") resolve(event);
                });
            });

            // Client2 joins with commands
            const session2 = await client2.resumeSession(session1.sessionId, {
                onPermissionRequest: approveAll,
                commands: [
                    { name: "deploy", description: "Deploy the app", handler: async () => {} },
                ],
                disableResume: true,
            });

            // Rely on default vitest timeout
            const commandsChanged = await commandsChangedPromise;
            expect(commandsChanged.data.commands).toEqual(
                expect.arrayContaining([
                    expect.objectContaining({ name: "deploy", description: "Deploy the app" }),
                ])
            );

            await session2.disconnect();
        }
    );

    it("session with commands creates successfully", async () => {
        const session = await client1.createSession({
            onPermissionRequest: approveAll,
            commands: [
                { name: "deploy", description: "Deploy the app", handler: async () => {} },
                { name: "rollback", handler: async () => {} },
            ],
        });

        expect(session).toBeDefined();
        expect(session.sessionId).toMatch(/^[a-f0-9-]+$/);

        await session.disconnect();
    });

    it("session with commands resumes successfully", async () => {
        const session1 = await client1.createSession({ onPermissionRequest: approveAll });
        const sessionId = session1.sessionId;

        const session2 = await client1.resumeSession(sessionId, {
            onPermissionRequest: approveAll,
            commands: [{ name: "deploy", description: "Deploy", handler: async () => {} }],
        });

        expect(session2).toBeDefined();
        expect(session2.sessionId).toBe(sessionId);

        await session2.disconnect();
    });

    it("session with no commands creates successfully", async () => {
        const session = await client1.createSession({
            onPermissionRequest: approveAll,
        });

        expect(session).toBeDefined();
        expect(session.sessionId).toMatch(/^[a-f0-9-]+$/);

        await session.disconnect();
    });
});

/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { afterAll, describe, expect, it } from "vitest";
import { CopilotClient, approveAll } from "../../src/index.js";
import type { SessionEvent } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

describe("UI Elicitation", async () => {
    const { copilotClient: client } = await createSdkTestContext();

    it("elicitation methods throw in headless mode", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
        });

        // The SDK spawns the CLI headless - no TUI means no elicitation support.
        expect(session.capabilities.ui?.elicitation).toBeFalsy();
        await expect(session.ui.confirm("test")).rejects.toThrow(/not supported/);
    });
});

describe("UI Elicitation Callback", async () => {
    const ctx = await createSdkTestContext();
    const client = ctx.copilotClient;

    it(
        "session created with onElicitationRequest reports elicitation capability",
        { timeout: 20_000 },
        async () => {
            const session = await client.createSession({
                onPermissionRequest: approveAll,
                onElicitationRequest: async () => ({ action: "accept", content: {} }),
            });

            expect(session.capabilities.ui?.elicitation).toBe(true);
        }
    );

    it(
        "session created without onElicitationRequest reports no elicitation capability",
        { timeout: 20_000 },
        async () => {
            const session = await client.createSession({
                onPermissionRequest: approveAll,
            });

            expect(session.capabilities.ui?.elicitation).toBe(false);
        }
    );
});

describe("UI Elicitation Multi-Client Capabilities", async () => {
    // Use TCP mode so a second client can connect to the same CLI process
    const ctx = await createSdkTestContext({ useStdio: false });
    const client1 = ctx.copilotClient;

    // Trigger connection so we can read the port
    const initSession = await client1.createSession({ onPermissionRequest: approveAll });
    await initSession.disconnect();

    const actualPort = (client1 as unknown as { actualPort: number }).actualPort;
    const client2 = new CopilotClient({ cliUrl: `localhost:${actualPort}` });

    afterAll(async () => {
        await client2.stop();
    });

    it(
        "capabilities.changed fires when second client joins with elicitation handler",
        { timeout: 20_000 },
        async () => {
            // Client1 creates session without elicitation
            const session1 = await client1.createSession({
                onPermissionRequest: approveAll,
            });
            expect(session1.capabilities.ui?.elicitation).toBe(false);

            // Listen for capabilities.changed event
            const capChangedPromise = new Promise<SessionEvent>((resolve) => {
                session1.on((event) => {
                    if ((event as { type: string }).type === "capabilities.changed") {
                        resolve(event);
                    }
                });
            });

            // Client2 joins WITH elicitation handler — triggers capabilities.changed
            const session2 = await client2.resumeSession(session1.sessionId, {
                onPermissionRequest: approveAll,
                onElicitationRequest: async () => ({ action: "accept", content: {} }),
                disableResume: true,
            });

            const capEvent = await capChangedPromise;
            const data = (capEvent as { data: { ui?: { elicitation?: boolean } } }).data;
            expect(data.ui?.elicitation).toBe(true);

            // Client1's capabilities should have been auto-updated
            expect(session1.capabilities.ui?.elicitation).toBe(true);

            await session2.disconnect();
        }
    );

    it(
        "capabilities.changed fires when elicitation provider disconnects",
        { timeout: 20_000 },
        async () => {
            // Client1 creates session without elicitation
            const session1 = await client1.createSession({
                onPermissionRequest: approveAll,
            });
            expect(session1.capabilities.ui?.elicitation).toBe(false);

            // Wait for elicitation to become available
            const capEnabledPromise = new Promise<void>((resolve) => {
                session1.on((event) => {
                    const data = event as {
                        type: string;
                        data: { ui?: { elicitation?: boolean } };
                    };
                    if (
                        data.type === "capabilities.changed" &&
                        data.data.ui?.elicitation === true
                    ) {
                        resolve();
                    }
                });
            });

            // Use a dedicated client so we can stop it without affecting shared client2
            const client3 = new CopilotClient({ cliUrl: `localhost:${actualPort}` });

            // Client3 joins WITH elicitation handler
            await client3.resumeSession(session1.sessionId, {
                onPermissionRequest: approveAll,
                onElicitationRequest: async () => ({ action: "accept", content: {} }),
                disableResume: true,
            });

            await capEnabledPromise;
            expect(session1.capabilities.ui?.elicitation).toBe(true);

            // Now listen for the capability being removed
            const capDisabledPromise = new Promise<void>((resolve) => {
                session1.on((event) => {
                    const data = event as {
                        type: string;
                        data: { ui?: { elicitation?: boolean } };
                    };
                    if (
                        data.type === "capabilities.changed" &&
                        data.data.ui?.elicitation === false
                    ) {
                        resolve();
                    }
                });
            });

            // Force-stop client3 — destroys the socket, triggering server-side cleanup
            await client3.forceStop();

            await capDisabledPromise;
            expect(session1.capabilities.ui?.elicitation).toBe(false);
        }
    );
});

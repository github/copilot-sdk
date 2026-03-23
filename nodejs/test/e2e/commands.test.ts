/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { afterAll, describe, expect, it } from "vitest";
import { CopilotClient, approveAll } from "../../src/index.js";
import type { SessionEvent } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

describe("Commands", async () => {
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

    it("client receives commands.changed when another client joins with commands", { timeout: 20_000 }, async () => {
        const session1 = await client1.createSession({
            onPermissionRequest: approveAll,
        });

        // Collect events after session creation
        const events: SessionEvent[] = [];
        session1.on((event) => events.push(event));

        // Client2 joins with commands
        const session2 = await client2.resumeSession(session1.sessionId, {
            onPermissionRequest: approveAll,
            commands: [
                { name: "deploy", description: "Deploy the app", handler: async () => {} },
            ],
            disableResume: true,
        });

        // Wait for events to propagate
        await new Promise((resolve) => setTimeout(resolve, 2000));

        const commandsChanged = events.filter((e) => e.type === "commands.changed");
        expect(commandsChanged).toHaveLength(1);
        expect(commandsChanged[0].data.commands).toEqual(
            expect.arrayContaining([
                expect.objectContaining({ name: "deploy", description: "Deploy the app" }),
            ]),
        );

        await session2.disconnect();
    });
});

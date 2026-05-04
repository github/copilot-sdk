/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { approveAll } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

describe("Client session management", async () => {
    const { copilotClient: client } = await createSdkTestContext();

    async function assertFailure(
        action: () => Promise<unknown>,
        expectedMessage: string
    ): Promise<void> {
        await expect(action()).rejects.toSatisfy((err: unknown) => {
            const text = err instanceof Error ? `${err.message}\n${err.stack ?? ""}` : String(err);
            expect(text.toLowerCase()).toContain(expectedMessage.toLowerCase());
            return true;
        });
    }

    it("should delete session by id", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        const sessionId = session.sessionId;

        await session.sendAndWait({ prompt: "Say OK." });
        await session.disconnect();
        await client.deleteSession(sessionId);

        const metadata = await client.getSessionMetadata(sessionId);
        expect(metadata).toBeFalsy();
    });

    it("should report error when deleting unknown session id", async () => {
        await client.start();
        const unknownSessionId = "00000000-0000-0000-0000-000000000000";

        await assertFailure(
            () => client.deleteSession(unknownSessionId),
            `Failed to delete session ${unknownSessionId}`
        );
    });

    it("should get null last session id before any sessions exist", async () => {
        await client.start();

        const result = await client.getLastSessionId();
        expect(result).toBeFalsy();
    });

    it("should track last session id after session created", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        await session.sendAndWait({ prompt: "Say OK." });
        const sessionId = session.sessionId;
        await session.disconnect();

        const lastId = await client.getLastSessionId();
        expect(lastId).toBe(sessionId);
    });

    it("should get null foreground session id in headless mode", async () => {
        await client.start();

        const sessionId = await client.getForegroundSessionId();
        expect(sessionId).toBeFalsy();
    });

    it("should report error when setting foreground session in headless mode", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });

        await assertFailure(
            () => client.setForegroundSessionId(session.sessionId),
            "Not running in TUI+server mode"
        );

        await session.disconnect();
    });
});

import { describe, expect, it, onTestFinished } from "vitest";
import { CopilotClient } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

function onTestFinishedForceStop(client: CopilotClient) {
    onTestFinished(async () => {
        try {
            await client.forceStop();
        } catch {
            // Ignore cleanup errors - process may already be stopped
        }
    });
}

describe("RPC", () => {
    it("should call rpc.ping with typed params and result", async () => {
        const client = new CopilotClient({ useStdio: true });
        onTestFinishedForceStop(client);

        await client.start();

        const result = await client.rpc.ping({ message: "typed rpc test" });
        expect(result.message).toBe("pong: typed rpc test");
        expect(typeof result.timestamp).toBe("number");

        await client.stop();
    });

    it("should call rpc.models.list with typed result", async () => {
        const client = new CopilotClient({ useStdio: true });
        onTestFinishedForceStop(client);

        await client.start();

        const authStatus = await client.getAuthStatus();
        if (!authStatus.isAuthenticated) {
            await client.stop();
            return;
        }

        const result = await client.rpc.models.list();
        expect(result.models).toBeDefined();
        expect(Array.isArray(result.models)).toBe(true);

        await client.stop();
    });

    // account.getQuota is defined in schema but not yet implemented in CLI
    it.skip("should call rpc.account.getQuota when authenticated", async () => {
        const client = new CopilotClient({ useStdio: true });
        onTestFinishedForceStop(client);

        await client.start();

        const authStatus = await client.getAuthStatus();
        if (!authStatus.isAuthenticated) {
            await client.stop();
            return;
        }

        const result = await client.rpc.account.getQuota();
        expect(result.quotaSnapshots).toBeDefined();
        expect(typeof result.quotaSnapshots).toBe("object");

        await client.stop();
    });
});

describe("Session RPC", async () => {
    const { copilotClient: client } = await createSdkTestContext();

    // session.model.getCurrent is defined in schema but not yet implemented in CLI
    it.skip("should call session.rpc.model.getCurrent", async () => {
        const session = await client.createSession({ model: "claude-sonnet-4.5" });

        const result = await session.rpc.model.getCurrent();
        expect(result.modelId).toBeDefined();
        expect(typeof result.modelId).toBe("string");
    });

    // session.model.switchTo is defined in schema but not yet implemented in CLI
    it.skip("should call session.rpc.model.switchTo", async () => {
        const session = await client.createSession({ model: "claude-sonnet-4.5" });

        // Get initial model
        const before = await session.rpc.model.getCurrent();
        expect(before.modelId).toBeDefined();

        // Switch to a different model
        const result = await session.rpc.model.switchTo({ modelId: "gpt-4.1" });
        expect(result.modelId).toBe("gpt-4.1");

        // Verify the switch persisted
        const after = await session.rpc.model.getCurrent();
        expect(after.modelId).toBe("gpt-4.1");
    });
});

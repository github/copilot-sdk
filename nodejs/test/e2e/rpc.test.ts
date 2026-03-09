import { describe, expect, it, onTestFinished } from "vitest";
import { CopilotClient, approveAll } from "../../src/index.js";
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
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            model: "claude-sonnet-4.5",
        });

        const result = await session.rpc.model.getCurrent();
        expect(result.modelId).toBeDefined();
        expect(typeof result.modelId).toBe("string");
    });

    // session.model.switchTo is defined in schema but not yet implemented in CLI
    it.skip("should call session.rpc.model.switchTo", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            model: "claude-sonnet-4.5",
        });

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

    it("should get and set session mode", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });

        // Get initial mode (default should be interactive)
        const initial = await session.rpc.mode.get();
        expect(initial.mode).toBe("interactive");

        // Switch to plan mode
        const planResult = await session.rpc.mode.set({ mode: "plan" });
        expect(planResult.mode).toBe("plan");

        // Verify mode persisted
        const afterPlan = await session.rpc.mode.get();
        expect(afterPlan.mode).toBe("plan");

        // Switch back to interactive
        const interactiveResult = await session.rpc.mode.set({ mode: "interactive" });
        expect(interactiveResult.mode).toBe("interactive");
    });

    it("should read, update, and delete plan", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });

        // Initially plan should not exist
        const initial = await session.rpc.plan.read();
        expect(initial.exists).toBe(false);
        expect(initial.content).toBeNull();

        // Create/update plan
        const planContent = "# Test Plan\n\n- Step 1\n- Step 2";
        await session.rpc.plan.update({ content: planContent });

        // Verify plan exists and has correct content
        const afterUpdate = await session.rpc.plan.read();
        expect(afterUpdate.exists).toBe(true);
        expect(afterUpdate.content).toBe(planContent);

        // Delete plan
        await session.rpc.plan.delete();

        // Verify plan is deleted
        const afterDelete = await session.rpc.plan.read();
        expect(afterDelete.exists).toBe(false);
        expect(afterDelete.content).toBeNull();
    });

    it("should create, list, and read workspace files", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });

        // Initially no files
        const initialFiles = await session.rpc.workspace.listFiles();
        expect(initialFiles.files).toEqual([]);

        // Create a file
        const fileContent = "Hello, workspace!";
        await session.rpc.workspace.createFile({ path: "test.txt", content: fileContent });

        // List files
        const afterCreate = await session.rpc.workspace.listFiles();
        expect(afterCreate.files).toContain("test.txt");

        // Read file
        const readResult = await session.rpc.workspace.readFile({ path: "test.txt" });
        expect(readResult.content).toBe(fileContent);

        // Create nested file
        await session.rpc.workspace.createFile({
            path: "subdir/nested.txt",
            content: "Nested content",
        });

        const afterNested = await session.rpc.workspace.listFiles();
        expect(afterNested.files).toContain("test.txt");
        expect(afterNested.files.some((f) => f.includes("nested.txt"))).toBe(true);
    });
});

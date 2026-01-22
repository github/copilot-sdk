import { ChildProcess } from "child_process";
import { describe, expect, it, onTestFinished } from "vitest";
import { CopilotClient } from "../../src/index.js";
import { CLI_PATH } from "./harness/sdkTestContext.js";

function onTestFinishedForceStop(client: CopilotClient) {
    onTestFinished(async () => {
        try {
            await client.forceStop();
        } catch {
            // Ignore cleanup errors - process may already be stopped
        }
    });
}

describe("Client", () => {
    it("should start and connect to server using stdio", async () => {
        const client = new CopilotClient({ cliPath: CLI_PATH, useStdio: true });
        onTestFinishedForceStop(client);

        await client.start();
        expect(client.getState()).toBe("connected");

        const pong = await client.ping("test message");
        expect(pong.message).toBe("pong: test message");
        expect(pong.timestamp).toBeGreaterThanOrEqual(0);

        expect(await client.stop()).toHaveLength(0); // No errors on stop
        expect(client.getState()).toBe("disconnected");
    });

    it("should start and connect to server using tcp", async () => {
        const client = new CopilotClient({ cliPath: CLI_PATH, useStdio: false });
        onTestFinishedForceStop(client);

        await client.start();
        expect(client.getState()).toBe("connected");

        const pong = await client.ping("test message");
        expect(pong.message).toBe("pong: test message");
        expect(pong.timestamp).toBeGreaterThanOrEqual(0);

        expect(await client.stop()).toHaveLength(0); // No errors on stop
        expect(client.getState()).toBe("disconnected");
    });

    it.skipIf(process.platform === "darwin")("should return errors on failed cleanup", async () => {
        // Use TCP mode to avoid stdin stream destruction issues
        // Without this, on macOS there are intermittent test failures
        // saying "Cannot call write after a stream was destroyed"
        // because the JSON-RPC logic is still trying to write to stdin after
        // the process has exited.
        const client = new CopilotClient({ cliPath: CLI_PATH, useStdio: false });

        await client.createSession();

        // Kill the server process to force cleanup to fail
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const cliProcess = (client as any).cliProcess as ChildProcess;
        expect(cliProcess).toBeDefined();
        cliProcess.kill("SIGKILL");
        await new Promise((resolve) => setTimeout(resolve, 100));

        const errors = await client.stop();
        expect(errors.length).toBeGreaterThan(0);
        expect(errors[0].message).toContain("Failed to destroy session");
    });

    it("should forceStop without cleanup", async () => {
        const client = new CopilotClient({ cliPath: CLI_PATH });
        onTestFinishedForceStop(client);

        await client.createSession();
        await client.forceStop();
        expect(client.getState()).toBe("disconnected");
    });

    it("should get status with version and protocol info", async () => {
        const client = new CopilotClient({ cliPath: CLI_PATH, useStdio: true });
        onTestFinishedForceStop(client);

        await client.start();

        const status = await client.getStatus();
        expect(status.version).toBeDefined();
        expect(typeof status.version).toBe("string");
        expect(status.protocolVersion).toBeDefined();
        expect(typeof status.protocolVersion).toBe("number");
        expect(status.protocolVersion).toBeGreaterThanOrEqual(1);

        await client.stop();
    });

    it("should get auth status", async () => {
        const client = new CopilotClient({ cliPath: CLI_PATH, useStdio: true });
        onTestFinishedForceStop(client);

        await client.start();

        const authStatus = await client.getAuthStatus();
        expect(typeof authStatus.isAuthenticated).toBe("boolean");
        if (authStatus.isAuthenticated) {
            expect(authStatus.authType).toBeDefined();
            expect(authStatus.statusMessage).toBeDefined();
        }

        await client.stop();
    });

    it("should list models when authenticated", async () => {
        const client = new CopilotClient({ cliPath: CLI_PATH, useStdio: true });
        onTestFinishedForceStop(client);

        await client.start();

        const authStatus = await client.getAuthStatus();
        if (!authStatus.isAuthenticated) {
            // Skip if not authenticated - models.list requires auth
            await client.stop();
            return;
        }

        const models = await client.listModels();
        expect(Array.isArray(models)).toBe(true);
        if (models.length > 0) {
            const model = models[0];
            expect(model.id).toBeDefined();
            expect(model.name).toBeDefined();
            expect(model.capabilities).toBeDefined();
            expect(model.capabilities.supports).toBeDefined();
            expect(model.capabilities.limits).toBeDefined();
        }

        await client.stop();
    });
});

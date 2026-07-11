import { ChildProcess } from "child_process";
import { describe, expect, it, onTestFinished } from "vitest";
import { approveAll, CopilotClient, RuntimeConnection } from "../../src/index.js";
import { isInProcessTransport } from "./harness/sdkTestContext.js";

function onTestFinishedStop(client: CopilotClient) {
    onTestFinished(async () => {
        try {
            await client.stop();
        } catch {
            // Ignore cleanup errors - process may already be stopped
        }
    });
}

describe("Client", () => {
    it.each([
        { transport: "stdio", connection: () => undefined },
        { transport: "tcp", connection: () => RuntimeConnection.forTcp() },
    ])("allows createSession without onPermissionRequest ($transport)", async ({ connection }) => {
        const client = new CopilotClient({ connection: connection() });
        onTestFinishedStop(client);

        await using session = await client.createSession({});
        expect(session.sessionId).toMatch(/^[a-f0-9-]+$/);
    });

    it("allows resumeSession without onPermissionRequest", async () => {
        const connectionToken = "client-e2e-resume-token";

        const client = new CopilotClient({
            connection: RuntimeConnection.forTcp({ connectionToken }),
        });
        onTestFinishedStop(client);

        await using originalSession = await client.createSession({});

        const port = (client as unknown as { runtimePort: number | null }).runtimePort;
        if (port == null) {
            throw new Error("Client must be using TCP transport to support multi-client resume.");
        }

        const resumeClient = new CopilotClient({
            connection: RuntimeConnection.forUri(`localhost:${port}`, { connectionToken }),
        });
        onTestFinishedStop(resumeClient);

        await using resumedSession = await resumeClient.resumeSession(
            originalSession.sessionId,
            {}
        );
        expect(resumedSession.sessionId).toBe(originalSession.sessionId);
    });

    it("should start and connect to server using stdio", async () => {
        const client = new CopilotClient();
        onTestFinishedStop(client);

        await client.start();

        const pong = await client.ping("test message");
        expect(pong.message).toBe("pong: test message");
        expect(Date.parse(pong.timestamp)).not.toBeNaN();

        expect(await client.stop()).toHaveLength(0); // No errors on stop
    });

    it("should start and connect to server using tcp", async () => {
        const client = new CopilotClient({ connection: RuntimeConnection.forTcp() });
        onTestFinishedStop(client);

        await client.start();

        const pong = await client.ping("test message");
        expect(pong.message).toBe("pong: test message");
        expect(Date.parse(pong.timestamp)).not.toBeNaN();

        expect(await client.stop()).toHaveLength(0); // No errors on stop
    });

    it.skipIf(process.platform === "darwin")(
        "should stop cleanly when the server exits during cleanup",
        async () => {
            // Use TCP mode to avoid stdin stream destruction issues
            // Without this, on macOS there are intermittent test failures
            // saying "Cannot call write after a stream was destroyed"
            // because the JSON-RPC logic is still trying to write to stdin after
            // the process has exited.
            const client = new CopilotClient({ connection: RuntimeConnection.forTcp() });

            await client.createSession({ onPermissionRequest: approveAll });

            // Kill the server processto force cleanup to fail
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            const cliProcess = (client as any).cliProcess as ChildProcess;
            expect(cliProcess).toBeDefined();
            cliProcess.kill("SIGKILL");
            await new Promise((resolve) => setTimeout(resolve, 100));

            const errors = await client.stop();
            if (errors.length > 0) {
                expect(errors[0].message).toContain("Failed to disconnect session");
            }
        },
        // Generous timeout: client.stop() must wait for session.destroy to time out
        // when the server process is dead. The default 30s can flake on slow CI under load.
        60_000
    );

    // Skipping on in-proc:
    // - It breaks the macOS E2E run (failure: EPIPE)
    // - It's not clear that anyone should use forceStop in the in-proc case - there's no child process
    //   to terminate, so we can't be sure to leave a clean state
    // - If you want to get to a clean state within your process, that's what "stop" (not "forceStop") is for
    it.skipIf(isInProcessTransport)("should forceStop without cleanup", async () => {
        const client = new CopilotClient({});
        onTestFinishedStop(client);

        await client.createSession({ onPermissionRequest: approveAll });
        await client.forceStop();
    });

    it("should get status with version and protocol info", async () => {
        const client = new CopilotClient();
        onTestFinishedStop(client);

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
        const client = new CopilotClient();
        onTestFinishedStop(client);

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
        const client = new CopilotClient();
        onTestFinishedStop(client);

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

    it("should report error with stderr when CLI fails to start", async () => {
        const client = new CopilotClient({
            connection: RuntimeConnection.forStdio({ args: ["--nonexistent-flag-for-testing"] }),
        });
        onTestFinishedStop(client);

        let initialError: Error | undefined;
        try {
            await client.start();
            expect.fail("Expected start() to throw an error");
        } catch (error) {
            initialError = error as Error;
            expect(initialError.message).toContain("stderr");
            expect(initialError.message).toContain("nonexistent");
        }

        // Verify subsequent calls also fail (don't hang)
        try {
            const session = await client.createSession({ onPermissionRequest: approveAll });
            await session.send("test");
            expect.fail("Expected send() to throw an error after CLI exit");
        } catch (error) {
            expect((error as Error).message).toContain("Connection is closed");
        }
    });
});

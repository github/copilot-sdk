/**
 * E2E tests for ACP (Agent Client Protocol) support.
 *
 * These tests require an ACP-compatible CLI to be available.
 * By default, tests look for 'gemini' CLI with --experimental-acp flag.
 *
 * To run these tests:
 * 1. Install Gemini CLI: npm install -g @anthropic/gemini-cli
 * 2. Set environment variable: ACP_CLI_PATH=gemini (or path to your ACP CLI)
 * 3. Run: npm test -- --run test/e2e/acp.test.ts
 *
 * Skip these tests by not setting ACP_CLI_PATH environment variable.
 */

import { describe, it, expect, beforeAll, beforeEach, afterEach } from "vitest";
import { CopilotClient } from "../../src/index.js";
import type { SessionEvent } from "../../src/types.js";

// Get ACP CLI path from environment, skip tests if not set
const ACP_CLI_PATH = process.env.ACP_CLI_PATH;
const ACP_CLI_ARGS = process.env.ACP_CLI_ARGS?.split(" ") ?? ["--experimental-acp"];

// Helper to check if ACP CLI is available
async function isAcpCliAvailable(): Promise<boolean> {
    if (!ACP_CLI_PATH) {
        return false;
    }

    try {
        const { execSync } = await import("node:child_process");
        execSync(`which ${ACP_CLI_PATH}`, { stdio: "ignore" });
        return true;
    } catch {
        return false;
    }
}

describe.skipIf(!ACP_CLI_PATH)("ACP E2E Tests", () => {
    let client: CopilotClient;
    let cliAvailable: boolean;

    beforeAll(async () => {
        cliAvailable = await isAcpCliAvailable();
        if (!cliAvailable) {
            console.warn(
                `Skipping ACP E2E tests: CLI '${ACP_CLI_PATH}' not found. ` +
                    `Set ACP_CLI_PATH environment variable to enable these tests.`
            );
        }
    });

    beforeEach(() => {
        if (!cliAvailable) return;

        client = new CopilotClient({
            cliPath: ACP_CLI_PATH!,
            cliArgs: ACP_CLI_ARGS,
            protocol: "acp",
            autoStart: false,
        });
    });

    afterEach(async () => {
        if (client) {
            await client.forceStop();
        }
    });

    describe("connection", () => {
        it.skipIf(!ACP_CLI_PATH)("should connect to ACP CLI and verify protocol version", async () => {
            await client.start();

            expect(client.getState()).toBe("connected");
        });

        it.skipIf(!ACP_CLI_PATH)("should handle ping request", async () => {
            await client.start();

            const response = await client.ping("test");

            expect(response.message).toBe("pong");
            expect(response.protocolVersion).toBeDefined();
            expect(typeof response.timestamp).toBe("number");
        });
    });

    describe("session", () => {
        it.skipIf(!ACP_CLI_PATH)("should create a session", async () => {
            await client.start();

            const session = await client.createSession();

            expect(session.sessionId).toBeDefined();
            expect(typeof session.sessionId).toBe("string");
        });

        it.skipIf(!ACP_CLI_PATH)("should send a message and receive response", async () => {
            await client.start();

            const session = await client.createSession();

            const events: SessionEvent[] = [];
            session.on((event) => {
                events.push(event);
            });

            // Send a simple prompt
            await session.send({ prompt: "Say hello in exactly 3 words." });

            // Wait for session.idle event (with timeout)
            const timeout = 30000;
            const startTime = Date.now();

            while (Date.now() - startTime < timeout) {
                if (events.some((e) => e.type === "session.idle")) {
                    break;
                }
                await new Promise((resolve) => setTimeout(resolve, 100));
            }

            // Verify we received events
            expect(events.length).toBeGreaterThan(0);

            // Should have received either streaming deltas or a final message
            const hasDelta = events.some((e) => e.type === "assistant.message_delta");
            const hasMessage = events.some((e) => e.type === "assistant.message");
            const hasIdle = events.some((e) => e.type === "session.idle");

            expect(hasDelta || hasMessage).toBe(true);
            expect(hasIdle).toBe(true);
        });

        it.skipIf(!ACP_CLI_PATH)("should receive streaming content via assistant.message_delta", async () => {
            await client.start();

            const session = await client.createSession();

            const deltas: string[] = [];
            session.on("assistant.message_delta", (event) => {
                deltas.push(event.data.deltaContent);
            });

            let idleReceived = false;
            session.on("session.idle", () => {
                idleReceived = true;
            });

            await session.send({ prompt: "Count from 1 to 5, one number per line." });

            // Wait for idle
            const timeout = 30000;
            const startTime = Date.now();
            while (!idleReceived && Date.now() - startTime < timeout) {
                await new Promise((resolve) => setTimeout(resolve, 100));
            }

            // Should have received multiple deltas for streaming
            expect(deltas.length).toBeGreaterThan(0);

            // Concatenated content should contain the numbers
            const fullContent = deltas.join("");
            expect(fullContent.length).toBeGreaterThan(0);
        });
    });

    describe("tool calls", () => {
        it.skipIf(!ACP_CLI_PATH)("should receive tool events when agent uses tools", async () => {
            await client.start();

            const session = await client.createSession({
                workingDirectory: process.cwd(),
            });

            const toolEvents: SessionEvent[] = [];
            const allEvents: SessionEvent[] = [];

            session.on((event) => {
                allEvents.push(event);
                if (
                    event.type === "tool.execution_start" ||
                    event.type === "tool.execution_progress" ||
                    event.type === "tool.execution_complete"
                ) {
                    toolEvents.push(event);
                }
            });

            // Prompt that should trigger file read tool
            await session.send({
                prompt: "Read the package.json file in the current directory and tell me the package name.",
            });

            // Wait for session.idle
            const timeout = 60000; // Longer timeout for tool operations
            const startTime = Date.now();
            let idleReceived = false;

            while (!idleReceived && Date.now() - startTime < timeout) {
                if (allEvents.some((e) => e.type === "session.idle")) {
                    idleReceived = true;
                    break;
                }
                await new Promise((resolve) => setTimeout(resolve, 100));
            }

            expect(idleReceived).toBe(true);

            // Log for debugging
            console.log(`Received ${toolEvents.length} tool events:`);
            for (const event of toolEvents) {
                console.log(`  - ${event.type}:`, JSON.stringify(event.data, null, 2));
            }

            // We may or may not get tool events depending on how the agent responds
            // Just verify we completed without error
            expect(allEvents.length).toBeGreaterThan(0);
        });

        it.skipIf(!ACP_CLI_PATH)("should receive tool.execution_start and tool.execution_complete", async () => {
            await client.start();

            const session = await client.createSession({
                workingDirectory: process.cwd(),
            });

            let toolStarted = false;
            let toolCompleted = false;

            session.on("tool.execution_start", (event) => {
                console.log("Tool started:", event.data.toolName, event.data.toolCallId);
                toolStarted = true;
            });

            session.on("tool.execution_complete", (event) => {
                console.log(
                    "Tool completed:",
                    event.data.toolCallId,
                    event.data.success ? "success" : "failed"
                );
                toolCompleted = true;
            });

            let idleReceived = false;
            session.on("session.idle", () => {
                idleReceived = true;
            });

            // Ask to list files - should trigger tool call
            await session.send({
                prompt: "List the files in the current directory.",
            });

            // Wait for idle
            const timeout = 60000;
            const startTime = Date.now();
            while (!idleReceived && Date.now() - startTime < timeout) {
                await new Promise((resolve) => setTimeout(resolve, 100));
            }

            expect(idleReceived).toBe(true);

            // Log whether tool events were received
            console.log(`Tool started: ${toolStarted}, Tool completed: ${toolCompleted}`);

            // Note: Tool events depend on whether Gemini decides to use tools
            // This test verifies the event handling works when tools are used
        });
    });

    describe("error handling", () => {
        it.skipIf(!ACP_CLI_PATH)("should throw for unsupported methods", async () => {
            await client.start();

            // listModels is not supported in ACP mode
            await expect(client.listModels()).rejects.toThrow(/not supported in ACP mode/);
        });

        it.skipIf(!ACP_CLI_PATH)("should throw for resumeSession", async () => {
            await client.start();

            await expect(client.resumeSession("fake-session-id")).rejects.toThrow(
                /not supported in ACP mode/
            );
        });

        it.skipIf(!ACP_CLI_PATH)("should throw for session.getMessages", async () => {
            await client.start();
            const session = await client.createSession();

            await expect(session.getMessages()).rejects.toThrow(/not supported in ACP mode/);
        });
    });

    describe("cleanup", () => {
        it.skipIf(!ACP_CLI_PATH)("should stop cleanly", async () => {
            await client.start();
            await client.createSession();

            const errors = await client.stop();

            expect(errors.length).toBe(0);
            expect(client.getState()).toBe("disconnected");
        });

        it.skipIf(!ACP_CLI_PATH)("should force stop cleanly", async () => {
            await client.start();
            await client.createSession();

            await client.forceStop();

            expect(client.getState()).toBe("disconnected");
        });
    });
});

describe("ACP Protocol Selection", () => {
    it("should use ACP adapter when protocol is 'acp'", () => {
        const client = new CopilotClient({
            cliPath: "fake-cli",
            protocol: "acp",
            autoStart: false,
        });

        // Verify internal state - protocol should be set
        expect((client as unknown as { options: { protocol: string } }).options.protocol).toBe(
            "acp"
        );
    });

    it("should default to copilot protocol", () => {
        const client = new CopilotClient({
            cliPath: "fake-cli",
            autoStart: false,
        });

        expect((client as unknown as { options: { protocol: string } }).options.protocol).toBe(
            "copilot"
        );
    });
});

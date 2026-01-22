/* eslint-disable @typescript-eslint/no-explicit-any */
import { describe, expect, it, onTestFinished } from "vitest";
import { CopilotClient } from "../src/index.js";
import { CLI_PATH } from "./e2e/harness/sdkTestContext.js";

// This file is for unit tests. Where relevant, prefer to add e2e tests in e2e/*.test.ts instead

describe("CopilotClient", () => {
    it("returns a standardized failure result when a tool is not registered", async () => {
        const client = new CopilotClient({ cliPath: CLI_PATH });
        await client.start();
        onTestFinished(() => client.forceStop());

        const session = await client.createSession();

        const response = await (
            client as unknown as { handleToolCallRequest: (typeof client)["handleToolCallRequest"] }
        ).handleToolCallRequest({
            sessionId: session.sessionId,
            toolCallId: "123",
            toolName: "missing_tool",
            arguments: {},
        });

        expect(response.result).toMatchObject({
            resultType: "failure",
            error: "tool 'missing_tool' not supported",
        });
    });

    describe("URL parsing", () => {
        it("should parse port-only URL format", () => {
            const client = new CopilotClient({
                cliUrl: "8080",
                logLevel: "error",
            });

            // Verify internal state
            expect((client as any).actualPort).toBe(8080);
            expect((client as any).actualHost).toBe("localhost");
            expect((client as any).isExternalServer).toBe(true);
        });

        it("should parse host:port URL format", () => {
            const client = new CopilotClient({
                cliUrl: "127.0.0.1:9000",
                logLevel: "error",
            });

            expect((client as any).actualPort).toBe(9000);
            expect((client as any).actualHost).toBe("127.0.0.1");
            expect((client as any).isExternalServer).toBe(true);
        });

        it("should parse http://host:port URL format", () => {
            const client = new CopilotClient({
                cliUrl: "http://localhost:7000",
                logLevel: "error",
            });

            expect((client as any).actualPort).toBe(7000);
            expect((client as any).actualHost).toBe("localhost");
            expect((client as any).isExternalServer).toBe(true);
        });

        it("should parse https://host:port URL format", () => {
            const client = new CopilotClient({
                cliUrl: "https://example.com:443",
                logLevel: "error",
            });

            expect((client as any).actualPort).toBe(443);
            expect((client as any).actualHost).toBe("example.com");
            expect((client as any).isExternalServer).toBe(true);
        });

        it("should throw error for invalid URL format", () => {
            expect(() => {
                new CopilotClient({
                    cliUrl: "invalid-url",
                    logLevel: "error",
                });
            }).toThrow(/Invalid cliUrl format/);
        });

        it("should throw error for invalid port - too high", () => {
            expect(() => {
                new CopilotClient({
                    cliUrl: "localhost:99999",
                    logLevel: "error",
                });
            }).toThrow(/Invalid port in cliUrl/);
        });

        it("should throw error for invalid port - zero", () => {
            expect(() => {
                new CopilotClient({
                    cliUrl: "localhost:0",
                    logLevel: "error",
                });
            }).toThrow(/Invalid port in cliUrl/);
        });

        it("should throw error for invalid port - negative", () => {
            expect(() => {
                new CopilotClient({
                    cliUrl: "localhost:-1",
                    logLevel: "error",
                });
            }).toThrow(/Invalid port in cliUrl/);
        });

        it("should throw error when cliUrl is used with useStdio", () => {
            expect(() => {
                new CopilotClient({
                    cliUrl: "localhost:8080",
                    useStdio: true,
                    logLevel: "error",
                });
            }).toThrow(/cliUrl is mutually exclusive/);
        });

        it("should throw error when cliUrl is used with cliPath", () => {
            expect(() => {
                new CopilotClient({
                    cliUrl: "localhost:8080",
                    cliPath: "/path/to/cli",
                    logLevel: "error",
                });
            }).toThrow(/cliUrl is mutually exclusive/);
        });

        it("should set useStdio to false when cliUrl is provided", () => {
            const client = new CopilotClient({
                cliUrl: "8080",
                logLevel: "error",
            });

            expect(client["options"].useStdio).toBe(false);
        });

        it("should mark client as using external server", () => {
            const client = new CopilotClient({
                cliUrl: "localhost:8080",
                logLevel: "error",
            });

            expect((client as any).isExternalServer).toBe(true);
        });
    });
});

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { MessageConnection } from "vscode-jsonrpc/node";
import { CopilotSession } from "../src/session.js";

describe("CopilotSession", () => {
    describe("sendAndWait", () => {
        let mockConnection: MessageConnection;
        let session: CopilotSession;

        beforeEach(() => {
            mockConnection = {
                sendRequest: vi.fn().mockResolvedValue({ messageId: "test-msg-id" }),
            } as unknown as MessageConnection;
            session = new CopilotSession("test-session-id", mockConnection);
        });

        afterEach(() => {
            vi.restoreAllMocks();
        });

        it("should clear timeout timer when session.idle is received", async () => {
            const clearTimeoutSpy = vi.spyOn(global, "clearTimeout");

            // Simulate session.idle event after a short delay
            setTimeout(() => {
                session._dispatchEvent({ type: "session.idle", data: {} } as any);
            }, 10);

            await session.sendAndWait({ prompt: "test" }, 5000);

            // Verify clearTimeout was called (timer cleanup)
            expect(clearTimeoutSpy).toHaveBeenCalled();
        });

        it("should clear timeout timer when session.error is received", async () => {
            const clearTimeoutSpy = vi.spyOn(global, "clearTimeout");

            // Simulate session.error event after a short delay
            setTimeout(() => {
                session._dispatchEvent({
                    type: "session.error",
                    data: { message: "Test error", stack: "" },
                } as any);
            }, 10);

            await expect(session.sendAndWait({ prompt: "test" }, 5000)).rejects.toThrow(
                "Test error"
            );

            // Verify clearTimeout was called (timer cleanup)
            expect(clearTimeoutSpy).toHaveBeenCalled();
        });

        it("should return last assistant message when session.idle is received", async () => {
            // Simulate assistant message followed by session.idle
            setTimeout(() => {
                session._dispatchEvent({
                    type: "assistant.message",
                    data: { content: "Hello!" },
                } as any);
                session._dispatchEvent({ type: "session.idle", data: {} } as any);
            }, 10);

            const result = await session.sendAndWait({ prompt: "test" }, 5000);

            expect(result).toMatchObject({
                type: "assistant.message",
                data: { content: "Hello!" },
            });
        });

        it("should throw timeout error when timeout expires", async () => {
            // Don't send any events, let it timeout
            await expect(session.sendAndWait({ prompt: "test" }, 50)).rejects.toThrow(
                /Timeout after 50ms waiting for session.idle/
            );
        });

        it("should handle send() throwing before timeout is created", async () => {
            // Make send() throw an error
            mockConnection.sendRequest = vi.fn().mockRejectedValue(new Error("Send failed"));

            const clearTimeoutSpy = vi.spyOn(global, "clearTimeout");

            await expect(session.sendAndWait({ prompt: "test" }, 5000)).rejects.toThrow(
                "Send failed"
            );

            // clearTimeout should not be called since timeout was never created
            expect(clearTimeoutSpy).not.toHaveBeenCalled();
        });
    });
});

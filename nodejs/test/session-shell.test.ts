import { describe, it, expect } from "vitest";
import { CopilotSession } from "../src/session.js";
import type { ShellOutputNotification, ShellExitNotification } from "../src/types.js";

// Create a minimal mock session for testing
function createMockSession(): CopilotSession {
    const mockConnection = {
        sendRequest: async () => ({}),
    } as any;
    return new CopilotSession("test-session", mockConnection);
}

describe("CopilotSession shell notifications", () => {
    describe("onShellOutput", () => {
        it("should register and dispatch shell output notifications", () => {
            const session = createMockSession();
            const received: ShellOutputNotification[] = [];

            session.onShellOutput((notification) => {
                received.push(notification);
            });

            const notification: ShellOutputNotification = {
                processId: "proc-1",
                stream: "stdout",
                data: "hello world\n",
            };

            session._dispatchShellOutput(notification);

            expect(received).toHaveLength(1);
            expect(received[0]).toEqual(notification);
        });

        it("should support multiple handlers", () => {
            const session = createMockSession();
            const received1: ShellOutputNotification[] = [];
            const received2: ShellOutputNotification[] = [];

            session.onShellOutput((n) => received1.push(n));
            session.onShellOutput((n) => received2.push(n));

            const notification: ShellOutputNotification = {
                processId: "proc-1",
                stream: "stderr",
                data: "error output",
            };

            session._dispatchShellOutput(notification);

            expect(received1).toHaveLength(1);
            expect(received2).toHaveLength(1);
        });

        it("should unsubscribe when the returned function is called", () => {
            const session = createMockSession();
            const received: ShellOutputNotification[] = [];

            const unsubscribe = session.onShellOutput((n) => received.push(n));

            session._dispatchShellOutput({
                processId: "proc-1",
                stream: "stdout",
                data: "first",
            });

            unsubscribe();

            session._dispatchShellOutput({
                processId: "proc-1",
                stream: "stdout",
                data: "second",
            });

            expect(received).toHaveLength(1);
            expect(received[0].data).toBe("first");
        });

        it("should not crash when a handler throws", () => {
            const session = createMockSession();
            const received: ShellOutputNotification[] = [];

            session.onShellOutput(() => {
                throw new Error("handler error");
            });
            session.onShellOutput((n) => received.push(n));

            session._dispatchShellOutput({
                processId: "proc-1",
                stream: "stdout",
                data: "test",
            });

            expect(received).toHaveLength(1);
        });
    });

    describe("onShellExit", () => {
        it("should register and dispatch shell exit notifications", () => {
            const session = createMockSession();
            const received: ShellExitNotification[] = [];

            session.onShellExit((notification) => {
                received.push(notification);
            });

            const notification: ShellExitNotification = {
                processId: "proc-1",
                exitCode: 0,
            };

            session._dispatchShellExit(notification);

            expect(received).toHaveLength(1);
            expect(received[0]).toEqual(notification);
        });

        it("should unsubscribe when the returned function is called", () => {
            const session = createMockSession();
            const received: ShellExitNotification[] = [];

            const unsubscribe = session.onShellExit((n) => received.push(n));

            session._dispatchShellExit({ processId: "proc-1", exitCode: 0 });
            unsubscribe();
            session._dispatchShellExit({ processId: "proc-2", exitCode: 1 });

            expect(received).toHaveLength(1);
        });
    });

    describe("shell process tracking", () => {
        it("should track and untrack process IDs via callbacks", () => {
            const session = createMockSession();
            const registered = new Map<string, any>();

            session._setShellProcessCallbacks(
                (processId, s) => registered.set(processId, s),
                (processId) => registered.delete(processId)
            );

            session._trackShellProcess("proc-1");
            expect(registered.has("proc-1")).toBe(true);

            session._untrackShellProcess("proc-1");
            expect(registered.has("proc-1")).toBe(false);
        });
    });
});

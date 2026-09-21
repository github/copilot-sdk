import { describe, expect, it, vi } from "vitest";
import { CopilotHost, type CopilotHostExit } from "../src/host.js";

const info = {
    hostId: "host-1",
    url: "ws://127.0.0.1:12345",
    token: "test-only-token",
    pid: 1234,
};

function deferred<T>() {
    let resolve!: (value: T) => void;
    const promise = new Promise<T>((complete) => {
        resolve = complete;
    });
    return { promise, resolve };
}

describe("CopilotHost", () => {
    it("exposes connection information without starting a process", () => {
        const closed = deferred<CopilotHostExit>();
        const dispose = vi.fn();
        const host = new CopilotHost(info, closed.promise, dispose);

        expect(host.hostId).toBe(info.hostId);
        expect(host.url).toBe(info.url);
        expect(host.token).toBe(info.token);
        expect(host.pid).toBe(info.pid);
        expect(host.closed).toBe(closed.promise);
        expect(dispose).not.toHaveBeenCalled();
    });

    it("shares concurrent disposal and supports async disposal", async () => {
        const closed = deferred<CopilotHostExit>();
        const stopped = deferred<void>();
        const dispose = vi.fn(() => stopped.promise);
        const host = new CopilotHost(info, closed.promise, dispose);

        const first = host.dispose();
        expect(host.dispose()).toBe(first);
        stopped.resolve();
        await first;
        await host[Symbol.asyncDispose]();
        expect(dispose).toHaveBeenCalledOnce();
    });

    it("propagates disposal failures and permits retry", async () => {
        const closed = deferred<CopilotHostExit>();
        const dispose = vi
            .fn<() => Promise<void>>()
            .mockRejectedValueOnce(new Error("connection write failed"))
            .mockResolvedValueOnce(undefined);
        const host = new CopilotHost(info, closed.promise, dispose);

        await expect(host.dispose()).rejects.toThrow("connection write failed");
        await host.dispose();
        expect(dispose).toHaveBeenCalledTimes(2);
    });

    it("reports an unexpected child exit without an unhandled rejection", async () => {
        const closed = deferred<CopilotHostExit>();
        const dispose = vi.fn();
        const host = new CopilotHost(info, closed.promise, dispose);
        const exit: CopilotHostExit = {
            hostId: info.hostId,
            reason: "exited",
            error: "copilotd-lite exited unexpectedly",
        };

        closed.resolve(exit);
        await expect(host.closed).resolves.toEqual(exit);
        await host.dispose();
        expect(dispose).not.toHaveBeenCalled();
    });
});

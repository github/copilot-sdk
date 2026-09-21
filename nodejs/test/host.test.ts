import { describe, expect, expectTypeOf, it, vi } from "vitest";
import { AhpHost } from "../src/index.js";
import type {
    HostExitedNotification,
    HostStartRequest,
    HostStartResult,
} from "../src/generated/rpc.js";

const info = {
    hostId: "host-1",
    url: "ws://127.0.0.1:12345",
    token: "test-only-token",
    pid: 1234,
};

describe("AhpHost", () => {
    it("keeps generated listener options optional but nonnullable", () => {
        expectTypeOf<HostStartRequest>().toEqualTypeOf<{
            hostId: string;
            hostname?: string;
            port?: number;
            token?: string;
            requireConnectionToken?: boolean;
        }>();
        expectTypeOf<HostStartResult["token"]>().toEqualTypeOf<string | undefined>();
        expectTypeOf<AhpHost["token"]>().toEqualTypeOf<string | undefined>();
        expectTypeOf<HostExitedNotification["exitCode"]>().toEqualTypeOf<
            number | null | undefined
        >();
        expectTypeOf<HostExitedNotification["error"]>().toEqualTypeOf<string | null | undefined>();
    });

    it("exposes connection information without starting a process or exposing closed", () => {
        const dispose = vi.fn();
        const host = new AhpHost(info, dispose);

        expect(host.hostId).toBe(info.hostId);
        expect(host.url).toBe(info.url);
        expect(host.token).toBe(info.token);
        expect(host.pid).toBe(info.pid);
        expect(host).not.toHaveProperty("closed");
        expect(dispose).not.toHaveBeenCalled();
    });

    it("supports a listener without a connection token", () => {
        const { token: _token, ...withoutToken } = info;
        const host = new AhpHost(withoutToken, vi.fn());
        expect(host.token).toBeUndefined();
    });

    it("forwards concurrent, repeated, and async disposal calls independently", async () => {
        const dispose = vi.fn(async () => {});
        const host = new AhpHost(info, dispose);

        await Promise.all([host.dispose(), host.dispose()]);
        await host.dispose();
        await host[Symbol.asyncDispose]();
        expect(dispose).toHaveBeenCalledTimes(4);
    });

    it("returns the disposal promise without handling or retrying failures", async () => {
        const failure = Promise.reject(new Error("connection write failed"));
        const dispose = vi.fn(() => failure);
        const host = new AhpHost(info, dispose);

        expect(host.dispose()).toBe(failure);
        await expect(failure).rejects.toThrow("connection write failed");
        expect(dispose).toHaveBeenCalledOnce();
    });
});

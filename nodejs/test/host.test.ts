import { describe, expect, expectTypeOf, it, vi } from "vitest";
import { AhpHost } from "../src/index.js";
import type {
    HostExitedNotification,
    HostStartRequest,
    HostStartResult,
    HostLocalServerOptions,
    HostGitHubEnvironmentOptions,
} from "../src/generated/rpc.js";

const info = {
    hostId: "host-1",
    url: "ws://127.0.0.1:12345",
    token: "test-only-token",
};

describe("AhpHost", () => {
    it("keeps generated listener options optional but nonnullable", () => {
        expectTypeOf<HostStartRequest>().toEqualTypeOf<{
            hostId: string;
            computeId?: string;
            localServer?: HostLocalServerOptions;
            githubEnvironment?: HostGitHubEnvironmentOptions;
            sessionFactory?: boolean;
            resumeFactory?: boolean;
        }>();
        expectTypeOf<HostStartResult["token"]>().toEqualTypeOf<string | undefined>();
        expectTypeOf<AhpHost["token"]>().toEqualTypeOf<string | undefined>();
        expectTypeOf<HostStartResult["pid"]>().toEqualTypeOf<number | undefined>();
        expectTypeOf<AhpHost["pid"]>().toEqualTypeOf<number | undefined>();
        expectTypeOf<AhpHost["url"]>().toEqualTypeOf<string | undefined>();
        expectTypeOf<AhpHost["environmentId"]>().toEqualTypeOf<string | undefined>();
        expectTypeOf<HostExitedNotification["exitCode"]>().toEqualTypeOf<
            number | null | undefined
        >();
        expectTypeOf<HostExitedNotification["error"]>().toEqualTypeOf<string | null | undefined>();
    });

    it("exposes connection information without starting a process or exposing closed", () => {
        const dispose = vi.fn();
        const host = new AhpHost(info, dispose, vi.fn(), vi.fn());

        expect(host.hostId).toBe(info.hostId);
        expect(host.url).toBe(info.url);
        expect(host.token).toBe(info.token);
        expect(host.pid).toBeUndefined();
        expect(host).not.toHaveProperty("closed");
        expect(dispose).not.toHaveBeenCalled();
    });

    it("preserves an optional legacy separate host process ID", () => {
        const host = new AhpHost({ ...info, pid: 1234 }, vi.fn(), vi.fn(), vi.fn());
        expect(host.pid).toBe(1234);
    });

    it("supports GitHub-only hosts without a local URL, token, or process", () => {
        const host = new AhpHost(
            { hostId: "remote", environmentId: "environment-1" },
            vi.fn(),
            vi.fn(),
            vi.fn()
        );
        expect(host.environmentId).toBe("environment-1");
        expect(host.url).toBeUndefined();
        expect(host.token).toBeUndefined();
        expect(host.pid).toBeUndefined();
    });

    it("supports a listener without a connection token", () => {
        const { token: _token, ...withoutToken } = info;
        const host = new AhpHost(withoutToken, vi.fn(), vi.fn(), vi.fn());
        expect(host.token).toBeUndefined();
    });

    it("forwards concurrent, repeated, and async disposal calls independently", async () => {
        const dispose = vi.fn(async () => {});
        const host = new AhpHost(info, dispose, vi.fn(), vi.fn());

        await Promise.all([host.dispose(), host.dispose()]);
        await host.dispose();
        await host[Symbol.asyncDispose]();
        expect(dispose).toHaveBeenCalledTimes(4);
    });

    it("returns the disposal promise without handling or retrying failures", async () => {
        const failure = Promise.reject(new Error("connection write failed"));
        const dispose = vi.fn(() => failure);
        const host = new AhpHost(info, dispose, vi.fn(), vi.fn());

        expect(host.dispose()).toBe(failure);
        await expect(failure).rejects.toThrow("connection write failed");
        expect(dispose).toHaveBeenCalledOnce();
    });

    it("forwards existing session publication without creating or copying a session", async () => {
        const result = { sessionId: "resident", sessionUri: "copilot:/resident" };
        const publish = vi.fn(async () => result);
        const host = new AhpHost(info, vi.fn(), publish, vi.fn());
        await expect(host.publishSession("resident")).resolves.toBe(result);
        expect(publish).toHaveBeenCalledExactlyOnceWith("resident");
    });

    it("forwards host session listing to the owning RPC", async () => {
        const result = { sessions: [] };
        const listSessions = vi.fn(async () => result);
        const host = new AhpHost(info, vi.fn(), vi.fn(), listSessions);
        await expect(host.listSessions()).resolves.toBe(result);
        expect(listSessions).toHaveBeenCalledOnce();
    });
});

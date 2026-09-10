/* eslint-disable @typescript-eslint/no-explicit-any */
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const ffi = vi.hoisted(() => {
    let registeredCallback:
        ((userData: unknown, bytesPtr: unknown, bytesLen: number) => void) | undefined;
    const callbackToken = {};
    const hostStart = Object.assign(vi.fn(), {
        async: vi.fn(
            (
                _argv: Buffer,
                _argvLength: number,
                _env: Buffer | null,
                _envLength: number,
                callback: (error: Error | null, result: number) => void
            ) => callback(null, 11)
        ),
    });
    const hostShutdown = vi.fn(() => true);
    const connectionOpen = vi.fn(() => 21);
    const connectionWrite = vi.fn(() => true);
    const connectionClose = vi.fn<() => boolean>();
    const register = vi.fn(
        (callback: (userData: unknown, bytesPtr: unknown, bytesLen: number) => void) => {
            registeredCallback = callback;
            return callbackToken;
        }
    );
    const unregister = vi.fn();

    return {
        callbackToken,
        connectionClose,
        connectionOpen,
        connectionWrite,
        getRegisteredCallback: () => registeredCallback,
        hostShutdown,
        hostStart,
        register,
        unregister,
    };
});

vi.mock("node:fs", () => ({
    existsSync: vi.fn(() => true),
}));

vi.mock("koffi", () => ({
    default: {
        array: vi.fn(() => ({})),
        decode: vi.fn(() => new Uint8Array([1])),
        load: vi.fn(() => ({
            func: vi.fn((name: string) => {
                if (name.endsWith("host_start")) return ffi.hostStart;
                if (name.endsWith("host_shutdown")) return ffi.hostShutdown;
                if (name.endsWith("connection_open")) return ffi.connectionOpen;
                if (name.endsWith("connection_write")) return ffi.connectionWrite;
                if (name.endsWith("connection_close")) return ffi.connectionClose;
                throw new Error(`Unexpected FFI symbol: ${name}`);
            }),
        })),
        pointer: vi.fn(() => ({})),
        proto: vi.fn(() => ({})),
        register: ffi.register,
        unregister: ffi.unregister,
    },
}));

import { FfiRuntimeHost } from "../src/ffiRuntimeHost.js";

describe("FfiRuntimeHost callback cleanup", () => {
    beforeEach(() => {
        vi.useFakeTimers();
        ffi.connectionClose.mockReset();
        ffi.connectionOpen.mockClear();
        ffi.hostShutdown.mockClear();
        ffi.hostStart.mockClear();
        ffi.hostStart.async.mockClear();
        ffi.register.mockClear();
        ffi.unregister.mockClear();
    });

    afterEach(() => {
        vi.clearAllTimers();
        vi.useRealTimers();
    });

    it("retains the callback and keepalive until a retry closes the connection", async () => {
        ffi.connectionClose.mockReturnValueOnce(false).mockReturnValueOnce(true);
        const host = FfiRuntimeHost.create("runtime.node", undefined, undefined, []);
        await host.start();

        host.dispose();

        expect(ffi.connectionClose).toHaveBeenCalledTimes(1);
        expect(ffi.unregister).not.toHaveBeenCalled();
        expect(ffi.hostShutdown).not.toHaveBeenCalled();
        expect((host as any).outboundCallback).toBe(ffi.callbackToken);
        expect((host as any).keepAliveTimer).toBeDefined();

        await vi.advanceTimersByTimeAsync(100);

        expect(ffi.connectionClose).toHaveBeenCalledTimes(2);
        expect(ffi.unregister).toHaveBeenCalledTimes(1);
        expect(ffi.hostShutdown).toHaveBeenCalledTimes(1);
        expect((host as any).outboundCallback).toBeUndefined();
        expect((host as any).keepAliveTimer).toBeUndefined();
        expect(vi.getTimerCount()).toBe(0);

        host.dispose();
        await vi.advanceTimersByTimeAsync(100);
        expect(ffi.connectionClose).toHaveBeenCalledTimes(2);
        expect(ffi.unregister).toHaveBeenCalledTimes(1);
        expect(ffi.hostShutdown).toHaveBeenCalledTimes(1);
    });

    it("defers reclamation when dispose is reentrant from the outbound callback", async () => {
        ffi.connectionClose.mockReturnValueOnce(false).mockReturnValueOnce(true);
        const host = FfiRuntimeHost.create("runtime.node", undefined, undefined, []);
        await host.start();
        vi.spyOn(host as any, "feedInbound").mockImplementation(() => host.dispose());

        ffi.getRegisteredCallback()?.(null, {}, 1);

        expect(ffi.unregister).not.toHaveBeenCalled();
        expect((host as any).outboundCallback).toBe(ffi.callbackToken);

        await vi.advanceTimersByTimeAsync(100);

        expect(ffi.unregister).toHaveBeenCalledTimes(1);
        expect(ffi.hostShutdown).toHaveBeenCalledTimes(1);
        expect(vi.getTimerCount()).toBe(0);
    });
});

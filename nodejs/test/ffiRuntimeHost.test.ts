/* eslint-disable @typescript-eslint/no-explicit-any */
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const ffi = vi.hoisted(() => {
    let registeredCallback:
        | ((userData: unknown, bytesPtr: unknown, bytesLen: number) => void)
        | undefined;
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
    const connectionClose = Object.assign(vi.fn<() => boolean>(), {
        async: vi.fn<
            (connectionId: number, callback: (error: Error | null, result: boolean) => void) => void
        >(),
    });
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
        ffi.connectionClose.async
            .mockReset()
            .mockImplementation((_id, callback) => callback(null, true));
        ffi.connectionOpen.mockClear();
        ffi.hostShutdown.mockClear();
        ffi.hostStart.mockClear();
        ffi.hostStart.async.mockClear();
        ffi.register.mockClear();
        ffi.unregister.mockClear();
    });

    afterEach(() => {
        expect(ffi.connectionClose).not.toHaveBeenCalled();
        vi.clearAllTimers();
        vi.useRealTimers();
    });

    it("finishes disposal after a false close while retaining resources for the detached retry", async () => {
        ffi.connectionClose.async.mockImplementationOnce((_id, callback) => callback(null, false));
        const host = FfiRuntimeHost.create("runtime.node", undefined, undefined, []);
        await host.start();

        await host.dispose();

        expect(ffi.connectionClose.async).toHaveBeenCalledTimes(1);
        expect(ffi.unregister).not.toHaveBeenCalled();
        expect(ffi.hostShutdown).not.toHaveBeenCalled();
        expect((host as any).outboundCallback).toBe(ffi.callbackToken);
        expect((host as any).keepAliveTimer).toBeDefined();

        await vi.advanceTimersByTimeAsync(100);

        expect(ffi.connectionClose.async).toHaveBeenCalledTimes(2);
        expect(ffi.unregister).toHaveBeenCalledTimes(1);
        expect(ffi.hostShutdown).toHaveBeenCalledTimes(1);
        expect((host as any).outboundCallback).toBeUndefined();
        expect((host as any).keepAliveTimer).toBeUndefined();
        expect(vi.getTimerCount()).toBe(0);

        await host.dispose();
        await vi.advanceTimersByTimeAsync(100);
        expect(ffi.connectionClose.async).toHaveBeenCalledTimes(2);
        expect(ffi.unregister).toHaveBeenCalledTimes(1);
        expect(ffi.hostShutdown).toHaveBeenCalledTimes(1);
    });

    it("waits for successful initial cleanup without overlapping close calls", async () => {
        let finishClose!: (error: Error | null, result: boolean) => void;
        ffi.connectionClose.async.mockImplementationOnce((_id, callback) => {
            finishClose = callback;
        });
        const host = FfiRuntimeHost.create("runtime.node", undefined, undefined, []);
        await host.start();

        let disposed = false;
        const cleanup = host.dispose().then(() => {
            disposed = true;
        });
        await host.dispose();
        await vi.advanceTimersByTimeAsync(1000);

        expect(disposed).toBe(false);
        expect(ffi.connectionClose.async).toHaveBeenCalledTimes(1);
        expect(ffi.unregister).not.toHaveBeenCalled();
        expect(ffi.hostShutdown).not.toHaveBeenCalled();
        expect((host as any).outboundCallback).toBe(ffi.callbackToken);
        expect((host as any).keepAliveTimer).toBeDefined();

        finishClose(null, true);
        await cleanup;

        expect(disposed).toBe(true);
        expect(ffi.unregister).toHaveBeenCalledTimes(1);
        expect(ffi.hostShutdown).toHaveBeenCalledTimes(1);
        expect(vi.getTimerCount()).toBe(0);
    });

    it("defers reclamation when dispose is called from the outbound callback", async () => {
        ffi.connectionClose.async.mockImplementationOnce((_id, callback) => {
            setImmediate(() => callback(null, true));
        });
        const host = FfiRuntimeHost.create("runtime.node", undefined, undefined, []);
        await host.start();
        host.receiveStream.once("data", () => {
            void host.dispose();
        });

        ffi.getRegisteredCallback()?.(null, {}, 1);

        expect(ffi.unregister).not.toHaveBeenCalled();
        expect((host as any).outboundCallback).toBe(ffi.callbackToken);

        await vi.advanceTimersByTimeAsync(100);

        expect(ffi.unregister).toHaveBeenCalledTimes(1);
        expect(ffi.hostShutdown).toHaveBeenCalledTimes(1);
        expect(vi.getTimerCount()).toBe(0);
    });

    it.each(["callback", "throw"])(
        "quarantines the callback on a close %s error",
        async (failure) => {
            const closeError = new Error("close failed");
            ffi.connectionClose.async.mockImplementationOnce((_id, callback) => {
                if (failure === "throw") {
                    throw closeError;
                }
                callback(closeError, false);
            });
            const error = vi.spyOn(console, "error").mockImplementation(() => {});
            const host = FfiRuntimeHost.create("runtime.node", undefined, undefined, []);
            await host.start();

            await host.dispose();
            await vi.advanceTimersByTimeAsync(500);

            expect(ffi.connectionClose.async).toHaveBeenCalledTimes(1);
            expect(ffi.unregister).not.toHaveBeenCalled();
            expect(ffi.hostShutdown).not.toHaveBeenCalled();
            expect((host as any).outboundCallback).toBe(ffi.callbackToken);
            expect((FfiRuntimeHost as any).quarantinedHosts.has(host)).toBe(true);
            expect(error).toHaveBeenCalledTimes(1);
            expect(vi.getTimerCount()).toBe(0);
            error.mockRestore();
        }
    );

    it("does not retry a terminal host shutdown failure", async () => {
        ffi.hostShutdown.mockReturnValueOnce(false);
        const error = vi.spyOn(console, "error").mockImplementation(() => {});
        const host = FfiRuntimeHost.create("runtime.node", undefined, undefined, []);
        await host.start();

        await host.dispose();
        await vi.advanceTimersByTimeAsync(500);

        expect(ffi.connectionClose.async).toHaveBeenCalledTimes(1);
        expect(ffi.unregister).toHaveBeenCalledTimes(1);
        expect(ffi.hostShutdown).toHaveBeenCalledTimes(1);
        expect(vi.getTimerCount()).toBe(0);
        error.mockRestore();
    });

    it("retains the callback token when Koffi unregistration fails", async () => {
        ffi.unregister.mockImplementationOnce(() => {
            throw new Error("unregister failed");
        });
        const error = vi.spyOn(console, "error").mockImplementation(() => {});
        const host = FfiRuntimeHost.create("runtime.node", undefined, undefined, []);
        await host.start();

        await host.dispose();
        await vi.advanceTimersByTimeAsync(500);

        expect(ffi.connectionClose.async).toHaveBeenCalledTimes(1);
        expect(ffi.unregister).toHaveBeenCalledTimes(1);
        expect(ffi.hostShutdown).toHaveBeenCalledTimes(1);
        expect((host as any).outboundCallback).toBe(ffi.callbackToken);
        expect((FfiRuntimeHost as any).quarantinedHosts.has(host)).toBe(true);
        expect((host as any).keepAliveTimer).toBeUndefined();
        expect(vi.getTimerCount()).toBe(0);

        await host.dispose();
        expect(ffi.unregister).toHaveBeenCalledTimes(1);
        error.mockRestore();
    });
});

/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

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
    const hostShutdown = Object.assign(
        vi.fn(() => true),
        {
            async: vi.fn<
                (serverId: number, callback: (error: Error | null, result: boolean) => void) => void
            >(),
        }
    );
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
        ffi.hostShutdown.async
            .mockReset()
            .mockImplementation((_id, callback) => callback(null, true));
        ffi.hostStart.mockClear();
        ffi.hostStart.async.mockClear();
        ffi.register.mockClear();
        ffi.unregister.mockClear();
    });

    afterEach(() => {
        expect(ffi.connectionClose).not.toHaveBeenCalled();
        expect(ffi.hostShutdown).not.toHaveBeenCalled();
        vi.clearAllTimers();
        vi.useRealTimers();
    });

    it("forwards the CLI login sync integration without a CLI entrypoint", async () => {
        const host = FfiRuntimeHost.create("runtime.node", undefined, undefined, [
            "--cli-login-sync-integration-id=github/github-app",
        ]);
        await host.start();

        const argv = JSON.parse(ffi.hostStart.async.mock.calls[0][0].toString()) as string[];
        expect(argv).toEqual(["--cli-login-sync-integration-id=github/github-app"]);

        await host.dispose();
    });

    it("finishes disposal after a false close while retaining resources for the detached retry", async () => {
        ffi.connectionClose.async.mockImplementationOnce((_id, callback) => callback(null, false));
        const host = FfiRuntimeHost.create("runtime.node", undefined, undefined, []);
        await host.start();

        await host.dispose();

        expect(ffi.connectionClose.async).toHaveBeenCalledTimes(1);
        expect(ffi.unregister).not.toHaveBeenCalled();
        expect(ffi.hostShutdown.async).not.toHaveBeenCalled();
        expect((host as any).outboundCallback).toBe(ffi.callbackToken);
        expect((host as any).keepAliveTimer).toBeDefined();

        await vi.advanceTimersByTimeAsync(100);

        expect(ffi.connectionClose.async).toHaveBeenCalledTimes(2);
        expect(ffi.unregister).toHaveBeenCalledTimes(1);
        expect(ffi.hostShutdown.async).toHaveBeenCalledTimes(1);
        expect((host as any).outboundCallback).toBeUndefined();
        expect((host as any).keepAliveTimer).toBeUndefined();
        expect(vi.getTimerCount()).toBe(0);

        await host.dispose();
        await vi.advanceTimersByTimeAsync(100);
        expect(ffi.connectionClose.async).toHaveBeenCalledTimes(2);
        expect(ffi.unregister).toHaveBeenCalledTimes(1);
        expect(ffi.hostShutdown.async).toHaveBeenCalledTimes(1);
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
        expect(ffi.hostShutdown.async).not.toHaveBeenCalled();
        expect((host as any).outboundCallback).toBe(ffi.callbackToken);
        expect((host as any).keepAliveTimer).toBeDefined();

        finishClose(null, true);
        await cleanup;

        expect(disposed).toBe(true);
        expect(ffi.unregister).toHaveBeenCalledTimes(1);
        expect(ffi.hostShutdown.async).toHaveBeenCalledTimes(1);
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
        expect(ffi.hostShutdown.async).toHaveBeenCalledTimes(1);
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
            expect(ffi.hostShutdown.async).not.toHaveBeenCalled();
            expect((host as any).outboundCallback).toBe(ffi.callbackToken);
            expect((FfiRuntimeHost as any).quarantinedHosts.has(host)).toBe(true);
            expect(error).toHaveBeenCalledTimes(1);
            expect(vi.getTimerCount()).toBe(0);
            error.mockRestore();
        }
    );

    it("does not retry a terminal host shutdown failure", async () => {
        ffi.hostShutdown.async.mockImplementationOnce((_id, callback) => callback(null, false));
        const error = vi.spyOn(console, "error").mockImplementation(() => {});
        const host = FfiRuntimeHost.create("runtime.node", undefined, undefined, []);
        await host.start();

        await host.dispose();
        await vi.advanceTimersByTimeAsync(500);

        expect(ffi.connectionClose.async).toHaveBeenCalledTimes(1);
        expect(ffi.unregister).toHaveBeenCalledTimes(1);
        expect(ffi.hostShutdown.async).toHaveBeenCalledTimes(1);
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
        expect(ffi.hostShutdown.async).toHaveBeenCalledTimes(1);
        expect((host as any).outboundCallback).toBe(ffi.callbackToken);
        expect((FfiRuntimeHost as any).quarantinedHosts.has(host)).toBe(true);
        expect((host as any).keepAliveTimer).toBeUndefined();
        expect(vi.getTimerCount()).toBe(0);

        await host.dispose();
        expect(ffi.unregister).toHaveBeenCalledTimes(1);
        error.mockRestore();
    });

    it("joins host shutdown before startup rejects after disposal during startup", async () => {
        let finishStart!: (error: Error | null, result: number) => void;
        let finishShutdown: ((error: Error | null, result: boolean) => void) | undefined;
        ffi.hostStart.async.mockImplementationOnce(
            (_argv, _argvLength, _env, _envLength, callback) => {
                finishStart = callback;
            }
        );
        ffi.hostShutdown.async.mockImplementationOnce((_id, callback) => {
            finishShutdown = callback;
        });
        const host = FfiRuntimeHost.create("runtime.node", undefined, undefined, []);
        const settled = vi.fn();
        const startup = host.start().catch((error: unknown) => {
            settled();
            return error;
        });
        await host.dispose();
        finishStart(null, 11);
        try {
            await vi.advanceTimersByTimeAsync(0);
            expect(ffi.hostShutdown.async).toHaveBeenCalledExactlyOnceWith(
                11,
                expect.any(Function)
            );
            expect(settled).not.toHaveBeenCalled();
        } finally {
            finishShutdown?.(null, true);
            await startup;
        }
        expect(await startup).toEqual(
            new Error("The in-process runtime host was disposed during startup.")
        );
        expect(host["serverId"]).toBe(0);
        expect(ffi.connectionOpen).not.toHaveBeenCalled();
    });

    it.each(["disposal", "connection-open failure"])(
        "awaits asynchronous host shutdown while JS progresses during %s",
        async (path) => {
            let finishShutdown: ((error: Error | null, result: boolean) => void) | undefined;
            ffi.hostShutdown.async.mockImplementationOnce((_id, callback) => {
                finishShutdown = callback;
            });
            const host = FfiRuntimeHost.create("runtime.node", undefined, undefined, []);
            if (path === "connection-open failure") {
                ffi.connectionOpen.mockReturnValueOnce(0);
            } else {
                await host.start();
            }

            const settled = vi.fn();
            const operation = (path === "disposal" ? host.dispose() : host.start()).then(
                () => {
                    settled();
                    return undefined;
                },
                (error: unknown) => {
                    settled();
                    return error;
                }
            );
            try {
                const progress = vi.fn();
                setImmediate(progress);
                await vi.advanceTimersByTimeAsync(0);

                expect(ffi.hostShutdown.async).toHaveBeenCalledExactlyOnceWith(
                    11,
                    expect.any(Function)
                );
                expect(progress).toHaveBeenCalledTimes(1);
                expect(settled).not.toHaveBeenCalled();
                expect(host["serverId"]).toBe(11);
                // Callback reclamation precedes host shutdown only once no live connection remains.
                expect(ffi.connectionClose.async).toHaveBeenCalledTimes(
                    path === "disposal" ? 1 : 0
                );
                expect(ffi.unregister).toHaveBeenCalledExactlyOnceWith(ffi.callbackToken);
                expect(host["outboundCallback"]).toBeUndefined();

                await host.dispose();
                await vi.advanceTimersByTimeAsync(500);

                expect(settled).not.toHaveBeenCalled();
                expect(host["serverId"]).toBe(11);
                expect(host["starting"]).toBe(path === "connection-open failure");
                expect(host["cleanupInProgress"]).toBe(path === "disposal");
                expect(ffi.hostShutdown.async).toHaveBeenCalledTimes(1);
                expect(ffi.unregister).toHaveBeenCalledTimes(1);
            } finally {
                finishShutdown?.(null, true);
                await operation;
                await host.dispose();
            }

            if (path === "connection-open failure") {
                expect(await operation).toEqual(
                    new Error("copilot_runtime_connection_open failed.")
                );
            } else {
                expect(await operation).toBeUndefined();
            }
            expect(host["serverId"]).toBe(0);
            expect(host["starting"]).toBe(false);
            expect(host["cleanupInProgress"]).toBe(false);
            expect(ffi.hostShutdown.async).toHaveBeenCalledTimes(1);
            expect(ffi.unregister).toHaveBeenCalledTimes(1);
            expect(vi.getTimerCount()).toBe(0);
        }
    );

    it.each(["callback", "throw"])(
        "preserves host shutdown %s errors during disposal",
        async (failure) => {
            const shutdownError = new Error("shutdown failed");
            ffi.hostShutdown.async.mockImplementationOnce((_id, callback) => {
                if (failure === "throw") {
                    throw shutdownError;
                }
                callback(shutdownError, false);
            });
            const error = vi.spyOn(console, "error").mockImplementation(() => {});
            const host = FfiRuntimeHost.create("runtime.node", undefined, undefined, []);
            try {
                await host.start();
                await host.dispose();
                await vi.advanceTimersByTimeAsync(500);

                expect(error).toHaveBeenCalledWith(
                    expect.stringContaining(
                        "Failed to shut down in-process FFI host: Error: shutdown failed"
                    )
                );
                expect(ffi.hostShutdown.async).toHaveBeenCalledTimes(1);
                expect(ffi.unregister).toHaveBeenCalledTimes(1);
                expect(host["serverId"]).toBe(0);
                expect(vi.getTimerCount()).toBe(0);
            } finally {
                await host.dispose();
                error.mockRestore();
            }
        }
    );
});

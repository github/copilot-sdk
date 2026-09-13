/* eslint-disable @typescript-eslint/no-explicit-any */
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const ffi = vi.hoisted(() => {
    let registeredCallback:
        | ((userData: bigint, bytesPtr: bigint, bytesLen: number) => number)
        | undefined;
    const callbackToken = { owner: true };
    const callbackPointer = { pointer: true };
    const callbackOwner = [callbackToken];
    const addressOwner = [{ address: true }];
    const addressSlot = Buffer.alloc(8);
    const bytesPointer = { bytes: true };
    const hostStart = vi.fn(async (..._args: unknown[]) => 11);
    const hostShutdown = vi.fn((..._args: unknown[]) => true);
    const connectionOpen = vi.fn((..._args: unknown[]) => 21);
    const connectionWrite = vi.fn((..._args: unknown[]) => true);
    const connectionClose = vi.fn(async (..._args: unknown[]) => true);
    const createPointer = vi.fn(
        ({ paramsValue: [value] }: { paramsType: unknown[]; paramsValue: [unknown] }) => {
            if (typeof value === "function") {
                registeredCallback = value as (
                    userData: bigint,
                    bytesPtr: bigint,
                    bytesLen: number
                ) => number;
                return callbackOwner;
            }
            return addressOwner;
        }
    );
    const freePointer = vi.fn();
    const createExternalBuffer = vi.fn(() => addressSlot);
    const restorePointer = vi.fn(() => [Buffer.from([1])]);

    return {
        addressOwner,
        addressSlot,
        bytesPointer,
        callbackOwner,
        callbackPointer,
        callbackToken,
        connectionClose,
        connectionOpen,
        createExternalBuffer,
        createPointer,
        freePointer,
        connectionWrite,
        getRegisteredCallback: () => registeredCallback,
        hostShutdown,
        hostStart,
        restorePointer,
    };
});

vi.mock("node:fs", () => ({
    existsSync: vi.fn(() => true),
}));

vi.mock("ffi-rs", () => ({
    DataType: {
        BigInt: 16,
        Boolean: 6,
        External: 11,
        U8Array: 10,
        U32: 20,
        U64: 12,
        Void: 7,
    },
    PointerType: { RsPointer: 0 },
    arrayConstructor: vi.fn((options) => options),
    createExternalBuffer: ffi.createExternalBuffer,
    createPointer: ffi.createPointer,
    freePointer: ffi.freePointer,
    funcConstructor: vi.fn((options) => options),
    load: vi.fn(({ funcName, paramsValue }: { funcName: string; paramsValue: unknown[] }) => {
        if (funcName.endsWith("host_start")) return ffi.hostStart(...paramsValue);
        if (funcName.endsWith("host_shutdown")) return ffi.hostShutdown(...paramsValue);
        if (funcName.endsWith("connection_open")) return ffi.connectionOpen(...paramsValue);
        if (funcName.endsWith("connection_write")) return ffi.connectionWrite(...paramsValue);
        if (funcName.endsWith("connection_close")) return ffi.connectionClose(...paramsValue);
        throw new Error(`Unexpected FFI symbol: ${funcName}`);
    }),
    open: vi.fn(),
    restorePointer: ffi.restorePointer,
    unwrapPointer: vi.fn((owner) =>
        owner === ffi.callbackOwner ? [ffi.callbackPointer] : [ffi.bytesPointer]
    ),
}));

import { FfiRuntimeHost } from "../src/ffiRuntimeHost.js";

describe("FfiRuntimeHost callback cleanup", () => {
    beforeEach(() => {
        vi.useFakeTimers();
        ffi.connectionClose.mockReset();
        ffi.connectionClose.mockImplementation(async () => true);
        ffi.connectionOpen.mockClear();
        ffi.createExternalBuffer.mockReset();
        ffi.createExternalBuffer.mockReturnValue(ffi.addressSlot);
        ffi.hostShutdown.mockClear();
        ffi.hostStart.mockClear();
        ffi.addressSlot.fill(0);
        ffi.createPointer.mockClear();
        ffi.freePointer.mockClear();
        ffi.restorePointer.mockClear();
    });

    afterEach(() => {
        vi.clearAllTimers();
        vi.useRealTimers();
    });

    it("finishes disposal after a false close while retaining resources for the detached retry", async () => {
        ffi.connectionClose.mockResolvedValueOnce(false);
        const host = FfiRuntimeHost.create("runtime.node", undefined, undefined, []);
        await host.start();

        await host.dispose();

        expect(ffi.connectionClose).toHaveBeenCalledTimes(1);
        expect(ffi.freePointer).not.toHaveBeenCalled();
        expect(ffi.hostShutdown).not.toHaveBeenCalled();
        expect((host as any).outboundCallback).toBe(ffi.callbackOwner);

        await vi.advanceTimersByTimeAsync(100);

        expect(ffi.connectionClose).toHaveBeenCalledTimes(2);
        expect(ffi.freePointer).toHaveBeenCalledTimes(2);
        expect(ffi.hostShutdown).toHaveBeenCalledTimes(1);
        expect((host as any).outboundCallback).toBeUndefined();
        expect(vi.getTimerCount()).toBe(0);

        await host.dispose();
        await vi.advanceTimersByTimeAsync(100);
        expect(ffi.connectionClose).toHaveBeenCalledTimes(2);
        expect(ffi.freePointer).toHaveBeenCalledTimes(2);
        expect(ffi.hostShutdown).toHaveBeenCalledTimes(1);
    });

    it("waits for successful initial cleanup without overlapping close calls", async () => {
        let finishClose!: (error: Error | null, result: boolean) => void;
        ffi.connectionClose.mockImplementationOnce(
            () =>
                new Promise<boolean>((resolve) => {
                    finishClose = (_error, result) => resolve(result);
                })
        );
        const host = FfiRuntimeHost.create("runtime.node", undefined, undefined, []);
        await host.start();

        let disposed = false;
        const cleanup = host.dispose().then(() => {
            disposed = true;
        });
        await host.dispose();
        await vi.advanceTimersByTimeAsync(1000);

        expect(disposed).toBe(false);
        expect(ffi.connectionClose).toHaveBeenCalledTimes(1);
        expect(ffi.freePointer).not.toHaveBeenCalled();
        expect(ffi.hostShutdown).not.toHaveBeenCalled();
        expect((host as any).outboundCallback).toBe(ffi.callbackOwner);

        finishClose(null, true);
        await cleanup;

        expect(disposed).toBe(true);
        expect(ffi.freePointer).toHaveBeenCalledTimes(2);
        expect(ffi.hostShutdown).toHaveBeenCalledTimes(1);
        expect(vi.getTimerCount()).toBe(0);
    });

    it("defers reclamation when dispose is called from the outbound callback", async () => {
        ffi.connectionClose.mockImplementationOnce(
            () => new Promise<boolean>((resolve) => setImmediate(() => resolve(true)))
        );
        const host = FfiRuntimeHost.create("runtime.node", undefined, undefined, []);
        await host.start();
        host.receiveStream.once("data", () => {
            void host.dispose();
        });

        const callbackResult = ffi.getRegisteredCallback()?.(0n, 1234n, 1);

        expect(callbackResult).toBe(0);
        expect(ffi.createPointer.mock.calls[0][0].paramsType[0]).toEqual({
            paramsType: [16, 16, 12],
            retType: 12,
        });
        expect(ffi.createPointer.mock.calls[1][0].paramsType).toEqual([16]);
        expect(ffi.freePointer).not.toHaveBeenCalled();
        expect((host as any).outboundCallback).toBe(ffi.callbackOwner);

        await vi.advanceTimersByTimeAsync(100);

        expect(ffi.freePointer).toHaveBeenCalledTimes(2);
        expect(ffi.freePointer.mock.calls[0][0].paramsValue).toBe(ffi.callbackOwner);
        expect(ffi.freePointer.mock.calls[1][0].paramsValue).toBe(ffi.addressOwner);
        expect(ffi.hostShutdown).toHaveBeenCalledTimes(1);
        expect(vi.getTimerCount()).toBe(0);
    });

    it("reconstructs callback pointers with the requested payload length", async () => {
        const host = FfiRuntimeHost.create("runtime.node", undefined, undefined, []);
        await host.start();

        const callbackResult = ffi.getRegisteredCallback()?.(0n, 0x1234_5678n, 17);

        expect(callbackResult).toBe(0);
        expect(ffi.addressSlot.readBigInt64LE()).toBe(0x1234_5678n);
        expect(ffi.restorePointer).toHaveBeenCalledWith({
            retType: [{ type: 10, length: 17 }],
            paramsValue: ffi.addressOwner,
        });

        await host.dispose();
    });

    it.each(["callback storage", "connection open"])(
        "rolls back native resources when %s setup fails",
        async (failure) => {
            const setupError = new Error("setup failed");
            if (failure === "callback storage") {
                ffi.createExternalBuffer.mockImplementationOnce(() => {
                    throw setupError;
                });
            } else {
                ffi.connectionOpen.mockImplementationOnce(() => {
                    throw setupError;
                });
            }
            const host = FfiRuntimeHost.create("runtime.node", undefined, undefined, []);

            await expect(host.start()).rejects.toBe(setupError);

            expect(ffi.freePointer).toHaveBeenCalledTimes(2);
            expect(ffi.hostShutdown).toHaveBeenCalledWith(11);
            expect((host as any).outboundCallback).toBeUndefined();
            expect((host as any).inboundAddressOwner).toBeUndefined();
            expect((host as any).serverId).toBe(0);
        }
    );

    it.each(["callback", "throw"])("retries cleanup after a close %s error", async (failure) => {
        const closeError = new Error("close failed");
        ffi.connectionClose.mockImplementationOnce(() => {
            if (failure === "throw") {
                throw closeError;
            }
            return Promise.reject(closeError);
        });
        const error = vi.spyOn(console, "error").mockImplementation(() => {});
        const host = FfiRuntimeHost.create("runtime.node", undefined, undefined, []);
        await host.start();

        await host.dispose();

        expect(ffi.connectionClose).toHaveBeenCalledTimes(1);
        expect(ffi.freePointer).not.toHaveBeenCalled();
        expect(ffi.hostShutdown).not.toHaveBeenCalled();
        expect((host as any).outboundCallback).toBe(ffi.callbackOwner);
        expect(error).toHaveBeenCalledTimes(1);

        await vi.advanceTimersByTimeAsync(100);

        expect(ffi.connectionClose).toHaveBeenCalledTimes(2);
        expect(ffi.freePointer).toHaveBeenCalledTimes(2);
        expect(ffi.hostShutdown).toHaveBeenCalledTimes(1);
        expect((host as any).outboundCallback).toBeUndefined();
        expect(vi.getTimerCount()).toBe(0);
        error.mockRestore();
    });

    it("does not retry a terminal host shutdown failure", async () => {
        ffi.hostShutdown.mockReturnValueOnce(false);
        const error = vi.spyOn(console, "error").mockImplementation(() => {});
        const host = FfiRuntimeHost.create("runtime.node", undefined, undefined, []);
        await host.start();

        await host.dispose();
        await vi.advanceTimersByTimeAsync(500);

        expect(ffi.connectionClose).toHaveBeenCalledTimes(1);
        expect(ffi.freePointer).toHaveBeenCalledTimes(2);
        expect(ffi.hostShutdown).toHaveBeenCalledTimes(1);
        expect(vi.getTimerCount()).toBe(0);
        error.mockRestore();
    });

    it("retains the callback token when ffi-rs callback cleanup fails", async () => {
        ffi.freePointer.mockImplementationOnce(() => {
            throw new Error("free failed");
        });
        const error = vi.spyOn(console, "error").mockImplementation(() => {});
        const host = FfiRuntimeHost.create("runtime.node", undefined, undefined, []);
        await host.start();

        await host.dispose();
        await vi.advanceTimersByTimeAsync(500);

        expect(ffi.connectionClose).toHaveBeenCalledTimes(1);
        expect(ffi.freePointer).toHaveBeenCalledTimes(2);
        expect(ffi.hostShutdown).toHaveBeenCalledTimes(1);
        expect((host as any).outboundCallback).toBe(ffi.callbackOwner);
        expect((FfiRuntimeHost as any).quarantinedHosts.has(host)).toBe(true);
        expect(vi.getTimerCount()).toBe(0);

        await host.dispose();
        expect(ffi.freePointer).toHaveBeenCalledTimes(2);
        error.mockRestore();
    });
});

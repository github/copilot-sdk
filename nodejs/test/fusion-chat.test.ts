/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { PassThrough } from "node:stream";
import { mkdtemp, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { onTestFinished, beforeEach, describe, expect, it, vi } from "vitest";
import {
    DEFAULT_TURN_TIMEOUT_MS,
    resolveTurnTimeoutMs,
    runFusionChat,
} from "../samples/fusion-chat.js";

const mocks = vi.hoisted(() => ({
    start: vi.fn<() => Promise<void>>(),
    createSession: vi.fn(),
    stop: vi.fn<() => Promise<Error[]>>(),
    sendAndWait: vi.fn(),
}));

vi.mock("../src/index.js", () => ({
    CopilotClient: class {
        start = mocks.start;
        createSession = mocks.createSession;
        stop = mocks.stop;
    },
    approveAll: vi.fn(),
}));

async function runWithTimeout(turnTimeoutMs?: number) {
    const directory = await mkdtemp(join(tmpdir(), "fusion-chat-timeout-"));
    onTestFinished(() => rm(directory, { recursive: true, force: true }));
    const input = new PassThrough();
    const output = new PassThrough();
    output.resume();
    const running = runFusionChat(
        input,
        output,
        join(directory, "events.jsonl"),
        false,
        turnTimeoutMs
    );
    input.end("hello\n/exit\n");
    await running;
}

beforeEach(() => {
    vi.resetAllMocks();
    mocks.start.mockResolvedValue();
    mocks.createSession.mockResolvedValue({ sendAndWait: mocks.sendAndWait });
    mocks.sendAndWait.mockResolvedValue(undefined);
    mocks.stop.mockResolvedValue([]);
});

describe("Fusion chat turn timeout", () => {
    it("allows five minutes by default for multi-step research turns", async () => {
        await runWithTimeout();
        expect(mocks.sendAndWait).toHaveBeenCalledExactlyOnceWith({ prompt: "hello" }, 5 * 60_000);
        expect(DEFAULT_TURN_TIMEOUT_MS).toBe(5 * 60_000);
    });

    it("passes a custom bounded timeout to the SDK helper", async () => {
        await runWithTimeout(12 * 60_000);
        expect(mocks.sendAndWait).toHaveBeenCalledExactlyOnceWith({ prompt: "hello" }, 12 * 60_000);
    });

    it.each([
        [undefined, 5 * 60_000],
        ["90", 90_000],
    ])("resolves timeout %s", (value, expected) => {
        expect(resolveTurnTimeoutMs(value)).toBe(expected);
    });

    it.each(["0", "-1", "1.5", "not-a-number"])("rejects invalid timeout %s", (value) => {
        expect(() => resolveTurnTimeoutMs(value)).toThrow(
            "--timeout-seconds must be a positive integer"
        );
    });
});

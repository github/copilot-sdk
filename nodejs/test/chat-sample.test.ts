/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { PassThrough } from "node:stream";
import { beforeEach, describe, expect, it, onTestFinished, vi } from "vitest";
import { WriteStream } from "node:fs";
import { mkdtemp, readFile, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import type {
    CopilotClientOptions,
    SessionConfig,
    SessionEvent,
    SessionLifecycleHandler,
} from "../src/index.js";
import { runChat } from "../samples/chat.js";

const mocks = vi.hoisted(() => ({
    construct: vi.fn<(options: CopilotClientOptions) => void>(),
    start: vi.fn<() => Promise<void>>(),
    listModels: vi.fn(),
    createSession: vi.fn(),
    onLifecycle: vi.fn<(handler: SessionLifecycleHandler) => () => void>(),
    stop: vi.fn<() => Promise<Error[]>>(),
    unsubscribe: vi.fn(),
    sendAndWait: vi.fn(),
    setModel: vi.fn(),
    approveAll: vi.fn(),
}));

vi.mock("../src/index.js", () => ({
    CopilotClient: class {
        constructor(options: CopilotClientOptions) {
            mocks.construct(options);
        }
        start = mocks.start;
        listModels = mocks.listModels;
        createSession = mocks.createSession;
        onLifecycle = mocks.onLifecycle;
        stop = mocks.stop;
    },
    approveAll: mocks.approveAll,
}));

const eventBase = {
    id: "event-1",
    parentId: null,
    timestamp: "2026-09-16T00:00:00.000Z",
};
const startEvent: SessionEvent = {
    ...eventBase,
    type: "session.start",
    data: {
        sessionId: "chat-session",
        producer: "test",
        copilotVersion: "local",
        startTime: eventBase.timestamp,
        version: 1,
    },
};
const reply: SessionEvent = {
    ...eventBase,
    type: "assistant.message",
    data: { messageId: "message-1", content: "Answer from the assistant" },
};

async function createLogPath() {
    const directory = await mkdtemp(join(tmpdir(), "sdk-chat-log-"));
    onTestFinished(() => rm(directory, { recursive: true, force: true }));
    return join(directory, "events.jsonl");
}

async function runWithInput(text: string, eventsFile?: string, enableHydraFusion = false) {
    const logPath = eventsFile ?? (await createLogPath());
    const input = new PassThrough();
    const output = new PassThrough();
    let transcript = "";
    output.setEncoding("utf8");
    output.on("data", (chunk: string) => {
        transcript += chunk;
    });
    const running = runChat(input, output, logPath, enableHydraFusion);
    input.end(text);
    try {
        await running;
        return transcript;
    } finally {
        input.destroy();
        output.destroy();
    }
}

beforeEach(() => {
    vi.resetAllMocks();
    mocks.start.mockResolvedValue();
    mocks.listModels.mockResolvedValue([
        { id: "model-a", name: "Model A" },
        { id: "model-b", name: "Model B" },
    ]);
    mocks.onLifecycle.mockReturnValue(mocks.unsubscribe);
    mocks.createSession.mockImplementation(async (config: SessionConfig) => {
        config.onEvent?.(startEvent);
        return { sendAndWait: mocks.sendAndWait, setModel: mocks.setModel };
    });
    mocks.sendAndWait.mockResolvedValue(reply);
    mocks.setModel.mockResolvedValue(undefined);
    mocks.stop.mockResolvedValue([]);
});

describe("chat sample", () => {
    it("selects a model by number, keeps pasted input, and switches without losing the session", async () => {
        const transcript = await runWithInput(
            "2\n first prompt \n/model model-a\nsecond prompt\n/exit\n"
        );

        expect(mocks.createSession).toHaveBeenCalledTimes(1);
        expect(mocks.createSession).toHaveBeenCalledWith(
            expect.objectContaining({
                model: "model-b",
                streaming: true,
                includeSubAgentStreamingEvents: true,
                onPermissionRequest: mocks.approveAll,
                onEvent: expect.any(Function),
            })
        );
        expect(mocks.setModel).toHaveBeenCalledExactlyOnceWith("model-a");
        expect(mocks.sendAndWait.mock.calls).toEqual([
            [{ prompt: " first prompt " }],
            [{ prompt: "second prompt" }],
        ]);
        expect(transcript).toContain("1. Model A (model-a)");
        expect(transcript).toContain("2. Model B (model-b)");
        expect(transcript).toContain("Model: model-a");
        expect(transcript).toContain("Assistant: Answer from the assistant");
        expect(mocks.stop).toHaveBeenCalledOnce();
        expect(mocks.unsubscribe).toHaveBeenCalledOnce();
    });

    it("rejects invalid numbers and accepts IDs and the interactive model command", async () => {
        const transcript = await runWithInput("99\nmodel-b\n/model\n0\n1\n/exit\n");
        expect(transcript).toContain("Unknown model: 99");
        expect(transcript).toContain("Unknown model: 0");
        expect(mocks.createSession).toHaveBeenCalledWith(
            expect.objectContaining({ model: "model-b" })
        );
        expect(mocks.setModel).toHaveBeenCalledExactlyOnceWith("model-a");
        expect(mocks.sendAndWait).not.toHaveBeenCalled();
    });

    it.each(["hydrafusion", "Custom-Model-ID"])(
        "accepts the unlisted model ID %s at startup without rewriting it",
        async (modelId) => {
            await runWithInput(`${modelId}\n/exit\n`);
            expect(mocks.createSession).toHaveBeenCalledExactlyOnceWith(
                expect.objectContaining({ model: modelId })
            );
        }
    );

    it("switches to an unlisted model ID without creating another session", async () => {
        await runWithInput("1\n/model hydrafusion\n/exit\n");
        expect(mocks.setModel).toHaveBeenCalledExactlyOnceWith("hydrafusion");
        expect(mocks.createSession).toHaveBeenCalledOnce();
        expect(mocks.sendAndWait).not.toHaveBeenCalled();
    });

    it("opts into Fusion in the child environment and session, without changing the host environment", async () => {
        const originalEnv = { ...process.env };
        await runWithInput("1\n/model hydrafusion\n/exit\n", undefined, true);

        const env = mocks.construct.mock.calls[0][0].env;
        expect(env?.HYDRAFUSION).toBe("true");
        expect(env?.HYDRAFUSION_ROLLOUT).toBe("true");
        expect(
            Object.entries(originalEnv)
                .filter(([key]) => key !== "HYDRAFUSION" && key !== "HYDRAFUSION_ROLLOUT")
                .every(([key, value]) => env?.[key] === value)
        ).toBe(true);
        expect(mocks.createSession).toHaveBeenCalledWith(
            expect.objectContaining({ enableExperimentalMode: true })
        );
        expect(mocks.setModel).toHaveBeenCalledExactlyOnceWith("hydrafusion");
        expect(JSON.stringify(process.env) === JSON.stringify(originalEnv)).toBe(true);
    });

    it("does not override runtime feature gates without the development opt-in", async () => {
        await runWithInput("1\n/exit\n");
        expect(mocks.construct.mock.calls[0][0].env).toBeUndefined();
        expect(mocks.createSession.mock.calls[0][0].enableExperimentalMode).toBeUndefined();
    });

    it("records a diagnostic when a requested Fusion turn only returns an ordinary reply", async () => {
        const logPath = await createLogPath();
        const transcript = await runWithInput("hydrafusion\nhello\n/exit\n", logPath);
        expect(transcript).toContain("HydraFusion was requested but no session.fusion_completed");
        expect(transcript).toContain("--enable-hydrafusion");
        const records = (await readFile(logPath, "utf8"))
            .trimEnd()
            .split("\n")
            .map((line) => JSON.parse(line));
        expect(records).toContainEqual(
            expect.objectContaining({
                source: "chat.diagnostic",
                event: expect.objectContaining({ type: "fusion.not_executed" }),
            })
        );
    });

    it("requires a fresh Fusion completion for each turn and preserves its full payload", async () => {
        const completed: SessionEvent = {
            ...eventBase,
            type: "session.fusion_completed",
            data: {
                fusionId: "fusion-1",
                commitId: "commit-1",
                syntheticModel: "hydrafusion",
                turnId: "1",
                pattern: "single",
                outcome: "completed",
                phaseCount: 1,
                requestCount: 1,
                finalSourceModel: "gpt-5.6-sol",
                finalSourcePhaseId: "phase-1",
                followUpModel: "gpt-5.6-sol",
                degradedReason: null,
                durationMs: 1,
                inputTokens: 1,
                outputTokens: 1,
                cachedTokens: 0,
                totalNanoAiu: 1,
            },
        };
        mocks.sendAndWait.mockImplementationOnce(async () => {
            const config: SessionConfig = mocks.createSession.mock.calls[0][0];
            config.onEvent?.(completed);
            return reply;
        });
        const logPath = await createLogPath();
        await runWithInput("hydrafusion\nfirst\nsecond\n/exit\n", logPath, true);
        const records = (await readFile(logPath, "utf8"))
            .trimEnd()
            .split("\n")
            .map((line) => JSON.parse(line));
        expect(records).toContainEqual(
            expect.objectContaining({ source: "sdk.session", event: completed })
        );
        expect(records.filter((record) => record.source === "chat.diagnostic")).toHaveLength(1);
    });

    it("keeps the runtime default on Enter and closes cleanly on EOF", async () => {
        await runWithInput("\n\n");
        expect(mocks.createSession).toHaveBeenCalledWith(
            expect.objectContaining({ model: undefined })
        );
        expect(mocks.sendAndWait).not.toHaveBeenCalled();
        expect(mocks.stop).toHaveBeenCalledOnce();
    });

    it("does not create a session when input closes at the model picker", async () => {
        await runWithInput("");
        expect(mocks.createSession).not.toHaveBeenCalled();
        expect(mocks.stop).toHaveBeenCalledOnce();
    });

    it("prints and saves every early, tool, subagent delta, lifecycle, telemetry, and shutdown notification", async () => {
        const toolEvent: SessionEvent = {
            ...eventBase,
            type: "tool.execution_start",
            data: {
                toolCallId: "tool-1",
                toolName: "example",
                arguments: {
                    nested: { one: { two: { three: { four: "deep-value" } } } },
                    items: Array.from({ length: 150 }, (_, index) => index),
                    text: `${"x".repeat(12000)}\n"quoted text"\r\n`,
                },
            },
        };
        const delta: SessionEvent = {
            ...eventBase,
            type: "assistant.message_delta",
            agentId: "subagent-1",
            ephemeral: true,
            data: { messageId: "message-1", deltaContent: "streamed chunk" },
        };
        const lifecycle = { type: "session.deleted", sessionId: "chat-session" } as const;
        const telemetry = {
            restricted: true,
            sessionId: "chat-session",
            event: {
                kind: "shutdown",
                properties: { detail: "full telemetry value" },
                metrics: { count: 1 },
            },
        };
        mocks.start.mockImplementation(async () => {
            mocks.onLifecycle.mock.calls[0][0](lifecycle);
        });
        mocks.sendAndWait.mockImplementation(async () => {
            const config: SessionConfig = mocks.createSession.mock.calls[0][0];
            for (const event of [toolEvent, delta, reply]) config.onEvent?.(event);
            return reply;
        });
        mocks.stop.mockImplementation(async () => {
            await mocks.construct.mock.calls[0][0].onGitHubTelemetry?.(telemetry);
            return [];
        });

        const logPath = await createLogPath();
        const transcript = await runWithInput("1\nhello\n/exit\n", logPath);
        for (const event of [startEvent, toolEvent, delta, reply, lifecycle, telemetry]) {
            expect(transcript).toContain(JSON.stringify(event, null, 2));
        }
        expect(transcript).toContain("[sdk.session]");
        expect(transcript).toContain("[sdk.lifecycle]");
        expect(transcript).toContain("[sdk.telemetry]");
        expect(transcript.indexOf('"session.start"')).toBeLessThan(
            transcript.indexOf("Chat with Copilot")
        );
        expect(transcript).toContain(`SDK event log: ${logPath}`);

        const log = await readFile(logPath, "utf8");
        expect(log.endsWith("\n")).toBe(true);
        const records: Array<{ receivedAt: string; source: string; event: unknown }> = log
            .trimEnd()
            .split("\n")
            .map((line) => JSON.parse(line));
        expect(records).toEqual(
            [
                ["sdk.lifecycle", lifecycle],
                ["sdk.session", startEvent],
                ["sdk.session", toolEvent],
                ["sdk.session", delta],
                ["sdk.session", reply],
                ["sdk.telemetry", telemetry],
            ].map(([source, event]) => ({
                receivedAt: expect.any(String),
                source,
                event,
            }))
        );
        for (const record of records) {
            expect(Number.isNaN(Date.parse(record.receivedAt))).toBe(false);
        }
    });

    it("creates parent directories for a custom log path", async () => {
        const logPath = join(await createLogPath(), "nested", "chat.jsonl");
        await runWithInput("1\n/exit\n", logPath);
        const record = JSON.parse(await readFile(logPath, "utf8"));
        expect(record).toMatchObject({ source: "sdk.session", event: startEvent });
    });

    it("refuses to overwrite an existing event log before starting the runtime", async () => {
        const logPath = await createLogPath();
        await writeFile(logPath, "existing log\n");
        await expect(runWithInput("1\n/exit\n", logPath)).rejects.toMatchObject({
            message: "Chat failed",
            errors: expect.arrayContaining([expect.objectContaining({ code: "EEXIST" })]),
        });
        expect(await readFile(logPath, "utf8")).toBe("existing log\n");
        expect(mocks.start).not.toHaveBeenCalled();
    });

    it("reports a write failure rather than silently dropping events", async () => {
        const diskError = new Error("disk full");
        const write = vi
            .spyOn(WriteStream.prototype, "_write")
            .mockImplementation((_chunk, _encoding, callback) => callback(diskError));
        try {
            await expect(runWithInput("1\n/exit\n")).rejects.toMatchObject({
                message: "Chat failed",
                errors: [diskError],
            });
            expect(mocks.stop).toHaveBeenCalledOnce();
        } finally {
            write.mockRestore();
        }
    });

    it("reports both a failed turn and cleanup errors", async () => {
        const turnError = new Error("turn failed");
        const stopError = new Error("stop failed");
        mocks.sendAndWait.mockRejectedValue(turnError);
        mocks.stop.mockResolvedValue([stopError]);

        const logPath = await createLogPath();
        await expect(runWithInput("1\nhello\n", logPath)).rejects.toMatchObject({
            message: "Chat failed",
            errors: [turnError, stopError],
        });
        expect(mocks.unsubscribe).toHaveBeenCalledOnce();
        expect(JSON.parse(await readFile(logPath, "utf8"))).toMatchObject({
            source: "sdk.session",
            event: startEvent,
        });
    });
});

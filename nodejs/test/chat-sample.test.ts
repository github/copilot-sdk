/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { PassThrough } from "node:stream";
import { beforeEach, describe, expect, it, vi } from "vitest";
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

async function runWithInput(text: string) {
    const input = new PassThrough();
    const output = new PassThrough();
    let transcript = "";
    output.setEncoding("utf8");
    output.on("data", (chunk: string) => {
        transcript += chunk;
    });
    const running = runChat(input, output);
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

    it("prints complete early, tool, subagent delta, lifecycle, telemetry, and shutdown notifications", async () => {
        const toolEvent: SessionEvent = {
            ...eventBase,
            type: "tool.execution_start",
            data: {
                toolCallId: "tool-1",
                toolName: "example",
                arguments: {
                    nested: { one: { two: { three: { four: "deep-value" } } } },
                    items: Array.from({ length: 150 }, (_, index) => index),
                    text: "x".repeat(12000),
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

        const transcript = await runWithInput("1\nhello\n/exit\n");
        for (const event of [startEvent, toolEvent, delta, reply, lifecycle, telemetry]) {
            expect(transcript).toContain(JSON.stringify(event, null, 2));
        }
        expect(transcript).toContain("[sdk.session]");
        expect(transcript).toContain("[sdk.lifecycle]");
        expect(transcript).toContain("[sdk.telemetry]");
        expect(transcript.indexOf('"session.start"')).toBeLessThan(
            transcript.indexOf("Chat with Copilot")
        );
    });

    it("reports both a failed turn and cleanup errors", async () => {
        const turnError = new Error("turn failed");
        const stopError = new Error("stop failed");
        mocks.sendAndWait.mockRejectedValue(turnError);
        mocks.stop.mockResolvedValue([stopError]);

        await expect(runWithInput("1\nhello\n")).rejects.toMatchObject({
            message: "Chat failed",
            errors: [turnError, stopError],
        });
        expect(mocks.unsubscribe).toHaveBeenCalledOnce();
    });
});

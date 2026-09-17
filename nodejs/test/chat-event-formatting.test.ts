/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { createChatEventFormatter } from "../samples/chatEventFormatting.js";

const timing = {
    receivedAt: "2026-09-16T23:00:01.234Z",
    elapsedMs: 1234,
    sequence: 7,
};

describe("compact chat timeline", () => {
    it.each([
        ["session.fusion_route_started", "Fusion routing started"],
        ["session.fusion_resolved", "Fusion workflow selected"],
        ["assistant.fusion_phase_started", "Fusion phase started"],
        ["assistant.fusion_phase_completed", "Fusion phase completed"],
        ["session.fusion_commit_started", "Fusion commit started"],
        ["session.fusion_completed", "Fusion turn completed"],
        ["session.fusion_route_failed", "Fusion route failed"],
        ["assistant.fusion_phase_failed", "Fusion phase failed"],
        ["session.fusion_future_event", "Fusion future event"],
    ])("shows every %s as one concise arrival line", (type, title) => {
        const format = createChatEventFormatter();
        const display = format(
            "sdk.session",
            {
                type,
                id: "unnecessary-event-id",
                data: {
                    pattern: "critique",
                    model: "gpt-5.6-sol",
                    content: "huge-body".repeat(1000),
                    futureField: "unnecessary-detail",
                },
            },
            timing
        );
        expect(display).toContain(`[23:00:01.234Z +1.234s #7]`);
        expect(display).toContain(title);
        expect(display).toContain("workflow: critique");
        expect(display).toContain("model: gpt-5.6-sol");
        expect(display?.trimEnd().split("\n")).toHaveLength(1);
        expect(display).not.toContain("huge-body");
        expect(display).not.toContain("unnecessary");
    });

    it.each(["model_output", "tool_started", "tool_completed", "future_activity"])(
        "keeps every repeated Fusion %s progress event",
        (activity) => {
            const format = createChatEventFormatter();
            const event = {
                type: "assistant.fusion_phase_activity",
                data: { activity, totalResponseSizeBytes: 283 },
            };
            for (let sequence = 1; sequence <= 3; sequence++) {
                const display = format("sdk.session", event, { ...timing, sequence });
                expect(display).toContain("Fusion progress: ");
                expect(display).toContain("bytes: 283");
                expect(display).toContain(`#${sequence}]`);
            }
        }
    );

    it("summarizes workflow plans and Fusion telemetry without payload bodies", () => {
        const format = createChatEventFormatter();
        const plan = format(
            "sdk.session",
            {
                type: "session.fusion_resolved",
                data: {
                    phasePlan: [
                        { kind: "draft" },
                        { kind: "critic" },
                        { kind: "revision", conditional: true },
                    ],
                    scores: { secret: "not-for-summary" },
                },
            },
            timing
        );
        expect(plan).toContain("draft -> critic -> revision (optional)");
        expect(plan).not.toContain("not-for-summary");
        const telemetry = format(
            "sdk.telemetry",
            {
                event: {
                    kind: "hydrafusion_phase",
                    properties: { model: "gpt-5.6-sol" },
                    metrics: { duration_ms: 5800 },
                },
            },
            timing
        );
        expect(telemetry).toContain("Fusion phase telemetry");
        expect(telemetry).toContain("duration ms: 5800");
        expect(
            format(
                "sdk.telemetry",
                {
                    event: {
                        kind: "response.success",
                        properties: { fusion_id: "fusion-1" },
                        metrics: {},
                    },
                },
                timing
            )
        ).toContain("[Fusion]");
    });

    it("filters housekeeping while keeping turn boundaries and errors", () => {
        const format = createChatEventFormatter();
        for (const type of [
            "session.start",
            "session.created",
            "session.tools_updated",
            "session.usage_info",
            "session.background_tasks_changed",
            "pending_messages.modified",
            "model.messages_snapshot",
        ]) {
            expect(format("sdk.session", { type, data: {} }, timing)).toBeUndefined();
        }
        expect(
            format(
                "sdk.telemetry",
                {
                    event: { kind: "memory_usage", features: { HYDRAFUSION: "true" } },
                },
                timing
            )
        ).toBeUndefined();
        expect(format("sdk.session", { type: "assistant.turn_start" }, timing)).toContain(
            "Turn started"
        );
        expect(format("sdk.session", { type: "assistant.turn_end" }, timing)).toContain(
            "Turn finished"
        );
        expect(
            format(
                "sdk.session",
                { type: "session.error", data: { message: "important error" } },
                timing
            )
        ).toContain("Warning: important error");
        const warning = `${"explanation ".repeat(30)}restart with --enable-hydrafusion`;
        expect(
            format("chat.diagnostic", { type: "fusion.not_executed", message: warning }, timing)
        ).toContain(warning);
    });

    it("shows tool lifecycles without arguments/results and retains tool names by agent", () => {
        const format = createChatEventFormatter();
        const start = format(
            "sdk.session",
            {
                type: "tool.execution_start",
                data: {
                    toolCallId: "t1",
                    toolName: "powershell",
                    arguments: { secret: "hidden-args" },
                },
            },
            timing
        );
        expect(start).toContain("Tool invoked: powershell");
        expect(start).not.toContain("hidden-args");
        const otherAgent = format(
            "sdk.session",
            {
                type: "tool.execution_complete",
                agentId: "child",
                data: { toolCallId: "t1", success: true },
            },
            timing
        );
        expect(otherAgent).not.toContain("powershell");
        const end = format(
            "sdk.session",
            {
                type: "tool.execution_complete",
                data: { toolCallId: "t1", success: true, result: { content: "hidden-result" } },
            },
            timing
        );
        expect(end).toContain("Tool completed: powershell");
        expect(end).toContain("succeeded");
        expect(end).not.toContain("hidden-result");
        expect(
            format("sdk.session", { type: "assistant.tool_call_delta", data: {} }, timing)
        ).toContain("(*streaming*)");
    });

    it("shows message/reasoning events as previews and marks matched streamed messages", () => {
        const format = createChatEventFormatter();
        const delta = {
            type: "assistant.message_delta",
            data: { messageId: "m1", deltaContent: "hello" },
        };
        expect(format("sdk.session", delta, timing)).toContain(
            "Assistant message delta: hello (*streaming*)"
        );
        expect(
            format(
                "sdk.session",
                {
                    type: "assistant.message",
                    agentId: "child",
                    data: { messageId: "m1", content: "other" },
                },
                timing
            )
        ).not.toContain("(*streaming*)");
        expect(
            format(
                "sdk.session",
                { type: "assistant.message", data: { messageId: "m1", content: "hello world" } },
                timing
            )
        ).toContain("Assistant: hello world (*streaming*)");
        expect(
            format(
                "sdk.session",
                {
                    type: "assistant.message",
                    data: { messageId: "m1", content: "not streamed again" },
                },
                timing
            )
        ).not.toContain("(*streaming*)");
        expect(
            format(
                "sdk.session",
                {
                    type: "assistant.reasoning_delta",
                    data: { reasoningId: "r1", deltaContent: "thinking" },
                },
                timing
            )
        ).toContain("(*streaming*)");
        expect(
            format(
                "sdk.session",
                { type: "assistant.reasoning", data: { reasoningId: "r1", content: "thought" } },
                timing
            )
        ).toContain("(*streaming*)");
        const long = format(
            "sdk.session",
            { type: "system.message", data: { content: "x".repeat(5000) } },
            timing
        );
        expect(long).toContain(`${"x".repeat(177)}...`);
        expect(long).not.toContain("x".repeat(181));
        expect(
            format("sdk.session", { type: "user.message", data: { content: "hi!" } }, timing)
        ).toContain("You: hi!");
        expect(
            format(
                "sdk.session",
                { type: "model.messages_snapshot", data: { messages: [{}, {}] } },
                timing
            )
        ).toBeUndefined();
        expect(
            format(
                "sdk.session",
                {
                    type: "model.message",
                    data: { message: { role: "assistant", content: "nested message" } },
                },
                timing
            )
        ).toContain("Assistant message: nested message");
        expect(
            format(
                "sdk.telemetry",
                { event: { kind: "assistant_message", properties: {}, metrics: {} } },
                timing
            )
        ).toContain("Assistant message telemetry");
    });

    it("marks streaming generated inside a Fusion phase even when the answer is staged", () => {
        const format = createChatEventFormatter();
        const attribution = { fusionId: "f1", phaseId: "p1", sourceModel: "gpt-5.6-sol" };
        format(
            "sdk.session",
            {
                type: "assistant.fusion_phase_activity",
                data: { ...attribution, activity: "model_output" },
            },
            timing
        );
        expect(
            format(
                "sdk.session",
                {
                    type: "assistant.fusion_phase_completed",
                    data: { ...attribution, content: "answer" },
                },
                timing
            )
        ).toContain("(*streaming*) [staged phase]");
        const message = {
            type: "assistant.message",
            data: { messageId: "m1", content: "answer", fusion: attribution },
        };
        expect(format("sdk.session", message, timing)).toContain("(*streaming*) [staged phase]");
        expect(
            format(
                "sdk.session",
                {
                    type: "assistant.message",
                    data: { content: "another phase", fusion: { ...attribution, phaseId: "p2" } },
                },
                timing
            )
        ).not.toContain("(*streaming*)");
        format(
            "sdk.session",
            { type: "session.fusion_completed", data: { fusionId: "f1" } },
            timing
        );
        expect(format("sdk.session", message, timing)).not.toContain("(*streaming*)");
    });

    it("tracks generic streaming only for the current model call", () => {
        const format = createChatEventFormatter();
        format(
            "sdk.session",
            { type: "assistant.streaming_delta", data: { totalResponseSizeBytes: 12 } },
            timing
        );
        expect(
            format(
                "sdk.session",
                { type: "assistant.message", data: { content: "streamed" } },
                timing
            )
        ).toContain("(*streaming*)");
        for (const type of ["user.message", "system.message"]) {
            expect(
                format("sdk.session", { type, data: { content: "not streamed" } }, timing)
            ).not.toContain("(*streaming*)");
        }
        expect(
            format("sdk.session", { type: "model.call_start", data: {} }, timing)
        ).toBeUndefined();
        expect(
            format("sdk.session", { type: "assistant.message", data: { content: "plain" } }, timing)
        ).not.toContain("(*streaming*)");
        expect(
            format(
                "sdk.session",
                { type: "assistant.message_start", ephemeral: true, data: {} },
                timing
            )
        ).not.toContain("(*streaming*)");
    });

    it("keeps control characters and multiline messages from corrupting the timeline", () => {
        const format = createChatEventFormatter();
        const event = {
            type: "assistant.message",
            data: { content: "\u001b[2J\nhello\rworld\u0007" },
        };
        const before = structuredClone(event);
        const display = format("sdk.session", event, timing);
        expect(display).not.toContain("\u001b");
        expect(display).not.toContain("\r");
        expect(display).toContain("\\u001b[2J hello world\\u0007");
        expect(display?.trimEnd().split("\n")).toHaveLength(1);
        expect(event).toEqual(before);
    });

    it("uses indentation for logical levels and optional color, not a JSON body", () => {
        const format = createChatEventFormatter();
        const turn = format("sdk.session", { type: "assistant.turn_start" }, timing);
        const phase = format("sdk.session", { type: "assistant.fusion_phase_started" }, timing);
        const tool = format(
            "sdk.session",
            { type: "tool.execution_start", data: { fusion: { phaseId: "p1" } } },
            timing
        );
        expect(turn).toContain("] Turn started");
        expect(phase).toContain("]     Fusion phase started");
        expect(tool).toContain("]       Tool invoked");
        expect(phase).not.toContain("\u001b");
        expect(
            format("sdk.session", { type: "session.fusion_completed" }, { ...timing, color: true })
        ).toContain("\u001b");
    });
});

/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { PassThrough } from "node:stream";
import { describe, expect, it } from "vitest";
import { FusionChatRenderer } from "../samples/fusionChatRenderer.js";

const eventBase = {
    id: "event-1",
    parentId: null,
    timestamp: "2026-09-17T00:00:00.000Z",
};

function createRenderer(options: { debug?: boolean; interactive?: boolean } = {}) {
    const output = new PassThrough();
    if (options.interactive) {
        Object.assign(output, { isTTY: true });
    }
    let transcript = "";
    output.setEncoding("utf8");
    output.on("data", (chunk: string) => {
        transcript += chunk;
    });
    return {
        renderer: new FusionChatRenderer(output, { color: false, ...options }),
        transcript: () => transcript,
    };
}

describe("FusionChatRenderer", () => {
    it("shows a concise workflow and nested phase by default", () => {
        const { renderer, transcript } = createRenderer();
        renderer.handle({
            ...eventBase,
            type: "session.fusion_route_started",
            ephemeral: true,
            data: {
                attemptId: "route-1",
                turnKind: "user",
                syntheticModel: "hydrafusion",
                policy: "max",
            },
        });
        renderer.handle({
            ...eventBase,
            type: "session.fusion_resolved",
            data: {
                fusionId: "fusion-1",
                turnId: "fusion-turn-1",
                syntheticModel: "hydrafusion",
                policy: "max",
                routeSource: "capi_plan",
                contractVersion: 1,
                planVersion: "1",
                policyVersion: null,
                modelUniverseVersion: null,
                ruleId: null,
                scores: null,
                pattern: "critique",
                phasePlan: [
                    { kind: "draft", role: "solver", scope: "root", conditional: false },
                    { kind: "critic", role: "critic", scope: "review", conditional: false },
                    { kind: "revision", role: "solver", scope: "root", conditional: true },
                ],
                primaryModel: "gpt-5.6-luna",
                secondaryModel: "gpt-5.6-terra",
                fallbackModel: "gpt-5.6-sol",
                followUpModel: "gpt-5.6-luna",
                followUp: null,
                routingLatencyMs: 123,
            },
        });
        renderer.handle({
            ...eventBase,
            type: "assistant.fusion_phase_started",
            ephemeral: true,
            data: {
                fusionId: "fusion-1",
                phaseId: "phase-critic",
                phaseKind: "critic",
                role: "critic",
                conversationScope: "review",
                pattern: "critique",
                model: "gpt-5.6-terra",
            },
        });

        expect(transcript()).toContain("Selecting the Fusion workflow...");
        expect(transcript()).toContain("Workflow selected: Critique");
        expect(transcript()).not.toContain("policy:");
        expect(transcript()).not.toContain("routing:");
        expect(transcript()).not.toContain("[1] Draft");
        expect(transcript()).not.toContain("[2] Critic");
        expect(transcript()).not.toContain("[3] Revision");
        expect(transcript()).toContain("    +-- Critic started");
        expect(transcript()).not.toContain("role:");
        expect(transcript()).not.toContain("scope:");
        expect(transcript()).not.toContain("model:");
    });

    it("shows the workflow overview and phase metadata with debug enabled", () => {
        const { renderer, transcript } = createRenderer({ debug: true });
        renderer.handle({
            ...eventBase,
            type: "session.fusion_resolved",
            data: {
                fusionId: "fusion-1",
                pattern: "critique",
                policy: "max",
                routingLatencyMs: 123,
                phasePlan: [
                    { kind: "draft", role: "solver", scope: "root", conditional: false },
                    { kind: "critic", role: "critic", scope: "review", conditional: false },
                ],
            },
        });
        renderer.handle({
            ...eventBase,
            type: "assistant.fusion_phase_started",
            data: {
                fusionId: "fusion-1",
                phaseId: "phase-critic",
                phaseKind: "critic",
                role: "critic",
                conversationScope: "review",
                model: "gpt-5.6-terra",
            },
        });
        renderer.handle({
            ...eventBase,
            type: "session.fusion_commit_started",
            data: {
                fusionId: "fusion-1",
                sourcePhaseId: "phase-critic",
                sourceModel: "gpt-5.6-terra",
            },
        });

        expect(transcript()).toContain("Workflow selected: Critique");
        expect(transcript()).not.toContain("policy:");
        expect(transcript()).not.toContain("routing:");
        expect(transcript()).toContain("[1] Draft");
        expect(transcript()).toContain("[2] Critic");
        expect(transcript()).toContain(
            "Critic started (role: critic, scope: review, model: gpt-5.6-terra)"
        );
        expect(transcript()).toContain("Committing Critic from gpt-5.6-terra...");
    });

    it("streams assistant and reasoning deltas and suppresses their terminal duplicates", () => {
        const { renderer, transcript } = createRenderer({ interactive: true });
        renderer.handle({
            ...eventBase,
            type: "assistant.reasoning_delta",
            ephemeral: true,
            data: { reasoningId: "reasoning-1", deltaContent: "First " },
        });
        renderer.handle({
            ...eventBase,
            type: "assistant.reasoning_delta",
            ephemeral: true,
            data: { reasoningId: "reasoning-1", deltaContent: "thought" },
        });
        renderer.handle({
            ...eventBase,
            type: "assistant.reasoning",
            data: { reasoningId: "reasoning-1", content: "First thought" },
        });
        renderer.handle({
            ...eventBase,
            type: "assistant.message_delta",
            ephemeral: true,
            data: { messageId: "message-1", deltaContent: "Hello " },
        });
        renderer.handle({
            ...eventBase,
            type: "assistant.message_delta",
            ephemeral: true,
            data: { messageId: "message-1", deltaContent: "world" },
        });
        renderer.handle({
            ...eventBase,
            type: "assistant.message",
            data: { messageId: "message-1", content: "Hello world" },
        });
        renderer.finish();

        expect(transcript()).toContain("Thinking (*streaming*): First thought");
        expect(transcript()).toContain("Assistant (*streaming*): Hello world");
        expect(transcript().match(/First thought/g)).toHaveLength(1);
        expect(transcript().match(/Hello world/g)).toHaveLength(1);
    });

    it("shows reasoning embedded in a completed assistant message", () => {
        const { renderer, transcript } = createRenderer();
        renderer.handle({
            ...eventBase,
            type: "assistant.message",
            data: {
                messageId: "message-1",
                content: "Final answer",
                reasoningText: "I checked the relevant files before answering.",
            },
        });
        expect(transcript()).toContain("Thinking: I checked the relevant files before answering.");
        expect(transcript()).toContain("Assistant: Final answer");
    });

    it("shows provisional tools immediately and ignores committed replay duplicates", () => {
        const { renderer, transcript } = createRenderer();
        const fusion = {
            fusionId: "fusion-1",
            syntheticModel: "hydrafusion",
            policy: "max",
            pattern: "single",
            phaseId: "phase-primary",
            phaseKind: "primary",
            role: "solver",
            conversationScope: "root",
            sourceModel: "gpt-5.6-sol",
        };
        const start = {
            ...eventBase,
            type: "tool.execution_start",
            ephemeral: true,
            data: {
                toolCallId: "tool-1",
                toolName: "view",
                arguments: {
                    path: "README.md",
                    description: "Read the project overview",
                },
                fusion,
            },
        };
        const complete = {
            ...eventBase,
            type: "tool.execution_complete",
            ephemeral: true,
            data: {
                toolCallId: "tool-1",
                success: true,
                result: { content: "large result that should not be printed in full" },
                fusion,
            },
        };
        renderer.handle(start);
        renderer.handle(complete);
        renderer.handle({
            ...start,
            id: "event-2",
            ephemeral: undefined,
            data: { ...start.data, fusion: { ...fusion, commitId: "commit-1" } },
        });
        renderer.handle({
            ...complete,
            id: "event-3",
            ephemeral: undefined,
            data: { ...complete.data, fusion: { ...fusion, commitId: "commit-1" } },
        });

        expect(transcript()).toContain("      |-- Read the project overview");
        expect(transcript()).toContain("Read the project overview...done");
        expect(transcript()).not.toContain("`-- Completed");
        expect(transcript()).not.toContain("Tool: view");
        expect(transcript()).not.toContain("README.md");
        expect(transcript()).not.toContain("large result that should not be printed in full");
    });

    it("renders phase completion, commit choice, and final outcome in debug mode", () => {
        const { renderer, transcript } = createRenderer({ debug: true });
        renderer.handle({
            ...eventBase,
            type: "assistant.fusion_phase_activity",
            ephemeral: true,
            data: {
                fusionId: "fusion-1",
                phaseId: "phase-primary",
                phaseKind: "primary",
                pattern: "single",
                role: "solver",
                conversationScope: "root",
                activity: "model_output",
                totalResponseSizeBytes: 283,
            },
        });
        renderer.handle({
            ...eventBase,
            type: "assistant.message",
            ephemeral: true,
            data: {
                messageId: "message-1",
                content: "Phase answer",
                fusion: {
                    fusionId: "fusion-1",
                    phaseId: "phase-primary",
                    sourceModel: "gpt-5.6-sol",
                },
            },
        });
        renderer.handle({
            ...eventBase,
            type: "assistant.fusion_phase_completed",
            data: {
                fusionId: "fusion-1",
                phaseId: "phase-primary",
                phaseKind: "primary",
                role: "solver",
                conversationScope: "root",
                model: "gpt-5.6-sol",
                status: "succeeded",
                content: "Phase answer",
                verdict: null,
                durationMs: 5800,
                usage: {
                    requestCount: 2,
                    inputTokens: 100,
                    outputTokens: 20,
                    cachedTokens: 50,
                    totalNanoAiu: 10,
                },
                projectionMessage: { role: "assistant", content: "Phase answer" },
                projectionMode: "staged",
            },
        });
        renderer.handle({
            ...eventBase,
            type: "session.fusion_commit_started",
            data: {
                fusionId: "fusion-1",
                commitId: "commit-1",
                sourcePhaseId: "phase-primary",
                sourceModel: "gpt-5.6-sol",
                kind: "text",
                toolCallId: null,
            },
        });
        renderer.handle({
            ...eventBase,
            type: "session.fusion_completed",
            data: {
                fusionId: "fusion-1",
                commitId: "commit-1",
                turnId: "turn-1",
                syntheticModel: "hydrafusion",
                pattern: "single",
                outcome: "completed",
                finalSourcePhaseId: "phase-primary",
                finalSourceModel: "gpt-5.6-sol",
                followUpModel: "gpt-5.6-sol",
                degradedReason: null,
                phaseCount: 1,
                requestCount: 2,
                inputTokens: 100,
                outputTokens: 20,
                cachedTokens: 50,
                totalNanoAiu: 10,
                durationMs: 6000,
            },
        });

        expect(transcript()).not.toContain("producing output");
        expect(transcript()).toContain("Assistant: Phase answer");
        expect(transcript()).not.toContain("streaming, staged");
        expect(transcript()).toContain("    `-- Primary succeeded");
        expect(transcript()).toContain("5.80s");
        expect(transcript()).toContain("Committing Primary");
        expect(transcript()).toContain("Fusion complete: Single");
        expect(transcript()).toContain("2 requests");
    });

    it("hides phase-success and Fusion-complete summaries by default", () => {
        const { renderer, transcript } = createRenderer();
        renderer.handle({
            ...eventBase,
            type: "assistant.fusion_phase_completed",
            data: {
                fusionId: "fusion-1",
                phaseId: "phase-primary",
                phaseKind: "primary",
                model: "gpt-5.6-sol",
                status: "succeeded",
                durationMs: 5800,
                usage: { requestCount: 2 },
                projectionMode: "staged",
            },
        });
        renderer.handle({
            ...eventBase,
            type: "session.fusion_completed",
            data: {
                fusionId: "fusion-1",
                pattern: "single",
                outcome: "completed",
                finalSourceModel: "gpt-5.6-sol",
                requestCount: 2,
                durationMs: 6000,
            },
        });

        expect(transcript()).not.toContain("Primary succeeded");
        expect(transcript()).not.toContain("Fusion complete");
    });

    it("keeps review content visible without showing the phase-success summary", () => {
        const { renderer, transcript } = createRenderer();
        renderer.handle({
            ...eventBase,
            type: "assistant.fusion_phase_completed",
            data: {
                fusionId: "fusion-1",
                phaseId: "phase-critic",
                phaseKind: "critic",
                status: "succeeded",
                projectionMode: "none",
                content: "The implementation needs a revision.",
                verdict: "revise",
            },
        });
        expect(transcript()).not.toContain("Critic succeeded");
        expect(transcript()).toContain("Review: The implementation needs a revision.");
        expect(transcript()).toContain("Verdict: revise");
    });

    it.each([false, true])(
        "shows total and per-model AI Credits when the session quits (debug=%s)",
        (debug) => {
            const { renderer, transcript } = createRenderer({ debug });
            renderer.handle({
                ...eventBase,
                type: "session.shutdown",
                data: {
                    shutdownType: "routine",
                    totalNanoAiu: 12_719_240_000,
                    modelMetrics: {
                        "gpt-5.6-sol": { totalNanoAiu: 10_500_000_000 },
                        "gpt-5.6-terra": { totalNanoAiu: 2_219_240_000 },
                    },
                },
            });
            expect(transcript()).toContain(
                "Session ending - 12.72 AI Credits used (gpt-5.6-sol: 10.5; gpt-5.6-terra: 2.22)"
            );
        }
    );

    it("escapes control characters and bounds argument previews", () => {
        const { renderer, transcript } = createRenderer();
        renderer.handle({
            ...eventBase,
            type: "tool.execution_start",
            data: {
                toolCallId: "tool-1",
                toolName: "\u001b[2Jdanger",
                arguments: {
                    description: "\u001b[2JInspect input safely",
                    input: "x".repeat(1000),
                },
            },
        });
        expect(transcript()).not.toContain("\u001b");
        expect(transcript()).toContain("\\u001b[2JInspect input safely");
        expect(transcript()).not.toContain("danger");
        expect(transcript()).not.toContain("x".repeat(50));
        expect(transcript().length).toBeLessThan(700);
    });

    it.each(["web_search", "github-mcp-server-web_search"])(
        "shows the query for %s instead of requiring a description",
        (toolName) => {
            const { renderer, transcript } = createRenderer({ interactive: true });
            renderer.handle({
                ...eventBase,
                type: "tool.execution_start",
                data: {
                    toolCallId: "web-1",
                    toolName,
                    arguments: {
                        query: "GitHub Copilot release notes",
                        description: "Ignore this description",
                    },
                },
            });
            renderer.handle({
                ...eventBase,
                type: "tool.execution_complete",
                data: { toolCallId: "web-1", success: true },
            });

            expect(transcript()).toContain('|-- Searching the web: "GitHub Copilot release notes"');
            expect(transcript()).toContain(
                'Searching the web: "GitHub Copilot release notes"...done'
            );
            expect(transcript()).not.toContain("Ignore this description");
            expect(transcript()).not.toContain(toolName);
        }
    );
});

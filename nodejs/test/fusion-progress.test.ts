import { describe, expect, it } from "vitest";
import {
    initialFusionProgressState,
    reduceFusionProgress,
    type FusionProgressEventInput,
    type FusionProgressState,
} from "../src/fusionProgress.js";
import type { SessionEvent } from "../src/types.js";

// Compile-time proof that the generated session-event union satisfies the structural input the
// reducer accepts, so consumers can feed session events directly without adapters.
const _generatedEventsAreAcceptedInput: FusionProgressEventInput = {} as SessionEvent;

const FUSION_ID = "fusion-1";

function apply(events: readonly FusionProgressEventInput[]): FusionProgressState {
    return events.reduce(reduceFusionProgress, initialFusionProgressState());
}

function routeStarted(turnKind = "user"): FusionProgressEventInput {
    return {
        type: "session.fusion_route_started",
        data: { attemptId: "attempt-1", turnKind, policy: "balanced" },
    };
}

function resolved(
    pattern: string,
    phasePlan?: readonly Record<string, unknown>[],
    fusionId = FUSION_ID
): FusionProgressEventInput {
    return {
        type: "session.fusion_resolved",
        data: {
            contractVersion: 1,
            fusionId,
            turnId: "turn-1",
            pattern,
            policy: "balanced",
            // Model identities are part of the real payload and must never surface in the state.
            primaryModel: "secret-primary-model",
            secondaryModel: "secret-secondary-model",
            fallbackModel: "secret-fallback-model",
            followUpModel: "secret-follow-up-model",
            syntheticModel: "secret-synthetic-model",
            ...(phasePlan !== undefined ? { phasePlan } : {}),
        },
    };
}

function phaseStarted(
    phaseId: string,
    phaseKind: string,
    role: string,
    conversationScope = "root",
    fusionId = FUSION_ID
): FusionProgressEventInput {
    return {
        type: "assistant.fusion_phase_started",
        data: {
            fusionId,
            phaseId,
            phaseKind,
            role,
            conversationScope,
            pattern: "cascade",
            model: "secret-phase-model",
        },
    };
}

function phaseCompleted(
    phaseId: string,
    phaseKind: string,
    status = "succeeded",
    role = "solver",
    conversationScope = "root",
    fusionId = FUSION_ID
): FusionProgressEventInput {
    return {
        type: "assistant.fusion_phase_completed",
        data: {
            fusionId,
            phaseId,
            phaseKind,
            role,
            conversationScope,
            status,
            content: "secret phase content that must never be projected",
            verdict: "secret verdict",
            model: "secret-phase-model",
            durationMs: 1234,
            usage: { inputTokens: 10, outputTokens: 20 },
        },
    };
}

function activity(
    phaseId: string,
    kind: string,
    extra: Record<string, unknown> = {},
    fusionId = FUSION_ID
): FusionProgressEventInput {
    return {
        type: "assistant.fusion_phase_activity",
        data: {
            fusionId,
            phaseId,
            phaseKind: "primary",
            pattern: "single",
            role: "solver",
            conversationScope: "root",
            activity: kind,
            ...extra,
        },
    };
}

function completed(overrides: Record<string, unknown> = {}): FusionProgressEventInput {
    return {
        type: "session.fusion_completed",
        data: {
            fusionId: FUSION_ID,
            turnId: "turn-1",
            outcome: "succeeded",
            commitId: "commit-1",
            degradedReason: null,
            finalSourcePhaseId: "phase-primary",
            pattern: "single",
            phaseCount: 1,
            durationMs: 4321,
            finalSourceModel: "secret-final-model",
            syntheticModel: "secret-synthetic-model",
            followUpModel: "secret-follow-up-model",
            ...overrides,
        },
    };
}

const CASCADE_PLAN = [
    { kind: "primary", role: "solver", scope: "root", conditional: false },
    { kind: "judge", role: "judge", scope: "review", conditional: false },
    { kind: "repair", role: "repairer", scope: "root", conditional: true },
];

describe("reduceFusionProgress", () => {
    it("starts empty and ignores unrelated events", () => {
        const state = initialFusionProgressState();
        expect(state.status).toBe("inactive");
        expect(state.plan).toEqual([]);
        expect(state.planSource).toBe("unavailable");
        expect(reduceFusionProgress(state, { type: "user.message", data: { text: "hi" } })).toBe(
            state
        );
        expect(reduceFusionProgress(state, { type: "assistant.message", data: undefined })).toBe(
            state
        );
    });

    it("projects a single-pattern turn from routing to completion", () => {
        const state = apply([
            routeStarted(),
            resolved("single", [{ kind: "primary", role: "solver", scope: "root" }]),
            phaseStarted("phase-primary", "primary", "solver"),
            activity("phase-primary", "model_output", { totalResponseSizeBytes: 512 }),
            phaseCompleted("phase-primary", "primary"),
            {
                type: "assistant.message",
                data: {
                    content: "final answer",
                    fusion: {
                        fusionId: FUSION_ID,
                        pattern: "single",
                        policy: "balanced",
                        commitId: "commit-1",
                        syntheticModel: "secret",
                    },
                },
            },
            completed(),
        ]);

        expect(state.status).toBe("completed");
        expect(state.fusionId).toBe(FUSION_ID);
        expect(state.turnId).toBe("turn-1");
        expect(state.turnKind).toBe("user");
        expect(state.pattern).toBe("single");
        expect(state.planSource).toBe("runtime");
        expect(state.plan).toEqual([
            { kind: "primary", role: "solver", scope: "root", conditional: false },
        ]);
        expect(state.phases).toHaveLength(1);
        expect(state.phases[0]).toMatchObject({
            phaseId: "phase-primary",
            kind: "primary",
            role: "solver",
            scope: "root",
            status: "succeeded",
            planIndex: 0,
            totalResponseSizeBytes: 512,
        });
        expect(state.publishedCommitId).toBe("commit-1");
        expect(state.completion).toEqual({
            outcome: "succeeded",
            degraded: false,
            commitId: "commit-1",
            finalSourcePhaseId: "phase-primary",
        });
    });

    it("tracks cascade phases against the published plan, including a conditional repair", () => {
        const state = apply([
            routeStarted(),
            resolved("cascade", CASCADE_PLAN),
            phaseStarted("p1", "primary", "solver"),
            phaseCompleted("p1", "primary"),
            phaseStarted("p2", "judge", "judge", "review"),
            phaseCompleted("p2", "judge", "succeeded", "judge", "review"),
            phaseStarted("p3", "repair", "repairer"),
        ]);

        expect(state.status).toBe("running");
        expect(state.plan).toHaveLength(3);
        expect(state.plan[2]).toMatchObject({ kind: "repair", conditional: true });
        expect(state.phases.map((phase) => [phase.phaseId, phase.status, phase.planIndex])).toEqual(
            [
                ["p1", "succeeded", 0],
                ["p2", "succeeded", 1],
                ["p3", "running", 2],
            ]
        );
    });

    it("tracks a critique turn including a review-scoped critic and a failed phase", () => {
        const state = apply([
            resolved("critique", [
                { kind: "draft", role: "drafter", scope: "root", conditional: false },
                { kind: "critic", role: "critic", scope: "review", conditional: false },
                { kind: "revision", role: "reviser", scope: "root", conditional: true },
            ]),
            phaseStarted("d1", "draft", "drafter"),
            phaseCompleted("d1", "draft", "succeeded", "drafter"),
            phaseStarted("c1", "critic", "critic", "review"),
            {
                type: "assistant.fusion_phase_failed",
                data: {
                    fusionId: FUSION_ID,
                    phaseId: "c1",
                    phaseKind: "critic",
                    role: "critic",
                    conversationScope: "review",
                    status: "failed",
                    reason: "provider_error",
                    errorMessage: "secret provider error detail",
                    model: "secret-critic-model",
                    degradedToPhaseId: "d1",
                    durationMs: 12,
                    usage: { inputTokens: 1, outputTokens: 0 },
                },
            },
        ]);

        expect(state.phases[0]).toMatchObject({
            phaseId: "d1",
            scope: "root",
            status: "succeeded",
        });
        expect(state.phases[1]).toMatchObject({
            phaseId: "c1",
            kind: "critic",
            scope: "review",
            status: "failed",
            degraded: true,
            degradedToPhaseId: "d1",
        });
    });

    it("degrades to phase events when an older runtime publishes no plan", () => {
        const state = apply([
            routeStarted(),
            resolved("cascade"),
            phaseStarted("p1", "primary", "solver"),
            phaseCompleted("p1", "primary"),
        ]);

        expect(state.planSource).toBe("unavailable");
        expect(state.plan).toEqual([]);
        expect(state.phases[0].planIndex).toBeUndefined();
        expect(state.phases[0]).toMatchObject({ kind: "primary", status: "succeeded" });
        expect(state.status).toBe("running");
    });

    it("ignores a malformed phase plan payload without losing the route", () => {
        const state = apply([
            resolved("cascade", undefined),
            {
                type: "session.fusion_resolved",
                data: { fusionId: FUSION_ID, pattern: "cascade", phasePlan: "not-an-array" },
            },
        ]);

        expect(state.status).toBe("running");
        expect(state.plan).toEqual([]);
        expect(state.planSource).toBe("unavailable");
    });

    it("recovers a phase that never emitted its ephemeral started event", () => {
        const state = apply([resolved("single"), phaseCompleted("p1", "primary")]);

        expect(state.phases).toHaveLength(1);
        expect(state.phases[0]).toMatchObject({
            phaseId: "p1",
            kind: "primary",
            status: "succeeded",
        });
    });

    it("is idempotent for duplicated events", () => {
        const events = [
            routeStarted(),
            resolved("cascade", CASCADE_PLAN),
            phaseStarted("p1", "primary", "solver"),
            activity("p1", "tool_started", { toolCallId: "call-1" }),
            activity("p1", "tool_completed", { toolCallId: "call-1" }),
            phaseCompleted("p1", "primary"),
            completed({ pattern: "cascade" }),
        ];

        const once = apply(events);
        const twice = apply(events.flatMap((event) => [event, event]));

        expect(twice).toEqual(once);
        expect(reduceFusionProgress(once, completed({ pattern: "cascade" }))).toBe(once);
        expect(reduceFusionProgress(once, resolved("cascade", CASCADE_PLAN))).toBe(once);

        // Routing signals carry no turn identity, so a repeat while still routing is a no-op.
        const routing = apply([routeStarted()]);
        expect(reduceFusionProgress(routing, routeStarted())).toBe(routing);
        // A routing signal after a resolved turn never discards the resolved projection.
        const running = apply([routeStarted(), resolved("single")]);
        expect(reduceFusionProgress(running, routeStarted())).toBe(running);
        // Repeating a phase signal leaves the projection untouched.
        const phase = apply([resolved("single"), phaseStarted("p1", "primary", "solver")]);
        expect(reduceFusionProgress(phase, phaseStarted("p1", "primary", "solver"))).toBe(phase);
    });

    it("does not regress terminal phase state on out-of-order events", () => {
        const state = apply([
            resolved("single"),
            phaseCompleted("p1", "primary"),
            phaseStarted("p1", "primary", "solver"),
            activity("p1", "model_output", { totalResponseSizeBytes: 64 }),
        ]);

        expect(state.phases[0].status).toBe("succeeded");
        expect(state.phases[0].totalResponseSizeBytes).toBe(64);
    });

    it("keeps the largest observed response size across out-of-order activity", () => {
        const state = apply([
            resolved("single"),
            activity("p1", "model_output", { totalResponseSizeBytes: 900 }),
            activity("p1", "model_output", { totalResponseSizeBytes: 300 }),
        ]);

        expect(state.phases[0].totalResponseSizeBytes).toBe(900);
        expect(state.phases[0].activity).toEqual({
            kind: "model_output",
            totalResponseSizeBytes: 300,
        });
    });

    it("records phase activity and tool calls, keeping completion terminal", () => {
        const state = apply([
            resolved("single"),
            phaseStarted("p1", "primary", "solver"),
            activity("p1", "tool_started", { toolCallId: "call-1" }),
            activity("p1", "tool_completed", { toolCallId: "call-1" }),
            activity("p1", "tool_started", { toolCallId: "call-1" }),
            activity("p1", "tool_started", { toolCallId: "call-2" }),
        ]);

        expect(state.phases[0].toolCalls).toEqual([
            { toolCallId: "call-1", status: "completed" },
            { toolCallId: "call-2", status: "started" },
        ]);
        expect(state.phases[0].activity).toEqual({ kind: "tool_started", toolCallId: "call-2" });
    });

    it("attributes tool executions to phases on runtimes without activity events", () => {
        const fusion = {
            fusionId: FUSION_ID,
            pattern: "single",
            policy: "balanced",
            syntheticModel: "secret-synthetic-model",
            phaseId: "p1",
            phaseKind: "primary",
            role: "solver",
            conversationScope: "root",
            sourceModel: "secret-source-model",
        };
        const state = apply([
            resolved("single"),
            {
                type: "tool.execution_start",
                data: { toolCallId: "call-9", toolName: "bash", arguments: "secret args", fusion },
            },
            {
                type: "tool.execution_complete",
                data: { toolCallId: "call-9", result: "secret tool output", fusion },
            },
        ]);

        expect(state.phases[0]).toMatchObject({ phaseId: "p1", kind: "primary", role: "solver" });
        expect(state.phases[0].toolCalls).toEqual([{ toolCallId: "call-9", status: "completed" }]);
    });

    it("tracks attributed permission requests until they are resolved", () => {
        const requested: FusionProgressEventInput = {
            type: "permission.requested",
            data: {
                requestId: "req-1",
                permissionRequest: { kind: "shell", command: "rm -rf secret" },
                fusion: {
                    fusionId: FUSION_ID,
                    pattern: "cascade",
                    policy: "balanced",
                    syntheticModel: "secret-synthetic-model",
                    phaseId: "p1",
                    phaseKind: "primary",
                    role: "solver",
                    conversationScope: "root",
                },
            },
        };

        const pending = apply([resolved("cascade"), requested, requested]);
        expect(pending.pendingPermissions).toEqual([
            {
                requestId: "req-1",
                fusionId: FUSION_ID,
                phaseId: "p1",
                phaseKind: "primary",
                role: "solver",
                scope: "root",
            },
        ]);

        const resolvedPermission = reduceFusionProgress(pending, {
            type: "permission.completed",
            data: { requestId: "req-1", result: { kind: "approved" } },
        });
        expect(resolvedPermission.pendingPermissions).toEqual([]);
        expect(
            reduceFusionProgress(resolvedPermission, {
                type: "permission.completed",
                data: { requestId: "req-1", result: { kind: "approved" } },
            })
        ).toBe(resolvedPermission);
    });

    it("ignores permission requests that carry no fusion attribution", () => {
        const state = apply([
            resolved("single"),
            {
                type: "permission.requested",
                data: { requestId: "req-2", permissionRequest: { kind: "shell" } },
            },
        ]);

        expect(state.pendingPermissions).toEqual([]);
    });

    it("records the published commit and the authoritative completion commit", () => {
        const state = apply([
            resolved("cascade", CASCADE_PLAN),
            phaseStarted("p1", "primary", "solver"),
            {
                type: "assistant.message",
                data: {
                    content: "answer",
                    fusion: {
                        fusionId: FUSION_ID,
                        pattern: "cascade",
                        policy: "balanced",
                        syntheticModel: "secret-synthetic-model",
                        commitId: "commit-7",
                        sourcePhaseId: "p1",
                    },
                },
            },
            completed({ commitId: "commit-7", degradedReason: "judge_unavailable" }),
        ]);

        expect(state.publishedCommitId).toBe("commit-7");
        expect(state.completion).toEqual({
            outcome: "succeeded",
            degraded: true,
            commitId: "commit-7",
            finalSourcePhaseId: "phase-primary",
        });
        expect(state.status).toBe("completed");
    });

    it("reports a deterministic fallback when routing fails", () => {
        const state = apply([
            routeStarted("compaction"),
            {
                type: "session.fusion_route_failed",
                data: {
                    attemptId: "attempt-1",
                    policy: "balanced",
                    reason: "router_unavailable",
                    errorMessage: "secret router error",
                    fallbackModel: "secret-fallback-model",
                    syntheticModel: "secret-synthetic-model",
                },
            },
        ]);

        expect(state.status).toBe("fallback");
        expect(state.policy).toBe("balanced");
        expect(state.turnKind).toBe("compaction");
        expect(state.phases).toEqual([]);
        expect(JSON.stringify(state)).not.toContain("secret");
    });

    it("starts a new turn on a new resolved route and ignores late events from the previous turn", () => {
        const firstTurn = apply([
            resolved("single"),
            phaseStarted("p1", "primary", "solver"),
            completed(),
        ]);
        const secondTurn = apply([
            resolved("single"),
            phaseStarted("p1", "primary", "solver"),
            completed(),
            resolved("cascade", CASCADE_PLAN, "fusion-2"),
            phaseStarted("q1", "primary", "solver", "root", "fusion-2"),
            // Late ephemeral from the previous turn must not disturb the current projection.
            activity("p1", "model_output", { totalResponseSizeBytes: 10 }),
            phaseCompleted("p1", "primary", "succeeded"),
        ]);

        expect(firstTurn.status).toBe("completed");
        expect(secondTurn.fusionId).toBe("fusion-2");
        expect(secondTurn.status).toBe("running");
        expect(secondTurn.completion).toBeUndefined();
        expect(secondTurn.publishedCommitId).toBeUndefined();
        expect(secondTurn.phases.map((phase) => phase.phaseId)).toEqual(["q1"]);
    });

    it("begins a fresh projection when routing starts after a completed turn", () => {
        const state = apply([
            resolved("single", [{ kind: "primary", role: "solver", scope: "root" }]),
            phaseStarted("p1", "primary", "solver"),
            completed(),
            routeStarted(),
        ]);

        expect(state.status).toBe("routing");
        expect(state.fusionId).toBeUndefined();
        expect(state.phases).toEqual([]);
        expect(state.plan).toEqual([]);
        expect(state.completion).toBeUndefined();
    });

    it("adopts a turn identity discovered from a phase event before the route resolves", () => {
        const state = apply([
            phaseStarted("p1", "primary", "solver"),
            resolved("single", [{ kind: "primary", role: "solver", scope: "root" }]),
        ]);

        expect(state.fusionId).toBe(FUSION_ID);
        expect(state.status).toBe("running");
        expect(state.phases.map((phase) => phase.phaseId)).toEqual(["p1"]);
        expect(state.phases[0].planIndex).toBe(0);
    });

    it("never projects content, verdicts, error detail, or concrete model identities", () => {
        const state = apply([
            routeStarted(),
            resolved("cascade", CASCADE_PLAN),
            phaseStarted("p1", "primary", "solver"),
            activity("p1", "model_output", { totalResponseSizeBytes: 128 }),
            phaseCompleted("p1", "primary"),
            {
                type: "assistant.fusion_phase_failed",
                data: {
                    fusionId: FUSION_ID,
                    phaseId: "p2",
                    phaseKind: "judge",
                    role: "judge",
                    conversationScope: "review",
                    status: "failed",
                    reason: "provider_error",
                    errorMessage: "secret provider error",
                    model: "secret-judge-model",
                },
            },
            {
                type: "assistant.message",
                data: {
                    content: "secret assistant content",
                    reasoningText: "secret reasoning",
                    fusion: {
                        fusionId: FUSION_ID,
                        pattern: "cascade",
                        policy: "balanced",
                        commitId: "commit-1",
                        sourceModel: "secret-source-model",
                        syntheticModel: "secret-synthetic-model",
                    },
                },
            },
            completed({ degradedReason: "judge_unavailable" }),
        ]);

        const serialized = JSON.stringify(state);
        expect(serialized).not.toContain("secret");
        expect(serialized).not.toContain("provider_error");
        expect(serialized).not.toContain("judge_unavailable");
        expect(serialized).not.toContain("Model");
        expect(serialized).not.toContain("content");
        expect(serialized).not.toContain("verdict");
        expect(state.completion?.degraded).toBe(true);
    });
});

/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * Presentation-neutral HydraFusion progress projection.
 *
 * `reduceFusionProgress` folds session events into {@link FusionProgressState}, a deterministic
 * snapshot of *what the runtime is doing right now* for a HydraFusion turn. It contains no prose,
 * colors, timers, layout, or CLI-specific concepts: consumers (terminal UIs, web UIs, logs,
 * dashboards) decide entirely how to render it.
 *
 * Design constraints:
 *
 * - **Pure and event-driven.** The reducer never performs I/O and never issues an RPC. Feed it the
 *   events a session already emits; a resumed session can be rebuilt by replaying its durable
 *   event log.
 * - **Tolerant.** Duplicate events are idempotent, out-of-order events never regress a terminal
 *   phase state, missed ephemeral events are recovered from the durable events that follow, and
 *   every field added by newer runtimes is optional — against an older runtime the projection
 *   simply degrades to what the existing phase events carry.
 * - **Single-turn and authoritative about turn identity.** The projection tracks one HydraFusion
 *   turn at a time. Evidence that arrives before the turn resolves (a phase, activity, tool, or
 *   permission event) is adopted only while *no* turn identity has been established yet, and it
 *   promotes the projection to `"running"`. Once an identity is established — or once the turn
 *   reaches a terminal `"completed"`/`"fallback"` state — only the durable `session.fusion_resolved`
 *   event may replace it. Late ephemerals from a previous turn, and the first ephemerals of a next
 *   turn that has not resolved yet, are intentionally ignored until that resolution arrives.
 * - **Privacy-preserving.** Only event discriminants, phase kinds/roles/scopes, phase-plan
 *   metadata, safe response-byte counts, tool call IDs, permission attribution, commit IDs, and
 *   the stable terminal outcome are retained. Phase content, verdicts, prompts, reasoning,
 *   critiques, provider error messages, and concrete model identities are never read or stored.
 *
 * The reducer accepts a structurally typed event ({@link FusionProgressEventInput}) rather than the
 * generated `SessionEvent` union, so it works with generated events, hand-built events, and raw
 * JSON-RPC payloads alike, and can land ahead of the generated declarations for the newest
 * experimental fields (`session.fusion_resolved.data.phasePlan`,
 * `assistant.fusion_phase_activity`, and `fusion` attribution on permission events).
 *
 * @experimental The underlying HydraFusion event contract is experimental and may change.
 */

/** Known HydraFusion phase kinds. Unrecognized kinds from newer runtimes are preserved verbatim. */
export type FusionProgressPhaseKind =
    | "primary"
    | "judge"
    | "repair"
    | "draft"
    | "critic"
    | "revision"
    | "follow_up"
    | (string & {});

/** Conversation scope a phase executes in. Unrecognized scopes are preserved verbatim. */
export type FusionProgressScope = "root" | "review" | (string & {});

/** Orchestration pattern selected for the turn. Unrecognized patterns are preserved verbatim. */
export type FusionProgressPattern = "single" | "cascade" | "critique" | (string & {});

/** Kind of turn HydraFusion routing ran for. */
export type FusionProgressTurnKind = "user" | "compaction" | (string & {});

/** Lifecycle of the HydraFusion turn currently represented by the state. */
export type FusionProgressStatus =
    /** No HydraFusion activity has been observed. */
    | "inactive"
    /** Routing started and has not yet resolved. */
    | "routing"
    /** Routing failed; the turn runs on a deterministic concrete fallback instead. */
    | "fallback"
    /**
     * The turn's phases are executing — either because the route resolved, or because phase
     * evidence was observed before the resolution and no other turn identity existed yet.
     */
    | "running"
    /** The turn reached its aggregate outcome. */
    | "completed";

/** Observed lifecycle of a single phase. */
export type FusionProgressPhaseStatus = "running" | "succeeded" | "failed" | "cancelled";

/** Safe activity discriminant most recently observed for a phase. */
export type FusionProgressActivityKind =
    | "model_output"
    | "tool_started"
    | "tool_completed"
    | (string & {});

/** Observed lifecycle of a tool call attributed to a phase. */
export type FusionProgressToolCallStatus = "started" | "completed";

/** One entry of the expected phase plan published with the resolved route. */
export interface FusionPlannedPhase {
    /** Phase kind the plan expects to run. */
    readonly kind: FusionProgressPhaseKind;
    /** Semantic role assigned to the planned phase. Never a concrete model identity. */
    readonly role: string;
    /** Conversation scope the planned phase executes in. */
    readonly scope: FusionProgressScope;
    /** Whether the planned phase only runs when an earlier phase requires it. */
    readonly conditional: boolean;
}

/** A tool call attributed to a phase, tracked by its identifier only. */
export interface FusionProgressToolCall {
    /** Runtime-assigned tool call identifier. Carries no tool arguments or results. */
    readonly toolCallId: string;
    /** Whether the call has been observed completing. */
    readonly status: FusionProgressToolCallStatus;
}

/** Most recently observed safe activity for a phase. */
export interface FusionProgressActivity {
    /** Activity discriminant reported by the runtime. */
    readonly kind: FusionProgressActivityKind;
    /** Tool call the activity refers to, when the activity is tool-scoped. */
    readonly toolCallId?: string;
    /** Cumulative response size in bytes reported with the activity, when available. */
    readonly totalResponseSizeBytes?: number;
}

/** Observed state of one concrete phase of the turn. */
export interface FusionProgressPhase {
    /** Stable identifier of the concrete phase. */
    readonly phaseId: string;
    /** Phase kind, when any observed event carried it. */
    readonly kind?: FusionProgressPhaseKind;
    /** Semantic role assigned to the phase, when observed. Never a concrete model identity. */
    readonly role?: string;
    /** Conversation scope the phase executes in, when observed. */
    readonly scope?: FusionProgressScope;
    /** Observed lifecycle state. Terminal states are never downgraded by later events. */
    readonly status: FusionProgressPhaseStatus;
    /** Whether the phase failed and the turn degraded to another phase. */
    readonly degraded: boolean;
    /** Phase the turn degraded to after this phase failed, when reported. */
    readonly degradedToPhaseId?: string;
    /** Most recent safe activity observed for the phase. */
    readonly activity?: FusionProgressActivity;
    /** Highest cumulative response size in bytes observed for the phase. */
    readonly totalResponseSizeBytes?: number;
    /** Tool calls attributed to the phase, in first-observation order. */
    readonly toolCalls: readonly FusionProgressToolCall[];
    /** Index of the matching {@link FusionProgressState.plan} entry, when the plan is known. */
    readonly planIndex?: number;
}

/** A permission request attributed to a HydraFusion phase and still awaiting a decision. */
export interface FusionProgressPermission {
    /** Identifier used to respond to the request. */
    readonly requestId: string;
    /** Turn the request belongs to. */
    readonly fusionId?: string;
    /** Phase that raised the request, when attributed. */
    readonly phaseId?: string;
    /** Kind of the phase that raised the request, when attributed. */
    readonly phaseKind?: FusionProgressPhaseKind;
    /** Semantic role of the phase that raised the request, when attributed. */
    readonly role?: string;
    /** Conversation scope of the phase that raised the request, when attributed. */
    readonly scope?: FusionProgressScope;
}

/** Aggregate terminal outcome of the turn. */
export interface FusionProgressCompletion {
    /** Stable machine-readable aggregate outcome reported by the runtime. */
    readonly outcome: string;
    /** Whether the turn reported using a degraded route. The reason string is deliberately dropped. */
    readonly degraded: boolean;
    /** Idempotency identifier of the authoritative final commit, when reported. */
    readonly commitId?: string;
    /** Phase whose output supplied the authoritative final content, when reported. */
    readonly finalSourcePhaseId?: string;
}

/** Deterministic, presentation-neutral projection of HydraFusion progress. */
export interface FusionProgressState {
    /** Lifecycle of the turn currently represented. */
    readonly status: FusionProgressStatus;
    /** Stable identifier of the turn, once any event carries it. */
    readonly fusionId?: string;
    /** Session turn the route belongs to, when reported. */
    readonly turnId?: string;
    /** Kind of turn routing ran for, when reported. */
    readonly turnKind?: FusionProgressTurnKind;
    /** Orchestration pattern selected for the turn, when reported. */
    readonly pattern?: FusionProgressPattern;
    /** Routing policy used for the turn, when reported. */
    readonly policy?: string;
    /** Expected phase plan. Empty when the runtime does not publish one. */
    readonly plan: readonly FusionPlannedPhase[];
    /** Whether {@link FusionProgressState.plan} came from the runtime or is unavailable. */
    readonly planSource: "runtime" | "unavailable";
    /** Observed phases in first-observation order. Active phases have status `"running"`. */
    readonly phases: readonly FusionProgressPhase[];
    /** Attributed permission requests still awaiting a decision, in request order. */
    readonly pendingPermissions: readonly FusionProgressPermission[];
    /** Commit identifier observed on published authoritative output for this turn. */
    readonly publishedCommitId?: string;
    /** Aggregate terminal outcome, once the turn completes. */
    readonly completion?: FusionProgressCompletion;
}

/**
 * Minimal structural shape the reducer needs from a session event.
 *
 * Generated `SessionEvent` values satisfy this shape, as do raw decoded JSON-RPC payloads.
 */
export interface FusionProgressEventInput {
    /** Event type discriminator. */
    readonly type: string;
    /** Event payload. Read defensively; unknown or malformed payloads are ignored. */
    readonly data?: unknown;
}

const EMPTY_STATE: FusionProgressState = {
    status: "inactive",
    plan: [],
    planSource: "unavailable",
    phases: [],
    pendingPermissions: [],
};

/** Returns the empty projection used as the seed for {@link reduceFusionProgress}. */
export function initialFusionProgressState(): FusionProgressState {
    return EMPTY_STATE;
}

/**
 * Folds one session event into the HydraFusion progress projection.
 *
 * Events that are not HydraFusion-related, that carry no usable payload, or that belong to a
 * different turn are returned unchanged — by reference, so consumers can cheaply detect that
 * nothing moved — and the reducer can be applied to an entire event stream:
 *
 * ```ts
 * const state = events.reduce(reduceFusionProgress, initialFusionProgressState());
 * ```
 *
 * Turn identity is authoritative: unresolved early evidence is adopted only when no turn identity
 * exists yet, and an established or terminal projection is replaced only by a durable
 * `session.fusion_resolved`. Late ephemerals from a previous turn — and the first ephemerals of a
 * next turn — are ignored until that turn resolves.
 *
 * @experimental
 */
export function reduceFusionProgress(
    state: FusionProgressState,
    event: FusionProgressEventInput
): FusionProgressState {
    const data = asRecord(event?.data);
    switch (event?.type) {
        case "session.fusion_route_started":
            return beginTurn(state, {
                ...EMPTY_STATE,
                status: "routing",
                ...definedEntry("turnKind", optionalString(data?.turnKind)),
                ...definedEntry("policy", optionalString(data?.policy)),
            });
        case "session.fusion_route_failed":
            return beginTurn(state, {
                ...EMPTY_STATE,
                status: "fallback",
                ...definedEntry("turnKind", state.turnKind),
                ...definedEntry("policy", optionalString(data?.policy) ?? state.policy),
            });
        case "session.fusion_resolved":
            return reduceResolved(state, data);
        case "assistant.fusion_phase_started":
            return reducePhaseSignal(state, data, "running");
        case "assistant.fusion_phase_completed":
            return reducePhaseSignal(state, data, phaseStatusOf(data, "succeeded"));
        case "assistant.fusion_phase_failed":
            return reducePhaseSignal(state, data, phaseStatusOf(data, "failed"));
        case "assistant.fusion_phase_activity":
            return reduceActivity(state, data);
        case "assistant.message":
            return reducePublishedCommit(state, asRecord(data?.fusion));
        case "tool.execution_start":
            return reduceAttributedTool(state, data, "started");
        case "tool.execution_complete":
            return reduceAttributedTool(state, data, "completed");
        case "permission.requested":
            return reducePermissionRequested(state, data);
        case "permission.completed":
            return reducePermissionCompleted(state, data);
        case "session.fusion_completed":
            return reduceCompleted(state, data);
        default:
            return state;
    }
}

function reduceResolved(
    state: FusionProgressState,
    data: Record<string, unknown> | undefined
): FusionProgressState {
    const fusionId = optionalString(data?.fusionId);
    if (fusionId === undefined) {
        return state;
    }
    const base = alignTurn(state, fusionId, true);
    if (base === undefined) {
        return state;
    }
    const plan = readPhasePlan(data?.phasePlan);
    const next: FusionProgressState = {
        ...base,
        status: base.status === "completed" ? "completed" : "running",
        fusionId,
        ...definedEntry("turnId", optionalString(data?.turnId) ?? base.turnId),
        ...definedEntry("pattern", optionalString(data?.pattern) ?? base.pattern),
        ...definedEntry("policy", optionalString(data?.policy) ?? base.policy),
        plan: plan ?? base.plan,
        planSource: plan !== undefined ? "runtime" : base.planSource,
    };
    return settle(state, withPlanIndexes(next));
}

function reducePhaseSignal(
    state: FusionProgressState,
    data: Record<string, unknown> | undefined,
    status: FusionProgressPhaseStatus
): FusionProgressState {
    const phaseId = optionalString(data?.phaseId);
    if (phaseId === undefined) {
        return state;
    }
    const base = alignTurn(state, optionalString(data?.fusionId), false);
    if (base === undefined) {
        return state;
    }
    const degradedToPhaseId = optionalString(data?.degradedToPhaseId);
    return upsertPhase(base, phaseId, (phase) => ({
        ...phase,
        kind: optionalString(data?.phaseKind) ?? phase.kind,
        role: optionalString(data?.role) ?? phase.role,
        scope: optionalString(data?.conversationScope) ?? phase.scope,
        status: mergePhaseStatus(phase.status, status),
        degraded: phase.degraded || degradedToPhaseId !== undefined,
        degradedToPhaseId: degradedToPhaseId ?? phase.degradedToPhaseId,
    }));
}

function reduceActivity(
    state: FusionProgressState,
    data: Record<string, unknown> | undefined
): FusionProgressState {
    const phaseId = optionalString(data?.phaseId);
    const activityKind = optionalString(data?.activity);
    if (phaseId === undefined || activityKind === undefined) {
        return state;
    }
    const base = alignTurn(state, optionalString(data?.fusionId), false);
    if (base === undefined) {
        return state;
    }
    const toolCallId = optionalString(data?.toolCallId);
    const bytes = optionalNonNegativeInteger(data?.totalResponseSizeBytes);
    return upsertPhase(base, phaseId, (phase) => ({
        ...phase,
        kind: optionalString(data?.phaseKind) ?? phase.kind,
        role: optionalString(data?.role) ?? phase.role,
        scope: optionalString(data?.conversationScope) ?? phase.scope,
        activity: {
            kind: activityKind,
            ...(toolCallId !== undefined ? { toolCallId } : {}),
            ...(bytes !== undefined ? { totalResponseSizeBytes: bytes } : {}),
        },
        totalResponseSizeBytes: maxDefined(phase.totalResponseSizeBytes, bytes),
        toolCalls:
            toolCallId !== undefined && activityKind !== "model_output"
                ? upsertToolCall(
                      phase.toolCalls,
                      toolCallId,
                      activityKind === "tool_completed" ? "completed" : "started"
                  )
                : phase.toolCalls,
    }));
}

function reduceAttributedTool(
    state: FusionProgressState,
    data: Record<string, unknown> | undefined,
    status: FusionProgressToolCallStatus
): FusionProgressState {
    const fusion = asRecord(data?.fusion);
    const phaseId = optionalString(fusion?.phaseId);
    const toolCallId = optionalString(data?.toolCallId);
    if (phaseId === undefined || toolCallId === undefined) {
        return state;
    }
    const base = alignTurn(state, optionalString(fusion?.fusionId), false);
    if (base === undefined) {
        return state;
    }
    return upsertPhase(base, phaseId, (phase) => ({
        ...phase,
        kind: optionalString(fusion?.phaseKind) ?? phase.kind,
        role: optionalString(fusion?.role) ?? phase.role,
        scope: optionalString(fusion?.conversationScope) ?? phase.scope,
        toolCalls: upsertToolCall(phase.toolCalls, toolCallId, status),
    }));
}

function reducePublishedCommit(
    state: FusionProgressState,
    fusion: Record<string, unknown> | undefined
): FusionProgressState {
    const commitId = optionalString(fusion?.commitId);
    if (commitId === undefined) {
        return state;
    }
    const base = alignTurn(state, optionalString(fusion?.fusionId), false);
    if (base === undefined || base.publishedCommitId === commitId) {
        return base ?? state;
    }
    return { ...base, publishedCommitId: commitId };
}

function reducePermissionRequested(
    state: FusionProgressState,
    data: Record<string, unknown> | undefined
): FusionProgressState {
    const fusion = asRecord(data?.fusion);
    const requestId = optionalString(data?.requestId);
    if (fusion === undefined || requestId === undefined) {
        return state;
    }
    const base = alignTurn(state, optionalString(fusion.fusionId), false);
    if (base === undefined) {
        return state;
    }
    const pending: FusionProgressPermission = {
        requestId,
        ...definedEntry("fusionId", optionalString(fusion.fusionId)),
        ...definedEntry("phaseId", optionalString(fusion.phaseId)),
        ...definedEntry("phaseKind", optionalString(fusion.phaseKind)),
        ...definedEntry("role", optionalString(fusion.role)),
        ...definedEntry("scope", optionalString(fusion.conversationScope)),
    };
    const existingIndex = base.pendingPermissions.findIndex(
        (candidate) => candidate.requestId === requestId
    );
    if (existingIndex >= 0) {
        const existing = base.pendingPermissions[existingIndex];
        if (shallowEqual(existing, pending)) {
            return base;
        }
        const pendingPermissions = [...base.pendingPermissions];
        pendingPermissions[existingIndex] = pending;
        return { ...base, pendingPermissions };
    }
    return { ...base, pendingPermissions: [...base.pendingPermissions, pending] };
}

function reducePermissionCompleted(
    state: FusionProgressState,
    data: Record<string, unknown> | undefined
): FusionProgressState {
    const requestId = optionalString(data?.requestId);
    if (requestId === undefined) {
        return state;
    }
    const pendingPermissions = state.pendingPermissions.filter(
        (pending) => pending.requestId !== requestId
    );
    if (pendingPermissions.length === state.pendingPermissions.length) {
        return state;
    }
    return { ...state, pendingPermissions };
}

function reduceCompleted(
    state: FusionProgressState,
    data: Record<string, unknown> | undefined
): FusionProgressState {
    const outcome = optionalString(data?.outcome);
    if (outcome === undefined) {
        return state;
    }
    const base = alignTurn(state, optionalString(data?.fusionId), false);
    if (base === undefined) {
        return state;
    }
    const commitId = optionalString(data?.commitId);
    const completion: FusionProgressCompletion = {
        outcome,
        degraded: data?.degradedReason !== undefined && data?.degradedReason !== null,
        ...definedEntry("commitId", commitId),
        ...definedEntry("finalSourcePhaseId", optionalString(data?.finalSourcePhaseId)),
    };
    return settle(state, {
        ...base,
        status: "completed",
        ...definedEntry("pattern", optionalString(data?.pattern) ?? base.pattern),
        ...definedEntry("turnId", optionalString(data?.turnId) ?? base.turnId),
        completion,
    });
}

/** Preserves referential identity when an event produced no observable change. */
function settle(state: FusionProgressState, next: FusionProgressState): FusionProgressState {
    return shallowEqual(state, next) ? state : next;
}

/**
 * Applies a pre-turn routing signal.
 *
 * Routing signals carry no `fusionId`, so a duplicated one cannot be distinguished from the start
 * of another turn. A resolved turn is therefore only ever replaced by a later
 * `session.fusion_resolved`, which is durable and carries an explicit identity.
 */
function beginTurn(state: FusionProgressState, next: FusionProgressState): FusionProgressState {
    if (state.status === "running") {
        return state;
    }
    return shallowEqual(state, next) ? state : next;
}

/**
 * Reconciles the incoming turn identity with the projected one.
 *
 * Two rules keep the projection stable:
 *
 * - **Early evidence is adopted only when no turn identity exists yet.** A phase, activity, tool,
 *   or permission event that arrives before `session.fusion_resolved` establishes the turn and
 *   promotes the projection to `"running"`, because that evidence can only come from a turn that is
 *   already executing.
 * - **Only an authoritative turn-starting event replaces an established or terminal projection.**
 *   Once a turn identity is known, a mismatching `fusionId` is accepted only from the durable
 *   `session.fusion_resolved` path, and a completed or fallback projection is likewise only ever
 *   replaced there. Late ephemerals from a previous turn — and the first ephemerals of a next turn
 *   that has not resolved yet — are ignored instead of overwriting or resurrecting it.
 *
 * Returns the state to apply the event to, or `undefined` when the event must not disturb the
 * current projection.
 */
function alignTurn(
    state: FusionProgressState,
    fusionId: string | undefined,
    startsTurn: boolean
): FusionProgressState | undefined {
    if (fusionId === undefined) {
        return state;
    }
    if (state.fusionId === undefined) {
        if (startsTurn) {
            return { ...state, fusionId };
        }
        if (isTerminalStatus(state.status)) {
            return undefined;
        }
        // Evidence of an executing phase implies the turn resolved, even if that event was missed.
        return { ...state, fusionId, status: "running" };
    }
    if (state.fusionId === fusionId) {
        return state;
    }
    return startsTurn ? { ...EMPTY_STATE, fusionId } : undefined;
}

/** Terminal projections are never resurrected by anything but an authoritative new route. */
function isTerminalStatus(status: FusionProgressStatus): boolean {
    return status === "completed" || status === "fallback";
}

function upsertPhase(
    state: FusionProgressState,
    phaseId: string,
    update: (phase: FusionProgressPhase) => FusionProgressPhase
): FusionProgressState {
    const index = state.phases.findIndex((phase) => phase.phaseId === phaseId);
    const existing =
        index >= 0
            ? state.phases[index]
            : {
                  phaseId,
                  status: "running" as FusionProgressPhaseStatus,
                  degraded: false,
                  toolCalls: [],
              };
    const updated = compact(update(existing));
    if (index >= 0 && shallowEqual(existing, updated)) {
        return state;
    }
    const phases = index >= 0 ? [...state.phases] : [...state.phases, updated];
    if (index >= 0) {
        phases[index] = updated;
    }
    return withPlanIndexes({ ...state, phases });
}

/** Drops explicitly-undefined keys so equality checks and serialization stay stable. */
function compact<T extends object>(value: T): T {
    const entries = Object.entries(value).filter(([, entry]) => entry !== undefined);
    return Object.fromEntries(entries) as T;
}

function upsertToolCall(
    toolCalls: readonly FusionProgressToolCall[],
    toolCallId: string,
    status: FusionProgressToolCallStatus
): readonly FusionProgressToolCall[] {
    const index = toolCalls.findIndex((call) => call.toolCallId === toolCallId);
    if (index < 0) {
        return [...toolCalls, { toolCallId, status }];
    }
    // "completed" is terminal: a duplicated or out-of-order "started" never regresses it.
    if (toolCalls[index].status === "completed" || status === "started") {
        return toolCalls;
    }
    const next = [...toolCalls];
    next[index] = { toolCallId, status };
    return next;
}

/**
 * Matches observed phases against the published plan, in order, consuming each plan entry once.
 *
 * Matching prefers an entry with the same kind *and* scope, then falls back to kind alone so a
 * runtime that reports a scope the plan did not anticipate still yields a usable projection.
 */
function withPlanIndexes(state: FusionProgressState): FusionProgressState {
    if (state.plan.length === 0) {
        return state.phases.some((phase) => phase.planIndex !== undefined)
            ? { ...state, phases: state.phases.map(({ planIndex: _planIndex, ...rest }) => rest) }
            : state;
    }
    const used = new Set<number>();
    const matched = new Map<string, number>();
    for (const phase of state.phases) {
        const index = state.plan.findIndex(
            (planned, candidate) =>
                !used.has(candidate) && planned.kind === phase.kind && planned.scope === phase.scope
        );
        if (index >= 0) {
            used.add(index);
            matched.set(phase.phaseId, index);
        }
    }
    for (const phase of state.phases) {
        if (matched.has(phase.phaseId)) {
            continue;
        }
        const index = state.plan.findIndex(
            (planned, candidate) => !used.has(candidate) && planned.kind === phase.kind
        );
        if (index >= 0) {
            used.add(index);
            matched.set(phase.phaseId, index);
        }
    }
    let changed = false;
    const phases = state.phases.map((phase) => {
        const planIndex = matched.get(phase.phaseId);
        if (planIndex === undefined) {
            if (phase.planIndex === undefined) {
                return phase;
            }
            changed = true;
            const { planIndex: _planIndex, ...rest } = phase;
            return rest;
        }
        if (phase.planIndex === planIndex) {
            return phase;
        }
        changed = true;
        return { ...phase, planIndex };
    });
    return changed ? { ...state, phases } : state;
}

function readPhasePlan(value: unknown): readonly FusionPlannedPhase[] | undefined {
    if (!Array.isArray(value)) {
        return undefined;
    }
    const plan: FusionPlannedPhase[] = [];
    for (const entry of value) {
        const record = asRecord(entry);
        const kind = optionalString(record?.kind);
        if (kind === undefined) {
            continue;
        }
        plan.push({
            kind,
            role: optionalString(record?.role) ?? "",
            scope: optionalString(record?.scope) ?? "root",
            conditional: record?.conditional === true,
        });
    }
    return plan;
}

function phaseStatusOf(
    data: Record<string, unknown> | undefined,
    fallback: FusionProgressPhaseStatus
): FusionProgressPhaseStatus {
    const status = optionalString(data?.status);
    return status === "succeeded" || status === "failed" || status === "cancelled"
        ? status
        : fallback;
}

/** Terminal phase states are sticky, so duplicated or late "started" events cannot regress them. */
function mergePhaseStatus(
    current: FusionProgressPhaseStatus,
    incoming: FusionProgressPhaseStatus
): FusionProgressPhaseStatus {
    if (incoming === "running") {
        return current;
    }
    return current === "running" ? incoming : current;
}

function asRecord(value: unknown): Record<string, unknown> | undefined {
    return typeof value === "object" && value !== null && !Array.isArray(value)
        ? (value as Record<string, unknown>)
        : undefined;
}

function optionalString(value: unknown): string | undefined {
    return typeof value === "string" && value.length > 0 ? value : undefined;
}

function optionalNonNegativeInteger(value: unknown): number | undefined {
    return typeof value === "number" && Number.isFinite(value) && value >= 0 ? value : undefined;
}

function maxDefined(current: number | undefined, incoming: number | undefined): number | undefined {
    if (incoming === undefined) {
        return current;
    }
    return current === undefined ? incoming : Math.max(current, incoming);
}

function definedEntry<K extends string, V>(key: K, value: V | undefined): Record<K, V> | undefined {
    return value === undefined ? undefined : ({ [key]: value } as Record<K, V>);
}

function shallowEqual(left: object, right: object): boolean {
    const leftRecord = left as Record<string, unknown>;
    const rightRecord = right as Record<string, unknown>;
    const leftKeys = Object.keys(leftRecord);
    if (leftKeys.length !== Object.keys(rightRecord).length) {
        return false;
    }
    return leftKeys.every((key) => valueEqual(leftRecord[key], rightRecord[key]));
}

function valueEqual(left: unknown, right: unknown): boolean {
    if (Array.isArray(left) && Array.isArray(right)) {
        return left.length === right.length && left.every((item, i) => valueEqual(item, right[i]));
    }
    const leftRecord = asRecord(left);
    const rightRecord = asRecord(right);
    if (leftRecord !== undefined && rightRecord !== undefined) {
        return shallowEqual(leftRecord, rightRecord);
    }
    return left === right;
}

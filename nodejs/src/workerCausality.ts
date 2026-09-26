/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

function object(value: unknown): value is Record<string, unknown> {
    return typeof value === "object" && value !== null && !Array.isArray(value);
}

function fields(value: Record<string, unknown>, allowed: readonly string[]): boolean {
    return Object.keys(value).every((key) => allowed.includes(key));
}

function text(value: unknown): value is string {
    return typeof value === "string" && value.length > 0 && Buffer.byteLength(value, "utf8") <= 256;
}

function small(value: unknown, budget: { remaining: number }, depth = 0): boolean {
    budget.remaining--;
    if (budget.remaining < 0 || depth > 32) return false;
    if (typeof value === "string") budget.remaining -= value.length;
    if (object(value)) {
        for (const [key, item] of Object.entries(value)) {
            if (!small(key, budget, depth + 1) || !small(item, budget, depth + 1)) return false;
        }
    } else if (Array.isArray(value)) {
        for (const item of value) if (!small(item, budget, depth + 1)) return false;
    }
    return budget.remaining >= 0;
}

function reference(value: unknown, type: string): boolean {
    return (
        object(value) &&
        fields(value, ["sessionId", "eventId", "agentId", "eventType", "provenance"]) &&
        text(value.sessionId) &&
        /^[A-Za-z0-9_-]+$/.test(value.sessionId) &&
        typeof value.eventId === "string" &&
        /^[\da-f]{8}-[\da-f]{4}-[\da-f]{4}-[\da-f]{4}-[\da-f]{12}$/i.test(value.eventId) &&
        (!("agentId" in value) || text(value.agentId)) &&
        value.eventType === type &&
        (value.provenance === "native" || value.provenance === "ahp_coordinator")
    );
}

function source(value: unknown): boolean {
    if (!object(value) || !object(value.input)) return false;
    const input = value.input;
    return (
        fields(value, [
            "input",
            "admissions",
            "captureComplete",
            "completion",
            "notification",
            "admittedDuring",
        ]) &&
        fields(input, ["queueItemId", "agentId", "sender", "senderBridges"]) &&
        typeof value.captureComplete === "boolean" &&
        typeof input.queueItemId === "string" &&
        /^[\da-f]{8}-[\da-f]{4}-[\da-f]{4}-[\da-f]{4}-[\da-f]{12}$/i.test(input.queueItemId) &&
        text(input.agentId) &&
        (!("sender" in input) || reference(input.sender, "tool.execution_start")) &&
        (!("senderBridges" in input) ||
            ("sender" in input &&
                Array.isArray(input.senderBridges) &&
                input.senderBridges.length <= 32 &&
                input.senderBridges.every(
                    (edge: unknown) =>
                        object(edge) &&
                        fields(edge, ["source", "reported"]) &&
                        reference(edge.source, "tool.execution_start") &&
                        reference(edge.reported, "tool.execution_start")
                ))) &&
        Array.isArray(value.admissions) &&
        value.admissions.length <= 32 &&
        value.admissions.every(
            (item: unknown) =>
                object(item) &&
                fields(item, ["kind", "messageId", "event", "ahpTurnId"]) &&
                (item.kind === "queued_input" || item.kind === "system_continuation") &&
                text(item.messageId) &&
                (!("event" in item) ||
                    (reference(item.event, "user.message") &&
                        (item.event as Record<string, unknown>).agentId === input.agentId)) &&
                (!("ahpTurnId" in item) ||
                    (typeof item.ahpTurnId === "string" &&
                        /^[\da-f]{8}-[\da-f]{4}-[\da-f]{4}-[\da-f]{4}-[\da-f]{12}$/i.test(
                            item.ahpTurnId
                        )))
        ) &&
        (!("completion" in value) || reference(value.completion, "subagent.completed")) &&
        (!("admittedDuring" in value) || reference(value.admittedDuring, "assistant.turn_start")) &&
        (!("notification" in value) ||
            (object(value.notification) &&
                fields(value.notification, ["deliveryId", "event", "mode"]) &&
                typeof value.notification.deliveryId === "string" &&
                /^[\da-f]{8}-[\da-f]{4}-[\da-f]{4}-[\da-f]{4}-[\da-f]{12}$/i.test(
                    value.notification.deliveryId
                ) &&
                (value.notification.mode === "queued" || value.notification.mode === "immediate") &&
                (!("event" in value.notification) ||
                    reference(value.notification.event, "system.notification"))))
    );
}

/** Decode availability only; consumers must still validate placement and enclosing self identity. */
export function withWorkerCausality<T extends object>(data: T): T {
    if (!("workerCausality" in data)) return data;
    const value = data.workerCausality;
    const valid =
        object(value) &&
        fields(value, ["version", "observationProvenance", "sources", "captureComplete"]) &&
        value.version === 1 &&
        (value.observationProvenance === "native" ||
            value.observationProvenance === "ahp_coordinator") &&
        typeof value.captureComplete === "boolean" &&
        Array.isArray(value.sources) &&
        value.sources.length <= 32 &&
        small(value, { remaining: 4096 }) &&
        value.sources.every(
            (item: unknown) =>
                source(item) &&
                (!value.captureComplete ||
                    (item as Record<string, unknown>).captureComplete === true)
        ) &&
        Buffer.byteLength(JSON.stringify({ workerCausality: value }), "utf8") <= 4096;
    if (valid) return data;
    console.warn("Ignoring invalid, unsupported or oversized workerCausality metadata");
    const copy = { ...data };
    delete (copy as { workerCausality?: unknown }).workerCausality;
    return copy;
}

/** Apply the same diagnostic tolerance to historical events as live delivery. */
export function withWorkerCausalityEvents<T extends { events: { data: object }[] }>(result: T): T {
    const events = result.events.map((event) => {
        const data = withWorkerCausality(event.data);
        return data === event.data ? event : { ...event, data };
    });
    return events.every((event, index) => event === result.events[index])
        ? result
        : { ...result, events };
}

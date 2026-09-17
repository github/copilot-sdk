/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { styleText } from "node:util";

export interface EventDisplayTiming {
    receivedAt: string;
    elapsedMs: number;
    sequence: number;
    color?: boolean;
}

function isRecord(value: unknown): value is Record<string, unknown> {
    return value !== null && typeof value === "object" && !Array.isArray(value);
}

function record(value: unknown): Record<string, unknown> {
    return isRecord(value) ? value : {};
}

function text(value: unknown): string | undefined {
    return typeof value === "string" ? value : undefined;
}

/** Keep summaries on one line and prevent payloads from controlling the terminal. */
function oneLine(value: string): string {
    return value
        .replace(
            /[\u0000-\u0008\u000b\u000c\u000e-\u001f\u007f-\u009f]/g,
            (character) => `\\u${character.charCodeAt(0).toString(16).padStart(4, "0")}`
        )
        .replace(/\s+/g, " ")
        .trim();
}

function preview(value: string): string {
    const escaped = oneLine(value);
    return escaped.length > 180 ? `${escaped.slice(0, 177)}...` : escaped;
}

function humanize(value: string): string {
    const words = value.replace(/([a-z0-9])([A-Z])/g, "$1 $2").replace(/[._-]+/g, " ");
    return preview(words.charAt(0).toUpperCase() + words.slice(1));
}

const TURN_EVENTS = new Set([
    "assistant.turn_start",
    "assistant.turn_end",
    "model.turn_started",
    "model.turn_ended",
    "session.idle",
    "assistant.idle",
]);

const FUSION_FIELDS = [
    ["pattern", "workflow"],
    ["policy", "policy"],
    ["phaseKind", "phase"],
    ["phase_kind", "phase"],
    ["model", "model"],
    ["primaryModel", "primary"],
    ["primary_model", "primary"],
    ["secondaryModel", "secondary"],
    ["secondary_model", "secondary"],
    ["fallbackModel", "fallback"],
    ["fallback_model", "fallback"],
    ["followUpModel", "follow-up"],
    ["follow_up_model", "follow-up"],
    ["sourceModel", "source"],
    ["source_model", "source"],
    ["finalSourceModel", "final model"],
    ["final_source_model", "final model"],
    ["status", "status"],
    ["outcome", "outcome"],
    ["totalResponseSizeBytes", "bytes"],
    ["durationMs", "duration ms"],
    ["duration_ms", "duration ms"],
    ["routingLatencyMs", "routing ms"],
    ["routing_latency_ms", "routing ms"],
    ["phaseCount", "phases"],
    ["phase_count", "phases"],
    ["requestCount", "requests"],
    ["request_count", "requests"],
    ["degradedReason", "degraded"],
    ["degraded_reason", "degraded"],
] as const;

/** Stateful display only: the JSONL writer must receive every event before this filter. */
export function createChatEventFormatter() {
    const streamedMessages = new Set<string>();
    const streamedCalls = new Set<string>();
    const streamedPhases = new Map<string, Set<string>>();
    const toolNames = new Map<string, string>();

    return (source: string, event: unknown, timing: EventDisplayTiming): string | undefined => {
        const envelope = record(event);
        const telemetry = source === "sdk.telemetry";
        const payload = telemetry ? record(envelope.event) : envelope;
        const type = text(payload.type) ?? text(payload.kind) ?? "event";
        const data = telemetry
            ? { ...record(payload.properties), ...record(payload.metrics) }
            : record(payload.data);
        const fusion = record(data.fusion);
        const fusionRelated =
            /fusion/i.test(type) ||
            Object.keys(fusion).length > 0 ||
            typeof data.fusionId === "string" ||
            typeof data.fusion_id === "string";
        const tool =
            type.startsWith("tool.execution_") ||
            type === "assistant.tool_call_delta" ||
            type === "model.tool_execution" ||
            type === "tool_call_executed";
        const message =
            /^(assistant|user|system|model)\.(message(?:_|$)|reasoning(?:_|$)|streaming_delta$)/.test(
                type
            ) || /^(assistant|user|system)_(message|reasoning)$/.test(type);
        const warning = source === "chat.diagnostic" || type === "session.error";
        const turn = TURN_EVENTS.has(type);
        const agent = text(envelope.agentId) ?? "root";
        const fusionId = text(data.fusionId) ?? text(fusion.fusionId);
        const phaseId = text(data.phaseId) ?? text(fusion.phaseId);
        const messageId = text(data.messageId) ?? text(data.reasoningId);
        const channel = type.includes("reasoning") ? "reasoning" : "message";
        const messageKey = messageId ? JSON.stringify([agent, channel, messageId]) : undefined;
        const delta = type.endsWith("_delta");
        const assistantReply =
            type === "assistant.message" ||
            type === "assistant.reasoning" ||
            (type === "model.message" && record(data.message).role === "assistant");

        if (
            type === "model.call_start" ||
            type === "model.model_call_started" ||
            type === "assistant.turn_end" ||
            type === "session.idle"
        ) {
            streamedCalls.delete(agent);
        }
        if (type === "assistant.streaming_delta") streamedCalls.add(agent);
        if (delta && messageKey) streamedMessages.add(messageKey);
        if (
            type === "assistant.fusion_phase_activity" &&
            data.activity === "model_output" &&
            fusionId &&
            phaseId
        ) {
            const phases = streamedPhases.get(fusionId) ?? new Set<string>();
            phases.add(phaseId);
            streamedPhases.set(fusionId, phases);
        }
        const phaseStreamed = Boolean(
            fusionId && phaseId && streamedPhases.get(fusionId)?.has(phaseId)
        );
        const streamed =
            delta ||
            (assistantReply &&
                ((messageKey !== undefined && streamedMessages.has(messageKey)) ||
                    streamedCalls.has(agent)));
        if (assistantReply && messageKey) {
            streamedMessages.delete(messageKey);
        }
        if (type === "session.fusion_completed" && fusionId) streamedPhases.delete(fusionId);
        if (!(fusionRelated || tool || message || warning || turn)) return undefined;

        let title = humanize(type.replace(/^hydrafusion_/, "fusion_"));
        let depth = 1;
        const details: string[] = [];
        if (turn) {
            title =
                type === "assistant.turn_start"
                    ? "Turn started"
                    : type === "assistant.turn_end"
                      ? "Turn finished"
                      : humanize(type);
            depth = 0;
        }
        if (fusionRelated) {
            title = humanize(
                type.replace(/^(session|assistant)\./, "").replace(/^hydrafusion_/, "fusion_")
            );
            if (type === "session.fusion_resolved") title = "Fusion workflow selected";
            if (type === "session.fusion_route_started") title = "Fusion routing started";
            if (type === "session.fusion_completed") title = "Fusion turn completed";
            if (type.includes("fusion_phase")) depth = 2;
            if (type === "assistant.fusion_phase_activity") {
                depth = 3;
                title = `Fusion progress: ${humanize(text(data.activity) ?? "activity").toLowerCase()}`;
            }
            for (const [key, label] of FUSION_FIELDS) {
                const value = data[key] ?? fusion[key];
                if (typeof value === "string" || typeof value === "number") {
                    details.push(`${label}: ${preview(String(value))}`);
                }
            }
            if (Array.isArray(data.phasePlan)) {
                const plan = data.phasePlan
                    .map((step) => {
                        const phase = record(step);
                        return `${text(phase.kind) ?? "phase"}${phase.conditional ? " (optional)" : ""}`;
                    })
                    .join(" -> ");
                details.push(preview(plan));
            }
            if (telemetry) title += " telemetry";
        }
        if (tool) {
            depth = fusionRelated ? 3 : 2;
            const toolId = text(data.toolCallId) ?? text(data.tool_call_id);
            const toolKey = toolId ? JSON.stringify([agent, toolId]) : undefined;
            const name =
                text(data.toolName) ??
                text(data.tool_name) ??
                (toolKey ? toolNames.get(toolKey) : undefined);
            if (name && toolKey) toolNames.set(toolKey, name);
            title =
                type === "tool.execution_start"
                    ? "Tool invoked"
                    : type === "tool.execution_complete"
                      ? "Tool completed"
                      : type === "tool.execution_partial_result"
                        ? "Tool output"
                        : type === "assistant.tool_call_delta"
                          ? "Tool input delta"
                          : "Tool execution";
            if (name) title += `: ${preview(name)}`;
            if (typeof data.success === "boolean")
                details.push(data.success ? "succeeded" : "failed");
            if (type === "tool.execution_complete" && toolKey) toolNames.delete(toolKey);
            if (telemetry) title += " telemetry";
        }
        if (message) {
            depth = fusionRelated ? 3 : type === "user.message" ? 0 : 1;
            const nestedMessage = record(data.message);
            const role = text(nestedMessage.role);
            title =
                type === "assistant.message"
                    ? "Assistant"
                    : type === "user.message"
                      ? "You"
                      : type === "system.message"
                        ? "System message"
                        : type === "assistant.message_delta"
                          ? "Assistant message delta"
                          : type === "assistant.reasoning"
                            ? "Assistant reasoning"
                            : type === "assistant.reasoning_delta"
                              ? "Assistant reasoning delta"
                              : type === "assistant.streaming_delta"
                                ? "Assistant streaming progress"
                                : type === "model.message"
                                  ? `${humanize(role ?? "model")} message`
                                  : humanize(type);
            if (telemetry) title += " telemetry";
            const content =
                text(data.deltaContent) ?? text(data.content) ?? text(nestedMessage.content);
            if (content?.trim()) title += `: ${preview(content)}`;
            else if (Array.isArray(data.messages)) details.push(`${data.messages.length} messages`);
            else if (Array.isArray(nestedMessage.content))
                details.push(`${nestedMessage.content.length} content blocks`);
            if (Array.isArray(data.toolRequests) && data.toolRequests.length) {
                details.push(`${data.toolRequests.length} tool request(s)`);
            }
            if (!fusionRelated && typeof data.totalResponseSizeBytes === "number") {
                details.push(`${data.totalResponseSizeBytes} bytes`);
            }
        }
        if (warning) {
            depth = 0;
            title = "Warning";
            const warningText = text(envelope.message) ?? text(data.message);
            if (warningText) title += `: ${oneLine(warningText)}`;
        }
        if (agent !== "root") details.push(`agent: ${preview(agent)}`);
        if (fusionRelated && !/fusion/i.test(title)) title += " [Fusion]";
        if ((assistantReply || type === "assistant.fusion_phase_completed") && phaseStreamed) {
            title += " (*streaming*) [staged phase]";
        } else if (streamed) {
            title += " (*streaming*)";
        }

        const clock = `[${timing.receivedAt.slice(11, 23)}Z +${(timing.elapsedMs / 1000).toFixed(3)}s #${timing.sequence}]`;
        const summary = `${"  ".repeat(depth)}${title}${details.length ? ` | ${details.join(" | ")}` : ""}`;
        const color = warning ? "yellow" : fusionRelated ? "cyan" : tool ? "magenta" : "blue";
        return (
            `${timing.color ? styleText("dim", clock, { validateStream: false }) : clock} ` +
            `${timing.color ? styleText(color, summary, { validateStream: false }) : summary}\n`
        );
    };
}

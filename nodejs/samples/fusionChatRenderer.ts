/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { styleText } from "node:util";
import { clearLine, cursorTo, moveCursor } from "node:readline";

interface RendererOptions {
    color?: boolean;
    debug?: boolean;
    interactive?: boolean;
}

interface OpenStream {
    kind: "assistant" | "reasoning";
    id: string;
    content: string;
}

interface PhaseInfo {
    index: number;
    kind: string;
    role?: string;
    scope?: string;
    conditional?: boolean;
    model?: string;
}

interface ToolDisplay {
    description: string;
    depth: number;
    lineIndex: number;
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

function clean(value: string): string {
    return value.replace(
        /[\u0000-\u0008\u000b\u000c\u000e-\u001f\u007f-\u009f]/g,
        (character) => `\\u${character.charCodeAt(0).toString(16).padStart(4, "0")}`
    );
}

function compact(value: string, limit = 320): string {
    const normalized = clean(value).replace(/\s+/g, " ").trim();
    return normalized.length > limit ? `${normalized.slice(0, limit - 3)}...` : normalized;
}

function title(value: string): string {
    const words = value.replace(/([a-z0-9])([A-Z])/g, "$1 $2").replace(/[._-]+/g, " ");
    return words.charAt(0).toUpperCase() + words.slice(1);
}

function seconds(milliseconds: unknown): string | undefined {
    return typeof milliseconds === "number" ? `${(milliseconds / 1000).toFixed(2)}s` : undefined;
}

function formatAiCredits(nanoAiu: number): string {
    return new Intl.NumberFormat("en-US", { maximumFractionDigits: 2 }).format(
        nanoAiu / 1_000_000_000
    );
}

function fusionData(event: Record<string, unknown>): Record<string, unknown> {
    const data = record(event.data);
    return isRecord(data.fusion) ? data.fusion : {};
}

function phaseLabel(kind: string): string {
    return title(kind);
}

export class FusionChatRenderer {
    private readonly color: boolean;
    private readonly phaseById = new Map<string, PhaseInfo>();
    private readonly plannedPhases: PhaseInfo[] = [];
    private readonly shownMessages = new Set<string>();
    private readonly shownToolStarts = new Set<string>();
    private readonly shownToolCompletions = new Set<string>();
    private readonly toolDescriptions = new Map<string, string>();
    private readonly toolDisplays = new Map<string, ToolDisplay>();
    private readonly debug: boolean;
    private readonly interactive: boolean;
    private renderedLineCount = 0;
    private openStream: OpenStream | undefined;

    constructor(
        private readonly output: NodeJS.WritableStream,
        options: RendererOptions = {}
    ) {
        this.color = options.color ?? false;
        this.debug = options.debug ?? false;
        this.interactive = options.interactive ?? false;
    }

    handle(event: unknown): void {
        const envelope = record(event);
        const eventType = text(envelope.type);
        const data = record(envelope.data);
        switch (eventType) {
            case "session.fusion_route_started":
                this.closeStream();
                this.line(1, "Selecting the Fusion workflow...", "cyan");
                break;
            case "session.fusion_resolved":
                this.renderResolved(data);
                break;
            case "assistant.fusion_phase_started":
                this.renderPhaseStarted(data);
                break;
            case "assistant.fusion_phase_activity":
                this.renderPhaseActivity(data);
                break;
            case "assistant.fusion_phase_completed":
                this.renderPhaseCompleted(data);
                break;
            case "session.fusion_commit_started":
                this.renderCommit(data);
                break;
            case "session.fusion_completed":
                this.renderCompleted(data);
                break;
            case "session.shutdown":
                this.renderShutdown(data);
                break;
            case "session.fusion_route_failed":
            case "assistant.fusion_phase_failed":
                this.closeStream();
                this.line(2, `${title(eventType)}: ${compact(JSON.stringify(data))}`, "red");
                break;
            case "assistant.message_delta":
                this.renderDelta("assistant", data);
                break;
            case "assistant.reasoning_delta":
                this.renderDelta("reasoning", data);
                break;
            case "assistant.message":
                this.renderAssistantMessage(envelope);
                break;
            case "assistant.reasoning":
                this.renderReasoning(envelope);
                break;
            case "tool.execution_start":
                this.renderToolStart(envelope);
                break;
            case "tool.execution_partial_result":
                break;
            case "tool.execution_complete":
                this.renderToolComplete(envelope);
                break;
            case "session.error": {
                this.closeStream();
                this.line(
                    0,
                    `Error: ${compact(String(data.message ?? "Unknown session error"))}`,
                    "red"
                );
                break;
            }
        }
    }

    finish(): void {
        this.closeStream();
    }

    private renderResolved(data: Record<string, unknown>): void {
        this.closeStream();
        this.plannedPhases.length = 0;
        this.phaseById.clear();
        const pattern = typeof data.pattern === "string" ? data.pattern : "unknown";
        this.line(1, `Workflow selected: ${title(pattern)}`, "cyan");

        if (!Array.isArray(data.phasePlan)) return;
        data.phasePlan.forEach((raw, index) => {
            const phase = isRecord(raw) ? raw : {};
            const info: PhaseInfo = {
                index,
                kind: typeof phase.kind === "string" ? phase.kind : `phase ${index + 1}`,
                role: typeof phase.role === "string" ? phase.role : undefined,
                scope: typeof phase.scope === "string" ? phase.scope : undefined,
                conditional: phase.conditional === true,
            };
            this.plannedPhases.push(info);
            if (!this.debug) return;
            const details = [info.role, info.scope].filter(Boolean).join(", ");
            this.line(
                1,
                `  [${index + 1}] ${phaseLabel(info.kind)}${info.conditional ? " (optional)" : ""}${
                    details ? ` - ${details}` : ""
                }`,
                "dim"
            );
        });
    }

    private renderPhaseStarted(data: Record<string, unknown>): void {
        this.closeStream();
        const phaseId = String(data.phaseId ?? "");
        const kind = String(data.phaseKind ?? "phase");
        let phase = this.plannedPhases.find(
            (candidate) =>
                candidate.kind === kind && ![...this.phaseById.values()].includes(candidate)
        );
        phase ??= { index: this.phaseById.size, kind };
        phase.model = typeof data.model === "string" ? data.model : phase.model;
        if (phaseId) this.phaseById.set(phaseId, phase);
        const details = this.debug
            ? [
                  `role: ${String(data.role ?? phase.role ?? "unknown")}`,
                  `scope: ${String(data.conversationScope ?? phase.scope ?? "unknown")}`,
                  phase.model ? `model: ${phase.model}` : undefined,
              ]
                  .filter(Boolean)
                  .join(", ")
            : "";
        this.line(2, `+-- ${phaseLabel(kind)} started${details ? ` (${details})` : ""}`, "blue");
    }

    private renderPhaseActivity(data: Record<string, unknown>): void {
        void data;
    }

    private renderPhaseCompleted(data: Record<string, unknown>): void {
        this.closeStream();
        const phase = this.getPhase(data);
        const status = String(data.status ?? "completed");
        const duration = seconds(data.durationMs);
        const usage = isRecord(data.usage) ? data.usage : {};
        const details = [
            typeof data.model === "string" ? `model: ${data.model}` : undefined,
            duration,
            typeof usage.requestCount === "number"
                ? `${usage.requestCount} ${usage.requestCount === 1 ? "request" : "requests"}`
                : undefined,
        ]
            .filter(Boolean)
            .join(", ");
        if (this.debug) {
            this.line(
                2,
                `\`-- ${phaseLabel(phase.kind)} ${status}${details ? ` (${details})` : ""}`,
                status === "succeeded" ? "green" : "yellow"
            );
        }
        if (
            data.projectionMode === "none" &&
            typeof data.content === "string" &&
            data.content.trim()
        ) {
            this.line(3, `Review: ${compact(data.content)}`, "dim");
        }
        if (typeof data.verdict === "string" && data.verdict.trim()) {
            this.line(3, `Verdict: ${compact(data.verdict)}`, "dim");
        }
    }

    private renderCommit(data: Record<string, unknown>): void {
        if (!this.debug) return;
        this.closeStream();
        const phase = this.getPhase({ phaseId: data.sourcePhaseId });
        const model = typeof data.sourceModel === "string" ? ` from ${data.sourceModel}` : "";
        this.line(1, `Committing ${phaseLabel(phase.kind)}${model}...`, "cyan");
    }

    private renderCompleted(data: Record<string, unknown>): void {
        this.closeStream();
        if (!this.debug) return;
        const details = [
            `source: ${String(data.finalSourceModel ?? "unknown")}`,
            typeof data.durationMs === "number"
                ? `duration: ${seconds(data.durationMs)}`
                : undefined,
            typeof data.requestCount === "number"
                ? `${data.requestCount} ${data.requestCount === 1 ? "request" : "requests"}`
                : undefined,
            data.degradedReason ? `degraded: ${compact(String(data.degradedReason))}` : undefined,
        ]
            .filter(Boolean)
            .join(", ");
        this.line(
            1,
            `Fusion complete: ${title(String(data.pattern ?? "unknown"))} (${details})`,
            data.outcome === "completed" ? "green" : "yellow"
        );
    }

    private renderShutdown(data: Record<string, unknown>): void {
        this.closeStream();
        const modelMetrics = record(data.modelMetrics);
        const perModel = Object.entries(modelMetrics)
            .map(([model, value]) => {
                const totalNanoAiu = record(value).totalNanoAiu;
                return typeof totalNanoAiu === "number" && Number.isFinite(totalNanoAiu)
                    ? { model, totalNanoAiu }
                    : undefined;
            })
            .filter(
                (entry): entry is { model: string; totalNanoAiu: number } => entry !== undefined
            )
            .sort((left, right) => right.totalNanoAiu - left.totalNanoAiu);
        const totalNanoAiu =
            typeof data.totalNanoAiu === "number" && Number.isFinite(data.totalNanoAiu)
                ? data.totalNanoAiu
                : perModel.reduce((sum, entry) => sum + entry.totalNanoAiu, 0);
        const breakdown =
            perModel.length > 0
                ? perModel
                      .map(
                          ({ model, totalNanoAiu: modelNanoAiu }) =>
                              `${clean(model)}: ${formatAiCredits(modelNanoAiu)}`
                      )
                      .join("; ")
                : "no per-model breakdown";
        this.line(
            0,
            `Session ending - ${formatAiCredits(totalNanoAiu)} AI Credits used (${breakdown})`,
            "dim"
        );
    }

    private renderDelta(kind: OpenStream["kind"], data: Record<string, unknown>): void {
        const id =
            kind === "assistant" ? String(data.messageId ?? "") : String(data.reasoningId ?? "");
        const delta = typeof data.deltaContent === "string" ? clean(data.deltaContent) : "";
        if (!id || !delta) return;
        if (!this.openStream || this.openStream.kind !== kind || this.openStream.id !== id) {
            this.closeStream();
            this.output.write(
                `${this.indent(3)}${kind === "assistant" ? "Assistant" : "Thinking"} (*streaming*): `
            );
            this.openStream = { kind, id, content: "" };
        }
        this.output.write(delta);
        this.openStream.content += delta;
    }

    private renderAssistantMessage(event: Record<string, unknown>): void {
        const data = record(event.data);
        const messageId = typeof data.messageId === "string" ? data.messageId : undefined;
        if (messageId && this.shownMessages.has(messageId)) return;
        if (messageId && this.closeMatchingStream("assistant", messageId, data.content)) {
            this.shownMessages.add(messageId);
            return;
        }
        this.closeStream();
        if (typeof data.reasoningText === "string" && data.reasoningText.trim()) {
            this.line(this.eventDepth(event), `Thinking: ${compact(data.reasoningText)}`, "dim");
        }
        if (typeof data.content === "string" && data.content.trim()) {
            this.line(this.eventDepth(event), `Assistant: ${compact(data.content, 800)}`, "green");
        }
        if (messageId) this.shownMessages.add(messageId);
    }

    private renderReasoning(event: Record<string, unknown>): void {
        const data = record(event.data);
        const reasoningId = typeof data.reasoningId === "string" ? data.reasoningId : undefined;
        if (reasoningId && this.closeMatchingStream("reasoning", reasoningId, data.content)) return;
        this.closeStream();
        if (typeof data.content === "string" && data.content.trim()) {
            this.line(this.eventDepth(event), `Thinking: ${compact(data.content, 800)}`, "dim");
        }
    }

    private renderToolStart(event: Record<string, unknown>): void {
        const data = record(event.data);
        const toolCallId = typeof data.toolCallId === "string" ? data.toolCallId : undefined;
        if (!toolCallId || this.shownToolStarts.has(toolCallId)) return;
        this.shownToolStarts.add(toolCallId);
        this.closeStream();
        const toolName = typeof data.toolName === "string" ? data.toolName : "";
        const argumentsData = record(data.arguments);
        const isWebSearch =
            toolName === "web_search" ||
            toolName.endsWith("-web_search") ||
            data.mcpToolName === "web_search";
        const query =
            isWebSearch && typeof argumentsData.query === "string" && argumentsData.query.trim()
                ? compact(argumentsData.query, 220)
                : undefined;
        const description = query
            ? `Searching the web: "${query}"`
            : typeof argumentsData.description === "string" && argumentsData.description.trim()
              ? compact(argumentsData.description, 220)
              : typeof data.description === "string" && data.description.trim()
                ? compact(data.description, 220)
                : undefined;
        if (description) this.toolDescriptions.set(toolCallId, description);
        const depth = this.eventDepth(event);
        const renderedDescription = description ?? "Tool invoked";
        const lineIndex = this.line(depth, `|-- ${renderedDescription}`, "magenta");
        this.toolDisplays.set(toolCallId, {
            description: renderedDescription,
            depth,
            lineIndex,
        });
    }

    private renderToolComplete(event: Record<string, unknown>): void {
        const data = record(event.data);
        const toolCallId = typeof data.toolCallId === "string" ? data.toolCallId : undefined;
        if (!toolCallId || this.shownToolCompletions.has(toolCallId)) return;
        this.shownToolCompletions.add(toolCallId);
        this.closeStream();
        const succeeded = data.success !== false;
        const display = this.toolDisplays.get(toolCallId);
        if (display && this.interactive) {
            const line = `${this.indent(display.depth)}|-- ${display.description}...${
                succeeded ? "done" : "failed"
            }`;
            const distance = this.renderedLineCount - display.lineIndex;
            moveCursor(this.output, 0, -distance);
            cursorTo(this.output, 0);
            clearLine(this.output, 0);
            this.output.write(this.paint(line, succeeded ? "green" : "red"));
            moveCursor(this.output, 0, distance);
            cursorTo(this.output, 0);
            return;
        }
        const description = display?.description ?? this.toolDescriptions.get(toolCallId);
        this.line(
            this.eventDepth(event),
            `${description ?? "Tool invoked"}...${succeeded ? "done" : "failed"}`,
            succeeded ? "green" : "red"
        );
    }

    private getPhase(data: Record<string, unknown>): PhaseInfo {
        const phaseId = typeof data.phaseId === "string" ? data.phaseId : "";
        const existing = this.phaseById.get(phaseId);
        if (existing) return existing;
        const phase = {
            index: this.phaseById.size,
            kind:
                typeof data.phaseKind === "string"
                    ? data.phaseKind
                    : typeof data.kind === "string"
                      ? data.kind
                      : "phase",
        };
        if (phaseId) this.phaseById.set(phaseId, phase);
        return phase;
    }

    private eventDepth(event: Record<string, unknown>): number {
        return Object.keys(fusionData(event)).length > 0 ? 3 : 1;
    }

    private closeMatchingStream(
        kind: OpenStream["kind"],
        id: string,
        finalContent: unknown
    ): boolean {
        if (!this.openStream || this.openStream.kind !== kind || this.openStream.id !== id) {
            return false;
        }
        if (typeof finalContent === "string" && finalContent.startsWith(this.openStream.content)) {
            this.output.write(clean(finalContent.slice(this.openStream.content.length)));
        }
        this.output.write("\n");
        this.openStream = undefined;
        return true;
    }

    private closeStream(): void {
        if (!this.openStream) return;
        this.output.write("\n");
        this.openStream = undefined;
    }

    private indent(depth: number): string {
        return "  ".repeat(depth);
    }

    private line(
        depth: number,
        content: string,
        color: "blue" | "cyan" | "dim" | "green" | "magenta" | "red" | "yellow"
    ): number {
        const line = `${this.indent(depth)}${content}`;
        const lineIndex = this.renderedLineCount;
        this.output.write(`${this.paint(line, color)}\n`);
        this.renderedLineCount += 1;
        return lineIndex;
    }

    private paint(
        line: string,
        color: "blue" | "cyan" | "dim" | "green" | "magenta" | "red" | "yellow"
    ): string {
        return this.color ? styleText(color, line, { validateStream: false }) : line;
    }
}

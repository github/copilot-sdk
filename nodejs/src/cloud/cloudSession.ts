/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import type { AssistantMessageEvent } from "../session.js";
import type {
    CloudAskUserResponsePayload,
    CloudElicitationResponsePayload,
    CloudModeSwitchPayload,
    CloudPermissionResponsePayload,
    CloudPlanApprovalResponsePayload,
    CloudSessionEvent,
    CloudSessionEventHandler,
    CloudSessionEventPayload,
    CloudSessionEventType,
    CloudSessionMetadata,
    ElicitationResult,
    ExitPlanModeResult,
    MessageOptions,
    MissionControlCommandType,
    TypedCloudSessionEventHandler,
} from "../types.js";
import { MissionControlCommandType as CommandType } from "../types.js";
import type { MissionControlClient } from "./missionControlClient.js";

const DEFAULT_POLL_INTERVAL_MS = 5_000;
const DEFAULT_INITIAL_EVENT_TIMEOUT_MS = 10_000;
const DEFAULT_INITIAL_EVENT_POLL_INTERVAL_MS = 500;

export interface CloudSessionCreateOptions {
    client: MissionControlClient;
    metadata: CloudSessionMetadata;
    pollIntervalMs?: number;
    initialEventTimeoutMs?: number;
    initialEventPollIntervalMs?: number;
    onEventPollError?: (error: Error) => void;
}

export class CloudSession {
    private readonly client: MissionControlClient;
    private readonly pollIntervalMs: number;
    private readonly initialEventTimeoutMs: number;
    private readonly initialEventPollIntervalMs: number;
    private readonly onEventPollError?: (error: Error) => void;
    private readonly eventHandlers = new Set<CloudSessionEventHandler>();
    private readonly typedEventHandlers = new Map<
        CloudSessionEventType,
        Set<(event: CloudSessionEvent) => void>
    >();
    private readonly events: CloudSessionEvent[] = [];
    private seenEventIds = new Set<string>();
    private seenEventIdsAtLastTimestamp = new Set<string>();
    private lastSeenTimestamp?: string;
    private eventPoller: ReturnType<typeof setInterval> | undefined;
    private isPolling = false;
    private isDisconnected = false;
    private remoteSteerable = true;

    readonly sessionId: string;
    readonly metadata: CloudSessionMetadata;

    constructor(options: CloudSessionCreateOptions) {
        this.client = options.client;
        this.metadata = options.metadata;
        this.sessionId = options.metadata.missionControlSessionId ?? options.metadata.taskId;
        this.pollIntervalMs = options.pollIntervalMs ?? DEFAULT_POLL_INTERVAL_MS;
        this.initialEventTimeoutMs =
            options.initialEventTimeoutMs ?? DEFAULT_INITIAL_EVENT_TIMEOUT_MS;
        this.initialEventPollIntervalMs =
            options.initialEventPollIntervalMs ?? DEFAULT_INITIAL_EVENT_POLL_INTERVAL_MS;
        this.onEventPollError = options.onEventPollError;
    }

    async connect(): Promise<void> {
        const initialEvents = await this.waitForInitialEvents();
        this.recordEvents(initialEvents);
        this.startEventPolling();
    }

    on<K extends CloudSessionEventType>(
        eventType: K,
        handler: TypedCloudSessionEventHandler<K>
    ): () => void;
    on(handler: CloudSessionEventHandler): () => void;
    on<K extends CloudSessionEventType>(
        eventTypeOrHandler: K | CloudSessionEventHandler,
        handler?: TypedCloudSessionEventHandler<K>
    ): () => void {
        if (typeof eventTypeOrHandler === "string" && handler) {
            const eventType = eventTypeOrHandler;
            if (!this.typedEventHandlers.has(eventType)) {
                this.typedEventHandlers.set(eventType, new Set());
            }
            const storedHandler = handler as (event: CloudSessionEvent) => void;
            this.typedEventHandlers.get(eventType)!.add(storedHandler);
            return () => {
                this.typedEventHandlers.get(eventType)?.delete(storedHandler);
            };
        }

        const wildcardHandler = eventTypeOrHandler as CloudSessionEventHandler;
        this.eventHandlers.add(wildcardHandler);
        return () => {
            this.eventHandlers.delete(wildcardHandler);
        };
    }

    async send(options: MessageOptions): Promise<void> {
        this.assertConnected();
        await this.submitRemoteCommand(CommandType.UserMessage, options.prompt);
    }

    async sendAndWait(
        options: MessageOptions,
        timeout?: number
    ): Promise<AssistantMessageEvent | undefined> {
        const effectiveTimeout = timeout ?? 60_000;
        let lastAssistantMessage: AssistantMessageEvent | undefined;
        let timeoutId: ReturnType<typeof setTimeout> | undefined;
        let unsubscribe: (() => void) | undefined;

        const idlePromise = new Promise<void>((resolve, reject) => {
            unsubscribe = this.on((event) => {
                if (event.type === "assistant.message") {
                    lastAssistantMessage = event as AssistantMessageEvent;
                } else if (event.type === "session.idle") {
                    resolve();
                } else if (event.type === "session.error") {
                    reject(new Error(event.data.message));
                }
            });
        });

        try {
            await this.send(options);
            await Promise.race([
                idlePromise,
                new Promise<never>((_, reject) => {
                    timeoutId = setTimeout(
                        () =>
                            reject(
                                new Error(
                                    `Timeout after ${effectiveTimeout}ms waiting for session.idle`
                                )
                            ),
                        effectiveTimeout
                    );
                }),
            ]);
            return lastAssistantMessage;
        } finally {
            if (timeoutId !== undefined) {
                clearTimeout(timeoutId);
            }
            unsubscribe?.();
        }
    }

    async abort(): Promise<void> {
        this.assertConnected();
        await this.submitRemoteCommand(CommandType.Abort);
    }

    async submitRemoteCommand(type: MissionControlCommandType, content?: string): Promise<void> {
        this.assertConnected();
        if (!this.remoteSteerable) {
            throw new Error("This session is read-only — remote steering is not enabled");
        }
        await this.client.steerTask(this.metadata.taskId, { type, content });
    }

    async respondToPermission(payload: CloudPermissionResponsePayload): Promise<void> {
        await this.submitRemoteCommand(CommandType.PermissionResponse, JSON.stringify(payload));
    }

    async respondToAskUser(payload: CloudAskUserResponsePayload): Promise<void> {
        await this.submitRemoteCommand(CommandType.AskUserResponse, JSON.stringify(payload));
    }

    async respondToElicitation(payload: CloudElicitationResponsePayload): Promise<void> {
        await this.submitRemoteCommand(CommandType.ElicitationResponse, JSON.stringify(payload));
    }

    async respondToExitPlanMode(payload: CloudPlanApprovalResponsePayload): Promise<void> {
        await this.submitRemoteCommand(CommandType.PlanApprovalResponse, JSON.stringify(payload));
    }

    async switchMode(payload: CloudModeSwitchPayload): Promise<void> {
        await this.submitRemoteCommand(CommandType.ModeSwitch, JSON.stringify(payload));
    }

    async respondToElicitationResult(promptId: string, result: ElicitationResult): Promise<void> {
        await this.respondToElicitation({ promptId, ...result });
    }

    async respondToPlanApproval(promptId: string, result: ExitPlanModeResult): Promise<void> {
        await this.respondToExitPlanMode({ promptId, ...result });
    }

    getMessages(): CloudSessionEvent[] {
        return [...this.events];
    }

    async disconnect(): Promise<void> {
        this.stopEventPolling();
        this.eventHandlers.clear();
        this.typedEventHandlers.clear();
        this.isDisconnected = true;
    }

    async destroy(): Promise<void> {
        return this.disconnect();
    }

    async [Symbol.asyncDispose](): Promise<void> {
        return this.disconnect();
    }

    startEventPolling(): void {
        if (this.eventPoller || this.isDisconnected) {
            return;
        }

        this.eventPoller = setInterval(() => {
            this.pollEvents().catch((error) => this.reportPollError(error));
        }, this.pollIntervalMs);
        this.eventPoller.unref?.();
    }

    stopEventPolling(): void {
        if (this.eventPoller) {
            clearInterval(this.eventPoller);
            this.eventPoller = undefined;
        }
    }

    private async waitForInitialEvents(): Promise<CloudSessionEvent[]> {
        const deadline = Date.now() + this.initialEventTimeoutMs;
        while (true) {
            const events = await this.client.listTaskEvents(this.metadata.taskId);
            if (events.length > 0) {
                return CloudSession.sortEventsChronologically(events);
            }
            if (this.initialEventTimeoutMs <= 0 || Date.now() >= deadline) {
                return [];
            }
            await sleep(this.initialEventPollIntervalMs);
        }
    }

    private async pollEvents(): Promise<void> {
        if (this.isPolling || this.isDisconnected) {
            return;
        }

        this.isPolling = true;
        try {
            const events = await this.client.listTaskEvents(this.metadata.taskId);
            const newEvents = this.collectNewEvents(events);
            this.recordEvents(newEvents);
        } finally {
            this.isPolling = false;
        }
    }

    private collectNewEvents(events: CloudSessionEvent[]): CloudSessionEvent[] {
        const newEvents = events.filter((event) => {
            if (this.seenEventIds.has(event.id)) return false;
            if (!this.lastSeenTimestamp) return true;
            const order = event.timestamp.localeCompare(this.lastSeenTimestamp);
            if (order > 0) return true;
            if (order < 0) return false;
            return !this.seenEventIdsAtLastTimestamp.has(event.id);
        });

        return CloudSession.sortEventsChronologically(newEvents);
    }

    private recordEvents(events: CloudSessionEvent[]): void {
        for (const event of CloudSession.sortEventsChronologically(events)) {
            if (this.seenEventIds.has(event.id)) continue;
            this.seenEventIds.add(event.id);
            this.events.push(event);
            this.markEventAsSeenAtTimestamp(event);
            this.updateRemoteSteerable(event);
            this.dispatchEvent(event);
        }
    }

    private markEventAsSeenAtTimestamp(event: CloudSessionEvent): void {
        if (this.lastSeenTimestamp !== event.timestamp) {
            this.lastSeenTimestamp = event.timestamp;
            this.seenEventIdsAtLastTimestamp = new Set();
        }
        this.seenEventIdsAtLastTimestamp.add(event.id);
    }

    private updateRemoteSteerable(event: CloudSessionEvent): void {
        if (event.type === "session.remote_steerable_changed") {
            this.remoteSteerable = event.data.remoteSteerable;
        }
    }

    private dispatchEvent(event: CloudSessionEvent): void {
        const typedHandlers = this.typedEventHandlers.get(event.type);
        if (typedHandlers) {
            for (const handler of typedHandlers) {
                try {
                    handler(event as CloudSessionEventPayload<typeof event.type>);
                } catch {
                    // Keep one failing handler from stopping event polling.
                }
            }
        }

        for (const handler of this.eventHandlers) {
            try {
                handler(event);
            } catch {
                // Keep one failing handler from stopping event polling.
            }
        }
    }

    private reportPollError(error: unknown): void {
        const normalized = error instanceof Error ? error : new Error(String(error));
        if (this.onEventPollError) {
            this.onEventPollError(normalized);
        }
    }

    private assertConnected(): void {
        if (this.isDisconnected) {
            throw new Error("Cloud session is disconnected");
        }
    }

    private static sortEventsChronologically(events: CloudSessionEvent[]): CloudSessionEvent[] {
        return [...events].sort(
            (left, right) =>
                left.timestamp.localeCompare(right.timestamp) || left.id.localeCompare(right.id)
        );
    }
}

function sleep(ms: number): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, ms));
}

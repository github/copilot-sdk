/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import type { Disposable, MessageConnection } from "vscode-jsonrpc/node.js";
import type { CopilotSession } from "./session.js";

const APP_SESSION_BADGES_PROTOCOL_VERSION = 1 as const;
const REGISTER_METHOD = "extensions.appSessionBadges.register";
const SET_BADGE_METHOD = "extensions.appSessionBadges.setBadge";
const SET_BADGES_METHOD = "extensions.appSessionBadges.setBadges";
const SNAPSHOT_NOTIFICATION = "appSessionBadges.snapshot";

/** Badge states an app-level extension can contribute for a workspace. */
export type AppSessionBadgeState = "draft" | "open" | "merged" | "closed";

/** Constrained badge presentation contributed by an app-level extension. */
export interface AppSessionBadge {
    state: AppSessionBadgeState;
    label?: string;
}

/** An app-visible session that is eligible for an extension-provided badge. */
export interface AppSessionBadgeTarget {
    workspaceId: string;
    sessionId: string;
    repositoryPath: string;
    worktreePath: string;
    branch?: string;
}

/** Complete replacement snapshot of app-visible sessions eligible for extension badges. */
export interface AppSessionBadgesSnapshot {
    readonly protocolVersion: 1;
    readonly revision: number;
    readonly sessions: readonly AppSessionBadgeTarget[];
}

/** Target identity used when publishing or clearing a badge. */
export type AppSessionBadgeTargetIdentity = Pick<
    AppSessionBadgeTarget,
    "workspaceId" | "sessionId"
>;

/** One badge publication or clear in an atomic batch. */
export interface AppSessionBadgeUpdate {
    readonly workspaceId: string;
    readonly sessionId: string;
    readonly badge: AppSessionBadge | null;
}

/** Callback invoked for each full eligible-session snapshot. */
export type AppSessionBadgesSnapshotHandler = (snapshot: AppSessionBadgesSnapshot) => void;

/**
 * Registered app-session badge contribution for an executable extension.
 *
 * The host supplies full replacement snapshots. Native app badges remain
 * authoritative; the host decides which sessions are eligible and validates
 * eligibility again when applying an extension update.
 */
export class AppSessionBadgesExtension {
    private readonly handlers = new Set<AppSessionBadgesSnapshotHandler>();
    private latestSnapshot: AppSessionBadgesSnapshot | undefined;
    private notificationRegistration: Disposable | undefined;

    private constructor(
        /** The retained hidden session this contribution joined. */
        readonly session: CopilotSession,
        private readonly connection: MessageConnection
    ) {}

    /** Most recently received full snapshot, if the host has supplied one. */
    get snapshot(): AppSessionBadgesSnapshot | undefined {
        return this.latestSnapshot;
    }

    /**
     * Subscribe to full replacement snapshots.
     *
     * If a snapshot has already arrived, the handler receives it synchronously
     * before this method returns.
     */
    onSnapshot(handler: AppSessionBadgesSnapshotHandler): () => void {
        this.handlers.add(handler);
        if (this.latestSnapshot) {
            handler(this.latestSnapshot);
        }
        return () => {
            this.handlers.delete(handler);
        };
    }

    /** Publish a constrained badge for a currently eligible target. */
    async setBadge(
        target: AppSessionBadgeTargetIdentity,
        badge: AppSessionBadge | null
    ): Promise<void> {
        const update = normalizeBadgeUpdate({ ...target, badge }, 0);

        await this.connection.sendRequest(SET_BADGE_METHOD, {
            protocolVersion: APP_SESSION_BADGES_PROTOCOL_VERSION,
            ...update,
        });
    }

    /**
     * Atomically publish or clear badges for multiple eligible targets.
     *
     * The complete batch is validated before one request is sent. Duplicate
     * workspace and session target pairs are rejected.
     */
    async setBadges(updates: readonly AppSessionBadgeUpdate[]): Promise<void> {
        const normalizedUpdates = normalizeBadgeUpdates(updates);
        if (normalizedUpdates.length === 0) {
            return;
        }

        await this.connection.sendRequest(SET_BADGES_METHOD, {
            protocolVersion: APP_SESSION_BADGES_PROTOCOL_VERSION,
            updates: normalizedUpdates,
        });
    }

    /** Clear a previously published badge for a target. */
    async clearBadge(target: AppSessionBadgeTargetIdentity): Promise<void> {
        await this.setBadge(target, null);
    }

    /**
     * Stop receiving snapshots in this process.
     *
     * The runtime clears published state when the extension disconnects or is
     * disabled; disposing this local listener does not alter that lifecycle.
     */
    dispose(): void {
        this.notificationRegistration?.dispose();
        this.notificationRegistration = undefined;
        this.handlers.clear();
    }

    /** @internal */
    static async register(
        session: CopilotSession,
        connection: MessageConnection
    ): Promise<AppSessionBadgesExtension> {
        const contribution = new AppSessionBadgesExtension(session, connection);
        contribution.notificationRegistration = connection.onNotification(
            SNAPSHOT_NOTIFICATION,
            (payload: unknown) => {
                try {
                    contribution.handleSnapshot(payload);
                } catch (error) {
                    console.error("Invalid app session badges snapshot ignored", error);
                }
            }
        );

        try {
            await connection.sendRequest(REGISTER_METHOD);
            return contribution;
        } catch (error) {
            contribution.dispose();
            throw error;
        }
    }

    private handleSnapshot(payload: unknown): void {
        const snapshot = parseSnapshot(payload);
        this.latestSnapshot = snapshot;
        for (const handler of this.handlers) {
            try {
                handler(snapshot);
            } catch (error) {
                console.error("App session badges snapshot handler failed", error);
            }
        }
    }
}

function parseSnapshot(payload: unknown): AppSessionBadgesSnapshot {
    if (!isRecord(payload)) {
        throw new TypeError("appSessionBadges.snapshot must be an object");
    }
    if (payload.protocolVersion !== APP_SESSION_BADGES_PROTOCOL_VERSION) {
        throw new TypeError(
            `Unsupported app session badges protocol version: ${String(payload.protocolVersion)}`
        );
    }
    if (!Number.isSafeInteger(payload.revision) || (payload.revision as number) < 0) {
        throw new TypeError("appSessionBadges.snapshot revision must be a non-negative integer");
    }
    if (!Array.isArray(payload.sessions)) {
        throw new TypeError("appSessionBadges.snapshot sessions must be an array");
    }

    const targetIds = new Set<string>();
    const sessions = payload.sessions.map((session, index) => {
        if (!isRecord(session)) {
            throw new TypeError(`appSessionBadges.snapshot sessions[${index}] must be an object`);
        }
        const workspaceId = readNonEmptyString(session, "workspaceId", index);
        const sessionId = readNonEmptyString(session, "sessionId", index);
        const targetId = `${workspaceId}\0${sessionId}`;
        if (targetIds.has(targetId)) {
            throw new TypeError(
                `appSessionBadges.snapshot contains duplicate target: ${workspaceId}/${sessionId}`
            );
        }
        targetIds.add(targetId);

        const target: AppSessionBadgeTarget = {
            workspaceId,
            sessionId,
            repositoryPath: readNonEmptyString(session, "repositoryPath", index),
            worktreePath: readNonEmptyString(session, "worktreePath", index),
        };
        if (session.branch !== undefined) {
            if (typeof session.branch !== "string") {
                throw new TypeError(
                    `appSessionBadges.snapshot sessions[${index}].branch must be a string`
                );
            }
            target.branch = session.branch;
        }
        return Object.freeze(target);
    });

    return Object.freeze({
        protocolVersion: APP_SESSION_BADGES_PROTOCOL_VERSION,
        revision: payload.revision as number,
        sessions: Object.freeze(sessions),
    });
}

function normalizeBadge(badge: AppSessionBadge | null): AppSessionBadge | null {
    if (badge === null) {
        return null;
    }
    if (!isRecord(badge)) {
        throw new TypeError("badge must be an object or null");
    }
    if (!["draft", "open", "merged", "closed"].includes(badge.state as string)) {
        throw new TypeError(`Unsupported app session badge state: ${String(badge.state)}`);
    }
    if (badge.label !== undefined && typeof badge.label !== "string") {
        throw new TypeError("badge.label must be a string when provided");
    }
    return badge.label === undefined
        ? { state: badge.state as AppSessionBadgeState }
        : { state: badge.state as AppSessionBadgeState, label: badge.label };
}

function normalizeBadgeUpdates(updates: readonly AppSessionBadgeUpdate[]): AppSessionBadgeUpdate[] {
    if (!Array.isArray(updates)) {
        throw new TypeError("updates must be an array");
    }

    const targetIds = new Set<string>();
    return updates.map((update, index) => {
        const normalized = normalizeBadgeUpdate(update, index);
        const targetId = `${normalized.workspaceId}\0${normalized.sessionId}`;
        if (targetIds.has(targetId)) {
            throw new TypeError(
                `updates contains duplicate target: ${normalized.workspaceId}/${normalized.sessionId}`
            );
        }
        targetIds.add(targetId);
        return normalized;
    });
}

function normalizeBadgeUpdate(update: AppSessionBadgeUpdate, index: number): AppSessionBadgeUpdate {
    if (!isRecord(update)) {
        throw new TypeError(`updates[${index}] must be an object`);
    }
    assertNonEmptyString(update.workspaceId, `updates[${index}].workspaceId`);
    assertNonEmptyString(update.sessionId, `updates[${index}].sessionId`);
    return {
        workspaceId: update.workspaceId,
        sessionId: update.sessionId,
        badge: normalizeBadge(update.badge),
    };
}

function isRecord(value: unknown): value is Record<string, unknown> {
    return typeof value === "object" && value !== null && !Array.isArray(value);
}

function assertNonEmptyString(value: unknown, name: string): asserts value is string {
    if (typeof value !== "string" || value.length === 0) {
        throw new TypeError(`${name} must be a non-empty string`);
    }
}

function readNonEmptyString(value: Record<string, unknown>, field: string, index: number): string {
    const fieldValue = value[field];
    assertNonEmptyString(fieldValue, `sessions[${index}].${field}`);
    return fieldValue;
}

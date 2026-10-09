// Copyright (c) Microsoft Corporation. All rights reserved.

import {
    CancellationTokenSource,
    ErrorCodes,
    ResponseError,
    type MessageConnection,
} from "vscode-jsonrpc/node.js";
import type {
    NotificationCapabilitiesResult,
    NotificationPermissionResult,
    NotificationShowParams,
    NotificationShowResult,
} from "./generated/rpc.js";

export type {
    NotificationCapabilitiesResult,
    NotificationClickAction,
    NotificationClickKind,
    NotificationOsPermissionState,
    NotificationPermission,
    NotificationPermissionResult,
    NotificationPermissionState,
    NotificationPlatform,
    NotificationShowParams,
    NotificationShowResult,
    NotificationSound,
    NotificationSounds,
} from "./generated/rpc.js";

/** Local request options. Never serialized into a notification payload. @experimental */
export interface NotificationRequestOptions {
    /** Cancels pending runtime/host work; cannot retract an OS-accepted notification. */
    signal?: AbortSignal;
}

/** Provider-process desktop notifications. Does not require a visible canvas. @experimental */
export interface SessionNotificationsApi {
    /** Discover support and current permission without prompting or delivering a notification. */
    getCapabilities(options?: NotificationRequestOptions): Promise<NotificationCapabilitiesResult>;
    /** Request permission after an explicit user action, not when a canvas opens. */
    requestPermission(options?: NotificationRequestOptions): Promise<NotificationPermissionResult>;
    /**
     * Request one OS notification. Accepted means OS handoff, not confirmed display.
     * Never retry or fall back after a failed or ambiguous delivery.
     */
    show(
        params: NotificationShowParams,
        options?: NotificationRequestOptions
    ): Promise<NotificationShowResult>;
}

/** @internal */
export function createSessionNotifications(
    connection: MessageConnection,
    sessionId: string
): SessionNotificationsApi {
    async function request<T>(
        method: string,
        params: object,
        options?: NotificationRequestOptions
    ): Promise<T> {
        const signal = options?.signal;
        if (signal?.aborted) {
            throw new DOMException("Notification request cancelled", "AbortError");
        }
        const payload = { ...params, sessionId };
        if (!signal) {
            return connection.sendRequest<T>(method, payload);
        }
        const cancellation = new CancellationTokenSource();
        const abort = () => cancellation.cancel();
        signal.addEventListener("abort", abort, { once: true });
        try {
            return await connection.sendRequest<T>(method, payload, cancellation.token);
        } finally {
            signal.removeEventListener("abort", abort);
            cancellation.dispose();
        }
    }

    return {
        async getCapabilities(options) {
            try {
                return await request<NotificationCapabilitiesResult>(
                    "session.notifications.getCapabilities",
                    {},
                    options
                );
            } catch (error) {
                if (error instanceof ResponseError && error.code === ErrorCodes.MethodNotFound) {
                    return { status: "unsupported" };
                }
                throw error;
            }
        },
        requestPermission: (options) =>
            request<NotificationPermissionResult>(
                "session.notifications.requestPermission",
                {},
                options
            ),
        show: (params, options) =>
            request<NotificationShowResult>("session.notifications.show", params, options),
    };
}

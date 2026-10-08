/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import assert from "node:assert/strict";
import { randomUUID } from "node:crypto";
import { once } from "node:events";
import WebSocket from "ws";
import { withDeadline } from "./ahpClient.js";
import { sealAhpAuthToken } from "./ahpSealedAuth.js";

type JsonObject = Record<string, unknown>;
type Notification = { method: string; params: JsonObject };

// Wire-only observer: never passes a 1.0 snapshot through the released 0.9
// client/reducers. The ordinary SDK E2Es continue to negotiate 0.9.
export async function connectRawAhp(
    host: { url?: string; token?: string },
    protocolVersion: "0.9.0" | "1.0.0",
    token: string
) {
    assert(host.url);
    const url = new URL(host.url);
    if (host.token !== undefined) url.searchParams.set("tkn", host.token);
    const socket = new WebSocket(url, { handshakeTimeout: 10_000 });
    const pending = new Map<
        number,
        { resolve: (value: JsonObject) => void; reject: (error: Error) => void }
    >();
    const notifications: Notification[] = [];
    const listeners = new Set<() => void>();
    let sequence = 0;
    let closed: Error | undefined;
    function fail(error: Error) {
        closed = error;
        for (const request of pending.values()) request.reject(error);
        pending.clear();
        for (const notify of listeners) notify();
    }
    socket.on("error", fail);
    socket.on("close", () => fail(new Error("Raw AHP connection closed")));
    socket.on("message", (data) => {
        try {
            const message = JSON.parse(data.toString());
            if (typeof message.id === "number") {
                const request = pending.get(message.id);
                if (request) {
                    pending.delete(message.id);
                    if (message.error) {
                        // Do not include request payloads or credentials in failures.
                        request.reject(new Error(`AHP error ${message.error.code}`));
                    } else {
                        request.resolve(message.result);
                    }
                }
            } else if (typeof message.method === "string") {
                notifications.push({ method: message.method, params: message.params });
                for (const notify of listeners) notify();
            }
        } catch {
            fail(new Error("Invalid AHP JSON-RPC frame"));
        }
    });
    async function request(method: string, params: JsonObject): Promise<JsonObject> {
        if (closed) throw closed;
        const id = ++sequence;
        try {
            return await withDeadline(
                new Promise<JsonObject>((resolve, reject) => {
                    pending.set(id, { resolve, reject });
                    socket.send(JSON.stringify({ jsonrpc: "2.0", id, method, params }));
                }),
                `raw ${method}`
            );
        } finally {
            pending.delete(id);
        }
    }
    async function notification(
        method: string,
        matches: (params: JsonObject) => boolean
    ): Promise<JsonObject> {
        let wake: () => void = () => {};
        try {
            return await withDeadline(
                new Promise<JsonObject>((resolve, reject) => {
                    wake = () => {
                        const index = notifications.findIndex(
                            (entry) => entry.method === method && matches(entry.params)
                        );
                        if (index >= 0) resolve(notifications.splice(index, 1)[0].params);
                        else if (closed) reject(closed);
                    };
                    listeners.add(wake);
                    wake();
                }),
                `raw ${method} notification`
            );
        } finally {
            listeners.delete(wake);
        }
    }
    try {
        await withDeadline(once(socket, "open"), "raw AHP connect");
        const initialized = await request("initialize", {
            channel: "ahp-root://",
            clientId: randomUUID(),
            protocolVersions: [protocolVersion],
        });
        assert.equal(initialized.protocolVersion, protocolVersion);
        const subscribed = await request("subscribe", { channel: "ahp-root://" });
        const root = (subscribed.snapshot as { state: JsonObject }).state;
        const agents = root.agents as {
            provider: string;
            protectedResources?: { resource: string; resource_name: string }[];
        }[];
        const resource = agents
            .find((agent) => agent.provider === "copilot")
            ?.protectedResources?.find((entry) => entry.resource_name === "GitHub API")?.resource;
        assert(resource);
        await request("authenticate", {
            channel: "ahp-root://",
            resource,
            token: await sealAhpAuthToken(initialized._meta, root._meta, resource, token),
        });
        return {
            request,
            notification,
            async [Symbol.asyncDispose]() {
                if (socket.readyState === WebSocket.CLOSED) return;
                const finished = once(socket, "close");
                socket.terminate();
                await withDeadline(finished, "raw AHP close");
            },
        };
    } catch (error) {
        socket.terminate();
        throw error;
    }
}

// Copyright (c) Microsoft Corporation. All rights reserved.

import { once } from "node:events";
import { createServer } from "node:net";
import { describe, expect, it, onTestFinished } from "vitest";
import {
    createMessageConnection,
    StreamMessageReader,
    StreamMessageWriter,
    type MessageConnection,
} from "vscode-jsonrpc/node.js";
import {
    approveAll,
    CopilotClient,
    RuntimeConnection,
    type ImageGenerationConfig,
} from "../src/index.js";
import { getSdkProtocolVersion } from "../src/sdkProtocolVersion.js";

describe("image generation request serialization", () => {
    it.each([undefined, {}, { enabled: true }, { enabled: false }] satisfies (
        | ImageGenerationConfig
        | undefined
    )[])("forwards %j on create and resume", async (imageGeneration) => {
        const requests = new Map<string, Record<string, unknown>>();
        const connections: MessageConnection[] = [];
        const server = createServer((socket) => {
            const connection = createMessageConnection(
                new StreamMessageReader(socket),
                new StreamMessageWriter(socket)
            );
            connections.push(connection);
            connection.onRequest((method, params) => {
                if (method === "connect") {
                    return { ok: true, protocolVersion: getSdkProtocolVersion(), version: "test" };
                }
                if (method === "session.create" || method === "session.resume") {
                    const payload = params as Record<string, unknown>;
                    requests.set(method, payload);
                    return { sessionId: payload.sessionId };
                }
                return {};
            });
            connection.listen();
        });
        onTestFinished(async () => {
            for (const connection of connections) connection.dispose();
            await new Promise<void>((resolve, reject) =>
                server.close((error) => (error ? reject(error) : resolve()))
            );
        });
        server.listen(0, "127.0.0.1");
        await once(server, "listening");
        const address = server.address();
        if (!address || typeof address === "string") throw new Error("Missing TCP address");
        const client = new CopilotClient({
            connection: RuntimeConnection.forUri(`127.0.0.1:${address.port}`),
        });
        onTestFinished(() => client.forceStop());
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            imageGeneration,
        });
        await client.resumeSession(session.sessionId, {
            onPermissionRequest: approveAll,
            imageGeneration,
        });
        for (const method of ["session.create", "session.resume"]) {
            const payload = requests.get(method);
            expect(payload).toBeDefined();
            if (imageGeneration === undefined) {
                expect(payload).not.toHaveProperty("imageGeneration");
            } else {
                expect(payload?.imageGeneration).toEqual(imageGeneration);
            }
        }
    });
});

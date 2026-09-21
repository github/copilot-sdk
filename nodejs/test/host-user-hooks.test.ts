/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/* eslint-disable @typescript-eslint/no-explicit-any */
import { describe, expect, it, vi } from "vitest";
import { ErrorCodes, ResponseError } from "vscode-jsonrpc/node.js";
import { approveAll, CopilotClient, RuntimeConnection, type SessionConfig } from "../src/index.js";

function setup(mode: "empty" | "copilot-cli") {
    const client = new CopilotClient({
        mode,
        connection: RuntimeConnection.forUri("http://localhost:1234"),
    });
    const sendRequest = vi.fn(async (_method: string, params: any) => ({
        sessionId: params.sessionId ?? "session-id",
        success: true,
    }));
    (client as any).connection = { sendRequest };
    return { client, sendRequest };
}

describe("host user hooks admission", () => {
    it("does not probe ping when connect is unsupported", async () => {
        const { client, sendRequest } = setup("copilot-cli");
        const error = new ResponseError(ErrorCodes.MethodNotFound, "Unhandled method connect");
        sendRequest.mockRejectedValue(error);
        await expect((client as any).verifyProtocolVersion()).rejects.toBe(error);
        expect(sendRequest).toHaveBeenCalledTimes(1);
        expect(sendRequest.mock.calls[0][0]).toBe("connect");
    });

    it("rejects protocol 3 without retrying or falling back", async () => {
        const { client, sendRequest } = setup("copilot-cli");
        sendRequest.mockImplementation(async () => ({ protocolVersion: 3 }) as any);
        await expect((client as any).verifyProtocolVersion()).rejects.toThrow(
            "SDK protocol version mismatch"
        );
        expect(sendRequest).toHaveBeenCalledTimes(1);
        expect(sendRequest.mock.calls[0][0]).toBe("connect");
    });

    for (const mode of ["empty", "copilot-cli"] as const) {
        for (const supplied of [undefined, false, true]) {
            for (const operation of ["create", "resume"] as const) {
                it(`${operation}: ${mode}, supplied=${supplied}`, async () => {
                    const { client, sendRequest } = setup(mode);
                    const onSessionStart = vi.fn(() => undefined);
                    const config: SessionConfig = {
                        onPermissionRequest: approveAll,
                        availableTools: [],
                        enableHostUserHooks: supplied,
                        hooks: { onSessionStart },
                    };
                    const session =
                        operation === "create"
                            ? await client.createSession(config)
                            : await client.resumeSession("existing-enabled-session", config);
                    const calls = sendRequest.mock.calls.filter(
                        ([method]) => method === `session.${operation}`
                    );
                    expect(calls).toHaveLength(1);
                    const payload = JSON.parse(JSON.stringify(calls[0][1]));
                    expect(payload.enableHostUserHooks).toBe(supplied ?? mode !== "empty");
                    expect(payload.hooks).toBe(true);
                    await session._handleHooksInvoke("sessionStart", {});
                    expect(onSessionStart).toHaveBeenCalledOnce();
                    expect(config.enableHostUserHooks).toBe(supplied);
                });
            }
        }
    }

    it("resolves a reused config for the current client without mutating it", async () => {
        const config: SessionConfig = { onPermissionRequest: approveAll, availableTools: [] };
        for (const mode of ["empty", "copilot-cli"] as const) {
            const { client, sendRequest } = setup(mode);
            const session = await client.createSession(config);
            await client.resumeSession(session.sessionId, config);
            for (const [method, params] of sendRequest.mock.calls) {
                if (method === "session.create" || method === "session.resume") {
                    expect(params.enableHostUserHooks).toBe(mode !== "empty");
                }
            }
            expect(config).not.toHaveProperty("enableHostUserHooks");
        }
    });

    it("resume without host hook options does not inherit the previously enabled value", async () => {
        for (const mode of ["empty", "copilot-cli"] as const) {
            const { client, sendRequest } = setup(mode);
            const session = await client.createSession({
                onPermissionRequest: approveAll,
                availableTools: [],
                enableHostUserHooks: true,
            });
            if (mode === "empty") {
                await client.resumeSession(session.sessionId, { availableTools: [] });
            } else {
                await client.resumeSession(session.sessionId);
            }
            const payload = sendRequest.mock.calls.find(([m]) => m === "session.resume")![1];
            expect(payload.enableHostUserHooks).toBe(mode !== "empty");
        }
    });
});

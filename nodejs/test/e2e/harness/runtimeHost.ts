/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import assert from "node:assert/strict";
import { execFile } from "node:child_process";
import { randomUUID } from "node:crypto";
import { once } from "node:events";
import { accessSync, constants, realpathSync } from "node:fs";
import { readFile, readlink } from "node:fs/promises";
import { connect } from "node:net";
import { isAbsolute } from "node:path";
import { pathToFileURL } from "node:url";
import { promisify } from "node:util";
import {
    ActionType,
    MessageKind,
    PROTOCOL_VERSION,
    ResponsePartKind,
    type SessionState,
} from "@microsoft/agent-host-protocol";
import { AhpClient, type Subscription } from "@microsoft/agent-host-protocol/client";
import { WebSocketTransport } from "@microsoft/agent-host-protocol/ws";
import WebSocket from "ws";
import type { CopilotHost } from "../../../src/index.js";
import { waitForCondition } from "./sdkTestHelper.js";

export function localHostArtifacts() {
    function artifact(name: string, executable = true): string {
        const value = process.env[name];
        assert(value && isAbsolute(value), `${name} must name an absolute, locally built artifact`);
        const path = realpathSync(value);
        assert(!path.includes("/node_modules/"), `${name} must not use a released runtime package`);
        accessSync(path, executable ? constants.X_OK : constants.R_OK);
        return path;
    }
    return {
        runtimePath: artifact("COPILOT_CLI_PATH"),
        providerPath: artifact("COPILOT_RUNTIME_PROVIDER_LIB", false),
        litePath: artifact("COPILOTD_LITE_PATH"),
    };
}

export async function withDeadline<T>(promise: Promise<T>, label: string, ms = 30_000): Promise<T> {
    let timer: ReturnType<typeof setTimeout> | undefined;
    try {
        return await Promise.race([
            promise,
            new Promise<never>((_, reject) => {
                timer = setTimeout(() => reject(new Error(`Timed out: ${label}`)), ms);
            }),
        ]);
    } finally {
        clearTimeout(timer);
    }
}

export async function connectAhp(host: Pick<CopilotHost, "url" | "token">) {
    const url = new URL(host.url);
    url.searchParams.set("tkn", host.token);
    const socket = new WebSocket(url, {
        handshakeTimeout: 10_000,
    });
    try {
        await once(socket, "open");
        const transport = WebSocketTransport.fromSocket(socket as unknown as globalThis.WebSocket);
        const client = new AhpClient(transport, { requestTimeoutMs: 15_000 });
        client.connect();
        const clientId = randomUUID();
        const initialized = await client.initialize({
            clientId,
            protocolVersions: [PROTOCOL_VERSION],
        });
        assert.equal(initialized.protocolVersion, PROTOCOL_VERSION);
        return { client, clientId, transport };
    } catch (error) {
        socket.terminate();
        throw error;
    }
}

export async function createAhpSession(
    ahp: Awaited<ReturnType<typeof connectAhp>>,
    workDir: string
) {
    const sessionId = randomUUID();
    const sessionUri = `ahp-session:/${sessionId}`;
    await ahp.client.request("createSession", {
        channel: sessionUri,
        provider: "copilot",
        workingDirectories: [pathToFileURL(workDir).href],
        activeClient: { clientId: ahp.clientId, displayName: "Runtime host E2E", tools: [] },
    });
    const { result } = await ahp.client.subscribe(sessionUri);
    const chatUri = (result.snapshot?.state as SessionState | undefined)?.defaultChat;
    assert(chatUri, "AHP createSession must create a default chat");
    const { subscription } = await ahp.client.subscribe(chatUri);
    return { sessionId, sessionUri, chatUri, subscription };
}

export async function streamedTurn(
    client: AhpClient,
    chatUri: string,
    subscription: Subscription,
    prompt: string,
    model = "claude-sonnet-5"
) {
    const turnId = randomUUID();
    const parts = new Map<string, string>();
    let deltas = 0;
    client.dispatch(chatUri, {
        type: ActionType.ChatTurnStarted,
        turnId,
        startedAt: new Date().toISOString(),
        message: { text: prompt, origin: { kind: MessageKind.User }, model: { id: model } },
    });
    return withDeadline(
        (async () => {
            for await (const event of subscription) {
                if (event.type !== "action") continue;
                if (event.params.rejectionReason) throw new Error(event.params.rejectionReason);
                const action = event.params.action;
                if (!("turnId" in action) || action.turnId !== turnId) continue;
                if (
                    action.type === ActionType.ChatResponsePart &&
                    action.part.kind === ResponsePartKind.Markdown
                ) {
                    parts.set(action.part.id, action.part.content);
                } else if (action.type === ActionType.ChatDelta) {
                    deltas++;
                    parts.set(action.partId, (parts.get(action.partId) ?? "") + action.content);
                } else if (action.type === ActionType.ChatError) {
                    throw new Error(action.part.error.message);
                } else if (action.type === ActionType.ChatTurnComplete) {
                    return { text: [...parts.values()].join("").trim(), deltas };
                }
            }
            throw new Error("AHP subscription closed before chat/turnComplete");
        })(),
        "streamed AHP turn"
    );
}

/** Check the actual OS parent and executable, not merely the PID in an RPC response. */
export async function assertRuntimeChild(
    host: CopilotHost,
    runtimePid: number,
    artifacts: ReturnType<typeof localHostArtifacts>
) {
    assert.notEqual(
        runtimePid,
        process.pid,
        "Use an out-of-process runtime for this topology test"
    );
    const { stdout } = await promisify(execFile)("ps", ["-o", "ppid=", "-p", String(host.pid)]);
    assert.equal(Number(stdout.trim()), runtimePid, "copilotd-lite must be the runtime's child");
    assert.equal(await readlink(`/proc/${host.pid}/exe`), artifacts.litePath);
    assert.equal(await readlink(`/proc/${runtimePid}/exe`), artifacts.runtimePath);
    const runtimeMaps = await readFile(`/proc/${runtimePid}/maps`, "utf8");
    assert(runtimeMaps.includes(artifacts.providerPath), "Runtime must load the local provider");
    const liteMaps = await readFile(`/proc/${host.pid}/maps`, "utf8");
    assert(!liteMaps.includes(artifacts.providerPath), "Lite must not embed another runtime");
    const children = await readFile(`/proc/${host.pid}/task/${host.pid}/children`, "utf8");
    assert.equal(children.trim(), "", "Idle copilotd-lite must not spawn a second runtime");
}

export async function assertHostStopped(
    host: CopilotHost,
    ahp: Awaited<ReturnType<typeof connectAhp>>
) {
    await waitForCondition(
        () => {
            try {
                process.kill(host.pid, 0);
                return false;
            } catch (error) {
                if ((error as NodeJS.ErrnoException).code !== "ESRCH") throw error;
                return true;
            }
        },
        { timeoutMessage: "Runtime did not reap copilotd-lite" }
    );
    await waitForCondition(() => ahp.transport.lastClose !== null, {
        timeoutMessage: "Existing AHP client was not disconnected",
    });
    const url = new URL(host.url);
    const refusal = await new Promise<string | undefined>((resolve, reject) => {
        const socket = connect({ host: url.hostname, port: Number(url.port) });
        socket.setTimeout(5_000, () => {
            socket.destroy();
            reject(new Error("Timed out probing stopped listener"));
        });
        socket.once("connect", () => {
            socket.destroy();
            resolve(undefined);
        });
        socket.once("error", (error: NodeJS.ErrnoException) => resolve(error.code));
    });
    assert.equal(refusal, "ECONNREFUSED", "The listener must close, not merely reject AHP auth");
}

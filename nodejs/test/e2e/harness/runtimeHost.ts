/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import assert from "node:assert/strict";
import { randomUUID } from "node:crypto";
import { once } from "node:events";
import { accessSync, constants, realpathSync } from "node:fs";
import { readFile, readlink } from "node:fs/promises";
import { connect } from "node:net";
import { isAbsolute } from "node:path";
import { pathToFileURL } from "node:url";
import {
    ActionType,
    MessageKind,
    PROTOCOL_VERSION,
    ResponsePartKind,
    type RootState,
    type SessionState,
} from "@microsoft/agent-host-protocol";
import { AhpClient, type Subscription } from "@microsoft/agent-host-protocol/client";
import { WebSocketTransport } from "@microsoft/agent-host-protocol/ws";
import WebSocket from "ws";
import type { CopilotHost } from "../../../src/index.js";
import { waitForCondition } from "./sdkTestHelper.js";
import { candidateHostArtifacts } from "./runtimeHostCandidate.js";

export function localHostArtifacts() {
    const candidate = process.env.COPILOT_RUNTIME_HOST_CANDIDATE_MANIFEST;
    if (candidate) return candidateHostArtifacts(candidate);
    function artifact(name: string, executable = true): string {
        const value = process.env[name];
        assert(value && isAbsolute(value), `${name} must name an absolute, locally built artifact`);
        const path = realpathSync(value);
        assert(!path.includes("/node_modules/"), `${name} must not use a released runtime package`);
        accessSync(path, executable ? constants.X_OK : constants.R_OK);
        return path;
    }
    const artifacts = {
        runtimePath: artifact("COPILOT_CLI_PATH"),
        providerPath: artifact("COPILOT_RUNTIME_PROVIDER_LIB", false),
        litePath: artifact("COPILOTD_LITE_PATH"),
    };
    return {
        ...artifacts,
        bundled: false,
        env: {
            COPILOTD_LITE_PATH: artifacts.litePath,
            COPILOT_RUNTIME_PROVIDER_LIB: artifacts.providerPath,
        },
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

export async function authenticateAhp(
    ahp: Awaited<ReturnType<typeof connectAhp>>,
    githubToken: string
) {
    assert(githubToken, "Session creation requires a GitHub credential");
    const { result: root } = await ahp.client.subscribe("ahp-root://");
    const agent = (root.snapshot?.state as RootState | undefined)?.agents.find(
        (entry) => entry.provider === "copilot"
    );
    const resource = agent?.protectedResources?.find(
        (entry) => entry.resource_name === "GitHub API"
    );
    assert(resource, "The Copilot agent must advertise its GitHub protected resource");
    await ahp.client.request("authenticate", {
        channel: "ahp-root://",
        resource: resource.resource,
        token: githubToken,
    });
}

export async function createAhpSession(
    ahp: Awaited<ReturnType<typeof connectAhp>>,
    workDir: string,
    githubToken: string
) {
    await authenticateAhp(ahp, githubToken);
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
    artifacts: ReturnType<typeof localHostArtifacts>,
    catalogPath?: string
) {
    assert.notEqual(
        runtimePid,
        process.pid,
        "Use an out-of-process runtime for this topology test"
    );
    const status = await readFile(`/proc/${host.pid}/status`, "utf8");
    const parent = /^PPid:\s+(\d+)$/m.exec(status);
    assert(parent, "Linux process status must report the host parent PID");
    assert.equal(Number(parent[1]), runtimePid, "copilotd-lite must be the runtime's child");
    const hostCommand = (await readFile(`/proc/${host.pid}/cmdline`, "utf8")).split("\0");
    assert.equal(hostCommand[0], artifacts.litePath);
    if (catalogPath) {
        const option = hostCommand.indexOf("--catalog-path");
        assert(option > 0, "Runtime must pass its resolved AHP catalog path");
        assert.equal(hostCommand[option + 1], catalogPath);
    }
    // Lite intentionally disables dumpability when receiving its private
    // bootstrap stream. Do not weaken that protection just to inspect its maps.
    await assert.rejects(readlink(`/proc/${host.pid}/exe`), { code: "EACCES" });
    await assert.rejects(readFile(`/proc/${host.pid}/maps`, "utf8"), { code: "EACCES" });
    assert.equal(await readlink(`/proc/${runtimePid}/exe`), artifacts.runtimePath);
    const runtimeMaps = await readFile(`/proc/${runtimePid}/maps`, "utf8");
    assert(runtimeMaps.includes(artifacts.providerPath), "Runtime must load the local provider");
    if (artifacts.bundled) {
        const environment = (await readFile(`/proc/${runtimePid}/environ`, "utf8")).split("\0");
        for (const name of ["COPILOTD_LITE_PATH", "COPILOT_RUNTIME_PROVIDER_LIB"]) {
            assert(
                !environment.some((entry) => entry.startsWith(`${name}=`)),
                `Candidate runtime must not receive the ${name} development override`
            );
        }
    }
    const children = await readFile(`/proc/${host.pid}/task/${host.pid}/children`, "utf8");
    assert.equal(children.trim(), "", "Idle copilotd-lite must not spawn a second runtime");
}

export async function assertProcessStopped(pid: number, label: string) {
    await waitForCondition(
        () => {
            try {
                process.kill(pid, 0);
                return false;
            } catch (error) {
                if ((error as NodeJS.ErrnoException).code !== "ESRCH") throw error;
                return true;
            }
        },
        { timeoutMessage: `${label} was not reaped` }
    );
}

export async function assertHostStopped(
    host: CopilotHost,
    ahp: Awaited<ReturnType<typeof connectAhp>>
) {
    await assertProcessStopped(host.pid, "copilotd-lite");
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

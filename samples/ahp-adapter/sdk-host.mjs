import assert from "node:assert/strict";
import { spawn } from "node:child_process";
import { randomUUID } from "node:crypto";
import { once } from "node:events";
import { createConnection } from "node:net";
import { createInterface } from "node:readline";
import { fileURLToPath } from "node:url";
import {
    SocketMessageReader, SocketMessageWriter,
    StreamMessageReader, StreamMessageWriter,
} from "vscode-jsonrpc/node.js";
import { CopilotClient, RuntimeConnection, defineTool } from "../../nodejs/dist/index.js";

const [runtimePath, adapterPath, mode] = process.argv.slice(2);
if (!runtimePath || !adapterPath) {
    throw new Error("Usage: node sdk-host.mjs <runtime-path> <adapter-path> [--verify]");
}
const workspace = fileURLToPath(new URL("./workspace", import.meta.url));
const token = randomUUID();
const runtime = spawn(runtimePath, [
    "--server", "--port", "0", "--auth-token-env", "COPILOT_SDK_AUTH_TOKEN", "--no-auto-login",
], {
    env: {
        ...process.env,
        COPILOT_CONNECTION_TOKEN: token,
        COPILOT_SDK_AUTH_TOKEN: process.env.COPILOT_SDK_AUTH_TOKEN ??
            process.env.GITHUB_TOKEN ?? process.env.GH_TOKEN ?? "",
    },
    stdio: ["ignore", "pipe", "pipe"],
});
runtime.stderr.pipe(process.stderr);
let client;
let adapter;
let probe;
let socket;
let stoppingAdapter = false;
const resources = [];
const sessions = new Map();
const creating = new Map();
let toolCalls = 0;
let factoryCalls = 0;
let forwardedFilesystemCalls = 0;
const finished = Promise.withResolvers();
void finished.promise.catch(() => {});
const fail = error => finished.reject(error instanceof Error ? error : new Error(String(error)));
runtime.on("error", fail);
const runtimeExit = once(runtime, "exit");
runtime.once("exit", (code, signal) => {
    if (code !== 0 && signal === null) fail(new Error(`Runtime exited with code ${code}`));
});

function closeBridge() {
    for (const resource of resources) resource.dispose();
    resources.length = 0;
    socket?.destroy();
}

function waitForLine(stream, pattern, child) {
    return new Promise((resolve, reject) => {
        const lines = createInterface({ input: stream });
        const timer = setTimeout(() => done(new Error("Timed out waiting for child readiness")), 60_000);
        const exited = (code, signal) => done(new Error(`Child exited before readiness: ${code ?? signal}`));
        const errored = error => done(error);
        function done(error, value) {
            clearTimeout(timer);
            child.off("exit", exited);
            child.off("error", errored);
            lines.close();
            if (error) reject(error);
            else resolve(value);
        }
        child.once("exit", exited);
        child.once("error", errored);
        lines.on("line", line => {
            const match = pattern.exec(line);
            if (match) done(undefined, match[1]);
        });
    });
}

async function factory(method, params) {
    const id = params.sessionId;
    if (typeof id !== "string") throw new Error("Factory requires a sessionId");
    if (sessions.has(id)) return { sessionId: id };
    if (creating.has(id)) return creating.get(id);
    const work = (async () => {
        factoryCalls++;
        const options = {
            model: process.env.COPILOT_MODEL ?? "gpt-4.1",
            streaming: true,
            workingDirectory: workspace,
            systemMessage: {
                mode: "replace",
                content: "You are Bert. When asked who you are, reply exactly: I am Bert. " +
                    "When explicitly asked to call bert_identity, call that tool and then reply with its result.",
            },
            tools: [defineTool("bert_identity", {
                description: "Returns Bert's identity from the owning SDK application's tool handler.",
                parameters: { type: "object", properties: {}, required: [] },
                handler: async () => {
                    toolCalls++;
                    console.log("[SDK tool] bert_identity invoked in the application");
                    return "I am Bert.";
                },
            })],
            onPermissionRequest: async () => ({ kind: "approve-once" }),
        };
        const session = method === "sdkHost.createSession"
            ? await client.createSession({ ...options, sessionId: id })
            : await client.resumeSession(id, options);
        let deltas = 0;
        session.on("assistant.message_delta", () => deltas++);
        session.on("assistant.message", event => {
            console.log(`[SDK observed ${session.sessionId}] ${event.data.content}`);
            console.log(`[SDK streaming] ${deltas} deltas`);
            deltas = 0;
        });
        session.on("session.error", event => console.error(`[SDK session error] ${event.data.message}`));
        sessions.set(session.sessionId, session);
        console.log(`SDK factory session: ${session.sessionId}`);
        return { sessionId: session.sessionId };
    })();
    creating.set(id, work);
    try {
        return await work;
    } finally {
        creating.delete(id);
    }
}

try {
    const port = Number(await waitForLine(runtime.stdout, /CLI server listening on port (\d+)/, runtime));
    client = new CopilotClient({
        connection: RuntimeConnection.forUri(`127.0.0.1:${port}`, { connectionToken: token }),
    });
    await client.start();
    socket = createConnection({ host: "127.0.0.1", port });
    socket.on("error", fail);
    await once(socket, "connect");
    adapter = spawn(adapterPath, ["--workspace", workspace], {
        env: process.env, stdio: ["pipe", "pipe", "pipe"],
    });
    adapter.on("error", fail);
    const adapterExit = once(adapter, "exit");
    void adapterExit.catch(fail);
    adapter.once("exit", (code, signal) => {
        closeBridge();
        if (!stoppingAdapter || (code !== 0 && signal === null)) {
            fail(new Error(`AHP adapter exited: ${code ?? signal}`));
        }
    });
    const readiness = waitForLine(adapter.stderr, /AHP URL: (ws:\/\/\S+)/, adapter);
    adapter.stderr.pipe(process.stderr);
    const childReader = new StreamMessageReader(adapter.stdout);
    const childWriter = new StreamMessageWriter(adapter.stdin);
    const runtimeReader = new SocketMessageReader(socket);
    const runtimeWriter = new SocketMessageWriter(socket);
    resources.push(childReader, childWriter, runtimeReader, runtimeWriter);
    for (const resource of resources) resource.onError(fail);
    runtimeReader.listen(message => { void childWriter.write(message).catch(fail); });
    childReader.listen(message => {
        if (message.method === "sdkHost.createSession" || message.method === "sdkHost.resumeSession") {
            void factory(message.method, message.params ?? {}).then(
                result => childWriter.write({ jsonrpc: "2.0", id: message.id, result }),
                error => childWriter.write({
                    jsonrpc: "2.0", id: message.id,
                    error: { code: -32603, message: error.message },
                }),
            ).catch(fail);
            return;
        }
        // Every ordinary frame retains its ID on the child's independent runtime connection.
        if (message.method === "connect") {
            message = { ...message, params: { ...message.params, token } };
        }
        if (/^resource(Read|List|Resolve|Write)$/.test(message.method ?? "")) {
            forwardedFilesystemCalls++;
        }
        void runtimeWriter.write(message).catch(fail);
    });
    const url = await readiness;
    console.log(`AHP URL: ${url}`);
    console.log(`Workspace: ${workspace}`);
    console.log(`Processes: app=${process.pid} runtime=${runtime.pid} adapter=${adapter.pid}`);
    console.log(`Run: npm run client -- '${url}' '${workspace}'`);
    if (mode === "--verify") {
        probe = spawn(process.execPath, [
            fileURLToPath(new URL("./ahp-client.mjs", import.meta.url)), url, workspace,
        ], { stdio: "inherit" });
        const [code] = await Promise.race([once(probe, "exit"), finished.promise]);
        assert.equal(code, 0, "Independent AHP client must succeed");
        assert.equal(factoryCalls, 1, "One application factory creates the session");
        assert(toolCalls > 0, "Original application-owned tool callback must run");
        assert.equal(forwardedFilesystemCalls, 0, "AHP filesystem runs in adapter, not SDK/runtime");
        stoppingAdapter = true;
        // Keep replies flowing while the child disconnects its SDK attachment.
        adapter.kill("SIGTERM");
        const [adapterCode] = await Promise.race([
            adapterExit,
            finished.promise,
            new Promise((_, reject) => setTimeout(() => reject(new Error("Adapter did not stop")), 15_000).unref()),
        ]);
        assert.equal(adapterCode, 0, "Adapter must shut down cleanly");
        for (const session of sessions.values()) {
            const response = await Promise.race([
                session.sendAndWait({ prompt: "who are you" }, 60_000),
                finished.promise,
            ]);
            assert.equal(response?.data.content.trim(), "I am Bert.");
        }
        console.log("Verified: Bert + original SDK tool + adapter filesystem; SDK session survives adapter shutdown.");
    } else {
        process.once("SIGINT", () => finished.resolve());
        process.once("SIGTERM", () => finished.resolve());
        await finished.promise;
    }
} finally {
    stoppingAdapter = true;
    closeBridge();
    if (probe && probe.exitCode === null && probe.signalCode === null) probe.kill("SIGTERM");
    if (adapter && adapter.exitCode === null && adapter.signalCode === null) adapter.kill("SIGTERM");
    await client?.stop();
    if (runtime.exitCode === null && runtime.signalCode === null) runtime.kill("SIGTERM");
    await runtimeExit;
}

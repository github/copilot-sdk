import assert from "node:assert/strict";
import { spawn, spawnSync } from "node:child_process";
import { createHash } from "node:crypto";
import { once } from "node:events";
import { cpSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { parseArgs } from "node:util";
import {
    createMessageConnection,
    StreamMessageReader,
    StreamMessageWriter,
} from "vscode-jsonrpc-8/node.js";

const { values } = parseArgs({
    options: {
        package: { type: "string" },
        node: { type: "string", default: process.execPath },
        "node-types": { type: "string" },
    },
});
assert.ok(values.package, "--package must identify an already built SDK tarball");
assert.ok(values["node-types"], "--node-types must specify an exact version");
assert.match(values["node-types"], /^\d+\.\d+\.\d+$/);
assert.ok(process.env.npm_execpath, "Run this verifier through npm run test:consumer-types");

const tarball = resolve(values.package);
const node = values.node;
const fixtures = join(dirname(fileURLToPath(import.meta.url)), "..", "test", "consumer-types");
const consumer = mkdtempSync(join(tmpdir(), "copilot-sdk-consumer-"));
const compilerVersion = "7.0.2";

function run(args, timeout = 120_000) {
    const result = spawnSync(node, args, {
        cwd: consumer,
        encoding: "utf8",
        timeout,
        maxBuffer: 10 * 1024 * 1024,
    });
    if (result.error) throw result.error;
    assert.equal(
        result.status,
        0,
        `${node} ${args.join(" ")} failed (signal ${result.signal})\n${result.stdout}\n${result.stderr}`
    );
    return result.stdout.trim();
}

async function verifyExtensionRuntime() {
    const child = spawn(node, [join(consumer, "runtime-extension.mjs")], {
        cwd: consumer,
        env: { ...process.env, SESSION_ID: "consumer-session" },
        stdio: ["pipe", "pipe", "pipe"],
    });
    const exited = once(child, "exit");
    const connection = createMessageConnection(
        new StreamMessageReader(child.stdout),
        new StreamMessageWriter(child.stdin)
    );
    let stderr = "";
    const ready = new Promise((resolveReady) => {
        child.stderr.on("data", (chunk) => {
            stderr += chunk.toString();
            if (stderr.includes("consumer-ready\n")) resolveReady();
        });
    });
    const resume = new Promise((resolveResume) => {
        connection.onRequest("session.resume", (params) => {
            resolveResume(params);
            return { sessionId: params.sessionId };
        });
    });
    const toolResult = new Promise((resolveResult) => {
        connection.onRequest("session.tools.handlePendingToolCall", (params) => {
            resolveResult(params);
            return { success: true };
        });
    });
    connection.onRequest("connect", () => ({ protocolVersion: 3 }));
    connection.listen();
    let timer;
    try {
        await Promise.race([
            (async () => {
                const params = await resume;
                assert.equal(params.sessionId, "consumer-session");
                assert.equal(params.requestPermission, true);
                assert.equal(params.hooks, true);
                assert.equal(params.disableResume, true);
                assert.deepEqual(params.tools, [
                    {
                        name: "consumer_probe",
                        description: "Return a deterministic result.",
                        parameters: { type: "object", properties: {} },
                    },
                ]);
                await ready;
                assert.deepEqual(
                    await connection.sendRequest("hooks.invoke", {
                        sessionId: "consumer-session",
                        hookType: "sessionStart",
                        input: { source: "resume", timestamp: 0, cwd: consumer },
                    }),
                    { output: { additionalContext: "resume:consumer-session" } }
                );
                await connection.sendNotification("session.event", {
                    sessionId: "consumer-session",
                    event: {
                        id: "consumer-event",
                        timestamp: new Date(0).toISOString(),
                        parentId: null,
                        type: "external_tool.requested",
                        data: {
                            sessionId: "consumer-session",
                            requestId: "consumer-request",
                            toolCallId: "consumer-call",
                            toolName: "consumer_probe",
                            arguments: {},
                        },
                    },
                });
                assert.deepEqual(await toolResult, {
                    sessionId: "consumer-session",
                    requestId: "consumer-request",
                    result: "consumer-result",
                });
                assert.deepEqual(
                    await connection.sendRequest("hooks.invoke", {
                        sessionId: "consumer-session",
                        hookType: "sessionEnd",
                        input: { reason: "complete", timestamp: 0, cwd: consumer },
                    }),
                    { output: { sessionSummary: "complete" } }
                );
            })(),
            exited.then(([code, signal]) => {
                throw new Error(
                    `Extension exited before verification: ${code}, ${signal}\n${stderr}`
                );
            }),
            new Promise((_, reject) => {
                timer = setTimeout(
                    () => reject(new Error(`Extension verification timed out\n${stderr}`)),
                    30_000
                );
            }),
        ]);
    } finally {
        clearTimeout(timer);
        connection.dispose();
        if (child.exitCode === null && child.signalCode === null) child.kill();
        await exited;
    }
}

try {
    cpSync(fixtures, consumer, { recursive: true });
    writeFileSync(
        join(consumer, "package.json"),
        JSON.stringify({
            name: "copilot-sdk-strict-consumer",
            private: true,
            type: "module",
            dependencies: {
                "@github/copilot-sdk": `file:${tarball.replaceAll("\\", "/")}`,
                "@types/node": values["node-types"],
                typescript: compilerVersion,
            },
        })
    );
    run(
        [process.env.npm_execpath, "install", "--ignore-scripts", "--no-audit", "--no-fund"],
        180_000
    );
    const compiler = join(consumer, "node_modules", "typescript", "bin", "tsc");
    assert.equal(run([compiler, "--version"]), `Version ${compilerVersion}`);
    assert.equal(
        JSON.parse(
            readFileSync(join(consumer, "node_modules", "@types", "node", "package.json"), "utf8")
        ).version,
        values["node-types"]
    );
    run([compiler, "-p", "tsconfig.json", "--pretty", "false"]);

    const declaration = readFileSync(join(consumer, "declarations", "extension.d.mts"), "utf8");
    for (const match of declaration.matchAll(/(?:from\s+|import\()["']([^"']+)["']/g)) {
        assert.equal(match[1], "@github/copilot-sdk", `Non-public declaration import: ${match[1]}`);
    }
    // Prove the emitted declarations work without their originating source.
    rmSync(join(consumer, "extension.mts"));
    rmSync(join(consumer, "lifecycle.mts"));
    run([compiler, "-p", "tsconfig.consumer.json", "--pretty", "false"]);
    run([
        "--input-type=module",
        "--eval",
        `
        import assert from "node:assert/strict";
        import { CopilotClient } from "@github/copilot-sdk";
        import { joinSession } from "@github/copilot-sdk/extension";
        assert.equal(typeof CopilotClient, "function");
        assert.equal(typeof joinSession, "function");
    `,
    ]);
    run([
        "--input-type=commonjs",
        "--eval",
        `
        const assert = require("node:assert/strict");
        assert.equal(typeof require("@github/copilot-sdk").CopilotClient, "function");
        assert.equal(typeof require("@github/copilot-sdk/extension").joinSession, "function");
    `,
    ]);
    await verifyExtensionRuntime();
    console.log(
        JSON.stringify(
            {
                node: run(["--version"]),
                compiler: compilerVersion,
                nodeTypes: values["node-types"],
                sdk: JSON.parse(
                    readFileSync(
                        join(consumer, "node_modules", "@github", "copilot-sdk", "package.json"),
                        "utf8"
                    )
                ).version,
                sha256: createHash("sha256").update(readFileSync(tarball)).digest("hex"),
                strictDeclarations: "passed",
                declarationConsumer: "passed",
                publicEsmAndCjs: "passed",
                extensionAgainstJsonRpc8: "passed",
            },
            null,
            2
        )
    );
} finally {
    rmSync(consumer, { recursive: true, force: true });
}

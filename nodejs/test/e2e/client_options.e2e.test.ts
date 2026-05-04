/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import * as fs from "fs";
import * as net from "net";
import * as path from "path";
import { describe, expect, it, onTestFinished } from "vitest";
import { approveAll, CopilotClient } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

const FAKE_STDIO_CLI_SCRIPT = `const fs = require("fs");

const captureIndex = process.argv.indexOf("--capture-file");
const captureFile = captureIndex >= 0 ? process.argv[captureIndex + 1] : undefined;
const requests = [];

function saveCapture() {
  if (!captureFile) {
    return;
  }

  fs.writeFileSync(captureFile, JSON.stringify({
    args: process.argv.slice(2),
    cwd: process.cwd(),
    requests,
    env: {
      COPILOT_HOME: process.env.COPILOT_HOME,
      COPILOT_SDK_AUTH_TOKEN: process.env.COPILOT_SDK_AUTH_TOKEN,
      COPILOT_OTEL_ENABLED: process.env.COPILOT_OTEL_ENABLED,
      OTEL_EXPORTER_OTLP_ENDPOINT: process.env.OTEL_EXPORTER_OTLP_ENDPOINT,
      COPILOT_OTEL_FILE_EXPORTER_PATH: process.env.COPILOT_OTEL_FILE_EXPORTER_PATH,
      COPILOT_OTEL_EXPORTER_TYPE: process.env.COPILOT_OTEL_EXPORTER_TYPE,
      COPILOT_OTEL_SOURCE_NAME: process.env.COPILOT_OTEL_SOURCE_NAME,
      OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT: process.env.OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT
    }
  }));
}

saveCapture();

let buffer = Buffer.alloc(0);

process.stdin.on("data", chunk => {
  buffer = Buffer.concat([buffer, chunk]);
  processBuffer();
});

process.stdin.resume();

function processBuffer() {
  while (true) {
    const headerEnd = buffer.indexOf("\\r\\n\\r\\n");
    if (headerEnd < 0) {
      return;
    }

    const header = buffer.subarray(0, headerEnd).toString("utf8");
    const match = /Content-Length:\\s*(\\d+)/i.exec(header);
    if (!match) {
      throw new Error("Missing Content-Length header");
    }

    const length = Number(match[1]);
    const bodyStart = headerEnd + 4;
    const bodyEnd = bodyStart + length;
    if (buffer.length < bodyEnd) {
      return;
    }

    const body = buffer.subarray(bodyStart, bodyEnd).toString("utf8");
    buffer = buffer.subarray(bodyEnd);
    handleMessage(JSON.parse(body));
  }
}

function handleMessage(message) {
  if (!Object.prototype.hasOwnProperty.call(message, "id")) {
    return;
  }

  requests.push({ method: message.method, params: message.params });
  saveCapture();

  if (message.method === "connect") {
    writeResponse(message.id, { ok: true, protocolVersion: 3, version: "fake" });
    return;
  }

  if (message.method === "ping") {
    writeResponse(message.id, { message: "pong", protocolVersion: 3 });
    return;
  }

  if (message.method === "session.create") {
    const sessionId = message.params?.sessionId ?? message.params?.[0]?.sessionId ?? "fake-session";
    writeResponse(message.id, { sessionId, workspacePath: null, capabilities: null });
    return;
  }

  writeResponse(message.id, {});
}

function writeResponse(id, result) {
  const body = JSON.stringify({ jsonrpc: "2.0", id, result });
  process.stdout.write(\`Content-Length: \${Buffer.byteLength(body, "utf8")}\\r\\n\\r\\n\${body}\`);
}
`;

async function getAvailableTcpPort(): Promise<number> {
    return new Promise((resolve, reject) => {
        const server = net.createServer();
        server.once("error", reject);
        server.listen(0, "127.0.0.1", () => {
            const address = server.address();
            if (typeof address === "object" && address !== null) {
                const port = address.port;
                server.close(() => resolve(port));
            } else {
                server.close(() => reject(new Error("Failed to get available TCP port")));
            }
        });
    });
}

function assertArgumentValue(
    args: (string | undefined)[],
    name: string,
    expectedValue: string
): void {
    const index = args.indexOf(name);
    expect(
        index,
        `Expected argument '${name}' was not present. Args: ${args.join(" ")}`
    ).toBeGreaterThanOrEqual(0);
    expect(index + 1).toBeLessThan(args.length);
    expect(args[index + 1]).toBe(expectedValue);
}

describe("Client options", async () => {
    const { copilotClient: defaultClient, env, workDir } = await createSdkTestContext();

    it("autostart false requires explicit start", async () => {
        const client = new CopilotClient({
            cwd: workDir,
            env,
            cliPath: process.env.COPILOT_CLI_PATH,
            autoStart: false,
        });
        onTestFinished(async () => {
            try {
                await client.forceStop();
            } catch {
                // Ignore cleanup errors
            }
        });

        expect(client.getState()).toBe("disconnected");

        await expect(client.createSession({ onPermissionRequest: approveAll })).rejects.toThrow(
            /start/i
        );

        await client.start();
        expect(client.getState()).toBe("connected");

        const session = await client.createSession({ onPermissionRequest: approveAll });
        expect(session.sessionId).toMatch(/^[a-f0-9-]+$/);

        await session.disconnect();
    });

    it("should listen on configured tcp port", async () => {
        const port = await getAvailableTcpPort();
        const client = new CopilotClient({
            cwd: workDir,
            env,
            cliPath: process.env.COPILOT_CLI_PATH,
            useStdio: false,
            port,
        });
        onTestFinished(async () => {
            try {
                await client.forceStop();
            } catch {
                // Ignore cleanup errors
            }
        });

        await client.start();

        expect(client.getState()).toBe("connected");
        expect((client as unknown as { actualPort: number }).actualPort).toBe(port);

        const response = await client.ping("fixed-port");
        expect(response.message).toBe("pong: fixed-port");
    });

    it("should use client cwd for default workingdirectory", async () => {
        const clientCwd = path.join(workDir, "client-cwd");
        fs.mkdirSync(clientCwd, { recursive: true });
        fs.writeFileSync(path.join(clientCwd, "marker.txt"), "I am in the client cwd");

        // Reference defaultClient to keep the shared test context (and its CAPI proxy/env)
        // alive for the duration of this test; we deliberately spin up a fresh client with
        // a custom cwd to assert that the custom cwd is honored.
        void defaultClient;
        const client = new CopilotClient({
            cwd: clientCwd,
            env,
            cliPath: process.env.COPILOT_CLI_PATH,
            gitHubToken: process.env.CI ? "fake-token-for-e2e-tests" : undefined,
        });
        onTestFinished(async () => {
            try {
                await client.forceStop();
            } catch {
                // Ignore cleanup errors
            }
        });

        const session = await client.createSession({ onPermissionRequest: approveAll });

        const message = await session.sendAndWait({
            prompt: "Read the file marker.txt and tell me what it says",
        });

        expect(message?.data.content ?? "").toContain("client cwd");

        await session.disconnect();
    });

    it("should propagate process options to spawned cli", async () => {
        const cliPath = path.join(
            workDir,
            `fake-cli-${Date.now()}-${Math.random().toString(36).slice(2)}.js`
        );
        const capturePath = path.join(
            workDir,
            `fake-cli-capture-${Date.now()}-${Math.random().toString(36).slice(2)}.json`
        );
        const telemetryPath = path.join(workDir, "telemetry.jsonl");
        const copilotHomeFromEnv = path.join(workDir, "copilot-home-from-env");
        const copilotHomeFromOption = path.join(workDir, "copilot-home-from-option");
        fs.writeFileSync(cliPath, FAKE_STDIO_CLI_SCRIPT);

        const client = new CopilotClient({
            cwd: workDir,
            env: { ...env, COPILOT_HOME: copilotHomeFromEnv },
            autoStart: false,
            cliPath,
            cliArgs: ["--capture-file", capturePath],
            copilotHome: copilotHomeFromOption,
            gitHubToken: "process-option-token",
            logLevel: "debug",
            sessionIdleTimeoutSeconds: 17,
            telemetry: {
                otlpEndpoint: "http://127.0.0.1:4318",
                filePath: telemetryPath,
                exporterType: "file",
                sourceName: "ts-sdk-e2e",
                captureContent: true,
            },
            useLoggedInUser: false,
        });
        onTestFinished(async () => {
            try {
                await client.forceStop();
            } catch {
                // Ignore cleanup errors
            }
        });

        await client.start();

        const captureRaw = fs.readFileSync(capturePath, "utf8");
        const capture = JSON.parse(captureRaw) as {
            args: string[];
            cwd: string;
            env: Record<string, string | undefined>;
            requests: { method: string; params: unknown }[];
        };

        assertArgumentValue(capture.args, "--log-level", "debug");
        expect(capture.args).toContain("--stdio");
        assertArgumentValue(capture.args, "--auth-token-env", "COPILOT_SDK_AUTH_TOKEN");
        expect(capture.args).toContain("--no-auto-login");
        assertArgumentValue(capture.args, "--session-idle-timeout", "17");
        expect(path.resolve(capture.cwd)).toBe(path.resolve(workDir));

        expect(capture.env.COPILOT_HOME).toBe(copilotHomeFromOption);
        expect(capture.env.COPILOT_SDK_AUTH_TOKEN).toBe("process-option-token");
        expect(capture.env.COPILOT_OTEL_ENABLED).toBe("true");
        expect(capture.env.OTEL_EXPORTER_OTLP_ENDPOINT).toBe("http://127.0.0.1:4318");
        expect(capture.env.COPILOT_OTEL_FILE_EXPORTER_PATH).toBe(telemetryPath);
        expect(capture.env.COPILOT_OTEL_EXPORTER_TYPE).toBe("file");
        expect(capture.env.COPILOT_OTEL_SOURCE_NAME).toBe("ts-sdk-e2e");
        expect(capture.env.OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT).toBe("true");

        const session = await client.createSession({
            onPermissionRequest: approveAll,
            enableConfigDiscovery: true,
            includeSubAgentStreamingEvents: false,
        });

        const updatedRaw = fs.readFileSync(capturePath, "utf8");
        const updated = JSON.parse(updatedRaw) as {
            requests: {
                method: string;
                params: {
                    enableConfigDiscovery?: boolean;
                    includeSubAgentStreamingEvents?: boolean;
                };
            }[];
        };
        const createRequests = updated.requests.filter((r) => r.method === "session.create");
        expect(createRequests).toHaveLength(1);
        expect(createRequests[0].params.enableConfigDiscovery).toBe(true);
        expect(createRequests[0].params.includeSubAgentStreamingEvents).toBe(false);

        await session.disconnect();
    });

    it("should throw when githubtoken used with cliurl", () => {
        expect(() => {
            new CopilotClient({
                cliUrl: "localhost:8080",
                gitHubToken: "gho_test_token",
            });
        }).toThrow();
    });

    it("should throw when useloggedinuser used with cliurl", () => {
        expect(() => {
            new CopilotClient({
                cliUrl: "localhost:8080",
                useLoggedInUser: false,
            });
        }).toThrow();
    });
});

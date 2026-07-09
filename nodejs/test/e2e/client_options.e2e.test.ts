/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import * as fs from "fs";
import * as net from "net";
import * as path from "path";
import { describe, expect, it, onTestFinished } from "vitest";
import { approveAll, CopilotClient, createCanvas, RuntimeConnection } from "../../src/index.js";
import { createSdkTestContext, DEFAULT_GITHUB_TOKEN } from "./harness/sdkTestContext.js";

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
      OTEL_EXPORTER_OTLP_PROTOCOL: process.env.OTEL_EXPORTER_OTLP_PROTOCOL,
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

  if (message.method === "session.resume") {
    const sessionId = message.params?.sessionId ?? message.params?.[0]?.sessionId ?? "fake-session";
    writeResponse(message.id, {
      sessionId,
      workspacePath: null,
      capabilities: null,
      openCanvases: message.params?.openCanvases ?? []
    });
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

function getCapturedRequest(capturePath: string, method: string): Record<string, unknown> {
    const raw = fs.readFileSync(capturePath, "utf8");
    const capture = JSON.parse(raw) as {
        requests: { method: string; params: Record<string, unknown> }[];
    };
    const request = capture.requests.find((r) => r.method === method);
    expect(request, `Expected ${method} request in capture`).toBeDefined();
    return request!.params;
}

function getObject(value: unknown): Record<string, unknown> {
    expect(value).toBeTypeOf("object");
    expect(value).not.toBeNull();
    return value as Record<string, unknown>;
}

function getArray(value: unknown): unknown[] {
    expect(Array.isArray(value)).toBe(true);
    return value as unknown[];
}

describe("Client options", async () => {
    const { copilotClient: defaultClient, env, workDir } = await createSdkTestContext();

    it("createSession starts the client lazily", async () => {
        const client = new CopilotClient({
            workingDirectory: workDir,
            env,
            connection: RuntimeConnection.forStdio({ path: process.env.COPILOT_CLI_PATH }),
            gitHubToken: DEFAULT_GITHUB_TOKEN,
        });
        onTestFinished(async () => {
            try {
                await client.stop();
            } catch {
                // Ignore cleanup errors
            }
        });

        const session = await client.createSession({ onPermissionRequest: approveAll });
        expect(session.sessionId).toMatch(/^[a-f0-9-]+$/);

        await session.disconnect();
    });

    it("should listen on configured tcp port", async () => {
        const port = await getAvailableTcpPort();
        const client = new CopilotClient({
            workingDirectory: workDir,
            env,
            connection: RuntimeConnection.forTcp({
                path: process.env.COPILOT_CLI_PATH,
                port,
            }),
        });
        onTestFinished(async () => {
            try {
                await client.stop();
            } catch {
                // Ignore cleanup errors
            }
        });

        await client.start();

        expect((client as unknown as { runtimePort: number }).runtimePort).toBe(port);

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
            workingDirectory: clientCwd,
            env,
            connection: RuntimeConnection.forStdio({ path: process.env.COPILOT_CLI_PATH }),
            gitHubToken: DEFAULT_GITHUB_TOKEN,
        });
        onTestFinished(async () => {
            try {
                await client.stop();
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
            workingDirectory: workDir,
            env: { ...env, COPILOT_HOME: copilotHomeFromEnv },
            connection: RuntimeConnection.forStdio({
                path: cliPath,
                args: ["--capture-file", capturePath],
            }),
            baseDirectory: copilotHomeFromOption,
            gitHubToken: "process-option-token",
            logLevel: "debug",
            sessionIdleTimeoutSeconds: 17,
            telemetry: {
                otlpEndpoint: "http://127.0.0.1:4318",
                otlpProtocol: "http/protobuf",
                filePath: telemetryPath,
                exporterType: "file",
                sourceName: "ts-sdk-e2e",
                captureContent: true,
            },
            useLoggedInUser: false,
        });
        onTestFinished(async () => {
            try {
                await client.stop();
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
        expect(capture.env.OTEL_EXPORTER_OTLP_PROTOCOL).toBe("http/protobuf");
        expect(capture.env.COPILOT_OTEL_FILE_EXPORTER_PATH).toBe(telemetryPath);
        expect(capture.env.COPILOT_OTEL_EXPORTER_TYPE).toBe("file");
        expect(capture.env.COPILOT_OTEL_SOURCE_NAME).toBe("ts-sdk-e2e");
        expect(capture.env.OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT).toBe("true");

        const session = await client.createSession({
            onPermissionRequest: approveAll,
            enableConfigDiscovery: true,
            enableOnDemandInstructionDiscovery: true,
            includeSubAgentStreamingEvents: false,
        });

        const updatedRaw = fs.readFileSync(capturePath, "utf8");
        const updated = JSON.parse(updatedRaw) as {
            requests: {
                method: string;
                params: {
                    enableConfigDiscovery?: boolean;
                    enableOnDemandInstructionDiscovery?: boolean;
                    includeSubAgentStreamingEvents?: boolean;
                };
            }[];
        };
        const createRequests = updated.requests.filter((r) => r.method === "session.create");
        expect(createRequests).toHaveLength(1);
        expect(createRequests[0].params.enableConfigDiscovery).toBe(true);
        expect(createRequests[0].params.enableOnDemandInstructionDiscovery).toBe(true);
        expect(createRequests[0].params.includeSubAgentStreamingEvents).toBe(false);

        await session.disconnect();
    });

    it("should forward advanced session options in create wire request", async () => {
        const cliPath = path.join(
            workDir,
            `fake-cli-advanced-create-${Date.now()}-${Math.random().toString(36).slice(2)}.js`
        );
        const capturePath = path.join(
            workDir,
            `fake-cli-advanced-create-capture-${Date.now()}-${Math.random().toString(36).slice(2)}.json`
        );
        const outputDirectory = path.join(workDir, "large-output-create");
        fs.writeFileSync(cliPath, FAKE_STDIO_CLI_SCRIPT);

        const client = new CopilotClient({
            workingDirectory: workDir,
            env,
            connection: RuntimeConnection.forStdio({
                path: cliPath,
                args: ["--capture-file", capturePath],
            }),
            useLoggedInUser: false,
        });
        onTestFinished(async () => {
            try {
                await client.stop();
            } catch {
                // Ignore cleanup errors
            }
        });

        await client.start();

        const canvas = createCanvas({
            id: "advanced-create-canvas",
            displayName: "Advanced Create Canvas",
            description: "Covers create-time canvas options.",
            open: () => ({ url: "https://example.test/advanced-create-canvas" }),
        });
        const session = await client.createSession({
            clientName: "advanced-create-client",
            model: "claude-sonnet-4.5",
            reasoningEffort: "medium",
            reasoningSummary: "detailed",
            contextTier: "long_context",
            enableCitations: true,
            capi: { enableWebSocketResponses: false },
            mcpOAuthTokenStorage: "persistent",
            customAgents: [
                {
                    name: "agent-one",
                    displayName: "Agent One",
                    description: "Handles agent-one tasks.",
                    prompt: "Be agent one.",
                    tools: ["view"],
                    infer: true,
                    skills: ["create-skill"],
                    model: "claude-haiku-4.5",
                },
            ],
            defaultAgent: { excludedTools: ["edit"] },
            agent: "agent-one",
            skillDirectories: ["skills-create"],
            disabledSkills: ["disabled-create-skill"],
            pluginDirectories: ["plugins-create"],
            infiniteSessions: {
                enabled: false,
                backgroundCompactionThreshold: 0.5,
                bufferExhaustionThreshold: 0.9,
            },
            largeOutput: {
                enabled: true,
                maxSizeBytes: 4096,
                outputDirectory,
            },
            memory: { enabled: true },
            gitHubToken: "session-create-token",
            remoteSession: "export",
            cloud: {
                repository: {
                    owner: "github",
                    name: "copilot-sdk",
                    branch: "main",
                },
            },
            enableMcpApps: true,
            requestCanvasRenderer: true,
            requestExtensions: true,
            extensionSdkPath: "custom-extension-sdk",
            extensionInfo: { source: "typescript-sdk-tests", name: "advanced-create-extension" },
            canvases: [canvas],
            providers: [
                {
                    name: "create-provider",
                    type: "openai",
                    wireApi: "responses",
                    baseUrl: "https://create-provider.example.test/v1",
                    apiKey: "create-provider-key",
                    headers: { "X-Create-Provider": "yes" },
                },
            ],
            models: [
                {
                    provider: "create-provider",
                    id: "create-model",
                    name: "Create Model",
                    modelId: "claude-sonnet-4.5",
                    wireModel: "create-wire-model",
                    maxContextWindowTokens: 12_000,
                    maxPromptTokens: 10_000,
                    maxOutputTokens: 2_000,
                },
            ],
            onPermissionRequest: approveAll,
        });

        const createRequest = getCapturedRequest(capturePath, "session.create");
        expect(createRequest.clientName).toBe("advanced-create-client");
        expect(createRequest.model).toBe("claude-sonnet-4.5");
        expect(createRequest.reasoningEffort).toBe("medium");
        expect(createRequest.reasoningSummary).toBe("detailed");
        expect(createRequest.contextTier).toBe("long_context");
        expect(createRequest.enableCitations).toBe(true);
        expect(getObject(createRequest.capi).enableWebSocketResponses).toBe(false);
        expect(createRequest.mcpOAuthTokenStorage).toBe("persistent");
        expect(createRequest.agent).toBe("agent-one");
        expect(getArray(getObject(createRequest.defaultAgent).excludedTools)[0]).toBe("edit");
        expect(getObject(getArray(createRequest.customAgents)[0]).name).toBe("agent-one");
        expect(getArray(createRequest.pluginDirectories)[0]).toBe("plugins-create");
        expect(getArray(createRequest.disabledSkills)[0]).toBe("disabled-create-skill");
        expect(getObject(createRequest.infiniteSessions).enabled).toBe(false);
        expect(getObject(createRequest.largeOutput).enabled).toBe(true);
        expect(getObject(createRequest.largeOutput).maxSizeBytes).toBe(4096);
        expect(getObject(createRequest.largeOutput).outputDir).toBe(outputDirectory);
        expect(getObject(createRequest.memory).enabled).toBe(true);
        expect(createRequest.gitHubToken).toBe("session-create-token");
        expect(createRequest.remoteSession).toBe("export");
        expect(getObject(getObject(createRequest.cloud).repository).owner).toBe("github");
        expect(createRequest.requestMcpApps).toBe(true);
        expect(createRequest.requestCanvasRenderer).toBe(true);
        expect(createRequest.requestExtensions).toBe(true);
        expect(createRequest.extensionSdkPath).toBe("custom-extension-sdk");
        expect(getObject(createRequest.extensionInfo).name).toBe("advanced-create-extension");
        expect(getObject(getArray(createRequest.canvases)[0]).id).toBe("advanced-create-canvas");
        expect(getObject(getArray(createRequest.providers)[0]).name).toBe("create-provider");
        expect(getObject(getArray(createRequest.providers)[0]).wireApi).toBe("responses");
        expect(getObject(getArray(createRequest.models)[0]).id).toBe("create-model");
        expect(getObject(getArray(createRequest.models)[0]).maxContextWindowTokens).toBe(12_000);

        await session.disconnect();
    });

    it("should forward singular provider options in create wire request", async () => {
        const cliPath = path.join(
            workDir,
            `fake-cli-provider-create-${Date.now()}-${Math.random().toString(36).slice(2)}.js`
        );
        const capturePath = path.join(
            workDir,
            `fake-cli-provider-create-capture-${Date.now()}-${Math.random().toString(36).slice(2)}.json`
        );
        fs.writeFileSync(cliPath, FAKE_STDIO_CLI_SCRIPT);

        const client = new CopilotClient({
            workingDirectory: workDir,
            env,
            connection: RuntimeConnection.forStdio({
                path: cliPath,
                args: ["--capture-file", capturePath],
            }),
            useLoggedInUser: false,
        });
        onTestFinished(async () => {
            try {
                await client.stop();
            } catch {
                // Ignore cleanup errors
            }
        });

        await client.start();

        const session = await client.createSession({
            model: "claude-sonnet-4.5",
            provider: {
                type: "azure",
                wireApi: "responses",
                transport: "http",
                baseUrl: "https://azure-provider.example.test/openai",
                apiKey: "provider-api-key",
                bearerToken: "provider-bearer-token",
                azure: { apiVersion: "2024-02-15-preview" },
                headers: { "X-Provider-Wire": "yes" },
                modelId: "claude-sonnet-4.5",
                wireModel: "azure-deployment",
                maxPromptTokens: 8192,
                maxOutputTokens: 1024,
            },
            onPermissionRequest: approveAll,
        });

        const provider = getObject(getCapturedRequest(capturePath, "session.create").provider);
        expect(provider.type).toBe("azure");
        expect(provider.wireApi).toBe("responses");
        expect(provider.transport).toBe("http");
        expect(provider.baseUrl).toBe("https://azure-provider.example.test/openai");
        expect(provider.apiKey).toBe("provider-api-key");
        expect(provider.bearerToken).toBe("provider-bearer-token");
        expect(getObject(provider.azure).apiVersion).toBe("2024-02-15-preview");
        expect(getObject(provider.headers)["X-Provider-Wire"]).toBe("yes");
        expect(provider.modelId).toBe("claude-sonnet-4.5");
        expect(provider.wireModel).toBe("azure-deployment");
        expect(provider.maxPromptTokens).toBe(8192);
        expect(provider.maxOutputTokens).toBe(1024);

        await session.disconnect();
    });

    it("should forward advanced session options in resume wire request", async () => {
        const cliPath = path.join(
            workDir,
            `fake-cli-advanced-resume-${Date.now()}-${Math.random().toString(36).slice(2)}.js`
        );
        const capturePath = path.join(
            workDir,
            `fake-cli-advanced-resume-capture-${Date.now()}-${Math.random().toString(36).slice(2)}.json`
        );
        const outputDirectory = path.join(workDir, "large-output-resume");
        fs.writeFileSync(cliPath, FAKE_STDIO_CLI_SCRIPT);

        const client = new CopilotClient({
            workingDirectory: workDir,
            env,
            connection: RuntimeConnection.forStdio({
                path: cliPath,
                args: ["--capture-file", capturePath],
            }),
            useLoggedInUser: false,
        });
        onTestFinished(async () => {
            try {
                await client.stop();
            } catch {
                // Ignore cleanup errors
            }
        });

        await client.start();

        const session = await client.resumeSession("advanced-resume-session", {
            clientName: "advanced-resume-client",
            model: "claude-haiku-4.5",
            reasoningEffort: "low",
            reasoningSummary: "none",
            contextTier: "default",
            suppressResumeEvent: true,
            continuePendingWork: true,
            mcpOAuthTokenStorage: "persistent",
            pluginDirectories: ["plugins-resume"],
            largeOutput: {
                enabled: false,
                maxSizeBytes: 2048,
                outputDirectory,
            },
            memory: { enabled: false },
            remoteSession: "on",
            openCanvases: [
                {
                    canvasId: "resume-canvas",
                    extensionId: "typescript-sdk-tests/resume-extension",
                    extensionName: "Resume Extension",
                    instanceId: "resume-canvas-1",
                    input: { start: 41 },
                    status: "ready",
                    title: "Resume Canvas",
                    url: "https://example.com/resume-canvas",
                },
            ],
            onPermissionRequest: approveAll,
        });

        const resumeRequest = getCapturedRequest(capturePath, "session.resume");
        expect(resumeRequest.sessionId).toBe("advanced-resume-session");
        expect(resumeRequest.clientName).toBe("advanced-resume-client");
        expect(resumeRequest.model).toBe("claude-haiku-4.5");
        expect(resumeRequest.reasoningEffort).toBe("low");
        expect(resumeRequest.reasoningSummary).toBe("none");
        expect(resumeRequest.contextTier).toBe("default");
        expect(resumeRequest.disableResume).toBe(true);
        expect(resumeRequest.continuePendingWork).toBe(true);
        expect(resumeRequest.mcpOAuthTokenStorage).toBe("persistent");
        expect(getArray(resumeRequest.pluginDirectories)[0]).toBe("plugins-resume");
        expect(getObject(resumeRequest.largeOutput).enabled).toBe(false);
        expect(getObject(resumeRequest.largeOutput).maxSizeBytes).toBe(2048);
        expect(getObject(resumeRequest.largeOutput).outputDir).toBe(outputDirectory);
        expect(getObject(resumeRequest.memory).enabled).toBe(false);
        expect(resumeRequest.remoteSession).toBe("on");

        const openCanvas = getObject(getArray(resumeRequest.openCanvases)[0]);
        expect(openCanvas.canvasId).toBe("resume-canvas");
        expect(openCanvas.extensionId).toBe("typescript-sdk-tests/resume-extension");
        expect(openCanvas.extensionName).toBe("Resume Extension");
        expect(openCanvas.instanceId).toBe("resume-canvas-1");
        expect(getObject(openCanvas.input).start).toBe(41);
        expect(openCanvas.status).toBe("ready");
        expect(openCanvas.title).toBe("Resume Canvas");
        expect(openCanvas.url).toBe("https://example.com/resume-canvas");

        await session.disconnect();
    });

    it("should throw when gitHubToken used with forUri", () => {
        expect(() => {
            new CopilotClient({
                connection: RuntimeConnection.forUri("localhost:8080"),
                gitHubToken: "gho_test_token",
            });
        }).toThrow();
    });

    it("should throw when useLoggedInUser used with forUri", () => {
        expect(() => {
            new CopilotClient({
                connection: RuntimeConnection.forUri("localhost:8080"),
                useLoggedInUser: false,
            });
        }).toThrow();
    });
});

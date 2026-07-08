import { describe, expect, it } from "vitest";
import { writeFile, mkdir } from "fs/promises";
import { join } from "path";
import {
    approveAll,
    CopilotClient,
    CopilotRequestHandler,
    RuntimeConnection,
    type CopilotRequestContext,
} from "../../src/index.js";
import { createSdkTestContext, DEFAULT_GITHUB_TOKEN } from "./harness/sdkTestContext.js";
import { retry } from "./harness/sdkTestHelper.js";

describe("Session Configuration", async () => {
    const { copilotClient: client, workDir, openAiEndpoint, env } = await createSdkTestContext();

    async function waitForExchanges(minimumCount = 1) {
        await retry(
            `capture ${minimumCount} chat completion request(s)`,
            async () => {
                const exchanges = await openAiEndpoint.getExchanges();
                expect(exchanges.length).toBeGreaterThanOrEqual(minimumCount);
            },
            1_200
        );
        return openAiEndpoint.getExchanges();
    }

    it("should use workingDirectory for tool execution", async () => {
        const subDir = join(workDir, "subproject");
        await mkdir(subDir, { recursive: true });
        await writeFile(join(subDir, "marker.txt"), "I am in the subdirectory");

        const session = await client.createSession({
            onPermissionRequest: approveAll,
            workingDirectory: subDir,
        });

        const assistantMessage = await session.sendAndWait({
            prompt: "Read the file marker.txt and tell me what it says",
        });
        expect(assistantMessage?.data.content).toContain("subdirectory");

        await session.disconnect();
    });

    it("should create session with custom provider config", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            provider: {
                baseUrl: "https://api.example.com/v1",
                apiKey: "test-key",
            },
        });

        expect(session.sessionId).toMatch(/^[a-f0-9-]+$/);

        try {
            await session.disconnect();
        } catch {
            // disconnect may fail since the provider is fake
        }
    });

    it("should accept blob attachments", async () => {
        // Write the image to disk so the model can view it if it tries
        const pngBase64 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";
        await writeFile(join(workDir, "pixel.png"), Buffer.from(pngBase64, "base64"));

        const session = await client.createSession({ onPermissionRequest: approveAll });

        await session.sendAndWait({
            prompt: "What color is this pixel? Reply in one word.",
            attachments: [
                {
                    type: "blob",
                    data: pngBase64,
                    mimeType: "image/png",
                    displayName: "pixel.png",
                },
            ],
        });

        await session.disconnect();
    });

    it("should accept message attachments", async () => {
        await writeFile(join(workDir, "attached.txt"), "This file is attached");

        const session = await client.createSession({ onPermissionRequest: approveAll });

        await session.sendAndWait({
            prompt: "Summarize the attached file",
            attachments: [{ type: "file", path: join(workDir, "attached.txt") }],
        });

        await session.disconnect();
    });

    const PNG_1X1 = Buffer.from(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==",
        "base64"
    );
    const VIEW_IMAGE_PROMPT =
        "Use the view tool to look at the file test.png and describe what you see";

    function hasImageUrlContent(messages: Array<{ role: string; content: unknown }>): boolean {
        return messages.some(
            (m) =>
                m.role === "user" &&
                Array.isArray(m.content) &&
                m.content.some((p: { type: string }) => p.type === "image_url")
        );
    }

    it("vision disabled then enabled via setModel", async () => {
        await writeFile(join(workDir, "test.png"), PNG_1X1);

        const session = await client.createSession({
            onPermissionRequest: approveAll,
            modelCapabilities: { supports: { vision: false } },
        });

        // Turn 1: vision off — no image_url expected
        await session.sendAndWait({ prompt: VIEW_IMAGE_PROMPT });
        const trafficAfterT1 = await openAiEndpoint.getExchanges();
        const t1Messages = trafficAfterT1.flatMap((e) => e.request.messages ?? []);
        expect(hasImageUrlContent(t1Messages)).toBe(false);

        // Switch vision on (re-specify same model with updated capabilities)
        await session.setModel("claude-sonnet-4.5", {
            modelCapabilities: { supports: { vision: true } },
        });

        // Turn 2: vision on — image_url expected
        await session.sendAndWait({ prompt: VIEW_IMAGE_PROMPT });
        const trafficAfterT2 = await openAiEndpoint.getExchanges();
        // Only check exchanges added after turn 1
        const newExchanges = trafficAfterT2.slice(trafficAfterT1.length);
        const t2Messages = newExchanges.flatMap((e) => e.request.messages ?? []);
        expect(hasImageUrlContent(t2Messages)).toBe(true);

        await session.disconnect();
    });

    it("vision enabled then disabled via setModel", async () => {
        await writeFile(join(workDir, "test.png"), PNG_1X1);

        const session = await client.createSession({
            onPermissionRequest: approveAll,
            modelCapabilities: { supports: { vision: true } },
        });

        // Turn 1: vision on — image_url expected
        await session.sendAndWait({ prompt: VIEW_IMAGE_PROMPT });
        const trafficAfterT1 = await openAiEndpoint.getExchanges();
        const t1Messages = trafficAfterT1.flatMap((e) => e.request.messages ?? []);
        expect(hasImageUrlContent(t1Messages)).toBe(true);

        // Switch vision off
        await session.setModel("claude-sonnet-4.5", {
            modelCapabilities: { supports: { vision: false } },
        });

        // Turn 2: vision off — no image_url expected in new exchanges
        await session.sendAndWait({ prompt: VIEW_IMAGE_PROMPT });
        const trafficAfterT2 = await openAiEndpoint.getExchanges();
        const newExchanges = trafficAfterT2.slice(trafficAfterT1.length);
        const t2Messages = newExchanges.flatMap((e) => e.request.messages ?? []);
        expect(hasImageUrlContent(t2Messages)).toBe(false);

        await session.disconnect();
    });

    const PROVIDER_HEADER_NAME = "x-copilot-sdk-provider-header";
    const CLIENT_NAME = "ts-public-surface-client";

    function createProxyProvider(headerValue: string) {
        return {
            type: "openai" as const,
            baseUrl: openAiEndpoint.url,
            apiKey: "test-provider-key",
            headers: {
                [PROVIDER_HEADER_NAME]: headerValue,
            },
        };
    }

    function getHeaderString(
        headers: Record<string, string | string[] | undefined> | undefined,
        name: string
    ): string | undefined {
        if (!headers) {
            return undefined;
        }
        const matchingKey = Object.keys(headers).find(
            (k) => k.toLowerCase() === name.toLowerCase()
        );
        if (!matchingKey) {
            return undefined;
        }
        const value = headers[matchingKey];
        if (Array.isArray(value)) {
            return value.join(",");
        }
        return value ?? "";
    }

    function getSystemMessage(exchange: {
        request: { messages?: Array<{ role: string; content: unknown }> };
    }): string | undefined {
        const sys = (exchange.request.messages ?? []).find((m) => m.role === "system") as
            | { content: string }
            | undefined;
        return sys?.content;
    }

    function getToolNames(exchange: {
        request: { tools?: Array<{ function: { name: string } }> };
    }): string[] {
        return (exchange.request.tools ?? []).map((t) => t.function.name);
    }

    async function sendAndGetNextExchange(
        session: { sendAndWait(options: { prompt: string }): Promise<unknown> },
        prompt: string
    ) {
        const existingCount = (await openAiEndpoint.getExchanges()).length;
        await session.sendAndWait({ prompt });
        const exchanges = await waitForExchanges(existingCount + 1);
        return exchanges[existingCount];
    }

    function assertSessionLimitsStatus(
        exchange: { request: { messages?: Array<{ role: string; content: unknown }> } },
        expectedRemaining: string
    ) {
        const message = (exchange.request.messages ?? []).find(
            (m) =>
                m.role === "user" &&
                typeof m.content === "string" &&
                m.content.includes("<session_limits_status>")
        );
        expect(message?.content).toContain(`Remaining session limits: ${expectedRemaining}.`);
        expect(message?.content).toContain(
            "Be frugal; avoid optional exploration and unnecessary tool calls."
        );
    }

    function getTaskAgentTypes(exchange: {
        request: {
            tools?: Array<{
                function: { name: string; parameters?: unknown };
            }>;
        };
    }): string[] {
        const taskTool = (exchange.request.tools ?? []).find(
            (tool) => tool.function.name === "task"
        );
        expect(taskTool).toBeDefined();
        const parameters = taskTool?.function.parameters as
            | { properties?: { agent_type?: { enum?: string[] } } }
            | undefined;
        const values = parameters?.properties?.agent_type?.enum;
        expect(values).toBeDefined();
        return values ?? [];
    }

    interface InterceptedRequest {
        url: string;
        body: string;
    }

    class RecordingRequestHandler extends CopilotRequestHandler {
        readonly records: InterceptedRequest[] = [];

        protected override async sendRequest(
            request: Request,
            _ctx: CopilotRequestContext
        ): Promise<Response> {
            const body = request.body ? await request.text() : "";
            this.records.push({ url: request.url, body });
            return isInferenceUrl(request.url)
                ? buildInferenceResponse(request.url, body)
                : buildNonInferenceResponse(request.url);
        }

        inferenceRequests(): InterceptedRequest[] {
            return this.records.filter((record) => isInferenceUrl(record.url));
        }
    }

    function isInferenceUrl(url: string): boolean {
        const u = url.toLowerCase();
        return (
            u.endsWith("/chat/completions") ||
            u.endsWith("/responses") ||
            u.endsWith("/v1/messages") ||
            u.endsWith("/messages")
        );
    }

    function json(body: unknown): Response {
        return new Response(typeof body === "string" ? body : JSON.stringify(body), {
            status: 200,
            headers: { "content-type": "application/json" },
        });
    }

    function sse(body: string): Response {
        return new Response(body, {
            status: 200,
            headers: { "content-type": "text/event-stream" },
        });
    }

    function anthropicMessageStreamBody(text: string): string {
        const events: Array<[string, unknown]> = [
            [
                "message_start",
                {
                    type: "message_start",
                    message: {
                        id: "msg_stub_1",
                        type: "message",
                        role: "assistant",
                        model: "claude-sonnet-4.5",
                        content: [],
                        stop_reason: null,
                        stop_sequence: null,
                        usage: { input_tokens: 5, output_tokens: 1 },
                    },
                },
            ],
            [
                "content_block_start",
                {
                    type: "content_block_start",
                    index: 0,
                    content_block: { type: "text", text: "" },
                },
            ],
            [
                "content_block_delta",
                { type: "content_block_delta", index: 0, delta: { type: "text_delta", text } },
            ],
            ["content_block_stop", { type: "content_block_stop", index: 0 }],
            [
                "message_delta",
                {
                    type: "message_delta",
                    delta: { stop_reason: "end_turn", stop_sequence: null },
                    usage: { output_tokens: 7 },
                },
            ],
            ["message_stop", { type: "message_stop" }],
        ];
        return events
            .map(([event, data]) => `event: ${event}\ndata: ${JSON.stringify(data)}\n\n`)
            .join("");
    }

    function buildNonInferenceResponse(url: string): Response {
        const u = url.toLowerCase();
        if (u.endsWith("/models")) {
            return json({
                data: [
                    {
                        id: "claude-sonnet-4.5",
                        name: "Claude Sonnet 4.5",
                        object: "model",
                        vendor: "Anthropic",
                        version: "1",
                        preview: false,
                        model_picker_enabled: true,
                        capabilities: {
                            type: "chat",
                            family: "claude-sonnet-4.5",
                            tokenizer: "o200k_base",
                            limits: { max_context_window_tokens: 200000, max_output_tokens: 8192 },
                            supports: {
                                streaming: true,
                                tool_calls: true,
                                parallel_tool_calls: true,
                                vision: true,
                            },
                        },
                    },
                ],
            });
        }
        if (u.includes("/models/session")) return json({});
        if (u.includes("/policy")) return json({ state: "enabled" });
        return json({});
    }

    function buildInferenceResponse(url: string, body: string): Response {
        const u = url.toLowerCase();
        const wantsStream = /"stream"\s*:\s*true/.test(body);
        if (u.endsWith("/messages")) {
            if (wantsStream) {
                return sse(anthropicMessageStreamBody("OK from the synthetic stream."));
            }
            return json({
                id: "msg_stub_1",
                type: "message",
                role: "assistant",
                model: "claude-sonnet-4.5",
                content: [{ type: "text", text: "OK from the synthetic stream." }],
                stop_reason: "end_turn",
                stop_sequence: null,
                usage: { input_tokens: 5, output_tokens: 7 },
            });
        }
        return json({
            id: "chatcmpl-stub-1",
            object: "chat.completion",
            created: 1,
            model: "claude-sonnet-4.5",
            choices: [
                {
                    index: 0,
                    message: { role: "assistant", content: "OK from the synthetic stream." },
                    finish_reason: "stop",
                },
            ],
            usage: { prompt_tokens: 5, completion_tokens: 7, total_tokens: 12 },
        });
    }

    function createPdfAttachment() {
        const pdfText =
            "%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\ntrailer\n<< /Root 1 0 R >>\n%%EOF\n";
        return {
            type: "blob" as const,
            data: Buffer.from(pdfText, "ascii").toString("base64"),
            displayName: "citation-source.pdf",
            mimeType: "application/pdf",
        };
    }

    function createAnthropicProvider() {
        return {
            type: "anthropic" as const,
            baseUrl: "https://anthropic-citations.invalid/v1",
            apiKey: "test-provider-key",
            modelId: "claude-sonnet-4.5",
            wireModel: "claude-sonnet-4.5",
        };
    }

    function assertAnthropicDocumentCitationsEnabled(requestBody: string) {
        const body = JSON.parse(requestBody) as {
            messages: Array<{ content: Array<Record<string, unknown>> }>;
        };
        const documentBlocks = body.messages.flatMap((message) =>
            message.content.filter((block) => block.type === "document")
        );
        expect(documentBlocks).toHaveLength(1);
        expect(documentBlocks[0].title).toBe("citation-source.pdf");
        expect(documentBlocks[0].citations).toEqual({ enabled: true });
    }

    it("should apply session limits on create", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            sessionLimits: { maxAiCredits: 30 },
        });

        const exchange = await sendAndGetNextExchange(
            session,
            "Acknowledge the current session limits."
        );
        assertSessionLimitsStatus(exchange, "30 AI credits");

        await session.disconnect();
    });

    it("should apply session limits on resume", async () => {
        const session1 = await client.createSession({ onPermissionRequest: approveAll });
        const session2 = await client.resumeSession(session1.sessionId, {
            onPermissionRequest: approveAll,
            sessionLimits: { maxAiCredits: 30 },
        });

        const exchange = await sendAndGetNextExchange(
            session2,
            "Acknowledge the current session limits."
        );
        assertSessionLimitsStatus(exchange, "30 AI credits");

        await session2.disconnect();
        await session1.disconnect();
    });

    it("should apply excluded built-in agents on create", async () => {
        const excludedAgent = "explore";
        const prompt = "What is 1+1?";

        const baselineSession = await client.createSession({ onPermissionRequest: approveAll });
        const baselineExchange = await sendAndGetNextExchange(baselineSession, prompt);
        expect(getTaskAgentTypes(baselineExchange)).toContain(excludedAgent);
        await baselineSession.disconnect();

        const excludedSession = await client.createSession({
            onPermissionRequest: approveAll,
            excludedBuiltinAgents: [excludedAgent],
        });
        const excludedExchange = await sendAndGetNextExchange(excludedSession, prompt);
        const agentTypes = getTaskAgentTypes(excludedExchange);
        expect(agentTypes.length).toBeGreaterThan(0);
        expect(agentTypes).not.toContain(excludedAgent);

        await excludedSession.disconnect();
    });

    it("should apply excluded built-in agents on resume", async () => {
        const excludedAgent = "explore";
        const session1 = await client.createSession({ onPermissionRequest: approveAll });
        const session2 = await client.resumeSession(session1.sessionId, {
            onPermissionRequest: approveAll,
            excludedBuiltinAgents: [excludedAgent],
        });

        const exchange = await sendAndGetNextExchange(session2, "What is 1+1?");
        const agentTypes = getTaskAgentTypes(exchange);
        expect(agentTypes.length).toBeGreaterThan(0);
        expect(agentTypes).not.toContain(excludedAgent);

        await session2.disconnect();
        await session1.disconnect();
    });

    it("should enable citations for Anthropic file attachments on create", async () => {
        const handler = new RecordingRequestHandler();
        const citationClient = new CopilotClient({
            connection: RuntimeConnection.forStdio({ path: process.env.COPILOT_CLI_PATH }),
            workingDirectory: workDir,
            env,
            gitHubToken: DEFAULT_GITHUB_TOKEN,
            requestHandler: handler,
        });

        await citationClient.start();
        try {
            const session = await citationClient.createSession({
                onPermissionRequest: approveAll,
                model: "claude-sonnet-4.5",
                enableCitations: true,
                provider: createAnthropicProvider(),
            });
            try {
                await session.sendAndWait({
                    prompt: "Summarize the attached PDF with citations enabled.",
                    attachments: [createPdfAttachment()],
                });
                expect(handler.inferenceRequests()).toHaveLength(1);
                assertAnthropicDocumentCitationsEnabled(handler.inferenceRequests()[0].body);
            } finally {
                await session.disconnect();
            }
        } finally {
            await citationClient.stop();
        }
    });

    it("should enable citations for Anthropic file attachments on resume", async () => {
        const handler = new RecordingRequestHandler();
        const connectionToken = "ts-citation-resume-token";
        const serverClient = new CopilotClient({
            connection: RuntimeConnection.forTcp({
                path: process.env.COPILOT_CLI_PATH,
                connectionToken,
            }),
            workingDirectory: workDir,
            env,
            gitHubToken: DEFAULT_GITHUB_TOKEN,
            requestHandler: handler,
        });

        await serverClient.start();
        try {
            const session1 = await serverClient.createSession({ onPermissionRequest: approveAll });
            const port = (serverClient as unknown as { runtimePort: number | null }).runtimePort;
            expect(port).not.toBeNull();
            const resumeClient = new CopilotClient({
                connection: RuntimeConnection.forUri(`localhost:${port}`, { connectionToken }),
            });
            try {
                const session2 = await resumeClient.resumeSession(session1.sessionId, {
                    onPermissionRequest: approveAll,
                    model: "claude-sonnet-4.5",
                    enableCitations: true,
                    provider: createAnthropicProvider(),
                });
                try {
                    await session2.sendAndWait({
                        prompt: "Summarize the attached PDF with citations enabled.",
                        attachments: [createPdfAttachment()],
                    });
                    expect(handler.inferenceRequests()).toHaveLength(1);
                    assertAnthropicDocumentCitationsEnabled(handler.inferenceRequests()[0].body);
                } finally {
                    await session2.disconnect();
                }
            } finally {
                await resumeClient.stop();
                await session1.disconnect();
            }
        } finally {
            await serverClient.stop();
        }
    });

    it("should apply instructionDirectories on session create", async () => {
        const projectDir = join(workDir, "instruction-create-project");
        const instructionDir = join(workDir, "extra-create-instructions");
        const instructionFilesDir = join(instructionDir, ".github", "instructions");
        const sentinel = "TS_CREATE_INSTRUCTION_DIRECTORIES_SENTINEL";
        await mkdir(projectDir, { recursive: true });
        await mkdir(instructionFilesDir, { recursive: true });
        await writeFile(
            join(instructionFilesDir, "extra.instructions.md"),
            `Always include ${sentinel}.`
        );

        const session = await client.createSession({
            onPermissionRequest: approveAll,
            workingDirectory: projectDir,
            instructionDirectories: [instructionDir],
        });

        await session.sendAndWait({ prompt: "What is 1+1?" });

        const exchanges = await openAiEndpoint.getExchanges();
        expect(exchanges.length).toBeGreaterThan(0);
        const sys = getSystemMessage(exchanges[exchanges.length - 1]);
        expect(sys).toContain(sentinel);

        await session.disconnect();
    });

    it("should apply instructionDirectories on session resume", async () => {
        const projectDir = join(workDir, "instruction-resume-project");
        const instructionDir = join(workDir, "extra-resume-instructions");
        const instructionFilesDir = join(instructionDir, ".github", "instructions");
        const sentinel = "TS_RESUME_INSTRUCTION_DIRECTORIES_SENTINEL";
        await mkdir(projectDir, { recursive: true });
        await mkdir(instructionFilesDir, { recursive: true });
        await writeFile(
            join(instructionFilesDir, "extra.instructions.md"),
            `Always include ${sentinel}.`
        );

        const session1 = await client.createSession({
            onPermissionRequest: approveAll,
            workingDirectory: projectDir,
        });
        const session2 = await client.resumeSession(session1.sessionId, {
            onPermissionRequest: approveAll,
            workingDirectory: projectDir,
            instructionDirectories: [instructionDir],
        });

        await session2.sendAndWait({ prompt: "What is 1+1?" });

        const exchanges = await openAiEndpoint.getExchanges();
        expect(exchanges.length).toBeGreaterThan(0);
        const sys = getSystemMessage(exchanges[exchanges.length - 1]);
        expect(sys).toContain(sentinel);

        await session2.disconnect();
        await session1.disconnect();
    });

    it("should forward clientName in user-agent", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            clientName: CLIENT_NAME,
        });

        await session.sendAndWait({ prompt: "What is 1+1?" });

        const exchanges = await openAiEndpoint.getExchanges();
        expect(exchanges.length).toBeGreaterThan(0);
        const userAgent = getHeaderString(exchanges[0].requestHeaders, "user-agent");
        expect(userAgent).toBeDefined();
        expect(userAgent).toContain(CLIENT_NAME);

        await session.disconnect();
    });

    it("should forward custom provider headers on create", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            model: "claude-sonnet-4.5",
            provider: createProxyProvider("create-provider-header"),
        });

        const message = await session.sendAndWait({ prompt: "What is 1+1?" });
        expect(message?.data.content ?? "").toContain("2");

        const exchanges = await openAiEndpoint.getExchanges();
        expect(exchanges.length).toBeGreaterThan(0);
        const auth = getHeaderString(exchanges[0].requestHeaders, "authorization");
        expect(auth).toContain("Bearer test-provider-key");
        const customHeader = getHeaderString(exchanges[0].requestHeaders, PROVIDER_HEADER_NAME);
        expect(customHeader).toContain("create-provider-header");

        await session.disconnect();
    });

    it("should forward custom provider headers on resume", async () => {
        const session1 = await client.createSession({ onPermissionRequest: approveAll });
        const sessionId = session1.sessionId;

        const session2 = await client.resumeSession(sessionId, {
            onPermissionRequest: approveAll,
            model: "claude-sonnet-4.5",
            provider: createProxyProvider("resume-provider-header"),
        });

        const message = await session2.sendAndWait({ prompt: "What is 2+2?" });
        expect(message?.data.content ?? "").toContain("4");

        const exchanges = await openAiEndpoint.getExchanges();
        expect(exchanges.length).toBeGreaterThan(0);
        const lastExchange = exchanges[exchanges.length - 1];
        const auth = getHeaderString(lastExchange.requestHeaders, "authorization");
        expect(auth).toContain("Bearer test-provider-key");
        const customHeader = getHeaderString(lastExchange.requestHeaders, PROVIDER_HEADER_NAME);
        expect(customHeader).toContain("resume-provider-header");

        await session2.disconnect();
    });

    it("should forward provider wire model", async () => {
        // Verifies that ProviderConfig.wireModel overrides the model name sent to
        // the provider API, while SessionConfig.model still drives runtime
        // configuration lookup (capabilities, prompts, reasoning behavior).
        // maxOutputTokens is also set here to confirm the SDK accepts it without
        // serialization errors; the CLI does not echo it as `max_tokens` on the
        // OpenAI-style wire request, so we don't assert on it directly (see unit
        // tests for serialization coverage).
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            model: "claude-sonnet-4.5",
            provider: {
                type: "openai",
                baseUrl: openAiEndpoint.url,
                apiKey: "test-provider-key",
                wireModel: "test-wire-model",
                maxOutputTokens: 1024,
            },
        });

        await session.sendAndWait({ prompt: "What is 1+1?" });

        const exchanges = await openAiEndpoint.getExchanges();
        expect(exchanges.length).toBe(1);
        expect(exchanges[0].request.model).toBe("test-wire-model");

        await session.disconnect();
    });

    it("should use provider model id as wire model", async () => {
        // ProviderConfig.modelId drives both the runtime resolved model AND the wire
        // model when wireModel is not specified. SessionConfig.model is intentionally
        // omitted so that modelId is the only model source.
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            provider: {
                type: "openai",
                baseUrl: openAiEndpoint.url,
                apiKey: "test-provider-key",
                modelId: "claude-sonnet-4.5",
            },
        });

        await session.sendAndWait({ prompt: "What is 1+1?" });

        const exchanges = await openAiEndpoint.getExchanges();
        expect(exchanges.length).toBe(1);
        expect(exchanges[0].request.model).toBe("claude-sonnet-4.5");

        await session.disconnect();
    });

    it("should apply workingDirectory on session resume", async () => {
        const subDir = join(workDir, "resume-subproject");
        await mkdir(subDir, { recursive: true });
        await writeFile(join(subDir, "resume-marker.txt"), "I am in the resume working directory");

        const session1 = await client.createSession({ onPermissionRequest: approveAll });
        const sessionId = session1.sessionId;

        const session2 = await client.resumeSession(sessionId, {
            onPermissionRequest: approveAll,
            workingDirectory: subDir,
        });

        const message = await session2.sendAndWait({
            prompt: "Read the file resume-marker.txt and tell me what it says",
        });
        expect(message?.data.content ?? "").toContain("resume working directory");

        await session2.disconnect();
    });

    it("should apply systemMessage on session resume", async () => {
        const session1 = await client.createSession({ onPermissionRequest: approveAll });
        const sessionId = session1.sessionId;

        const resumeInstruction = "End the response with RESUME_SYSTEM_MESSAGE_SENTINEL.";
        const session2 = await client.resumeSession(sessionId, {
            onPermissionRequest: approveAll,
            systemMessage: { mode: "append", content: resumeInstruction },
        });

        const message = await session2.sendAndWait({ prompt: "What is 1+1?" });
        expect(message?.data.content ?? "").toContain("RESUME_SYSTEM_MESSAGE_SENTINEL");

        const exchanges = await openAiEndpoint.getExchanges();
        expect(exchanges.length).toBeGreaterThan(0);
        const sys = getSystemMessage(exchanges[exchanges.length - 1]);
        expect(sys).toContain(resumeInstruction);

        await session2.disconnect();
    });

    it("should apply availableTools on session resume", async () => {
        const session1 = await client.createSession({ onPermissionRequest: approveAll });
        const sessionId = session1.sessionId;

        const session2 = await client.resumeSession(sessionId, {
            onPermissionRequest: approveAll,
            availableTools: ["view"],
        });

        try {
            await session2.send({ prompt: "What is 1+1?" });

            const exchanges = await waitForExchanges();
            const toolNames = getToolNames(exchanges[exchanges.length - 1]);
            expect(toolNames).toEqual(["view"]);
        } finally {
            await session2.disconnect();
        }
    });
});

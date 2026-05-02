import { describe, expect, it } from "vitest";
import { writeFile, mkdir } from "fs/promises";
import { join } from "path";
import { approveAll } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

describe("Session Configuration", async () => {
    const { copilotClient: client, workDir, openAiEndpoint } = await createSdkTestContext();

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

        await session2.sendAndWait({ prompt: "What is 1+1?" });

        const exchanges = await openAiEndpoint.getExchanges();
        expect(exchanges.length).toBeGreaterThan(0);
        const toolNames = getToolNames(exchanges[exchanges.length - 1]);
        expect(toolNames).toEqual(["view"]);

        await session2.disconnect();
    });
});

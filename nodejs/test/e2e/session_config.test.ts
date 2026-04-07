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
});

import { describe, expect, it } from "vitest";
import { writeFile, mkdir } from "fs/promises";
import { join } from "path";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

describe("Session Configuration", async () => {
    const { copilotClient: client, workDir } = await createSdkTestContext();

    it("should use workingDirectory for tool execution", async () => {
        const subDir = join(workDir, "subproject");
        await mkdir(subDir, { recursive: true });
        await writeFile(join(subDir, "marker.txt"), "I am in the subdirectory");

        const session = await client.createSession({
            workingDirectory: subDir,
        });

        const assistantMessage = await session.sendAndWait({
            prompt: "Read the file marker.txt and tell me what it says",
        });
        expect(assistantMessage?.data.content).toContain("subdirectory");

        await session.destroy();
    });

    it("should create session with custom provider config", async () => {
        const session = await client.createSession({
            provider: {
                baseUrl: "https://api.example.com/v1",
                apiKey: "test-key",
            },
        });

        expect(session.sessionId).toMatch(/^[a-f0-9-]+$/);

        try {
            await session.destroy();
        } catch {
            // destroy may fail since the provider is fake
        }
    });

    it("should accept message attachments", async () => {
        await writeFile(join(workDir, "attached.txt"), "This file is attached");

        const session = await client.createSession();

        await session.send({
            prompt: "Summarize the attached file",
            attachments: [{ type: "file", path: join(workDir, "attached.txt") }],
        });

        // Just verify send doesn't throw — attachment support varies by runtime
        await session.destroy();
    });
});

import * as fs from "node:fs";
import { describe, expect, it, onTestFinished } from "vitest";
import { ParsedHttpExchange } from "../../../test/harness/replayingCapiProxy.js";
import { CopilotClient } from "../../src/index.js";
import { CLI_PATH, createSdkTestContext } from "./harness/sdkTestContext.js";
import { getFinalAssistantMessage } from "./harness/sdkTestHelper.js";

describe("Sessions", async () => {
    const { copilotClient: client, openAiEndpoint, homeDir } = await createSdkTestContext();

    it("should create and destroy sessions", async () => {
        const session = await client.createSession({ model: "fake-test-model" });
        expect(session.sessionId).toMatch(/^[a-f0-9-]+$/);

        expect(await session.getMessages()).toMatchObject([
            {
                type: "session.start",
                data: { sessionId: session.sessionId, selectedModel: "fake-test-model" },
            },
        ]);

        await session.destroy();
        await expect(() => session.getMessages()).rejects.toThrow(/Session not found/);
    });

    it("should have stateful conversation", async () => {
        const session = await client.createSession();
        await session.send({ prompt: "What is 1+1?" });
        const assistantMessage = await getFinalAssistantMessage(session);
        expect(assistantMessage.data.content).toContain("2");

        await session.send({ prompt: "Now if you double that, what do you get?" });
        const secondAssistantMessage = await getFinalAssistantMessage(session);
        expect(secondAssistantMessage.data.content).toContain("4");
    });

    it("should create a session with appended systemMessage config", async () => {
        const systemMessageSuffix = "End each response with the phrase 'Have a nice day!'";
        const session = await client.createSession({
            systemMessage: {
                mode: "append",
                content: systemMessageSuffix,
            },
        });

        await session.send({ prompt: "What is your full name?" });
        const assistantMessage = await getFinalAssistantMessage(session);
        expect(assistantMessage.data.content).toContain("GitHub");
        expect(assistantMessage.data.content).toContain("Have a nice day!");

        // Also validate the underlying traffic
        const traffic = await openAiEndpoint.getExchanges();
        const systemMessage = getSystemMessage(traffic[0]);
        expect(systemMessage).toContain("GitHub");
        expect(systemMessage).toContain(systemMessageSuffix);
    });

    it("should create a session with replaced systemMessage config", async () => {
        const testSystemMessage = "You are an assistant called Testy McTestface. Reply succinctly.";
        const session = await client.createSession({
            systemMessage: { mode: "replace", content: testSystemMessage },
        });

        await session.send({ prompt: "What is your full name?" });
        const assistantMessage = await getFinalAssistantMessage(session);
        expect(assistantMessage.data.content).not.toContain("GitHub");
        expect(assistantMessage.data.content).toContain("Testy");

        // Also validate the underlying traffic
        const traffic = await openAiEndpoint.getExchanges();
        const systemMessage = getSystemMessage(traffic[0]);
        expect(systemMessage).toEqual(testSystemMessage); // Exact match
    });

    it("should create a session with availableTools", async () => {
        const session = await client.createSession({
            availableTools: ["view", "edit"],
        });

        await session.send({ prompt: "What is 1+1?" });
        await getFinalAssistantMessage(session);

        // It only tells the model about the specified tools and no others
        const traffic = await openAiEndpoint.getExchanges();
        expect(traffic[0].request.tools).toMatchObject([
            { function: { name: "view" } },
            { function: { name: "edit" } },
        ]);
    });

    it("should create a session with excludedTools", async () => {
        const session = await client.createSession({
            excludedTools: ["view"],
        });

        await session.send({ prompt: "What is 1+1?" });
        await getFinalAssistantMessage(session);

        // It has other tools, but not the one we excluded
        const traffic = await openAiEndpoint.getExchanges();
        const functionNames = traffic[0].request.tools?.map(
            (t) => (t as { function: { name: string } }).function.name
        );
        expect(functionNames).toContain("edit");
        expect(functionNames).toContain("grep");
        expect(functionNames).not.toContain("view");
    });

    // TODO: This test shows there's a race condition inside client.ts. If createSession is called
    // concurrently and autoStart is on, it may start multiple child processes. This needs to be fixed.
    // Right now it manifests as being unable to delete the temp directories during afterAll even though
    // we stopped all the clients (one or more child processes were left orphaned).
    it.skip("should handle multiple concurrent sessions", async () => {
        const [s1, s2, s3] = await Promise.all([
            client.createSession(),
            client.createSession(),
            client.createSession(),
        ]);

        // All sessions should have unique IDs
        const distinctSessionIds = new Set([s1.sessionId, s2.sessionId, s3.sessionId]);
        expect(distinctSessionIds.size).toBe(3);

        // All are connected
        for (const s of [s1, s2, s3]) {
            expect(await s.getMessages()).toMatchObject([
                {
                    type: "session.start",
                    data: { sessionId: s.sessionId },
                },
            ]);
        }

        // All can be destroyed
        await Promise.all([s1.destroy(), s2.destroy(), s3.destroy()]);
        for (const s of [s1, s2, s3]) {
            await expect(() => s.getMessages()).rejects.toThrow(/Session not found/);
        }
    });

    it("should resume a session using the same client", async () => {
        // Create initial session
        const session1 = await client.createSession();
        const sessionId = session1.sessionId;
        await session1.send({ prompt: "What is 1+1?" });
        const answer = await getFinalAssistantMessage(session1);
        expect(answer.data.content).toContain("2");

        // Resume using the same client
        const session2 = await client.resumeSession(sessionId);
        expect(session2.sessionId).toBe(sessionId);
        const answer2 = await getFinalAssistantMessage(session2);
        expect(answer2.data.content).toContain("2");
    });

    it("should resume a session using a new client", async () => {
        // Create initial session
        const session1 = await client.createSession();
        const sessionId = session1.sessionId;
        await session1.send({ prompt: "What is 1+1?" });
        const answer = await getFinalAssistantMessage(session1);
        expect(answer.data.content).toContain("2");

        // Resume using a new client
        const newClient = new CopilotClient({
            cliPath: CLI_PATH,
            env: {
                ...process.env,
                XDG_CONFIG_HOME: homeDir,
                XDG_STATE_HOME: homeDir,
            },
        });

        onTestFinished(() => newClient.forceStop());
        const session2 = await newClient.resumeSession(sessionId);
        expect(session2.sessionId).toBe(sessionId);

        // TODO: There's an inconsistency here. When resuming with a new client, we don't see
        // the session.idle message in the history, which means we can't use getFinalAssistantMessage.

        const messages = await session2.getMessages();
        expect(messages).toContainEqual(expect.objectContaining({ type: "user.message" }));
        expect(messages).toContainEqual(expect.objectContaining({ type: "session.resume" }));
    });

    it("should throw error when resuming non-existent session", async () => {
        await expect(client.resumeSession("non-existent-session-id")).rejects.toThrow();
    });

    it("should create session with custom tool", async () => {
        const session = await client.createSession({
            tools: [
                {
                    name: "get_secret_number",
                    description: "Gets the secret number",
                    parameters: {
                        type: "object",
                        properties: {
                            key: { type: "string", description: "Key" },
                        },
                        required: ["key"],
                    },
                    // Shows that raw JSON schemas still work - Zod is optional
                    handler: async (args: { key: string }) => {
                        return {
                            textResultForLlm: args.key === "ALPHA" ? "54321" : "unknown",
                            resultType: "success" as const,
                        };
                    },
                },
            ],
        });

        await session.send({ prompt: "What is the secret number for key ALPHA?" });
        const session1Answer = await getFinalAssistantMessage(session);
        expect(session1Answer.data.content).toContain("54321");
    });

    it("should resume session with a custom provider", async () => {
        const session = await client.createSession();
        const sessionId = session.sessionId;

        // Resume the session with a provider
        const session2 = await client.resumeSession(sessionId, {
            provider: {
                type: "openai",
                baseUrl: "https://api.openai.com/v1",
                apiKey: "fake-key",
            },
        });

        expect(session2.sessionId).toBe(sessionId);
    });

    it("should abort a session", async () => {
        const session = await client.createSession();

        // Send a message that will take some time to process
        await session.send({ prompt: "What is 1+1?" });

        // Abort the session immediately
        await session.abort();

        // The session should still be alive and usable after abort
        const messages = await session.getMessages();
        expect(messages.length).toBeGreaterThan(0);

        // We should be able to send another message
        await session.send({ prompt: "What is 2+2?" });
        const answer = await getFinalAssistantMessage(session);
        expect(answer.data.content).toContain("4");
    });

    it("should receive streaming delta events when streaming is enabled", async () => {
        const session = await client.createSession({
            streaming: true,
        });

        const deltaContents: string[] = [];
        let _finalMessage: string | undefined;

        // Set up event listener before sending
        const unsubscribe = session.on((event) => {
            if (event.type === "assistant.message_delta") {
                const delta = (event.data as { deltaContent?: string }).deltaContent;
                if (delta) {
                    deltaContents.push(delta);
                }
            } else if (event.type === "assistant.message") {
                _finalMessage = event.data.content;
            }
        });

        await session.send({ prompt: "What is 2+2?" });
        const assistantMessage = await getFinalAssistantMessage(session);

        unsubscribe();

        // Should have received delta events
        expect(deltaContents.length).toBeGreaterThan(0);

        // Accumulated deltas should equal the final message
        const accumulated = deltaContents.join("");
        expect(accumulated).toBe(assistantMessage.data.content);

        // Final message should contain the answer
        expect(assistantMessage.data.content).toContain("4");
    });

    it("should pass streaming option to session creation", async () => {
        // Verify that the streaming option is accepted without errors
        const session = await client.createSession({
            streaming: true,
        });

        expect(session.sessionId).toMatch(/^[a-f0-9-]+$/);

        // Session should still work normally
        await session.send({ prompt: "What is 1+1?" });
        const assistantMessage = await getFinalAssistantMessage(session);
        expect(assistantMessage.data.content).toContain("2");
    });

    it("should receive session events", async () => {
        const session = await client.createSession();
        const receivedEvents: Array<{ type: string }> = [];
        let idleResolve: () => void;
        const idlePromise = new Promise<void>((resolve) => {
            idleResolve = resolve;
        });

        session.on((event) => {
            receivedEvents.push(event);
            if (event.type === "session.idle") {
                idleResolve();
            }
        });

        // Send a message to trigger events
        await session.send({ prompt: "What is 100+200?" });

        // Wait for session to become idle
        await Promise.race([
            idlePromise,
            new Promise((_, reject) => setTimeout(() => reject(new Error("Timeout")), 60000)),
        ]);

        // Should have received multiple events
        expect(receivedEvents.length).toBeGreaterThan(0);
        expect(receivedEvents.some((e) => e.type === "user.message")).toBe(true);
        expect(receivedEvents.some((e) => e.type === "assistant.message")).toBe(true);
        expect(receivedEvents.some((e) => e.type === "session.idle")).toBe(true);

        // Verify the assistant response contains the expected answer
        const assistantMessage = await getFinalAssistantMessage(session);
        expect(assistantMessage.data.content).toContain("300");
    });

    it("should create session with custom config dir", async () => {
        const customConfigDir = `${homeDir}/custom-config`;
        const session = await client.createSession({
            configDir: customConfigDir,
        });

        expect(session.sessionId).toMatch(/^[a-f0-9-]+$/);

        // Verify config.json was written to the custom config dir
        const configPath = `${customConfigDir}/github-copilot/config.json`;
        expect(fs.existsSync(configPath)).toBe(true);
        const configContent = JSON.parse(fs.readFileSync(configPath, "utf-8"));
        expect(configContent).toHaveProperty("sessionId", session.sessionId);

        // Session should work normally with custom config dir
        await session.send({ prompt: "What is 1+1?" });
        const assistantMessage = await getFinalAssistantMessage(session);
        expect(assistantMessage.data.content).toContain("2");
    });
});

function getSystemMessage(exchange: ParsedHttpExchange): string | undefined {
    const systemMessage = exchange.request.messages.find((m) => m.role === "system") as
        | { role: "system"; content: string }
        | undefined;
    return systemMessage?.content;
}

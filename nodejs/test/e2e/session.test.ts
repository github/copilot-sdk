import { describe, expect, it, onTestFinished } from "vitest";
import { ParsedHttpExchange } from "../../../test/harness/replayingCapiProxy.js";
import { CopilotClient } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";
import { getFinalAssistantMessage, getNextEventOfType } from "./harness/sdkTestHelper.js";

describe("Sessions", async () => {
    const { copilotClient: client, openAiEndpoint, homeDir, env } = await createSdkTestContext();

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
        const assistantMessage = await session.sendAndWait({ prompt: "What is 1+1?" });
        expect(assistantMessage?.data.content).toContain("2");

        const secondAssistantMessage = await session.sendAndWait({
            prompt: "Now if you double that, what do you get?",
        });
        expect(secondAssistantMessage?.data.content).toContain("4");
    });

    it("should create a session with appended systemMessage config", async () => {
        const systemMessageSuffix = "End each response with the phrase 'Have a nice day!'";
        const session = await client.createSession({
            systemMessage: {
                mode: "append",
                content: systemMessageSuffix,
            },
        });

        const assistantMessage = await session.sendAndWait({ prompt: "What is your full name?" });
        expect(assistantMessage?.data.content).toContain("GitHub");
        expect(assistantMessage?.data.content).toContain("Have a nice day!");

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

        const assistantMessage = await session.sendAndWait({ prompt: "What is your full name?" });
        expect(assistantMessage?.data.content).not.toContain("GitHub");
        expect(assistantMessage?.data.content).toContain("Testy");

        // Also validate the underlying traffic
        const traffic = await openAiEndpoint.getExchanges();
        const systemMessage = getSystemMessage(traffic[0]);
        expect(systemMessage).toEqual(testSystemMessage); // Exact match
    });

    it("should create a session with availableTools", async () => {
        const session = await client.createSession({
            availableTools: ["view", "edit"],
        });

        await session.sendAndWait({ prompt: "What is 1+1?" });

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

        await session.sendAndWait({ prompt: "What is 1+1?" });

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
        const answer = await session1.sendAndWait({ prompt: "What is 1+1?" });
        expect(answer?.data.content).toContain("2");

        // Resume using the same client
        const session2 = await client.resumeSession(sessionId);
        expect(session2.sessionId).toBe(sessionId);
        const messages = await session2.getMessages();
        const assistantMessages = messages.filter((m) => m.type === "assistant.message");
        expect(assistantMessages[assistantMessages.length - 1].data.content).toContain("2");
    });

    it("should resume a session using a new client", async () => {
        // Create initial session
        const session1 = await client.createSession();
        const sessionId = session1.sessionId;
        const answer = await session1.sendAndWait({ prompt: "What is 1+1?" });
        expect(answer?.data.content).toContain("2");

        // Resume using a new client
        const newClient = new CopilotClient({
            env,
            githubToken: process.env.CI === "true" ? "fake-token-for-e2e-tests" : undefined,
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

        const answer = await session.sendAndWait({
            prompt: "What is the secret number for key ALPHA?",
        });
        expect(answer?.data.content).toContain("54321");
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

        // Set up event listeners BEFORE sending to avoid race conditions
        const nextToolCallStart = getNextEventOfType(session, "tool.execution_start");
        const nextSessionIdle = getNextEventOfType(session, "session.idle");

        await session.send({
            prompt: "run the shell command 'sleep 100' (note this works on both bash and PowerShell)",
        });

        // Abort once we see a tool execution start
        await nextToolCallStart;
        await session.abort();
        await nextSessionIdle;

        // The session should still be alive and usable after abort
        const messages = await session.getMessages();
        expect(messages.length).toBeGreaterThan(0);
        expect(messages.some((m) => m.type === "abort")).toBe(true);

        // We should be able to send another message
        const answer = await session.sendAndWait({ prompt: "What is 2+2?" });
        expect(answer?.data.content).toContain("4");
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

        const assistantMessage = await session.sendAndWait({ prompt: "What is 2+2?" });

        unsubscribe();

        // Should have received delta events
        expect(deltaContents.length).toBeGreaterThan(0);

        // Accumulated deltas should equal the final message
        const accumulated = deltaContents.join("");
        expect(accumulated).toBe(assistantMessage?.data.content);

        // Final message should contain the answer
        expect(assistantMessage?.data.content).toContain("4");
    });

    it("should pass streaming option to session creation", async () => {
        // Verify that the streaming option is accepted without errors
        const session = await client.createSession({
            streaming: true,
        });

        expect(session.sessionId).toMatch(/^[a-f0-9-]+$/);

        // Session should still work normally
        const assistantMessage = await session.sendAndWait({ prompt: "What is 1+1?" });
        expect(assistantMessage?.data.content).toContain("2");
    });

    it("should receive session events", async () => {
        const session = await client.createSession();
        const receivedEvents: Array<{ type: string }> = [];

        session.on((event) => {
            receivedEvents.push(event);
        });

        // Send a message and wait for completion
        const assistantMessage = await session.sendAndWait({ prompt: "What is 100+200?" });

        // Should have received multiple events
        expect(receivedEvents.length).toBeGreaterThan(0);
        expect(receivedEvents.some((e) => e.type === "user.message")).toBe(true);
        expect(receivedEvents.some((e) => e.type === "assistant.message")).toBe(true);
        expect(receivedEvents.some((e) => e.type === "session.idle")).toBe(true);

        // Verify the assistant response contains the expected answer
        expect(assistantMessage?.data.content).toContain("300");
    });

    it("should create session with custom config dir", async () => {
        const customConfigDir = `${homeDir}/custom-config`;
        const session = await client.createSession({
            configDir: customConfigDir,
        });

        expect(session.sessionId).toMatch(/^[a-f0-9-]+$/);

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

describe("Send Blocking Behavior", async () => {
    // Tests for Issue #17: send() should return immediately, not block until turn completes
    const { copilotClient: client } = await createSdkTestContext();

    it("send returns immediately while events stream in background", async () => {
        const session = await client.createSession();

        const events: string[] = [];
        session.on((event) => {
            events.push(event.type);
        });

        // Use a slow command so we can verify send() returns before completion
        await session.send({ prompt: "Run 'sleep 2 && echo done'" });

        // send() should return before turn completes (no session.idle yet)
        expect(events).not.toContain("session.idle");

        // Wait for turn to complete
        const message = await getFinalAssistantMessage(session);

        expect(message.data.content).toContain("done");
        expect(events).toContain("session.idle");
        expect(events).toContain("assistant.message");
    });

    it("sendAndWait blocks until session.idle and returns final assistant message", async () => {
        const session = await client.createSession();

        const events: string[] = [];
        session.on((event) => {
            events.push(event.type);
        });

        const response = await session.sendAndWait({ prompt: "What is 2+2?" });

        expect(response).toBeDefined();
        expect(response?.type).toBe("assistant.message");
        expect(response?.data.content).toContain("4");
        expect(events).toContain("session.idle");
        expect(events).toContain("assistant.message");
    });

    // This test validates client-side timeout behavior.
    // The snapshot has no assistant response since we expect timeout before completion.
    it("sendAndWait throws on timeout", async () => {
        const session = await client.createSession();

        // Use a slow command to ensure timeout triggers before completion
        await expect(
            session.sendAndWait({ prompt: "Run 'sleep 2 && echo done'" }, 100)
        ).rejects.toThrow(/Timeout after 100ms/);
    });
});

import { describe, expect, it } from "vitest";
import { SessionEvent, approveAll } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";
import { getNextEventOfType } from "./harness/sdkTestHelper.js";

describe("Compaction", async () => {
    const { copilotClient: client } = await createSdkTestContext();

    it("should trigger compaction with low threshold and emit events", async () => {
        // Create session with very low compaction thresholds to trigger compaction quickly
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            infiniteSessions: {
                enabled: true,
                // Trigger background compaction at 0.5% context usage (~1000 tokens)
                backgroundCompactionThreshold: 0.005,
                // Block at 1% to ensure compaction runs
                bufferExhaustionThreshold: 0.01,
            },
        });

        // The first prompt leaves the session below the compaction processor's minimum
        // message count. The second prompt is therefore the first deterministic point
        // at which low thresholds can trigger compaction. Register event waiters before
        // any prompts are sent so we never miss the events.
        const compactionStartedP = getNextEventOfType(session, "session.compaction_start");
        // Wait specifically for a *successful* compaction_complete so that any transient
        // failed compaction event the daemon may emit before a successful retry is ignored
        // (mirrors the dotnet/rust references).
        const compactionCompletedP = new Promise<
            Extract<SessionEvent, { type: "session.compaction_complete" }>
        >((resolve, reject) => {
            const unsubscribe = session.on((event) => {
                if (event.type === "session.compaction_complete" && event.data.success) {
                    unsubscribe();
                    resolve(event);
                } else if (event.type === "session.error") {
                    unsubscribe();
                    reject(new Error(`${event.data.message}\n${event.data.stack}`));
                }
            });
        });

        await session.sendAndWait({
            prompt: "Tell me a story about a dragon. Be detailed.",
        });
        await session.sendAndWait({
            prompt: "Continue the story with more details about the dragon's castle.",
        });

        const [startEvent, completeEvent] = await Promise.all([
            compactionStartedP as Promise<
                Extract<SessionEvent, { type: "session.compaction_start" }>
            >,
            compactionCompletedP,
        ]);

        expect(startEvent.data.conversationTokens ?? 0).toBeGreaterThan(0);
        expect(completeEvent.data.success).toBe(true);
        expect(completeEvent.data.compactionTokensUsed).toBeDefined();
        expect(completeEvent.data.compactionTokensUsed?.inputTokens ?? 0).toBeGreaterThan(0);
        const summary = completeEvent.data.summaryContent ?? "";
        expect(summary).toContain("<overview>");
        expect(summary).toContain("<history>");
        expect(summary).toContain("<checkpoint_title>");

        await session.sendAndWait({
            prompt: "Now describe the dragon's treasure in great detail.",
        });

        // Verify the session still works after compaction
        const answer = await session.sendAndWait({ prompt: "What was the story about?" });
        const content = (answer?.data.content ?? "").toLowerCase();
        // Should remember it was about a dragon (context preserved via summary)
        expect(content).toContain("kaedrith");
        expect(content).toContain("dragon");
    }, 120000);

    it("should not emit compaction events when infinite sessions disabled", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            infiniteSessions: {
                enabled: false,
            },
        });

        const compactionEvents: SessionEvent[] = [];
        session.on((event) => {
            if (
                event.type === "session.compaction_start" ||
                event.type === "session.compaction_complete"
            ) {
                compactionEvents.push(event);
            }
        });

        await session.sendAndWait({ prompt: "What is 2+2?" });

        // Should not have any compaction events when disabled
        expect(compactionEvents.length).toBe(0);
    });
});

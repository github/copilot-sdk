import { describe, expect, it } from "vitest";
import { SessionEvent } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

describe("Compaction", async () => {
    const { copilotClient: client } = await createSdkTestContext();

    it("should trigger compaction with low threshold and emit events", async () => {
        // Create session with very low compaction thresholds to trigger compaction quickly
        const session = await client.createSession({
            infiniteSessions: {
                enabled: true,
                // Trigger background compaction at 0.5% context usage (~1000 tokens)
                backgroundCompactionThreshold: 0.005,
                // Block at 1% to ensure compaction runs
                bufferExhaustionThreshold: 0.01,
            },
        });

        const events: SessionEvent[] = [];
        session.on((event) => {
            events.push(event);
        });

        // Send multiple messages to fill up the context window
        // With such low thresholds, even a few messages should trigger compaction
        await session.sendAndWait({
            prompt: "Tell me a long story about a dragon. Be very detailed.",
        });
        await session.sendAndWait({
            prompt: "Continue the story with more details about the dragon's castle.",
        });
        await session.sendAndWait({
            prompt: "Now describe the dragon's treasure in great detail.",
        });

        // Check for compaction events
        const compactionStartEvents = events.filter((e) => e.type === "session.compaction_start");
        const compactionCompleteEvents = events.filter(
            (e) => e.type === "session.compaction_complete"
        );

        // Should have triggered compaction at least once
        expect(compactionStartEvents.length).toBeGreaterThanOrEqual(1);
        expect(compactionCompleteEvents.length).toBeGreaterThanOrEqual(1);

        // Compaction should have succeeded
        const lastCompactionComplete =
            compactionCompleteEvents[compactionCompleteEvents.length - 1];
        expect(lastCompactionComplete.data.success).toBe(true);

        // Should have removed some tokens
        if (lastCompactionComplete.data.tokensRemoved !== undefined) {
            expect(lastCompactionComplete.data.tokensRemoved).toBeGreaterThan(0);
        }

        // Verify the session still works after compaction
        const answer = await session.sendAndWait({ prompt: "What was the story about?" });
        expect(answer?.data.content).toBeDefined();
        // Should remember it was about a dragon (context preserved via summary)
        expect(answer?.data.content?.toLowerCase()).toContain("dragon");
    }, 120000);

    it("should not emit compaction events when infinite sessions disabled", async () => {
        const session = await client.createSession({
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

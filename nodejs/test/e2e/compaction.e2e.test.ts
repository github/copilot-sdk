import { describe, expect, it } from "vitest";
import { approveAll, type CopilotSession, type SessionEvent } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

const compactionTimeoutMs = 60_000;

function getNextSessionEvent<TEventType extends SessionEvent["type"]>(
    session: CopilotSession,
    eventType: TEventType,
    description: string,
    predicate: (event: Extract<SessionEvent, { type: TEventType }>) => boolean = () => true
): Promise<Extract<SessionEvent, { type: TEventType }>> {
    return new Promise((resolve, reject) => {
        let unsubscribe: () => void = () => {};
        const timeout = setTimeout(() => {
            unsubscribe();
            reject(new Error(`Timed out waiting for ${description}`));
        }, compactionTimeoutMs);

        unsubscribe = session.on((event) => {
            if (event.type === eventType) {
                const typedEvent = event as Extract<SessionEvent, { type: TEventType }>;
                if (predicate(typedEvent)) {
                    clearTimeout(timeout);
                    unsubscribe();
                    resolve(typedEvent);
                }
            } else if (event.type === "session.error") {
                clearTimeout(timeout);
                unsubscribe();
                reject(new Error(`${event.data.message}\n${event.data.stack}`));
            }
        });
    });
}

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
        const compactionStartedP = getNextSessionEvent(
            session,
            "session.compaction_start",
            "session.compaction_start"
        );
        // Wait specifically for a *successful* compaction_complete so that any transient
        // failed compaction event the daemon may emit before a successful retry is ignored
        // (mirrors the dotnet/rust references).
        const compactionCompletedP = getNextSessionEvent(
            session,
            "session.compaction_complete",
            "successful session.compaction_complete",
            (event) => event.data.success
        );

        await session.sendAndWait({
            prompt: "Tell me a story about a dragon. Be detailed.",
        });
        await session.sendAndWait({
            prompt: "Continue the story with more details about the dragon's castle.",
        });

        const [startEvent, completeEvent] = await Promise.all([
            compactionStartedP,
            compactionCompletedP,
        ]);

        expect(startEvent.data.conversationTokens ?? 0).toBeGreaterThan(0);
        expect(completeEvent.data.success).toBe(true);
        expect(completeEvent.data.compactionTokensUsed).toBeDefined();
        expect(completeEvent.data.compactionTokensUsed?.inputTokens ?? 0).toBeGreaterThan(0);
        const summary = (completeEvent.data.summaryContent ?? "").toLowerCase();
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

    it("should return empty handoff summary for fresh session", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        try {
            const result = await session.rpc.history.summarizeForHandoff();
            expect(result.summary).toBe("");
        } finally {
            await session.disconnect();
        }
    });

    it("should summarize for handoff after non-ephemeral log event", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        try {
            await session.log("handoff summary log coverage");
            const result = await session.rpc.history.summarizeForHandoff();
            expect(typeof result.summary).toBe("string");
        } finally {
            await session.disconnect();
        }
    });

    it("should report no-op when cancelling compaction without in-flight work", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        try {
            const backgroundResult = await session.rpc.history.cancelBackgroundCompaction();
            const manualResult = await session.rpc.history.abortManualCompaction();

            expect(backgroundResult.cancelled).toBe(false);
            expect(manualResult.aborted).toBe(false);
        } finally {
            await session.disconnect();
        }
    });
});

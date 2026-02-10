/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.github.copilot.sdk.events.AbstractSessionEvent;
import com.github.copilot.sdk.events.AssistantMessageEvent;
import com.github.copilot.sdk.events.SessionCompactionCompleteEvent;
import com.github.copilot.sdk.events.SessionCompactionStartEvent;
import com.github.copilot.sdk.json.InfiniteSessionConfig;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.SessionConfig;

/**
 * Tests for compaction and infinite sessions functionality.
 *
 * <p>
 * These tests verify that sessions can trigger compaction with low thresholds
 * and emit appropriate events. Snapshots are stored in
 * test/snapshots/compaction/.
 * </p>
 */
public class CompactionTest {

    private static E2ETestContext ctx;

    @BeforeAll
    static void setup() throws Exception {
        ctx = E2ETestContext.create();
    }

    @AfterAll
    static void teardown() throws Exception {
        if (ctx != null) {
            ctx.close();
        }
    }

    /**
     * Verifies that compaction is triggered with low threshold and emits events.
     *
     * @see Snapshot:
     *      compaction/should_trigger_compaction_with_low_threshold_and_emit_events
     */
    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void testShouldTriggerCompactionWithLowThresholdAndEmitEvents() throws Exception {
        ctx.configureForTest("compaction", "should_trigger_compaction_with_low_threshold_and_emit_events");

        // Create session with very low compaction thresholds to trigger compaction
        // quickly
        var infiniteConfig = new InfiniteSessionConfig().setEnabled(true)
                // Trigger background compaction at 0.5% context usage (~1000 tokens)
                .setBackgroundCompactionThreshold(0.005)
                // Block at 1% to ensure compaction runs
                .setBufferExhaustionThreshold(0.01);

        var config = new SessionConfig().setInfiniteSessions(infiniteConfig);

        var events = new ArrayList<AbstractSessionEvent>();

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(config).get();

            session.on(event -> events.add(event));

            // Send multiple messages to fill up the context window
            // With such low thresholds, even a few messages should trigger compaction
            session.sendAndWait(
                    new MessageOptions().setPrompt("Tell me a long story about a dragon. Be very detailed."))
                    .get(60, TimeUnit.SECONDS);
            session.sendAndWait(
                    new MessageOptions().setPrompt("Continue the story with more details about the dragon's castle."))
                    .get(60, TimeUnit.SECONDS);
            session.sendAndWait(new MessageOptions().setPrompt("Now describe the dragon's treasure in great detail."))
                    .get(60, TimeUnit.SECONDS);

            // Check for compaction events
            long compactionStartCount = events.stream().filter(e -> e instanceof SessionCompactionStartEvent).count();
            long compactionCompleteCount = events.stream().filter(e -> e instanceof SessionCompactionCompleteEvent)
                    .count();

            // Should have triggered compaction at least once
            assertTrue(compactionStartCount >= 1,
                    "Should have triggered compaction start at least once, got: " + compactionStartCount);
            assertTrue(compactionCompleteCount >= 1,
                    "Should have triggered compaction complete at least once, got: " + compactionCompleteCount);

            // Compaction should have succeeded
            SessionCompactionCompleteEvent lastCompactionComplete = events.stream()
                    .filter(e -> e instanceof SessionCompactionCompleteEvent)
                    .map(e -> (SessionCompactionCompleteEvent) e).reduce((first, second) -> second).orElse(null);

            assertNotNull(lastCompactionComplete);
            assertTrue(lastCompactionComplete.getData().success(), "Compaction should have succeeded");

            // Verify the session still works after compaction
            AssistantMessageEvent answer = session
                    .sendAndWait(new MessageOptions().setPrompt("What was the story about?")).get(60, TimeUnit.SECONDS);

            assertNotNull(answer);
            assertNotNull(answer.getData().content());
            // Should remember it was about a dragon (context preserved via summary)
            assertTrue(answer.getData().content().toLowerCase().contains("dragon"),
                    "Should remember the story was about a dragon: " + answer.getData().content());

            session.close();
        }
    }

    /**
     * Verifies that compaction events are not emitted when infinite sessions is
     * disabled.
     *
     * @see Snapshot:
     *      compaction/should_not_emit_compaction_events_when_infinite_sessions_disabled
     */
    @Test
    void testShouldNotEmitCompactionEventsWhenInfiniteSessionsDisabled() throws Exception {
        ctx.configureForTest("compaction", "should_not_emit_compaction_events_when_infinite_sessions_disabled");

        var infiniteConfig = new InfiniteSessionConfig().setEnabled(false);

        var config = new SessionConfig().setInfiniteSessions(infiniteConfig);

        var compactionEvents = new ArrayList<AbstractSessionEvent>();

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(config).get();

            session.on(event -> {
                if (event instanceof SessionCompactionStartEvent || event instanceof SessionCompactionCompleteEvent) {
                    compactionEvents.add(event);
                }
            });

            session.sendAndWait(new MessageOptions().setPrompt("What is 2+2?")).get(60, TimeUnit.SECONDS);

            // Should not have any compaction events when disabled
            assertEquals(0, compactionEvents.size(),
                    "Should not have any compaction events when infinite sessions is disabled");

            session.close();
        }
    }
}

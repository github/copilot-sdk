/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.sdk.events.AbstractSessionEvent;
import com.github.copilot.sdk.events.AssistantMessageDeltaEvent;
import com.github.copilot.sdk.events.AssistantMessageEvent;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.PermissionHandler;
import com.github.copilot.sdk.json.ResumeSessionConfig;
import com.github.copilot.sdk.json.SessionConfig;

/**
 * E2E tests for streaming fidelity — verifying that delta events are produced
 * when streaming is enabled and absent when it is disabled.
 *
 * <p>
 * Snapshots are stored in {@code test/snapshots/streaming_fidelity/}.
 * </p>
 */
public class StreamingFidelityTest {

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
     * Verifies that assistant.message_delta events are produced when streaming is
     * enabled.
     *
     * @see Snapshot:
     *      streaming_fidelity/should_produce_delta_events_when_streaming_is_enabled
     */
    @Test
    void testShouldProduceDeltaEventsWhenStreamingIsEnabled() throws Exception {
        ctx.configureForTest("streaming_fidelity", "should_produce_delta_events_when_streaming_is_enabled");

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(
                    new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setStreaming(true)).get();

            List<AbstractSessionEvent> events = new ArrayList<>();
            session.on(events::add);

            session.sendAndWait(new MessageOptions().setPrompt("Count from 1 to 5, separated by commas.")).get(60,
                    TimeUnit.SECONDS);

            List<String> types = events.stream().map(AbstractSessionEvent::getType).toList();

            // Should have streaming deltas before the final message
            List<AssistantMessageDeltaEvent> deltaEvents = events.stream()
                    .filter(e -> e instanceof AssistantMessageDeltaEvent).map(e -> (AssistantMessageDeltaEvent) e)
                    .toList();
            assertFalse(deltaEvents.isEmpty(), "Should have received delta events when streaming is enabled");

            // Deltas should have content
            for (AssistantMessageDeltaEvent delta : deltaEvents) {
                assertFalse(delta.getData().deltaContent() == null || delta.getData().deltaContent().isEmpty(),
                        "Delta event should have content");
            }

            // Should still have a final assistant.message
            assertTrue(types.contains("assistant.message"), "Should have a final assistant.message event");

            // Deltas should come before the final message
            int firstDeltaIdx = types.indexOf("assistant.message_delta");
            int lastAssistantIdx = types.lastIndexOf("assistant.message");
            assertTrue(firstDeltaIdx < lastAssistantIdx, "Delta events should come before the final assistant.message");

            session.close();
        }
    }

    /**
     * Verifies that no delta events are produced when streaming is disabled.
     *
     * @see Snapshot:
     *      streaming_fidelity/should_not_produce_deltas_when_streaming_is_disabled
     */
    @Test
    void testShouldNotProduceDeltasWhenStreamingIsDisabled() throws Exception {
        ctx.configureForTest("streaming_fidelity", "should_not_produce_deltas_when_streaming_is_disabled");

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(
                    new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setStreaming(false))
                    .get();

            List<AbstractSessionEvent> events = new ArrayList<>();
            session.on(events::add);

            session.sendAndWait(new MessageOptions().setPrompt("Say 'hello world'.")).get(60, TimeUnit.SECONDS);

            List<AssistantMessageDeltaEvent> deltaEvents = events.stream()
                    .filter(e -> e instanceof AssistantMessageDeltaEvent).map(e -> (AssistantMessageDeltaEvent) e)
                    .toList();

            // No deltas when streaming is off
            assertTrue(deltaEvents.isEmpty(), "Should not receive delta events when streaming is disabled");

            // But should still have a final assistant.message
            List<AssistantMessageEvent> assistantEvents = events.stream()
                    .filter(e -> e instanceof AssistantMessageEvent).map(e -> (AssistantMessageEvent) e).toList();
            assertFalse(assistantEvents.isEmpty(),
                    "Should still have a final assistant.message when streaming is disabled");

            session.close();
        }
    }

    /**
     * Verifies that delta events are produced after resuming a session with
     * streaming enabled.
     *
     * @see Snapshot: streaming_fidelity/should_produce_deltas_after_session_resume
     */
    @Test
    void testShouldProduceDeltasAfterSessionResume() throws Exception {
        ctx.configureForTest("streaming_fidelity", "should_produce_deltas_after_session_resume");

        try (CopilotClient client = ctx.createClient()) {
            // Create a non-streaming session and send an initial message
            CopilotSession session = client.createSession(
                    new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setStreaming(false))
                    .get();
            session.sendAndWait(new MessageOptions().setPrompt("What is 3 + 6?")).get(60, TimeUnit.SECONDS);
            String sessionId = session.getSessionId();
            session.close();

            // Resume using a new client with streaming enabled
            try (CopilotClient newClient = ctx.createClient()) {
                CopilotSession session2 = newClient.resumeSession(sessionId, new ResumeSessionConfig()
                        .setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setStreaming(true)).get();

                List<AbstractSessionEvent> events = new ArrayList<>();
                session2.on(events::add);

                AssistantMessageEvent answer = session2
                        .sendAndWait(new MessageOptions().setPrompt("Now if you double that, what do you get?"))
                        .get(60, TimeUnit.SECONDS);
                assertNotNull(answer);
                assertTrue(answer.getData().content().contains("18"),
                        "Follow-up response should contain 18: " + answer.getData().content());

                // Should have streaming deltas before the final message
                List<AssistantMessageDeltaEvent> deltaEvents = events.stream()
                        .filter(e -> e instanceof AssistantMessageDeltaEvent).map(e -> (AssistantMessageDeltaEvent) e)
                        .toList();
                assertFalse(deltaEvents.isEmpty(), "Should have received delta events after session resume");

                // Deltas should have content
                for (AssistantMessageDeltaEvent delta : deltaEvents) {
                    assertFalse(delta.getData().deltaContent() == null || delta.getData().deltaContent().isEmpty(),
                            "Delta event should have content");
                }

                session2.close();
            }
        }
    }
}

/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.generated.AssistantUsageEvent;
import com.github.copilot.generated.SessionEvent;
import com.github.copilot.generated.SessionUsageInfoEvent;
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.SessionConfig;

/**
 * E2E tests for event fidelity — verifying the shape, ordering, and presence of
 * key events emitted from the runtime.
 *
 * <p>
 * Snapshots are stored in {@code test/snapshots/event_fidelity/}.
 * </p>
 */
public class EventFidelityTest {

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
     * Verifies that an {@code assistant.usage} event is emitted after the model
     * processes a prompt.
     *
     * @see Snapshot:
     *      event_fidelity/should_emit_assistant_usage_event_after_model_call
     */
    @Test
    void testShouldEmitAssistantUsageEventAfterModelCall() throws Exception {
        ctx.configureForTest("event_fidelity", "should_emit_assistant_usage_event_after_model_call");

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client
                    .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get();

            List<SessionEvent> events = new ArrayList<>();
            session.on(events::add);

            session.sendAndWait(new MessageOptions().setPrompt("What is 5+5? Reply with just the number.")).get(60,
                    TimeUnit.SECONDS);

            List<AssistantUsageEvent> usageEvents = events.stream().filter(e -> e instanceof AssistantUsageEvent)
                    .map(e -> (AssistantUsageEvent) e).toList();

            assertFalse(usageEvents.isEmpty(), "Should have received an assistant.usage event after model call");

            AssistantUsageEvent lastUsage = usageEvents.get(usageEvents.size() - 1);
            assertNotNull(lastUsage.getData().model(), "Usage event should have a model field");
            assertFalse(lastUsage.getData().model().isEmpty(), "Model field should not be empty");

            session.close();
        }
    }

    /**
     * Verifies that a {@code session.usage_info} event is emitted after the model
     * processes a prompt.
     *
     * @see Snapshot:
     *      event_fidelity/should_emit_session_usage_info_event_after_model_call
     */
    @Test
    void testShouldEmitSessionUsageInfoEventAfterModelCall() throws Exception {
        ctx.configureForTest("event_fidelity", "should_emit_session_usage_info_event_after_model_call");

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client
                    .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get();

            List<SessionEvent> events = new ArrayList<>();
            session.on(events::add);

            session.sendAndWait(new MessageOptions().setPrompt("What is 5+5? Reply with just the number.")).get(60,
                    TimeUnit.SECONDS);

            List<SessionUsageInfoEvent> usageInfoEvents = events.stream()
                    .filter(e -> e instanceof SessionUsageInfoEvent).map(e -> (SessionUsageInfoEvent) e).toList();

            assertFalse(usageInfoEvents.isEmpty(), "Should have received a session.usage_info event after model call");

            session.close();
        }
    }
}

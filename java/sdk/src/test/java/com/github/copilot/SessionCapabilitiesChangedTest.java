/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.copilot.generated.CapabilitiesChangedEvent;
import com.github.copilot.generated.CapabilitiesChangedEvent.CapabilitiesChangedEventData;
import com.github.copilot.generated.CapabilitiesChangedUI;
import com.github.copilot.rpc.SessionCapabilities;
import com.github.copilot.rpc.SessionUiCapabilities;

/**
 * Unit tests for how {@link CopilotSession} applies
 * {@code capabilities.changed} events, which name only the capabilities that
 * changed.
 * <p>
 * These are pure unit tests that don't require the Copilot CLI. They drive the
 * package-private {@code dispatchEvent} hook directly.
 */
public class SessionCapabilitiesChangedTest {

    private CopilotSession session;

    @BeforeEach
    void setup() throws Exception {
        var constructor = CopilotSession.class.getDeclaredConstructor(String.class, JsonRpcClient.class, String.class);
        constructor.setAccessible(true);
        session = constructor.newInstance("test-session-id", null, null);
    }

    @Test
    void elicitationChangeKeepsMcpApps() {
        session.setCapabilities(new SessionCapabilities().setUi(new SessionUiCapabilities().setMcpApps(true)));
        var before = session.getCapabilities();

        session.dispatchEvent(changed(true, null, null));

        var ui = session.getCapabilities().getUi();
        assertEquals(Optional.of(true), ui.getElicitation());
        assertEquals(Optional.of(true), ui.getMcpApps());
        assertTrue(before.getUi().getElicitation().isEmpty());
    }

    @Test
    void mcpAppsChangeIsAppliedAndKeepsElicitation() {
        session.setCapabilities(new SessionCapabilities().setUi(new SessionUiCapabilities().setElicitation(true)));

        session.dispatchEvent(changed(null, true, null));

        var ui = session.getCapabilities().getUi();
        assertEquals(Optional.of(true), ui.getElicitation());
        assertEquals(Optional.of(true), ui.getMcpApps());
    }

    @Test
    void changeToFalseKeepsOtherCapabilities() {
        session.setCapabilities(
                new SessionCapabilities().setUi(new SessionUiCapabilities().setElicitation(true).setMcpApps(true)));

        session.dispatchEvent(changed(false, null, null));

        var ui = session.getCapabilities().getUi();
        assertEquals(Optional.of(false), ui.getElicitation());
        assertEquals(Optional.of(true), ui.getMcpApps());
    }

    @Test
    void eventsWithoutKnownCapabilitiesChangeNothing() {
        session.setCapabilities(new SessionCapabilities().setUi(new SessionUiCapabilities().setElicitation(true)));
        var subscriberCalls = new int[1];
        session.on(event -> subscriberCalls[0]++);

        session.dispatchEvent(changed(null, null, true));
        var noUi = new CapabilitiesChangedEvent();
        noUi.setData(new CapabilitiesChangedEventData(null));
        session.dispatchEvent(noUi);

        var ui = session.getCapabilities().getUi();
        assertEquals(Optional.of(true), ui.getElicitation());
        assertTrue(ui.getMcpApps().isEmpty());
        assertEquals(2, subscriberCalls[0]);
    }

    private static CapabilitiesChangedEvent changed(Boolean elicitation, Boolean mcpApps, Boolean canvases) {
        var event = new CapabilitiesChangedEvent();
        event.setData(new CapabilitiesChangedEventData(new CapabilitiesChangedUI(elicitation, mcpApps, canvases)));
        return event;
    }
}

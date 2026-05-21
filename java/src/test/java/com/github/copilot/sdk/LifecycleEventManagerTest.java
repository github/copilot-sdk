/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.copilot.sdk.json.SessionLifecycleEvent;

/**
 * Unit tests for {@link LifecycleEventManager} covering subscribe, unsubscribe,
 * dispatch, typed handlers, wildcard handlers, and error handling paths
 * identified as gaps by JaCoCo.
 */
class LifecycleEventManagerTest {

    private LifecycleEventManager manager;

    @BeforeEach
    void setup() {
        manager = new LifecycleEventManager();
    }

    private static SessionLifecycleEvent event(String type) {
        var e = new SessionLifecycleEvent();
        e.setType(type);
        return e;
    }

    // ===== wildcard subscribe / dispatch =====

    @Test
    void wildcardHandlerReceivesAllEvents() {
        var received = new ArrayList<SessionLifecycleEvent>();
        manager.subscribe(received::add);

        manager.dispatch(event("created"));
        manager.dispatch(event("deleted"));

        assertEquals(2, received.size());
        assertEquals("created", received.get(0).getType());
        assertEquals("deleted", received.get(1).getType());
    }

    @Test
    void wildcardUnsubscribeStopsDelivery() throws Exception {
        var received = new ArrayList<SessionLifecycleEvent>();
        AutoCloseable sub = manager.subscribe(received::add);

        manager.dispatch(event("created"));
        assertEquals(1, received.size());

        sub.close();

        manager.dispatch(event("deleted"));
        assertEquals(1, received.size(), "Should not receive events after unsubscribe");
    }

    // ===== typed subscribe / dispatch =====

    @Test
    void typedHandlerReceivesOnlyMatchingEvents() {
        var received = new ArrayList<SessionLifecycleEvent>();
        manager.subscribe("created", received::add);

        manager.dispatch(event("created"));
        manager.dispatch(event("deleted"));

        assertEquals(1, received.size());
        assertEquals("created", received.get(0).getType());
    }

    @Test
    void typedUnsubscribeStopsDelivery() throws Exception {
        var received = new ArrayList<SessionLifecycleEvent>();
        AutoCloseable sub = manager.subscribe("created", received::add);

        manager.dispatch(event("created"));
        assertEquals(1, received.size());

        sub.close();

        manager.dispatch(event("created"));
        assertEquals(1, received.size(), "Should not receive events after unsubscribe");
    }

    // ===== both typed + wildcard =====

    @Test
    void bothTypedAndWildcardReceiveEvent() {
        var typedReceived = new ArrayList<SessionLifecycleEvent>();
        var wildcardReceived = new ArrayList<SessionLifecycleEvent>();

        manager.subscribe("created", typedReceived::add);
        manager.subscribe(wildcardReceived::add);

        manager.dispatch(event("created"));

        assertEquals(1, typedReceived.size());
        assertEquals(1, wildcardReceived.size());
    }

    // ===== dispatch with no handlers =====

    @Test
    void dispatchWithNoHandlersDoesNotThrow() {
        assertDoesNotThrow(() -> manager.dispatch(event("created")));
    }

    @Test
    void dispatchWithNoTypedMatchDoesNotThrow() {
        var received = new ArrayList<SessionLifecycleEvent>();
        manager.subscribe("deleted", received::add);

        assertDoesNotThrow(() -> manager.dispatch(event("created")));
        assertTrue(received.isEmpty());
    }

    // ===== error handling =====

    @Test
    void typedHandlerExceptionDoesNotPreventOtherHandlers() {
        var received = new ArrayList<SessionLifecycleEvent>();

        // First handler throws
        manager.subscribe("created", e -> {
            throw new RuntimeException("typed handler error");
        });
        // Second handler should still receive the event
        manager.subscribe("created", received::add);

        assertDoesNotThrow(() -> manager.dispatch(event("created")));
        assertEquals(1, received.size());
    }

    @Test
    void wildcardHandlerExceptionDoesNotPreventOtherHandlers() {
        var received = new ArrayList<SessionLifecycleEvent>();

        manager.subscribe(e -> {
            throw new RuntimeException("wildcard handler error");
        });
        manager.subscribe(received::add);

        assertDoesNotThrow(() -> manager.dispatch(event("created")));
        assertEquals(1, received.size());
    }

    @Test
    void typedAndWildcardErrorsDoNotAffectEachOther() {
        var wildcardReceived = new ArrayList<SessionLifecycleEvent>();

        // Typed handler throws
        manager.subscribe("created", e -> {
            throw new RuntimeException("typed error");
        });
        // Wildcard still receives
        manager.subscribe(wildcardReceived::add);

        assertDoesNotThrow(() -> manager.dispatch(event("created")));
        assertEquals(1, wildcardReceived.size());
    }

    // ===== multiple handlers =====

    @Test
    void multipleWildcardHandlersAllReceive() {
        var list1 = new ArrayList<SessionLifecycleEvent>();
        var list2 = new ArrayList<SessionLifecycleEvent>();

        manager.subscribe(list1::add);
        manager.subscribe(list2::add);

        manager.dispatch(event("updated"));

        assertEquals(1, list1.size());
        assertEquals(1, list2.size());
    }

    @Test
    void multipleTypedHandlersAllReceive() {
        var list1 = new ArrayList<SessionLifecycleEvent>();
        var list2 = new ArrayList<SessionLifecycleEvent>();

        manager.subscribe("updated", list1::add);
        manager.subscribe("updated", list2::add);

        manager.dispatch(event("updated"));

        assertEquals(1, list1.size());
        assertEquals(1, list2.size());
    }
}

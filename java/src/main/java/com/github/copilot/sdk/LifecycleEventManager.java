/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.github.copilot.sdk.json.SessionLifecycleEvent;
import com.github.copilot.sdk.json.SessionLifecycleHandler;

/**
 * Manages lifecycle event subscriptions and dispatching.
 * <p>
 * This class handles registration/unregistration of lifecycle event handlers
 * and dispatches events to the appropriate handlers.
 */
final class LifecycleEventManager {

    private static final Logger LOG = Logger.getLogger(LifecycleEventManager.class.getName());

    private final List<SessionLifecycleHandler> wildcardHandlers = new ArrayList<>();
    private final Map<String, List<SessionLifecycleHandler>> typedHandlers = new ConcurrentHashMap<>();
    private final Object handlersLock = new Object();

    /**
     * Subscribes to all session lifecycle events.
     *
     * @param handler
     *            a callback that receives lifecycle events
     * @return an AutoCloseable that, when closed, unsubscribes the handler
     */
    AutoCloseable subscribe(SessionLifecycleHandler handler) {
        synchronized (handlersLock) {
            wildcardHandlers.add(handler);
        }
        return () -> {
            synchronized (handlersLock) {
                wildcardHandlers.remove(handler);
            }
        };
    }

    /**
     * Subscribes to a specific session lifecycle event type.
     *
     * @param eventType
     *            the event type to listen for
     * @param handler
     *            a callback that receives events of the specified type
     * @return an AutoCloseable that, when closed, unsubscribes the handler
     */
    AutoCloseable subscribe(String eventType, SessionLifecycleHandler handler) {
        synchronized (handlersLock) {
            typedHandlers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(handler);
        }
        return () -> {
            synchronized (handlersLock) {
                List<SessionLifecycleHandler> handlers = typedHandlers.get(eventType);
                if (handlers != null) {
                    handlers.remove(handler);
                }
            }
        };
    }

    /**
     * Dispatches a lifecycle event to all registered handlers.
     *
     * @param event
     *            the lifecycle event to dispatch
     */
    void dispatch(SessionLifecycleEvent event) {
        List<SessionLifecycleHandler> typed;
        List<SessionLifecycleHandler> wildcard;

        synchronized (handlersLock) {
            List<SessionLifecycleHandler> handlers = typedHandlers.get(event.getType());
            typed = handlers != null ? new ArrayList<>(handlers) : new ArrayList<>();
            wildcard = new ArrayList<>(wildcardHandlers);
        }

        for (SessionLifecycleHandler handler : typed) {
            try {
                handler.onLifecycleEvent(event);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Lifecycle handler error", e);
            }
        }

        for (SessionLifecycleHandler handler : wildcard) {
            try {
                handler.onLifecycleEvent(event);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Lifecycle handler error", e);
            }
        }
    }
}

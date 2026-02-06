/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import com.github.copilot.sdk.events.AbstractSessionEvent;

/**
 * A handler for errors thrown by event handlers during event dispatch.
 * <p>
 * When an event handler registered via
 * {@link CopilotSession#on(java.util.function.Consumer)} or
 * {@link CopilotSession#on(Class, java.util.function.Consumer)} throws an
 * exception, the {@code EventErrorHandler} is invoked with the event that was
 * being dispatched and the exception that was thrown.
 *
 * <p>
 * When no error handler is set, exceptions are silently consumed. Applications
 * should set an error handler to log, track, or respond to handler failures:
 *
 * <pre>{@code
 * session.setEventErrorHandler((event, exception) -> {
 * 	metrics.increment("handler.errors");
 * 	logger.error("Handler failed on {}: {}", event.getType(), exception.getMessage());
 * });
 * }</pre>
 *
 * <p>
 * Whether dispatch continues or stops after an error is controlled by the
 * {@link EventErrorPolicy} set via
 * {@link CopilotSession#setEventErrorPolicy(EventErrorPolicy)}. The error
 * handler is always invoked regardless of the policy.
 *
 * <p>
 * If the error handler itself throws an exception, that exception is caught and
 * logged at {@link java.util.logging.Level#SEVERE}, and dispatch is stopped
 * regardless of the configured policy.
 *
 * @see CopilotSession#setEventErrorHandler(EventErrorHandler)
 * @see EventErrorPolicy
 * @since 1.0.8
 */
@FunctionalInterface
public interface EventErrorHandler {

    /**
     * Called when an event handler throws an exception during event dispatch.
     *
     * @param event
     *            the event that was being dispatched when the error occurred
     * @param exception
     *            the exception thrown by the event handler
     */
    void handleError(AbstractSessionEvent event, Exception exception);
}

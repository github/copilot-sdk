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
 * The handler's return value controls whether event dispatch continues to
 * remaining handlers:
 * <ul>
 * <li>Return {@code true} to continue dispatching to remaining handlers
 * (default behavior for independent listeners)</li>
 * <li>Return {@code false} to stop dispatching (short-circuit behavior for
 * validation or critical errors)</li>
 * </ul>
 *
 * <p>
 * When no error handler is set, exceptions are silently caught and dispatch
 * continues to remaining handlers. This makes the SDK non-intrusive by default.
 * Applications should set an error handler to log, track, or respond to handler
 * failures.
 *
 * <p>
 * Example configurations:
 *
 * <pre>{@code
 * // Continue on error (log and keep dispatching)
 * session.setEventErrorHandler((event, exception) -> {
 * 	logger.error("Handler failed: {}", exception.getMessage(), exception);
 * 	return true; // keep dispatching
 * });
 *
 * // Short-circuit on error (stop at first failure)
 * session.setEventErrorHandler((event, exception) -> {
 * 	logger.error("Handler failed, stopping: {}", exception.getMessage(), exception);
 * 	return false; // stop dispatching
 * });
 *
 * // Selective: short-circuit only for critical events
 * session.setEventErrorHandler((event, exception) -> {
 * 	logger.error("Handler failed: {}", exception.getMessage(), exception);
 * 	return !(event instanceof SessionErrorEvent); // stop only for error events
 * });
 * }</pre>
 *
 * <p>
 * If the error handler itself throws an exception, that exception is caught,
 * logged at {@link java.util.logging.Level#SEVERE}, and dispatch is stopped to
 * prevent cascading failures.
 *
 * @see CopilotSession#setEventErrorHandler(EventErrorHandler)
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
     * @return {@code true} to continue dispatching to remaining handlers,
     *         {@code false} to stop dispatching (the exception is not rethrown)
     */
    boolean handleError(AbstractSessionEvent event, Exception exception);
}

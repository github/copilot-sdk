/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

/**
 * Controls how event dispatch behaves when an event handler throws an
 * exception.
 * <p>
 * This policy is set via
 * {@link CopilotSession#setEventErrorPolicy(EventErrorPolicy)} and determines
 * whether remaining event listeners continue to execute after a preceding
 * listener throws an exception. Errors are always logged at
 * {@link java.util.logging.Level#WARNING} regardless of the policy.
 *
 * <p>
 * The configured {@link EventErrorHandler} (if any) is always invoked
 * regardless of the policy — the policy only controls whether dispatch
 * continues after the error has been logged and the error handler has been
 * called.
 *
 * <p>
 * The naming follows the convention used by Spring Framework's
 * {@code TaskUtils.LOG_AND_SUPPRESS_ERROR_HANDLER} and
 * {@code TaskUtils.LOG_AND_PROPAGATE_ERROR_HANDLER}.
 *
 * <p>
 * <b>Example:</b>
 *
 * <pre>{@code
 * // Default: propagate errors (stop dispatch on first error, log the error)
 * session.setEventErrorPolicy(EventErrorPolicy.PROPAGATE_AND_LOG_ERRORS);
 *
 * // Opt-in to suppress errors (continue dispatching, log each error)
 * session.setEventErrorPolicy(EventErrorPolicy.SUPPRESS_AND_LOG_ERRORS);
 * }</pre>
 *
 * @see CopilotSession#setEventErrorPolicy(EventErrorPolicy)
 * @see EventErrorHandler
 * @since 1.0.8
 */
public enum EventErrorPolicy {

    /**
     * Suppress errors: log the error and continue dispatching to remaining
     * listeners.
     * <p>
     * When a handler throws an exception, the error is logged at
     * {@link java.util.logging.Level#WARNING} and remaining handlers still execute.
     * The configured {@link EventErrorHandler} is called for each error. This is
     * analogous to Spring's {@code LOG_AND_SUPPRESS_ERROR_HANDLER} behavior.
     */
    SUPPRESS_AND_LOG_ERRORS,

    /**
     * Propagate errors: log the error and stop dispatch on first listener error
     * (default).
     * <p>
     * When a handler throws an exception, the error is logged at
     * {@link java.util.logging.Level#WARNING} and no further handlers are invoked.
     * The configured {@link EventErrorHandler} is still called before dispatch
     * stops. This is analogous to Spring's {@code LOG_AND_PROPAGATE_ERROR_HANDLER}
     * behavior.
     */
    PROPAGATE_AND_LOG_ERRORS
}

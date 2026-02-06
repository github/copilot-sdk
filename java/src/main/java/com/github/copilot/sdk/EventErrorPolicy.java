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
 * listener throws an exception.
 *
 * <p>
 * The configured {@link EventErrorHandler} (if any) is always invoked
 * regardless of the policy — the policy only controls whether dispatch
 * continues after the error handler has been called.
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
 * // Default: suppress errors and continue dispatching
 * session.setEventErrorPolicy(EventErrorPolicy.SUPPRESS);
 *
 * // Opt-in to propagate errors (stop dispatch on first error)
 * session.setEventErrorPolicy(EventErrorPolicy.PROPAGATE);
 * }</pre>
 *
 * @see CopilotSession#setEventErrorPolicy(EventErrorPolicy)
 * @see EventErrorHandler
 * @since 1.0.8
 */
public enum EventErrorPolicy {

    /**
     * Suppress errors and continue dispatching to remaining listeners (default).
     * <p>
     * When a handler throws an exception, remaining handlers still execute. The
     * configured {@link EventErrorHandler} is called for each error. This is
     * analogous to Spring's {@code LOG_AND_SUPPRESS_ERROR_HANDLER} behavior.
     */
    SUPPRESS,

    /**
     * Propagate the error effect by stopping dispatch on first listener error.
     * <p>
     * When a handler throws an exception, no further handlers are invoked. The
     * configured {@link EventErrorHandler} is still called before dispatch stops.
     * This is analogous to Spring's {@code LOG_AND_PROPAGATE_ERROR_HANDLER}
     * behavior.
     */
    PROPAGATE
}

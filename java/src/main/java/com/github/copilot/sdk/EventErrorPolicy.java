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
 * <b>Example:</b>
 *
 * <pre>{@code
 * // Default: continue dispatching despite errors
 * session.setEventErrorPolicy(EventErrorPolicy.CONTINUE);
 *
 * // Opt-in to short-circuit on first error
 * session.setEventErrorPolicy(EventErrorPolicy.STOP);
 * }</pre>
 *
 * @see CopilotSession#setEventErrorPolicy(EventErrorPolicy)
 * @see EventErrorHandler
 * @since 1.0.8
 */
public enum EventErrorPolicy {

    /**
     * Stop dispatching on first listener error.
     * <p>
     * When a handler throws an exception, no further handlers are invoked. The
     * configured {@link EventErrorHandler} is still called before dispatch stops.
     */
    STOP,

    /**
     * Continue dispatching to remaining listeners despite errors (default).
     * <p>
     * When a handler throws an exception, remaining handlers still execute. The
     * configured {@link EventErrorHandler} is called for each error.
     */
    CONTINUE
}

/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

/**
 * Handler for session lifecycle events.
 * <p>
 * Implement this interface to receive notifications when sessions are created,
 * deleted, updated, or change foreground/background state.
 *
 * @since 1.0.0
 */
@FunctionalInterface
public interface SessionLifecycleHandler {

    /**
     * Called when a session lifecycle event occurs.
     *
     * @param event
     *            the lifecycle event
     */
    void onLifecycleEvent(SessionLifecycleEvent event);
}

/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

/**
 * Types of session lifecycle events.
 * <p>
 * Constants for session lifecycle event types used with
 * {@link com.github.copilot.sdk.CopilotClient#onLifecycle(String, SessionLifecycleHandler)}.
 *
 * @since 1.0.0
 */
public final class SessionLifecycleEventTypes {

    /**
     * Event fired when a session is created.
     */
    public static final String CREATED = "session.created";

    /**
     * Event fired when a session is deleted.
     */
    public static final String DELETED = "session.deleted";

    /**
     * Event fired when a session is updated.
     */
    public static final String UPDATED = "session.updated";

    /**
     * Event fired when a session moves to foreground (TUI+server mode).
     */
    public static final String FOREGROUND = "session.foreground";

    /**
     * Event fired when a session moves to background (TUI+server mode).
     */
    public static final String BACKGROUND = "session.background";

    private SessionLifecycleEventTypes() {
        // Utility class
    }
}

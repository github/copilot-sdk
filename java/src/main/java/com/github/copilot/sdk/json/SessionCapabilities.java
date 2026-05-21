/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

/**
 * Represents the capabilities reported by the host for a session.
 * <p>
 * Capabilities are populated from the session create/resume response and
 * updated in real time via {@code capabilities.changed} events.
 *
 * @since 1.0.0
 */
public class SessionCapabilities {

    private SessionUiCapabilities ui;

    /**
     * Gets the UI-related capabilities.
     *
     * @return the UI capabilities, or {@code null} if not reported
     */
    public SessionUiCapabilities getUi() {
        return ui;
    }

    /**
     * Sets the UI-related capabilities.
     *
     * @param ui
     *            the UI capabilities
     * @return this instance for method chaining
     */
    public SessionCapabilities setUi(SessionUiCapabilities ui) {
        this.ui = ui;
        return this;
    }
}

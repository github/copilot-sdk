/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents an unrecognized session event type received from the CLI.
 * <p>
 * When the CLI sends an event with a type that this SDK does not recognize (for
 * example, an event type introduced in a newer CLI version), the SDK wraps it
 * in an {@code UnknownSessionEvent} rather than dropping it. This ensures
 * forward compatibility: event handlers can simply ignore unknown event types
 * without the SDK crashing.
 *
 * <h2>Example</h2>
 *
 * <pre>{@code
 * session.on(event -> {
 * 	if (event instanceof UnknownSessionEvent unknown) {
 * 		// Ignore events from newer CLI versions
 * 	}
 * });
 * }</pre>
 *
 * @see AbstractSessionEvent
 * @since 1.2.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class UnknownSessionEvent extends AbstractSessionEvent {

    private final String originalType;

    /**
     * Creates an unknown session event with the given original type string.
     *
     * @param originalType
     *            the event type string received from the CLI; may be {@code null}
     */
    public UnknownSessionEvent(String originalType) {
        this.originalType = originalType != null ? originalType : "unknown";
    }

    /**
     * No-arg constructor for internal use by the parser.
     * <p>
     * Creates an unknown event with {@code "unknown"} as the original type. Callers
     * that need the original type should use {@link #UnknownSessionEvent(String)}.
     */
    UnknownSessionEvent() {
        this("unknown");
    }

    /**
     * Returns {@code "unknown"} as the canonical type for all unrecognized events.
     *
     * @return always {@code "unknown"}
     */
    @Override
    public String getType() {
        return "unknown";
    }

    /**
     * Returns the original event type string as received from the CLI.
     *
     * @return the original type, never {@code null}
     */
    public String getOriginalType() {
        return originalType;
    }
}

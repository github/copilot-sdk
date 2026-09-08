/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The origin of a message sent to a Copilot session.
 * <p>
 * Set on {@link MessageOptions#setSource(MessageSource)} to distinguish user
 * input, system context, and messages from identified agents. This does not
 * configure the session's system prompt or change the message delivery mode.
 *
 * @see MessageOptions
 */
public final class MessageSource {

    /** Input originating from the user. */
    public static final MessageSource USER = new MessageSource("user");

    /** Application-generated system context. */
    public static final MessageSource SYSTEM = new MessageSource("system");

    private final String value;

    private MessageSource(String value) {
        this.value = value;
    }

    /**
     * Identifies a message from an agent.
     *
     * @param id
     *            the opaque agent identifier, preserved exactly after
     *            {@code agent-}
     * @return the agent message source
     * @throws NullPointerException
     *             if {@code id} is {@code null}
     */
    public static MessageSource agent(String id) {
        return new MessageSource("agent-" + Objects.requireNonNull(id, "id"));
    }

    /**
     * Returns the JSON value for this message source.
     *
     * @return the string value used in JSON serialization
     */
    @JsonValue
    public String getValue() {
        return value;
    }

    /**
     * Deserializes a JSON string into the corresponding message source.
     *
     * @param value
     *            the JSON string value
     * @return the matching source, or {@code null} if value is {@code null}
     * @throws IllegalArgumentException
     *             if the value does not match a known message source
     */
    @JsonCreator
    public static MessageSource fromValue(String value) {
        if (value == null) {
            return null;
        }
        if (USER.value.equals(value)) {
            return USER;
        }
        if (SYSTEM.value.equals(value)) {
            return SYSTEM;
        }
        if (value.startsWith("agent-")) {
            return new MessageSource(value);
        }
        throw new IllegalArgumentException("Unknown MessageSource value: " + value);
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof MessageSource source && value.equals(source.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}

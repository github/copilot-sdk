/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Selects how the built-in {@code ask_user} tool collects user input.
 */
public enum AskUserVariant {

    /** Uses the legacy question-and-answer experience. */
    LEGACY("legacy"),

    /** Uses structured elicitation to collect user input. */
    ELICITATION("elicitation");

    private final String value;

    AskUserVariant(String value) {
        this.value = value;
    }

    /**
     * Returns the wire-format value.
     *
     * @return the value used in JSON serialization
     */
    @JsonValue
    public String getValue() {
        return value;
    }

    /**
     * Creates an {@code AskUserVariant} from its wire-format value.
     *
     * @param value
     *            the wire-format value
     * @return the matching variant, or {@code null} when {@code value} is
     *         {@code null}
     * @throws IllegalArgumentException
     *             if the value is not {@code legacy} or {@code elicitation}
     */
    @JsonCreator
    public static AskUserVariant fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (AskUserVariant variant : values()) {
            if (variant.value.equals(value)) {
                return variant;
            }
        }
        throw new IllegalArgumentException("Unknown AskUserVariant value: " + value);
    }
}

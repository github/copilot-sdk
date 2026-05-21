/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Specifies how the system message should be applied to a session.
 * <p>
 * The system message controls the behavior and personality of the AI assistant.
 * This enum determines whether to append custom instructions to the default
 * system message or replace it entirely.
 *
 * @see com.github.copilot.sdk.json.SystemMessageConfig
 * @since 1.0.0
 */
public enum SystemMessageMode {
    /**
     * Append the custom content to the default system message.
     * <p>
     * This mode preserves the default guardrails and behaviors while adding
     * additional instructions or context.
     */
    APPEND("append"),

    /**
     * Replace the default system message entirely with the custom content.
     * <p>
     * <strong>Warning:</strong> This mode removes all default guardrails and
     * behaviors. Use with caution.
     */
    REPLACE("replace"),

    /**
     * Override individual sections of the system prompt.
     * <p>
     * Use this mode with
     * {@link com.github.copilot.sdk.json.SystemMessageConfig#setSections} to
     * selectively replace, remove, append, prepend, or transform individual
     * sections of the default system prompt. An optional {@code content} string is
     * appended after all sections when provided.
     *
     * @since 1.2.0
     */
    CUSTOMIZE("customize");

    private final String value;

    SystemMessageMode(String value) {
        this.value = value;
    }

    /**
     * Returns the JSON value for this mode.
     *
     * @return the string value used in JSON serialization
     */
    @JsonValue
    public String getValue() {
        return value;
    }
}

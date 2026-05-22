/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Specifies the operation to perform on a system prompt section in
 * {@link SystemMessageMode#CUSTOMIZE} mode.
 *
 * @see SectionOverride
 * @see SystemMessageConfig
 * @since 1.2.0
 */
public enum SectionOverrideAction {

    /** Replace the section content entirely. */
    REPLACE("replace"),

    /** Remove the section from the prompt. */
    REMOVE("remove"),

    /** Append content after the existing section. */
    APPEND("append"),

    /** Prepend content before the existing section. */
    PREPEND("prepend"),

    /**
     * Transform the section content via a callback.
     * <p>
     * When this action is used, the {@link SectionOverride#getTransform()} callback
     * must be set. The SDK will not serialize this action over the wire directly;
     * instead it registers a {@code systemMessage.transform} RPC handler.
     */
    TRANSFORM("transform");

    private final String value;

    SectionOverrideAction(String value) {
        this.value = value;
    }

    /**
     * Returns the JSON value for this action.
     *
     * @return the string value used in JSON serialization
     */
    @JsonValue
    public String getValue() {
        return value;
    }
}

/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

/**
 * Action value for an {@link ElicitationResult}.
 *
 * @since 1.0.0
 */
public enum ElicitationResultAction {

    /** The user submitted the form (accepted). */
    ACCEPT("accept"),

    /** The user explicitly rejected the request. */
    DECLINE("decline"),

    /** The user dismissed the dialog without responding. */
    CANCEL("cancel");

    private final String value;

    ElicitationResultAction(String value) {
        this.value = value;
    }

    /** Returns the wire-format string value. @return the string value */
    public String getValue() {
        return value;
    }
}

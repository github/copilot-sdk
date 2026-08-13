/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Reason a captured file was not restored.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum HistoryFileRestoreSkipReason {
    /** The {@code user-modified} variant. */
    USER_MODIFIED("user-modified"),
    /** The {@code skipped-capture} variant. */
    SKIPPED_CAPTURE("skipped-capture");

    private final String value;
    HistoryFileRestoreSkipReason(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static HistoryFileRestoreSkipReason fromValue(String value) {
        for (HistoryFileRestoreSkipReason v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown HistoryFileRestoreSkipReason value: " + value);
    }
}

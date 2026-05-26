/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import javax.annotation.processing.Generated;

/**
 * Current status: running, disabled, failed, or starting
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ExtensionsLoadedExtensionStatus {
    /** The {@code running} variant. */
    RUNNING("running"),
    /** The {@code disabled} variant. */
    DISABLED("disabled"),
    /** The {@code failed} variant. */
    FAILED("failed"),
    /** The {@code starting} variant. */
    STARTING("starting");

    private final String value;
    ExtensionsLoadedExtensionStatus(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ExtensionsLoadedExtensionStatus fromValue(String value) {
        for (ExtensionsLoadedExtensionStatus v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ExtensionsLoadedExtensionStatus value: " + value);
    }
}

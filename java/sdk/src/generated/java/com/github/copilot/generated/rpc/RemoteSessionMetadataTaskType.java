/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Whether the remote task originated from CCA or CLI `--remote`.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum RemoteSessionMetadataTaskType {
    /** The {@code cca} variant. */
    CCA("cca"),
    /** The {@code cli} variant. */
    CLI("cli");

    private final String value;
    RemoteSessionMetadataTaskType(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static RemoteSessionMetadataTaskType fromValue(String value) {
        for (RemoteSessionMetadataTaskType v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown RemoteSessionMetadataTaskType value: " + value);
    }
}

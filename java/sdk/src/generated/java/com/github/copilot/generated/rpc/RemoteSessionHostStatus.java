/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * What a remote host says one of its sessions is doing right now. Deliberately coarse: this is what a host can report for EVERY session in a catalogue listing, without a client subscribing to each one. AHP's `SessionSummary.status` is the source today; `input-needed` covers both a permission prompt and an `ask_user` question, since the summary does not say which.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum RemoteSessionHostStatus {
    /** The {@code idle} variant. */
    IDLE("idle"),
    /** The {@code working} variant. */
    WORKING("working"),
    /** The {@code input-needed} variant. */
    INPUT_NEEDED("input-needed"),
    /** The {@code error} variant. */
    ERROR("error");

    private final String value;
    RemoteSessionHostStatus(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static RemoteSessionHostStatus fromValue(String value) {
        for (RemoteSessionHostStatus v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown RemoteSessionHostStatus value: " + value);
    }
}

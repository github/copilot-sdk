/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * SQLite transaction failure classification.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum SessionFsSqliteTransactionErrorClass {
    /** The {@code busyOrLocked} variant. */
    BUSYORLOCKED("busyOrLocked"),
    /** The {@code fatal} variant. */
    FATAL("fatal"),
    /** The {@code postCommitAmbiguous} variant. */
    POSTCOMMITAMBIGUOUS("postCommitAmbiguous");

    private final String value;
    SessionFsSqliteTransactionErrorClass(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static SessionFsSqliteTransactionErrorClass fromValue(String value) {
        for (SessionFsSqliteTransactionErrorClass v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown SessionFsSqliteTransactionErrorClass value: " + value);
    }
}

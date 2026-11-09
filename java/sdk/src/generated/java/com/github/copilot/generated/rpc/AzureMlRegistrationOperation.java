/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * A managed registration change that leaves unrelated provider rows unchanged.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum AzureMlRegistrationOperation {
    /** The {@code upsert} variant. */
    UPSERT("upsert"),
    /** The {@code remove} variant. */
    REMOVE("remove"),
    /** The {@code restore} variant. */
    RESTORE("restore");

    private final String value;
    AzureMlRegistrationOperation(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static AzureMlRegistrationOperation fromValue(String value) {
        for (AzureMlRegistrationOperation v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown AzureMlRegistrationOperation value: " + value);
    }
}

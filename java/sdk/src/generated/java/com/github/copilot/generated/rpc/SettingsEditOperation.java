/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * One atomic settings mutation.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
enum SettingsEditOperation {
    /** The {@code set} variant. */
    SET("set"),
    /** The {@code add-item} variant. */
    ADD_ITEM("add-item"),
    /** The {@code remove-item} variant. */
    REMOVE_ITEM("remove-item"),
    /** The {@code remove} variant. */
    REMOVE("remove");

    private final String value;
    SettingsEditOperation(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static SettingsEditOperation fromValue(String value) {
        for (SettingsEditOperation v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown SettingsEditOperation value: " + value);
    }
}

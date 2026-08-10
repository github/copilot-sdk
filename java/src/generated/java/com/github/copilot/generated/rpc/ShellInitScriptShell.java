/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Supported built-in shells for initialization scripts.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ShellInitScriptShell {
    /** The {@code bash} variant. */
    BASH("bash"),
    /** The {@code powershell} variant. */
    POWERSHELL("powershell");

    private final String value;
    ShellInitScriptShell(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ShellInitScriptShell fromValue(String value) {
        for (ShellInitScriptShell v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ShellInitScriptShell value: " + value);
    }
}

/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Optional source for allow-all telemetry. Defaults to `rpc` when omitted for SDK callers.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum PermissionsSetApproveAllSource {
    /** The {@code cli_flag} variant. */
    CLI_FLAG("cli_flag"),
    /** The {@code slash_command} variant. */
    SLASH_COMMAND("slash_command"),
    /** The {@code autopilot_confirmation} variant. */
    AUTOPILOT_CONFIRMATION("autopilot_confirmation"),
    /** The {@code rpc} variant. */
    RPC("rpc");

    private final String value;
    PermissionsSetApproveAllSource(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static PermissionsSetApproveAllSource fromValue(String value) {
        for (PermissionsSetApproveAllSource v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown PermissionsSetApproveAllSource value: " + value);
    }
}

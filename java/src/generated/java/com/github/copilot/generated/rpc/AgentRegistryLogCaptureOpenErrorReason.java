/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Categorized reason for log-open failure
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum AgentRegistryLogCaptureOpenErrorReason {
    /** The {@code permission} variant. */
    PERMISSION("permission"),
    /** The {@code disk_full} variant. */
    DISK_FULL("disk_full"),
    /** The {@code other} variant. */
    OTHER("other");

    private final String value;
    AgentRegistryLogCaptureOpenErrorReason(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static AgentRegistryLogCaptureOpenErrorReason fromValue(String value) {
        for (AgentRegistryLogCaptureOpenErrorReason v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown AgentRegistryLogCaptureOpenErrorReason value: " + value);
    }
}

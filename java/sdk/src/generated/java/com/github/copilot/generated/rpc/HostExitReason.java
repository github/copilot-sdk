/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Values for {@code HostExitReason}.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum HostExitReason {
    /** The {@code disposed} variant. */
    DISPOSED("disposed"),
    /** The {@code exited} variant. */
    EXITED("exited"),
    /** The {@code ownerDisconnected} variant. */
    OWNERDISCONNECTED("ownerDisconnected"),
    /** The {@code runtimeShutdown} variant. */
    RUNTIMESHUTDOWN("runtimeShutdown");

    private final String value;
    HostExitReason(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static HostExitReason fromValue(String value) {
        for (HostExitReason v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown HostExitReason value: " + value);
    }
}

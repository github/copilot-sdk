/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: lifecycle-diagnostics.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * How a settled lifecycle operation or progress record ended.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
enum RuntimeLifecycleDisposition {
    /** The {@code succeeded} variant. */
    SUCCEEDED("succeeded"),
    /** The {@code failed} variant. */
    FAILED("failed"),
    /** The {@code cancelled} variant. */
    CANCELLED("cancelled"),
    /** The {@code timed_out} variant. */
    TIMED_OUT("timed_out"),
    /** The {@code superseded} variant. */
    SUPERSEDED("superseded"),
    /** The {@code abandoned} variant. */
    ABANDONED("abandoned");

    private final String value;
    RuntimeLifecycleDisposition(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static RuntimeLifecycleDisposition fromValue(String value) {
        for (RuntimeLifecycleDisposition v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown RuntimeLifecycleDisposition value: " + value);
    }
}

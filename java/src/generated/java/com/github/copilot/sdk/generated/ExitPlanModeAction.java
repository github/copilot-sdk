/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import javax.annotation.processing.Generated;

/**
 * Exit plan mode action
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ExitPlanModeAction {
    /** The {@code exit_only} variant. */
    EXIT_ONLY("exit_only"),
    /** The {@code interactive} variant. */
    INTERACTIVE("interactive"),
    /** The {@code autopilot} variant. */
    AUTOPILOT("autopilot"),
    /** The {@code autopilot_fleet} variant. */
    AUTOPILOT_FLEET("autopilot_fleet");

    private final String value;
    ExitPlanModeAction(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ExitPlanModeAction fromValue(String value) {
        for (ExitPlanModeAction v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ExitPlanModeAction value: " + value);
    }
}

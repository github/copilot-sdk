/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * The action the user selected. Defaults to 'autopilot' when autoApproveEdits is true, otherwise 'interactive'.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum UIExitPlanModeAction {
    /** The {@code exit_only} variant. */
    EXIT_ONLY("exit_only"),
    /** The {@code interactive} variant. */
    INTERACTIVE("interactive"),
    /** The {@code autopilot} variant. */
    AUTOPILOT("autopilot"),
    /** The {@code autopilot_fleet} variant. */
    AUTOPILOT_FLEET("autopilot_fleet");

    private final String value;
    UIExitPlanModeAction(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static UIExitPlanModeAction fromValue(String value) {
        for (UIExitPlanModeAction v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown UIExitPlanModeAction value: " + value);
    }
}

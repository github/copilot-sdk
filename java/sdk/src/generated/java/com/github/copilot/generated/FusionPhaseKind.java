/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * HydraFusion phase kind.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum FusionPhaseKind {
    /** The {@code primary} variant. */
    PRIMARY("primary"),
    /** The {@code judge} variant. */
    JUDGE("judge"),
    /** The {@code repair} variant. */
    REPAIR("repair"),
    /** The {@code draft} variant. */
    DRAFT("draft"),
    /** The {@code critic} variant. */
    CRITIC("critic"),
    /** The {@code revision} variant. */
    REVISION("revision"),
    /** The {@code follow_up} variant. */
    FOLLOW_UP("follow_up");

    private final String value;
    FusionPhaseKind(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static FusionPhaseKind fromValue(String value) {
        for (FusionPhaseKind v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown FusionPhaseKind value: " + value);
    }
}

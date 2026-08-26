/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Kind of turn for which HydraFusion routing is running.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum FusionTurnKind {
    /** The {@code user} variant. */
    USER("user"),
    /** The {@code compaction} variant. */
    COMPACTION("compaction");

    private final String value;
    FusionTurnKind(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static FusionTurnKind fromValue(String value) {
        for (FusionTurnKind v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown FusionTurnKind value: " + value);
    }
}

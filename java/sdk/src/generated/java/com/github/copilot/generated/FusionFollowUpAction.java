/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Server-recommended routing behavior for a later HydraFusion turn.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum FusionFollowUpAction {
    /** The {@code reuse_primary} variant. */
    REUSE_PRIMARY("reuse_primary"),
    /** The {@code reroute} variant. */
    REROUTE("reroute");

    private final String value;
    FusionFollowUpAction(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static FusionFollowUpAction fromValue(String value) {
        for (FusionFollowUpAction v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown FusionFollowUpAction value: " + value);
    }
}

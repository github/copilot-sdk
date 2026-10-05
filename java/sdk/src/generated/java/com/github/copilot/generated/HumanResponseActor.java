/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Controlled provenance for a typed runtime response. Only `human_response`, minted by a trusted direct-interaction ingress, is human authorization evidence.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum HumanResponseActor {
    /** The {@code human_response} variant. */
    HUMAN_RESPONSE("human_response"),
    /** The {@code host_automation} variant. */
    HOST_AUTOMATION("host_automation"),
    /** The {@code unknown} variant. */
    UNKNOWN("unknown");

    private final String value;
    HumanResponseActor(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static HumanResponseActor fromValue(String value) {
        for (HumanResponseActor v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown HumanResponseActor value: " + value);
    }
}

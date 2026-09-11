/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Action the runtime selected for a durable factory pause checkpoint.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum FactoryPauseCheckpointAction {
    /** The {@code continue} variant. */
    CONTINUE("continue"),
    /** The {@code pause} variant. */
    PAUSE("pause");

    private final String value;
    FactoryPauseCheckpointAction(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static FactoryPauseCheckpointAction fromValue(String value) {
        for (FactoryPauseCheckpointAction v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown FactoryPauseCheckpointAction value: " + value);
    }
}

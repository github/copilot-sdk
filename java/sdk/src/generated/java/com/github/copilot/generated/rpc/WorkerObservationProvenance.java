/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Producer of an observation, not the execution location of every referenced source.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum WorkerObservationProvenance {
    /** The {@code native} variant. */
    NATIVE("native"),
    /** The {@code ahp_coordinator} variant. */
    AHP_COORDINATOR("ahp_coordinator");

    private final String value;
    WorkerObservationProvenance(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static WorkerObservationProvenance fromValue(String value) {
        for (WorkerObservationProvenance v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown WorkerObservationProvenance value: " + value);
    }
}

/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Terminal outcome declared for a retained catalog selection group
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum CatalogSelectionDecision {
    /** The {@code selected} variant. */
    SELECTED("selected"),
    /** The {@code declined} variant. */
    DECLINED("declined"),
    /** The {@code cancelled} variant. */
    CANCELLED("cancelled"),
    /** The {@code timed-out} variant. */
    TIMED_OUT("timed-out");

    private final String value;
    CatalogSelectionDecision(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static CatalogSelectionDecision fromValue(String value) {
        for (CatalogSelectionDecision v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown CatalogSelectionDecision value: " + value);
    }
}

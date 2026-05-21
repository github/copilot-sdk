/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Relative cost tier for token-based billing users
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ModelPickerPriceCategory {
    /** The {@code low} variant. */
    LOW("low"),
    /** The {@code medium} variant. */
    MEDIUM("medium"),
    /** The {@code high} variant. */
    HIGH("high"),
    /** The {@code very_high} variant. */
    VERY_HIGH("very_high");

    private final String value;
    ModelPickerPriceCategory(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ModelPickerPriceCategory fromValue(String value) {
        for (ModelPickerPriceCategory v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ModelPickerPriceCategory value: " + value);
    }
}

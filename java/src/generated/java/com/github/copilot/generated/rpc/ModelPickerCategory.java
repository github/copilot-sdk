/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Model capability category for grouping in the model picker
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ModelPickerCategory {
    /** The {@code lightweight} variant. */
    LIGHTWEIGHT("lightweight"),
    /** The {@code versatile} variant. */
    VERSATILE("versatile"),
    /** The {@code powerful} variant. */
    POWERFUL("powerful");

    private final String value;
    ModelPickerCategory(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ModelPickerCategory fromValue(String value) {
        for (ModelPickerCategory v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ModelPickerCategory value: " + value);
    }
}

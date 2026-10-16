/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Typed outcome for a provider operation.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ModelProviderOperationOutcomeCode {
    /** The {@code success} variant. */
    SUCCESS("success"),
    /** The {@code absent} variant. */
    ABSENT("absent"),
    /** The {@code unreachable} variant. */
    UNREACHABLE("unreachable"),
    /** The {@code failed} variant. */
    FAILED("failed");

    private final String value;
    ModelProviderOperationOutcomeCode(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ModelProviderOperationOutcomeCode fromValue(String value) {
        for (ModelProviderOperationOutcomeCode v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ModelProviderOperationOutcomeCode value: " + value);
    }
}

/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Registry that owns an Azure ML model version.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum AzureMlRegistry {
    /** The {@code azureml} variant. */
    AZUREML("azureml"),
    /** The {@code azureml-dev} variant. */
    AZUREML_DEV("azureml-dev");

    private final String value;
    AzureMlRegistry(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static AzureMlRegistry fromValue(String value) {
        for (AzureMlRegistry v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown AzureMlRegistry value: " + value);
    }
}

/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Binary asset type discriminator. Use "image" for images and "resource" otherwise.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum BinaryAssetType {
    /** The {@code image} variant. */
    IMAGE("image"),
    /** The {@code resource} variant. */
    RESOURCE("resource");

    private final String value;
    BinaryAssetType(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static BinaryAssetType fromValue(String value) {
        for (BinaryAssetType v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown BinaryAssetType value: " + value);
    }
}

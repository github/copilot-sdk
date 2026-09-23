/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Live indexed-search state for this session activation, never inferred from persisted history.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum IndexedSearchState {
    /** The {@code disabled} variant. */
    DISABLED("disabled"),
    /** The {@code starting} variant. */
    STARTING("starting"),
    /** The {@code enabled} variant. */
    ENABLED("enabled"),
    /** The {@code ready} variant. */
    READY("ready"),
    /** The {@code failed} variant. */
    FAILED("failed");

    private final String value;
    IndexedSearchState(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static IndexedSearchState fromValue(String value) {
        for (IndexedSearchState v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown IndexedSearchState value: " + value);
    }
}

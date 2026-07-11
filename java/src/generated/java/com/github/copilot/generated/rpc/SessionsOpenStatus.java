/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Outcome of the open request.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum SessionsOpenStatus {
    /** The {@code created} variant. */
    CREATED("created"),
    /** The {@code resumed} variant. */
    RESUMED("resumed"),
    /** The {@code not_found} variant. */
    NOT_FOUND("not_found"),
    /** The {@code connected} variant. */
    CONNECTED("connected"),
    /** The {@code handed_off} variant. */
    HANDED_OFF("handed_off");

    private final String value;
    SessionsOpenStatus(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static SessionsOpenStatus fromValue(String value) {
        for (SessionsOpenStatus v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown SessionsOpenStatus value: " + value);
    }
}

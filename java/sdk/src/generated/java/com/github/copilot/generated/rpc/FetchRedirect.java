/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * HTTP redirect policy.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum FetchRedirect {
    /** The {@code follow} variant. */
    FOLLOW("follow"),
    /** The {@code sameOrigin} variant. */
    SAMEORIGIN("sameOrigin"),
    /** The {@code manual} variant. */
    MANUAL("manual"),
    /** The {@code error} variant. */
    ERROR("error");

    private final String value;
    FetchRedirect(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static FetchRedirect fromValue(String value) {
        for (FetchRedirect v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown FetchRedirect value: " + value);
    }
}

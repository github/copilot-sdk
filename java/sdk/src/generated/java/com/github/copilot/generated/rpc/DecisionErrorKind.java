/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Why a decisions request failed.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum DecisionErrorKind {
    /** The {@code notEnabled} variant. */
    NOTENABLED("notEnabled"),
    /** The {@code noModel} variant. */
    NOMODEL("noModel"),
    /** The {@code unauthenticated} variant. */
    UNAUTHENTICATED("unauthenticated"),
    /** The {@code invalidRequest} variant. */
    INVALIDREQUEST("invalidRequest"),
    /** The {@code tooLarge} variant. */
    TOOLARGE("tooLarge"),
    /** The {@code upstreamProtocol} variant. */
    UPSTREAMPROTOCOL("upstreamProtocol"),
    /** The {@code rateLimited} variant. */
    RATELIMITED("rateLimited"),
    /** The {@code timeout} variant. */
    TIMEOUT("timeout"),
    /** The {@code unavailable} variant. */
    UNAVAILABLE("unavailable"),
    /** The {@code overloaded} variant. */
    OVERLOADED("overloaded");

    private final String value;
    DecisionErrorKind(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static DecisionErrorKind fromValue(String value) {
        for (DecisionErrorKind v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown DecisionErrorKind value: " + value);
    }
}

/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * The neutral kind of a model provider — the model analog of `AccountKind`. A model provider is the live, entitled source a model came from; central code never branches on this beyond a single dispatch.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ModelProviderKind {
    /** The {@code copilot} variant. */
    COPILOT("copilot"),
    /** The {@code loki} variant. */
    LOKI("loki");

    private final String value;
    ModelProviderKind(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ModelProviderKind fromValue(String value) {
        for (ModelProviderKind v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ModelProviderKind value: " + value);
    }
}

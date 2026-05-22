/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Neutral SDK discriminator for the connected remote session kind.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ConnectedRemoteSessionMetadataKind {
    /** The {@code remote-session} variant. */
    REMOTE_SESSION("remote-session"),
    /** The {@code coding-agent} variant. */
    CODING_AGENT("coding-agent");

    private final String value;
    ConnectedRemoteSessionMetadataKind(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ConnectedRemoteSessionMetadataKind fromValue(String value) {
        for (ConnectedRemoteSessionMetadataKind v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ConnectedRemoteSessionMetadataKind value: " + value);
    }
}

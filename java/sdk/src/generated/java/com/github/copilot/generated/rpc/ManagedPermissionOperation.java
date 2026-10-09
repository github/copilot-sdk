/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ManagedPermissionOperation(
    /** Absolute HTTP(S) URL with a host. Invalid URLs reject the entire evaluation batch. */
    @JsonProperty("url") String url,
    /** Operation to evaluate. Currently only url is supported. */
    @JsonProperty("kind") ManagedPermissionOperationKind kind
) {

    /** Operation to evaluate. Currently only url is supported. */
    public enum ManagedPermissionOperationKind {
        /** The {@code url} variant. */
        URL("url");

        private final String value;
        ManagedPermissionOperationKind(String value) { this.value = value; }
        @com.fasterxml.jackson.annotation.JsonValue
        public String getValue() { return value; }
        @com.fasterxml.jackson.annotation.JsonCreator
        public static ManagedPermissionOperationKind fromValue(String value) {
            for (ManagedPermissionOperationKind v : values()) {
                if (v.value.equals(value)) return v;
            }
            throw new IllegalArgumentException("Unknown ManagedPermissionOperationKind value: " + value);
        }
    }
}

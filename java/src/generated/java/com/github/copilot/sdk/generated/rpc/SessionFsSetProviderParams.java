/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Request parameters for the {@code sessionFs.setProvider} RPC method.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionFsSetProviderParams(
    /** Initial working directory for sessions */
    @JsonProperty("initialCwd") String initialCwd,
    /** Path within each session's SessionFs where the runtime stores files for that session */
    @JsonProperty("sessionStatePath") String sessionStatePath,
    /** Path conventions used by this filesystem */
    @JsonProperty("conventions") SessionFsSetProviderParamsConventions conventions
) {

    /** Path conventions used by this filesystem */
    public enum SessionFsSetProviderParamsConventions {
        /** The {@code windows} variant. */
        WINDOWS("windows"),
        /** The {@code posix} variant. */
        POSIX("posix");

        private final String value;
        SessionFsSetProviderParamsConventions(String value) { this.value = value; }
        @com.fasterxml.jackson.annotation.JsonValue
        public String getValue() { return value; }
        @com.fasterxml.jackson.annotation.JsonCreator
        public static SessionFsSetProviderParamsConventions fromValue(String value) {
            for (SessionFsSetProviderParamsConventions v : values()) {
                if (v.value.equals(value)) return v;
            }
            throw new IllegalArgumentException("Unknown SessionFsSetProviderParamsConventions value: " + value);
        }
    }
}

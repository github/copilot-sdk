/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Result for the {@code sessionFs.readdirWithTypes} RPC method.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionFsReaddirWithTypesResult(
    /** Directory entries with type information */
    @JsonProperty("entries") List<SessionFsReaddirWithTypesResultEntriesItem> entries
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionFsReaddirWithTypesResultEntriesItem(
        /** Entry name */
        @JsonProperty("name") String name,
        /** Entry type */
        @JsonProperty("type") SessionFsReaddirWithTypesResultEntriesItemType type
    ) {

        /** Entry type */
        public enum SessionFsReaddirWithTypesResultEntriesItemType {
            /** The {@code file} variant. */
            FILE("file"),
            /** The {@code directory} variant. */
            DIRECTORY("directory");

            private final String value;
            SessionFsReaddirWithTypesResultEntriesItemType(String value) { this.value = value; }
            @com.fasterxml.jackson.annotation.JsonValue
            public String getValue() { return value; }
            @com.fasterxml.jackson.annotation.JsonCreator
            public static SessionFsReaddirWithTypesResultEntriesItemType fromValue(String value) {
                for (SessionFsReaddirWithTypesResultEntriesItemType v : values()) {
                    if (v.value.equals(value)) return v;
                }
                throw new IllegalArgumentException("Unknown SessionFsReaddirWithTypesResultEntriesItemType value: " + value);
            }
        }
    }
}

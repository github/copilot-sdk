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
 * Result for the {@code session.extensions.list} RPC method.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionExtensionsListResult(
    /** Discovered extensions and their current status */
    @JsonProperty("extensions") List<SessionExtensionsListResultExtensionsItem> extensions
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionExtensionsListResultExtensionsItem(
        /** Source-qualified ID (e.g., 'project:my-ext', 'user:auth-helper') */
        @JsonProperty("id") String id,
        /** Extension name (directory name) */
        @JsonProperty("name") String name,
        /** Discovery source: project (.github/extensions/) or user (~/.copilot/extensions/) */
        @JsonProperty("source") SessionExtensionsListResultExtensionsItemSource source,
        /** Current status: running, disabled, failed, or starting */
        @JsonProperty("status") SessionExtensionsListResultExtensionsItemStatus status,
        /** Process ID if the extension is running */
        @JsonProperty("pid") Long pid
    ) {

        /** Discovery source: project (.github/extensions/) or user (~/.copilot/extensions/) */
        public enum SessionExtensionsListResultExtensionsItemSource {
            /** The {@code project} variant. */
            PROJECT("project"),
            /** The {@code user} variant. */
            USER("user");

            private final String value;
            SessionExtensionsListResultExtensionsItemSource(String value) { this.value = value; }
            @com.fasterxml.jackson.annotation.JsonValue
            public String getValue() { return value; }
            @com.fasterxml.jackson.annotation.JsonCreator
            public static SessionExtensionsListResultExtensionsItemSource fromValue(String value) {
                for (SessionExtensionsListResultExtensionsItemSource v : values()) {
                    if (v.value.equals(value)) return v;
                }
                throw new IllegalArgumentException("Unknown SessionExtensionsListResultExtensionsItemSource value: " + value);
            }
        }

        /** Current status: running, disabled, failed, or starting */
        public enum SessionExtensionsListResultExtensionsItemStatus {
            /** The {@code running} variant. */
            RUNNING("running"),
            /** The {@code disabled} variant. */
            DISABLED("disabled"),
            /** The {@code failed} variant. */
            FAILED("failed"),
            /** The {@code starting} variant. */
            STARTING("starting");

            private final String value;
            SessionExtensionsListResultExtensionsItemStatus(String value) { this.value = value; }
            @com.fasterxml.jackson.annotation.JsonValue
            public String getValue() { return value; }
            @com.fasterxml.jackson.annotation.JsonCreator
            public static SessionExtensionsListResultExtensionsItemStatus fromValue(String value) {
                for (SessionExtensionsListResultExtensionsItemStatus v : values()) {
                    if (v.value.equals(value)) return v;
                }
                throw new IllegalArgumentException("Unknown SessionExtensionsListResultExtensionsItemStatus value: " + value);
            }
        }
    }
}

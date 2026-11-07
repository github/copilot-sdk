/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * What the server returned for this session
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpAppsDiagnoseServer(
    /** Whether the named server is currently connected */
    @JsonProperty("connected") Boolean connected,
    /** Total tools returned by the server's tools/list */
    @JsonProperty("toolCount") Double toolCount,
    /** Tools whose `_meta.ui` is populated (resourceUri and/or visibility set) */
    @JsonProperty("toolsWithUiMeta") Double toolsWithUiMeta,
    /** Up to 5 tool names with `_meta.ui` for quick inspection */
    @JsonProperty("sampleToolNames") List<String> sampleToolNames
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public McpAppsDiagnoseServer {}

    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    public static final class Builder {
        private Builder() {}
        private Boolean connected;
        private Double toolCount;
        private Double toolsWithUiMeta;
        private List<String> sampleToolNames;

        /**
         * Sets the {@code connected} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder connected(Boolean value) {
            this.connected = value;
            return this;
        }

        /**
         * Sets the {@code toolCount} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder toolCount(Double value) {
            this.toolCount = value;
            return this;
        }

        /**
         * Sets the {@code toolsWithUiMeta} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder toolsWithUiMeta(Double value) {
            this.toolsWithUiMeta = value;
            return this;
        }

        /**
         * Sets the {@code sampleToolNames} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sampleToolNames(List<String> value) {
            this.sampleToolNames = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public McpAppsDiagnoseServer build() {
            if (connected == null) throw new IllegalStateException("Missing required property: connected");
            if (toolCount == null) throw new IllegalStateException("Missing required property: toolCount");
            if (toolsWithUiMeta == null) throw new IllegalStateException("Missing required property: toolsWithUiMeta");
            if (sampleToolNames == null) throw new IllegalStateException("Missing required property: sampleToolNames");
            return new McpAppsDiagnoseServer(connected, toolCount, toolsWithUiMeta, sampleToolNames);
        }
    }
}

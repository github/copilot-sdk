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

/**
 * MCP server filtered by policy, with name, reason, and optional redacted reason.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record McpFilteredServer(
    /** Filtered server name */
    @JsonProperty("name") String name,
    /** Human-readable filter reason */
    @JsonProperty("reason") String reason,
    /** PII-free filter reason */
    @JsonProperty("redactedReason") String redactedReason,
    /** Deprecated. This field is no longer populated. */
    @Deprecated
    @JsonProperty("enterpriseName") String enterpriseName
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    McpFilteredServer {}

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
        private String name;
        private String reason;
        private String redactedReason;
        private String enterpriseName;

        /**
         * Sets the {@code name} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder name(String value) {
            this.name = value;
            return this;
        }

        /**
         * Sets the {@code reason} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder reason(String value) {
            this.reason = value;
            return this;
        }

        /**
         * Sets the {@code redactedReason} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder redactedReason(String value) {
            this.redactedReason = value;
            return this;
        }

        /**
         * Sets the {@code enterpriseName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder enterpriseName(String value) {
            this.enterpriseName = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public McpFilteredServer build() {
            if (name == null) throw new IllegalStateException("Missing required property: name");
            if (reason == null) throw new IllegalStateException("Missing required property: reason");
            return new McpFilteredServer(name, reason, redactedReason, enterpriseName);
        }
    }
}

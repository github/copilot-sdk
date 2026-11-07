/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Metadata advertised by the AHP host, not a guarantee that history can be resumed.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record HostSessionSummary(
    /** Stable AHP resource URI identifying the session on this host. */
    @JsonProperty("resource") String resource,
    /** Authoritative title advertised by the host. */
    @JsonProperty("title") String title,
    /** Session creation time as an ISO 8601 timestamp. */
    @JsonProperty("createdAt") String createdAt,
    /** Last modification time as an ISO 8601 timestamp. */
    @JsonProperty("modifiedAt") String modifiedAt,
    /** Unsigned 32-bit AHP session status bitset (0..=4294967295); unknown bits must be preserved. */
    @JsonProperty("status") Long status,
    /** Optional activity description advertised by the host. */
    @JsonProperty("activity") String activity
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public HostSessionSummary {}

    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static final class Builder {
        private Builder() {}
        private String resource;
        private String title;
        private String createdAt;
        private String modifiedAt;
        private Long status;
        private String activity;

        /**
         * Sets the {@code resource} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder resource(String value) {
            this.resource = value;
            return this;
        }

        /**
         * Sets the {@code title} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder title(String value) {
            this.title = value;
            return this;
        }

        /**
         * Sets the {@code createdAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder createdAt(String value) {
            this.createdAt = value;
            return this;
        }

        /**
         * Sets the {@code modifiedAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder modifiedAt(String value) {
            this.modifiedAt = value;
            return this;
        }

        /**
         * Sets the {@code status} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder status(Long value) {
            this.status = value;
            return this;
        }

        /**
         * Sets the {@code activity} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder activity(String value) {
            this.activity = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public HostSessionSummary build() {
            if (resource == null) throw new IllegalStateException("Missing required property: resource");
            if (title == null) throw new IllegalStateException("Missing required property: title");
            if (createdAt == null) throw new IllegalStateException("Missing required property: createdAt");
            if (modifiedAt == null) throw new IllegalStateException("Missing required property: modifiedAt");
            if (status == null) throw new IllegalStateException("Missing required property: status");
            return new HostSessionSummary(resource, title, createdAt, modifiedAt, status, activity);
        }
    }
}

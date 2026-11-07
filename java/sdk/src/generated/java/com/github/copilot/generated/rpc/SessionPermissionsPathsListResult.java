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
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Snapshot of the session's recursive directory grants, exact session-approved paths, and primary working directory.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionPermissionsPathsListResult(
    /** All directories currently allowed for tool access on this session. */
    @JsonProperty("directories") List<String> directories,
    /** Exact paths approved for this session without recursively allowing their descendants. */
    @JsonProperty("sessionApprovedPaths") List<String> sessionApprovedPaths,
    /** The primary working directory for this session. */
    @JsonProperty("primary") String primary
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionPermissionsPathsListResult {}

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
        private List<String> directories;
        private List<String> sessionApprovedPaths;
        private String primary;

        /**
         * Sets the {@code directories} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder directories(List<String> value) {
            this.directories = value;
            return this;
        }

        /**
         * Sets the {@code sessionApprovedPaths} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sessionApprovedPaths(List<String> value) {
            this.sessionApprovedPaths = value;
            return this;
        }

        /**
         * Sets the {@code primary} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder primary(String value) {
            this.primary = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionPermissionsPathsListResult build() {
            if (directories == null) throw new IllegalStateException("Missing required property: directories");
            if (primary == null) throw new IllegalStateException("Missing required property: primary");
            return new SessionPermissionsPathsListResult(directories, sessionApprovedPaths, primary);
        }
    }
}

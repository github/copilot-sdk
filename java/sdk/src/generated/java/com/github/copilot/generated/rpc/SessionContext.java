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
 * Pre-resolved working-directory context for session startup.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionContext(
    /** Most recent working directory for this session */
    @JsonProperty("cwd") String cwd,
    /** Git repository root, if the cwd was inside a git repo */
    @JsonProperty("gitRoot") String gitRoot,
    /** Repository slug in `owner/name` form, when known */
    @JsonProperty("repository") String repository,
    /** Repository host type */
    @JsonProperty("hostType") SessionContextHostType hostType,
    /** Active git branch */
    @JsonProperty("branch") String branch
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionContext {}

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
        private String cwd;
        private String gitRoot;
        private String repository;
        private SessionContextHostType hostType;
        private String branch;

        /**
         * Sets the {@code cwd} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder cwd(String value) {
            this.cwd = value;
            return this;
        }

        /**
         * Sets the {@code gitRoot} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder gitRoot(String value) {
            this.gitRoot = value;
            return this;
        }

        /**
         * Sets the {@code repository} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder repository(String value) {
            this.repository = value;
            return this;
        }

        /**
         * Sets the {@code hostType} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder hostType(SessionContextHostType value) {
            this.hostType = value;
            return this;
        }

        /**
         * Sets the {@code branch} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder branch(String value) {
            this.branch = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionContext build() {
            if (cwd == null) throw new IllegalStateException("Missing required property: cwd");
            return new SessionContext(cwd, gitRoot, repository, hostType, branch);
        }
    }
}

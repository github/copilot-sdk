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
 * Per-session context-board metadata. Configuration does not open the store or grant a store capability.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionDynamicContextConfig(
    /** Session-store path on the runtime host, preserved as supplied. */
    @JsonProperty("storePath") String storePath,
    /** Repository identifier for the context board. */
    @JsonProperty("repository") String repository,
    /** Branch identifier for the context board. */
    @JsonProperty("branch") String branch
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionDynamicContextConfig {}

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
        private String storePath;
        private String repository;
        private String branch;

        /**
         * Sets the {@code storePath} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder storePath(String value) {
            this.storePath = value;
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
        public SessionDynamicContextConfig build() {
            if (storePath == null) throw new IllegalStateException("Missing required property: storePath");
            if (repository == null) throw new IllegalStateException("Missing required property: repository");
            if (branch == null) throw new IllegalStateException("Missing required property: branch");
            return new SessionDynamicContextConfig(storePath, repository, branch);
        }
    }
}

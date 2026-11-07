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
 * The remote the checked-out branch tracks.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record GitCurrentBranchRemoteResult(
    /** Name of the tracked remote. Reports `origin` whenever the working tree has no tracking configuration to read, including on a detached HEAD, so this is never null and never empty. */
    @JsonProperty("remote") String remote
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    GitCurrentBranchRemoteResult {}

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
        private String remote;

        /**
         * Sets the {@code remote} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder remote(String value) {
            this.remote = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public GitCurrentBranchRemoteResult build() {
            if (remote == null) throw new IllegalStateException("Missing required property: remote");
            return new GitCurrentBranchRemoteResult(remote);
        }
    }
}

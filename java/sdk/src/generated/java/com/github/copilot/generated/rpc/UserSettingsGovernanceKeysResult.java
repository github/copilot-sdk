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
 * Top-level user-setting keys grouped by who may govern them.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record UserSettingsGovernanceKeysResult(
    /** Top-level keys a repository may override through `.github/copilot/settings.json` or `.github/copilot/settings.local.json`. */
    @JsonProperty("repo") List<String> repo,
    /** Top-level keys a repository may override through a Claude-compatible repository config. */
    @JsonProperty("claudeRepo") List<String> claudeRepo,
    /** Top-level keys an enterprise policy may fix through managed settings. */
    @JsonProperty("managed") List<String> managed
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    UserSettingsGovernanceKeysResult {}

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
        private List<String> repo;
        private List<String> claudeRepo;
        private List<String> managed;

        /**
         * Sets the {@code repo} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder repo(List<String> value) {
            this.repo = value;
            return this;
        }

        /**
         * Sets the {@code claudeRepo} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder claudeRepo(List<String> value) {
            this.claudeRepo = value;
            return this;
        }

        /**
         * Sets the {@code managed} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder managed(List<String> value) {
            this.managed = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public UserSettingsGovernanceKeysResult build() {
            if (repo == null) throw new IllegalStateException("Missing required property: repo");
            if (claudeRepo == null) throw new IllegalStateException("Missing required property: claudeRepo");
            if (managed == null) throw new IllegalStateException("Missing required property: managed");
            return new UserSettingsGovernanceKeysResult(repo, claudeRepo, managed);
        }
    }
}

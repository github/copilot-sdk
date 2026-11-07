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
 * Redacted repository and GitHub host settings for a session.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record SessionSettingsRepoSnapshot(
    /** Repository name. */
    @JsonProperty("name") String name,
    /** GitHub repository database ID. */
    @JsonProperty("id") Double id,
    /** Checked-out repository branch. */
    @JsonProperty("branch") String branch,
    /** Checked-out commit SHA. */
    @JsonProperty("commit") String commit,
    /** Whether the repository is writable. */
    @JsonProperty("readWrite") Boolean readWrite,
    /** Repository owner login. */
    @JsonProperty("ownerName") String ownerName,
    /** GitHub repository owner database ID. */
    @JsonProperty("ownerId") Double ownerId,
    /** GitHub server base URL. */
    @JsonProperty("serverUrl") String serverUrl,
    /** GitHub server host name. */
    @JsonProperty("host") String host,
    /** Protocol used to access the GitHub host. */
    @JsonProperty("hostProtocol") String hostProtocol,
    /** GitHub secret-scanning service URL. */
    @JsonProperty("secretScanningUrl") String secretScanningUrl,
    /** Number of commits in the pull request. */
    @JsonProperty("prCommitCount") Double prCommitCount
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    SessionSettingsRepoSnapshot {}

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
        private Double id;
        private String branch;
        private String commit;
        private Boolean readWrite;
        private String ownerName;
        private Double ownerId;
        private String serverUrl;
        private String host;
        private String hostProtocol;
        private String secretScanningUrl;
        private Double prCommitCount;

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
         * Sets the {@code id} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder id(Double value) {
            this.id = value;
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
         * Sets the {@code commit} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder commit(String value) {
            this.commit = value;
            return this;
        }

        /**
         * Sets the {@code readWrite} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder readWrite(Boolean value) {
            this.readWrite = value;
            return this;
        }

        /**
         * Sets the {@code ownerName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder ownerName(String value) {
            this.ownerName = value;
            return this;
        }

        /**
         * Sets the {@code ownerId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder ownerId(Double value) {
            this.ownerId = value;
            return this;
        }

        /**
         * Sets the {@code serverUrl} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder serverUrl(String value) {
            this.serverUrl = value;
            return this;
        }

        /**
         * Sets the {@code host} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder host(String value) {
            this.host = value;
            return this;
        }

        /**
         * Sets the {@code hostProtocol} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder hostProtocol(String value) {
            this.hostProtocol = value;
            return this;
        }

        /**
         * Sets the {@code secretScanningUrl} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder secretScanningUrl(String value) {
            this.secretScanningUrl = value;
            return this;
        }

        /**
         * Sets the {@code prCommitCount} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder prCommitCount(Double value) {
            this.prCommitCount = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionSettingsRepoSnapshot build() {
            return new SessionSettingsRepoSnapshot(name, id, branch, commit, readWrite, ownerName, ownerId, serverUrl, host, hostProtocol, secretScanningUrl, prCommitCount);
        }
    }
}

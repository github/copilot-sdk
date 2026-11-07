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
 * Immutable options for one connection-owned release snapshot.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record GitHubReleaseGetSnapshotParams(
    /** An unused id from gitHubRelease.allocateRequestId. Reuse the same id and options to read the same snapshot. */
    @JsonProperty("requestId") Long requestId,
    /** The release channel used for update selection. */
    @JsonProperty("updateChannel") GitHubReleaseUpdateChannel updateChannel,
    /** The version embedded in the caller's build. */
    @JsonProperty("buildVersion") String buildVersion,
    /** Optional runtime version override. Invalid or noncanonical values do not replace buildVersion. */
    @JsonProperty("cliVersionOverride") String cliVersionOverride,
    /** The embedded package identity. Omission uses the existing development-version fallback rule. */
    @JsonProperty("packageIdentity") GitHubReleasePackageIdentity packageIdentity
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    GitHubReleaseGetSnapshotParams {}

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
        private Long requestId;
        private GitHubReleaseUpdateChannel updateChannel;
        private String buildVersion;
        private String cliVersionOverride;
        private GitHubReleasePackageIdentity packageIdentity;

        /**
         * Sets the {@code requestId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder requestId(Long value) {
            this.requestId = value;
            return this;
        }

        /**
         * Sets the {@code updateChannel} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder updateChannel(GitHubReleaseUpdateChannel value) {
            this.updateChannel = value;
            return this;
        }

        /**
         * Sets the {@code buildVersion} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder buildVersion(String value) {
            this.buildVersion = value;
            return this;
        }

        /**
         * Sets the {@code cliVersionOverride} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder cliVersionOverride(String value) {
            this.cliVersionOverride = value;
            return this;
        }

        /**
         * Sets the {@code packageIdentity} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder packageIdentity(GitHubReleasePackageIdentity value) {
            this.packageIdentity = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public GitHubReleaseGetSnapshotParams build() {
            if (requestId == null) throw new IllegalStateException("Missing required property: requestId");
            if (updateChannel == null) throw new IllegalStateException("Missing required property: updateChannel");
            if (buildVersion == null) throw new IllegalStateException("Missing required property: buildVersion");
            return new GitHubReleaseGetSnapshotParams(requestId, updateChannel, buildVersion, cliVersionOverride, packageIdentity);
        }
    }
}

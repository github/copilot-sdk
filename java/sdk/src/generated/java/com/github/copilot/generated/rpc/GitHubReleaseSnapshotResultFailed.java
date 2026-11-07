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
 * The release lookup failed.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
final class GitHubReleaseSnapshotResultFailed extends GitHubReleaseSnapshotResult {

    @JsonProperty("outcome")
    private final String outcome = "failed";

    @Override
    public String getOutcome() { return outcome; }

    /** The original update channel. */
    @JsonProperty("updateChannel")
    private GitHubReleaseUpdateChannel updateChannel;

    /** The original lookup duration in milliseconds. */
    @JsonProperty("lookupDurationMs")
    private Double lookupDurationMs;

    /** The original user-facing lookup failure. */
    @JsonProperty("error")
    private String error;

    public GitHubReleaseUpdateChannel getUpdateChannel() { return updateChannel; }
    public void setUpdateChannel(GitHubReleaseUpdateChannel updateChannel) { this.updateChannel = updateChannel; }

    public Double getLookupDurationMs() { return lookupDurationMs; }
    public void setLookupDurationMs(Double lookupDurationMs) { this.lookupDurationMs = lookupDurationMs; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }


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
        private GitHubReleaseUpdateChannel updateChannel;
        private Double lookupDurationMs;
        private String error;

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
         * Sets the {@code lookupDurationMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder lookupDurationMs(Double value) {
            this.lookupDurationMs = value;
            return this;
        }

        /**
         * Sets the {@code error} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder error(String value) {
            this.error = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public GitHubReleaseSnapshotResultFailed build() {
            if (updateChannel == null) throw new IllegalStateException("Missing required property: updateChannel");
            if (lookupDurationMs == null) throw new IllegalStateException("Missing required property: lookupDurationMs");
            if (error == null) throw new IllegalStateException("Missing required property: error");
            GitHubReleaseSnapshotResultFailed instance = new GitHubReleaseSnapshotResultFailed();
            instance.setUpdateChannel(updateChannel);
            instance.setLookupDurationMs(lookupDurationMs);
            instance.setError(error);
            return instance;
        }
    }
}

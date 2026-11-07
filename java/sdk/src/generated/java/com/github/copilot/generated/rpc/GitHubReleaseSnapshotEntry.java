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
 * Release facts needed for update selection and admission. GitHub field names remain unchanged.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record GitHubReleaseSnapshotEntry(
    /** The release tag. */
    @JsonProperty("tag_name") String tagName,
    /** Whether GitHub marks this release as a prerelease. */
    @JsonProperty("prerelease") Boolean prerelease,
    /** Whether GitHub marks this release as a draft. */
    @JsonProperty("draft") Boolean draft,
    /** The published release assets. */
    @JsonProperty("assets") List<GitHubReleaseSnapshotAsset> assets
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    GitHubReleaseSnapshotEntry {}

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
        private String tagName;
        private Boolean prerelease;
        private Boolean draft;
        private List<GitHubReleaseSnapshotAsset> assets;

        /**
         * Sets the {@code tag_name} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder tagName(String value) {
            this.tagName = value;
            return this;
        }

        /**
         * Sets the {@code prerelease} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder prerelease(Boolean value) {
            this.prerelease = value;
            return this;
        }

        /**
         * Sets the {@code draft} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder draft(Boolean value) {
            this.draft = value;
            return this;
        }

        /**
         * Sets the {@code assets} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder assets(List<GitHubReleaseSnapshotAsset> value) {
            this.assets = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public GitHubReleaseSnapshotEntry build() {
            if (tagName == null) throw new IllegalStateException("Missing required property: tag_name");
            if (prerelease == null) throw new IllegalStateException("Missing required property: prerelease");
            if (draft == null) throw new IllegalStateException("Missing required property: draft");
            if (assets == null) throw new IllegalStateException("Missing required property: assets");
            return new GitHubReleaseSnapshotEntry(tagName, prerelease, draft, assets);
        }
    }
}

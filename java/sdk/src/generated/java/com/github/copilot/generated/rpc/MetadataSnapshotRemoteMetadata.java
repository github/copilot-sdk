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
 * Remote-session-specific metadata. Populated only when `isRemote` is true. Fields are immutable for the lifetime of the session.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record MetadataSnapshotRemoteMetadata(
    /** The original resource identifier (task ID or PR node ID), preserved across event-replay reconstructions. Falls back to `sessionId` when absent. */
    @JsonProperty("resourceId") String resourceId,
    /** The repository the remote session targets. */
    @JsonProperty("repository") MetadataSnapshotRemoteMetadataRepository repository,
    /** The pull request number the remote session is associated with, if any. */
    @JsonProperty("pullRequestNumber") Long pullRequestNumber,
    /** Whether the remote task originated from Copilot Coding Agent (cca) or a CLI `--remote` invocation. */
    @JsonProperty("taskType") MetadataSnapshotRemoteMetadataTaskType taskType
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public MetadataSnapshotRemoteMetadata {}

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
        private String resourceId;
        private MetadataSnapshotRemoteMetadataRepository repository;
        private Long pullRequestNumber;
        private MetadataSnapshotRemoteMetadataTaskType taskType;

        /**
         * Sets the {@code resourceId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder resourceId(String value) {
            this.resourceId = value;
            return this;
        }

        /**
         * Sets the {@code repository} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder repository(MetadataSnapshotRemoteMetadataRepository value) {
            this.repository = value;
            return this;
        }

        /**
         * Sets the {@code pullRequestNumber} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder pullRequestNumber(Long value) {
            this.pullRequestNumber = value;
            return this;
        }

        /**
         * Sets the {@code taskType} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder taskType(MetadataSnapshotRemoteMetadataTaskType value) {
            this.taskType = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public MetadataSnapshotRemoteMetadata build() {
            if (repository == null) throw new IllegalStateException("Missing required property: repository");
            return new MetadataSnapshotRemoteMetadata(resourceId, repository, pullRequestNumber, taskType);
        }
    }
}

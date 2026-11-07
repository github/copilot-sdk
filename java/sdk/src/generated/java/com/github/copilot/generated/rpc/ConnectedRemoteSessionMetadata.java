/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import javax.annotation.processing.Generated;

/**
 * Metadata for a connected remote session.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ConnectedRemoteSessionMetadata(
    /** SDK session ID for the connected remote session. */
    @JsonProperty("sessionId") String sessionId,
    /** Optional friendly session name. */
    @JsonProperty("name") String name,
    /** Optional session summary. */
    @JsonProperty("summary") String summary,
    /** Session start time as an ISO 8601 string. */
    @JsonProperty("startTime") OffsetDateTime startTime,
    /** Last session update time as an ISO 8601 string. */
    @JsonProperty("modifiedTime") OffsetDateTime modifiedTime,
    /** Repository associated with the connected remote session. */
    @JsonProperty("repository") ConnectedRemoteSessionMetadataRepository repository,
    /** Pull request number associated with the session. */
    @JsonProperty("pullRequestNumber") Long pullRequestNumber,
    /** Original remote resource identifier. */
    @JsonProperty("resourceId") String resourceId,
    /** Neutral SDK discriminator for the connected remote session kind. */
    @JsonProperty("kind") ConnectedRemoteSessionMetadataKind kind,
    /** Remote session staleness deadline as an ISO 8601 string. */
    @JsonProperty("staleAt") OffsetDateTime staleAt,
    /** Remote session state returned by the backing service. */
    @JsonProperty("state") String state
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ConnectedRemoteSessionMetadata {}

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
        private String sessionId;
        private String name;
        private String summary;
        private OffsetDateTime startTime;
        private OffsetDateTime modifiedTime;
        private ConnectedRemoteSessionMetadataRepository repository;
        private Long pullRequestNumber;
        private String resourceId;
        private ConnectedRemoteSessionMetadataKind kind;
        private OffsetDateTime staleAt;
        private String state;

        /**
         * Sets the {@code sessionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sessionId(String value) {
            this.sessionId = value;
            return this;
        }

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
         * Sets the {@code summary} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder summary(String value) {
            this.summary = value;
            return this;
        }

        /**
         * Sets the {@code startTime} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder startTime(OffsetDateTime value) {
            this.startTime = value;
            return this;
        }

        /**
         * Sets the {@code modifiedTime} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder modifiedTime(OffsetDateTime value) {
            this.modifiedTime = value;
            return this;
        }

        /**
         * Sets the {@code repository} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder repository(ConnectedRemoteSessionMetadataRepository value) {
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
         * Sets the {@code kind} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder kind(ConnectedRemoteSessionMetadataKind value) {
            this.kind = value;
            return this;
        }

        /**
         * Sets the {@code staleAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder staleAt(OffsetDateTime value) {
            this.staleAt = value;
            return this;
        }

        /**
         * Sets the {@code state} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder state(String value) {
            this.state = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ConnectedRemoteSessionMetadata build() {
            if (sessionId == null) throw new IllegalStateException("Missing required property: sessionId");
            if (startTime == null) throw new IllegalStateException("Missing required property: startTime");
            if (modifiedTime == null) throw new IllegalStateException("Missing required property: modifiedTime");
            if (repository == null) throw new IllegalStateException("Missing required property: repository");
            if (kind == null) throw new IllegalStateException("Missing required property: kind");
            return new ConnectedRemoteSessionMetadata(sessionId, name, summary, startTime, modifiedTime, repository, pullRequestNumber, resourceId, kind, staleAt, state);
        }
    }
}

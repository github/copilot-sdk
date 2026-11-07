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
 * Remote session metadata for the session to hand off (typically obtained from `sessions.list` with `source: "remote"`).
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record RemoteSessionMetadataValue(
    /** Stable session identifier. */
    @JsonProperty("sessionId") String sessionId,
    /** Session creation time as an ISO 8601 timestamp. */
    @JsonProperty("startTime") String startTime,
    /** Last-modified time as an ISO 8601 timestamp. */
    @JsonProperty("modifiedTime") String modifiedTime,
    /** Short summary of the session, when one has been derived. */
    @JsonProperty("summary") String summary,
    /** Optional human-friendly name set via /rename. */
    @JsonProperty("name") String name,
    /** Always true for remote sessions. */
    @JsonProperty("isRemote") Boolean isRemote,
    /** Most recent working directory context. */
    @JsonProperty("context") SessionContext context,
    /** GitHub repository the remote session belongs to. */
    @JsonProperty("repository") RemoteSessionMetadataRepository repository,
    /** Backing remote session IDs (most recent first). */
    @JsonProperty("remoteSessionIds") List<String> remoteSessionIds,
    /** Pull request number associated with the session. */
    @JsonProperty("pullRequestNumber") Long pullRequestNumber,
    /** Original remote resource identifier (task ID or PR node ID). */
    @JsonProperty("resourceId") String resourceId,
    /** Whether the remote task originated from CCA or CLI `--remote`. */
    @JsonProperty("taskType") RemoteSessionMetadataTaskType taskType,
    /** Deadline (ISO 8601) at which a CLI remote session becomes stale without further heartbeats. */
    @JsonProperty("staleAt") String staleAt,
    /** Server-side task state returned by GitHub. */
    @JsonProperty("state") String state,
    /** Live status as the owning host reports it in its session listing, so a row for a session running elsewhere can show that it is running. Absent for hosts that publish no such status (the cloud task managers), which read as idle. */
    @JsonProperty("hostStatus") RemoteSessionHostStatus hostStatus,
    /** Host-supplied human description of what the session is doing right now ("running tests", "waiting for approval"). Optional in the protocol and absent on hosts that do not publish it, so never rely on it -- it enriches `hostStatus`, it does not replace it. */
    @JsonProperty("hostActivity") String hostActivity
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public RemoteSessionMetadataValue {}

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
        private String startTime;
        private String modifiedTime;
        private String summary;
        private String name;
        private Boolean isRemote;
        private SessionContext context;
        private RemoteSessionMetadataRepository repository;
        private List<String> remoteSessionIds;
        private Long pullRequestNumber;
        private String resourceId;
        private RemoteSessionMetadataTaskType taskType;
        private String staleAt;
        private String state;
        private RemoteSessionHostStatus hostStatus;
        private String hostActivity;

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
         * Sets the {@code startTime} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder startTime(String value) {
            this.startTime = value;
            return this;
        }

        /**
         * Sets the {@code modifiedTime} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder modifiedTime(String value) {
            this.modifiedTime = value;
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
         * Sets the {@code isRemote} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder isRemote(Boolean value) {
            this.isRemote = value;
            return this;
        }

        /**
         * Sets the {@code context} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder context(SessionContext value) {
            this.context = value;
            return this;
        }

        /**
         * Sets the {@code repository} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder repository(RemoteSessionMetadataRepository value) {
            this.repository = value;
            return this;
        }

        /**
         * Sets the {@code remoteSessionIds} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder remoteSessionIds(List<String> value) {
            this.remoteSessionIds = value;
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
         * Sets the {@code taskType} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder taskType(RemoteSessionMetadataTaskType value) {
            this.taskType = value;
            return this;
        }

        /**
         * Sets the {@code staleAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder staleAt(String value) {
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
         * Sets the {@code hostStatus} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder hostStatus(RemoteSessionHostStatus value) {
            this.hostStatus = value;
            return this;
        }

        /**
         * Sets the {@code hostActivity} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder hostActivity(String value) {
            this.hostActivity = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public RemoteSessionMetadataValue build() {
            if (sessionId == null) throw new IllegalStateException("Missing required property: sessionId");
            if (startTime == null) throw new IllegalStateException("Missing required property: startTime");
            if (modifiedTime == null) throw new IllegalStateException("Missing required property: modifiedTime");
            if (isRemote == null) throw new IllegalStateException("Missing required property: isRemote");
            if (repository == null) throw new IllegalStateException("Missing required property: repository");
            if (remoteSessionIds == null) throw new IllegalStateException("Missing required property: remoteSessionIds");
            return new RemoteSessionMetadataValue(sessionId, startTime, modifiedTime, summary, name, isRemote, context, repository, remoteSessionIds, pullRequestNumber, resourceId, taskType, staleAt, state, hostStatus, hostActivity);
        }
    }
}

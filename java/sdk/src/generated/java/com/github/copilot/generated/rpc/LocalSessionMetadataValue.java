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
 * Persisted local session metadata, including identifiers, timestamps, summary/name, client, context, detached state, and task ID.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record LocalSessionMetadataValue(
    /** Stable session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Session creation time as an ISO 8601 timestamp */
    @JsonProperty("startTime") String startTime,
    /** Last-modified time of the session's persisted state, as ISO 8601 */
    @JsonProperty("modifiedTime") String modifiedTime,
    /** Short summary of the session, when one has been derived */
    @JsonProperty("summary") String summary,
    /** Optional human-friendly name set via /rename */
    @JsonProperty("name") String name,
    /** Runtime client name that created this session */
    @JsonProperty("clientName") String clientName,
    /** Always false for local sessions. */
    @JsonProperty("isRemote") Boolean isRemote,
    /** True for detached maintenance sessions that should be hidden from normal resume lists. */
    @JsonProperty("isDetached") Boolean isDetached,
    /** Pre-resolved working-directory context for session startup. */
    @JsonProperty("context") SessionContext context,
    /** GitHub task ID, when this local session is bound to one. Only present for local sessions exported to remote control. */
    @JsonProperty("mcTaskId") String mcTaskId,
    /** Runtime client name that most recently ran this session: the client that created it, or a client that resumed it and then ran it. Resuming a session only to observe it does not change this. Omitted when unknown, such as for sessions persisted before this field existed. */
    @JsonProperty("lastClientName") String lastClientName
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public LocalSessionMetadataValue {}

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
        private String clientName;
        private Boolean isRemote;
        private Boolean isDetached;
        private SessionContext context;
        private String mcTaskId;
        private String lastClientName;

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
         * Sets the {@code clientName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder clientName(String value) {
            this.clientName = value;
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
         * Sets the {@code isDetached} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder isDetached(Boolean value) {
            this.isDetached = value;
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
         * Sets the {@code mcTaskId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder mcTaskId(String value) {
            this.mcTaskId = value;
            return this;
        }

        /**
         * Sets the {@code lastClientName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder lastClientName(String value) {
            this.lastClientName = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public LocalSessionMetadataValue build() {
            if (sessionId == null) throw new IllegalStateException("Missing required property: sessionId");
            if (startTime == null) throw new IllegalStateException("Missing required property: startTime");
            if (modifiedTime == null) throw new IllegalStateException("Missing required property: modifiedTime");
            if (isRemote == null) throw new IllegalStateException("Missing required property: isRemote");
            return new LocalSessionMetadataValue(sessionId, startTime, modifiedTime, summary, name, clientName, isRemote, isDetached, context, mcTaskId, lastClientName);
        }
    }

    /**
     * Creates local session metadata without the last client name.
     * @deprecated Use named construction via {@link #builder()}.
     * @param sessionId Stable session identifier
     * @param startTime Session creation time as an ISO 8601 timestamp
     * @param modifiedTime Last-modified time of the session's persisted state, as ISO 8601
     * @param summary Short summary of the session, when one has been derived
     * @param name Optional human-friendly name set via /rename
     * @param clientName Runtime client name that created this session
     * @param isRemote Always false for local sessions.
     * @param isDetached True for detached maintenance sessions that should be hidden from normal resume lists.
     * @param context Pre-resolved working-directory context for session startup.
     * @param mcTaskId GitHub task ID, when this local session is bound to one. Only present for local sessions exported to remote control.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public LocalSessionMetadataValue(
        String sessionId,
        String startTime,
        String modifiedTime,
        String summary,
        String name,
        String clientName,
        Boolean isRemote,
        Boolean isDetached,
        SessionContext context,
        String mcTaskId
    ) {
        this(sessionId, startTime, modifiedTime, summary, name, clientName, isRemote, isDetached, context, mcTaskId, null);
    }
}

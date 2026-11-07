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
 * Result of opening a session.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionsOpenResult(
    /** Outcome of the open request. */
    @JsonProperty("status") SessionsOpenStatus status,
    /** Opened session ID. Omitted when status is `not_found`. */
    @JsonProperty("sessionId") String sessionId,
    /** In-process SessionClientApi handle for the opened session, returned to CLI callers as a transitional shortcut. Marked internal so the public SDK surface does not expose it; SDK consumers should construct per-session clients from `sessionId` instead. */
    @JsonProperty("sessionApi") Object sessionApi,
    /** Startup prompts queued by user-level hook configs at session creation. Only populated when status is `created`; resumed sessions return an empty array. */
    @JsonProperty("startupPrompts") List<String> startupPrompts,
    /** Remote session ID, present when status is `connected`. */
    @JsonProperty("remoteSessionId") String remoteSessionId,
    /** Remote session metadata, present when status is `connected`. */
    @JsonProperty("metadata") RemoteSessionMetadataValue metadata,
    /** Handoff progress steps, present when status is `handed_off`. */
    @JsonProperty("progress") List<SessionsOpenProgress> progress
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionsOpenResult {}

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
        private SessionsOpenStatus status;
        private String sessionId;
        private Object sessionApi;
        private List<String> startupPrompts;
        private String remoteSessionId;
        private RemoteSessionMetadataValue metadata;
        private List<SessionsOpenProgress> progress;

        /**
         * Sets the {@code status} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder status(SessionsOpenStatus value) {
            this.status = value;
            return this;
        }

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
         * Sets the {@code sessionApi} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sessionApi(Object value) {
            this.sessionApi = value;
            return this;
        }

        /**
         * Sets the {@code startupPrompts} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder startupPrompts(List<String> value) {
            this.startupPrompts = value;
            return this;
        }

        /**
         * Sets the {@code remoteSessionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder remoteSessionId(String value) {
            this.remoteSessionId = value;
            return this;
        }

        /**
         * Sets the {@code metadata} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder metadata(RemoteSessionMetadataValue value) {
            this.metadata = value;
            return this;
        }

        /**
         * Sets the {@code progress} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder progress(List<SessionsOpenProgress> value) {
            this.progress = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionsOpenResult build() {
            if (status == null) throw new IllegalStateException("Missing required property: status");
            return new SessionsOpenResult(status, sessionId, sessionApi, startupPrompts, remoteSessionId, metadata, progress);
        }
    }
}

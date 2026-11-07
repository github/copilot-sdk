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
 * Parameters for connecting to a live remote session.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionsOpenRemote extends SessionsOpenParams {

    @JsonProperty("kind")
    private final String kind = "remote";

    @Override
    public String getKind() { return kind; }

    /** Remote session identifier to connect to. */
    @JsonProperty("remoteSessionId")
    private String remoteSessionId;

    /** Repository context for the remote session. */
    @JsonProperty("repository")
    private RemoteSessionRepository repository;

    /** Session options for the connection. */
    @JsonProperty("options")
    private SessionOpenOptions options;

    public String getRemoteSessionId() { return remoteSessionId; }
    public void setRemoteSessionId(String remoteSessionId) { this.remoteSessionId = remoteSessionId; }

    public RemoteSessionRepository getRepository() { return repository; }
    public void setRepository(RemoteSessionRepository repository) { this.repository = repository; }

    public SessionOpenOptions getOptions() { return options; }
    public void setOptions(SessionOpenOptions options) { this.options = options; }


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
        private String remoteSessionId;
        private RemoteSessionRepository repository;
        private SessionOpenOptions options;

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
         * Sets the {@code repository} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder repository(RemoteSessionRepository value) {
            this.repository = value;
            return this;
        }

        /**
         * Sets the {@code options} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder options(SessionOpenOptions value) {
            this.options = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionsOpenRemote build() {
            if (remoteSessionId == null) throw new IllegalStateException("Missing required property: remoteSessionId");
            SessionsOpenRemote instance = new SessionsOpenRemote();
            instance.setRemoteSessionId(remoteSessionId);
            instance.setRepository(repository);
            instance.setOptions(options);
            return instance;
        }
    }
}

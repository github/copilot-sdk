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
 * Parameters for creating a new cloud session.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionsOpenCloud extends SessionsOpenParams {

    @JsonProperty("kind")
    private final String kind = "cloud";

    @Override
    public String getKind() { return kind; }

    /** Repository for the cloud session. */
    @JsonProperty("repository")
    private RemoteSessionRepository repository;

    /** Optional owner (user or organization login) to associate with the cloud session when no repository is provided. Ignored when `repository` is set (the repo's owner takes precedence). */
    @JsonProperty("owner")
    private String owner;

    /** Session options for cloud session creation. */
    @JsonProperty("options")
    private SessionOpenOptions options;

    /** In-process callback invoked when the cloud task is created, before connection. Internal because function references cannot cross the JSON-RPC boundary. */
    @JsonProperty("onTaskCreated")
    private Object onTaskCreated;

    public RemoteSessionRepository getRepository() { return repository; }
    public void setRepository(RemoteSessionRepository repository) { this.repository = repository; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public SessionOpenOptions getOptions() { return options; }
    public void setOptions(SessionOpenOptions options) { this.options = options; }

    public Object getOnTaskCreated() { return onTaskCreated; }
    public void setOnTaskCreated(Object onTaskCreated) { this.onTaskCreated = onTaskCreated; }


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
        private RemoteSessionRepository repository;
        private String owner;
        private SessionOpenOptions options;
        private Object onTaskCreated;

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
         * Sets the {@code owner} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder owner(String value) {
            this.owner = value;
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
         * Sets the {@code onTaskCreated} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder onTaskCreated(Object value) {
            this.onTaskCreated = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionsOpenCloud build() {
            SessionsOpenCloud instance = new SessionsOpenCloud();
            instance.setRepository(repository);
            instance.setOwner(owner);
            instance.setOptions(options);
            instance.setOnTaskCreated(onTaskCreated);
            return instance;
        }
    }
}

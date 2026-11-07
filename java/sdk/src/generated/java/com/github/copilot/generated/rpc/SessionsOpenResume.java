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
 * Parameters for resuming a specific local session.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionsOpenResume extends SessionsOpenParams {

    @JsonProperty("kind")
    private final String kind = "resume";

    @Override
    public String getKind() { return kind; }

    /** Session ID or unique prefix to resume. */
    @JsonProperty("sessionId")
    private String sessionId;

    /** Session resume options. */
    @JsonProperty("options")
    private SessionOpenOptions options;

    /** Whether to emit session.resume after loading. Defaults to true. */
    @JsonProperty("resume")
    private Boolean resume;

    /** Suppress workspace.yaml metadata writeback when resuming from an incidental cwd. */
    @JsonProperty("suppressResumeWorkspaceMetadataWriteback")
    private Boolean suppressResumeWorkspaceMetadataWriteback;

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public SessionOpenOptions getOptions() { return options; }
    public void setOptions(SessionOpenOptions options) { this.options = options; }

    public Boolean getResume() { return resume; }
    public void setResume(Boolean resume) { this.resume = resume; }

    public Boolean getSuppressResumeWorkspaceMetadataWriteback() { return suppressResumeWorkspaceMetadataWriteback; }
    public void setSuppressResumeWorkspaceMetadataWriteback(Boolean suppressResumeWorkspaceMetadataWriteback) { this.suppressResumeWorkspaceMetadataWriteback = suppressResumeWorkspaceMetadataWriteback; }


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
        private SessionOpenOptions options;
        private Boolean resume;
        private Boolean suppressResumeWorkspaceMetadataWriteback;

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
         * Sets the {@code resume} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder resume(Boolean value) {
            this.resume = value;
            return this;
        }

        /**
         * Sets the {@code suppressResumeWorkspaceMetadataWriteback} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder suppressResumeWorkspaceMetadataWriteback(Boolean value) {
            this.suppressResumeWorkspaceMetadataWriteback = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionsOpenResume build() {
            if (sessionId == null) throw new IllegalStateException("Missing required property: sessionId");
            SessionsOpenResume instance = new SessionsOpenResume();
            instance.setSessionId(sessionId);
            instance.setOptions(options);
            instance.setResume(resume);
            instance.setSuppressResumeWorkspaceMetadataWriteback(suppressResumeWorkspaceMetadataWriteback);
            return instance;
        }
    }
}

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
 * Parameters for resuming the most relevant local session.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionsOpenResumeLast extends SessionsOpenParams {

    @JsonProperty("kind")
    private final String kind = "resumeLast";

    @Override
    public String getKind() { return kind; }

    /** Working-directory context used to choose the most relevant session. */
    @JsonProperty("context")
    private SessionContext context;

    /** Session resume options. */
    @JsonProperty("options")
    private SessionOpenOptions options;

    /** Suppress workspace.yaml metadata writeback when resuming from an incidental cwd. */
    @JsonProperty("suppressResumeWorkspaceMetadataWriteback")
    private Boolean suppressResumeWorkspaceMetadataWriteback;

    public SessionContext getContext() { return context; }
    public void setContext(SessionContext context) { this.context = context; }

    public SessionOpenOptions getOptions() { return options; }
    public void setOptions(SessionOpenOptions options) { this.options = options; }

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
        private SessionContext context;
        private SessionOpenOptions options;
        private Boolean suppressResumeWorkspaceMetadataWriteback;

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
        public SessionsOpenResumeLast build() {
            SessionsOpenResumeLast instance = new SessionsOpenResumeLast();
            instance.setContext(context);
            instance.setOptions(options);
            instance.setSuppressResumeWorkspaceMetadataWriteback(suppressResumeWorkspaceMetadataWriteback);
            return instance;
        }
    }
}

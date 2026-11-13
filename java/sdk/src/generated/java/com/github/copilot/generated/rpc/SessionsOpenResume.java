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
}

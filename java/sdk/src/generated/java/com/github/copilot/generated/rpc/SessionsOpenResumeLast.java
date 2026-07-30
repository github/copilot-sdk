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
}

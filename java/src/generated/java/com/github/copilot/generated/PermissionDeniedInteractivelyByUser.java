/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Schema for the `PermissionDeniedInteractivelyByUser` type.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class PermissionDeniedInteractivelyByUser extends PermissionResult {

    @JsonProperty("kind")
    private final String kind = "denied-interactively-by-user";

    @Override
    public String getKind() { return kind; }

    /** Optional feedback from the user explaining the denial */
    @JsonProperty("feedback")
    private String feedback;

    /** Whether to force-reject the current agent turn */
    @JsonProperty("forceReject")
    private Boolean forceReject;

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public Boolean getForceReject() { return forceReject; }
    public void setForceReject(Boolean forceReject) { this.forceReject = forceReject; }
}

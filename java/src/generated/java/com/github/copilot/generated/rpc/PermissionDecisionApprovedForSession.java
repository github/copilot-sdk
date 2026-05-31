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
 * Schema for the `PermissionDecisionApprovedForSession` type.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class PermissionDecisionApprovedForSession extends PermissionDecision {

    @JsonProperty("kind")
    private final String kind = "approved-for-session";

    @Override
    public String getKind() { return kind; }

    /** The approval to add as a session-scoped rule */
    @JsonProperty("approval")
    private UserToolSessionApproval approval;

    public UserToolSessionApproval getApproval() { return approval; }
    public void setApproval(UserToolSessionApproval approval) { this.approval = approval; }
}

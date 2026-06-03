/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.annotation.processing.Generated;

/**
 * The client's response to the pending permission prompt
 *
 * @since 1.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = PermissionDecisionApproveOnce.class, name = "approve-once"),
    @JsonSubTypes.Type(value = PermissionDecisionApproveForSession.class, name = "approve-for-session"),
    @JsonSubTypes.Type(value = PermissionDecisionApproveForLocation.class, name = "approve-for-location"),
    @JsonSubTypes.Type(value = PermissionDecisionApprovePermanently.class, name = "approve-permanently"),
    @JsonSubTypes.Type(value = PermissionDecisionReject.class, name = "reject"),
    @JsonSubTypes.Type(value = PermissionDecisionUserNotAvailable.class, name = "user-not-available"),
    @JsonSubTypes.Type(value = PermissionDecisionApproved.class, name = "approved"),
    @JsonSubTypes.Type(value = PermissionDecisionApprovedForSession.class, name = "approved-for-session"),
    @JsonSubTypes.Type(value = PermissionDecisionApprovedForLocation.class, name = "approved-for-location"),
    @JsonSubTypes.Type(value = PermissionDecisionCancelled.class, name = "cancelled"),
    @JsonSubTypes.Type(value = PermissionDecisionDeniedByRules.class, name = "denied-by-rules"),
    @JsonSubTypes.Type(value = PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUser.class, name = "denied-no-approval-rule-and-could-not-request-from-user"),
    @JsonSubTypes.Type(value = PermissionDecisionDeniedInteractivelyByUser.class, name = "denied-interactively-by-user"),
    @JsonSubTypes.Type(value = PermissionDecisionDeniedByContentExclusionPolicy.class, name = "denied-by-content-exclusion-policy"),
    @JsonSubTypes.Type(value = PermissionDecisionDeniedByPermissionRequestHook.class, name = "denied-by-permission-request-hook")
})
@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public abstract class PermissionDecision {

    /**
     * Returns the discriminator value for this variant.
     *
     * @return the kind discriminator
     */
    public abstract String getKind();
}

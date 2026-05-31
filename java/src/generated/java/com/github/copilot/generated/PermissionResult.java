/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.annotation.processing.Generated;

/**
 * The result of the permission request
 *
 * @since 1.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = PermissionApproved.class, name = "approved"),
    @JsonSubTypes.Type(value = PermissionApprovedForSession.class, name = "approved-for-session"),
    @JsonSubTypes.Type(value = PermissionApprovedForLocation.class, name = "approved-for-location"),
    @JsonSubTypes.Type(value = PermissionCancelled.class, name = "cancelled"),
    @JsonSubTypes.Type(value = PermissionDeniedByRules.class, name = "denied-by-rules"),
    @JsonSubTypes.Type(value = PermissionDeniedNoApprovalRuleAndCouldNotRequestFromUser.class, name = "denied-no-approval-rule-and-could-not-request-from-user"),
    @JsonSubTypes.Type(value = PermissionDeniedInteractivelyByUser.class, name = "denied-interactively-by-user"),
    @JsonSubTypes.Type(value = PermissionDeniedByContentExclusionPolicy.class, name = "denied-by-content-exclusion-policy"),
    @JsonSubTypes.Type(value = PermissionDeniedByPermissionRequestHook.class, name = "denied-by-permission-request-hook")
})
@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public abstract class PermissionResult {

    /**
     * Returns the discriminator value for this variant.
     *
     * @return the kind discriminator
     */
    public abstract String getKind();
}

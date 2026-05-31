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
 * Schema for the `PermissionDecisionApproveForLocation` type.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class PermissionDecisionApproveForLocation extends PermissionDecision {

    @JsonProperty("kind")
    private final String kind = "approve-for-location";

    @Override
    public String getKind() { return kind; }

    /** Approval to persist for this location */
    @JsonProperty("approval")
    private PermissionDecisionApproveForLocationApproval approval;

    /** Location key (git root or cwd) to persist the approval to */
    @JsonProperty("locationKey")
    private String locationKey;

    public PermissionDecisionApproveForLocationApproval getApproval() { return approval; }
    public void setApproval(PermissionDecisionApproveForLocationApproval approval) { this.approval = approval; }

    public String getLocationKey() { return locationKey; }
    public void setLocationKey(String locationKey) { this.locationKey = locationKey; }
}

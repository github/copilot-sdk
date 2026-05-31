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
 * Schema for the `PermissionDecisionApproveForLocationApprovalExtensionManagement` type.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class PermissionDecisionApproveForLocationApprovalExtensionManagement extends PermissionDecisionApproveForLocationApproval {

    @JsonProperty("kind")
    private final String kind = "extension-management";

    @Override
    public String getKind() { return kind; }

    /** Optional operation identifier; when omitted, the approval covers all extension management operations. */
    @JsonProperty("operation")
    private String operation;

    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }
}

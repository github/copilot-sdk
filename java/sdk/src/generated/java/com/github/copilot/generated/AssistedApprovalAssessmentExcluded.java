/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * A deterministic runtime or policy gate prevented automatic approval without consulting the judge.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AssistedApprovalAssessmentExcluded extends AssistedApprovalAssessment {

    @JsonProperty("status")
    private final String status = "excluded";

    @Override
    public String getStatus() { return status; }

    /** Machine-readable exclusion reason. Current runtimes emit `managed-approval-required`, `sandbox-bypass`, `extension-permission-access`, `action-too-long`, `authorization-history-incomplete`, `path-not-authorized`, or `content-excluded`; consumers must tolerate future strings. */
    @JsonProperty("reason")
    private String reason;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}

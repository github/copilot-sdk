/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.generated.AssistedApprovalAssessment;
import javax.annotation.processing.Generated;

/**
 * Pending permission prompt reconstructed from event history, with request ID and user-facing prompt details.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PendingPermissionRequest(
    /** Unique identifier for the pending permission request */
    @JsonProperty("requestId") String requestId,
    /** The user-facing permission prompt details (commands, write, read, mcp, url, memory, custom-tool, path, hook) */
    @JsonProperty("request") Object request,
    /** Canonical runtime-owned Assisted Permissions assessment captured on the originating permission request. Absent for historical events. */
    @JsonProperty("assistedApprovalAssessment") AssistedApprovalAssessment assistedApprovalAssessment,
    /** Permission-recovery episode that authorized this request to surface for interactive attention */
    @JsonProperty("recoveryEpisodeId") String recoveryEpisodeId
) {
}

/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Pending permission request ID and the decision to apply (approve/reject and scope).
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionPermissionsHandlePendingPermissionRequestParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Request ID of the pending permission request */
    @JsonProperty("requestId") String requestId,
    /** Decision to apply to a pending permission request. */
    @JsonProperty("result") Object result
) {
}

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
 * Optional informational context describing how and where the permission decision was made. This does not affect permission behavior.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PermissionDecisionContext(
    /** Disposition of the permission request as observed by the responding client. */
    @JsonProperty("outcome") PermissionDecisionOutcome outcome,
    /** Controlled reason or actor responsible for the response. */
    @JsonProperty("source") PermissionDecisionSource source,
    /** Client surface that submitted the response. */
    @JsonProperty("surface") PermissionDecisionSurface surface,
    /** Whether the responding client could ask a user interactively, was running headlessly, or had no response path. Omit when the client cannot determine this authoritatively. */
    @JsonProperty("responseCapability") PermissionResponseCapability responseCapability
) {
}

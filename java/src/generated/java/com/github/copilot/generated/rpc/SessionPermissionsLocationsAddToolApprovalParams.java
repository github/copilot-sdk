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
 * Location-scoped tool approval to persist.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionPermissionsLocationsAddToolApprovalParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Location key (git root or cwd) to persist the approval to */
    @JsonProperty("locationKey") String locationKey,
    /** Tool approval to persist and apply */
    @JsonProperty("approval") Object approval
) {
}

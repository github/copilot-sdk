/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Host-supplied exact model selection IDs to allow for this running session. CAPI IDs are intersected with repository `.github/allowed_models.txt` policy; provider-qualified IDs remain exempt from repository-only policy but are restricted by this host list. Omit or pass null to clear the host restriction; an explicit empty or disjoint list is rejected. Validation and pre-selection fallback failures preserve the previous restriction. Failures after a fallback selection commits retain the new restriction and selected model; callers should inspect current session state after such an error.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionModelSetAllowedModelsParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Exact model IDs to permit, or null to clear the host restriction. */
    @JsonProperty("allowedModels") List<String> allowedModels
) {
}

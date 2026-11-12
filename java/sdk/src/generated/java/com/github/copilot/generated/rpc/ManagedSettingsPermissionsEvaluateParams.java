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
import com.github.copilot.generated.ManagedPermissionsContext;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Managed policy context and ordered operations for pure, sessionless evaluation.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ManagedSettingsPermissionsEvaluateParams(
    /** Trusted, reusable managed permission policy snapshot; no policy is discovered during evaluation. */
    @JsonProperty("context") ManagedPermissionsContext context,
    /** Operations to evaluate, preserving input order and duplicates. An empty batch is valid. */
    @JsonProperty("operations") List<ManagedPermissionOperation> operations
) {
}

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
import javax.annotation.processing.Generated;

/**
 * Applies exactly one previously prepared operation on its original connection.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpApplyInstallParams(
    /** Capabilities required by the original prepared operation. */
    @JsonProperty("contract") CatalogClientContract contract,
    /** Runtime-issued ID already returned by prepareInstall, never reused or rebound. */
    @JsonProperty("operationId") String operationId,
    /** Same existing attached or privately borrowed session as preparation. */
    @JsonProperty("policySessionId") String policySessionId
) {
}

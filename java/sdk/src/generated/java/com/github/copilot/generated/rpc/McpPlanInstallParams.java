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
 * A side-effect-free request for an MCP install plan. Computing a plan never writes configuration, stores a secret, or reloads MCP servers.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpPlanInstallParams(
    /** Protocol version and capabilities the caller requires. */
    @JsonProperty("contract") CatalogClientContract contract,
    /** What to plan: either a candidate handle from a previous search, or a card supplied directly. */
    @JsonProperty("source") Object source,
    /** Configuration scope the plan targets. Defaults to user scope when omitted. */
    @JsonProperty("scope") McpPlanScope scope
) {
}

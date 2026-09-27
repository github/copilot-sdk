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
 * Side-effect-free preparation of one original bound, input-free remote MCP choice.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpPrepareInstallParams(
    /** Required bound catalogue and confirmed remote installation capabilities. */
    @JsonProperty("contract") CatalogClientContract contract,
    /** Original single-use bound plan, never a client-authored configuration. */
    @JsonProperty("planHandle") String planHandle,
    /** Exact selected alternative from that plan. */
    @JsonProperty("choiceId") String choiceId,
    /** An existing local session attached to this connection, not permission to attach one. */
    @JsonProperty("policySessionId") String policySessionId,
    /** Must be empty for the initial input-free remote installation capability. */
    @JsonProperty("inputs") List<McpInstallationInput> inputs,
    /** Must be empty; this capability does not allocate configured-input secrets. */
    @JsonProperty("secrets") List<McpInstallationSecret> secrets,
    /** The exact original source, used transiently only after confirmation. */
    @JsonProperty("source") Object source,
    /** The trusted host presents this choice alongside the exact secret placeholders. */
    @JsonProperty("secretStorage") McpInstallationSecretStorage secretStorage
) {
}

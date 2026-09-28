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
 * Side-effect-free planning of one verified Agent Finder Skill candidate.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SkillsPlanInstallParams(
    /** Required authenticated bound catalogue and Skill installation capabilities. */
    @JsonProperty("contract") CatalogClientContract contract,
    /** Fresh single-use AI skill candidate handle returned by a bound catalogue search. */
    @JsonProperty("candidateHandle") String candidateHandle,
    /** Existing local session attached to this connection. */
    @JsonProperty("policySessionId") String policySessionId
) {
}

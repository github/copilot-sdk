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
 * Persisted enablement update for one owned Skill installation.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SkillsInstallationsSetEnabledParams(
    /** Required authenticated bound Skill installation capability. */
    @JsonProperty("contract") CatalogClientContract contract,
    /** Exact receipt identity to update. */
    @JsonProperty("installationId") String installationId,
    /** Persisted enablement value. */
    @JsonProperty("enabled") Boolean enabled,
    /** Existing selected local session to reconcile after persistence. */
    @JsonProperty("policySessionId") String policySessionId
) {
}

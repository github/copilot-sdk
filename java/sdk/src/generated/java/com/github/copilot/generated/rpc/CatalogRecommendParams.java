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
 * Initial-page official-product recommendations for a complete task. The runtime fixes recommendationPolicy to official-product and pageSize to 10; callers cannot override the policy, request pagination or fall back to search.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogRecommendParams(
    /** Required contract, including catalog-recommend, catalog-search-session-bound and catalog-search-credential-required. */
    @JsonProperty("contract") CatalogClientContract contract,
    /** Existing attached local session whose account, host and connection own the candidate references. Never creates or resumes a session. */
    @JsonProperty("policySessionId") String policySessionId,
    /** Full original task, including action, account, output and host requirements. Forwarded without trimming or rewriting and excluded from telemetry. */
    @JsonProperty("query") String query,
    /** Requested product, forwarded verbatim. Agent Finder resolves aliases and applies its versioned official-product approvals before limiting results. */
    @JsonProperty("product") String product
) {
}

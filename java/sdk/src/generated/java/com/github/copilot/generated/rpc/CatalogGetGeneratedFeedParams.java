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
 * Retrieves one generated feed, computed on every request, by strategy and key, such as `telemetry` and `popular`. Requires catalog-feeds and catalog-search-credential-required; add catalog-search-session-bound with policySessionId to retain the selected session's authority for installation, exactly as for catalog.search.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogGetGeneratedFeedParams(
    /** Protocol version and capabilities the caller requires. */
    @JsonProperty("contract") CatalogClientContract contract,
    /** Select an existing attached local session. Requires authenticated, session-bound catalogue support.
The runtime never creates, resumes or reconfigures a session to honour this selector. */
    @JsonProperty("policySessionId") String policySessionId,
    /** Generation strategy, such as `telemetry`. */
    @JsonProperty("strategy") String strategy,
    /** Feed key within the strategy, such as `popular`. */
    @JsonProperty("key") String key,
    /** Accept only resources currently assessed at one of these trust tiers. Entries whose resource is in another tier, or has none, are returned as filtered in their original position. Omit for an unfiltered feed. */
    @JsonProperty("trustTiers") List<CatalogTrustTier> trustTiers
) {
}

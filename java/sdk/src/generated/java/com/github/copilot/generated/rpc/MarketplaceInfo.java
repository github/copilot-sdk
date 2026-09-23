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
 * Registered marketplace summary.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record MarketplaceInfo(
    /** Marketplace name (matches the @marketplace suffix in plugin specs) */
    @JsonProperty("name") String name,
    /** Human-readable description of where the marketplace data is fetched from (e.g. "GitHub: owner/repo"). */
    @JsonProperty("source") String source,
    /** True when this is a default marketplace shipped with the runtime. Defaults are not removable. */
    @JsonProperty("isDefault") Boolean isDefault,
    /** Whether enterprise managed settings provide and control this marketplace entry. */
    @JsonProperty("managed") Boolean managed,
    /** Whether the managed marketplace currently resolved into the runtime marketplace registry. Set to false when the desired managed entry is retained for governance visibility after loading or reconciliation failed. */
    @JsonProperty("available") Boolean available
) {
}

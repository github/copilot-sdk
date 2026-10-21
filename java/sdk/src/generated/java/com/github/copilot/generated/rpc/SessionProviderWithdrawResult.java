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
 * What the withdrawal actually removed from the registry.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionProviderWithdrawResult(
    /** Selection ids that were registered and are now withdrawn. Excludes requested ids that were not present. */
    @JsonProperty("withdrawn") List<String> withdrawn,
    /** Providers removed because one of the withdrawn models was the last entry referencing them. A provider that merely has no models is not removed. */
    @JsonProperty("providersRemoved") List<String> providersRemoved,
    /** True when withdrawal removed the selected host-managed model, leaving the session with no explicit selection, so ordinary model resolution picks the session default. Withdrawal never promotes a surviving model in its place: the choice of which model to use stays with the user. */
    @JsonProperty("modelDeselected") Boolean modelDeselected
) {
}

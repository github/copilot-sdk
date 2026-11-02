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

@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelSwitchConfirmation(
    /** Display name of the model that requires compaction confirmation. */
    @JsonProperty("targetModelDisplayName") String targetModelDisplayName,
    /** Current conversation token count before switching models. */
    @JsonProperty("currentTokens") Double currentTokens,
    /** Target model token limit used by the compaction preflight. */
    @JsonProperty("targetLimit") Double targetLimit
) {
}

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
public record ModelPickerPersistenceRequest(
    /** Filesystem and environment context used to resolve settings persistence. */
    @JsonProperty("settingsContext") ModelPickerSettingsContext settingsContext,
    /** Whether reasoning effort was explicitly selected and should be persisted. */
    @JsonProperty("reasoningEffortExplicit") Boolean reasoningEffortExplicit,
    /** Whether context tier was explicitly selected and should be persisted. */
    @JsonProperty("contextTierExplicit") Boolean contextTierExplicit
) {
}

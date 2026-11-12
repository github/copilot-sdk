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
public record SlashCommandModelPickerDialog(
    /** Discriminator for a model-picker dialog. */
    @JsonProperty("kind") String kind,
    /** Model that should be enabled before it can be selected. */
    @JsonProperty("modelToEnable") String modelToEnable,
    /** Settings scope the picker should modify. */
    @JsonProperty("scope") String scope,
    /** Model-selection target represented by the picker. */
    @JsonProperty("target") String target
) {
}

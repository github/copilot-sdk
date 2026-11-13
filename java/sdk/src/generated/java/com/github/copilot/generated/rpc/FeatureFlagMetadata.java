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
 * The declared shape of one feature flag, independent of any caller population.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record FeatureFlagMetadata(
    /** The flag's name, matching the key it is listed under. */
    @JsonProperty("name") String name,
    /** The population the flag is declared for before any environment or user-settings override is applied. */
    @JsonProperty("availability") FeatureFlagAvailability availability,
    /** Whether the flag participates in the CAPI sanity suite. */
    @JsonProperty("capiSanity") Boolean capiSanity,
    /** Text shown to a user choosing whether to turn the flag on in experimental mode. Absent when the flag is not user-facing. */
    @JsonProperty("experimentalDescription") String experimentalDescription,
    /** The online-experiment (ExP) flag this maps to, when the flag is assigned by an experiment rather than declared statically. */
    @JsonProperty("expFlag") String expFlag,
    /** Environment variable that overrides this flag's experiment assignment locally in the experiment service, not in `featureFlags.resolve`. Present exactly when `expFlag` is, and derived from it, so a caller reads a local override without reproducing the naming rule. */
    @JsonProperty("expFlagEnvVar") String expFlagEnvVar
) {
}

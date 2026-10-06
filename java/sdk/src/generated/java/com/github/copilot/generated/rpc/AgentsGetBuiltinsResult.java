/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * The agents this runtime ships, named so a consumer can tell them apart from authored ones.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record AgentsGetBuiltinsResult(
    /** Every agent name this runtime ships. */
    @JsonProperty("names") List<String> names,
    /** The subset of `names` a user is allowed to turn off. A shipped agent outside this list is always active and a client should not offer a toggle for it. */
    @JsonProperty("disableableNames") List<String> disableableNames,
    /** The subset of `names` defined by a shipped YAML definition. The remainder are special-cased in code and have no definition to load. */
    @JsonProperty("yamlBasedNames") List<String> yamlBasedNames
) {
}

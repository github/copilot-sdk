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
 * Eligible Azure ML model version and its provider metadata.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AzureMlModel(
    /** Registry that owns this model version. */
    @JsonProperty("registry") AzureMlRegistry registry,
    /** Stable model-family name. */
    @JsonProperty("name") String name,
    /** Exact published model version. */
    @JsonProperty("version") String version,
    /** Validated Windows ML requirements. */
    @JsonProperty("profile") AzureMlProfile profile,
    /** Provider metadata retained for registration. */
    @JsonProperty("metadata") Object metadata
) {
}

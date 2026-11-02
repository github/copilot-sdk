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
 * The authoring JSON schema for managed settings recognized by this runtime.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ManagedSettingsSchemaResult(
    /** JSON schema (draft 2020-12) with descriptive shared `x-composition` annotations, not a complete runtime composition contract. Model, effortLevel, and contextTier remain coupled; use `managedSettings.compose` for the runtime's effective result. */
    @JsonProperty("schema") Object schema,
    /** Version of the runtime that owns this schema. */
    @JsonProperty("runtimeVersion") String runtimeVersion
) {
}

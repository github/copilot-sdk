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
 * A candidate managed-settings document to validate without applying it.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ManagedSettingsValidateParams(
    /** The document to validate: a JSON object, or a string containing the document's JSON text. Preview documents are limited to 1 MiB and 64 levels of nesting, a stricter resource limit than delivered-policy parsing; violations are returned as diagnostics. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("content") Object content,
    /** Channel the document is meant for (`device`, `server`, or `policyHelper`). Some keys are only honored in some channels; for example, a `policyHelper` registration is ignored in policy-helper output. When omitted, no channel-specific checks run. */
    @JsonProperty("layer") String layer
) {
}

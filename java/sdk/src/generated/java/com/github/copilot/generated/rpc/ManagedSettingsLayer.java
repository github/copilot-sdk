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
 * One managed-settings channel and the document it delivered.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ManagedSettingsLayer(
    /** Channel identifier: `device` (MDM, plist, registry, or managed file), `server` (account or organization policy), or `policyHelper` (session-local helper output, supported by compose). Treat unknown output values as additional channels; more may be added. */
    @JsonProperty("source") String source,
    /** Validated managed-settings document this channel delivered. Absent when the channel delivered none. */
    @JsonProperty("settings") Object settings
) {
}

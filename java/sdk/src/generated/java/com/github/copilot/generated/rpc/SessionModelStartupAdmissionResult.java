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
 * A cached startup model snapshot and an optional one-time enablement prompt. This check never resolves credentials or fetches a catalog.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionModelStartupAdmissionResult(
    /** The selected model, when the session has one. */
    @JsonProperty("selectedModel") String selectedModel,
    /** Whether the session already has a resolved Copilot catalog. */
    @JsonProperty("catalogReady") Boolean catalogReady,
    /** Whether the cached selected model currently requires enablement. */
    @JsonProperty("requiresEnablement") Boolean requiresEnablement,
    /** Whether this call consumed the session's startup enablement claim. */
    @JsonProperty("claimedEnablement") Boolean claimedEnablement,
    /** The selected model to enable, only for the first successful prompt claim. */
    @JsonProperty("modelToEnable") String modelToEnable
) {
}

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
public record AutopilotOptions(
    /** Enable runtime-owned automatic continuation while in Autopilot mode. Defaults to true for SDK sessions; does not change the session mode. */
    @JsonProperty("continuationEnabled") Boolean continuationEnabled,
    /** Maximum automatic continuations outside an active objective. Zero allows none; defaults to five. Omitted values preserve the current limit on update or warm resume. */
    @JsonProperty("maxContinues") Long maxContinues
) {
}

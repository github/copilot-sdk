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
 * GitHub URL and remote export and steering state, with an optional warning when cloud session storage policy refuses export.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionRemoteEnableResult(
    /** GitHub frontend URL for this session */
    @JsonProperty("url") String url,
    /** Whether remote steering is enabled */
    @JsonProperty("remoteSteerable") Boolean remoteSteerable,
    /** Whether this request accepted or retained remote export. False when export is disabled or refused; initialization may still be pending when true. */
    @JsonProperty("started") Boolean started,
    /** Warning explaining why cloud session storage policy prevented export. Absent when no policy warning applies. */
    @JsonProperty("warning") String warning
) {
}

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
 * Targets only the original prepared/applying owned login on this exact session requester.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionMcpOauthCancelLoginParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** The same authoritative installation identity supplied during preparation. */
    @JsonProperty("expectedInstallationId") String expectedInstallationId,
    /** Runtime-issued login handle known before the effectful login request begins. */
    @JsonProperty("loginId") String loginId
) {
}

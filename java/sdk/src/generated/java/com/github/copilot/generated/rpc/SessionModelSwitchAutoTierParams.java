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
 * An Auto preference request for the session. This updates Auto configuration only; it does not change the selected model to `auto`.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionModelSwitchAutoTierParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Auto preference to activate when a future user turn using the `auto` model safely mints a replacement model and token pair. Pass null to return to provider-default Auto routing. */
    @JsonProperty("autoTier") AutoTier autoTier,
    /** Origin to record on the effective `session.model_change` event. Defaults to `sdk` when omitted. */
    @JsonProperty("source") ModelChangeSource source
) {
}

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
 * Allow-all mode to apply for the session.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionPermissionsSetAllowAllParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Allow-all mode to apply. `on` enables full allow-all; `auto` enables advisory LLM auto-approval; `off` disables both. */
    @JsonProperty("mode") PermissionsAllowAllMode mode,
    /** Legacy full allow-all toggle. Prefer `mode`; when `mode` is omitted, `enabled: true` is treated as `mode: "on"` and any other value is treated as `mode: "off"`. */
    @JsonProperty("enabled") Boolean enabled,
    /** Optional model id for the `auto` mode auto-approval LLM judging. Only meaningful when `mode` is `auto`; ignored otherwise. When omitted, the session's active model is used. */
    @JsonProperty("model") String model,
    /** Optional source for allow-all telemetry. Defaults to `rpc` when omitted for SDK callers. */
    @JsonProperty("source") PermissionsSetAllowAllSource source
) {
}

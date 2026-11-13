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
 * Opt-in policy for local session management.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionManagementConfigureResult(
    /** Master switch. Off by default. */
    @JsonProperty("enabled") Boolean enabled,
    /** Discovery and direct access boundary. Defaults to all eligible local peers. */
    @JsonProperty("scope") SessionManagementScope scope,
    /** Allow listing opted-in sessions in this configuration directory. */
    @JsonProperty("list") Boolean list,
    /** Allow requesting a new session from the owning host. */
    @JsonProperty("create") Boolean create,
    /** Allow sending a peer message to another opted-in session. */
    @JsonProperty("message") Boolean message,
    /** Allow closing sessions created by this session or its descendants. On by default. */
    @JsonProperty("close") Boolean close
) {
}

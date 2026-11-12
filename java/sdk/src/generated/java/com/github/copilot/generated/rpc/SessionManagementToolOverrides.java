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
 * Per-operation overrides for an enabled session management policy.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionManagementToolOverrides(
    /** Allow discovering eligible peer sessions. */
    @JsonProperty("list") Boolean list,
    /** Allow requesting an independent session from the owning host. */
    @JsonProperty("create") Boolean create,
    /** Allow delivering peer text to another eligible session. */
    @JsonProperty("message") Boolean message,
    /** Allow closing this session's live created descendants. */
    @JsonProperty("close") Boolean close
) {
}

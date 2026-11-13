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
 * Request parameters for the {@code session.commands.list} RPC method.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionCommandsListParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Include runtime built-in commands */
    @JsonProperty("includeBuiltins") Boolean includeBuiltins,
    /** Include enabled user-invocable skills and commands */
    @JsonProperty("includeSkills") Boolean includeSkills,
    /** Include commands registered by protocol clients, including SDK clients and extensions */
    @JsonProperty("includeClientCommands") Boolean includeClientCommands
) {
}

/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Public MCP skill provenance. The server name and resource URI address session.mcp.resources.read without invoking the skill; they do not grant permission or identify a durable server registration.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpSkillOrigin(
    /** Name of the MCP server hosting the skill, matching serverName in session.mcp.resources.read. */
    @JsonProperty("serverName") String serverName,
    /** Exact URI of the skill's SKILL.md resource, matching uri in session.mcp.resources.read. */
    @JsonProperty("uri") String uri
) {
}

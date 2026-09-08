/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Serializable definition of a caller-implemented tool whose execution is handled over the SDK connection.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProtocolExternalToolDefinition(
    /** Unique model-visible tool name. */
    @JsonProperty("name") String name,
    /** Model-visible explanation of what the tool does. */
    @JsonProperty("description") String description,
    /** Optional human-readable display title. */
    @JsonProperty("title") String title,
    /** JSON Schema describing the tool's input arguments. */
    @JsonProperty("parameters") Map<String, Object> parameters,
    /** Whether this definition replaces a built-in tool with the same name. */
    @JsonProperty("overridesBuiltInTool") Boolean overridesBuiltInTool,
    /** Whether execution bypasses the normal tool permission prompt. */
    @JsonProperty("skipPermission") Boolean skipPermission,
    /** Tool-loading deferral policy. */
    @JsonProperty("defer") ProtocolExternalToolDefer defer,
    /** Whether the tool executes commands in a terminal. */
    @JsonProperty("isTerminal") Boolean isTerminal,
    /** Optional caller-defined metadata associated with the tool. */
    @JsonProperty("metadata") Map<String, Object> metadata
) {
}

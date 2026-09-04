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

/**
 * Rust-owned metadata and input schema for a built-in tool.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record BuiltinToolDescriptor(
    /** Stable name used to invoke the built-in tool. */
    @JsonProperty("name") String name,
    /** Optional human-readable title for the tool. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("title") String title,
    /** Model-facing description of the tool's behavior. */
    @JsonProperty("description") String description,
    /** JSON Schema for the tool input, or null when the tool uses a custom format. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("inputSchema") BuiltinToolInputSchema inputSchema,
    /** Optional supplemental usage instructions for the tool. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("instructions") String instructions,
    /** Optional tool category discriminator. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("type") String type,
    /** Optional custom input format used instead of a JSON Schema. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("format") BuiltinToolFormat format,
    /** Policy describing which tool metadata may be recorded without obfuscation. */
    @JsonProperty("safeForTelemetry") Object safeForTelemetry,
    /** Whether the tool executes commands in a terminal. */
    @JsonProperty("isTerminal") Boolean isTerminal,
    /** Whether the tool provides a specialized intention summary. */
    @JsonProperty("hasSummariseIntention") Boolean hasSummariseIntention
) {
}

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
 * Custom grammar input format accepted by a built-in tool.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record BuiltinToolFormat(
    /** Custom input-format discriminator. */
    @JsonProperty("type") BuiltinToolFormatType type,
    /** Grammar syntax used by the format definition. */
    @JsonProperty("syntax") String syntax,
    /** Grammar definition accepted by the tool. */
    @JsonProperty("definition") String definition
) {
}

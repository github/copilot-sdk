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
 * Optional unstructured input hint
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SlashCommandInput(
    /** Hint to display when command input has not been provided */
    @JsonProperty("hint") String hint,
    /** When true, the command requires non-empty input; clients should render the input hint as required */
    @JsonProperty("required") Boolean required,
    /** Optional completion hint for the input (e.g. 'directory' for filesystem path completion) */
    @JsonProperty("completion") SlashCommandInputCompletion completion,
    /** When true, clients should pass the full text after the command name as a single argument rather than splitting on whitespace */
    @JsonProperty("preserveMultilineInput") Boolean preserveMultilineInput
) {
}

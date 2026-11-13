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
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Provider-reported model artifact metadata.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelArtifactDetails(
    /** Artifact format, such as `gguf`. */
    @JsonProperty("format") String format,
    /** Primary model family. */
    @JsonProperty("family") String family,
    /** Provider-reported model families. */
    @JsonProperty("families") List<String> families,
    /** Provider-reported parameter count label. */
    @JsonProperty("parameterSize") String parameterSize,
    /** Provider-reported quantization label. */
    @JsonProperty("quantization") String quantization,
    /** Provider-reported model architecture. */
    @JsonProperty("architecture") String architecture,
    /** Provider-reported tokenizer. */
    @JsonProperty("tokenizer") String tokenizer
) {
}

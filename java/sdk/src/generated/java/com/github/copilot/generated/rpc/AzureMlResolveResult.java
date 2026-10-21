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
 * Resolved artifact facts without signed download URLs.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AzureMlResolveResult(
    /** Exact bytes across all files in this version. */
    @JsonProperty("totalBytes") Long totalBytes,
    /** GGUF path relative to the version directory. */
    @JsonProperty("primaryPath") String primaryPath,
    /** Other paths relative to the version directory. */
    @JsonProperty("extraFiles") List<String> extraFiles,
    /** Validated local runtime settings. */
    @JsonProperty("serveOptions") Object serveOptions,
    /** Maximum supported local concurrency. */
    @JsonProperty("maxConcurrent") Long maxConcurrent
) {
}

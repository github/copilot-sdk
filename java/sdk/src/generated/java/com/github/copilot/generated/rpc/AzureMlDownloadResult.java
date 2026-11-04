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
 * Completed and verified Azure ML model files.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AzureMlDownloadResult(
    /** Installed GGUF path. */
    @JsonProperty("primaryPath") String primaryPath,
    /** Published manifest path. */
    @JsonProperty("manifestPath") String manifestPath,
    /** Exact bytes across the model version. */
    @JsonProperty("totalBytes") Long totalBytes,
    /** Whether all prior verified files were reused. */
    @JsonProperty("reused") Boolean reused,
    /** Validated local runtime settings. */
    @JsonProperty("serveOptions") Object serveOptions,
    /** Maximum supported local concurrency. */
    @JsonProperty("maxConcurrent") Long maxConcurrent,
    /** Installed companion file paths. */
    @JsonProperty("extraFiles") List<String> extraFiles,
    /** Transient transfer failures retried during this run. */
    @JsonProperty("totalFailures") Long totalFailures,
    /** Bytes received over the network during this run, including discarded bytes. */
    @JsonProperty("receivedBytes") Long receivedBytes,
    /** Whether a partial from a prior run continued after the server accepted its range. */
    @JsonProperty("resumed") Boolean resumed
) {
}

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
 * One language server in the runtime's configuration inventory.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record LspServerSummary(
    /** Configured server identifier. */
    @JsonProperty("id") String id,
    /** File suffixes handled by the server. */
    @JsonProperty("fileExtensions") Map<String, String> fileExtensions,
    /** Project root relative to the repository root. */
    @JsonProperty("rootUri") String rootUri,
    /** Request deadline in milliseconds. */
    @JsonProperty("requestTimeoutMs") Double requestTimeoutMs,
    /** Protocol initialization deadline in milliseconds. */
    @JsonProperty("initializationTimeoutMs") Double initializationTimeoutMs,
    /** Process startup deadline in milliseconds. */
    @JsonProperty("spawnTimeoutMs") Double spawnTimeoutMs,
    /** Warmup deadline in milliseconds. */
    @JsonProperty("warmupTimeoutMs") Double warmupTimeoutMs,
    /** Plugin that supplies the server, when applicable. */
    @JsonProperty("sourcePlugin") String sourcePlugin,
    /** Whether the effective configuration disables this entry. */
    @JsonProperty("disabled") Boolean disabled,
    /** Platform-selected launch script for a user entry. */
    @JsonProperty("launchDescription") String launchDescription
) {
}

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
 * Built-in session diagnostics to include in the bundle. Omitted fields default to true.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record DebugCollectLogsInclude(
    /** Include the session event log (`events.jsonl`). Defaults to true. */
    @JsonProperty("events") Boolean events,
    /** Include process logs for the session. Defaults to true. */
    @JsonProperty("processLogs") Boolean processLogs,
    /** Include interactive shell logs written under the session's `shell-logs` directory. Defaults to true. */
    @JsonProperty("shellLogs") Boolean shellLogs,
    /** Server-local path to the session's events.jsonl file. Internal callers normally omit this and let the runtime derive it from the session. */
    @JsonProperty("eventsPath") String eventsPath,
    /** Server-local path to the current process log. When set, it is included as `process.log` and its directory is searched for prior logs from the same session. */
    @JsonProperty("currentProcessLogPath") String currentProcessLogPath,
    /** Server-local process log directory to search when `currentProcessLogPath` is unavailable, useful for collecting logs for inactive sessions. */
    @JsonProperty("processLogDirectory") String processLogDirectory,
    /** Maximum number of previous process logs to include. Defaults to 5. */
    @JsonProperty("previousProcessLogLimit") Long previousProcessLogLimit
) {
}

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
import javax.annotation.processing.Generated;

/**
 * A complete JSON snapshot to write under the runtime's per-path file lock.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record PersistenceWriteRawJsonLockedParams(
    /** Absolute path of the file to replace. Parent directories are created when missing. */
    @JsonProperty("path") String path,
    /** The serialized JSON document to write. It replaces the file's contents entirely; nothing is merged. */
    @JsonProperty("dataJson") String dataJson,
    /** Comment lines to emit above the document, for a format that tolerates them. Omit for strict JSON. */
    @JsonProperty("fileHeader") String fileHeader
) {
}

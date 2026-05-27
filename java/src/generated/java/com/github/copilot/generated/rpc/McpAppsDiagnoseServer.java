/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * What the server returned for this session
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpAppsDiagnoseServer(
    /** Whether the named server is currently connected */
    @JsonProperty("connected") Boolean connected,
    /** Total tools returned by the server's tools/list */
    @JsonProperty("toolCount") Double toolCount,
    /** Tools whose `_meta.ui` is populated (resourceUri and/or visibility set) */
    @JsonProperty("toolsWithUiMeta") Double toolsWithUiMeta,
    /** Up to 5 tool names with `_meta.ui` for quick inspection */
    @JsonProperty("sampleToolNames") List<String> sampleToolNames
) {
}

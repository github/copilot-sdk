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
 * A page of factory runs in durable creation order.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionFactoryListRunsResult(
    /** Factory run summaries in durable creation order. */
    @JsonProperty("runs") List<FactoryRunSummary> runs,
    /** Oldest terminal-run cursor in this page, or null when the terminal window is empty. */
    @JsonProperty("oldestSeq") Long oldestSeq,
    /** Newest terminal-run cursor in this page, or null when the terminal window is empty. */
    @JsonProperty("newestSeq") Long newestSeq,
    /** Whether terminal runs newer than this page exist. */
    @JsonProperty("hasMoreNewer") Boolean hasMoreNewer,
    /** Number of terminal runs older than this page. */
    @JsonProperty("omittedOlder") Long omittedOlder
) {
}

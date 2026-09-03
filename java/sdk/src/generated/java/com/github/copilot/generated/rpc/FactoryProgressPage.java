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
 * A bidirectional page of factory progress.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record FactoryProgressPage(
    /** Progress records in sequence order. */
    @JsonProperty("records") List<FactoryProgressLine> records,
    /** Oldest sequence number in this page, or null when empty. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("oldestSeq") Long oldestSeq,
    /** Newest sequence number in this page, or null when empty. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("newestSeq") Long newestSeq,
    /** Whether progress records older than this page exist. */
    @JsonProperty("hasMoreOlder") Boolean hasMoreOlder,
    /** Whether progress records newer than this page exist. */
    @JsonProperty("hasMoreNewer") Boolean hasMoreNewer,
    /** Run revision reflected by this page. */
    @JsonProperty("revision") Long revision
) {
}

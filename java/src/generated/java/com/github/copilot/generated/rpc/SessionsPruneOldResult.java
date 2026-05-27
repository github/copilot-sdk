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
 * Outcome of the prune operation: deleted IDs, dry-run candidates, skipped IDs, total bytes freed, and the dry-run flag.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionsPruneOldResult(
    /** Session IDs that were deleted (always empty in dry-run mode) */
    @JsonProperty("deleted") List<String> deleted,
    /** Session IDs that would be deleted in dry-run mode (always empty otherwise) */
    @JsonProperty("candidates") List<String> candidates,
    /** Session IDs that were skipped (e.g., named sessions) */
    @JsonProperty("skipped") List<String> skipped,
    /** Total bytes freed (actual when not dry-run, projected when dry-run) */
    @JsonProperty("freedBytes") Long freedBytes,
    /** True when no deletions were actually performed */
    @JsonProperty("dryRun") Boolean dryRun
) {
}

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
 * Age threshold and optional flags controlling which old sessions are pruned (or simulated when dryRun is true).
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionsPruneOldParams(
    /** Delete sessions whose modifiedTime is at least this many days old */
    @JsonProperty("olderThanDays") Long olderThanDays,
    /** When true, only report what would be deleted without performing any deletion */
    @JsonProperty("dryRun") Boolean dryRun,
    /** When true, named sessions (set via /rename) are also eligible for pruning */
    @JsonProperty("includeNamed") Boolean includeNamed,
    /** Session IDs that should never be considered for pruning */
    @JsonProperty("excludeSessionIds") List<String> excludeSessionIds
) {
}

/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Session event "prompt_cache_break". A detected loss of a previously cached prompt prefix
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class PromptCacheBreakEvent extends SessionEvent {

    @Override
    public String getType() { return "prompt_cache_break"; }

    @JsonProperty("data")
    private PromptCacheBreakEventData data;

    public PromptCacheBreakEventData getData() { return data; }
    public void setData(PromptCacheBreakEventData data) { this.data = data; }

    /** Data payload for {@link PromptCacheBreakEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PromptCacheBreakEventData(
        /** The highest-precedence reason for the cache break */
        @JsonProperty("primaryReason") String primaryReason,
        /** All reasons that contributed to the cache break, ordered by precedence */
        @JsonProperty("contributingReasons") List<String> contributingReasons,
        /** Request state that established the prior cache frontier */
        @JsonProperty("beforeRequest") Object beforeRequest,
        /** Request state whose cached prefix fell short */
        @JsonProperty("afterRequest") Object afterRequest,
        /** Number of cached prefix tokens that survived */
        @JsonProperty("survivedTokens") Long survivedTokens,
        /** Prior cached prompt frontier in tokens */
        @JsonProperty("frontierTokens") Long frontierTokens,
        /** Cached prefix tokens lost since the prior call */
        @JsonProperty("shortfallTokens") Long shortfallTokens,
        /** Fraction of the prior cache frontier that survived */
        @JsonProperty("retentionRatio") Double retentionRatio,
        /** Model that held the prior cache frontier, when the call changed models */
        @JsonProperty("modelFrom") String modelFrom,
        /** Model this call targeted, when the call changed models */
        @JsonProperty("modelTo") String modelTo,
        /** Telemetry-safe names of tools added since the prior call */
        @JsonProperty("toolsAdded") List<String> toolsAdded,
        /** Telemetry-safe names of tools removed since the prior call */
        @JsonProperty("toolsRemoved") List<String> toolsRemoved,
        /** Telemetry-safe names of tools whose definition changed since the prior call */
        @JsonProperty("toolsRedefined") List<String> toolsRedefined,
        /** Raw names of tools added since the prior call, restricted because a tool name can be user-authored */
        @JsonProperty("toolsAddedRaw") List<String> toolsAddedRaw,
        /** Raw names of tools removed since the prior call, restricted because a tool name can be user-authored */
        @JsonProperty("toolsRemovedRaw") List<String> toolsRemovedRaw,
        /** Raw names of tools redefined since the prior call, restricted because a tool name can be user-authored */
        @JsonProperty("toolsRedefinedRaw") List<String> toolsRedefinedRaw,
        /** Whether the tool list kept its members but changed their order */
        @JsonProperty("toolsReordered") Boolean toolsReordered,
        /** Names of the system-prompt segments whose content changed */
        @JsonProperty("systemSegmentsChanged") List<String> systemSegmentsChanged,
        /** Names of the cache-configuration fields that changed */
        @JsonProperty("cacheConfigChangedFields") List<String> cacheConfigChangedFields,
        /** Index of the first conversation message whose content changed */
        @JsonProperty("rewriteMessageIndex") Long rewriteMessageIndex,
        /** Shape of the history rewrite, for example whether the history grew or shrank */
        @JsonProperty("rewriteShape") String rewriteShape,
        /** Subsystems that announced a history rewrite before this call, for example compaction or truncation */
        @JsonProperty("rewriteSource") List<String> rewriteSource,
        /** Name of the sub-agent whose conversation broke, stamped by the parent bridge */
        @JsonProperty("agentName") String agentName
    ) {
    }
}

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
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Per-source attribution breakdown for the session's current context window, or null if uninitialized.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionMetadataGetContextAttributionResult(
    /** Per-source context-window attribution, or null if the session has not yet been initialized (no system prompt or tool metadata cached). */
    @JsonProperty("contextAttribution") SessionMetadataGetContextAttributionResultContextAttribution contextAttribution
) {

    /** Per-source token attribution snapshot for the current context window. The heaviest individual messages are available separately via `metadata.getContextHeaviestMessages`. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionMetadataGetContextAttributionResultContextAttribution(
        /** Total token count of the current context window the entries are measured against (system message + conversation messages + tool definitions — the same total reported by /context). Divide an entry's `tokens` by this to derive its share. */
        @JsonProperty("totalTokens") Long totalTokens,
        /** The concrete model id the entire breakdown was tokenized against (feeds the per-model token multiplier). Under `Auto` (Free/Student) this is the resolved model, not the literal `auto` sentinel, so totals are not undercounted. A single-model approximation of a potentially multi-model Auto session. */
        @JsonProperty("modelId") String modelId,
        /** How `modelId` was chosen. Not a closed set — tolerate unknown values. Known values today: `autoResolved` (the model Auto resolved to), `selected` (the user's explicitly selected model), `default` (a fallback before any model is known). */
        @JsonProperty("modelSource") String modelSource,
        /** Maximum prompt tokens the resolved model accepts — the denominator for a `##k/###k` context-usage display. Mirrors `SessionContextInfo.promptTokenLimit`. */
        @JsonProperty("promptTokenLimit") Long promptTokenLimit,
        /** Prompt limit plus the model's output reserve: the full context window `categories.freeSpace` and `categories.buffer` are measured against. Mirrors `SessionContextInfo.limit`. */
        @JsonProperty("limit") Long limit,
        /** Output reserve plus the tokens past the buffer-exhaustion blocking threshold. Mirrors `SessionContextInfo.bufferTokens`. */
        @JsonProperty("bufferTokens") Long bufferTokens,
        /** Token count at which background compaction starts. Mirrors `SessionContextInfo.compactionThreshold`. */
        @JsonProperty("compactionThreshold") Long compactionThreshold,
        /** The six normalized `/context` header buckets, computed from the same tokenization as `entries` so the two never disagree. Convenience rollups: `freeSpace` and `buffer` describe window capacity rather than occupied context, so the values do not sum to `totalTokens`. */
        @JsonProperty("categories") SessionMetadataGetContextAttributionResultContextAttributionCategories categories,
        /** Flat list of per-source attribution entries. Group by `kind` and render unrecognized kinds generically. Nesting and rollups are expressed via `parentId`. */
        @JsonProperty("entries") List<SessionMetadataGetContextAttributionResultContextAttributionEntriesItem> entries,
        /** Successful compaction history for the session. */
        @JsonProperty("compactions") SessionMetadataGetContextAttributionResultContextAttributionCompactions compactions
    ) {

        /** The six normalized `/context` header buckets, computed from the same tokenization as `entries` so the two never disagree. Convenience rollups: `freeSpace` and `buffer` describe window capacity rather than occupied context, so the values do not sum to `totalTokens`. */
        @JsonIgnoreProperties(ignoreUnknown = true)
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record SessionMetadataGetContextAttributionResultContextAttributionCategories(
            /** System prompt tokens, excluding custom instructions. */
            @JsonProperty("systemPrompt") Long systemPrompt,
            /** Custom-instructions tokens (0 when none are configured). */
            @JsonProperty("customInstructions") Long customInstructions,
            /** Non-MCP tool-definition tokens. */
            @JsonProperty("systemTools") Long systemTools,
            /** MCP tool-definition tokens. */
            @JsonProperty("mcpTools") Long mcpTools,
            /** Conversation (user/assistant/tool) message tokens. */
            @JsonProperty("messages") Long messages,
            /** Remaining unused window capacity (clamped at 0). */
            @JsonProperty("freeSpace") Long freeSpace,
            /** Output reserve plus post-blocking-threshold buffer. */
            @JsonProperty("buffer") Long buffer
        ) {
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record SessionMetadataGetContextAttributionResultContextAttributionEntriesItem(
            /** Source category for this entry. Not a closed set — tolerate unknown values. Known values today: `skill`, `subagent`, `mcpServer`, `tool`, `system`, `toolDefinition`, `plugin`. */
            @JsonProperty("kind") String kind,
            /** Identifier for this entry, formed by joining its `kind` and source name (e.g. `tool:bash`, `skill:tmux`, `toolDefinition:bash`); unique within the snapshot. Use it to match the same entry across snapshots, to correlate with other APIs (skill/agent/MCP registries), and as the `parentId` target for nesting. Distinct from the human-facing `label`. */
            @JsonProperty("id") String id,
            /** Human-readable display label, e.g. `bash` or `skill: tmux`. Presentation-only; may be localized/reformatted without notice — do not key off it. */
            @JsonProperty("label") String label,
            /** Token count currently in context attributable to this entry. */
            @JsonProperty("tokens") Long tokens,
            /** Optional `id` of the parent entry: e.g. a `plugin` entry parenting its `skill`/`mcpServer` entries, or the `system` entry parenting `toolDefinition` entries. Omitted for top-level entries. */
            @JsonProperty("parentId") String parentId,
            /** Supplementary per-entry metadata (e.g. `messageCount`, `role`, `evictable`, `pluginSource`). Values are stringified; parse as needed and ignore unrecognized keys. */
            @JsonProperty("attributes") Map<String, String> attributes
        ) {
        }

        /** Successful compaction history for the session. */
        @JsonIgnoreProperties(ignoreUnknown = true)
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record SessionMetadataGetContextAttributionResultContextAttributionCompactions(
            /** Number of successful compactions in this session. */
            @JsonProperty("count") Long count
        ) {
        }
    }
}

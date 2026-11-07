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
    @JsonInclude(JsonInclude.Include.ALWAYS)
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
        /** Effective input budget after reserving requested output against the combined context ceiling. Mirrors `SessionContextInfo.promptTokenLimit`. */
        @JsonProperty("promptTokenLimit") Long promptTokenLimit,
        /** Advertised prompt allowance for the selected context tier: the denominator for context-usage displays and capacity for `categories.freeSpace` and `categories.buffer`. Mirrors `SessionContextInfo.limit`. */
        @JsonProperty("limit") Long limit,
        /** Output reservation overlapping the displayed prompt allowance plus the tokens past the effective input budget's buffer-exhaustion blocking threshold. Mirrors `SessionContextInfo.bufferTokens`. */
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
            /** Overlapping output reservation plus post-blocking-threshold buffer. */
            @JsonProperty("buffer") Long buffer
        ) {

            /**
             * Creates a record positionally.
             * @deprecated Use {@link #builder()} for named construction.
             */
            @Deprecated(since = "2.0.0", forRemoval = false)
            public SessionMetadataGetContextAttributionResultContextAttributionCategories {}

            /**
             * Returns a named builder.
             * @return a new builder
             * @since 2.0.0
             */
            public static Builder builder() { return new Builder(); }

            /**
             * Named construction for this record.
             * @since 2.0.0
             */
            public static final class Builder {
                private Builder() {}
                private Long systemPrompt;
                private Long customInstructions;
                private Long systemTools;
                private Long mcpTools;
                private Long messages;
                private Long freeSpace;
                private Long buffer;

                /**
                 * Sets the {@code systemPrompt} property.
                 * @param value the property value
                 * @return this builder
                 * @since 2.0.0
                 */
                public Builder systemPrompt(Long value) {
                    this.systemPrompt = value;
                    return this;
                }

                /**
                 * Sets the {@code customInstructions} property.
                 * @param value the property value
                 * @return this builder
                 * @since 2.0.0
                 */
                public Builder customInstructions(Long value) {
                    this.customInstructions = value;
                    return this;
                }

                /**
                 * Sets the {@code systemTools} property.
                 * @param value the property value
                 * @return this builder
                 * @since 2.0.0
                 */
                public Builder systemTools(Long value) {
                    this.systemTools = value;
                    return this;
                }

                /**
                 * Sets the {@code mcpTools} property.
                 * @param value the property value
                 * @return this builder
                 * @since 2.0.0
                 */
                public Builder mcpTools(Long value) {
                    this.mcpTools = value;
                    return this;
                }

                /**
                 * Sets the {@code messages} property.
                 * @param value the property value
                 * @return this builder
                 * @since 2.0.0
                 */
                public Builder messages(Long value) {
                    this.messages = value;
                    return this;
                }

                /**
                 * Sets the {@code freeSpace} property.
                 * @param value the property value
                 * @return this builder
                 * @since 2.0.0
                 */
                public Builder freeSpace(Long value) {
                    this.freeSpace = value;
                    return this;
                }

                /**
                 * Sets the {@code buffer} property.
                 * @param value the property value
                 * @return this builder
                 * @since 2.0.0
                 */
                public Builder buffer(Long value) {
                    this.buffer = value;
                    return this;
                }

                /**
                 * Builds a record after checking required property presence.
                 * @return a new record
                 * @throws IllegalStateException if a required property is missing
                 * @since 2.0.0
                 */
                public SessionMetadataGetContextAttributionResultContextAttributionCategories build() {
                    if (systemPrompt == null) throw new IllegalStateException("Missing required property: systemPrompt");
                    if (customInstructions == null) throw new IllegalStateException("Missing required property: customInstructions");
                    if (systemTools == null) throw new IllegalStateException("Missing required property: systemTools");
                    if (mcpTools == null) throw new IllegalStateException("Missing required property: mcpTools");
                    if (messages == null) throw new IllegalStateException("Missing required property: messages");
                    if (freeSpace == null) throw new IllegalStateException("Missing required property: freeSpace");
                    if (buffer == null) throw new IllegalStateException("Missing required property: buffer");
                    return new SessionMetadataGetContextAttributionResultContextAttributionCategories(systemPrompt, customInstructions, systemTools, mcpTools, messages, freeSpace, buffer);
                }
            }
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

            /**
             * Creates a record positionally.
             * @deprecated Use {@link #builder()} for named construction.
             */
            @Deprecated(since = "2.0.0", forRemoval = false)
            public SessionMetadataGetContextAttributionResultContextAttributionEntriesItem {}

            /**
             * Returns a named builder.
             * @return a new builder
             * @since 2.0.0
             */
            public static Builder builder() { return new Builder(); }

            /**
             * Named construction for this record.
             * @since 2.0.0
             */
            public static final class Builder {
                private Builder() {}
                private String kind;
                private String id;
                private String label;
                private Long tokens;
                private String parentId;
                private Map<String, String> attributes;

                /**
                 * Sets the {@code kind} property.
                 * @param value the property value
                 * @return this builder
                 * @since 2.0.0
                 */
                public Builder kind(String value) {
                    this.kind = value;
                    return this;
                }

                /**
                 * Sets the {@code id} property.
                 * @param value the property value
                 * @return this builder
                 * @since 2.0.0
                 */
                public Builder id(String value) {
                    this.id = value;
                    return this;
                }

                /**
                 * Sets the {@code label} property.
                 * @param value the property value
                 * @return this builder
                 * @since 2.0.0
                 */
                public Builder label(String value) {
                    this.label = value;
                    return this;
                }

                /**
                 * Sets the {@code tokens} property.
                 * @param value the property value
                 * @return this builder
                 * @since 2.0.0
                 */
                public Builder tokens(Long value) {
                    this.tokens = value;
                    return this;
                }

                /**
                 * Sets the {@code parentId} property.
                 * @param value the property value
                 * @return this builder
                 * @since 2.0.0
                 */
                public Builder parentId(String value) {
                    this.parentId = value;
                    return this;
                }

                /**
                 * Sets the {@code attributes} property.
                 * @param value the property value
                 * @return this builder
                 * @since 2.0.0
                 */
                public Builder attributes(Map<String, String> value) {
                    this.attributes = value;
                    return this;
                }

                /**
                 * Builds a record after checking required property presence.
                 * @return a new record
                 * @throws IllegalStateException if a required property is missing
                 * @since 2.0.0
                 */
                public SessionMetadataGetContextAttributionResultContextAttributionEntriesItem build() {
                    if (kind == null) throw new IllegalStateException("Missing required property: kind");
                    if (id == null) throw new IllegalStateException("Missing required property: id");
                    if (label == null) throw new IllegalStateException("Missing required property: label");
                    if (tokens == null) throw new IllegalStateException("Missing required property: tokens");
                    return new SessionMetadataGetContextAttributionResultContextAttributionEntriesItem(kind, id, label, tokens, parentId, attributes);
                }
            }
        }

        /** Successful compaction history for the session. */
        @JsonIgnoreProperties(ignoreUnknown = true)
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record SessionMetadataGetContextAttributionResultContextAttributionCompactions(
            /** Number of successful compactions in this session. */
            @JsonProperty("count") Long count
        ) {

            /**
             * Creates a record positionally.
             * @deprecated Use {@link #builder()} for named construction.
             */
            @Deprecated(since = "2.0.0", forRemoval = false)
            public SessionMetadataGetContextAttributionResultContextAttributionCompactions {}

            /**
             * Returns a named builder.
             * @return a new builder
             * @since 2.0.0
             */
            public static Builder builder() { return new Builder(); }

            /**
             * Named construction for this record.
             * @since 2.0.0
             */
            public static final class Builder {
                private Builder() {}
                private Long count;

                /**
                 * Sets the {@code count} property.
                 * @param value the property value
                 * @return this builder
                 * @since 2.0.0
                 */
                public Builder count(Long value) {
                    this.count = value;
                    return this;
                }

                /**
                 * Builds a record after checking required property presence.
                 * @return a new record
                 * @throws IllegalStateException if a required property is missing
                 * @since 2.0.0
                 */
                public SessionMetadataGetContextAttributionResultContextAttributionCompactions build() {
                    if (count == null) throw new IllegalStateException("Missing required property: count");
                    return new SessionMetadataGetContextAttributionResultContextAttributionCompactions(count);
                }
            }
        }

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionMetadataGetContextAttributionResultContextAttribution {}

        /**
         * Returns a named builder.
         * @return a new builder
         * @since 2.0.0
         */
        public static Builder builder() { return new Builder(); }

        /**
         * Named construction for this record.
         * @since 2.0.0
         */
        public static final class Builder {
            private Builder() {}
            private Long totalTokens;
            private String modelId;
            private String modelSource;
            private Long promptTokenLimit;
            private Long limit;
            private Long bufferTokens;
            private Long compactionThreshold;
            private SessionMetadataGetContextAttributionResultContextAttributionCategories categories;
            private List<SessionMetadataGetContextAttributionResultContextAttributionEntriesItem> entries;
            private SessionMetadataGetContextAttributionResultContextAttributionCompactions compactions;

            /**
             * Sets the {@code totalTokens} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder totalTokens(Long value) {
                this.totalTokens = value;
                return this;
            }

            /**
             * Sets the {@code modelId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder modelId(String value) {
                this.modelId = value;
                return this;
            }

            /**
             * Sets the {@code modelSource} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder modelSource(String value) {
                this.modelSource = value;
                return this;
            }

            /**
             * Sets the {@code promptTokenLimit} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder promptTokenLimit(Long value) {
                this.promptTokenLimit = value;
                return this;
            }

            /**
             * Sets the {@code limit} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder limit(Long value) {
                this.limit = value;
                return this;
            }

            /**
             * Sets the {@code bufferTokens} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder bufferTokens(Long value) {
                this.bufferTokens = value;
                return this;
            }

            /**
             * Sets the {@code compactionThreshold} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder compactionThreshold(Long value) {
                this.compactionThreshold = value;
                return this;
            }

            /**
             * Sets the {@code categories} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder categories(SessionMetadataGetContextAttributionResultContextAttributionCategories value) {
                this.categories = value;
                return this;
            }

            /**
             * Sets the {@code entries} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder entries(List<SessionMetadataGetContextAttributionResultContextAttributionEntriesItem> value) {
                this.entries = value;
                return this;
            }

            /**
             * Sets the {@code compactions} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder compactions(SessionMetadataGetContextAttributionResultContextAttributionCompactions value) {
                this.compactions = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionMetadataGetContextAttributionResultContextAttribution build() {
                if (totalTokens == null) throw new IllegalStateException("Missing required property: totalTokens");
                if (modelId == null) throw new IllegalStateException("Missing required property: modelId");
                if (modelSource == null) throw new IllegalStateException("Missing required property: modelSource");
                if (promptTokenLimit == null) throw new IllegalStateException("Missing required property: promptTokenLimit");
                if (limit == null) throw new IllegalStateException("Missing required property: limit");
                if (bufferTokens == null) throw new IllegalStateException("Missing required property: bufferTokens");
                if (compactionThreshold == null) throw new IllegalStateException("Missing required property: compactionThreshold");
                if (categories == null) throw new IllegalStateException("Missing required property: categories");
                if (entries == null) throw new IllegalStateException("Missing required property: entries");
                if (compactions == null) throw new IllegalStateException("Missing required property: compactions");
                return new SessionMetadataGetContextAttributionResultContextAttribution(totalTokens, modelId, modelSource, promptTokenLimit, limit, bufferTokens, compactionThreshold, categories, entries, compactions);
            }
        }
    }

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionMetadataGetContextAttributionResult {}

    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    public static final class Builder {
        private Builder() {}
        private SessionMetadataGetContextAttributionResultContextAttribution contextAttribution;
        private boolean $presence0;

        /**
         * Sets the {@code contextAttribution} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder contextAttribution(SessionMetadataGetContextAttributionResultContextAttribution value) {
            this.contextAttribution = value;
            this.$presence0 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionMetadataGetContextAttributionResult build() {
            if (!$presence0) throw new IllegalStateException("Missing required property: contextAttribution");
            return new SessionMetadataGetContextAttributionResult(contextAttribution);
        }
    }
}

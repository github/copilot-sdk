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
 * Token breakdown for the session's current context window, or null if uninitialized.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionMetadataContextInfoResult(
    /** Token breakdown for the current context window, or null if the session has not yet been initialized (no system prompt or tool metadata cached). */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("contextInfo") SessionMetadataContextInfoResultContextInfo contextInfo
) {

    /** Token-usage breakdown for the session's current context window */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionMetadataContextInfoResultContextInfo(
        /** The model used for token counting */
        @JsonProperty("modelName") String modelName,
        /** Provider whose catalog supplies this context window. Omitted when provider identity is unavailable. */
        @JsonProperty("provider") ModelProviderRef provider,
        /** Provider-owned model display label, or the Auto routing label. Presentation only; modelName remains the tokenization identity. */
        @JsonProperty("displayModelName") String displayModelName,
        /** Tokens consumed by the system prompt */
        @JsonProperty("systemTokens") Long systemTokens,
        /** Tokens consumed by user/assistant/tool messages */
        @JsonProperty("conversationTokens") Long conversationTokens,
        /** Tokens consumed by tool definitions sent to the model (excludes deferred tools) */
        @JsonProperty("toolDefinitionsTokens") Long toolDefinitionsTokens,
        /** Tokens consumed by MCP tool definitions (subset of toolDefinitionsTokens, excludes deferred tools) */
        @JsonProperty("mcpToolsTokens") Long mcpToolsTokens,
        /** Sum of system, conversation and tool-definition tokens */
        @JsonProperty("totalTokens") Long totalTokens,
        /** Effective input budget: the selected tier's prompt allowance bounded by the combined context ceiling minus the requested output allowance. Uses DEFAULT_TOKEN_LIMIT when limits are unspecified. */
        @JsonProperty("promptTokenLimit") Long promptTokenLimit,
        /** Token count at which background compaction starts (configurable percentage of promptTokenLimit) */
        @JsonProperty("compactionThreshold") Long compactionThreshold,
        /** Advertised prompt allowance for the selected context tier, without adding output tokens. The denominator for context-usage displays. */
        @JsonProperty("limit") Long limit,
        /** Output reservation overlapping the displayed prompt allowance plus tokens after the effective input budget's buffer-exhaustion blocking threshold (default 95%). */
        @JsonProperty("bufferTokens") Long bufferTokens
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionMetadataContextInfoResultContextInfo {}

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
            private String modelName;
            private ModelProviderRef provider;
            private String displayModelName;
            private Long systemTokens;
            private Long conversationTokens;
            private Long toolDefinitionsTokens;
            private Long mcpToolsTokens;
            private Long totalTokens;
            private Long promptTokenLimit;
            private Long compactionThreshold;
            private Long limit;
            private Long bufferTokens;

            /**
             * Sets the {@code modelName} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder modelName(String value) {
                this.modelName = value;
                return this;
            }

            /**
             * Sets the {@code provider} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder provider(ModelProviderRef value) {
                this.provider = value;
                return this;
            }

            /**
             * Sets the {@code displayModelName} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder displayModelName(String value) {
                this.displayModelName = value;
                return this;
            }

            /**
             * Sets the {@code systemTokens} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder systemTokens(Long value) {
                this.systemTokens = value;
                return this;
            }

            /**
             * Sets the {@code conversationTokens} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder conversationTokens(Long value) {
                this.conversationTokens = value;
                return this;
            }

            /**
             * Sets the {@code toolDefinitionsTokens} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder toolDefinitionsTokens(Long value) {
                this.toolDefinitionsTokens = value;
                return this;
            }

            /**
             * Sets the {@code mcpToolsTokens} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder mcpToolsTokens(Long value) {
                this.mcpToolsTokens = value;
                return this;
            }

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
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionMetadataContextInfoResultContextInfo build() {
                if (modelName == null) throw new IllegalStateException("Missing required property: modelName");
                if (systemTokens == null) throw new IllegalStateException("Missing required property: systemTokens");
                if (conversationTokens == null) throw new IllegalStateException("Missing required property: conversationTokens");
                if (toolDefinitionsTokens == null) throw new IllegalStateException("Missing required property: toolDefinitionsTokens");
                if (mcpToolsTokens == null) throw new IllegalStateException("Missing required property: mcpToolsTokens");
                if (totalTokens == null) throw new IllegalStateException("Missing required property: totalTokens");
                if (promptTokenLimit == null) throw new IllegalStateException("Missing required property: promptTokenLimit");
                if (compactionThreshold == null) throw new IllegalStateException("Missing required property: compactionThreshold");
                if (limit == null) throw new IllegalStateException("Missing required property: limit");
                if (bufferTokens == null) throw new IllegalStateException("Missing required property: bufferTokens");
                return new SessionMetadataContextInfoResultContextInfo(modelName, provider, displayModelName, systemTokens, conversationTokens, toolDefinitionsTokens, mcpToolsTokens, totalTokens, promptTokenLimit, compactionThreshold, limit, bufferTokens);
            }
        }
    }

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionMetadataContextInfoResult {}

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
        private SessionMetadataContextInfoResultContextInfo contextInfo;
        private boolean $presence0;

        /**
         * Sets the {@code contextInfo} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder contextInfo(SessionMetadataContextInfoResultContextInfo value) {
            this.contextInfo = value;
            this.$presence0 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionMetadataContextInfoResult build() {
            if (!$presence0) throw new IllegalStateException("Missing required property: contextInfo");
            return new SessionMetadataContextInfoResult(contextInfo);
        }
    }
}

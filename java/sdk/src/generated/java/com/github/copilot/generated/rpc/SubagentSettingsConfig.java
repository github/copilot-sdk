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
 * Configured per-agent subagent overrides
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SubagentSettingsConfig(
    /** Per-agent settings keyed by subagent agent_type */
    @JsonProperty("agents") Map<String, SubagentSettingsEntry> agents,
    /** Configured opt-in for subagent context management tools; applied from persisted settings when the session starts */
    @JsonProperty("contextManagementTools") Boolean contextManagementTools,
    /** Names of subagents the user has turned off; they cannot be dispatched */
    @JsonProperty("disabledSubagents") List<String> disabledSubagents,
    /** Maximum number of subagents that can run concurrently; applies to usage-based billing users only */
    @JsonProperty("maxConcurrency") Long maxConcurrency,
    /** Maximum subagent nesting depth; applies to usage-based billing users only */
    @JsonProperty("maxDepth") Long maxDepth
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SubagentSettingsConfig {}

    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static final class Builder {
        private Builder() {}
        private Map<String, SubagentSettingsEntry> agents;
        private Boolean contextManagementTools;
        private List<String> disabledSubagents;
        private Long maxConcurrency;
        private Long maxDepth;

        /**
         * Sets the {@code agents} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder agents(Map<String, SubagentSettingsEntry> value) {
            this.agents = value;
            return this;
        }

        /**
         * Sets the {@code contextManagementTools} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder contextManagementTools(Boolean value) {
            this.contextManagementTools = value;
            return this;
        }

        /**
         * Sets the {@code disabledSubagents} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder disabledSubagents(List<String> value) {
            this.disabledSubagents = value;
            return this;
        }

        /**
         * Sets the {@code maxConcurrency} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder maxConcurrency(Long value) {
            this.maxConcurrency = value;
            return this;
        }

        /**
         * Sets the {@code maxDepth} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder maxDepth(Long value) {
            this.maxDepth = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SubagentSettingsConfig build() {
            return new SubagentSettingsConfig(agents, contextManagementTools, disabledSubagents, maxConcurrency, maxDepth);
        }
    }
}

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
 * Whether the gh-replaceable GitHub MCP tools may be clipped for the session described by the request.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record McpShouldExcludeGitHubToolsResult(
    /** True only when both halves hold: the session's filters still reach the platform shell tool, and the host actually has the `gh` those tools would be replaced by. Feed it straight back as the `excludeGhReplaceableTools` build option. */
    @JsonProperty("excludeGhReplaceableTools") Boolean excludeGhReplaceableTools
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    McpShouldExcludeGitHubToolsResult {}

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
        private Boolean excludeGhReplaceableTools;

        /**
         * Sets the {@code excludeGhReplaceableTools} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder excludeGhReplaceableTools(Boolean value) {
            this.excludeGhReplaceableTools = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public McpShouldExcludeGitHubToolsResult build() {
            if (excludeGhReplaceableTools == null) throw new IllegalStateException("Missing required property: excludeGhReplaceableTools");
            return new McpShouldExcludeGitHubToolsResult(excludeGhReplaceableTools);
        }
    }
}

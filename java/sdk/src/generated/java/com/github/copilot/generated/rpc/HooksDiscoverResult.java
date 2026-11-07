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
 * Server-discovered hook actions and partial-load diagnostics from user, repository, plugin, and managed-policy sources. Concrete sessions may include additional session-specific hook sources.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record HooksDiscoverResult(
    /** All discovered hook actions. Byte-identical actions remain separate rows even when they share a disable key. */
    @JsonProperty("hooks") List<DiscoveredHook> hooks,
    /** Non-fatal source-loading warnings. Discovery remains complete for the affected source, although the source had a recoverable issue. Repository-settings warnings are prefixed with their project path when attribution is available. */
    @JsonProperty("warnings") List<String> warnings,
    /** Errors for hook sources or actions that could not be loaded, making the result partially incomplete. Other valid actions are still returned. Project-resolution and repository-settings errors are prefixed with their project path. */
    @JsonProperty("errors") List<String> errors
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public HooksDiscoverResult {}

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
        private List<DiscoveredHook> hooks;
        private List<String> warnings;
        private List<String> errors;

        /**
         * Sets the {@code hooks} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder hooks(List<DiscoveredHook> value) {
            this.hooks = value;
            return this;
        }

        /**
         * Sets the {@code warnings} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder warnings(List<String> value) {
            this.warnings = value;
            return this;
        }

        /**
         * Sets the {@code errors} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder errors(List<String> value) {
            this.errors = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public HooksDiscoverResult build() {
            if (hooks == null) throw new IllegalStateException("Missing required property: hooks");
            if (warnings == null) throw new IllegalStateException("Missing required property: warnings");
            if (errors == null) throw new IllegalStateException("Missing required property: errors");
            return new HooksDiscoverResult(hooks, warnings, errors);
        }
    }
}

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
 * Queued repo-level startup prompts and the total hook command count after loading.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionsLoadDeferredRepoHooksResult(
    /** Repo-level startup prompts queued from repo hook configs. Empty on resume, when no repo configs were pending, or when disableAllHooks is set. */
    @JsonProperty("startupPrompts") List<String> startupPrompts,
    /** Total hook command count (user + plugin + repo) loaded for the session by this call. Captured atomically with startupPrompts so callers don't need to read a separate counter. */
    @JsonProperty("hookCount") Long hookCount
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionsLoadDeferredRepoHooksResult {}

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
        private List<String> startupPrompts;
        private Long hookCount;

        /**
         * Sets the {@code startupPrompts} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder startupPrompts(List<String> value) {
            this.startupPrompts = value;
            return this;
        }

        /**
         * Sets the {@code hookCount} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder hookCount(Long value) {
            this.hookCount = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionsLoadDeferredRepoHooksResult build() {
            if (startupPrompts == null) throw new IllegalStateException("Missing required property: startupPrompts");
            if (hookCount == null) throw new IllegalStateException("Missing required property: hookCount");
            return new SessionsLoadDeferredRepoHooksResult(startupPrompts, hookCount);
        }
    }
}

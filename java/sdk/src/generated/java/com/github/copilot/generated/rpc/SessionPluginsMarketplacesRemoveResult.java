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
 * Outcome of the remove attempt, including dependent-plugin info when applicable.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionPluginsMarketplacesRemoveResult(
    /** True when the marketplace was actually removed. False when removal was skipped because the marketplace has dependent plugins and `force` was not set. */
    @JsonProperty("removed") Boolean removed,
    /** Names of installed plugins that prevented removal. Populated only when `removed=false`. */
    @JsonProperty("dependentPlugins") List<String> dependentPlugins
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionPluginsMarketplacesRemoveResult {}

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
        private Boolean removed;
        private List<String> dependentPlugins;

        /**
         * Sets the {@code removed} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder removed(Boolean value) {
            this.removed = value;
            return this;
        }

        /**
         * Sets the {@code dependentPlugins} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder dependentPlugins(List<String> value) {
            this.dependentPlugins = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionPluginsMarketplacesRemoveResult build() {
            if (removed == null) throw new IllegalStateException("Missing required property: removed");
            return new SessionPluginsMarketplacesRemoveResult(removed, dependentPlugins);
        }
    }
}

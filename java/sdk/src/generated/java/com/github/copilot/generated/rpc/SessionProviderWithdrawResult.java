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
 * What the withdrawal actually removed from the registry.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionProviderWithdrawResult(
    /** Selection ids that were registered and are now withdrawn. Excludes requested ids that were not present. */
    @JsonProperty("withdrawn") List<String> withdrawn,
    /** Providers removed because one of the withdrawn models was the last entry referencing them. A provider that merely has no models is not removed. */
    @JsonProperty("providersRemoved") List<String> providersRemoved,
    /** True when withdrawal removed the selected host-managed model, leaving the session with no explicit selection, so ordinary model resolution picks the session default. Withdrawal never promotes a surviving model in its place: the choice of which model to use stays with the user. */
    @JsonProperty("modelDeselected") Boolean modelDeselected
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionProviderWithdrawResult {}

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
        private List<String> withdrawn;
        private List<String> providersRemoved;
        private Boolean modelDeselected;

        /**
         * Sets the {@code withdrawn} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder withdrawn(List<String> value) {
            this.withdrawn = value;
            return this;
        }

        /**
         * Sets the {@code providersRemoved} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder providersRemoved(List<String> value) {
            this.providersRemoved = value;
            return this;
        }

        /**
         * Sets the {@code modelDeselected} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder modelDeselected(Boolean value) {
            this.modelDeselected = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionProviderWithdrawResult build() {
            if (withdrawn == null) throw new IllegalStateException("Missing required property: withdrawn");
            if (providersRemoved == null) throw new IllegalStateException("Missing required property: providersRemoved");
            return new SessionProviderWithdrawResult(withdrawn, providersRemoved, modelDeselected);
        }
    }
}

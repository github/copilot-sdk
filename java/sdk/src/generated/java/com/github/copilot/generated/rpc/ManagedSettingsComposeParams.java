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
 * Candidate managed-settings documents to merge without applying them.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ManagedSettingsComposeParams(
    /** One entry per channel. `source` must be `device`, `server`, or `policyHelper`, each at most once (checked at runtime); order does not matter, because channel precedence is fixed. To preview documents from resolve output, map recognized source strings to ManagedSettingsChannel and copy their settings; generated resolve and compose layer types are distinct. Omitted settings means this channel delivered no document. Supplied documents must be valid within the preview limits; warnings are returned in diagnostics. Compose does not reproduce source-failure state or retained enforcement floors from resolve. */
    @JsonProperty("layers") List<ManagedSettingsComposeLayer> layers
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ManagedSettingsComposeParams {}

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
        private List<ManagedSettingsComposeLayer> layers;

        /**
         * Sets the {@code layers} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder layers(List<ManagedSettingsComposeLayer> value) {
            this.layers = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ManagedSettingsComposeParams build() {
            if (layers == null) throw new IllegalStateException("Missing required property: layers");
            return new ManagedSettingsComposeParams(layers);
        }
    }
}

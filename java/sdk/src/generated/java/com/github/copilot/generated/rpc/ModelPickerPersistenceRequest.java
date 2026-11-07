/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelPickerPersistenceRequest(
    /** Filesystem and environment context used to resolve settings persistence. */
    @JsonProperty("settingsContext") ModelPickerSettingsContext settingsContext,
    /** Whether reasoning effort was explicitly selected and should be persisted. */
    @JsonProperty("reasoningEffortExplicit") Boolean reasoningEffortExplicit,
    /** Whether context tier was explicitly selected and should be persisted. */
    @JsonProperty("contextTierExplicit") Boolean contextTierExplicit
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ModelPickerPersistenceRequest {}

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
        private ModelPickerSettingsContext settingsContext;
        private Boolean reasoningEffortExplicit;
        private Boolean contextTierExplicit;

        /**
         * Sets the {@code settingsContext} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder settingsContext(ModelPickerSettingsContext value) {
            this.settingsContext = value;
            return this;
        }

        /**
         * Sets the {@code reasoningEffortExplicit} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder reasoningEffortExplicit(Boolean value) {
            this.reasoningEffortExplicit = value;
            return this;
        }

        /**
         * Sets the {@code contextTierExplicit} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder contextTierExplicit(Boolean value) {
            this.contextTierExplicit = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ModelPickerPersistenceRequest build() {
            if (settingsContext == null) throw new IllegalStateException("Missing required property: settingsContext");
            return new ModelPickerPersistenceRequest(settingsContext, reasoningEffortExplicit, contextTierExplicit);
        }
    }
}

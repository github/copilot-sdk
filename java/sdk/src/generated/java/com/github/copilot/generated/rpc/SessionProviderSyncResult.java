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
 * The selectable model entries and selection ids synthesized for the synchronized BYOK models.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionProviderSyncResult(
    /** Synthesized selectable model entries for the synchronized BYOK models. */
    @JsonProperty("models") List<Object> models,
    /** Provider-qualified model selection ids present after synchronization. */
    @JsonProperty("selectionIds") List<String> selectionIds,
    /** True when synchronization withdrew the selected host-managed model, leaving the session with no explicit selection, so ordinary model resolution picks the session default. Synchronization never promotes a surviving host model in its place: publishing a model offers it, and the choice of which model to use stays with the user. */
    @JsonProperty("modelDeselected") Boolean modelDeselected
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionProviderSyncResult {}

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
        private List<Object> models;
        private List<String> selectionIds;
        private Boolean modelDeselected;

        /**
         * Sets the {@code models} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder models(List<Object> value) {
            this.models = value;
            return this;
        }

        /**
         * Sets the {@code selectionIds} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder selectionIds(List<String> value) {
            this.selectionIds = value;
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
        public SessionProviderSyncResult build() {
            if (models == null) throw new IllegalStateException("Missing required property: models");
            if (selectionIds == null) throw new IllegalStateException("Missing required property: selectionIds");
            return new SessionProviderSyncResult(models, selectionIds, modelDeselected);
        }
    }
}

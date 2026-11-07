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
 * A cached startup model snapshot and an optional one-time enablement prompt. This check never resolves credentials or fetches a catalog.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionModelStartupAdmissionResult(
    /** The selected model, when the session has one. */
    @JsonProperty("selectedModel") String selectedModel,
    /** Whether the session already has a resolved Copilot catalog. */
    @JsonProperty("catalogReady") Boolean catalogReady,
    /** Whether the cached selected model currently requires enablement. */
    @JsonProperty("requiresEnablement") Boolean requiresEnablement,
    /** Whether this call consumed the session's startup enablement claim. */
    @JsonProperty("claimedEnablement") Boolean claimedEnablement,
    /** The selected model to enable, only for the first successful prompt claim. */
    @JsonProperty("modelToEnable") String modelToEnable
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionModelStartupAdmissionResult {}

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
        private String selectedModel;
        private Boolean catalogReady;
        private Boolean requiresEnablement;
        private Boolean claimedEnablement;
        private String modelToEnable;

        /**
         * Sets the {@code selectedModel} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder selectedModel(String value) {
            this.selectedModel = value;
            return this;
        }

        /**
         * Sets the {@code catalogReady} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder catalogReady(Boolean value) {
            this.catalogReady = value;
            return this;
        }

        /**
         * Sets the {@code requiresEnablement} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder requiresEnablement(Boolean value) {
            this.requiresEnablement = value;
            return this;
        }

        /**
         * Sets the {@code claimedEnablement} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder claimedEnablement(Boolean value) {
            this.claimedEnablement = value;
            return this;
        }

        /**
         * Sets the {@code modelToEnable} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder modelToEnable(String value) {
            this.modelToEnable = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionModelStartupAdmissionResult build() {
            if (catalogReady == null) throw new IllegalStateException("Missing required property: catalogReady");
            if (requiresEnablement == null) throw new IllegalStateException("Missing required property: requiresEnablement");
            if (claimedEnablement == null) throw new IllegalStateException("Missing required property: claimedEnablement");
            return new SessionModelStartupAdmissionResult(selectedModel, catalogReady, requiresEnablement, claimedEnablement, modelToEnable);
        }
    }
}

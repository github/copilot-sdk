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
public record AutopilotOptions(
    /** Enable runtime-owned automatic continuation while in Autopilot mode. Defaults to true for SDK sessions; does not change the session mode. */
    @JsonProperty("continuationEnabled") Boolean continuationEnabled,
    /** Maximum automatic continuations outside an active objective. Zero allows none; defaults to five. Omitted values preserve the current limit on update or warm resume. */
    @JsonProperty("maxContinues") Long maxContinues
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public AutopilotOptions {}

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
        private Boolean continuationEnabled;
        private Long maxContinues;

        /**
         * Sets the {@code continuationEnabled} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder continuationEnabled(Boolean value) {
            this.continuationEnabled = value;
            return this;
        }

        /**
         * Sets the {@code maxContinues} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder maxContinues(Long value) {
            this.maxContinues = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AutopilotOptions build() {
            return new AutopilotOptions(continuationEnabled, maxContinues);
        }
    }
}

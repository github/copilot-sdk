/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Per-candidate allow decisions, positionally aligned with the request.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record SandboxFilterPickerCandidatesResult(
    /** One decision per candidate, in the same order. Always the same length as the request's `candidates`. */
    @JsonProperty("allowed") List<Boolean> allowed
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    SandboxFilterPickerCandidatesResult {}

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
        private List<Boolean> allowed;

        /**
         * Sets the {@code allowed} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder allowed(List<Boolean> value) {
            this.allowed = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SandboxFilterPickerCandidatesResult build() {
            if (allowed == null) throw new IllegalStateException("Missing required property: allowed");
            return new SandboxFilterPickerCandidatesResult(allowed);
        }
    }
}

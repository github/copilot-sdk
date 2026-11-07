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
 * Batch content-exclusion result. Callers must fail closed when policy evaluation is unavailable.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionContentExclusionCheckPathsResult(
    /** Whether the session's policy service was available for the complete batch. When false, checks is empty and callers must treat every requested path as excluded. */
    @JsonProperty("available") Boolean available,
    /** Per-path decisions in request order. Empty when available is false. */
    @JsonProperty("checks") List<ContentExclusionPathCheck> checks
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionContentExclusionCheckPathsResult {}

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
        private Boolean available;
        private List<ContentExclusionPathCheck> checks;

        /**
         * Sets the {@code available} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder available(Boolean value) {
            this.available = value;
            return this;
        }

        /**
         * Sets the {@code checks} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder checks(List<ContentExclusionPathCheck> value) {
            this.checks = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionContentExclusionCheckPathsResult build() {
            if (available == null) throw new IllegalStateException("Missing required property: available");
            if (checks == null) throw new IllegalStateException("Missing required property: checks");
            return new SessionContentExclusionCheckPathsResult(available, checks);
        }
    }
}

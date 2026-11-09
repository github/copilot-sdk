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
 * Optional evaluations and readiness barriers for one feature snapshot.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionFeatureFlagsReadParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Wait for the current assignment. Omission does not wait. */
    @JsonProperty("waitForExpResponse") Boolean waitForExpResponse,
    /** Feature names evaluated with their canonical experiment mappings. */
    @JsonProperty("flags") List<String> flags,
    /** Explicit experiment/static queries, in result order. */
    @JsonProperty("overrides") List<FeatureFlagExpOverrideQuery> overrides,
    /** Wait for a different revision or service disposal using the existing change notification. */
    @JsonProperty("afterRevision") Long afterRevision
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionFeatureFlagsReadParams {}

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
        private String sessionId;
        private Boolean waitForExpResponse;
        private List<String> flags;
        private List<FeatureFlagExpOverrideQuery> overrides;
        private Long afterRevision;

        /**
         * Sets the {@code sessionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder sessionId(String value) {
            this.sessionId = value;
            return this;
        }

        /**
         * Sets the {@code waitForExpResponse} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder waitForExpResponse(Boolean value) {
            this.waitForExpResponse = value;
            return this;
        }

        /**
         * Sets the {@code flags} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder flags(List<String> value) {
            this.flags = value;
            return this;
        }

        /**
         * Sets the {@code overrides} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder overrides(List<FeatureFlagExpOverrideQuery> value) {
            this.overrides = value;
            return this;
        }

        /**
         * Sets the {@code afterRevision} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder afterRevision(Long value) {
            this.afterRevision = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionFeatureFlagsReadParams build() {
            return new SessionFeatureFlagsReadParams(sessionId, waitForExpResponse, flags, overrides, afterRevision);
        }
    }
}

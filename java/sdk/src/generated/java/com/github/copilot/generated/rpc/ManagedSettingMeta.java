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
 * Lock state and provenance of one managed setting.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ManagedSettingMeta(
    /** Whether users and repositories may choose a different value. `false` means policy locks the value. */
    @JsonProperty("overridable") Boolean overridable,
    /** Channel that supplied this scalar value, matching a `layers[].source`: `device`, `server`, or `policyHelper`. These scalar defaults select one winning channel, not a mixed source. Treat unknown values as additional channels; more may be added. */
    @JsonProperty("source") String source,
    /** Original managed value when the runtime adjusted it to a supported effective value. Omitted when no adjustment was needed. */
    @JsonProperty("requested") String requested
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ManagedSettingMeta {}

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
        private Boolean overridable;
        private String source;
        private String requested;

        /**
         * Sets the {@code overridable} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder overridable(Boolean value) {
            this.overridable = value;
            return this;
        }

        /**
         * Sets the {@code source} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder source(String value) {
            this.source = value;
            return this;
        }

        /**
         * Sets the {@code requested} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder requested(String value) {
            this.requested = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ManagedSettingMeta build() {
            if (overridable == null) throw new IllegalStateException("Missing required property: overridable");
            if (source == null) throw new IllegalStateException("Missing required property: source");
            return new ManagedSettingMeta(overridable, source, requested);
        }
    }
}

/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import javax.annotation.processing.Generated;

@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionSettingsPatch(
    /** Optional generation guard for a live configuration view. */
    @JsonProperty("serviceId") String serviceId,
    /** Whether this session enables memory, without changing the saved preference. */
    @JsonProperty("memory") Boolean memory,
    /** Explicit feature preferences. A null value removes that preference. */
    @JsonInclude(value = JsonInclude.Include.NON_NULL, content = JsonInclude.Include.ALWAYS)
    @JsonProperty("enabledFeatureFlags") Map<String, Boolean> enabledFeatureFlags
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionSettingsPatch {}

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
        private String serviceId;
        private Boolean memory;
        private Map<String, Boolean> enabledFeatureFlags;

        /**
         * Sets the {@code serviceId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder serviceId(String value) {
            this.serviceId = value;
            return this;
        }

        /**
         * Sets the {@code memory} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder memory(Boolean value) {
            this.memory = value;
            return this;
        }

        /**
         * Sets the {@code enabledFeatureFlags} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder enabledFeatureFlags(Map<String, Boolean> value) {
            this.enabledFeatureFlags = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionSettingsPatch build() {
            return new SessionSettingsPatch(serviceId, memory, enabledFeatureFlags);
        }
    }
}

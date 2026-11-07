/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: lifecycle-diagnostics.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import java.time.OffsetDateTime;

/** Experimental runtime lifecycle diagnostic event. */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
final class RuntimeLifecycleSystemStateDiagnosticEvent extends LifecycleDiagnosticEvent {
    @Override
    public String getType() { return "runtime.lifecycle.system_state"; }

    @JsonProperty("data")
    private RuntimeLifecycleSystemStateData data;

    public RuntimeLifecycleSystemStateData getData() { return data; }
    public void setData(RuntimeLifecycleSystemStateData data) { this.data = data; }

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
        private OffsetDateTime timestamp;
        private RuntimeLifecycleSystemStateData data;

        /**
         * Sets the {@code timestamp} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder timestamp(OffsetDateTime value) {
            this.timestamp = value;
            return this;
        }

        /**
         * Sets the {@code data} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder data(RuntimeLifecycleSystemStateData value) {
            this.data = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public RuntimeLifecycleSystemStateDiagnosticEvent build() {
            if (timestamp == null) throw new IllegalStateException("Missing required property: timestamp");
            if (data == null) throw new IllegalStateException("Missing required property: data");
            RuntimeLifecycleSystemStateDiagnosticEvent instance = new RuntimeLifecycleSystemStateDiagnosticEvent();
            instance.setTimestamp(timestamp);
            instance.setData(data);
            return instance;
        }
    }
}

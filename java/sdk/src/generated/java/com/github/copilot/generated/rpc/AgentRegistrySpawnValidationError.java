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

/**
 * Synchronous pre-validation rejected the spawn request.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AgentRegistrySpawnValidationError extends AgentRegistrySpawnResult {

    @JsonProperty("kind")
    private final String kind = "validation-error";

    @Override
    public String getKind() { return kind; }

    /** Categorized reason for the rejection. Low-cardinality enum so telemetry can aggregate by reason without leaking raw paths or agent/model names. */
    @JsonProperty("reason")
    private AgentRegistrySpawnValidationErrorReason reason;

    /** Which parameter field was invalid. Omitted when the rejection is not field-specific. */
    @JsonProperty("field")
    private AgentRegistrySpawnValidationErrorField field;

    /** Human-readable explanation; safe to surface in the UI banner. Never logged to unrestricted telemetry. */
    @JsonProperty("message")
    private String message;

    public AgentRegistrySpawnValidationErrorReason getReason() { return reason; }
    public void setReason(AgentRegistrySpawnValidationErrorReason reason) { this.reason = reason; }

    public AgentRegistrySpawnValidationErrorField getField() { return field; }
    public void setField(AgentRegistrySpawnValidationErrorField field) { this.field = field; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }


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
        private AgentRegistrySpawnValidationErrorReason reason;
        private AgentRegistrySpawnValidationErrorField field;
        private String message;

        /**
         * Sets the {@code reason} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder reason(AgentRegistrySpawnValidationErrorReason value) {
            this.reason = value;
            return this;
        }

        /**
         * Sets the {@code field} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder field(AgentRegistrySpawnValidationErrorField value) {
            this.field = value;
            return this;
        }

        /**
         * Sets the {@code message} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder message(String value) {
            this.message = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AgentRegistrySpawnValidationError build() {
            if (reason == null) throw new IllegalStateException("Missing required property: reason");
            if (message == null) throw new IllegalStateException("Missing required property: message");
            AgentRegistrySpawnValidationError instance = new AgentRegistrySpawnValidationError();
            instance.setReason(reason);
            instance.setField(field);
            instance.setMessage(message);
            return instance;
        }
    }
}

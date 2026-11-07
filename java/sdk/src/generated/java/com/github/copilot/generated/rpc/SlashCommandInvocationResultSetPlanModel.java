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
 * Variant {@code set-plan-model} of {@link SlashCommandInvocationResult}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SlashCommandInvocationResultSetPlanModel extends SlashCommandInvocationResult {

    @JsonProperty("kind")
    private final String kind = "set-plan-model";

    @Override
    public String getKind() { return kind; }

    /** Dedicated model selected for plan mode. */
    @JsonProperty("planModel")
    private String planModel;

    /** User-facing confirmation message for the plan-model selection. */
    @JsonProperty("message")
    private String message;

    /** Whether command execution changed persisted runtime settings. */
    @JsonProperty("runtimeSettingsChanged")
    private Boolean runtimeSettingsChanged;

    public String getPlanModel() { return planModel; }
    public void setPlanModel(String planModel) { this.planModel = planModel; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Boolean getRuntimeSettingsChanged() { return runtimeSettingsChanged; }
    public void setRuntimeSettingsChanged(Boolean runtimeSettingsChanged) { this.runtimeSettingsChanged = runtimeSettingsChanged; }


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
        private String planModel;
        private String message;
        private Boolean runtimeSettingsChanged;

        /**
         * Sets the {@code planModel} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder planModel(String value) {
            this.planModel = value;
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
         * Sets the {@code runtimeSettingsChanged} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder runtimeSettingsChanged(Boolean value) {
            this.runtimeSettingsChanged = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SlashCommandInvocationResultSetPlanModel build() {
            if (message == null) throw new IllegalStateException("Missing required property: message");
            SlashCommandInvocationResultSetPlanModel instance = new SlashCommandInvocationResultSetPlanModel();
            instance.setPlanModel(planModel);
            instance.setMessage(message);
            instance.setRuntimeSettingsChanged(runtimeSettingsChanged);
            return instance;
        }
    }
}

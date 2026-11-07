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
 * Outcome of a session mode change, including any model switch it triggered and follow-up the host must perform.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionModeSetResult(
    /** Lifecycle status of the requested mode change. */
    @JsonProperty("status") String status,
    /** Whether applying the mode changed the active model. */
    @JsonProperty("modelChanged") Boolean modelChanged,
    /** Whether the requested mode was applied to the session. False only when an 'expectedMode' precondition did not hold, in which case any model change reported alongside it was still applied. */
    @JsonProperty("modeApplied") Boolean modeApplied,
    /** Compaction confirmation required before the mode change can complete. */
    @JsonProperty("confirmation") ModelSwitchConfirmation confirmation,
    /** User-facing warning produced while applying the mode change. */
    @JsonProperty("warning") String warning,
    /** User-facing outcome message for the model switch triggered by the mode change. */
    @JsonProperty("message") String message,
    /** Deprecation warnings associated with the model selected by the mode change. */
    @JsonProperty("deprecationWarnings") List<String> deprecationWarnings,
    /** Whether the host must defer implementing the requested mode change. */
    @JsonProperty("deferImplementation") Boolean deferImplementation,
    /** Whether the host should arm an interactive continuation after the mode change. */
    @JsonProperty("armInteractiveContinuation") Boolean armInteractiveContinuation
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionModeSetResult {}

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
        private String status;
        private Boolean modelChanged;
        private Boolean modeApplied;
        private ModelSwitchConfirmation confirmation;
        private String warning;
        private String message;
        private List<String> deprecationWarnings;
        private Boolean deferImplementation;
        private Boolean armInteractiveContinuation;

        /**
         * Sets the {@code status} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder status(String value) {
            this.status = value;
            return this;
        }

        /**
         * Sets the {@code modelChanged} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder modelChanged(Boolean value) {
            this.modelChanged = value;
            return this;
        }

        /**
         * Sets the {@code modeApplied} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder modeApplied(Boolean value) {
            this.modeApplied = value;
            return this;
        }

        /**
         * Sets the {@code confirmation} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder confirmation(ModelSwitchConfirmation value) {
            this.confirmation = value;
            return this;
        }

        /**
         * Sets the {@code warning} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder warning(String value) {
            this.warning = value;
            return this;
        }

        /**
         * Sets the {@code message} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder message(String value) {
            this.message = value;
            return this;
        }

        /**
         * Sets the {@code deprecationWarnings} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder deprecationWarnings(List<String> value) {
            this.deprecationWarnings = value;
            return this;
        }

        /**
         * Sets the {@code deferImplementation} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder deferImplementation(Boolean value) {
            this.deferImplementation = value;
            return this;
        }

        /**
         * Sets the {@code armInteractiveContinuation} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder armInteractiveContinuation(Boolean value) {
            this.armInteractiveContinuation = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionModeSetResult build() {
            if (status == null) throw new IllegalStateException("Missing required property: status");
            if (modelChanged == null) throw new IllegalStateException("Missing required property: modelChanged");
            return new SessionModeSetResult(status, modelChanged, modeApplied, confirmation, warning, message, deprecationWarnings, deferImplementation, armInteractiveContinuation);
        }
    }
}

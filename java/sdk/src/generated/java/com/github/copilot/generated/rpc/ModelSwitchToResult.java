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
 * The model identifier active on the session after the switch.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelSwitchToResult(
    /** Stable queue item identifier when this request was enqueued. Remains present if the item drains before the response is returned. */
    @JsonProperty("queueId") String queueId,
    /** Currently active model identifier after the switch */
    @JsonProperty("modelId") String modelId,
    /** True when the switch was deferred (enqueued as a cancellable `/model` command) because a turn was active or another model change was already queued, rather than applied immediately. When true, the session's live model is unchanged until the queued change drains. */
    @JsonProperty("deferred") Boolean deferred,
    /** Lifecycle result for the requested switch */
    @JsonProperty("status") String status,
    /** Compaction confirmation projection when status is confirmation_required */
    @JsonProperty("confirmation") ModelSwitchConfirmation confirmation,
    /** Persistence failure encountered after applying the model switch. */
    @JsonProperty("persistenceError") String persistenceError,
    /** User-facing outcome message for the model switch. */
    @JsonProperty("message") String message,
    /** User-facing warning produced while applying the model switch. */
    @JsonProperty("warning") String warning,
    /** Deprecation warnings associated with the selected model or options. */
    @JsonProperty("deprecationWarnings") List<String> deprecationWarnings,
    /** Authoritative model and Auto preference state after an immediate switch. For deferred switches this remains the current state until the queued change drains. */
    @JsonProperty("modelState") CurrentModel modelState
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ModelSwitchToResult {}

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
        private String queueId;
        private String modelId;
        private Boolean deferred;
        private String status;
        private ModelSwitchConfirmation confirmation;
        private String persistenceError;
        private String message;
        private String warning;
        private List<String> deprecationWarnings;
        private CurrentModel modelState;

        /**
         * Sets the {@code queueId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder queueId(String value) {
            this.queueId = value;
            return this;
        }

        /**
         * Sets the {@code modelId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder modelId(String value) {
            this.modelId = value;
            return this;
        }

        /**
         * Sets the {@code deferred} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder deferred(Boolean value) {
            this.deferred = value;
            return this;
        }

        /**
         * Sets the {@code status} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder status(String value) {
            this.status = value;
            return this;
        }

        /**
         * Sets the {@code confirmation} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder confirmation(ModelSwitchConfirmation value) {
            this.confirmation = value;
            return this;
        }

        /**
         * Sets the {@code persistenceError} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder persistenceError(String value) {
            this.persistenceError = value;
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
         * Sets the {@code warning} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder warning(String value) {
            this.warning = value;
            return this;
        }

        /**
         * Sets the {@code deprecationWarnings} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder deprecationWarnings(List<String> value) {
            this.deprecationWarnings = value;
            return this;
        }

        /**
         * Sets the {@code modelState} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder modelState(CurrentModel value) {
            this.modelState = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ModelSwitchToResult build() {
            return new ModelSwitchToResult(queueId, modelId, deferred, status, confirmation, persistenceError, message, warning, deprecationWarnings, modelState);
        }
    }
}

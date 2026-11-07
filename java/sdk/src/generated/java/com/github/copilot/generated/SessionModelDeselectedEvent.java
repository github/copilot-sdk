/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Session event "session.model_deselected". The model the user had explicitly selected is no longer available, because the host that published it withdrew it, so the session no longer has an explicit selection. The next turn resolves a default as though the user had never chosen a model. Clients should stop presenting the previous model as selected. This event is durable because resume rebuilds the selected model from the event log; without it a resumed session would restore a model its provider no longer serves. Reasoning effort, verbosity, and other session-level preferences are deliberately unchanged, because they belong to the session rather than to the model.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionModelDeselectedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.model_deselected"; }

    @JsonProperty("data")
    private SessionModelDeselectedEventData data;

    public SessionModelDeselectedEventData getData() { return data; }
    public void setData(SessionModelDeselectedEventData data) { this.data = data; }

    /** Data payload for {@link SessionModelDeselectedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionModelDeselectedEventData(
        /** Model that was selected before the host withdrew it. */
        @JsonProperty("previousModel") String previousModel,
        /** Low-cardinality reason the selection was cleared. */
        @JsonProperty("reason") ModelDeselectedReason reason
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionModelDeselectedEventData {}

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
            private String previousModel;
            private ModelDeselectedReason reason;

            /**
             * Sets the {@code previousModel} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder previousModel(String value) {
                this.previousModel = value;
                return this;
            }

            /**
             * Sets the {@code reason} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder reason(ModelDeselectedReason value) {
                this.reason = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionModelDeselectedEventData build() {
                if (previousModel == null) throw new IllegalStateException("Missing required property: previousModel");
                if (reason == null) throw new IllegalStateException("Missing required property: reason");
                return new SessionModelDeselectedEventData(previousModel, reason);
            }
        }
    }
}

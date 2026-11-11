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
 * Session event "ide.elicitation_complete". An IDE elicitation completion notification.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class IdeElicitationCompleteEvent extends SessionEvent {

    @Override
    public String getType() { return "ide.elicitation_complete"; }

    @JsonProperty("data")
    private IdeElicitationCompleteEventData data;

    public IdeElicitationCompleteEventData getData() { return data; }
    public void setData(IdeElicitationCompleteEventData data) { this.data = data; }

    /** Data payload for {@link IdeElicitationCompleteEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record IdeElicitationCompleteEventData(
        /** Elicitation identifier returned by the IDE. */
        @JsonProperty("elicitationId") String elicitationId
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public IdeElicitationCompleteEventData {}

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
            private String elicitationId;

            /**
             * Sets the {@code elicitationId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder elicitationId(String value) {
                this.elicitationId = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public IdeElicitationCompleteEventData build() {
                if (elicitationId == null) throw new IllegalStateException("Missing required property: elicitationId");
                return new IdeElicitationCompleteEventData(elicitationId);
            }
        }
    }
}

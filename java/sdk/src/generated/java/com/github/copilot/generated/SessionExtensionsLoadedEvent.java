/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Session event "session.extensions_loaded". Payload of `session.extensions_loaded` listing discovered extensions and their statuses.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionExtensionsLoadedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.extensions_loaded"; }

    @JsonProperty("data")
    private SessionExtensionsLoadedEventData data;

    public SessionExtensionsLoadedEventData getData() { return data; }
    public void setData(SessionExtensionsLoadedEventData data) { this.data = data; }

    /** Data payload for {@link SessionExtensionsLoadedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionExtensionsLoadedEventData(
        /** Array of discovered extensions and their status */
        @JsonProperty("extensions") List<ExtensionsLoadedExtension> extensions
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionExtensionsLoadedEventData {}

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
            private List<ExtensionsLoadedExtension> extensions;

            /**
             * Sets the {@code extensions} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder extensions(List<ExtensionsLoadedExtension> value) {
                this.extensions = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionExtensionsLoadedEventData build() {
                if (extensions == null) throw new IllegalStateException("Missing required property: extensions");
                return new SessionExtensionsLoadedEventData(extensions);
            }
        }
    }
}

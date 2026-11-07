/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Session event "session.custom_notification". Opaque custom notification data. Consumers may branch on source and name, but payload semantics are source-defined.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionCustomNotificationEvent extends SessionEvent {

    @Override
    public String getType() { return "session.custom_notification"; }

    @JsonProperty("data")
    private SessionCustomNotificationEventData data;

    public SessionCustomNotificationEventData getData() { return data; }
    public void setData(SessionCustomNotificationEventData data) { this.data = data; }

    /** Data payload for {@link SessionCustomNotificationEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionCustomNotificationEventData(
        /** Namespace for the custom notification producer */
        @JsonProperty("source") String source,
        /** Source-defined custom notification name */
        @JsonProperty("name") String name,
        /** Optional source-defined payload schema version */
        @JsonProperty("version") Long version,
        /** Optional source-defined string identifiers describing the payload subject */
        @JsonProperty("subject") Map<String, String> subject,
        /** Source-defined JSON payload for the custom notification */
        @JsonProperty("payload") Object payload
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionCustomNotificationEventData {}

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
            private String source;
            private String name;
            private Long version;
            private Map<String, String> subject;
            private Object payload;
            private boolean $presence4;

            /**
             * Sets the {@code source} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder source(String value) {
                this.source = value;
                return this;
            }

            /**
             * Sets the {@code name} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder name(String value) {
                this.name = value;
                return this;
            }

            /**
             * Sets the {@code version} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder version(Long value) {
                this.version = value;
                return this;
            }

            /**
             * Sets the {@code subject} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder subject(Map<String, String> value) {
                this.subject = value;
                return this;
            }

            /**
             * Sets the {@code payload} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder payload(Object value) {
                this.payload = value;
                this.$presence4 = true;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionCustomNotificationEventData build() {
                if (source == null) throw new IllegalStateException("Missing required property: source");
                if (name == null) throw new IllegalStateException("Missing required property: name");
                if (!$presence4) throw new IllegalStateException("Missing required property: payload");
                return new SessionCustomNotificationEventData(source, name, version, subject, payload);
            }
        }
    }
}

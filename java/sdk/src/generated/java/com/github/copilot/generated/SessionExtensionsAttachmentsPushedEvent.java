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
 * Session event "session.extensions.attachments_pushed". Payload of `session.extensions.attachments_pushed` with extension-contributed attachments for the next send.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionExtensionsAttachmentsPushedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.extensions.attachments_pushed"; }

    @JsonProperty("data")
    private SessionExtensionsAttachmentsPushedEventData data;

    public SessionExtensionsAttachmentsPushedEventData getData() { return data; }
    public void setData(SessionExtensionsAttachmentsPushedEventData data) { this.data = data; }

    /** Data payload for {@link SessionExtensionsAttachmentsPushedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionExtensionsAttachmentsPushedEventData(
        /** Attachments contributed by an extension; the host should surface these as composer pills and forward them via the next session.send call. */
        @JsonProperty("attachments") List<Object> attachments
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionExtensionsAttachmentsPushedEventData {}

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
            private List<Object> attachments;

            /**
             * Sets the {@code attachments} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder attachments(List<Object> value) {
                this.attachments = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionExtensionsAttachmentsPushedEventData build() {
                if (attachments == null) throw new IllegalStateException("Missing required property: attachments");
                return new SessionExtensionsAttachmentsPushedEventData(attachments);
            }
        }
    }
}

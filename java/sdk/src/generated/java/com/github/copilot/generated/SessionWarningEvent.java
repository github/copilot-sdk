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
 * Session event "session.warning". Warning message for timeline display with categorization
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionWarningEvent extends SessionEvent {

    @Override
    public String getType() { return "session.warning"; }

    @JsonProperty("data")
    private SessionWarningEventData data;

    public SessionWarningEventData getData() { return data; }
    public void setData(SessionWarningEventData data) { this.data = data; }

    /** Data payload for {@link SessionWarningEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionWarningEventData(
        /** Category of warning (e.g., "subscription", "policy", "mcp") */
        @JsonProperty("warningType") String warningType,
        /** Human-readable warning message for display in the timeline */
        @JsonProperty("message") String message,
        /** Logical ID returned by session.send for the request this warning concerns. Set for policy-blocked prompts, which do not emit a user.message event. */
        @JsonProperty("originatingMessageId") String originatingMessageId,
        /** Optional URL associated with this warning that the user can open in a browser */
        @JsonProperty("url") String url,
        /** What the user must do to recover, when the runtime knows of an action. The `message` never names a client affordance, so a client that offers one — a slash command, a settings pane, a link — renders it from this value. */
        @JsonProperty("remediation") RemediationAction remediation
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionWarningEventData {}

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
            private String warningType;
            private String message;
            private String originatingMessageId;
            private String url;
            private RemediationAction remediation;

            /**
             * Sets the {@code warningType} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder warningType(String value) {
                this.warningType = value;
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
             * Sets the {@code originatingMessageId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder originatingMessageId(String value) {
                this.originatingMessageId = value;
                return this;
            }

            /**
             * Sets the {@code url} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder url(String value) {
                this.url = value;
                return this;
            }

            /**
             * Sets the {@code remediation} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder remediation(RemediationAction value) {
                this.remediation = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionWarningEventData build() {
                if (warningType == null) throw new IllegalStateException("Missing required property: warningType");
                if (message == null) throw new IllegalStateException("Missing required property: message");
                return new SessionWarningEventData(warningType, message, originatingMessageId, url, remediation);
            }
        }
    }
}

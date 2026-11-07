/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Session event "session.permissions_changed". Permission-mode transition details.
 *
 * @apiNote This event type is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionPermissionsChangedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.permissions_changed"; }

    @JsonProperty("data")
    private SessionPermissionsChangedEventData data;

    public SessionPermissionsChangedEventData getData() { return data; }
    public void setData(SessionPermissionsChangedEventData data) { this.data = data; }

    /** Data payload for {@link SessionPermissionsChangedEvent}. */
    @CopilotExperimental
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionPermissionsChangedEventData(
        /** Permission mode before the change */
        @CopilotExperimental
        @JsonProperty("previousMode") PermissionMode previousMode,
        /** Permission mode after the change */
        @CopilotExperimental
        @JsonProperty("mode") PermissionMode mode,
        /** Explicit LLM judge model override used by assisted mode; omitted when the provider default applies */
        @CopilotExperimental
        @JsonProperty("assistedApprovalModel") String assistedApprovalModel
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionPermissionsChangedEventData {}

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
            private PermissionMode previousMode;
            private PermissionMode mode;
            private String assistedApprovalModel;

            /**
             * Sets the {@code previousMode} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder previousMode(PermissionMode value) {
                this.previousMode = value;
                return this;
            }

            /**
             * Sets the {@code mode} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder mode(PermissionMode value) {
                this.mode = value;
                return this;
            }

            /**
             * Sets the {@code assistedApprovalModel} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder assistedApprovalModel(String value) {
                this.assistedApprovalModel = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionPermissionsChangedEventData build() {
                return new SessionPermissionsChangedEventData(previousMode, mode, assistedApprovalModel);
            }
        }
    }
}

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
 * Session event "session.managed_settings_enforced". Runtime enforcement of enterprise managed settings: fires when the session blocks or caps a runtime action because enterprise policy governs it, so SDK clients can explain *why* an action was governed. Unlike `session.managed_settings_resolved` (which reports *what* is managed), this reports a concrete governed action — e.g. a user or host tried to turn on bypass permissions or Assisted Permissions while the corresponding policy disables it. Emitted live (not persisted to the session event log) on user/host-initiated attempts only, never for silent policy application. Marked experimental while the managed-settings surface stabilizes.
 *
 * @apiNote This event type is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionManagedSettingsEnforcedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.managed_settings_enforced"; }

    @JsonProperty("data")
    private SessionManagedSettingsEnforcedEventData data;

    public SessionManagedSettingsEnforcedEventData getData() { return data; }
    public void setData(SessionManagedSettingsEnforcedEventData data) { this.data = data; }

    /** Data payload for {@link SessionManagedSettingsEnforcedEvent}. */
    @CopilotExperimental
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionManagedSettingsEnforcedEventData(
        /** The category of runtime action that managed policy governed. */
        @JsonProperty("action") ManagedSettingsEnforcedAction action,
        /** For a `bypass_permissions_blocked` action, which permission-mode or escalation primitive was refused. Absent for actions without a specific primitive. */
        @JsonProperty("escalation") ManagedSettingsEnforcedEscalation escalation,
        /** The managed setting key responsible for the enforcement (for example `permissions.disableBypassPermissionsMode` or `permissions.disableAssistedPermissionsMode`). */
        @JsonProperty("setting") String setting,
        /** Whether the enforcement was forced by fail-closed handling (managed policy could not be determined) rather than an explicit managed setting. When true, `setting` still names the restriction that was applied. */
        @JsonProperty("failClosed") Boolean failClosed,
        /** A human-readable explanation of why the action was governed, suitable for surfacing to the user. */
        @JsonProperty("message") String message
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionManagedSettingsEnforcedEventData {}

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
            private ManagedSettingsEnforcedAction action;
            private ManagedSettingsEnforcedEscalation escalation;
            private String setting;
            private Boolean failClosed;
            private String message;

            /**
             * Sets the {@code action} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder action(ManagedSettingsEnforcedAction value) {
                this.action = value;
                return this;
            }

            /**
             * Sets the {@code escalation} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder escalation(ManagedSettingsEnforcedEscalation value) {
                this.escalation = value;
                return this;
            }

            /**
             * Sets the {@code setting} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder setting(String value) {
                this.setting = value;
                return this;
            }

            /**
             * Sets the {@code failClosed} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder failClosed(Boolean value) {
                this.failClosed = value;
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
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionManagedSettingsEnforcedEventData build() {
                if (action == null) throw new IllegalStateException("Missing required property: action");
                if (setting == null) throw new IllegalStateException("Missing required property: setting");
                if (failClosed == null) throw new IllegalStateException("Missing required property: failClosed");
                if (message == null) throw new IllegalStateException("Missing required property: message");
                return new SessionManagedSettingsEnforcedEventData(action, escalation, setting, failClosed, message);
            }
        }
    }
}

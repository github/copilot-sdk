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
import javax.annotation.processing.Generated;

/**
 * Registers an in-process Auto-switch responder and suppresses server-bridge dispatch to avoid competing responders for the same requestId. When requested, also enrolls the responder for active-request Auto recovery and acknowledges support. Attach the actual listener separately through standard event subscriptions before starting a turn, and release the handle when the responder is removed.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionUiRegisterDirectAutoModeSwitchHandlerResult(
    /** Opaque handle representing the registration. Pass this same handle to `unregisterDirectAutoModeSwitchHandler` when the in-process handler is no longer active. Multiple registrations are reference-counted; the server bridge will only dispatch auto-mode-switch requests when no handles are active. */
    @JsonProperty("handle") String handle,
    /** Positive acknowledgment of recovery-aware enrollment. Absence on an older runtime means recovery is unsupported. */
    @JsonProperty("autoModeRecovery") Boolean autoModeRecovery
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionUiRegisterDirectAutoModeSwitchHandlerResult {}

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
        private String handle;
        private Boolean autoModeRecovery;

        /**
         * Sets the {@code handle} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder handle(String value) {
            this.handle = value;
            return this;
        }

        /**
         * Sets the {@code autoModeRecovery} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder autoModeRecovery(Boolean value) {
            this.autoModeRecovery = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionUiRegisterDirectAutoModeSwitchHandlerResult build() {
            if (handle == null) throw new IllegalStateException("Missing required property: handle");
            return new SessionUiRegisterDirectAutoModeSwitchHandlerResult(handle, autoModeRecovery);
        }
    }

    /**
     * Creates a direct Auto-switch registration result without recovery acknowledgment.
     * @deprecated Use named construction via {@link #builder()}.
     * @param handle Opaque handle representing the registration. Pass this same handle to `unregisterDirectAutoModeSwitchHandler` when the in-process handler is no longer active. Multiple registrations are reference-counted; the server bridge will only dispatch auto-mode-switch requests when no handles are active.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionUiRegisterDirectAutoModeSwitchHandlerResult(
        String handle
    ) {
        this(handle, null);
    }
}

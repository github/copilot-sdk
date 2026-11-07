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
 * Indicates whether the handle was active and the registration count was decremented.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionUiUnregisterDirectAutoModeSwitchHandlerResult(
    /** True if the handle was active and decremented the counter; false if the handle was unknown. */
    @JsonProperty("unregistered") Boolean unregistered
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionUiUnregisterDirectAutoModeSwitchHandlerResult {}

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
        private Boolean unregistered;

        /**
         * Sets the {@code unregistered} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder unregistered(Boolean value) {
            this.unregistered = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionUiUnregisterDirectAutoModeSwitchHandlerResult build() {
            if (unregistered == null) throw new IllegalStateException("Missing required property: unregistered");
            return new SessionUiUnregisterDirectAutoModeSwitchHandlerResult(unregistered);
        }
    }
}

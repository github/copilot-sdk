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
 * The sharing status a cloud task carries after the call. Read `credentialResolved` first: when it is false no request was attempted at all, which is a different outcome from a request the service declined.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record CloudTasksSharingStatusResult(
    /** Whether a credential could be resolved for the supplied `authInfo` at all. When false no request was attempted and every other field is absent, which a caller must not report as a transport failure: the account simply has no usable credential. */
    @JsonProperty("credentialResolved") Boolean credentialResolved,
    /** Whether the service returned the task successfully. Meaningful only when `credentialResolved` is true. */
    @JsonProperty("ok") Boolean ok,
    /** The task's sharing status, as the service reported it. Absent when the call did not succeed, and also when the service returned a task that carries no sharing status at all. It is a free-form string rather than a closed set because the service owns these values; a caller that only understands some of them should treat an unrecognized one as unknown rather than as an error. */
    @JsonProperty("sharingStatus") String sharingStatus
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    CloudTasksSharingStatusResult {}

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
        private Boolean credentialResolved;
        private Boolean ok;
        private String sharingStatus;

        /**
         * Sets the {@code credentialResolved} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder credentialResolved(Boolean value) {
            this.credentialResolved = value;
            return this;
        }

        /**
         * Sets the {@code ok} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder ok(Boolean value) {
            this.ok = value;
            return this;
        }

        /**
         * Sets the {@code sharingStatus} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder sharingStatus(String value) {
            this.sharingStatus = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CloudTasksSharingStatusResult build() {
            if (credentialResolved == null) throw new IllegalStateException("Missing required property: credentialResolved");
            if (ok == null) throw new IllegalStateException("Missing required property: ok");
            return new CloudTasksSharingStatusResult(credentialResolved, ok, sharingStatus);
        }
    }
}

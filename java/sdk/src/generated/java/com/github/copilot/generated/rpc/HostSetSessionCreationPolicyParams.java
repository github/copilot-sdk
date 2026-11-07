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
 * Changes remote session creation admission without disconnecting clients.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record HostSetSessionCreationPolicyParams(
    /** Listener UUID owned by the calling SDK connection. */
    @JsonProperty("hostId") String hostId,
    /** After acknowledgement, subsequent remote creation admissions use this policy. Already-admitted requests may finish. */
    @JsonProperty("allowCreatingNewSessions") Boolean allowCreatingNewSessions
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public HostSetSessionCreationPolicyParams {}

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
        private String hostId;
        private Boolean allowCreatingNewSessions;

        /**
         * Sets the {@code hostId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder hostId(String value) {
            this.hostId = value;
            return this;
        }

        /**
         * Sets the {@code allowCreatingNewSessions} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder allowCreatingNewSessions(Boolean value) {
            this.allowCreatingNewSessions = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public HostSetSessionCreationPolicyParams build() {
            if (hostId == null) throw new IllegalStateException("Missing required property: hostId");
            if (allowCreatingNewSessions == null) throw new IllegalStateException("Missing required property: allowCreatingNewSessions");
            return new HostSetSessionCreationPolicyParams(hostId, allowCreatingNewSessions);
        }
    }
}

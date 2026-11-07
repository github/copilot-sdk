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
 * Pending permission request ID and the decision to apply (approve/reject and scope).
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionPermissionsHandlePendingPermissionRequestParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Request ID of the pending permission request */
    @JsonProperty("requestId") String requestId,
    /** The client's response to the pending permission prompt */
    @JsonProperty("result") Object result,
    /** Optional informational context describing how and where this response was made. Omit it to preserve legacy behavior without attributing an origin. */
    @JsonProperty("decisionContext") PermissionDecisionContext decisionContext
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionPermissionsHandlePendingPermissionRequestParams {}

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
        private String sessionId;
        private String requestId;
        private Object result;
        private PermissionDecisionContext decisionContext;

        /**
         * Sets the {@code sessionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder sessionId(String value) {
            this.sessionId = value;
            return this;
        }

        /**
         * Sets the {@code requestId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder requestId(String value) {
            this.requestId = value;
            return this;
        }

        /**
         * Sets the {@code result} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder result(Object value) {
            this.result = value;
            return this;
        }

        /**
         * Sets the {@code decisionContext} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder decisionContext(PermissionDecisionContext value) {
            this.decisionContext = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionPermissionsHandlePendingPermissionRequestParams build() {
            if (requestId == null) throw new IllegalStateException("Missing required property: requestId");
            if (result == null) throw new IllegalStateException("Missing required property: result");
            return new SessionPermissionsHandlePendingPermissionRequestParams(sessionId, requestId, result, decisionContext);
        }
    }
}

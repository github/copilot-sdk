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
import com.github.copilot.generated.AssistedApprovalAssessment;
import javax.annotation.processing.Generated;

/**
 * Pending permission prompt reconstructed from event history, with request ID and user-facing prompt details.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PendingPermissionRequest(
    /** Unique identifier for the pending permission request */
    @JsonProperty("requestId") String requestId,
    /** The user-facing permission prompt details (commands, write, read, mcp, url, memory, custom-tool, path, hook) */
    @JsonProperty("request") Object request,
    /** Canonical runtime-owned Assisted Permissions assessment captured on the originating permission request. Absent for historical events. */
    @CopilotExperimental
    @JsonProperty("assistedApprovalAssessment") AssistedApprovalAssessment assistedApprovalAssessment,
    /** Permission-recovery episode that authorized this request to surface for interactive attention */
    @JsonProperty("recoveryEpisodeId") String recoveryEpisodeId
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public PendingPermissionRequest {}

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
        private String requestId;
        private Object request;
        private AssistedApprovalAssessment assistedApprovalAssessment;
        private String recoveryEpisodeId;

        /**
         * Sets the {@code requestId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder requestId(String value) {
            this.requestId = value;
            return this;
        }

        /**
         * Sets the {@code request} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder request(Object value) {
            this.request = value;
            return this;
        }

        /**
         * Sets the {@code assistedApprovalAssessment} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder assistedApprovalAssessment(AssistedApprovalAssessment value) {
            this.assistedApprovalAssessment = value;
            return this;
        }

        /**
         * Sets the {@code recoveryEpisodeId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder recoveryEpisodeId(String value) {
            this.recoveryEpisodeId = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public PendingPermissionRequest build() {
            if (requestId == null) throw new IllegalStateException("Missing required property: requestId");
            if (request == null) throw new IllegalStateException("Missing required property: request");
            return new PendingPermissionRequest(requestId, request, assistedApprovalAssessment, recoveryEpisodeId);
        }
    }

    /**
     * Retains the constructor published in Java SDK 1.0.28.
     * @deprecated Use named construction via {@link #builder()}.
     * @param requestId Unique identifier for the pending permission request
     * @param request The user-facing permission prompt details (commands, write, read, mcp, url, memory, custom-tool, path, hook)
     * @param recoveryEpisodeId Permission-recovery episode that authorized this request to surface for interactive attention
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public PendingPermissionRequest(
        String requestId,
        Object request,
        String recoveryEpisodeId
    ) {
        this(requestId, request, null, recoveryEpisodeId);
    }
}

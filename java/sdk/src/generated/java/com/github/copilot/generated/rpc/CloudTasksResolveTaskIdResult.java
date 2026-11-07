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
 * What a caller should open for the task. The four outcomes are distinct and carry different payloads, so read `kind` before any other field.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record CloudTasksResolveTaskIdResult(
    /** Outcome of the resolution: `local-session` when a persisted session matched, `remote-session` when only a cloud run did, `unresumable-task` when the task exists but cannot be continued, `none` when nothing matched, and `error` when the service call failed. */
    @JsonProperty("kind") String kind,
    /** Identifier of the matched local session. Present only when `kind` is `local-session`. */
    @JsonProperty("sessionId") String sessionId,
    /** The cloud session record, serialized, describing the run to open. Present only when `kind` is `remote-session`. It is carried as text because its shape is the service's own and the runtime does not narrow it. */
    @JsonProperty("metadataJson") String metadataJson,
    /** Why the cloud lookup produced less than it could have, for logging. It accompanies a successful outcome as well, so it is not a failure signal. */
    @JsonProperty("diagnostic") String diagnostic,
    /** Human-readable explanation of a failed service call. Present only when `kind` is `error`. */
    @JsonProperty("errorMessage") String errorMessage,
    /** HTTP status code of the failed call, when one was received. */
    @JsonProperty("errorStatusCode") Long errorStatusCode,
    /** Machine-readable classification of the failure, when the service supplied one. The vocabulary is the service's and grows with it, so treat an unrecognized value as a generic failure. */
    @JsonProperty("errorKind") String errorKind,
    /** The error body the service returned, preserved verbatim so a host can surface the service's own message. */
    @JsonProperty("errorPayloadJson") String errorPayloadJson
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    CloudTasksResolveTaskIdResult {}

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
        private String kind;
        private String sessionId;
        private String metadataJson;
        private String diagnostic;
        private String errorMessage;
        private Long errorStatusCode;
        private String errorKind;
        private String errorPayloadJson;

        /**
         * Sets the {@code kind} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder kind(String value) {
            this.kind = value;
            return this;
        }

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
         * Sets the {@code metadataJson} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder metadataJson(String value) {
            this.metadataJson = value;
            return this;
        }

        /**
         * Sets the {@code diagnostic} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder diagnostic(String value) {
            this.diagnostic = value;
            return this;
        }

        /**
         * Sets the {@code errorMessage} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder errorMessage(String value) {
            this.errorMessage = value;
            return this;
        }

        /**
         * Sets the {@code errorStatusCode} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder errorStatusCode(Long value) {
            this.errorStatusCode = value;
            return this;
        }

        /**
         * Sets the {@code errorKind} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder errorKind(String value) {
            this.errorKind = value;
            return this;
        }

        /**
         * Sets the {@code errorPayloadJson} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder errorPayloadJson(String value) {
            this.errorPayloadJson = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CloudTasksResolveTaskIdResult build() {
            if (kind == null) throw new IllegalStateException("Missing required property: kind");
            return new CloudTasksResolveTaskIdResult(kind, sessionId, metadataJson, diagnostic, errorMessage, errorStatusCode, errorKind, errorPayloadJson);
        }
    }
}

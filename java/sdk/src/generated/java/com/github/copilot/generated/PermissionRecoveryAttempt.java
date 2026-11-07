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

@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PermissionRecoveryAttempt(
    /** Unique identifier for this attempt record */
    @JsonProperty("attemptId") String attemptId,
    /** Tool-call identifier associated with this attempt, when available */
    @JsonProperty("toolCallId") String toolCallId,
    /** Controlled permission request kind, such as shell, path, URL, or tool */
    @JsonProperty("permissionKind") String permissionKind,
    /** SHA-256 fingerprint of normalized request data; raw permission arguments are not included */
    @JsonProperty("requestFingerprint") String requestFingerprint,
    /** Relationship between this attempt and earlier attempts in the episode */
    @JsonProperty("relation") PermissionRecoveryAttemptRelation relation,
    /** How the runtime handled this attempt */
    @JsonProperty("disposition") PermissionRecoveryAttemptDisposition disposition,
    /** Controlled reason for the attempt disposition */
    @JsonProperty("reason") PermissionRecoveryAttemptReason reason,
    /** One-based position of this attempt in the episode */
    @JsonProperty("ordinal") Long ordinal
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public PermissionRecoveryAttempt {}

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
        private String attemptId;
        private String toolCallId;
        private String permissionKind;
        private String requestFingerprint;
        private PermissionRecoveryAttemptRelation relation;
        private PermissionRecoveryAttemptDisposition disposition;
        private PermissionRecoveryAttemptReason reason;
        private Long ordinal;

        /**
         * Sets the {@code attemptId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder attemptId(String value) {
            this.attemptId = value;
            return this;
        }

        /**
         * Sets the {@code toolCallId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder toolCallId(String value) {
            this.toolCallId = value;
            return this;
        }

        /**
         * Sets the {@code permissionKind} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder permissionKind(String value) {
            this.permissionKind = value;
            return this;
        }

        /**
         * Sets the {@code requestFingerprint} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder requestFingerprint(String value) {
            this.requestFingerprint = value;
            return this;
        }

        /**
         * Sets the {@code relation} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder relation(PermissionRecoveryAttemptRelation value) {
            this.relation = value;
            return this;
        }

        /**
         * Sets the {@code disposition} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder disposition(PermissionRecoveryAttemptDisposition value) {
            this.disposition = value;
            return this;
        }

        /**
         * Sets the {@code reason} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder reason(PermissionRecoveryAttemptReason value) {
            this.reason = value;
            return this;
        }

        /**
         * Sets the {@code ordinal} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder ordinal(Long value) {
            this.ordinal = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public PermissionRecoveryAttempt build() {
            if (attemptId == null) throw new IllegalStateException("Missing required property: attemptId");
            if (permissionKind == null) throw new IllegalStateException("Missing required property: permissionKind");
            if (requestFingerprint == null) throw new IllegalStateException("Missing required property: requestFingerprint");
            if (relation == null) throw new IllegalStateException("Missing required property: relation");
            if (disposition == null) throw new IllegalStateException("Missing required property: disposition");
            if (reason == null) throw new IllegalStateException("Missing required property: reason");
            if (ordinal == null) throw new IllegalStateException("Missing required property: ordinal");
            return new PermissionRecoveryAttempt(attemptId, toolCallId, permissionKind, requestFingerprint, relation, disposition, reason, ordinal);
        }
    }
}

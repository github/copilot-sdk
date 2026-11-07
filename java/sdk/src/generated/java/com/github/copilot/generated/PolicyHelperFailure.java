/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * The latest unresolved Policy Helper failure for this policy context. Helper-authored text is bounded display-intended content but remains untrusted; render it as plain text, keep diagnostics secondary, and do not automatically log, persist, export, execute, or inject it into model context.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PolicyHelperFailure(
    /** Stable runtime classification. Known values are helper-reported, registration-undetermined, working-directory, resolve, resolve-task, spawn, timeout, exit-status, output-too-large, envelope, schema, and internal. This is not a closed set; clients must tolerate unknown values. */
    @JsonProperty("category") String category,
    /** Effective runtime behavior: ignore continues with remaining or retained policy, while failClosed activates restrictive safeguards. This is runtime-owned, not helper-authored. */
    @JsonProperty("failureMode") String failureMode,
    /** Runtime-calculated presentation severity: warning for ignore and error for failClosed. Error severity does not imply that session creation failed. */
    @JsonProperty("severity") String severity,
    /** Invocation reason. Known values are sessionStart and pluginActivation; clients must tolerate future values. */
    @JsonProperty("reason") String reason,
    /** Managed authority that registered the helper, when known. Known values are device and server; clients must tolerate future values. */
    @JsonProperty("authority") String authority,
    /** True when an earlier successful helper-derived layer still contributes to this effective snapshot. This does not promise that every relaxation from the old layer was retained. */
    @JsonProperty("previousPolicyRetained") Boolean previousPolicyRetained,
    /** Stable bounded helper-defined identifier for a valid structured helper error. Absent for runtime-detected failures and not globally unique. */
    @JsonProperty("code") String code,
    /** Bounded plain-text primary explanation. Runtime-authored for runtime failures and helper-authored for a valid structured helper error. */
    @JsonProperty("message") String message,
    /** Optional bounded helper-authored plain-text recovery guidance. It is explanatory and must not be executed automatically. */
    @JsonProperty("remediation") String remediation,
    /** Optional explicit validated additional-information link. URLs embedded in other text are not links. */
    @JsonProperty("helpLink") PolicyHelperHelpLink helpLink,
    /** Optional bounded field-specific findings supplied by the helper. */
    @JsonProperty("details") List<PolicyHelperFailureDetail> details,
    /** Optional bounded untrusted secondary troubleshooting text, such as trimmed stderr. Do not use it as primary UI copy. */
    @JsonProperty("diagnostic") String diagnostic
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public PolicyHelperFailure {}

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
        private String category;
        private String failureMode;
        private String severity;
        private String reason;
        private String authority;
        private Boolean previousPolicyRetained;
        private String code;
        private String message;
        private String remediation;
        private PolicyHelperHelpLink helpLink;
        private List<PolicyHelperFailureDetail> details;
        private String diagnostic;

        /**
         * Sets the {@code category} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder category(String value) {
            this.category = value;
            return this;
        }

        /**
         * Sets the {@code failureMode} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder failureMode(String value) {
            this.failureMode = value;
            return this;
        }

        /**
         * Sets the {@code severity} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder severity(String value) {
            this.severity = value;
            return this;
        }

        /**
         * Sets the {@code reason} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder reason(String value) {
            this.reason = value;
            return this;
        }

        /**
         * Sets the {@code authority} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder authority(String value) {
            this.authority = value;
            return this;
        }

        /**
         * Sets the {@code previousPolicyRetained} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder previousPolicyRetained(Boolean value) {
            this.previousPolicyRetained = value;
            return this;
        }

        /**
         * Sets the {@code code} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder code(String value) {
            this.code = value;
            return this;
        }

        /**
         * Sets the {@code message} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder message(String value) {
            this.message = value;
            return this;
        }

        /**
         * Sets the {@code remediation} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder remediation(String value) {
            this.remediation = value;
            return this;
        }

        /**
         * Sets the {@code helpLink} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder helpLink(PolicyHelperHelpLink value) {
            this.helpLink = value;
            return this;
        }

        /**
         * Sets the {@code details} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder details(List<PolicyHelperFailureDetail> value) {
            this.details = value;
            return this;
        }

        /**
         * Sets the {@code diagnostic} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder diagnostic(String value) {
            this.diagnostic = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public PolicyHelperFailure build() {
            if (category == null) throw new IllegalStateException("Missing required property: category");
            if (failureMode == null) throw new IllegalStateException("Missing required property: failureMode");
            if (severity == null) throw new IllegalStateException("Missing required property: severity");
            if (reason == null) throw new IllegalStateException("Missing required property: reason");
            if (previousPolicyRetained == null) throw new IllegalStateException("Missing required property: previousPolicyRetained");
            if (message == null) throw new IllegalStateException("Missing required property: message");
            return new PolicyHelperFailure(category, failureMode, severity, reason, authority, previousPolicyRetained, code, message, remediation, helpLink, details, diagnostic);
        }
    }
}

/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * The verdict of the `forceLoginOrgs` enterprise sign-in pin for one credential. A managed device may pin Copilot to an approved set of GitHub organizations so a developer cannot sign in with a personal — or another enterprise's — account and escape every control that keys off the signed-in identity. A host must refuse to proceed when `allowed` is false; the remaining fields explain the refusal rather than qualifying it.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record ForceLoginOrgsDecision(
    /** Whether the client may proceed. True whenever no pin is in force, so an unmanaged host is never blocked. */
    @JsonProperty("allowed") Boolean allowed,
    /** Whether a pin applies at all. Distinguishes "allowed because unmanaged" from "allowed because the account is approved", which a diagnostics surface needs and which also decides whether a bring-your-own-key provider may serve inference. */
    @JsonProperty("enforced") Boolean enforced,
    /** The approved organization logins, as configured. Enterprise configuration rather than user data, so it is safe to name in a message or a log line. */
    @JsonProperty("requiredOrgs") List<String> requiredOrgs,
    /** The approved organization the account matched, when allowed under a pin. */
    @JsonProperty("matchedOrg") String matchedOrg,
    /** A stable kebab-case reason code when refused: `org-not-approved`, `unverifiable-credential`, `policy-undetermined`, `policy-invalid`, `not-signed-in`, or `membership-unknown`. Omitted when allowed. Treat an unrecognized code as a refusal rather than mapping it to an allow: a code this host has not been taught about still denied the credential. */
    @JsonProperty("denial") String denial,
    /** A developer-facing explanation of the refusal. Omitted when allowed. */
    @JsonProperty("message") String message,
    /** Error-level detail from managed-policy discovery — which file failed to load, which field failed validation. Carried on the decision rather than left to the log because a refusal can exit the process before a log sink exists, which would discard the only actionable diagnostic a misconfigured managed device produces. */
    @JsonProperty("policyDiagnostics") List<String> policyDiagnostics
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    ForceLoginOrgsDecision {}

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
        private Boolean allowed;
        private Boolean enforced;
        private List<String> requiredOrgs;
        private String matchedOrg;
        private String denial;
        private String message;
        private List<String> policyDiagnostics;

        /**
         * Sets the {@code allowed} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder allowed(Boolean value) {
            this.allowed = value;
            return this;
        }

        /**
         * Sets the {@code enforced} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder enforced(Boolean value) {
            this.enforced = value;
            return this;
        }

        /**
         * Sets the {@code requiredOrgs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder requiredOrgs(List<String> value) {
            this.requiredOrgs = value;
            return this;
        }

        /**
         * Sets the {@code matchedOrg} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder matchedOrg(String value) {
            this.matchedOrg = value;
            return this;
        }

        /**
         * Sets the {@code denial} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder denial(String value) {
            this.denial = value;
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
         * Sets the {@code policyDiagnostics} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder policyDiagnostics(List<String> value) {
            this.policyDiagnostics = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ForceLoginOrgsDecision build() {
            if (allowed == null) throw new IllegalStateException("Missing required property: allowed");
            if (enforced == null) throw new IllegalStateException("Missing required property: enforced");
            if (requiredOrgs == null) throw new IllegalStateException("Missing required property: requiredOrgs");
            if (policyDiagnostics == null) throw new IllegalStateException("Missing required property: policyDiagnostics");
            return new ForceLoginOrgsDecision(allowed, enforced, requiredOrgs, matchedOrg, denial, message, policyDiagnostics);
        }
    }
}

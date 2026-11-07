/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Authentication-info input variant for GitHub-internal HMAC auth, carrying the public GitHub host and HMAC secret.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class HMACAuthInfo extends AuthInfo {

    @JsonProperty("type")
    private final String type = "hmac";

    @Override
    public String getType() { return type; }

    /** Authentication host. HMAC auth always targets the public GitHub host. */
    @JsonProperty("host")
    private String host;

    /** HMAC secret used to sign requests. */
    @JsonProperty("hmac")
    private String hmac;

    /** Snapshot of the authenticated user's Copilot subscription info, if known. Mirrors the GitHub API `/copilot_internal/v2/token` user response shape — the runtime trusts this verbatim and does not re-fetch when set. */
    @JsonProperty("copilotUser")
    private CopilotUserResponse copilotUser;

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public String getHmac() { return hmac; }
    public void setHmac(String hmac) { this.hmac = hmac; }

    public CopilotUserResponse getCopilotUser() { return copilotUser; }
    public void setCopilotUser(CopilotUserResponse copilotUser) { this.copilotUser = copilotUser; }


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
        private String host;
        private String hmac;
        private CopilotUserResponse copilotUser;

        /**
         * Sets the {@code host} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder host(String value) {
            this.host = value;
            return this;
        }

        /**
         * Sets the {@code hmac} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder hmac(String value) {
            this.hmac = value;
            return this;
        }

        /**
         * Sets the {@code copilotUser} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder copilotUser(CopilotUserResponse value) {
            this.copilotUser = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public HMACAuthInfo build() {
            if (host == null) throw new IllegalStateException("Missing required property: host");
            if (hmac == null) throw new IllegalStateException("Missing required property: hmac");
            HMACAuthInfo instance = new HMACAuthInfo();
            instance.setHost(host);
            instance.setHmac(hmac);
            instance.setCopilotUser(copilotUser);
            return instance;
        }
    }
}

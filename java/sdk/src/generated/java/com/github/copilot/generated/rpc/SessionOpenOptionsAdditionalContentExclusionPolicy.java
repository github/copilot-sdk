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
 * Content-exclusion policy supplied to `sessions.open` options, with rules, last-updated data, and scope.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionOpenOptionsAdditionalContentExclusionPolicy(
    /** Content-exclusion rules to apply. */
    @JsonProperty("rules") List<SessionOpenOptionsAdditionalContentExclusionPolicyRule> rules,
    /** Opaque policy update timestamp supplied by the host. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("last_updated_at") Object lastUpdatedAt,
    /** Allowed values for the `SessionOpenOptionsAdditionalContentExclusionPolicyScope` enumeration. */
    @JsonProperty("scope") SessionOpenOptionsAdditionalContentExclusionPolicyScope scope
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionOpenOptionsAdditionalContentExclusionPolicy {}

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
        private List<SessionOpenOptionsAdditionalContentExclusionPolicyRule> rules;
        private Object lastUpdatedAt;
        private boolean $presence1;
        private SessionOpenOptionsAdditionalContentExclusionPolicyScope scope;

        /**
         * Sets the {@code rules} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder rules(List<SessionOpenOptionsAdditionalContentExclusionPolicyRule> value) {
            this.rules = value;
            return this;
        }

        /**
         * Sets the {@code last_updated_at} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder lastUpdatedAt(Object value) {
            this.lastUpdatedAt = value;
            this.$presence1 = true;
            return this;
        }

        /**
         * Sets the {@code scope} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder scope(SessionOpenOptionsAdditionalContentExclusionPolicyScope value) {
            this.scope = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionOpenOptionsAdditionalContentExclusionPolicy build() {
            if (rules == null) throw new IllegalStateException("Missing required property: rules");
            if (!$presence1) throw new IllegalStateException("Missing required property: last_updated_at");
            if (scope == null) throw new IllegalStateException("Missing required property: scope");
            return new SessionOpenOptionsAdditionalContentExclusionPolicy(rules, lastUpdatedAt, scope);
        }
    }
}

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
 * If specified, replaces the session's approved/denied permission rules. Omit to leave the current rules unchanged.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PermissionRulesSet(
    /** Rules that auto-approve matching requests */
    @JsonProperty("approved") List<PermissionRule> approved,
    /** Rules that auto-deny matching requests */
    @JsonProperty("denied") List<PermissionRule> denied
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public PermissionRulesSet {}

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
        private List<PermissionRule> approved;
        private List<PermissionRule> denied;

        /**
         * Sets the {@code approved} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder approved(List<PermissionRule> value) {
            this.approved = value;
            return this;
        }

        /**
         * Sets the {@code denied} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder denied(List<PermissionRule> value) {
            this.denied = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public PermissionRulesSet build() {
            if (approved == null) throw new IllegalStateException("Missing required property: approved");
            if (denied == null) throw new IllegalStateException("Missing required property: denied");
            return new PermissionRulesSet(approved, denied);
        }
    }
}

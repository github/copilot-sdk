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
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Quota usage snapshots for the resolved user, keyed by quota type.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AccountGetQuotaResult(
    /** Quota snapshots keyed by type (e.g., chat, completions, premium_interactions) */
    @JsonProperty("quotaSnapshots") Map<String, AccountQuotaSnapshot> quotaSnapshots
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public AccountGetQuotaResult {}

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
        private Map<String, AccountQuotaSnapshot> quotaSnapshots;

        /**
         * Sets the {@code quotaSnapshots} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder quotaSnapshots(Map<String, AccountQuotaSnapshot> value) {
            this.quotaSnapshots = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AccountGetQuotaResult build() {
            if (quotaSnapshots == null) throw new IllegalStateException("Missing required property: quotaSnapshots");
            return new AccountGetQuotaResult(quotaSnapshots);
        }
    }
}

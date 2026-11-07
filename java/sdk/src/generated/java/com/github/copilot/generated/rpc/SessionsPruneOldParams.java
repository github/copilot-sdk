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
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Age threshold and optional flags controlling which old sessions are pruned (or simulated when dryRun is true).
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionsPruneOldParams(
    /** Delete sessions whose modifiedTime is at least this many days old */
    @JsonProperty("olderThanDays") Long olderThanDays,
    /** When true, only report what would be deleted without performing any deletion */
    @JsonProperty("dryRun") Boolean dryRun,
    /** When true, named sessions (set via /rename) are also eligible for pruning */
    @JsonProperty("includeNamed") Boolean includeNamed,
    /** Session IDs that should never be considered for pruning */
    @JsonProperty("excludeSessionIds") List<String> excludeSessionIds
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionsPruneOldParams {}

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
        private Long olderThanDays;
        private Boolean dryRun;
        private Boolean includeNamed;
        private List<String> excludeSessionIds;

        /**
         * Sets the {@code olderThanDays} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder olderThanDays(Long value) {
            this.olderThanDays = value;
            return this;
        }

        /**
         * Sets the {@code dryRun} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder dryRun(Boolean value) {
            this.dryRun = value;
            return this;
        }

        /**
         * Sets the {@code includeNamed} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder includeNamed(Boolean value) {
            this.includeNamed = value;
            return this;
        }

        /**
         * Sets the {@code excludeSessionIds} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder excludeSessionIds(List<String> value) {
            this.excludeSessionIds = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionsPruneOldParams build() {
            if (olderThanDays == null) throw new IllegalStateException("Missing required property: olderThanDays");
            return new SessionsPruneOldParams(olderThanDays, dryRun, includeNamed, excludeSessionIds);
        }
    }
}

/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Recovery completed and inventory was inspected.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public record SkillInstallationManagementOutcomeRecovered(
    /** Owned Skill installation summaries after recovery. */
    @JsonProperty("installations") List<SkillInstallationSummary> installations,
    /** Skill installation management outcome discriminator. */
    @JsonProperty("kind") String kind
) implements SkillInstallationManagementOutcome {
    /**
     * Creates this variant positionally and normalizes its discriminator.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SkillInstallationManagementOutcomeRecovered {
        kind = "recovered";
    }

    /**
     * Creates this variant without explicitly supplying its discriminator.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SkillInstallationManagementOutcomeRecovered(
        List<SkillInstallationSummary> installations
    ) {
        this(installations, "recovered");
    }

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
        private List<SkillInstallationSummary> installations;

        /**
         * Sets the {@code installations} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder installations(List<SkillInstallationSummary> value) {
            this.installations = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SkillInstallationManagementOutcomeRecovered build() {
            if (installations == null) throw new IllegalStateException("Missing required property: installations");
            return new SkillInstallationManagementOutcomeRecovered(installations, "recovered");
        }
    }
}

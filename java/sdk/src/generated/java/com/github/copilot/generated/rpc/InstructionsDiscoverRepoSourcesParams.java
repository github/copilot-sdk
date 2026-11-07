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
 * Explicit discovery arguments for one location, supplied by the caller rather than derived from a session.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record InstructionsDiscoverRepoSourcesParams(
    /** Repository location to discover. An empty string selects no repository scope; cwdPath can still supply working-directory sources. */
    @JsonProperty("location") String location,
    /** When true, include the sources a command-line host reads. Hosts that are not the CLI omit it. */
    @JsonProperty("isCli") Boolean isCli,
    /** Working directory whose instruction sources should also be included. Omission adds no separate working-directory scope. */
    @JsonProperty("cwdPath") String cwdPath,
    /** Copilot configuration directory for user-level sources. An omitted or empty value uses the server's configuration home. */
    @JsonProperty("configDir") String configDir,
    /** Extra directories to scan alongside instruction directories configured in the runtime environment. */
    @JsonProperty("additionalDirs") List<String> additionalDirs,
    /** When true, also discover instruction files nested below the location. */
    @JsonProperty("enableChildInstructions") Boolean enableChildInstructions
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    InstructionsDiscoverRepoSourcesParams {}

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
        private String location;
        private Boolean isCli;
        private String cwdPath;
        private String configDir;
        private List<String> additionalDirs;
        private Boolean enableChildInstructions;

        /**
         * Sets the {@code location} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder location(String value) {
            this.location = value;
            return this;
        }

        /**
         * Sets the {@code isCli} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder isCli(Boolean value) {
            this.isCli = value;
            return this;
        }

        /**
         * Sets the {@code cwdPath} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder cwdPath(String value) {
            this.cwdPath = value;
            return this;
        }

        /**
         * Sets the {@code configDir} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder configDir(String value) {
            this.configDir = value;
            return this;
        }

        /**
         * Sets the {@code additionalDirs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder additionalDirs(List<String> value) {
            this.additionalDirs = value;
            return this;
        }

        /**
         * Sets the {@code enableChildInstructions} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder enableChildInstructions(Boolean value) {
            this.enableChildInstructions = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public InstructionsDiscoverRepoSourcesParams build() {
            if (location == null) throw new IllegalStateException("Missing required property: location");
            return new InstructionsDiscoverRepoSourcesParams(location, isCli, cwdPath, configDir, additionalDirs, enableChildInstructions);
        }
    }
}

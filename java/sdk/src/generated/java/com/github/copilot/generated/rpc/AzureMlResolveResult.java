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
 * Resolved artifact facts without signed download URLs.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AzureMlResolveResult(
    /** Exact bytes across all files in this version. */
    @JsonProperty("totalBytes") Long totalBytes,
    /** GGUF path relative to the version directory. */
    @JsonProperty("primaryPath") String primaryPath,
    /** Other paths relative to the version directory. */
    @JsonProperty("extraFiles") List<String> extraFiles,
    /** Validated local runtime settings. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("serveOptions") Object serveOptions,
    /** Maximum supported local concurrency. */
    @JsonProperty("maxConcurrent") Long maxConcurrent
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public AzureMlResolveResult {}

    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static final class Builder {
        private Builder() {}
        private Long totalBytes;
        private String primaryPath;
        private List<String> extraFiles;
        private Object serveOptions;
        private boolean $presence3;
        private Long maxConcurrent;

        /**
         * Sets the {@code totalBytes} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder totalBytes(Long value) {
            this.totalBytes = value;
            return this;
        }

        /**
         * Sets the {@code primaryPath} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder primaryPath(String value) {
            this.primaryPath = value;
            return this;
        }

        /**
         * Sets the {@code extraFiles} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder extraFiles(List<String> value) {
            this.extraFiles = value;
            return this;
        }

        /**
         * Sets the {@code serveOptions} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder serveOptions(Object value) {
            this.serveOptions = value;
            this.$presence3 = true;
            return this;
        }

        /**
         * Sets the {@code maxConcurrent} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder maxConcurrent(Long value) {
            this.maxConcurrent = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AzureMlResolveResult build() {
            if (totalBytes == null) throw new IllegalStateException("Missing required property: totalBytes");
            if (primaryPath == null) throw new IllegalStateException("Missing required property: primaryPath");
            if (extraFiles == null) throw new IllegalStateException("Missing required property: extraFiles");
            if (!$presence3) throw new IllegalStateException("Missing required property: serveOptions");
            if (maxConcurrent == null) throw new IllegalStateException("Missing required property: maxConcurrent");
            return new AzureMlResolveResult(totalBytes, primaryPath, extraFiles, serveOptions, maxConcurrent);
        }
    }
}

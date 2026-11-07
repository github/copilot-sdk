/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * One language server in the runtime's configuration inventory.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record LspServerSummary(
    /** Configured server identifier. */
    @JsonProperty("id") String id,
    /** File suffixes handled by the server. */
    @JsonProperty("fileExtensions") Map<String, String> fileExtensions,
    /** Project root relative to the repository root. */
    @JsonProperty("rootUri") String rootUri,
    /** Request deadline in milliseconds. */
    @JsonProperty("requestTimeoutMs") Double requestTimeoutMs,
    /** Protocol initialization deadline in milliseconds. */
    @JsonProperty("initializationTimeoutMs") Double initializationTimeoutMs,
    /** Process startup deadline in milliseconds. */
    @JsonProperty("spawnTimeoutMs") Double spawnTimeoutMs,
    /** Warmup deadline in milliseconds. */
    @JsonProperty("warmupTimeoutMs") Double warmupTimeoutMs,
    /** Plugin that supplies the server, when applicable. */
    @JsonProperty("sourcePlugin") String sourcePlugin,
    /** Whether the effective configuration disables this entry. */
    @JsonProperty("disabled") Boolean disabled,
    /** Platform-selected launch script for a user entry. */
    @JsonProperty("launchDescription") String launchDescription
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public LspServerSummary {}

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
        private String id;
        private Map<String, String> fileExtensions;
        private String rootUri;
        private Double requestTimeoutMs;
        private Double initializationTimeoutMs;
        private Double spawnTimeoutMs;
        private Double warmupTimeoutMs;
        private String sourcePlugin;
        private Boolean disabled;
        private String launchDescription;

        /**
         * Sets the {@code id} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder id(String value) {
            this.id = value;
            return this;
        }

        /**
         * Sets the {@code fileExtensions} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder fileExtensions(Map<String, String> value) {
            this.fileExtensions = value;
            return this;
        }

        /**
         * Sets the {@code rootUri} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder rootUri(String value) {
            this.rootUri = value;
            return this;
        }

        /**
         * Sets the {@code requestTimeoutMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder requestTimeoutMs(Double value) {
            this.requestTimeoutMs = value;
            return this;
        }

        /**
         * Sets the {@code initializationTimeoutMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder initializationTimeoutMs(Double value) {
            this.initializationTimeoutMs = value;
            return this;
        }

        /**
         * Sets the {@code spawnTimeoutMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder spawnTimeoutMs(Double value) {
            this.spawnTimeoutMs = value;
            return this;
        }

        /**
         * Sets the {@code warmupTimeoutMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder warmupTimeoutMs(Double value) {
            this.warmupTimeoutMs = value;
            return this;
        }

        /**
         * Sets the {@code sourcePlugin} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sourcePlugin(String value) {
            this.sourcePlugin = value;
            return this;
        }

        /**
         * Sets the {@code disabled} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder disabled(Boolean value) {
            this.disabled = value;
            return this;
        }

        /**
         * Sets the {@code launchDescription} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder launchDescription(String value) {
            this.launchDescription = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public LspServerSummary build() {
            if (id == null) throw new IllegalStateException("Missing required property: id");
            if (fileExtensions == null) throw new IllegalStateException("Missing required property: fileExtensions");
            if (disabled == null) throw new IllegalStateException("Missing required property: disabled");
            return new LspServerSummary(id, fileExtensions, rootUri, requestTimeoutMs, initializationTimeoutMs, spawnTimeoutMs, warmupTimeoutMs, sourcePlugin, disabled, launchDescription);
        }
    }
}

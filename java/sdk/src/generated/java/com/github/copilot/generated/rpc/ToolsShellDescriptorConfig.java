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
 * Shell-specific names and description lines used to materialize built-in shell tool descriptors.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ToolsShellDescriptorConfig(
    /** Stable shell type identifier. */
    @JsonProperty("shellType") String shellType,
    /** Human-readable shell name. */
    @JsonProperty("displayName") String displayName,
    /** Tool name used to start shell commands. */
    @JsonProperty("shellToolName") String shellToolName,
    /** Tool name used to read shell output. */
    @JsonProperty("readShellToolName") String readShellToolName,
    /** Tool name used to stop shell commands. */
    @JsonProperty("stopShellToolName") String stopShellToolName,
    /** Tool name used to list active shells. */
    @JsonProperty("listShellsToolName") String listShellsToolName,
    /** Additional model-facing shell description lines. */
    @JsonProperty("descriptionLines") List<String> descriptionLines
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ToolsShellDescriptorConfig {}

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
        private String shellType;
        private String displayName;
        private String shellToolName;
        private String readShellToolName;
        private String stopShellToolName;
        private String listShellsToolName;
        private List<String> descriptionLines;

        /**
         * Sets the {@code shellType} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder shellType(String value) {
            this.shellType = value;
            return this;
        }

        /**
         * Sets the {@code displayName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder displayName(String value) {
            this.displayName = value;
            return this;
        }

        /**
         * Sets the {@code shellToolName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder shellToolName(String value) {
            this.shellToolName = value;
            return this;
        }

        /**
         * Sets the {@code readShellToolName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder readShellToolName(String value) {
            this.readShellToolName = value;
            return this;
        }

        /**
         * Sets the {@code stopShellToolName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder stopShellToolName(String value) {
            this.stopShellToolName = value;
            return this;
        }

        /**
         * Sets the {@code listShellsToolName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder listShellsToolName(String value) {
            this.listShellsToolName = value;
            return this;
        }

        /**
         * Sets the {@code descriptionLines} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder descriptionLines(List<String> value) {
            this.descriptionLines = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ToolsShellDescriptorConfig build() {
            if (shellType == null) throw new IllegalStateException("Missing required property: shellType");
            if (displayName == null) throw new IllegalStateException("Missing required property: displayName");
            if (shellToolName == null) throw new IllegalStateException("Missing required property: shellToolName");
            if (readShellToolName == null) throw new IllegalStateException("Missing required property: readShellToolName");
            if (stopShellToolName == null) throw new IllegalStateException("Missing required property: stopShellToolName");
            if (listShellsToolName == null) throw new IllegalStateException("Missing required property: listShellsToolName");
            if (descriptionLines == null) throw new IllegalStateException("Missing required property: descriptionLines");
            return new ToolsShellDescriptorConfig(shellType, displayName, shellToolName, readShellToolName, stopShellToolName, listShellsToolName, descriptionLines);
        }
    }
}

/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration for large tool output handling.
 * <p>
 * When a tool produces output exceeding {@link #getMaxSizeBytes()}, the SDK
 * writes the full output to a file in {@link #getOutputDirectory()} and returns
 * a truncated preview to the model.
 *
 * @since 1.3.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LargeToolOutputConfig {

    @JsonProperty("enabled")
    private Boolean enabled;

    @JsonProperty("maxSizeBytes")
    private Long maxSizeBytes;

    @JsonProperty("outputDir")
    private String outputDirectory;

    /**
     * Gets whether large tool output handling is enabled.
     *
     * @return {@code true} if enabled, {@code false} if disabled, {@code null} for
     *         default
     */
    public Boolean getEnabled() {
        return enabled;
    }

    /**
     * Sets whether large tool output handling is enabled. Defaults to {@code true}
     * when unset.
     *
     * @param enabled
     *            {@code true} to enable, {@code false} to disable
     * @return this config for method chaining
     */
    public LargeToolOutputConfig setEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     * Gets the maximum tool output size in bytes before it is redirected to a file.
     *
     * @return the maximum size in bytes, or {@code null} for default
     */
    public Long getMaxSizeBytes() {
        return maxSizeBytes;
    }

    /**
     * Sets the maximum tool output size in bytes before it is redirected to a file.
     *
     * @param maxSizeBytes
     *            the maximum size in bytes
     * @return this config for method chaining
     */
    public LargeToolOutputConfig setMaxSizeBytes(Long maxSizeBytes) {
        this.maxSizeBytes = maxSizeBytes;
        return this;
    }

    /**
     * Gets the directory where large tool output files are written.
     *
     * @return the output directory path, or {@code null} for default
     */
    public String getOutputDirectory() {
        return outputDirectory;
    }

    /**
     * Sets the directory where large tool output files are written.
     *
     * @param outputDirectory
     *            the output directory path
     * @return this config for method chaining
     */
    public LargeToolOutputConfig setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
        return this;
    }
}

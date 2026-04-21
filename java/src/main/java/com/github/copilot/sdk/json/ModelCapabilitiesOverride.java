/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Per-property overrides for model capabilities, deep-merged over runtime
 * defaults.
 * <p>
 * Use this to override specific model capabilities when creating a session or
 * switching models with {@link com.github.copilot.sdk.CopilotSession#setModel}.
 * Only non-null fields are applied; unset fields retain their runtime defaults.
 *
 * <h2>Example: Disable vision for a session</h2>
 *
 * <pre>{@code
 * var config = new SessionConfig().setModel("claude-sonnet-4.5").setModelCapabilities(
 * 		new ModelCapabilitiesOverride().setSupports(new ModelCapabilitiesOverride.Supports().setVision(false)));
 * }</pre>
 *
 * <h2>Example: Override capabilities when switching models</h2>
 *
 * <pre>{@code
 * session.setModel("claude-sonnet-4.5", null,
 * 		new ModelCapabilitiesOverride().setSupports(new ModelCapabilitiesOverride.Supports().setVision(true))).get();
 * }</pre>
 *
 * @see com.github.copilot.sdk.CopilotSession#setModel(String, String,
 *      ModelCapabilitiesOverride)
 * @see SessionConfig#setModelCapabilities(ModelCapabilitiesOverride)
 * @since 1.3.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModelCapabilitiesOverride {

    @JsonProperty("supports")
    private Supports supports;

    @JsonProperty("limits")
    private Limits limits;

    /**
     * Gets the feature flag overrides.
     *
     * @return the supports overrides, or {@code null} if not set
     */
    public Supports getSupports() {
        return supports;
    }

    /**
     * Sets the feature flag overrides.
     *
     * @param supports
     *            the supports overrides
     * @return this instance for method chaining
     */
    public ModelCapabilitiesOverride setSupports(Supports supports) {
        this.supports = supports;
        return this;
    }

    /**
     * Gets the token limit overrides.
     *
     * @return the limits overrides, or {@code null} if not set
     */
    public Limits getLimits() {
        return limits;
    }

    /**
     * Sets the token limit overrides.
     *
     * @param limits
     *            the limits overrides
     * @return this instance for method chaining
     */
    public ModelCapabilitiesOverride setLimits(Limits limits) {
        this.limits = limits;
        return this;
    }

    /**
     * Feature flag overrides for model capabilities.
     * <p>
     * Set a field to {@code true} or {@code false} to override that capability;
     * leave it {@code null} to use the runtime default.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Supports {

        @JsonProperty("vision")
        private Boolean vision;

        @JsonProperty("reasoningEffort")
        private Boolean reasoningEffort;

        /**
         * Gets the vision override.
         *
         * @return {@code true} to enable vision, {@code false} to disable, or
         *         {@code null} to use the runtime default
         */
        public Boolean getVision() {
            return vision;
        }

        /**
         * Sets whether vision (image input) is enabled.
         *
         * @param vision
         *            {@code true} to enable, {@code false} to disable, or {@code null}
         *            to use the runtime default
         * @return this instance for method chaining
         */
        public Supports setVision(Boolean vision) {
            this.vision = vision;
            return this;
        }

        /**
         * Gets the reasoning effort override.
         *
         * @return {@code true} to enable reasoning effort, {@code false} to disable, or
         *         {@code null} to use the runtime default
         */
        public Boolean getReasoningEffort() {
            return reasoningEffort;
        }

        /**
         * Sets whether reasoning effort configuration is enabled.
         *
         * @param reasoningEffort
         *            {@code true} to enable, {@code false} to disable, or {@code null}
         *            to use the runtime default
         * @return this instance for method chaining
         */
        public Supports setReasoningEffort(Boolean reasoningEffort) {
            this.reasoningEffort = reasoningEffort;
            return this;
        }
    }

    /**
     * Token limit overrides for model capabilities.
     * <p>
     * Set a field to override that limit; leave it {@code null} to use the runtime
     * default.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Limits {

        @JsonProperty("max_prompt_tokens")
        private Integer maxPromptTokens;

        @JsonProperty("max_output_tokens")
        private Integer maxOutputTokens;

        @JsonProperty("max_context_window_tokens")
        private Integer maxContextWindowTokens;

        /**
         * Gets the maximum prompt tokens override.
         *
         * @return the override value, or {@code null} to use the runtime default
         */
        public Integer getMaxPromptTokens() {
            return maxPromptTokens;
        }

        /**
         * Sets the maximum number of tokens in a prompt.
         *
         * @param maxPromptTokens
         *            the override value, or {@code null} to use the runtime default
         * @return this instance for method chaining
         */
        public Limits setMaxPromptTokens(Integer maxPromptTokens) {
            this.maxPromptTokens = maxPromptTokens;
            return this;
        }

        /**
         * Gets the maximum output tokens override.
         *
         * @return the override value, or {@code null} to use the runtime default
         */
        public Integer getMaxOutputTokens() {
            return maxOutputTokens;
        }

        /**
         * Sets the maximum number of output tokens.
         *
         * @param maxOutputTokens
         *            the override value, or {@code null} to use the runtime default
         * @return this instance for method chaining
         */
        public Limits setMaxOutputTokens(Integer maxOutputTokens) {
            this.maxOutputTokens = maxOutputTokens;
            return this;
        }

        /**
         * Gets the maximum context window tokens override.
         *
         * @return the override value, or {@code null} to use the runtime default
         */
        public Integer getMaxContextWindowTokens() {
            return maxContextWindowTokens;
        }

        /**
         * Sets the maximum total context window size in tokens.
         *
         * @param maxContextWindowTokens
         *            the override value, or {@code null} to use the runtime default
         * @return this instance for method chaining
         */
        public Limits setMaxContextWindowTokens(Integer maxContextWindowTokens) {
            this.maxContextWindowTokens = maxContextWindowTokens;
            return this;
        }
    }
}

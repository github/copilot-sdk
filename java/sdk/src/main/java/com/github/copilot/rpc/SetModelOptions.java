/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

/**
 * Optional settings for a model switch.
 * <p>
 * All setter methods return {@code this} for method chaining. Every option is
 * optional; an unset option leaves the corresponding session state unchanged.
 *
 * <pre>{@code
 * session.setModel(new SetModelOptions().setModel("auto").setAutoTier(AutoTier.INTELLIGENCE)).get();
 * }</pre>
 *
 * @since 1.6.0
 */
public class SetModelOptions {

    private String model;

    private String reasoningEffort;

    private String reasoningSummary;

    private ModelCapabilitiesOverride modelCapabilities;

    private AutoTier autoTier;

    private boolean resetAutoTier;

    /**
     * Gets the target model ID.
     *
     * @return the model ID, or {@code null} when none has been set
     */
    public String getModel() {
        return model;
    }

    /**
     * Sets the model to switch to. This option is required.
     *
     * @param model
     *            the model ID (e.g., {@code "gpt-5.4"} or {@code "auto"})
     * @return this options object for method chaining
     */
    public SetModelOptions setModel(String model) {
        this.model = model;
        return this;
    }

    /**
     * Gets the reasoning effort level.
     *
     * @return the reasoning effort level, or {@code null} to use the default
     */
    public String getReasoningEffort() {
        return reasoningEffort;
    }

    /**
     * Sets the reasoning effort level.
     *
     * @param reasoningEffort
     *            reasoning effort level (e.g., {@code "low"}, {@code "medium"},
     *            {@code "high"}, {@code "xhigh"}, {@code "max"}); {@code null} to
     *            use the default
     * @return this options object for method chaining
     */
    public SetModelOptions setReasoningEffort(String reasoningEffort) {
        this.reasoningEffort = reasoningEffort;
        return this;
    }

    /**
     * Gets the reasoning summary mode.
     *
     * @return the reasoning summary mode, or {@code null} to use the default
     */
    public String getReasoningSummary() {
        return reasoningSummary;
    }

    /**
     * Sets the reasoning summary mode.
     *
     * @param reasoningSummary
     *            reasoning summary mode ({@code "none"}, {@code "concise"}, or
     *            {@code "detailed"}); {@code null} to use the default
     * @return this options object for method chaining
     */
    public SetModelOptions setReasoningSummary(String reasoningSummary) {
        this.reasoningSummary = reasoningSummary;
        return this;
    }

    /**
     * Gets the model capability overrides.
     *
     * @return the capability overrides, or {@code null} to use runtime defaults
     */
    public ModelCapabilitiesOverride getModelCapabilities() {
        return modelCapabilities;
    }

    /**
     * Sets per-property overrides for model capabilities.
     *
     * @param modelCapabilities
     *            the capability overrides; {@code null} to use runtime defaults
     * @return this options object for method chaining
     */
    public SetModelOptions setModelCapabilities(ModelCapabilitiesOverride modelCapabilities) {
        this.modelCapabilities = modelCapabilities;
        return this;
    }

    /**
     * Gets the requested Auto routing preference.
     *
     * @return the requested tier, or {@code null} when no tier was requested
     */
    public AutoTier getAutoTier() {
        return autoTier;
    }

    /**
     * Requests an Auto routing preference alongside the model switch.
     * <p>
     * The runtime records the request and commits it only when a later user turn
     * using the {@code auto} model successfully obtains a usable model from the
     * provider. Use {@link #setResetAutoTier(boolean)} to return to the provider's
     * default Auto routing instead.
     *
     * @param autoTier
     *            the routing preference to request; {@code null} to leave the
     *            current preference unchanged
     * @return this options object for method chaining
     */
    public SetModelOptions setAutoTier(AutoTier autoTier) {
        this.autoTier = autoTier;
        return this;
    }

    /**
     * Gets whether the request returns to provider-default Auto routing.
     *
     * @return {@code true} when the request clears the Auto routing preference
     */
    public boolean isResetAutoTier() {
        return resetAutoTier;
    }

    /**
     * Requests a return to the provider's default Auto routing.
     * <p>
     * This differs from leaving {@link #setAutoTier(AutoTier)} unset, which keeps
     * the current preference. It cannot be combined with an explicit tier.
     *
     * @param resetAutoTier
     *            {@code true} to return to provider-default Auto routing
     * @return this options object for method chaining
     */
    public SetModelOptions setResetAutoTier(boolean resetAutoTier) {
        this.resetAutoTier = resetAutoTier;
        return this;
    }
}

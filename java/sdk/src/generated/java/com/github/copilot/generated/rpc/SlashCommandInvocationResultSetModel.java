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
 * Variant {@code set-model} of {@link SlashCommandInvocationResult}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SlashCommandInvocationResultSetModel extends SlashCommandInvocationResult {

    @JsonProperty("kind")
    private final String kind = "set-model";

    @Override
    public String getKind() { return kind; }

    /** Model selected by the command. */
    @JsonProperty("model")
    private String model;

    /** Auto routing profile selected by the command, when the model is Auto. */
    @JsonProperty("autoTier")
    private AutoTier autoTier;

    /** Settings scope modified by the command. */
    @JsonProperty("scope")
    private String scope;

    /** User-facing warning produced while selecting the model. */
    @JsonProperty("warning")
    private String warning;

    /** Reasoning effort selected for the model. */
    @JsonProperty("reasoningEffort")
    private String reasoningEffort;

    /** User-settings snapshot to restore if the host cancels the model switch. */
    @JsonProperty("revertOnCancel")
    private Map<String, Object> revertOnCancel;

    /** Repository settings scope modified by the command. */
    @JsonProperty("repoScope")
    private String repoScope;

    /** Whether command execution changed persisted runtime settings. */
    @JsonProperty("runtimeSettingsChanged")
    private Boolean runtimeSettingsChanged;

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public AutoTier getAutoTier() { return autoTier; }
    public void setAutoTier(AutoTier autoTier) { this.autoTier = autoTier; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public String getWarning() { return warning; }
    public void setWarning(String warning) { this.warning = warning; }

    public String getReasoningEffort() { return reasoningEffort; }
    public void setReasoningEffort(String reasoningEffort) { this.reasoningEffort = reasoningEffort; }

    public Map<String, Object> getRevertOnCancel() { return revertOnCancel; }
    public void setRevertOnCancel(Map<String, Object> revertOnCancel) { this.revertOnCancel = revertOnCancel; }

    public String getRepoScope() { return repoScope; }
    public void setRepoScope(String repoScope) { this.repoScope = repoScope; }

    public Boolean getRuntimeSettingsChanged() { return runtimeSettingsChanged; }
    public void setRuntimeSettingsChanged(Boolean runtimeSettingsChanged) { this.runtimeSettingsChanged = runtimeSettingsChanged; }


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
        private String model;
        private AutoTier autoTier;
        private String scope;
        private String warning;
        private String reasoningEffort;
        private Map<String, Object> revertOnCancel;
        private String repoScope;
        private Boolean runtimeSettingsChanged;

        /**
         * Sets the {@code model} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder model(String value) {
            this.model = value;
            return this;
        }

        /**
         * Sets the {@code autoTier} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder autoTier(AutoTier value) {
            this.autoTier = value;
            return this;
        }

        /**
         * Sets the {@code scope} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder scope(String value) {
            this.scope = value;
            return this;
        }

        /**
         * Sets the {@code warning} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder warning(String value) {
            this.warning = value;
            return this;
        }

        /**
         * Sets the {@code reasoningEffort} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder reasoningEffort(String value) {
            this.reasoningEffort = value;
            return this;
        }

        /**
         * Sets the {@code revertOnCancel} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder revertOnCancel(Map<String, Object> value) {
            this.revertOnCancel = value;
            return this;
        }

        /**
         * Sets the {@code repoScope} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder repoScope(String value) {
            this.repoScope = value;
            return this;
        }

        /**
         * Sets the {@code runtimeSettingsChanged} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder runtimeSettingsChanged(Boolean value) {
            this.runtimeSettingsChanged = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SlashCommandInvocationResultSetModel build() {
            if (model == null) throw new IllegalStateException("Missing required property: model");
            SlashCommandInvocationResultSetModel instance = new SlashCommandInvocationResultSetModel();
            instance.setModel(model);
            instance.setAutoTier(autoTier);
            instance.setScope(scope);
            instance.setWarning(warning);
            instance.setReasoningEffort(reasoningEffort);
            instance.setRevertOnCancel(revertOnCancel);
            instance.setRepoScope(repoScope);
            instance.setRuntimeSettingsChanged(runtimeSettingsChanged);
            return instance;
        }
    }
}

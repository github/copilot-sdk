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
}

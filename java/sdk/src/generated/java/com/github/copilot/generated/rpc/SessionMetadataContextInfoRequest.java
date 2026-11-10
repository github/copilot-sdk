/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import java.util.Objects;
import javax.annotation.processing.Generated;

/**
 * Model identifier and token limits used to compute the context-info breakdown.
 * <p>
 * Required inputs are constructor arguments. Optional inputs have fluent setters.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class SessionMetadataContextInfoRequest {

    /** Advertised prompt allowance. Pass 0 to resolve the selected model and context tier from the session. */
    @JsonProperty("promptTokenLimit")
    private final Long promptTokenLimit;

    /** Requested output allowance to reserve against the combined context ceiling. Pass 0 to resolve the session's request cap, falling back to the model's advertised output limit. */
    @JsonProperty("outputTokenLimit")
    private final Long outputTokenLimit;

    /** Model identifier used for tokenization. Omit to use the session default. Used both for token counting and to compute display values. */
    @JsonProperty("selectedModel")
    private String selectedModel;

    /** Provider owning selectedModel. Omit to use the current provider for the current model, or deterministic legacy lookup for a different model. */
    @JsonProperty("providerId")
    private String providerId;

    /** Whether a zero outputTokenLimit may resolve a live provider's request cap. Defaults to true. Set false for a cached-only measurement that must not start a managed provider; capacity values use advertised limits, while occupied token counts are unchanged. */
    @JsonProperty("resolveOutputTokenLimit")
    private Boolean resolveOutputTokenLimit;

    /**
     * Creates a request with its required inputs.
     *
     * @param promptTokenLimit Advertised prompt allowance. Pass 0 to resolve the selected model and context tier from the session.
     * @param outputTokenLimit Requested output allowance to reserve against the combined context ceiling. Pass 0 to resolve the session's request cap, falling back to the model's advertised output limit.
     */
    public SessionMetadataContextInfoRequest(Long promptTokenLimit, Long outputTokenLimit) {
        this.promptTokenLimit = Objects.requireNonNull(promptTokenLimit, "promptTokenLimit");
        this.outputTokenLimit = Objects.requireNonNull(outputTokenLimit, "outputTokenLimit");
    }

    /**
     * Returns the {@code promptTokenLimit} property.
     *
     * @return Advertised prompt allowance. Pass 0 to resolve the selected model and context tier from the session.
     */
    public Long getPromptTokenLimit() {
        return promptTokenLimit;
    }

    /**
     * Returns the {@code outputTokenLimit} property.
     *
     * @return Requested output allowance to reserve against the combined context ceiling. Pass 0 to resolve the session's request cap, falling back to the model's advertised output limit.
     */
    public Long getOutputTokenLimit() {
        return outputTokenLimit;
    }

    /**
     * Returns the {@code selectedModel} property.
     *
     * @return Model identifier used for tokenization. Omit to use the session default. Used both for token counting and to compute display values.
     */
    public String getSelectedModel() {
        return selectedModel;
    }

    /**
     * Returns the {@code providerId} property.
     *
     * @return Provider owning selectedModel. Omit to use the current provider for the current model, or deterministic legacy lookup for a different model.
     */
    public String getProviderId() {
        return providerId;
    }

    /**
     * Returns the {@code resolveOutputTokenLimit} property.
     *
     * @return Whether a zero outputTokenLimit may resolve a live provider's request cap. Defaults to true. Set false for a cached-only measurement that must not start a managed provider; capacity values use advertised limits, while occupied token counts are unchanged.
     */
    public Boolean getResolveOutputTokenLimit() {
        return resolveOutputTokenLimit;
    }

    /**
     * Sets the {@code selectedModel} property.
     *
     * @param value Model identifier used for tokenization. Omit to use the session default. Used both for token counting and to compute display values.
     * @return this request
     */
    public SessionMetadataContextInfoRequest setSelectedModel(String value) {
        this.selectedModel = value;
        return this;
    }

    /**
     * Sets the {@code providerId} property.
     *
     * @param value Provider owning selectedModel. Omit to use the current provider for the current model, or deterministic legacy lookup for a different model.
     * @return this request
     */
    public SessionMetadataContextInfoRequest setProviderId(String value) {
        this.providerId = value;
        return this;
    }

    /**
     * Sets the {@code resolveOutputTokenLimit} property.
     *
     * @param value Whether a zero outputTokenLimit may resolve a live provider's request cap. Defaults to true. Set false for a cached-only measurement that must not start a managed provider; capacity values use advertised limits, while occupied token counts are unchanged.
     * @return this request
     */
    public SessionMetadataContextInfoRequest setResolveOutputTokenLimit(Boolean value) {
        this.resolveOutputTokenLimit = value;
        return this;
    }
}

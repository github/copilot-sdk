/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response from the models.list RPC call.
 * <p>
 * Contains a list of available models with their metadata.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetModelsResponse {

    @JsonProperty("models")
    private List<ModelInfo> models;

    public List<ModelInfo> getModels() {
        return models;
    }

    public GetModelsResponse setModels(List<ModelInfo> models) {
        this.models = models;
        return this;
    }
}

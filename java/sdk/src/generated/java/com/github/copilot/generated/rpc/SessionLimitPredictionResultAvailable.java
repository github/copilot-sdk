/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Variant {@code available} of {@link SessionLimitPredictionResult}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionLimitPredictionResultAvailable extends SessionLimitPredictionResult {

    @JsonProperty("kind")
    private final String kind = "available";

    @Override
    public String getKind() { return kind; }

    /** Predicted session limit details. */
    @JsonProperty("prediction")
    private SessionLimitPredictionDetails prediction;

    public SessionLimitPredictionDetails getPrediction() { return prediction; }
    public void setPrediction(SessionLimitPredictionDetails prediction) { this.prediction = prediction; }
}

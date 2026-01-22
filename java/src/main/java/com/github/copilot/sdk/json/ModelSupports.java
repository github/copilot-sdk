/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model support flags.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModelSupports {

    @JsonProperty("vision")
    private boolean vision;

    public boolean isVision() {
        return vision;
    }

    public ModelSupports setVision(boolean vision) {
        this.vision = vision;
        return this;
    }
}

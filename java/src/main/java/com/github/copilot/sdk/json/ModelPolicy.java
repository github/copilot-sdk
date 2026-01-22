/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model policy state.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModelPolicy {

    @JsonProperty("state")
    private String state;

    @JsonProperty("terms")
    private String terms;

    public String getState() {
        return state;
    }

    public ModelPolicy setState(String state) {
        this.state = state;
        return this;
    }

    public String getTerms() {
        return terms;
    }

    public ModelPolicy setTerms(String terms) {
        this.terms = terms;
        return this;
    }
}

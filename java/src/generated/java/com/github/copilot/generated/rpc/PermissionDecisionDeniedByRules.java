/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Schema for the `PermissionDecisionDeniedByRules` type.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class PermissionDecisionDeniedByRules extends PermissionDecision {

    @JsonProperty("kind")
    private final String kind = "denied-by-rules";

    @Override
    public String getKind() { return kind; }

    /** Rules that denied the request */
    @JsonProperty("rules")
    private List<PermissionRule> rules;

    public List<PermissionRule> getRules() { return rules; }
    public void setRules(List<PermissionRule> rules) { this.rules = rules; }
}

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
 * Single content-exclusion rule supplied to `sessions.open` options, with paths, match conditions, and source.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionOpenOptionsAdditionalContentExclusionPolicyRule(
    @JsonProperty("paths") List<String> paths,
    @JsonProperty("ifAnyMatch") List<String> ifAnyMatch,
    @JsonProperty("ifNoneMatch") List<String> ifNoneMatch,
    /** Source descriptor for a `sessions.open` content-exclusion rule, with source name and type. */
    @JsonProperty("source") SessionOpenOptionsAdditionalContentExclusionPolicyRuleSource source
) {
}

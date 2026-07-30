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
 * Optional filter applied to the returned sessions
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionListFilter(
    /** Match sessions whose context.cwd equals this value */
    @JsonProperty("cwd") String cwd,
    /** Match sessions whose context.gitRoot equals this value */
    @JsonProperty("gitRoot") String gitRoot,
    /** Match sessions whose context.repository equals this value */
    @JsonProperty("repository") String repository,
    /** Match sessions whose context.branch equals this value */
    @JsonProperty("branch") String branch
) {
}

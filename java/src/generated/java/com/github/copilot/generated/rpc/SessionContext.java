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
 * Schema for the `SessionContext` type.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionContext(
    /** Most recent working directory for this session */
    @JsonProperty("cwd") String cwd,
    /** Git repository root, if the cwd was inside a git repo */
    @JsonProperty("gitRoot") String gitRoot,
    /** Repository slug in `owner/name` form, when known */
    @JsonProperty("repository") String repository,
    /** Repository host type */
    @JsonProperty("hostType") SessionContextHostType hostType,
    /** Active git branch */
    @JsonProperty("branch") String branch
) {
}

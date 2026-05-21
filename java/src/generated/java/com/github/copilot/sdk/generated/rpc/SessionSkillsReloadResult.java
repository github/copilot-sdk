/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Diagnostics from reloading skill definitions, with warnings and errors as separate lists.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionSkillsReloadResult(
    /** Warnings emitted while loading skills (e.g. skills that loaded but had issues) */
    @JsonProperty("warnings") List<String> warnings,
    /** Errors emitted while loading skills (e.g. skills that failed to load entirely) */
    @JsonProperty("errors") List<String> errors
) {
}

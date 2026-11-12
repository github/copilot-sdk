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
 * One setting that will not take effect, and why.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record UserSettingsProblem(
    /** Stable identifier for this problem within the reported set, so a caller can track a selection across refreshes. */
    @JsonProperty("id") String id,
    /** What is wrong in machine-readable form, for a caller that wants to branch rather than display. */
    @JsonProperty("kind") String kind,
    /** Coarse grouping for the problem, for a caller that presents problems in sections. */
    @JsonProperty("category") String category,
    /** The settings layer the problem was found in. */
    @JsonProperty("source") UserSettingsProblemLocation source,
    /** The offending key as written in the file, when the problem is attributable to a single key. Absent when the whole file is at fault. */
    @JsonProperty("rawKey") String rawKey,
    /** Why the setting will not take effect. Never contains the setting's value, which may be a token or another secret. */
    @JsonProperty("explanation") String explanation
) {
}

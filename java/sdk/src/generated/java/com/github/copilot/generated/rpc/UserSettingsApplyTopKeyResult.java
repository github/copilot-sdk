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
 * Outcome of validating and writing one top-level setting subtree.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record UserSettingsApplyTopKeyResult(
    /** `ok` when the value was written, `validation-error` when it was rejected before any write, `write-error` when validation passed but the write failed. */
    @JsonProperty("status") String status,
    /** Problems found, present only when `status` is `validation-error`. */
    @JsonProperty("issues") List<UserSettingsValidationIssue> issues,
    /** Why the write failed, present only when `status` is `write-error`. */
    @JsonProperty("error") String error
) {
}

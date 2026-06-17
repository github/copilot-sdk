/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * The list of models available to this session.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionModelListResult(
    /** Available models, ordered with the most preferred default first. Includes both Copilot (CAPI) models and any registry BYOK models; a BYOK model appears under its provider-qualified selection id (`provider/id`). */
    @JsonProperty("list") List<Object> list,
    /** Per-quota snapshots returned alongside the model list, keyed by quota type. */
    @JsonProperty("quotaSnapshots") Map<String, Object> quotaSnapshots
) {
}

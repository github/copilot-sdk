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
import javax.annotation.processing.Generated;

/**
 * The selectable model entries synthesized for the models added by this call.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionProviderAddResult(
    /** Synthesized selectable model entries for the newly added BYOK models, each under its provider-qualified selection id (`provider/id`). Empty when only providers were added. */
    @JsonProperty("models") List<Object> models
) {
}

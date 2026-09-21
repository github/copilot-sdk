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
import javax.annotation.processing.Generated;

/**
 * Terminates one retained catalog selection group through an opaque reference previously returned by the model-safe search projection.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogSelectParams(
    /** Protocol version and capabilities the caller requires. */
    @JsonProperty("contract") CatalogClientContract contract,
    /** Locally owned root session whose retained search state is being resolved. */
    @JsonProperty("sessionId") String sessionId,
    /** Opaque runtime-instance scoped reference to one visible candidate. For a non-selected outcome, any candidate reference from the same search closes that search's retained group. */
    @JsonProperty("selectionRef") String selectionRef,
    /** The terminal outcome declared by the caller. Timed-out means the host's live interaction deadline elapsed; a reference whose runtime TTL elapsed is rejected separately as stale. */
    @JsonProperty("outcome") CatalogSelectionDecision outcome
) {
}

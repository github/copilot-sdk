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
 * One SDK-host source's bounded JSON value for future Assisted Permissions judge requests. Replaces only the named source; a null value clears that source.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PermissionsConfigureAssistedApprovalContext(
    /** Stable host source identifier. It isolates this source from unrelated producers and may contain ASCII letters, digits, dots, underscores, and hyphens. */
    @JsonProperty("source") String source,
    /** Arbitrary bounded JSON context. A null value clears only this source. Runtime framing states that this value is not an instruction, policy decision, human authorization, or permission. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("value") Object value
) {
}

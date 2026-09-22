/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * One persisted structured system-message block and its cache intent
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SystemMessageContentBlock(
    /** Text content for this system-message block. */
    @JsonProperty("content") String content,
    /** Diagnostic classification indicating whether the block is stable across equivalent sessions. */
    @JsonProperty("isStatic") Boolean isStatic,
    /** Explicit prompt-cache intent. True places a breakpoint after this block, false suppresses one, and absence preserves the provider's legacy default. */
    @JsonProperty("cacheBreakpoint") Boolean cacheBreakpoint
) {
}

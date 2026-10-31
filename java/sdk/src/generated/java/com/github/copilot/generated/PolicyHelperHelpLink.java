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
 * An explicit bounded absolute HTTP(S) destination supplied by a Policy Helper. The runtime validates but never fetches, previews, resolves redirects for, or opens it; clients must sanitize it again before navigation.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PolicyHelperHelpLink(
    /** Validated absolute HTTP(S) URL. */
    @JsonProperty("url") String url,
    /** Optional bounded helper-authored plain-text label. Clients may use localized fallback text when absent. */
    @JsonProperty("label") String label
) {
}

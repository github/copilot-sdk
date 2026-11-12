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
 * Current host context
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpAppsHostContextDetails(
    /** UI theme preference per SEP-1865 */
    @JsonProperty("theme") McpAppsHostContextDetailsTheme theme,
    /** BCP-47 locale, e.g. 'en-US' */
    @JsonProperty("locale") String locale,
    /** IANA timezone, e.g. 'America/New_York' */
    @JsonProperty("timeZone") String timeZone,
    /** Current display mode (SEP-1865) */
    @JsonProperty("displayMode") McpAppsHostContextDetailsDisplayMode displayMode,
    /** Display modes the host supports */
    @JsonProperty("availableDisplayModes") List<McpAppsHostContextDetailsAvailableDisplayMode> availableDisplayModes,
    /** Platform type for responsive design */
    @JsonProperty("platform") McpAppsHostContextDetailsPlatform platform,
    /** Host application identifier */
    @JsonProperty("userAgent") String userAgent
) {
}

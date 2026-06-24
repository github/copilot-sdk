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
 * If specified, replaces the session's URL-permission policy. The runtime constructs a fresh DefaultUrlManager based on these inputs. Omit to leave the current URL policy unchanged.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PermissionUrlsConfig(
    /** If true, the runtime allows access to all URLs without prompting. Initial allow-list is ignored when this is true. */
    @JsonProperty("unrestricted") Boolean unrestricted,
    /** Initial list of allowed URL/domain patterns. Patterns may include path components. Ignored when `unrestricted` is true. */
    @JsonProperty("initialAllowed") List<String> initialAllowed
) {
}

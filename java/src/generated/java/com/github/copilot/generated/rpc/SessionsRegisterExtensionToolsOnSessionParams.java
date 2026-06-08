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
 * Params to attach an extension loader's tools to a session.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionsRegisterExtensionToolsOnSessionParams(
    /** Session to register extension tools on. */
    @JsonProperty("sessionId") String sessionId,
    /** In-process ExtensionLoader handle (CLI-only optimization). Marked internal: this field is excluded from the public SDK surface. When the CLI migrates to a process-separated SDK, extension discovery/launch moves entirely into the runtime — the CLI passes pure config (search paths, disabled ids) via SessionOptions instead. */
    @JsonProperty("loader") Object loader,
    /** Optional registration options. */
    @JsonProperty("options") SessionsRegisterExtensionToolsOnSessionOptions options
) {
}

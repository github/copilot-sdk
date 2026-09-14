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
 * Plugin names (or specs) to enable, plus the optional working directory the repository-controlled guard is evaluated against.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PluginsEnableParams(
    /** Plugin names or "plugin@marketplace" specs to enable. Unknown names are ignored. Non-marketplace direct installs are always enabled and cannot be toggled via this API. */
    @JsonProperty("names") List<String> names,
    /** Working directory whose repository `enabledPlugins` overlay decides whether this mutation is repository-controlled. Hosts that serve sessions across several repositories (the SDK server) should pass the session's directory; otherwise the guard is evaluated against the server process's own working directory, which may belong to a different repository. Defaults to the server's current working directory. */
    @JsonProperty("workingDirectory") String workingDirectory
) {
}

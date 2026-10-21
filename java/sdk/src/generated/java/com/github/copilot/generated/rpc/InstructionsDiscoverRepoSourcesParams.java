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
 * Explicit discovery arguments for one location, supplied by the caller rather than derived from a session.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record InstructionsDiscoverRepoSourcesParams(
    /** Repository location to discover. An empty string selects no repository scope; cwdPath can still supply working-directory sources. */
    @JsonProperty("location") String location,
    /** When true, include the sources a command-line host reads. Hosts that are not the CLI omit it. */
    @JsonProperty("isCli") Boolean isCli,
    /** Working directory whose instruction sources should also be included. Omission adds no separate working-directory scope. */
    @JsonProperty("cwdPath") String cwdPath,
    /** Copilot configuration directory for user-level sources. An omitted or empty value uses the server's configuration home. */
    @JsonProperty("configDir") String configDir,
    /** Extra directories to scan alongside instruction directories configured in the runtime environment. */
    @JsonProperty("additionalDirs") List<String> additionalDirs,
    /** When true, also discover instruction files nested below the location. */
    @JsonProperty("enableChildInstructions") Boolean enableChildInstructions
) {
}

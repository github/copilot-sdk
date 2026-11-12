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
 * Where this runtime keeps the sandbox proxy password.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record SandboxProxySecretLocation(
    /** Absolute path of the credential store holding the sandbox proxy password. The proxy password belongs to no MCP server, so it is kept in its own directory rather than in the MCP secret store. */
    @JsonProperty("configDir") String configDir,
    /** Identifier of the proxy password entry within that store. Derived from the configuration directory, so two Copilot homes on one machine never share an entry. Use it to recognize a `${secret:…}` reference in stored settings as this runtime's own proxy password. */
    @JsonProperty("secretId") String secretId
) {
}

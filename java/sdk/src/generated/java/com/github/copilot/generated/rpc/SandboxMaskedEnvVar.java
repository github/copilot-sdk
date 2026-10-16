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
 * Destinations authorized to receive one masked environment credential.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SandboxMaskedEnvVar(
    /** Nonempty list of HTTPS injection hostnames or *.example.com patterns. Bare * is not accepted. These grants never override the sandbox network policy. Values in plaintext HTTP requests, URLs, bodies, encoded credentials, and signed requests are not substituted. */
    @JsonProperty("injectHosts") List<String> injectHosts
) {
}

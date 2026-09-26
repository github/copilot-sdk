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
 * Whether this host can run one sandbox policy feature. A session whose effective policy uses an unsupported feature fails each sandboxed command with `reason`.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SandboxHostCapability(
    /** The policy feature, as an extensible string: ignore names you do not recognize. Known values: `network` (sandboxed commands can reach the network; on Linux this needs the tooling for Bubblewrap's private network namespace, such as slirp4netns), `network_filtering` (host rules and the sandbox proxy; on Linux this needs the same tooling as `network`; on Windows it needs Process Security Environment 1.1 host-loopback support, and a policy that uses it must also set `network.allowLocalNetwork`), `denied_paths` (native enforcement of `filesystem.deniedPaths`), and `shell` (shell commands inside the sandbox; on Windows this needs Process Security Environment 1.1 filesystem enumeration support). */
    @JsonProperty("name") String name,
    /** Whether this host can run the feature. */
    @JsonProperty("supported") Boolean supported,
    /** Human-readable reason and remedy when the feature is unsupported, such as a package to install or an OS update. Present only when `supported` is false. */
    @JsonProperty("reason") String reason
) {
}

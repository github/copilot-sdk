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
 * HTTP proxy configuration for sandboxed traffic.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SandboxConfigUserPolicyNetworkProxy(
    /** Proxy URL (e.g. http://proxy.example.com:8080). The port is optional and defaults to the scheme's standard port when omitted; an explicit port must be between 1 and 65535. Credentials must not be embedded here — a `user:pass@` authority is rejected; put them in the separate `username`/`password` fields. A credential-free http:// loopback proxy URL is routed through the localhost proxy automatically; loopback covers localhost and any *.localhost subdomain, the whole 127.0.0.0/8 range, ::1, and IPv4-mapped loopback (::ffff:127.0.0.1). An https:// URL, or one with a username/password set, is used as-is. */
    @JsonProperty("url") String url,
    /** Optional username for proxy authentication. Combined with the URL (and `password`) into `user:pass@host` when the sandboxed process routes through the proxy. */
    @JsonProperty("username") String username,
    /** Optional password for proxy authentication, combined with the URL at spawn time. The persisted value may be a literal password, a `${secret:…}` reference resolved from the OS keychain, or a `${VAR}`/`$VAR` environment reference; it is resolved just before the sandboxed process routes through the proxy. The /sandbox dialog stores a real password in the OS keychain and persists only a `${secret:…}` placeholder (never plaintext in settings.json); the field is masked in the dialog and redacted by /settings show. */
    @JsonProperty("password") String password
) {
}

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
import javax.annotation.processing.Generated;

/**
 * Versions and provider configuration for CAFE.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record CafeMetricsStartParams(
    /** Effective comparison version and appVersion tag. */
    @JsonProperty("appVersion") String appVersion,
    /** Raw package build version. */
    @JsonProperty("buildVersion") String buildVersion,
    /** Embedded package identity. Omission enables the unbundled development fallback. */
    @JsonProperty("embeddedPackageIdentity") CafeMetricsEmbeddedPackageIdentity embeddedPackageIdentity,
    /** Whether the host uses a custom model provider. */
    @JsonProperty("customProviderConfigured") Boolean customProviderConfigured
) {
}

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
 * Catalogue identity retained from a bound candidate or plan at installation time.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record InstallationCatalogueIdentity(
    /** Authority resource identifier when supplied by the catalogue. */
    @JsonProperty("resourceId") String resourceId,
    /** Catalogue item URL when supplied by the authority. */
    @JsonProperty("itemUrl") String itemUrl,
    /** Human display name retained from the catalogue candidate. */
    @JsonProperty("displayName") String displayName,
    /** Catalogue description retained at install planning time. */
    @JsonProperty("description") String description,
    /** Catalogue publisher retained at install planning time. */
    @JsonProperty("publisher") String publisher,
    /** Catalogue version retained at install planning time. */
    @JsonProperty("version") String version,
    /** Catalogue authority/source string that supplied the candidate. */
    @JsonProperty("source") String source,
    /** Catalogue trust observation retained at install planning time. */
    @JsonProperty("trustAtInstall") CatalogTrustSnapshot trustAtInstall
) {
}

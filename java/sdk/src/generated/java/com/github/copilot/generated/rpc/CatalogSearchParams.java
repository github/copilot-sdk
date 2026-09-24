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
 * A bounded catalog search. Both the query length and the result count are capped by the schema so a caller cannot request an unbounded scan.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogSearchParams(
    /** Protocol version and capabilities the caller requires. */
    @JsonProperty("contract") CatalogClientContract contract,
    /** Select an existing attached local session. Requires authenticated, session-bound search.
The runtime never creates, resumes or reconfigures a session to honour this selector. */
    @JsonProperty("policySessionId") String policySessionId,
    /** Free-text search query. Persisted as tool input for session continuity, but omitted from telemetry. */
    @JsonProperty("query") String query,
    /** Maximum number of candidates to return. Defaults to 10 when omitted. */
    @JsonProperty("limit") Long limit,
    /** Restrict results to these candidate kinds. Agent Plugins are opt-in and require the `agent-plugin-discovery` capability so protocol-v3 clients generated before that variant cannot receive an unknown result; when omitted, the backwards-compatible MCP server and AI skill kinds are searched. */
    @JsonProperty("kinds") List<CatalogCandidateKind> kinds,
    /** Numbered navigation using metadata from an earlier response. Requires catalog-search-pagination and the same query, kinds and effective limit. Omit for a fresh first-page search. */
    @JsonProperty("page") CatalogSearchPage page
) {
}

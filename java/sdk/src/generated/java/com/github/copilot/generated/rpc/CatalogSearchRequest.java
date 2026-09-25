/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import java.util.List;
import java.util.Objects;
import javax.annotation.processing.Generated;

/**
 * A bounded catalog search. Both the query length and the result count are capped by the schema so a caller cannot request an unbounded scan.
 * <p>
 * Required inputs are constructor arguments. Optional inputs have fluent setters.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class CatalogSearchRequest {

    /** Protocol version and capabilities the caller requires. */
    @JsonProperty("contract")
    private final CatalogClientContract contract;

    /** Free-text search query. Persisted as tool input for session continuity, but omitted from telemetry. */
    @JsonProperty("query")
    private final String query;

    /** Maximum number of candidates to return. Defaults to 10 when omitted. */
    @JsonProperty("limit")
    private Long limit;

    /** Restrict results to these candidate kinds. Agent Plugins are opt-in and require the `agent-plugin-discovery` capability so protocol-v3 clients generated before that variant cannot receive an unknown result; when omitted, the backwards-compatible MCP server and AI skill kinds are searched. */
    @JsonProperty("kinds")
    private List<CatalogCandidateKind> kinds;

    /** Numbered navigation using metadata from an earlier response. Requires catalog-search-pagination and the same query, kinds and effective limit. Omit for a fresh first-page search. */
    @JsonProperty("page")
    private CatalogSearchPage page;

    /** Select an existing attached local session. Requires authenticated, session-bound search. The runtime never creates, resumes or reconfigures a session to honour this selector. */
    @JsonProperty("policySessionId")
    private String policySessionId;

    /**
     * Creates a request with its required inputs.
     *
     * @param contract Protocol version and capabilities the caller requires.
     * @param query Free-text search query. Persisted as tool input for session continuity, but omitted from telemetry.
     */
    public CatalogSearchRequest(CatalogClientContract contract, String query) {
        this.contract = Objects.requireNonNull(contract, "contract");
        this.query = Objects.requireNonNull(query, "query");
    }

    /**
     * Returns the {@code contract} property.
     *
     * @return Protocol version and capabilities the caller requires.
     */
    public CatalogClientContract getContract() {
        return contract;
    }

    /**
     * Returns the {@code query} property.
     *
     * @return Free-text search query. Persisted as tool input for session continuity, but omitted from telemetry.
     */
    public String getQuery() {
        return query;
    }

    /**
     * Returns the {@code limit} property.
     *
     * @return Maximum number of candidates to return. Defaults to 10 when omitted.
     */
    public Long getLimit() {
        return limit;
    }

    /**
     * Returns the {@code kinds} property.
     *
     * @return Restrict results to these candidate kinds. Agent Plugins are opt-in and require the `agent-plugin-discovery` capability so protocol-v3 clients generated before that variant cannot receive an unknown result; when omitted, the backwards-compatible MCP server and AI skill kinds are searched.
     */
    public List<CatalogCandidateKind> getKinds() {
        return kinds;
    }

    /**
     * Returns the {@code page} property.
     *
     * @return Numbered navigation using metadata from an earlier response. Requires catalog-search-pagination and the same query, kinds and effective limit. Omit for a fresh first-page search.
     */
    public CatalogSearchPage getPage() {
        return page;
    }

    /**
     * Returns the {@code policySessionId} property.
     *
     * @return Select an existing attached local session. Requires authenticated, session-bound search. The runtime never creates, resumes or reconfigures a session to honour this selector.
     */
    public String getPolicySessionId() {
        return policySessionId;
    }

    /**
     * Sets the {@code limit} property.
     *
     * @param value Maximum number of candidates to return. Defaults to 10 when omitted.
     * @return this request
     */
    public CatalogSearchRequest setLimit(Long value) {
        this.limit = value;
        return this;
    }

    /**
     * Sets the {@code kinds} property.
     *
     * @param value Restrict results to these candidate kinds. Agent Plugins are opt-in and require the `agent-plugin-discovery` capability so protocol-v3 clients generated before that variant cannot receive an unknown result; when omitted, the backwards-compatible MCP server and AI skill kinds are searched.
     * @return this request
     */
    public CatalogSearchRequest setKinds(List<CatalogCandidateKind> value) {
        this.kinds = value;
        return this;
    }

    /**
     * Sets the {@code page} property.
     *
     * @param value Numbered navigation using metadata from an earlier response. Requires catalog-search-pagination and the same query, kinds and effective limit. Omit for a fresh first-page search.
     * @return this request
     */
    public CatalogSearchRequest setPage(CatalogSearchPage value) {
        this.page = value;
        return this;
    }

    /**
     * Sets the {@code policySessionId} property.
     *
     * @param value Select an existing attached local session. Requires authenticated, session-bound search. The runtime never creates, resumes or reconfigures a session to honour this selector.
     * @return this request
     */
    public CatalogSearchRequest setPolicySessionId(String value) {
        this.policySessionId = value;
        return this;
    }
}

/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.generated.rpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class CatalogSearchConformanceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void deserializesTypedCandidatesHandlesSourcesAndRefusals() throws Exception {
        var result = MAPPER.readValue("""
                {
                  "kind": "succeeded",
                  "searchId": "search-1",
                  "candidates": [
                    {
                      "kind": "mcp-server",
                      "handle": "mcp-handle",
                      "handleExpiresAt": "2026-09-02T12:00:00Z",
                      "mediaType": "application/mcp-server-card+json",
                      "installability": "installable",
                      "displayName": "Example MCP",
                      "source": {
                        "kind": "url",
                        "url": "https://example.com/mcp.json"
                      },
                      "provenance": {
                        "authority": "example.com",
                        "observedAt": "2026-09-02T11:00:00Z",
                        "mediaType": "application/mcp-server-card+json"
                      }
                    },
                    {
                      "kind": "ai-skill",
                      "handle": "skill-handle",
                      "handleExpiresAt": "2026-09-02T12:00:00Z",
                      "mediaType": "application/ai-skill",
                      "installability": "not-installable-kind",
                      "displayName": "Example skill",
                      "source": {
                        "kind": "embedded"
                      },
                      "provenance": {
                        "authority": "example.com",
                        "observedAt": "2026-09-02T11:00:00Z",
                        "mediaType": "application/ai-skill"
                      }
                    }
                  ],
                  "truncated": false,
                  "negotiated": {
                    "runtimeProtocolVersion": 1,
                    "grantedCapabilities": [
                      "mcp-server-card",
                      "ai-skill-discovery"
                    ]
                  }
                }
                """, CatalogSearchResult.class);

        var success = assertInstanceOf(CatalogSearchSucceeded.class, result);
        var mcpCandidate = assertInstanceOf(CatalogMcpServerCandidate.class, success.getCandidates().get(0));
        assertEquals("mcp-handle", mcpCandidate.getHandle());
        assertInstanceOf(CatalogCandidateSourceUrl.class, mcpCandidate.getSource());
        var skillCandidate = assertInstanceOf(CatalogAiSkillCandidate.class, success.getCandidates().get(1));
        assertEquals("skill-handle", skillCandidate.getHandle());
        assertInstanceOf(CatalogCandidateSourceEmbedded.class, skillCandidate.getSource());
        assertEquals("{\"kind\":\"embedded\"}", MAPPER.writeValueAsString(skillCandidate.getSource()));

        var refusal = MAPPER.readValue("""
                {
                  "kind": "unsupported-kind",
                  "message": "AI skills are unavailable",
                  "requestedKinds": ["ai-skill"],
                  "supportedKinds": ["mcp-server"]
                }
                """, CatalogSearchResult.class);
        assertInstanceOf(CatalogUnsupportedKindError.class, refusal);
    }
}

/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.generated.rpc;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class CatalogCandidateJacksonTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void searchResult_deserializesTypedCandidatesAndSources() throws Exception {
        var json = """
                {
                  "kind": "succeeded",
                  "searchId": "search-1",
                  "candidates": [
                    {
                      "handle": "mcp-handle",
                      "handleExpiresAt": "2026-09-04T14:00:00Z",
                      "kind": "mcp-server",
                      "mediaType": "application/mcp-server-card+json",
                      "installability": "installable",
                      "displayName": "Example MCP server",
                      "source": {
                        "kind": "url",
                        "url": "https://example.test/server.json"
                      },
                      "provenance": {
                        "authority": "example.test",
                        "observedAt": "2026-09-04T13:00:00Z",
                        "mediaType": "application/mcp-server-card+json"
                      }
                    },
                    {
                      "handle": "skill-handle",
                      "handleExpiresAt": "2026-09-04T14:00:00Z",
                      "kind": "ai-skill",
                      "mediaType": "application/ai-skill",
                      "installability": "not-installable-kind",
                      "displayName": "Example skill",
                      "source": {
                        "kind": "embedded"
                      },
                      "provenance": {
                        "authority": "example.test",
                        "observedAt": "2026-09-04T13:00:00Z",
                        "mediaType": "application/ai-skill"
                      }
                    }
                  ],
                  "truncated": false,
                  "negotiated": {
                    "runtimeProtocolVersion": 1,
                    "grantedCapabilities": ["mcp-server-card", "ai-skill-discovery"]
                  }
                }
                """;

        var result = MAPPER.readValue(json, CatalogSearchResult.class);
        var succeeded = assertInstanceOf(CatalogSearchSucceeded.class, result);
        assertEquals(2, succeeded.getCandidates().size());

        var mcp = assertInstanceOf(CatalogMcpServerCandidate.class, succeeded.getCandidates().get(0));
        var urlSource = assertInstanceOf(CatalogCandidateSourceUrl.class, mcp.getSource());
        assertEquals("https://example.test/server.json", urlSource.getUrl());

        var skill = assertInstanceOf(CatalogAiSkillCandidate.class, succeeded.getCandidates().get(1));
        assertInstanceOf(CatalogCandidateSourceEmbedded.class, skill.getSource());

        var serialized = MAPPER.valueToTree(result);
        assertEquals("mcp-server", serialized.at("/candidates/0/kind").asText());
        assertEquals("url", serialized.at("/candidates/0/source/kind").asText());
        assertEquals("ai-skill", serialized.at("/candidates/1/kind").asText());
        assertEquals("embedded", serialized.at("/candidates/1/source/kind").asText());
    }
}

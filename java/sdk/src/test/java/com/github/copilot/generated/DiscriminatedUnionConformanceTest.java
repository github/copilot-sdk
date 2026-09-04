/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.generated;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.generated.rpc.CatalogAiSkillCandidate;
import com.github.copilot.generated.rpc.CatalogAuthenticationRequiredError;
import com.github.copilot.generated.rpc.CatalogCandidateSourceEmbedded;
import com.github.copilot.generated.rpc.CatalogCandidateSourceUrl;
import com.github.copilot.generated.rpc.CatalogMcpServerCandidate;
import com.github.copilot.generated.rpc.CatalogNetworkFailureError;
import com.github.copilot.generated.rpc.CatalogSearchResult;
import com.github.copilot.generated.rpc.CatalogSearchSucceeded;

class DiscriminatedUnionConformanceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String OPAQUE_MCP_HANDLE = "opaque:mcp/01-do-not-parse";
    private static final String OPAQUE_SKILL_HANDLE = "opaque:skill/02-do-not-parse";

    @Test
    void preservesTypedCandidatesAndOpaqueHandles() throws Exception {
        var result = MAPPER.readValue(
                """
                        {
                          "kind": "succeeded",
                          "rawCard": { "secret": "must-not-survive" },
                          "searchId": "search-01",
                          "candidates": [
                            {
                              "kind": "mcp-server",
                              "handle": "opaque:mcp/01-do-not-parse",
                              "handleExpiresAt": "2026-09-02T12:00:00Z",
                              "mediaType": "application/mcp-server-card+json",
                              "installability": "installable",
                              "displayName": "Example MCP",
                              "rawCard": { "secret": "must-not-survive" },
                              "source": { "kind": "url", "url": "https://catalog.example/mcp.json", "rawCard": { "secret": "must-not-survive" } },
                              "provenance": {
                                "authority": "catalog.example",
                                "observedAt": "2026-09-02T11:00:00Z",
                                "mediaType": "application/mcp-server-card+json"
                              }
                            },
                            {
                              "kind": "ai-skill",
                              "handle": "opaque:skill/02-do-not-parse",
                              "handleExpiresAt": "2026-09-02T12:00:00Z",
                              "mediaType": "application/ai-skill",
                              "installability": "not-installable-kind",
                              "displayName": "Example skill",
                              "rawCard": { "secret": "must-not-survive" },
                              "source": { "kind": "embedded", "rawCard": { "secret": "must-not-survive" } },
                              "provenance": {
                                "authority": "catalog.example",
                                "observedAt": "2026-09-02T11:00:00Z",
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
                        """,
                CatalogSearchResult.class);

        var success = assertInstanceOf(CatalogSearchSucceeded.class, result);
        var mcp = assertInstanceOf(CatalogMcpServerCandidate.class, success.getCandidates().get(0));
        var skill = assertInstanceOf(CatalogAiSkillCandidate.class, success.getCandidates().get(1));
        assertEquals(OPAQUE_MCP_HANDLE, mcp.getHandle());
        assertEquals(OPAQUE_SKILL_HANDLE, skill.getHandle());
        assertInstanceOf(CatalogCandidateSourceUrl.class, mcp.getSource());
        assertInstanceOf(CatalogCandidateSourceEmbedded.class, skill.getSource());

        JsonNode encoded = MAPPER.valueToTree(success);
        assertFalse(encoded.has("rawCard"));
        for (JsonNode candidate : encoded.get("candidates")) {
            assertFalse(candidate.has("card"));
            assertFalse(candidate.has("cardData"));
            assertFalse(candidate.has("rawCard"));
            assertFalse(candidate.get("source").has("rawCard"));
        }
    }

    @Test
    void preservesRefusalsAndFailures() throws Exception {
        var authentication = MAPPER.readValue("""
                {"kind":"authentication-required","reason":"no-credential","message":"Sign in is required."}
                """, CatalogSearchResult.class);
        assertInstanceOf(CatalogAuthenticationRequiredError.class, authentication);

        var network = assertInstanceOf(CatalogNetworkFailureError.class,
                MAPPER.readValue(
                        """
                                {"kind":"network-failure","reason":"timeout","retryAfterSeconds":30,"message":"The catalogue timed out."}
                                """,
                        CatalogSearchResult.class));
        assertEquals(30L, network.getRetryAfterSeconds());
    }

    @Test
    void rejectsUnknownAndMissingDiscriminators() {
        var invalidPayloads = new String[]{"""
                {"kind":"future-result","rawCard":{"secret":"must-not-survive"}}
                """, """
                {"rawCard":{"secret":"must-not-survive"}}
                """, """
                {
                  "kind":"succeeded",
                  "searchId":"search-invalid",
                  "candidates":[{"kind":"future-candidate","rawCard":{"secret":"must-not-survive"}}],
                  "truncated":false,
                  "negotiated":{"runtimeProtocolVersion":1,"grantedCapabilities":[]}
                }
                """, """
                {
                  "kind":"succeeded",
                  "searchId":"search-invalid",
                  "candidates":[{"rawCard":{"secret":"must-not-survive"}}],
                  "truncated":false,
                  "negotiated":{"runtimeProtocolVersion":1,"grantedCapabilities":[]}
                }
                """,
                """
                        {
                          "kind":"succeeded",
                          "searchId":"search-invalid",
                          "candidates":[{
                            "kind":"mcp-server",
                            "handle":"opaque:mcp/01-do-not-parse",
                            "handleExpiresAt":"2026-09-02T12:00:00Z",
                            "mediaType":"application/mcp-server-card+json",
                            "installability":"installable",
                            "displayName":"Example MCP",
                            "source":{"kind":"future-source","rawCard":{"secret":"must-not-survive"}},
                            "provenance":{"authority":"catalog.example","observedAt":"2026-09-02T11:00:00Z","mediaType":"application/mcp-server-card+json"}
                          }],
                          "truncated":false,
                          "negotiated":{"runtimeProtocolVersion":1,"grantedCapabilities":[]}
                        }
                        """,
                """
                        {
                          "kind":"succeeded",
                          "searchId":"search-invalid",
                          "candidates":[{
                            "kind":"mcp-server",
                            "handle":"opaque:mcp/01-do-not-parse",
                            "handleExpiresAt":"2026-09-02T12:00:00Z",
                            "mediaType":"application/mcp-server-card+json",
                            "installability":"installable",
                            "displayName":"Example MCP",
                            "source":{"rawCard":{"secret":"must-not-survive"}},
                            "provenance":{"authority":"catalog.example","observedAt":"2026-09-02T11:00:00Z","mediaType":"application/mcp-server-card+json"}
                          }],
                          "truncated":false,
                          "negotiated":{"runtimeProtocolVersion":1,"grantedCapabilities":[]}
                        }
                        """};

        for (var payload : invalidPayloads) {
            assertThrows(JsonProcessingException.class, () -> MAPPER.readValue(payload, CatalogSearchResult.class));
        }
    }
}

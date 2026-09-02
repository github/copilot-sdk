using System.Text.Json;
using System.Text.Json.Serialization.Metadata;
using GitHub.Copilot.Rpc;
using Xunit;

#pragma warning disable GHCP001 // The catalogue search schema is experimental in CLI 1.0.83-2.

namespace GitHub.Copilot.Test.Unit;

public class CatalogueConformanceTests
{
    private const string OpaqueMcpHandle = "opaque:mcp/01-do-not-parse";
    private const string OpaqueSkillHandle = "opaque:skill/02-do-not-parse";
    private static readonly JsonSerializerOptions SerializerOptions = new(JsonSerializerDefaults.Web)
    {
        TypeInfoResolver = new DefaultJsonTypeInfoResolver(),
    };

    [Fact]
    public void CatalogSearchResult_PreservesTypedCandidatesAndOpaqueHandles()
    {
        const string json = """
            {
              "kind": "succeeded",
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
                  "source": { "kind": "url", "url": "https://catalog.example/mcp.json" },
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
                  "source": { "kind": "embedded" },
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
            """;

        var result = Assert.IsType<CatalogSearchResultSucceeded>(
            JsonSerializer.Deserialize<CatalogSearchResult>(json, SerializerOptions));
        var mcp = Assert.IsType<CatalogCandidateMcpServer>(result.Candidates[0]);
        var skill = Assert.IsType<CatalogCandidateAiSkill>(result.Candidates[1]);
        Assert.Equal(OpaqueMcpHandle, mcp.Handle);
        Assert.Equal(OpaqueSkillHandle, skill.Handle);
        Assert.IsType<CatalogCandidateSourceUrl>(mcp.Source);
        Assert.IsType<CatalogCandidateSourceEmbedded>(skill.Source);

        using var encoded = JsonDocument.Parse(JsonSerializer.Serialize<CatalogSearchResult>(
            result, SerializerOptions));
        foreach (var candidate in encoded.RootElement.GetProperty("candidates").EnumerateArray())
        {
            Assert.False(candidate.TryGetProperty("card", out _));
            Assert.False(candidate.TryGetProperty("cardData", out _));
            Assert.False(candidate.TryGetProperty("rawCard", out _));
        }
    }

    [Fact]
    public void CatalogSearchResult_PreservesRefusalsAndFailures()
    {
        var authentication = JsonSerializer.Deserialize<CatalogSearchResult>(
            """{"kind":"authentication-required","reason":"no-credential","message":"Sign in is required."}""",
            SerializerOptions);
        Assert.IsType<CatalogSearchResultAuthenticationRequired>(authentication);

        var network = Assert.IsType<CatalogSearchResultNetworkFailure>(
            JsonSerializer.Deserialize<CatalogSearchResult>(
                """{"kind":"network-failure","reason":"timeout","retryAfterSeconds":30,"message":"The catalogue timed out."}""",
                SerializerOptions));
        Assert.Equal(30, network.RetryAfterSeconds);
    }
}

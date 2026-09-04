using System.Text.Json;
using System.Text.Json.Serialization.Metadata;
using GitHub.Copilot.Rpc;
using Xunit;

#pragma warning disable GHCP001 // The catalogue search schema is experimental.

namespace GitHub.Copilot.Test.Unit;

public class DiscriminatedUnionConformanceTests
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
        Assert.False(encoded.RootElement.TryGetProperty("rawCard", out _));
        foreach (var candidate in encoded.RootElement.GetProperty("candidates").EnumerateArray())
        {
            Assert.False(candidate.TryGetProperty("card", out _));
            Assert.False(candidate.TryGetProperty("cardData", out _));
            Assert.False(candidate.TryGetProperty("rawCard", out _));
            Assert.False(candidate.GetProperty("source").TryGetProperty("rawCard", out _));
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

    [Fact]
    public void ClosedUnions_RejectUnknownAndMissingDiscriminators()
    {
        string[] invalidPayloads =
        [
            """{"kind":"future-result","rawCard":{"secret":"must-not-survive"}}""",
            """{"rawCard":{"secret":"must-not-survive"}}""",
            """
            {
              "kind":"succeeded",
              "searchId":"search-invalid",
              "candidates":[{"kind":"future-candidate","rawCard":{"secret":"must-not-survive"}}],
              "truncated":false,
              "negotiated":{"runtimeProtocolVersion":1,"grantedCapabilities":[]}
            }
            """,
            """
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
            """,
        ];

        foreach (string json in invalidPayloads)
        {
            Exception? exception = Record.Exception(() =>
                JsonSerializer.Deserialize<CatalogSearchResult>(json, SerializerOptions));
            Assert.True(exception is JsonException, $"Invalid closed union payload was accepted: {json}");
        }
    }
}

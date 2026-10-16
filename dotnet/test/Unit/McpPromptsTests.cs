/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Text.Json;
using System.Text.Json.Nodes;
using System.Text.Json.Serialization;
using System.Text.Json.Serialization.Metadata;
using GitHub.Copilot.Rpc;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

/// <summary>Generated result fidelity complements the real MCP transport E2E.</summary>
public class McpPromptsTests
{
    private static readonly JsonSerializerOptions Options = new()
    {
        DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull,
        TypeInfoResolver = new DefaultJsonTypeInfoResolver(),
    };

    [Theory]
    [InlineData("firstPage")]
    [InlineData("secondPage")]
    [InlineData("richPrompt")]
    public void Results_Preserve_Optional_Fields_And_Opaque_Content(string fixtureName)
    {
        var directory = new DirectoryInfo(AppContext.BaseDirectory);
        string? fixturePath = null;
        while (directory is not null)
        {
            var candidate = Path.Combine(directory.FullName, "test", "harness", "mcp-prompt-fixtures.json");
            if (File.Exists(candidate))
            {
                fixturePath = candidate;
                break;
            }
            directory = directory.Parent;
        }
        Assert.NotNull(fixturePath);
        var expected = JsonNode.Parse(File.ReadAllText(fixturePath))![fixtureName]!;
        var actual = fixtureName == "richPrompt"
            ? JsonSerializer.SerializeToNode(expected.Deserialize<McpPromptsGetResult>(Options), Options)
            : JsonSerializer.SerializeToNode(expected.Deserialize<McpPromptsListResult>(Options), Options);
        Assert.True(JsonNode.DeepEquals(expected, actual), $"Expected: {expected}\nActual: {actual}");
    }
}

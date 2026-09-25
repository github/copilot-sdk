/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Text.Json;
using System.Text.Json.Serialization;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public class CanvasDeclarationTests
{
    [Fact]
    public void SerializesIconPath()
    {
        var declaration = new CanvasDeclaration
        {
            Id = "counter",
            DisplayName = "Counter",
            Description = "Count things",
            Icon = "icons/counter.png",
        };
        var serialized = JsonSerializer.Serialize(declaration, CanvasDeclarationTestJsonContext.Default.CanvasDeclaration);
        using var doc = JsonDocument.Parse(serialized);

        Assert.Equal("icons/counter.png", doc.RootElement.GetProperty("icon").GetString());
    }

    [Fact]
    public void RoundtripsIconPath()
    {
        const string json = """
            {"id":"counter","displayName":"Counter","description":"Count things","icon":"icons/counter.png"}
            """;
        var declaration = JsonSerializer.Deserialize(json, CanvasDeclarationTestJsonContext.Default.CanvasDeclaration);
        Assert.NotNull(declaration);
        Assert.Equal("icons/counter.png", declaration.Icon);
        var serialized = JsonSerializer.Serialize(declaration, CanvasDeclarationTestJsonContext.Default.CanvasDeclaration);
        using var doc = JsonDocument.Parse(serialized);

        Assert.Equal("icons/counter.png", doc.RootElement.GetProperty("icon").GetString());
    }

    [Fact]
    public void OmitsUnspecifiedIcon()
    {
        var declaration = new CanvasDeclaration
        {
            Id = "counter",
            DisplayName = "Counter",
            Description = "Count things",
        };
        var serialized = JsonSerializer.Serialize(declaration, CanvasDeclarationTestJsonContext.Default.CanvasDeclaration);
        using var doc = JsonDocument.Parse(serialized);

        Assert.False(doc.RootElement.TryGetProperty("icon", out _));
    }
}

[JsonSourceGenerationOptions(DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull)]
[JsonSerializable(typeof(CanvasDeclaration))]
internal partial class CanvasDeclarationTestJsonContext : JsonSerializerContext;

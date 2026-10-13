/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.ComponentModel;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace GitHub.Copilot;

/// <summary>Reads hook errors from plain text or structured runtime error objects.</summary>
[EditorBrowsable(EditorBrowsableState.Never)]
public sealed class HookErrorJsonConverter : JsonConverter<string>
{
    /// <inheritdoc />
    public override string? Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
    {
        if (reader.TokenType == JsonTokenType.String)
        {
            return reader.GetString();
        }
        if (reader.TokenType != JsonTokenType.StartObject)
        {
            throw new JsonException("Expected an error string or object.");
        }

        using var document = JsonDocument.ParseValue(ref reader);
        var error = document.RootElement;
        return error.TryGetProperty("message", out var message) && message.ValueKind == JsonValueKind.String
            ? message.GetString()
            : error.GetRawText();
    }

    /// <inheritdoc />
    public override void Write(Utf8JsonWriter writer, string value, JsonSerializerOptions options) =>
        writer.WriteStringValue(value);
}

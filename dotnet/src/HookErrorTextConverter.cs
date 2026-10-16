/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Text.Json;
using System.Text.Json.Serialization;

namespace GitHub.Copilot;

/// <summary>
/// Reads the <c>error</c> of an errorOccurred or sessionEnd hook input as a string.
/// The CLI sends an error object (<c>{"name", "message", "stack"}</c> or <c>{"message"}</c>):
/// its message becomes the error text, and an object without a string message becomes its JSON.
/// A string passes through.
/// </summary>
internal sealed class HookErrorTextConverter : JsonConverter<string?>
{
    /// <inheritdoc />
    public override string? Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
    {
        if (reader.TokenType == JsonTokenType.StartObject)
        {
            using var document = JsonDocument.ParseValue(ref reader);
            var error = document.RootElement;
            return error.TryGetProperty("message", out var message) && message.ValueKind == JsonValueKind.String
                ? message.GetString()
                : error.GetRawText();
        }

        return reader.TokenType == JsonTokenType.String
            ? reader.GetString()
            : throw new JsonException(
                $"Expected a string or an error object for a hook error, got {reader.TokenType}.");
    }

    /// <inheritdoc />
    public override void Write(Utf8JsonWriter writer, string? value, JsonSerializerOptions options) =>
        writer.WriteStringValue(value);
}

/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.ComponentModel;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace GitHub.Copilot.SDK;

/// <summary>Converts between JSON numeric milliseconds-since-Unix-epoch and <see cref="DateTimeOffset"/>.</summary>
[EditorBrowsable(EditorBrowsableState.Never)]
public sealed class UnixMillisecondsDateTimeOffsetConverter : JsonConverter<DateTimeOffset>
{
    /// <inheritdoc />
    public override DateTimeOffset Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options) =>
        DateTimeOffset.FromUnixTimeMilliseconds(reader.GetInt64());

    /// <inheritdoc />
    public override void Write(Utf8JsonWriter writer, DateTimeOffset value, JsonSerializerOptions options) =>
        writer.WriteNumberValue(value.ToUnixTimeMilliseconds());
}

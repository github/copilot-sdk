/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.ComponentModel;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace GitHub.Copilot;

/// <summary>Converts between JSON numeric milliseconds-since-Unix-epoch and <see cref="DateTimeOffset"/>.</summary>
[EditorBrowsable(EditorBrowsableState.Never)]
public sealed class UnixMillisecondsDateTimeOffsetConverter : JsonConverter<DateTimeOffset>
{
    /// <inheritdoc />
    public override DateTimeOffset Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
    {
        // The CLI may serialize the epoch-millisecond timestamp as a JSON integer
        // or as a floating-point number (e.g. 1700000000000.0). GetInt64 throws on a
        // fractional token, so fall back to reading a double and truncating.
        long milliseconds = reader.TryGetInt64(out long value) ? value : (long)reader.GetDouble();
        return DateTimeOffset.FromUnixTimeMilliseconds(milliseconds);
    }

    /// <inheritdoc />
    public override void Write(Utf8JsonWriter writer, DateTimeOffset value, JsonSerializerOptions options) =>
        writer.WriteNumberValue(value.ToUnixTimeMilliseconds());
}

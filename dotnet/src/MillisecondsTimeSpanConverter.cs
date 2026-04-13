/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.ComponentModel;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace GitHub.Copilot.SDK;

/// <summary>Converts between JSON numeric milliseconds and <see cref="TimeSpan"/>.</summary>
[EditorBrowsable(EditorBrowsableState.Never)]
public sealed class MillisecondsTimeSpanConverter : JsonConverter<TimeSpan>
{
    /// <inheritdoc />
    public override TimeSpan Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options) =>
        TimeSpan.FromMilliseconds(reader.GetDouble());

    /// <inheritdoc />
    public override void Write(Utf8JsonWriter writer, TimeSpan value, JsonSerializerOptions options) =>
        writer.WriteNumberValue(value.TotalMilliseconds);
}

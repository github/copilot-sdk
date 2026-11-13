/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Buffers;
using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization.Metadata;
#if !NETSTANDARD2_0
using System.Text.Unicode;
#endif

namespace GitHub.Copilot;

internal sealed partial class SessionEventJsonConverter
{
    private static readonly Encoding StrictUtf8 = new UTF8Encoding(false, true);

    internal static SessionEvent Deserialize(string json)
    {
#if !NETSTANDARD2_0
        const int StackallocThreshold = 512;
#endif
        ArgumentNullException.ThrowIfNull(json);

        byte[]? rented = null;
        Span<byte> buffer =
#if !NETSTANDARD2_0
            json.Length <= StackallocThreshold / 3 ? stackalloc byte[StackallocThreshold] :
#endif
            rented = ArrayPool<byte>.Shared.Rent(GetUtf8ByteCount(json));
        try
        {
#if NETSTANDARD2_0
            int byteCount = StrictUtf8.GetBytes(json, 0, json.Length, rented, 0);
#else
            var status = Utf8.FromUtf16(json.AsSpan(), buffer, out _, out int byteCount, replaceInvalidSequences: false);
            if (status != OperationStatus.Done)
            {
                throw new ArgumentException($"Cannot transcode UTF-16 JSON text to UTF-8 ({status}).", nameof(json));
            }
#endif
            ReadOnlySpan<byte> utf8 = buffer.Slice(0, byteCount);
            var reader = new Utf8JsonReader(utf8);

            // Select metadata without entering the converter: reader-based nested deserialization
            // would scan the entire object again to scope its input before parsing it.
            JsonTypeInfo typeInfo;
            try
            {
                reader.Read();
                typeInfo = ReadEventTypeInfo(ref reader);
            }
            catch (JsonException ex) when (ex.GetType() != typeof(JsonException))
            {
                // Match JsonSerializer's normalization of reader errors.
                throw new JsonException(ex.Message, ex.Path, ex.LineNumber, ex.BytePositionInLine, ex);
            }
            return ToSessionEvent(JsonSerializer.Deserialize(utf8, typeInfo));
        }
        finally
        {
            if (rented is not null)
            {
                buffer.Clear();
                ArrayPool<byte>.Shared.Return(rented);
            }
        }
    }

    private static int GetUtf8ByteCount(string json)
    {
        try
        {
            return StrictUtf8.GetByteCount(json);
        }
        catch (EncoderFallbackException ex)
        {
            throw new ArgumentException("Cannot transcode invalid UTF-16 JSON text to UTF-8.", nameof(json), ex);
        }
    }

    private static string ReadString(ref Utf8JsonReader reader)
    {
        try
        {
            return reader.GetString()!;
        }
        catch (InvalidOperationException ex)
        {
            throw new JsonException(ex.Message, ex);
        }
    }
}

/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Diagnostics;
using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;
using System.Text.Json.Serialization.Metadata;

namespace GitHub.Copilot;

internal sealed class WorkerCausalityConverter<T> : JsonConverter<T> where T : class
{
    public override T? Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
    {
        using var document = JsonDocument.ParseValue(ref reader);
        var value = document.RootElement;
        var remaining = 4096;
        if (Small(value, ref remaining, 0) && Supported(value))
        {
            try
            {
                if (!WithinBudget(value))
                    throw new JsonException("optional worker metadata budget");
                return JsonSerializer.Deserialize(value, (JsonTypeInfo<T>)options.GetTypeInfo(typeof(T)));
            }
            catch (JsonException)
            {
                // Optional diagnostics do not change product event handling.
            }
        }
        Trace.TraceWarning("Ignoring invalid, unsupported or oversized workerCausality metadata");
        return null;
    }

    public override void Write(Utf8JsonWriter writer, T value, JsonSerializerOptions options) =>
        JsonSerializer.Serialize(writer, value, (JsonTypeInfo<T>)options.GetTypeInfo(typeof(T)));

    private static bool Supported(JsonElement value)
    {
        if (value.ValueKind != JsonValueKind.Object ||
            !Fields(value, "version", "observationProvenance", "sources", "captureComplete") ||
            !value.TryGetProperty("version", out var version) || version.ValueKind != JsonValueKind.Number || !version.TryGetInt32(out var number) || number != 1 ||
            !value.TryGetProperty("observationProvenance", out var provenance) || provenance.ValueKind != JsonValueKind.String ||
            provenance.GetString() is not ("native" or "ahp_coordinator") ||
            !value.TryGetProperty("captureComplete", out var complete) || complete.ValueKind is not (JsonValueKind.True or JsonValueKind.False) ||
            !value.TryGetProperty("sources", out var sources) || sources.ValueKind != JsonValueKind.Array || sources.GetArrayLength() > 32)
        {
            return false;
        }
        foreach (var source in sources.EnumerateArray())
        {
            if (source.ValueKind != JsonValueKind.Object ||
                !Fields(source, "input", "admissions", "captureComplete", "completion", "notification", "admittedDuring") ||
                !source.TryGetProperty("captureComplete", out var sourceComplete) || sourceComplete.ValueKind is not (JsonValueKind.True or JsonValueKind.False) ||
                (complete.ValueKind == JsonValueKind.True && sourceComplete.ValueKind != JsonValueKind.True) ||
                !source.TryGetProperty("admissions", out var admissions) || admissions.ValueKind != JsonValueKind.Array || admissions.GetArrayLength() > 32 ||
                !source.TryGetProperty("input", out var input) || input.ValueKind != JsonValueKind.Object ||
                !Fields(input, "queueItemId", "agentId", "sender", "senderBridges") ||
                !Uuid(input, "queueItemId") || !Text(input, "agentId") ||
                !OptionalReference(input, "sender", "tool.execution_start") ||
                !OptionalReference(source, "completion", "subagent.completed") ||
                !OptionalReference(source, "admittedDuring", "assistant.turn_start"))
            {
                return false;
            }
            if (input.TryGetProperty("senderBridges", out var edges))
            {
                if (!input.TryGetProperty("sender", out _) || edges.ValueKind != JsonValueKind.Array || edges.GetArrayLength() > 32)
                    return false;
                foreach (var edge in edges.EnumerateArray())
                {
                    if (edge.ValueKind != JsonValueKind.Object ||
                        !Fields(edge, "source", "reported") ||
                        !edge.TryGetProperty("source", out var origin) || !Reference(origin, "tool.execution_start") ||
                        !edge.TryGetProperty("reported", out var reported) || !Reference(reported, "tool.execution_start"))
                        return false;
                }
            }
            foreach (var admission in admissions.EnumerateArray())
            {
                if (admission.ValueKind != JsonValueKind.Object ||
                    !Fields(admission, "kind", "messageId", "event", "ahpTurnId") ||
                    !Text(admission, "messageId") ||
                    !Text(admission, "kind") || admission.GetProperty("kind").GetString() is not ("queued_input" or "system_continuation") ||
                    (admission.TryGetProperty("ahpTurnId", out _) && !Uuid(admission, "ahpTurnId")))
                    return false;
                if (admission.TryGetProperty("event", out var occurrence) &&
                    (!Reference(occurrence, "user.message") || !Text(occurrence, "agentId") ||
                     occurrence.GetProperty("agentId").GetString() != input.GetProperty("agentId").GetString()))
                    return false;
            }
            if (source.TryGetProperty("notification", out var notification) &&
                (notification.ValueKind != JsonValueKind.Object ||
                 !Fields(notification, "deliveryId", "event", "mode") ||
                 !Uuid(notification, "deliveryId") ||
                 !Text(notification, "mode") || notification.GetProperty("mode").GetString() is not ("queued" or "immediate") ||
                 !OptionalReference(notification, "event", "system.notification")))
                return false;
        }
        return true;
    }

    private static bool Fields(JsonElement value, params string[] allowed)
    {
        foreach (var property in value.EnumerateObject())
            if (!allowed.Contains(property.Name))
                return false;
        return true;
    }

    private static bool Text(JsonElement value, string property) =>
        value.TryGetProperty(property, out var text) &&
        text.ValueKind == JsonValueKind.String &&
        text.GetString()!.Length > 0 &&
        Encoding.UTF8.GetByteCount(text.GetString()!) <= 256;

    private static bool Uuid(JsonElement value, string property) =>
        Text(value, property) && Guid.TryParseExact(value.GetProperty(property).GetString(), "D", out _);

    private static bool OptionalReference(JsonElement value, string property, string eventType) =>
        !value.TryGetProperty(property, out var reference) || Reference(reference, eventType);

    private static bool Reference(JsonElement value, string eventType) =>
        value.ValueKind == JsonValueKind.Object &&
        Fields(value, "sessionId", "eventId", "agentId", "eventType", "provenance") &&
        Text(value, "sessionId") &&
        value.GetProperty("sessionId").GetString()!.All(c => c is >= 'A' and <= 'Z' or >= 'a' and <= 'z' or >= '0' and <= '9' or '_' or '-') &&
        Uuid(value, "eventId") && (!value.TryGetProperty("agentId", out _) || Text(value, "agentId")) &&
        Text(value, "eventType") && value.GetProperty("eventType").GetString() == eventType &&
        Text(value, "provenance") && value.GetProperty("provenance").GetString() is "native" or "ahp_coordinator";

    private static bool Small(JsonElement value, ref int remaining, int depth)
    {
        if (--remaining < 0 || depth > 32) return false;
        switch (value.ValueKind)
        {
            case JsonValueKind.String:
                remaining -= value.GetString()!.Length;
                break;
            case JsonValueKind.Array:
                foreach (var item in value.EnumerateArray())
                    if (!Small(item, ref remaining, depth + 1)) return false;
                break;
            case JsonValueKind.Object:
                foreach (var property in value.EnumerateObject())
                {
                    remaining -= property.Name.Length;
                    if (!Small(property.Value, ref remaining, depth + 1)) return false;
                }
                break;
        }
        return remaining >= 0;
    }

    private static bool WithinBudget(JsonElement value) =>
        3 + CompactStringBytes("workerCausality") + CompactBytes(value) <= 4096;

    private static int CompactBytes(JsonElement value)
    {
        switch (value.ValueKind)
        {
            case JsonValueKind.Object:
                var objectBytes = 2;
                var firstProperty = true;
                foreach (var property in value.EnumerateObject())
                {
                    if (!firstProperty)
                        objectBytes++;
                    firstProperty = false;
                    objectBytes += CompactStringBytes(property.Name) + 1 + CompactBytes(property.Value);
                }
                return objectBytes;
            case JsonValueKind.Array:
                var arrayBytes = 2;
                var firstItem = true;
                foreach (var item in value.EnumerateArray())
                {
                    if (!firstItem)
                        arrayBytes++;
                    firstItem = false;
                    arrayBytes += CompactBytes(item);
                }
                return arrayBytes;
            case JsonValueKind.String:
                return CompactStringBytes(value.GetString()!);
            case JsonValueKind.True:
                return 4;
            case JsonValueKind.False:
                return 5;
            case JsonValueKind.Null:
                return 4;
            case JsonValueKind.Number:
                return value.GetRawText().Length;
            default:
                return int.MaxValue;
        }
    }

    private static int CompactStringBytes(string value)
    {
        var bytes = 2;
        for (var index = 0; index < value.Length; index++)
        {
            var character = value[index];
            bytes += character switch
            {
                '"' or '\\' or '\b' or '\f' or '\n' or '\r' or '\t' => 2,
                <= '\u001f' => 6,
                <= '\u007f' => 1,
                <= '\u07ff' => 2,
                _ when char.IsHighSurrogate(character) &&
                    index + 1 < value.Length &&
                    char.IsLowSurrogate(value[index + 1]) => 4,
                _ => 3,
            };
            if (char.IsHighSurrogate(character) &&
                index + 1 < value.Length &&
                char.IsLowSurrogate(value[index + 1]))
                index++;
        }
        return bytes;
    }
}

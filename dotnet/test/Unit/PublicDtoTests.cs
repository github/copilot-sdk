/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Collections;
using System.Reflection;
using System.Text.Json;
using Xunit;

namespace GitHub.Copilot.SDK.Test.Unit;

/// <summary>
/// Reflection-based safety net that exercises the get/set surface of every public DTO in
/// the SDK assembly. The intent is to (1) keep System.Text.Json source-generation
/// configurations from drifting (NativeAOT-friendly serializer must know every public DTO),
/// and (2) catch accidental property-shape regressions (read-only setters, mismatched
/// nullability, generated bridge types). It is **not** a serialization-correctness test;
/// for that, write targeted serializer tests against fixed JSON payloads (see
/// <c>SessionEventSerializationTests</c> for the pattern).
/// </summary>
public class PublicDtoTests
{
    [Fact]
    public void Public_Dto_Properties_Can_Be_Set_And_Read()
    {
        var exercisedProperties = 0;
        var assembly = typeof(CopilotClient).Assembly;
        var candidateTypes = assembly
            .GetTypes()
            .Where(type =>
                type is { IsClass: true, IsAbstract: false, IsPublic: true } &&
                type.Namespace?.StartsWith("GitHub.Copilot.SDK", StringComparison.Ordinal) == true &&
                type.GetConstructor(Type.EmptyTypes) is not null)
            .OrderBy(type => type.FullName, StringComparer.Ordinal);

        foreach (var type in candidateTypes)
        {
            var instance = Activator.CreateInstance(type)!;

            foreach (var property in type.GetProperties(BindingFlags.Instance | BindingFlags.Public))
            {
                if (property.GetIndexParameters().Length != 0)
                {
                    continue;
                }

                if (property.SetMethod?.IsPublic == true &&
                    TryCreateSampleValue(property.PropertyType, [], out var sampleValue))
                {
                    property.SetValue(instance, sampleValue);
                }

                if (property.GetMethod?.IsPublic == true)
                {
                    _ = property.GetValue(instance);
                    exercisedProperties++;
                }
            }
        }

        Assert.True(exercisedProperties > 1_000, $"Expected to exercise many DTO properties, but only exercised {exercisedProperties}.");
    }

    private static bool TryCreateSampleValue(Type type, HashSet<Type> visited, out object? value)
    {
        var nullableType = Nullable.GetUnderlyingType(type);
        if (nullableType is not null)
        {
            return TryCreateSampleValue(nullableType, visited, out value);
        }

        if (type == typeof(string))
        {
            value = "value";
            return true;
        }

        if (type == typeof(bool))
        {
            value = true;
            return true;
        }

        if (type == typeof(int))
        {
            value = 1;
            return true;
        }

        if (type == typeof(long))
        {
            value = 1L;
            return true;
        }

        if (type == typeof(double))
        {
            value = 1.0;
            return true;
        }

        if (type == typeof(DateTimeOffset))
        {
            value = DateTimeOffset.UnixEpoch;
            return true;
        }

        if (type == typeof(DateTime))
        {
            value = DateTime.UnixEpoch;
            return true;
        }

        if (type == typeof(TimeSpan))
        {
            value = TimeSpan.FromMilliseconds(1);
            return true;
        }

        if (type == typeof(JsonElement))
        {
            using var document = JsonDocument.Parse("""{"value":1}""");
            value = document.RootElement.Clone();
            return true;
        }

        if (type == typeof(object))
        {
            value = "value";
            return true;
        }

        if (type.IsEnum)
        {
            var values = Enum.GetValues(type);
            value = values.Length > 0 ? values.GetValue(0) : Activator.CreateInstance(type);
            return true;
        }

        if (type.IsArray)
        {
            var elementType = type.GetElementType()!;
            if (!TryCreateSampleValue(elementType, visited, out var elementValue))
            {
                elementValue = elementType.IsValueType ? Activator.CreateInstance(elementType) : null;
            }

            var array = Array.CreateInstance(elementType, 1);
            array.SetValue(elementValue, 0);
            value = array;
            return true;
        }

        if (TryCreateGenericCollection(type, visited, out value))
        {
            return true;
        }

        if (!type.IsValueType && type.GetConstructor(Type.EmptyTypes) is not null && visited.Add(type))
        {
            value = Activator.CreateInstance(type);
            visited.Remove(type);
            return true;
        }

        value = type.IsValueType ? Activator.CreateInstance(type) : null;
        return true;
    }

    private static bool TryCreateGenericCollection(Type type, HashSet<Type> visited, out object? value)
    {
        var dictionaryInterface = type.GetInterfaces()
            .Append(type)
            .FirstOrDefault(candidate =>
                candidate.IsGenericType &&
                (candidate.GetGenericTypeDefinition() == typeof(IDictionary<,>) ||
                 candidate.GetGenericTypeDefinition() == typeof(IReadOnlyDictionary<,>)) &&
                candidate.GetGenericArguments()[0] == typeof(string));

        if (dictionaryInterface is not null)
        {
            var valueType = dictionaryInterface.GetGenericArguments()[1];
            TryCreateSampleValue(valueType, visited, out var sampleValue);
            var dictionary = (IDictionary)Activator.CreateInstance(typeof(Dictionary<,>).MakeGenericType(typeof(string), valueType))!;
            dictionary["key"] = sampleValue;
            value = dictionary;
            return true;
        }

        var enumerableInterface = type.GetInterfaces()
            .Append(type)
            .FirstOrDefault(candidate =>
                candidate.IsGenericType &&
                (candidate.GetGenericTypeDefinition() == typeof(IList<>) ||
                 candidate.GetGenericTypeDefinition() == typeof(IReadOnlyList<>) ||
                 candidate.GetGenericTypeDefinition() == typeof(IEnumerable<>)));

        if (enumerableInterface is not null)
        {
            var elementType = enumerableInterface.GetGenericArguments()[0];
            TryCreateSampleValue(elementType, visited, out var sampleValue);
            var list = (IList)Activator.CreateInstance(typeof(List<>).MakeGenericType(elementType))!;
            list.Add(sampleValue);
            value = list;
            return true;
        }

        value = null;
        return false;
    }
}

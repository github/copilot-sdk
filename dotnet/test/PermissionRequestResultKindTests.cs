/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Text.Json;
using Xunit;

namespace GitHub.Copilot.SDK.Test;

public class PermissionRequestResultKindTests
{
    private static readonly JsonSerializerOptions s_jsonOptions = new(JsonSerializerDefaults.Web)
    {
        TypeInfoResolver = TestJsonContext.Default,
    };

    [Fact]
    public void WellKnownKinds_HaveExpectedValues()
    {
        Assert.Equal("approve-once", PermissionRequestResultKind.Approved.Value);
        Assert.Equal("reject", PermissionRequestResultKind.Rejected.Value);
        Assert.Equal("user-not-available", PermissionRequestResultKind.UserNotAvailable.Value);
        Assert.Equal("no-result", PermissionRequestResultKind.NoResult.Value);

        // Deprecated aliases still resolve
#pragma warning disable CS0618
        Assert.Equal(PermissionRequestResultKind.Rejected, PermissionRequestResultKind.DeniedInteractivelyByUser);
        Assert.Equal(PermissionRequestResultKind.UserNotAvailable, PermissionRequestResultKind.DeniedCouldNotRequestFromUser);
        Assert.Equal(PermissionRequestResultKind.UserNotAvailable, PermissionRequestResultKind.DeniedByRules);
#pragma warning restore CS0618
    }

    [Fact]
    public void Equals_SameValue_ReturnsTrue()
    {
        var a = new PermissionRequestResultKind("approve-once");
        Assert.True(a == PermissionRequestResultKind.Approved);
        Assert.True(a.Equals(PermissionRequestResultKind.Approved));
        Assert.True(a.Equals((object)PermissionRequestResultKind.Approved));
    }

    [Fact]
    public void Equals_DifferentValue_ReturnsFalse()
    {
        Assert.True(PermissionRequestResultKind.Approved != PermissionRequestResultKind.Rejected);
        Assert.False(PermissionRequestResultKind.Approved.Equals(PermissionRequestResultKind.Rejected));
    }

    [Fact]
    public void Equals_IsCaseInsensitive()
    {
        var upper = new PermissionRequestResultKind("APPROVE-ONCE");
        Assert.Equal(PermissionRequestResultKind.Approved, upper);
    }

    [Fact]
    public void GetHashCode_IsCaseInsensitive()
    {
        var upper = new PermissionRequestResultKind("APPROVE-ONCE");
        Assert.Equal(PermissionRequestResultKind.Approved.GetHashCode(), upper.GetHashCode());
    }

    [Fact]
    public void ToString_ReturnsValue()
    {
        Assert.Equal("approve-once", PermissionRequestResultKind.Approved.ToString());
        Assert.Equal("reject", PermissionRequestResultKind.Rejected.ToString());
    }

    [Fact]
    public void CustomValue_IsPreserved()
    {
        var custom = new PermissionRequestResultKind("custom-kind");
        Assert.Equal("custom-kind", custom.Value);
        Assert.Equal("custom-kind", custom.ToString());
    }

    [Fact]
    public void Constructor_NullValue_TreatedAsEmpty()
    {
        var kind = new PermissionRequestResultKind(null!);
        Assert.Equal(string.Empty, kind.Value);
    }

    [Fact]
    public void Default_HasEmptyStringValue()
    {
        var defaultKind = default(PermissionRequestResultKind);
        Assert.Equal(string.Empty, defaultKind.Value);
        Assert.Equal(string.Empty, defaultKind.ToString());
        Assert.Equal(defaultKind.GetHashCode(), defaultKind.GetHashCode());
    }

    [Fact]
    public void Equals_NonPermissionRequestResultKindObject_ReturnsFalse()
    {
        Assert.False(PermissionRequestResultKind.Approved.Equals("approve-once"));
    }

    [Fact]
    public void JsonSerialize_WritesStringValue()
    {
        var result = new PermissionRequestResult { Kind = PermissionRequestResultKind.Approved };
        var json = JsonSerializer.Serialize(result, s_jsonOptions);
        Assert.Contains("\"kind\":\"approve-once\"", json);
    }

    [Fact]
    public void JsonDeserialize_ReadsStringValue()
    {
        var json = """{"kind":"reject"}""";
        var result = JsonSerializer.Deserialize<PermissionRequestResult>(json, s_jsonOptions)!;
        Assert.Equal(PermissionRequestResultKind.Rejected, result.Kind);
    }

    [Fact]
    public void JsonRoundTrip_PreservesAllKinds()
    {
        var kinds = new[]
        {
            PermissionRequestResultKind.Approved,
            PermissionRequestResultKind.Rejected,
            PermissionRequestResultKind.UserNotAvailable,
            PermissionRequestResultKind.NoResult,
        };

        foreach (var kind in kinds)
        {
            var result = new PermissionRequestResult { Kind = kind };
            var json = JsonSerializer.Serialize(result, s_jsonOptions);
            var deserialized = JsonSerializer.Deserialize<PermissionRequestResult>(json, s_jsonOptions)!;
            Assert.Equal(kind, deserialized.Kind);
        }
    }

    [Fact]
    public void JsonRoundTrip_CustomValue()
    {
        var result = new PermissionRequestResult { Kind = new PermissionRequestResultKind("custom") };
        var json = JsonSerializer.Serialize(result, s_jsonOptions);
        var deserialized = JsonSerializer.Deserialize<PermissionRequestResult>(json, s_jsonOptions)!;
        Assert.Equal("custom", deserialized.Kind.Value);
    }
}

[System.Text.Json.Serialization.JsonSerializable(typeof(PermissionRequestResult))]
internal partial class TestJsonContext : System.Text.Json.Serialization.JsonSerializerContext;

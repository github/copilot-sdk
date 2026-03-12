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
        Assert.Equal("approved", PermissionRequestResultKind.Approved.Value);
        Assert.Equal("denied-by-rules", PermissionRequestResultKind.DeniedByRules.Value);
        Assert.Equal("denied-no-approval-rule-and-could-not-request-from-user", PermissionRequestResultKind.DeniedCouldNotRequestFromUser.Value);
        Assert.Equal("denied-interactively-by-user", PermissionRequestResultKind.DeniedInteractivelyByUser.Value);
        Assert.Equal("no-result", new PermissionRequestResultKind("no-result").Value);
    }

    [Fact]
    public void Equals_SameValue_ReturnsTrue()
    {
        var a = new PermissionRequestResultKind("approved");
        Assert.True(a == PermissionRequestResultKind.Approved);
        Assert.True(a.Equals(PermissionRequestResultKind.Approved));
        Assert.True(a.Equals((object)PermissionRequestResultKind.Approved));
    }

    [Fact]
    public void Equals_DifferentValue_ReturnsFalse()
    {
        Assert.True(PermissionRequestResultKind.Approved != PermissionRequestResultKind.DeniedByRules);
        Assert.False(PermissionRequestResultKind.Approved.Equals(PermissionRequestResultKind.DeniedByRules));
    }

    [Fact]
    public void Equals_IsCaseInsensitive()
    {
        var upper = new PermissionRequestResultKind("APPROVED");
        Assert.Equal(PermissionRequestResultKind.Approved, upper);
    }

    [Fact]
    public void GetHashCode_IsCaseInsensitive()
    {
        var upper = new PermissionRequestResultKind("APPROVED");
        Assert.Equal(PermissionRequestResultKind.Approved.GetHashCode(), upper.GetHashCode());
    }

    [Fact]
    public void ToString_ReturnsValue()
    {
        Assert.Equal("approved", PermissionRequestResultKind.Approved.ToString());
        Assert.Equal("denied-by-rules", PermissionRequestResultKind.DeniedByRules.ToString());
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
        Assert.False(PermissionRequestResultKind.Approved.Equals("approved"));
    }

    [Fact]
    public void JsonSerialize_WritesStringValue()
    {
        var result = new PermissionRequestResult { Kind = PermissionRequestResultKind.Approved };
        var json = JsonSerializer.Serialize(result, s_jsonOptions);
        Assert.Contains("\"kind\":\"approved\"", json);
    }

    [Fact]
    public void JsonDeserialize_ReadsStringValue()
    {
        var json = """{"kind":"denied-by-rules"}""";
        var result = JsonSerializer.Deserialize<PermissionRequestResult>(json, s_jsonOptions)!;
        Assert.Equal(PermissionRequestResultKind.DeniedByRules, result.Kind);
    }

    [Fact]
    public void JsonRoundTrip_PreservesAllKinds()
    {
        var kinds = new[]
        {
            PermissionRequestResultKind.Approved,
            PermissionRequestResultKind.DeniedByRules,
            PermissionRequestResultKind.DeniedCouldNotRequestFromUser,
            PermissionRequestResultKind.DeniedInteractivelyByUser,
            new PermissionRequestResultKind("no-result"),
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

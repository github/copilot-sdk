/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Xunit;
using System.Reflection;

namespace GitHub.Copilot.Test.Unit;

public class RuntimeConnectionUrlParsingTests
{
    [Fact]
    public void ForUri_ParsesBracketedIpv6HostPort()
    {
        var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri("[::1]:9000")
        });

        Assert.Equal("::1", GetPrivateField<string>(client, "_optionsHost"));
        Assert.Equal(9000, GetPrivateField<int?>(client, "_optionsPort"));
    }

    [Fact]
    public void ForUri_ParsesHttpIpv6HostPort()
    {
        var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri("http://[::1]:7000")
        });

        Assert.Equal("::1", GetPrivateField<string>(client, "_optionsHost"));
        Assert.Equal(7000, GetPrivateField<int?>(client, "_optionsPort"));
    }

    [Fact]
    public void ForUri_RejectsUrlPath()
    {
        Assert.Throws<ArgumentException>(() => new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri("http://localhost:8080/path")
        }));
    }

    private static T? GetPrivateField<T>(object instance, string name)
    {
        var field = instance.GetType().GetField(name, BindingFlags.Instance | BindingFlags.NonPublic);
        Assert.NotNull(field);
        return (T?)field.GetValue(instance);
    }
}

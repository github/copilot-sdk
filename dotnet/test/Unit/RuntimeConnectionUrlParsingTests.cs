/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Xunit;
using System.Reflection;

namespace GitHub.Copilot.Test.Unit;

public class RuntimeConnectionUrlParsingTests
{
    [Theory]
    [InlineData("https://example.com:443")]
    [InlineData("HtTpS://example.com:443")]
    [InlineData("https://[::1]:9000")]
    [InlineData("https://localhost:9000")]
    public void ForUri_RejectsHttpsBeforeConnecting(string url)
    {
        var exception = Assert.Throws<ArgumentException>(() => new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(url, connectionToken: "connection-secret")
        }));

        Assert.Equal(
            "HTTPS runtime connections are not supported: this transport uses plaintext TCP, not TLS",
            exception.Message);
    }

    [Theory]
    [InlineData("8080")]
    [InlineData("localhost:9000")]
    [InlineData("http://localhost:7000")]
    [InlineData("[::1]:9000")]
    [InlineData("http://[::1]:7000")]
    [InlineData("copilot.example.com")]
    [InlineData("https:9000")]
    public void ForUri_AcceptsRawTcpAddresses(string url)
    {
        var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(url)
        });

        Assert.Null(client.RuntimePort);
    }

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

    private static T? GetPrivateField<T>(object instance, string name)
    {
        var field = instance.GetType().GetField(name, BindingFlags.Instance | BindingFlags.NonPublic);
        Assert.NotNull(field);
        return (T?)field.GetValue(instance);
    }
}

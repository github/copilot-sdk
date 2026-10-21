/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#if NET8_0_OR_GREATER

using System.Net;
using System.Net.Sockets;
using System.Net.WebSockets;
using System.Text;
using GitHub.Copilot.Test.E2E;
using Xunit;

namespace GitHub.Copilot.Test;

public class FakeCopilotUpstreamTests
{
    [Fact]
    public async Task RetainsItsListenerWhileServingHttpAndWebSockets()
    {
        using var deadline = new CancellationTokenSource(TimeSpan.FromSeconds(30));
        var ct = deadline.Token;
        await using var upstream = new FakeCopilotUpstream();
        var address = new Uri(upstream.BaseUrl);
        using var rival = new TcpListener(IPAddress.Loopback, address.Port);
        Assert.Throws<SocketException>(() => rival.Start());

        using var http = new HttpClient(new SocketsHttpHandler { UseProxy = false });
        using var response = await http.PostAsync(new Uri(address, "responses"), new StringContent("{}"), ct);
        Assert.True(response.Headers.ConnectionClose);
        Assert.Contains("OK from synthetic HTTP upstream", await response.Content.ReadAsStringAsync(ct));

        using var socket = new ClientWebSocket();
        socket.Options.Proxy = null;
        await socket.ConnectAsync(new UriBuilder(address) { Scheme = "ws", Path = "/responses" }.Uri,
            ct);
        await socket.SendAsync(Encoding.UTF8.GetBytes("{\"type\":\"response.create\"}"),
            WebSocketMessageType.Text, true, ct);
        var output = new StringBuilder();
        var buffer = new byte[16 * 1024];
        for (var index = 0; index < 6; index++)
        {
            ValueWebSocketReceiveResult message;
            do
            {
                message = await socket.ReceiveAsync(buffer.AsMemory(), ct);
                Assert.Equal(WebSocketMessageType.Text, message.MessageType);
                output.Append(Encoding.UTF8.GetString(buffer.AsSpan(0, message.Count)));
            }
            while (!message.EndOfMessage);
        }

        Assert.Contains("response.completed", output.ToString());
        Assert.Contains("OK from synthetic WS upstream", output.ToString());
        Assert.Equal(1, upstream.WsRequestMessageCount);
        await socket.CloseAsync(WebSocketCloseStatus.NormalClosure, null, ct);
    }

    [Theory]
    [InlineData("models?test=query", "\"model_picker_enabled\":true")]
    [InlineData("models/session", "{}")]
    [InlineData("policy", "{\"state\":\"enabled\"}")]
    public async Task ServesTheRuntimeBootstrapRoutes(string path, string expected)
    {
        using var deadline = new CancellationTokenSource(TimeSpan.FromSeconds(30));
        await using var upstream = new FakeCopilotUpstream();
        using var http = new HttpClient(new SocketsHttpHandler { UseProxy = false });
        var content = await http.GetStringAsync(new Uri(new Uri(upstream.BaseUrl), path), deadline.Token);
        Assert.Contains(expected, content);
    }
}

#endif

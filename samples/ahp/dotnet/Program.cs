using System.Net;
using System.Net.WebSockets;
using System.Text;
using GitHub.Copilot;
using GitHub.Copilot.Rpc;
using Microsoft.AspNetCore.Hosting.Server;
using Microsoft.AspNetCore.Hosting.Server.Features;

var runtimePath = args.FirstOrDefault() ?? Environment.GetEnvironmentVariable("COPILOT_CLI_PATH")
    ?? throw new ArgumentException("Pass the local copilot-runtime path or set COPILOT_CLI_PATH.");
await using var client = new CopilotClient(new CopilotClientOptions
{
    Connection = RuntimeConnection.ForStdio(path: runtimePath),
    GitHubToken = Environment.GetEnvironmentVariable("GITHUB_TOKEN") ?? Environment.GetEnvironmentVariable("GH_TOKEN")
});
await client.StartAsync();
await using var endpoint = await client.CreateAhpEndpointAsync();
await using var session = await client.CreateSessionAsync(new SessionConfig
{
    Model = Environment.GetEnvironmentVariable("COPILOT_MODEL") ?? "gpt-4.1",
    Streaming = true,
    SystemMessage = new()
    {
        Mode = SystemMessageMode.Replace,
        Content = "You are Bert. When asked who you are, reply exactly: I am Bert."
    },
    OnPermissionRequest = (_, _) => Task.FromResult(PermissionDecision.Reject())
});
var deltas = 0;
using var deltaSubscription = session.On<AssistantMessageDeltaEvent>(_ => Interlocked.Increment(ref deltas));
using var messageSubscription = session.On<AssistantMessageEvent>(message =>
{
    Console.WriteLine($"[SDK observed {session.SessionId}] {message.Data.Content}");
    Console.WriteLine($"[SDK streaming] {Interlocked.Exchange(ref deltas, 0)} message deltas");
});
using var errorSubscription = session.On<SessionErrorEvent>(error =>
    Console.Error.WriteLine($"[SDK session error] {error.Data.Message}"));

var builder = WebApplication.CreateBuilder();
builder.Logging.ClearProviders();
builder.WebHost.ConfigureKestrel(options => options.Listen(IPAddress.Loopback, 0));
await using var app = builder.Build();
app.UseWebSockets();
app.Map("/ahp", async context =>
{
    // Local demo only: any loopback process is trusted. Production hosts must authenticate before upgrade.
    if (context.Connection.RemoteIpAddress is not { } address || !IPAddress.IsLoopback(address))
    {
        context.Response.StatusCode = StatusCodes.Status403Forbidden;
        return;
    }
    if (!context.WebSockets.IsWebSocketRequest)
    {
        context.Response.StatusCode = StatusCodes.Status400BadRequest;
        return;
    }
    using var socket = await context.WebSockets.AcceptWebSocketAsync();
    var connection = endpoint.AcceptConnection(new SocketTransport(socket));
    var buffer = new byte[16 * 1024];
    try
    {
        while (socket.State is WebSocketState.Open or WebSocketState.CloseSent)
        {
            var frame = await socket.ReceiveAsync(buffer.AsMemory(), context.RequestAborted);
            if (frame.MessageType == WebSocketMessageType.Close) break;
            if (frame.MessageType != WebSocketMessageType.Text)
                throw new InvalidDataException("Only AHP text messages are supported.");
            await connection.ReceiveChunkAsync(buffer.AsMemory(0, frame.Count), frame.EndOfMessage, context.RequestAborted);
        }
    }
    catch (Exception error) when (error is WebSocketException or OperationCanceledException or IOException or ObjectDisposedException)
    {
        Console.Error.WriteLine($"AHP transport ended: {error.Message}");
        socket.Abort();
    }
    finally
    {
        try { await connection.EndAsync(); }
        catch (Exception error) when (error is not OutOfMemoryException and not StackOverflowException and not AccessViolationException)
        {
            Console.Error.WriteLine($"AHP connection cleanup failed: {error.Message}");
            socket.Abort();
        }
        try { await connection.Closed; }
        catch (Exception error) when (error is not OutOfMemoryException and not StackOverflowException and not AccessViolationException)
        {
            Console.Error.WriteLine($"AHP connection failed: {error.Message}");
        }
    }
});
await app.StartAsync();
var httpUrl = app.Services.GetRequiredService<IServer>().Features.Get<IServerAddressesFeature>()!.Addresses.Single();
var url = httpUrl.Replace("http://", "ws://", StringComparison.Ordinal) + "/ahp";
Console.WriteLine("DEMO AUTH: loopback-only; every local process is trusted. Do not expose this listener.");
Console.WriteLine($"AHP URL: {url}");
Console.WriteLine($"SDK session ID: {session.SessionId}");
Console.WriteLine($"In samples/ahp: npm run client -- '{url}' '{session.SessionId}'");
Console.WriteLine("Waiting for AHP prompts. Press Ctrl+C to stop.");
var stopping = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
using var shutdownRegistration = app.Lifetime.ApplicationStopping.Register(() => stopping.TrySetResult());
try
{
    await stopping.Task;
}
finally
{
    await endpoint.DisposeAsync();
    await app.StopAsync();
}

sealed class SocketTransport(WebSocket socket) : IAhpTransport
{
    public async Task SendAsync(string message, CancellationToken cancellationToken) =>
        await socket.SendAsync(Encoding.UTF8.GetBytes(message).AsMemory(),
            WebSocketMessageType.Text, true, cancellationToken);

    public async Task CloseAsync(Exception? error = null)
    {
        try
        {
            if (error is null && socket.State is WebSocketState.Open or WebSocketState.CloseReceived)
            {
                using var timeout = new CancellationTokenSource(TimeSpan.FromSeconds(1));
                await socket.CloseOutputAsync(WebSocketCloseStatus.NormalClosure, null, timeout.Token);
            }
        }
        finally
        {
            // Interrupt a pending receive even if the peer never completes the close handshake.
            socket.Abort();
        }
    }
}

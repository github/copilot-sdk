/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#if NET8_0_OR_GREATER
using System.Text.Json;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public sealed partial class ClientSessionLifetimeTests
{
    private sealed record HookArgsPayload(string Path, int Count);

    private static object? CreateHookPayload(string shape) => shape switch
    {
        "anonymous" => new { path = "/tmp/x", count = 3 },
        "poco" => new HookArgsPayload("/tmp/x", 3),
        "dictionary" => new Dictionary<string, object?> { ["path"] = "/tmp/x", ["count"] = 3, ["optional"] = null },
        "unset" => null,
        _ => throw new ArgumentOutOfRangeException(nameof(shape)),
    };

    private static async Task<JsonElement?> InvokeHookWithPayloadAsync(string hookType, string shape)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        var hooks = new SessionHooks
        {
            OnPreToolUse = (_, _) => Task.FromResult<PreToolUseHookOutput?>(new PreToolUseHookOutput
            {
                PermissionDecision = "allow",
                ModifiedArgs = CreateHookPayload(shape),
            }),
            OnPostToolUse = (_, _) => Task.FromResult<PostToolUseHookOutput?>(new PostToolUseHookOutput
            {
                ModifiedResult = CreateHookPayload(shape),
            }),
        };
        await using var session = await client.CreateSessionAsync(new SessionConfig { Hooks = hooks });
        var input = new Dictionary<string, object?>
        {
            ["sessionId"] = session.SessionId,
            ["timestamp"] = 1_730_000_000_000L,
            ["cwd"] = "workdir",
            ["toolName"] = "view",
            ["toolArgs"] = new Dictionary<string, object?> { ["path"] = "/tmp/original" },
        };
        if (hookType == "postToolUse")
        {
            input["toolResult"] = new Dictionary<string, object?> { ["resultType"] = "success", ["textResultForLlm"] = "original" };
        }
        var request = new Dictionary<string, object?>
        {
            ["sessionId"] = session.SessionId,
            ["hookType"] = hookType,
            ["input"] = input,
        };
        try
        {
            var result = await server.SendRequestAsync("hooks.invoke", request).WaitAsync(TimeSpan.FromSeconds(5));
            return result.GetProperty("output").Clone();
        }
        catch (InvalidOperationException) when (shape != "dictionary" && !JsonSerializer.IsReflectionEnabledByDefault)
        {
            // Without reflection (NativeAOT, or this suite's net8.0 configuration), a user type that no
            // source-generated context knows cannot be serialized, so the response is a JSON-RPC error.
            return null;
        }
    }

    [Theory]
    [InlineData("preToolUse", "modifiedArgs", "anonymous")]
    [InlineData("preToolUse", "modifiedArgs", "poco")]
    [InlineData("preToolUse", "modifiedArgs", "dictionary")]
    [InlineData("postToolUse", "modifiedResult", "anonymous")]
    [InlineData("postToolUse", "modifiedResult", "poco")]
    [InlineData("postToolUse", "modifiedResult", "dictionary")]
    public async Task HookPayload_Sends_User_Typed_Modified_Value(string hookType, string field, string shape)
    {
        var output = await InvokeHookWithPayloadAsync(hookType, shape);
        if (output is null)
        {
            Assert.False(JsonSerializer.IsReflectionEnabledByDefault);
            return;
        }

        var payload = output.Value.GetProperty(field);
        Assert.Equal("/tmp/x", payload.GetProperty("path").GetString());
        Assert.Equal(3, payload.GetProperty("count").GetInt32());
        if (shape == "dictionary")
        {
            Assert.Equal(JsonValueKind.Null, payload.GetProperty("optional").ValueKind);
        }
        if (hookType == "preToolUse")
        {
            Assert.Equal("allow", output.Value.GetProperty("permissionDecision").GetString());
        }
    }

    [Theory]
    [InlineData("preToolUse", "{\"permissionDecision\":\"allow\"}")]
    [InlineData("postToolUse", "{}")]
    public async Task HookPayload_Omits_Unset_Output_Fields(string hookType, string expected)
    {
        var output = await InvokeHookWithPayloadAsync(hookType, "unset");
        Assert.NotNull(output);
        Assert.Equal(expected, output.Value.GetRawText());
    }
}
#endif

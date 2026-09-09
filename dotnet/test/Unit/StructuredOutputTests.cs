/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#if NET8_0_OR_GREATER
using GitHub.Copilot.Rpc;
using System.Text.Json;
using System.Text.Json.Serialization;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public sealed partial class ClientSessionLifetimeTests
{
    [Theory]
    [InlineData("session")]
    [InlineData("rpc")]
    [InlineData("batch")]
    public async Task StructuredOutput_Raw_Format_Is_Forwarded_Without_Rewriting(string api)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await using var session = await client.CreateSessionAsync(new SessionConfig());
        using var document = JsonDocument.Parse("""{"type":"object","properties":{"value":{"type":"integer"}},"x-provider":{"anything":[true,42,null]}}""");
        var format = new ResponseFormat
        {
            Type = "json_schema",
            JsonSchema = new JsonSchemaResponseFormat
            {
                Name = "answer",
                Schema = document.RootElement.Clone(),
                Strict = false,
                Description = "An answer",
            },
        };
        var options = new MessageOptions { Prompt = "Answer", ResponseSchema = document.RootElement.Clone() };
        Assert.Equal(options.ResponseSchema, options.Clone().ResponseSchema);
        if (api == "batch")
        {
            await session.Rpc.SendMessagesAsync([new() { Prompt = "Answer" }], responseFormat: format);
        }
        else if (api == "rpc")
        {
            await session.Rpc.SendAsync("Answer", responseFormat: format);
        }
        else
        {
            await session.SendAsync(options);
        }
        var request = Assert.Single(server.Requests, r => r.Method == (api == "batch" ? "session.sendMessages" : "session.send"));
        var wireFormat = request.Params.GetProperty("responseFormat");
        Assert.Equal("json_schema", wireFormat.GetProperty("type").GetString());
        var jsonSchema = wireFormat.GetProperty("jsonSchema");
        Assert.Equal(api == "session" ? "response" : "answer", jsonSchema.GetProperty("name").GetString());
        if (api == "session")
        {
            Assert.False(jsonSchema.TryGetProperty("description", out _));
            Assert.True(jsonSchema.GetProperty("strict").GetBoolean());
        }
        else
        {
            Assert.Equal("An answer", jsonSchema.GetProperty("description").GetString());
            Assert.False(jsonSchema.GetProperty("strict").GetBoolean());
        }
        Assert.Equal(document.RootElement.GetRawText(), jsonSchema.GetProperty("schema").GetRawText());

        server.ClearRequests();
        await session.SendAsync("Ordinary text");
        Assert.False(Assert.Single(server.Requests, r => r.Method == "session.send").Params.TryGetProperty("responseFormat", out _));
    }

    [Fact]
    public async Task StructuredOutput_Uses_Default_Custom_Tool_Serialization_Options()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await using var session = await client.CreateSessionAsync(new SessionConfig());
        var task = session.SendAndWaitAsync<StructuredAnswer>("Answer");
        if (!JsonSerializer.IsReflectionEnabledByDefault)
        {
            await Assert.ThrowsAsync<NotSupportedException>(() => task);
            Assert.DoesNotContain(server.Requests, request => request.Method == "session.send");
            return;
        }
        var request = await WaitForRequestAsync(server, "session.send");
        var properties = request.Params.GetProperty("responseFormat").GetProperty("jsonSchema").GetProperty("schema").GetProperty("properties");
        Assert.True(properties.TryGetProperty("answer_text", out _));
        Assert.True(properties.TryGetProperty("count", out _));
        await SendStructuredAnswerAsync(server, session, "message-1", """{"answer_text":"correct","count":42}""");
        var result = await task.WaitAsync(TimeSpan.FromSeconds(5));
        Assert.Equal("correct", result.Answer);
        Assert.Equal(42, result.Count);
    }

    [Fact]
    public async Task StructuredOutput_Infers_Schema_Using_Serialization_Contract()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await using var session = await client.CreateSessionAsync(new SessionConfig());
        var options = new MessageOptions { Prompt = "Answer", RequestHeaders = new Dictionary<string, string> { ["x-test"] = "preserved" } };
        var task = session.SendAndWaitAsync<StructuredAnswer>(options, StructuredOutputJsonContext.Default.Options);
        var request = await WaitForRequestAsync(server, "session.send");
        Assert.Null(options.ResponseSchema);
        Assert.Equal("preserved", request.Params.GetProperty("requestHeaders").GetProperty("x-test").GetString());
        var format = request.Params.GetProperty("responseFormat").GetProperty("jsonSchema");
        Assert.True(format.GetProperty("strict").GetBoolean());
        var schema = format.GetProperty("schema");
        var properties = schema.GetProperty("properties");
        Assert.True(properties.TryGetProperty("answer_text", out _));
        Assert.True(properties.TryGetProperty("count", out _));
        Assert.True(properties.TryGetProperty("note", out var note));
        Assert.Contains("null", note.GetProperty("type").EnumerateArray().Select(t => t.GetString()));
        Assert.False(schema.GetProperty("additionalProperties").GetBoolean());
        Assert.Equal(3, schema.GetProperty("required").GetArrayLength());
        await SendStructuredAnswerAsync(server, session, "message-1", """{"answer_text":"correct","count":42,"note":null}""");
        var result = await task.WaitAsync(TimeSpan.FromSeconds(5));
        Assert.Equal("correct", result.Answer);
        Assert.Equal(42, result.Count);
        Assert.Null(result.Note);
    }

    [Theory]
    [InlineData("not JSON")]
    [InlineData("""{"answer_text":"wrong","count":"not a number"}""")]
    [InlineData("null")]
    [InlineData("""{"count":42}""")]
    public async Task StructuredOutput_Rejects_Unparseable_Or_Null_Result(string content)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await using var session = await client.CreateSessionAsync(new SessionConfig());
        var task = session.SendAndWaitAsync<StructuredAnswer>("Answer", StructuredOutputJsonContext.Default.Options);
        await WaitForRequestAsync(server, "session.send");
        await SendStructuredAnswerAsync(server, session, "message-1", content);
        await Assert.ThrowsAsync<JsonException>(() => task.WaitAsync(TimeSpan.FromSeconds(5)));
    }

    [Fact]
    public async Task StructuredOutput_Rejects_Conflicting_Options_Before_Sending()
    {
        using var schema = JsonDocument.Parse("{}");
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await using var session = await client.CreateSessionAsync(new SessionConfig());
        await Assert.ThrowsAsync<ArgumentException>(() => session.SendAndWaitAsync<StructuredAnswer>(
            new MessageOptions { Prompt = "Answer", Mode = "immediate" }, StructuredOutputJsonContext.Default.Options));
        await Assert.ThrowsAsync<ArgumentException>(() => session.SendAndWaitAsync<StructuredAnswer>(
            new MessageOptions { Prompt = "Answer", ResponseSchema = schema.RootElement.Clone() }, StructuredOutputJsonContext.Default.Options));
        Assert.DoesNotContain(server.Requests, r => r.Method == "session.send");
    }

    [Theory]
    [InlineData(false)]
    [InlineData(true)]
    public async Task StructuredOutput_Correlates_Concurrent_Queued_Sends(bool typed)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.UniqueMessageIds = true;
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await using var session = await client.CreateSessionAsync(new SessionConfig());
        async Task<StructuredAnswer> SendAsync(string prompt)
        {
            if (typed)
            {
                return await session.SendAndWaitAsync<StructuredAnswer>(prompt, StructuredOutputJsonContext.Default.Options);
            }
            using var schema = JsonDocument.Parse("""{"type":"object","properties":{"answer_text":{"type":"string"},"count":{"type":"integer"}},"required":["answer_text","count"],"additionalProperties":false}""");
            var message = await session.SendAndWaitAsync(new MessageOptions { Prompt = prompt, ResponseSchema = schema.RootElement.Clone() });
            Assert.NotNull(message);
            return JsonSerializer.Deserialize(message.Data.Content, StructuredOutputJsonContext.Default.StructuredAnswer)!;
        }
        var first = SendAsync("First");
        await WaitForRequestAsync(server, "session.send");
        server.ClearRequests();
        var second = SendAsync("Second");
        await WaitForRequestAsync(server, "session.send");

        await SendStructuredAnswerAsync(server, session, "message-1", """{"answer_text":"first","count":1}""");
        Assert.Equal("first", (await first.WaitAsync(TimeSpan.FromSeconds(5))).Answer);
        Assert.False(second.IsCompleted);
        await SendStructuredAnswerAsync(server, session, "message-2", """{"answer_text":"second","count":2}""");
        Assert.Equal("second", (await second.WaitAsync(TimeSpan.FromSeconds(5))).Answer);
    }

    [Fact]
    public async Task StructuredOutput_Buffers_Events_Before_Send_Response()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await using var session = await client.CreateSessionAsync(new SessionConfig());
        server.BeforeSendResponse = messageId =>
            SendStructuredAnswerAsync(server, session, messageId, """{"answer_text":"early","count":42}""");
        var result = await session.SendAndWaitAsync<StructuredAnswer>(
            "Answer", StructuredOutputJsonContext.Default.Options, TimeSpan.FromSeconds(5));
        Assert.Equal("early", result.Answer);
    }

    [Fact]
    public async Task StructuredOutput_Ignores_Idle_Until_Own_Message_Is_Consumed()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await using var session = await client.CreateSessionAsync(new SessionConfig());
        var task = session.SendAndWaitAsync<StructuredAnswer>("Answer", StructuredOutputJsonContext.Default.Options);
        await WaitForRequestAsync(server, "session.send");
        await server.SendSessionEventAsync(session.SessionId, "session.idle", new());
        await server.SendSessionEventAsync(session.SessionId, "session.error", new()
        {
            ["errorType"] = "provider",
            ["message"] = "another turn failed",
        });
        await SendStructuredAnswerAsync(server, session, "another-message", """{"answer_text":"wrong","count":0}""");
        await server.SendSessionEventAsync(session.SessionId, "user.message", new()
        {
            ["messageId"] = "message-1",
            ["content"] = "Answer",
        }, agentId: "subagent");
        await server.SendSessionEventAsync(session.SessionId, "session.idle", new());
        await SendStructuredAnswerAsync(server, session, "message-1", """{"answer_text":"correct","count":42}""");
        Assert.Equal("correct", (await task.WaitAsync(TimeSpan.FromSeconds(5))).Answer);
    }

    [Fact]
    public async Task StructuredOutput_Ignores_Subagent_Completion_And_Autopilot_Idle()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await using var session = await client.CreateSessionAsync(new SessionConfig());
        var task = session.SendAndWaitAsync<StructuredAnswer>("Answer", StructuredOutputJsonContext.Default.Options);
        await WaitForRequestAsync(server, "session.send");
        await server.SendSessionEventAsync(session.SessionId, "user.message", new()
        {
            ["messageId"] = "message-1",
            ["content"] = "Answer",
        });
        await server.SendSessionEventAsync(session.SessionId, "session.idle", new(), agentId: "subagent");
        await server.SendSessionEventAsync(session.SessionId, "session.error", new()
        {
            ["errorType"] = "provider",
            ["message"] = "subagent failed",
        }, agentId: "subagent");
        await server.SendSessionEventAsync(session.SessionId, "session.idle", new() { ["mode"] = "autopilot" });
        await SendStructuredAnswerAsync(server, session, "message-1", """{"answer_text":"correct","count":42}""");
        Assert.Equal("correct", (await task.WaitAsync(TimeSpan.FromSeconds(5))).Answer);
    }

    [Theory]
    [InlineData(false)]
    [InlineData(true)]
    public async Task StructuredOutput_Rejects_Missing_Final_Response(bool toolOnly)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await using var session = await client.CreateSessionAsync(new SessionConfig());
        var task = session.SendAndWaitAsync<StructuredAnswer>("Answer", StructuredOutputJsonContext.Default.Options);
        await WaitForRequestAsync(server, "session.send");
        await server.SendSessionEventAsync(session.SessionId, "user.message", new()
        {
            ["messageId"] = "message-1",
            ["content"] = "Answer",
        });
        if (toolOnly)
        {
            await server.SendSessionEventAsync(session.SessionId, "assistant.message", new()
            {
                ["messageId"] = "tool-message",
                ["originatingMessageId"] = "message-1",
                ["content"] = """{"answer_text":"not final","count":42}""",
                ["toolRequests"] = new[] { new Dictionary<string, object?> { ["toolCallId"] = "tool-1", ["name"] = "terminal_tool", ["arguments"] = new Dictionary<string, object?>() } },
            });
        }
        await server.SendSessionEventAsync(session.SessionId, "session.idle", new());
        var error = await Assert.ThrowsAsync<InvalidOperationException>(() => task.WaitAsync(TimeSpan.FromSeconds(5)));
        Assert.Contains("without a final structured response", error.Message);
    }

    [Theory]
    [InlineData(false)]
    [InlineData(true)]
    public async Task StructuredOutput_Uses_Last_Correlated_Message_Not_Subagent_Or_Tool_Commentary(bool laterWorkAborted)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await using var session = await client.CreateSessionAsync(new SessionConfig());
        var task = session.SendAndWaitAsync<StructuredAnswer>("Answer", StructuredOutputJsonContext.Default.Options);
        await WaitForRequestAsync(server, "session.send");
        await server.SendSessionEventAsync(session.SessionId, "user.message", new()
        {
            ["messageId"] = "message-1",
            ["content"] = "Answer",
        });
        foreach (var (origin, content) in new[]
        {
            ("message-1", "First I will inspect the inventory."),
            ("message-1", """{"answer_text":"final","count":42}"""),
            ("subagent-message", """{"answer_text":"wrong","count":0}"""),
            ("unrelated-queued-message", """{"answer_text":"also wrong","count":0}"""),
        })
        {
            await server.SendSessionEventAsync(session.SessionId, "assistant.message", new()
            {
                ["messageId"] = Guid.NewGuid().ToString(),
                ["originatingMessageId"] = origin,
                ["content"] = content,
            });
        }
        await server.SendSessionEventAsync(session.SessionId, "assistant.message", new()
        {
            ["messageId"] = "subagent-output",
            ["originatingMessageId"] = "message-1",
            ["content"] = """{"answer_text":"subagent must not win","count":0}""",
        }, agentId: "subagent-1");
        await server.SendSessionEventAsync(session.SessionId, "session.idle", new() { ["aborted"] = laterWorkAborted });
        if (laterWorkAborted)
        {
            var error = await Assert.ThrowsAsync<InvalidOperationException>(() => task.WaitAsync(TimeSpan.FromSeconds(5)));
            Assert.Contains("aborted", error.Message);
        }
        else
        {
            Assert.Equal("final", (await task.WaitAsync(TimeSpan.FromSeconds(5))).Answer);
        }
    }

    [Theory]
    [InlineData(false)]
    [InlineData(true)]
    public async Task StructuredOutput_Preserves_Timeout_And_Cancellation(bool cancel)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await using var session = await client.CreateSessionAsync(new SessionConfig());
        using var cts = new CancellationTokenSource();
        var task = session.SendAndWaitAsync<StructuredAnswer>("Answer", StructuredOutputJsonContext.Default.Options,
            cancel ? TimeSpan.FromSeconds(10) : TimeSpan.FromMilliseconds(100), cts.Token);
        await WaitForRequestAsync(server, "session.send");
        if (cancel)
        {
            cts.Cancel();
            await Assert.ThrowsAnyAsync<OperationCanceledException>(() => task);
        }
        else
        {
            await Assert.ThrowsAsync<TimeoutException>(() => task);
        }
    }

    [Theory]
    [InlineData(false)]
    [InlineData(true)]
    public async Task StructuredOutput_Propagates_Rpc_And_Session_Errors(bool rpcError)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await using var session = await client.CreateSessionAsync(new SessionConfig());
        if (rpcError)
        {
            server.FailSessionSend();
        }
        var task = session.SendAndWaitAsync<StructuredAnswer>("Answer", StructuredOutputJsonContext.Default.Options);
        await WaitForRequestAsync(server, "session.send");
        if (!rpcError)
        {
            await server.SendSessionEventAsync(session.SessionId, "user.message", new()
            {
                ["messageId"] = "message-1",
                ["content"] = "Answer",
            });
            await server.SendSessionEventAsync(session.SessionId, "session.error", new()
            {
                ["errorType"] = "provider",
                ["message"] = "structured output unsupported",
            });
            var error = await Assert.ThrowsAsync<InvalidOperationException>(() => task.WaitAsync(TimeSpan.FromSeconds(5)));
            Assert.Contains("structured output unsupported", error.Message);
        }
        else
        {
            var error = await Assert.ThrowsAsync<IOException>(() => task.WaitAsync(TimeSpan.FromSeconds(5)));
            Assert.Contains("session send failed", error.Message);
        }
    }

    [Fact]
    public async Task StructuredOutput_Final_Reply_Does_Not_Hide_Later_Session_Errors()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await using var session = await client.CreateSessionAsync(new SessionConfig());
        var task = session.SendAndWaitAsync<StructuredAnswer>("Answer", StructuredOutputJsonContext.Default.Options);
        await WaitForRequestAsync(server, "session.send");
        await server.SendSessionEventAsync(session.SessionId, "user.message", new()
        {
            ["messageId"] = "message-1",
            ["content"] = "Answer",
        });
        await server.SendSessionEventAsync(session.SessionId, "assistant.message", new()
        {
            ["messageId"] = "final-reply",
            ["originatingMessageId"] = "message-1",
            ["isFinalReply"] = true,
            ["content"] = """{"answer_text":"correct","count":42}""",
        });
        await server.SendSessionEventAsync(session.SessionId, "session.error", new()
        {
            ["errorType"] = "query",
            ["message"] = "post-response failure",
        });
        await server.SendSessionEventAsync(session.SessionId, "session.idle", new());
        var error = await Assert.ThrowsAsync<InvalidOperationException>(() => task.WaitAsync(TimeSpan.FromSeconds(5)));
        Assert.Contains("post-response failure", error.Message);
    }

    [Fact]
    public async Task StructuredOutput_Can_Correlate_Without_User_Message_Event()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await using var session = await client.CreateSessionAsync(new SessionConfig());
        var task = session.SendAndWaitAsync<StructuredAnswer>("Answer", StructuredOutputJsonContext.Default.Options);
        await WaitForRequestAsync(server, "session.send");
        await server.SendSessionEventAsync(session.SessionId, "assistant.message", new()
        {
            ["messageId"] = "assistant-result",
            ["originatingMessageId"] = "message-1",
            ["content"] = """{"answer_text":"correct","count":42}""",
        });
        await server.SendSessionEventAsync(session.SessionId, "session.idle", new());
        Assert.Equal("correct", (await task.WaitAsync(TimeSpan.FromSeconds(5))).Answer);
    }

    [Theory]
    [InlineData(false)]
    [InlineData(true)]
    public async Task StructuredOutput_Rejects_When_Connection_Or_Session_Closes(bool disposeSession)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await using var session = await client.CreateSessionAsync(new SessionConfig());
        var task = session.SendAndWaitAsync<StructuredAnswer>("Answer", StructuredOutputJsonContext.Default.Options);
        await WaitForRequestAsync(server, "session.send");
        if (disposeSession)
        {
            await session.DisposeAsync();
        }
        else
        {
            server.CloseConnection();
        }
        try
        {
            await Assert.ThrowsAsync<IOException>(() => task.WaitAsync(TimeSpan.FromSeconds(5)));
        }
        finally
        {
            // Graceful cleanup cannot wait for a peer whose transport was deliberately closed.
            await client.ForceStopAsync();
        }
    }

    [Fact]
    public async Task StructuredOutput_Timeout_Includes_Send_Acknowledgement()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await using var session = await client.CreateSessionAsync(new SessionConfig());
        var release = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        server.BeforeSendResponse = _ => release.Task;
        try
        {
            await Assert.ThrowsAsync<TimeoutException>(() => session.SendAndWaitAsync<StructuredAnswer>(
                "Answer", StructuredOutputJsonContext.Default.Options, TimeSpan.FromMilliseconds(100)));
        }
        finally
        {
            release.TrySetResult();
        }
    }

    private static async Task SendStructuredAnswerAsync(FakeCopilotServer server, CopilotSession session, string messageId, string content)
    {
        await server.SendSessionEventAsync(session.SessionId, "user.message", new()
        {
            ["messageId"] = messageId,
            ["content"] = "Answer",
        });
        await server.SendSessionEventAsync(session.SessionId, "assistant.message", new()
        {
            ["messageId"] = Guid.NewGuid().ToString(),
            ["originatingMessageId"] = messageId,
            ["content"] = content,
        });
        await server.SendSessionEventAsync(session.SessionId, "session.idle", new());
    }

    public sealed class StructuredAnswer
    {
        [JsonPropertyName("answer_text")]
        public required string Answer { get; set; }
        public int Count { get; set; }
        public string? Note { get; set; }
    }

    [JsonSourceGenerationOptions(PropertyNamingPolicy = JsonKnownNamingPolicy.CamelCase)]
    [JsonSerializable(typeof(StructuredAnswer))]
    internal sealed partial class StructuredOutputJsonContext : JsonSerializerContext;
}
#endif

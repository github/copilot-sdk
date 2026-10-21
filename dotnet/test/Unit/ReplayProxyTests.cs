/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Text.Json;
using System.Text.Json.Serialization;
using System.Text.Json.Nodes;
using System.Net.Http;
using System.Text;
using GitHub.Copilot.Test.Harness;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public partial class ReplayProxyTests
{
    [Fact]
    public async Task ReplayMissPreservesContextDirectoryCleanup()
    {
        var context = await E2ETestContext.CreateAsync();
        using var client = new HttpClient();
        try
        {
            using var config = new StringContent(
                new JsonObject
                {
                    ["filePath"] = Path.Join(context.WorkDir, "missing.yaml"),
                    ["workDir"] = context.WorkDir,
                    ["replayOnly"] = true,
                }.ToJsonString(), Encoding.UTF8, "application/json");
            using var configured = await client.PostAsync($"{context.ProxyUrl}/config", config);
            configured.EnsureSuccessStatusCode();
            using var body = new StringContent(
                """{"model":"gpt-4.1","messages":[{"role":"user","content":"UNMATCHED_INFERENCE"}]}""",
                Encoding.UTF8, "application/json");
            using var response = await client.PostAsync($"{context.ProxyUrl}/chat/completions", body);
            Assert.Equal(System.Net.HttpStatusCode.InternalServerError, response.StatusCode);
            var error = await Assert.ThrowsAsync<HttpRequestException>(() => context.DisposeAsync().AsTask());
            Assert.Contains("No cached response found", error.Message);
            Assert.False(Directory.Exists(context.HomeDir));
            Assert.False(Directory.Exists(context.WorkDir));
        }
        finally
        {
            await context.DisposeAsync();
        }
    }

    [Fact]
    public async Task ReplayMissFailsShutdownAfterProcessCleanup()
    {
        var directory = Path.Join(Path.GetTempPath(), "sdk-replay-failure-" + Guid.NewGuid().ToString("N"));
        Directory.CreateDirectory(directory);
        await using var proxy = new ReplayProxy();
        try
        {
            var url = await proxy.StartAsync();
            await proxy.ConfigureAsync(Path.Join(directory, "missing.yaml"), directory, "capi", replayOnly: true);
            using var client = new HttpClient();
            using var body = new StringContent(
                """{"model":"gpt-4.1","messages":[{"role":"user","content":"UNMATCHED_INFERENCE"}]}""",
                Encoding.UTF8, "application/json");
            using var response = await client.PostAsync($"{url}/chat/completions", body);
            Assert.Equal(System.Net.HttpStatusCode.InternalServerError, response.StatusCode);
            var error = await Assert.ThrowsAsync<HttpRequestException>(() => proxy.StopAsync(skipWritingCache: true));
            Assert.Contains("No cached response found", error.Message);
            await proxy.StopAsync(skipWritingCache: true);
            await Assert.ThrowsAsync<HttpRequestException>(() => client.GetAsync(url));
        }
        finally
        {
            Directory.Delete(directory, recursive: true);
        }
    }

    [Theory]
    [InlineData("37", 37L)]
    [InlineData("0", 0L)]
    [InlineData("", null)]
    [InlineData("null", null)]
    public void CapturedCompactionUsagePreservesInputCounts(string inputTokens, long? expectedInputTokens)
    {
        var inputProperty = inputTokens.Length == 0 ? string.Empty : ",\"inputTokens\":" + inputTokens;
        var json = "[{\"request\":{\"model\":\"test-model\",\"messages\":[]},"
            + "\"response\":{\"id\":\"compaction-response\",\"model\":\"test-model\","
            + "\"choices\":[]},"
            + "\"compactionUsage\":{\"interactionId\":\"compaction-interaction\","
            + "\"summary\":\"summary\",\"responseCount\":2" + inputProperty + "},"
            + "\"requestHeaders\":{\"x-interaction-type\":\"conversation-compaction\"}}]";
        var exchanges = JsonSerializer.Deserialize(json, ReplayProxyTestJsonContext.Default.ListParsedHttpExchange);
        Assert.NotNull(exchanges);
        var exchange = Assert.Single(exchanges);
        Assert.NotNull(exchange.RequestHeaders);
        Assert.Equal("conversation-compaction", exchange.RequestHeaders["x-interaction-type"].GetString());
        Assert.NotNull(exchange.CompactionUsage);
        Assert.Equal("compaction-interaction", exchange.CompactionUsage.InteractionId);
        Assert.Equal("summary", exchange.CompactionUsage.Summary);
        Assert.Equal(2, exchange.CompactionUsage.ResponseCount);
        Assert.Equal(expectedInputTokens, exchange.CompactionUsage.InputTokens);
    }

    [JsonSourceGenerationOptions(JsonSerializerDefaults.Web)]
    [JsonSerializable(typeof(List<ParsedHttpExchange>))]
    private partial class ReplayProxyTestJsonContext : JsonSerializerContext;
}

/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Text.Json;
using System.Text.Json.Nodes;
using System.Text.Json.Serialization;
using System.Text.Json.Serialization.Metadata;
using GitHub.Copilot.Rpc;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public class WorkerCausalityTests
{
    private static readonly JsonNode Corpus = JsonNode.Parse(File.ReadAllText(
        Path.Combine(AppContext.BaseDirectory, "worker-causality.json")))!;
    private static readonly JsonSerializerOptions RpcOptions = new()
    {
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
        DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull,
        TypeInfoResolver = new DefaultJsonTypeInfoResolver(),
    };

    public static IEnumerable<object[]> Cases => Corpus["valid"]!.AsArray().Select(
        value => new object[] { value!["name"]!.GetValue<string>(), value!.ToJsonString() });

    private static string Decode(JsonNode value, bool isEvent)
    {
        var wire = value.ToJsonString();
        return isEvent ? SessionEvent.FromJson(wire).ToJson() :
            JsonSerializer.Serialize(JsonSerializer.Deserialize<TasksSendMessageResult>(wire, RpcOptions), RpcOptions);
    }

    private static JsonObject Payload(JsonNode value, bool isEvent) =>
        (isEvent ? value["data"]! : value).AsObject();

    [Theory]
    [MemberData(nameof(Cases))]
    public void PublicReadersPreserveSourceAndHistoricalProductBytes(string name, string fixture)
    {
        var test = JsonNode.Parse(fixture)!;
        var isEvent = test["event"] is not null;
        var wire = (test[isEvent ? "event" : "result"]!).DeepClone();
        var observed = JsonNode.Parse(Decode(wire, isEvent))!;
        Assert.True(JsonNode.DeepEquals(Payload(observed, isEvent)["workerCausality"],
            Payload(wire, isEvent)["workerCausality"]), name);

        Payload(wire, isEvent).Remove("workerCausality");
        var baseline = Decode(wire, isEvent);
        foreach (var invalid in Corpus["invalid"]!.AsArray())
        {
            Payload(wire, isEvent)["workerCausality"] = invalid!["value"]?.DeepClone();
            Assert.Equal(baseline, Decode(wire, isEvent));
        }
    }

    [Fact]
    public void ExactUtf8BudgetAndUnknownFields()
    {
        foreach (var test in Corpus["boundaries"]!.AsArray())
        {
            var wire = Corpus["valid"]![0]!["event"]!.DeepClone();
            Payload(wire, true)["workerCausality"] = test!["value"]!.DeepClone();
            var observed = Payload(JsonNode.Parse(Decode(wire, true))!, true);
            Assert.True(
                observed.ContainsKey("workerCausality") == test["accepted"]!.GetValue<bool>(),
                test["name"]!.GetValue<string>());
            Assert.Equal(Payload(wire, true)["content"]!.GetValue<string>(), observed["content"]!.GetValue<string>());
        }
    }
}

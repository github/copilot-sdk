/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Text.Json;
using System.Text.Json.Serialization.Metadata;
using GitHub.Copilot.Rpc;
using Xunit;

#pragma warning disable GHCP001 // Exercise the public model RPC projection.

namespace GitHub.Copilot.Test.Unit;

public class AutoTierIdentityTests
{
    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        TypeInfoResolver = new DefaultJsonTypeInfoResolver(),
    };

    [Fact]
    public void CaseDistinctTierIdsRemainDistinctAcrossCollectionsAndJson()
    {
        var custom = new AutoTier("BALANCE");
        Assert.NotEqual(AutoTier.Balance, custom);
        Assert.Equal(2, new HashSet<AutoTier> { AutoTier.Balance, custom }.Count);
        Assert.Equal("\"BALANCE\"", JsonSerializer.Serialize(custom, JsonOptions));
        Assert.Equal(custom, JsonSerializer.Deserialize<AutoTier>("\"BALANCE\"", JsonOptions));
        Assert.Equal("balance", AutoTier.Balance.Value);

        var rpc = JsonSerializer.Deserialize<CurrentModel>("""{"modelId":"auto","autoTier":"BALANCE"}""", JsonOptions);
        Assert.NotNull(rpc);
        Assert.Equal(custom, rpc.AutoTier);
        Assert.NotEqual(AutoTier.Balance, rpc.AutoTier);
    }

    [Fact]
    public void CaseDistinctRecommendationIdsRemainDistinct()
    {
        var custom = new RecommendedAutoTier("BALANCE");
        Assert.NotEqual(RecommendedAutoTier.Balance, custom);
        Assert.Equal(2, new HashSet<RecommendedAutoTier> { RecommendedAutoTier.Balance, custom }.Count);
        Assert.Equal("\"BALANCE\"", JsonSerializer.Serialize(custom, JsonOptions));
        Assert.Equal(custom, JsonSerializer.Deserialize<RecommendedAutoTier>("\"BALANCE\"", JsonOptions));
    }
}

// Copyright (c) Microsoft Corporation. All rights reserved.

#if NET8_0_OR_GREATER
using System.Text;
using GitHub.Copilot.Rpc;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public sealed partial class ClientSessionLifetimeTests
{
    [Theory]
    [InlineData(2L, 2L)]
    [InlineData(4L, null)]
    [InlineData(3L, 0L)]
    [InlineData(7L, 20L)]
    [InlineData(9L, null)]
    [InlineData(100L, 2L)]
    public async Task RangeOnlySessionFsProvider_PreservesBytePositionsAndEof(long offset, long? length)
    {
        var data = Encoding.UTF8.GetBytes("a☃🚀z");
        var available = Math.Max(0, data.Length - offset);
        var expected = data.AsMemory((int)Math.Min(offset, data.Length), (int)Math.Min(length ?? available, available));
        var calls = 0;
        var provider = new RangeOnlySessionFsProvider((path, start, count, _) =>
        {
            Assert.Equal("/file", path);
            Assert.Equal(offset, start);
            Assert.Equal(length, count);
            calls++;
            return Task.FromResult(new SessionFsByteRange { Content = expected, ByteLength = data.Length });
        });
        Assert.False((object)provider is ISessionFsBinaryProvider);
        var result = await ((ISessionFsReadFileRangeHandler)provider).ReadFileRangeAsync(
            new SessionFsReadFileRangeRequest { SessionId = "session", Path = "/file", Offset = offset, Length = length });
        Assert.Null(result.Error);
        Assert.Equal(data.Length, result.ByteLength);
        Assert.Equal(expected.ToArray(), Convert.FromBase64String(result.Content));
        Assert.Equal(1, calls);
    }

    [Theory]
    [InlineData(-1L)]
    [InlineData(9_007_199_254_740_992L)]
    public async Task RangeSessionFsProvider_RejectsInvalidRequestsBeforeInvokingProvider(long position)
    {
        var provider = new RangeOnlySessionFsProvider((_, _, _, _) => throw new InvalidOperationException("must not invoke"));
        var handler = (ISessionFsReadFileRangeHandler)provider;
        foreach (var request in new[]
        {
            new SessionFsReadFileRangeRequest { SessionId = "session", Path = "/file", Offset = position },
            new SessionFsReadFileRangeRequest { SessionId = "session", Path = "/file", Offset = 0, Length = position },
        })
        {
            var result = await handler.ReadFileRangeAsync(request);
            Assert.NotNull(result.Error);
            Assert.Contains("safe integers", result.Error.Message);
        }
    }

    [Fact]
    public async Task RangeSessionFsProvider_RejectsMalformedResponsesAndPreservesCancellation()
    {
        var provider = new RangeOnlySessionFsProvider((_, _, _, _) =>
            Task.FromResult(new SessionFsByteRange { Content = new byte[1], ByteLength = 5 }));
        var request = new SessionFsReadFileRangeRequest { SessionId = "session", Path = "/file", Offset = 1, Length = 2 };
        var result = await ((ISessionFsReadFileRangeHandler)provider).ReadFileRangeAsync(request);
        Assert.NotNull(result.Error);
        Assert.Contains("incorrect byte range length", result.Error.Message);
        var legacy = new TextOnlySessionFsProvider();
        result = await ((ISessionFsReadFileRangeHandler)legacy).ReadFileRangeAsync(request);
        Assert.NotNull(result.Error);
        Assert.Contains("not supported", result.Error.Message);
        provider = new RangeOnlySessionFsProvider((_, _, _, token) => Task.FromCanceled<SessionFsByteRange>(token));
        await Assert.ThrowsAnyAsync<OperationCanceledException>(() =>
            ((ISessionFsReadFileRangeHandler)provider).ReadFileRangeAsync(request, new CancellationToken(true)));
    }

    private sealed class RangeOnlySessionFsProvider(
        Func<string, long, long?, CancellationToken, Task<SessionFsByteRange>> read)
        : TextOnlySessionFsProvider, ISessionFsReadFileRangeProvider
    {
        public Task<SessionFsByteRange> ReadFileRangeAsync(string path, long offset, long? length, CancellationToken cancellationToken) =>
            read(path, offset, length, cancellationToken);
    }
}
#endif

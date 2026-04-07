/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Xunit;

namespace GitHub.Copilot.SDK.Test;

public class SessionFsTests
{
    [Fact]
    public void SessionFsConfig_CanBeSetOnClientOptions()
    {
        var options = new CopilotClientOptions
        {
            SessionFs = new SessionFsConfig
            {
                InitialCwd = "/home/user/project",
                SessionStatePath = "/session-state",
                Conventions = SessionFsConventions.Posix,
            }
        };

        Assert.NotNull(options.SessionFs);
        Assert.Equal("/home/user/project", options.SessionFs.InitialCwd);
        Assert.Equal("/session-state", options.SessionFs.SessionStatePath);
        Assert.Equal(SessionFsConventions.Posix, options.SessionFs.Conventions);
    }

    [Fact]
    public void SessionFsConfig_CopiedInClone()
    {
        var original = new CopilotClientOptions
        {
            SessionFs = new SessionFsConfig
            {
                InitialCwd = "/",
                SessionStatePath = "/state",
                Conventions = SessionFsConventions.Windows,
            }
        };

        var clone = original.Clone();

        Assert.NotNull(clone.SessionFs);
        Assert.Same(original.SessionFs, clone.SessionFs);
    }

    [Fact]
    public void SessionConfig_HasCreateSessionFsHandler()
    {
        var config = new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            CreateSessionFsHandler = _ => new TestSessionFsHandler(),
        };

        Assert.NotNull(config.CreateSessionFsHandler);
    }

    [Fact]
    public void ResumeSessionConfig_HasCreateSessionFsHandler()
    {
        var config = new ResumeSessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            CreateSessionFsHandler = _ => new TestSessionFsHandler(),
        };

        Assert.NotNull(config.CreateSessionFsHandler);
    }

    [Fact]
    public void CreateSessionFsHandler_CopiedInSessionConfigClone()
    {
        Func<CopilotSession, ISessionFsHandler> factory = _ => new TestSessionFsHandler();
        var original = new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            CreateSessionFsHandler = factory,
        };

        var clone = original.Clone();

        Assert.Same(factory, clone.CreateSessionFsHandler);
    }

    [Fact]
    public void CreateSessionFsHandler_CopiedInResumeSessionConfigClone()
    {
        Func<CopilotSession, ISessionFsHandler> factory = _ => new TestSessionFsHandler();
        var original = new ResumeSessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            CreateSessionFsHandler = factory,
        };

        var clone = original.Clone();

        Assert.Same(factory, clone.CreateSessionFsHandler);
    }

    private class TestSessionFsHandler : ISessionFsHandler
    {
        public Task<SessionFsReadFileResult> ReadFileAsync(SessionFsReadFileParams request, CancellationToken cancellationToken = default)
            => Task.FromResult(new SessionFsReadFileResult { Content = "" });
        public Task WriteFileAsync(SessionFsWriteFileParams request, CancellationToken cancellationToken = default) => Task.CompletedTask;
        public Task AppendFileAsync(SessionFsAppendFileParams request, CancellationToken cancellationToken = default) => Task.CompletedTask;
        public Task<SessionFsExistsResult> ExistsAsync(SessionFsExistsParams request, CancellationToken cancellationToken = default)
            => Task.FromResult(new SessionFsExistsResult { Exists = false });
        public Task<SessionFsStatResult> StatAsync(SessionFsStatParams request, CancellationToken cancellationToken = default)
            => Task.FromResult(new SessionFsStatResult());
        public Task MkdirAsync(SessionFsMkdirParams request, CancellationToken cancellationToken = default) => Task.CompletedTask;
        public Task<SessionFsReaddirResult> ReaddirAsync(SessionFsReaddirParams request, CancellationToken cancellationToken = default)
            => Task.FromResult(new SessionFsReaddirResult());
        public Task<SessionFsReaddirWithTypesResult> ReaddirWithTypesAsync(SessionFsReaddirWithTypesParams request, CancellationToken cancellationToken = default)
            => Task.FromResult(new SessionFsReaddirWithTypesResult());
        public Task RmAsync(SessionFsRmParams request, CancellationToken cancellationToken = default) => Task.CompletedTask;
        public Task RenameAsync(SessionFsRenameParams request, CancellationToken cancellationToken = default) => Task.CompletedTask;
    }
}

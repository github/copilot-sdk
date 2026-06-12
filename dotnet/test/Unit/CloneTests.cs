/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Xunit;

namespace GitHub.Copilot.Test.Unit;

public class CloneTests
{
    [Fact]
    public void CopilotClientOptions_Clone_CopiesAllProperties()
    {
        var original = new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForTcp(port: 8080, connectionToken: "tok", path: "/usr/bin/copilot", args: ["--verbose", "--debug"]),
            WorkingDirectory = "/home/user",
            LogLevel = CopilotLogLevel.Debug,
            Environment = new Dictionary<string, string> { ["KEY"] = "value" },
            GitHubToken = "ghp_test",
            UseLoggedInUser = false,
            BaseDirectory = "/custom/copilot/home",
            EnableRemoteSessions = true,
            SessionIdleTimeoutSeconds = 600,
        };

        var clone = original.Clone();

        Assert.Same(original.Connection, clone.Connection);
        Assert.Equal(original.WorkingDirectory, clone.WorkingDirectory);
        Assert.Equal(original.LogLevel, clone.LogLevel);
        Assert.Equal(original.Environment, clone.Environment);
        Assert.Equal(original.GitHubToken, clone.GitHubToken);
        Assert.Equal(original.UseLoggedInUser, clone.UseLoggedInUser);
        Assert.Equal(original.BaseDirectory, clone.BaseDirectory);
        Assert.Equal(original.EnableRemoteSessions, clone.EnableRemoteSessions);
        Assert.Equal(original.SessionIdleTimeoutSeconds, clone.SessionIdleTimeoutSeconds);
    }

    [Fact]
    public void CopilotClientOptions_Clone_ConnectionIsShared()
    {
        var connection = RuntimeConnection.ForStdio();
        var original = new CopilotClientOptions { Connection = connection };

        var clone = original.Clone();

        Assert.Same(connection, clone.Connection);
    }

    [Fact]
    public void CopilotClientOptions_Clone_EnvironmentIsShared()
    {
        var env = new Dictionary<string, string> { ["key"] = "value" };
        var original = new CopilotClientOptions { Environment = env };

        var clone = original.Clone();

        Assert.Same(original.Environment, clone.Environment);
    }

    [Fact]
    public void SessionConfig_Clone_CopiesAllProperties()
    {
        var original = new SessionConfig
        {
            SessionId = "test-session",
            ClientName = "my-app",
            Model = "gpt-4",
            ReasoningEffort = "high",
            ReasoningSummary = ReasoningSummary.Detailed,
            ContextTier = ContextTier.LongContext,
            ConfigDirectory = "/config",
            AvailableTools = ["tool1", "tool2"],
            ExcludedTools = ["tool3"],
            WorkingDirectory = "/workspace",
            Streaming = true,
            EnableSessionTelemetry = false,
            IsExperimentalMode = true,
            EnableOnDemandInstructionDiscovery = true,
            IncludeSubAgentStreamingEvents = false,
            McpServers = new Dictionary<string, McpServerConfig> { ["server1"] = new McpStdioServerConfig { Command = "echo" } },
            McpOAuthTokenStorage = McpOAuthTokenStorageMode.Persistent,
            CustomAgents = [new CustomAgentConfig { Name = "agent1", Model = "claude-haiku-4.5" }],
            Agent = "agent1",
            Cloud = new CloudSessionOptions
            {
                Repository = new CloudSessionRepository
                {
                    Owner = "github",
                    Name = "copilot-sdk",
                    Branch = "main"
                }
            },
            DefaultAgent = new DefaultAgentConfig { ExcludedTools = ["hidden-tool"] },
            SkillDirectories = ["/skills"],
            InstructionDirectories = ["/instructions"],
            DisabledSkills = ["skill1"],
            PluginDirectories = ["/plugins"],
            LargeOutput = new LargeToolOutputConfig { Enabled = true, MaxSizeBytes = 2048, OutputDirectory = "/tmp/out" },
            OnExitPlanModeRequest = static (_, _) => Task.FromResult(new ExitPlanModeResult()),
            OnAutoModeSwitchRequest = static (_, _) => Task.FromResult(AutoModeSwitchResponse.No),
        };

        var clone = original.Clone();

        Assert.Equal(original.SessionId, clone.SessionId);
        Assert.Equal(original.ClientName, clone.ClientName);
        Assert.Equal(original.Model, clone.Model);
        Assert.Equal(original.ReasoningEffort, clone.ReasoningEffort);
        Assert.Equal(original.ReasoningSummary, clone.ReasoningSummary);
        Assert.Equal(original.ContextTier, clone.ContextTier);
        Assert.Equal(original.ConfigDirectory, clone.ConfigDirectory);
        Assert.Equal(original.AvailableTools, clone.AvailableTools);
        Assert.Equal(original.ExcludedTools, clone.ExcludedTools);
        Assert.Equal(original.WorkingDirectory, clone.WorkingDirectory);
        Assert.Equal(original.Streaming, clone.Streaming);
        Assert.Equal(original.EnableSessionTelemetry, clone.EnableSessionTelemetry);
        Assert.Equal(original.IsExperimentalMode, clone.IsExperimentalMode);
        Assert.Equal(original.EnableOnDemandInstructionDiscovery, clone.EnableOnDemandInstructionDiscovery);
        Assert.Equal(original.IncludeSubAgentStreamingEvents, clone.IncludeSubAgentStreamingEvents);
        Assert.Equal(original.McpServers.Count, clone.McpServers!.Count);
        Assert.Equal(original.McpOAuthTokenStorage, clone.McpOAuthTokenStorage);
        Assert.Equal(original.CustomAgents.Count, clone.CustomAgents!.Count);
        Assert.Equal(original.CustomAgents[0].Model, clone.CustomAgents[0].Model);
        Assert.Equal(original.Agent, clone.Agent);
        Assert.Same(original.Cloud, clone.Cloud);
        Assert.Equal(original.DefaultAgent!.ExcludedTools, clone.DefaultAgent!.ExcludedTools);
        Assert.Equal(original.SkillDirectories, clone.SkillDirectories);
        Assert.Equal(original.InstructionDirectories, clone.InstructionDirectories);
        Assert.Equal(original.DisabledSkills, clone.DisabledSkills);
        Assert.Equal(original.PluginDirectories, clone.PluginDirectories);
        Assert.Same(original.LargeOutput, clone.LargeOutput);
        Assert.Same(original.OnExitPlanModeRequest, clone.OnExitPlanModeRequest);
        Assert.Same(original.OnAutoModeSwitchRequest, clone.OnAutoModeSwitchRequest);
    }

    [Fact]
    public void SessionConfig_Clone_CollectionsAreIndependent()
    {
        var original = new SessionConfig
        {
            AvailableTools = ["tool1"],
            ExcludedTools = ["tool2"],
            McpServers = new Dictionary<string, McpServerConfig> { ["s1"] = new McpStdioServerConfig { Command = "echo" } },
            CustomAgents = [new CustomAgentConfig { Name = "a1" }],
            SkillDirectories = ["/skills"],
            InstructionDirectories = ["/instructions"],
            DisabledSkills = ["skill1"],
        };

        var clone = original.Clone();

        // Mutate clone collections
        clone.AvailableTools!.Add("tool99");
        clone.ExcludedTools!.Add("tool99");
        clone.McpServers!["s2"] = new McpStdioServerConfig { Command = "echo" };
        clone.CustomAgents!.Add(new CustomAgentConfig { Name = "a2" });
        clone.SkillDirectories!.Add("/more");
        clone.InstructionDirectories!.Add("/more-instructions");
        clone.DisabledSkills!.Add("skill99");

        // Original is unaffected
        Assert.Single(original.AvailableTools!);
        Assert.Single(original.ExcludedTools!);
        Assert.Single(original.McpServers!);
        Assert.Single(original.CustomAgents!);
        Assert.Single(original.SkillDirectories!);
        Assert.Single(original.InstructionDirectories!);
        Assert.Single(original.DisabledSkills!);
    }

    [Fact]
    public void SessionConfig_Clone_PreservesMcpServersComparer()
    {
        var servers = new Dictionary<string, McpServerConfig>(StringComparer.OrdinalIgnoreCase) { ["server"] = new McpStdioServerConfig { Command = "echo" } };
        var original = new SessionConfig { McpServers = servers };

        var clone = original.Clone();

        Assert.True(clone.McpServers!.ContainsKey("SERVER")); // case-insensitive lookup works
    }

    [Fact]
    public void ResumeSessionConfig_Clone_CollectionsAreIndependent()
    {
        var original = new ResumeSessionConfig
        {
            AvailableTools = ["tool1"],
            ExcludedTools = ["tool2"],
            McpServers = new Dictionary<string, McpServerConfig> { ["s1"] = new McpStdioServerConfig { Command = "echo" } },
            CustomAgents = [new CustomAgentConfig { Name = "a1" }],
            SkillDirectories = ["/skills"],
            InstructionDirectories = ["/instructions"],
            DisabledSkills = ["skill1"],
        };

        var clone = original.Clone();

        // Mutate clone collections
        clone.AvailableTools!.Add("tool99");
        clone.ExcludedTools!.Add("tool99");
        clone.McpServers!["s2"] = new McpStdioServerConfig { Command = "echo" };
        clone.CustomAgents!.Add(new CustomAgentConfig { Name = "a2" });
        clone.SkillDirectories!.Add("/more");
        clone.InstructionDirectories!.Add("/more-instructions");
        clone.DisabledSkills!.Add("skill99");

        // Original is unaffected
        Assert.Single(original.AvailableTools!);
        Assert.Single(original.ExcludedTools!);
        Assert.Single(original.McpServers!);
        Assert.Single(original.CustomAgents!);
        Assert.Single(original.SkillDirectories!);
        Assert.Single(original.InstructionDirectories!);
        Assert.Single(original.DisabledSkills!);
    }

    [Fact]
    public void ResumeSessionConfig_Clone_PreservesMcpServersComparer()
    {
        var servers = new Dictionary<string, McpServerConfig>(StringComparer.OrdinalIgnoreCase) { ["server"] = new McpStdioServerConfig { Command = "echo" } };
        var original = new ResumeSessionConfig { McpServers = servers };

        var clone = original.Clone();

        Assert.True(clone.McpServers!.ContainsKey("SERVER"));
    }

    [Fact]
    public void MessageOptions_Clone_CopiesAllProperties()
    {
        var original = new MessageOptions
        {
            Prompt = "Hello",
            Attachments = [new AttachmentFile { Path = "/test.txt", DisplayName = "test.txt" }],
            Mode = "chat",
        };

        var clone = original.Clone();

        Assert.Equal(original.Prompt, clone.Prompt);
        Assert.Equal(original.Mode, clone.Mode);
        Assert.Single(clone.Attachments!);
    }

    [Fact]
    public void MessageOptions_Clone_AttachmentsAreIndependent()
    {
        var original = new MessageOptions
        {
            Attachments = [new AttachmentFile { Path = "/test.txt", DisplayName = "test.txt" }],
        };

        var clone = original.Clone();

        clone.Attachments!.Add(new AttachmentFile { Path = "/other.txt", DisplayName = "other.txt" });

        Assert.Single(original.Attachments!);
    }

    [Fact]
    public void Clone_WithNullCollections_ReturnsNullCollections()
    {
        var original = new SessionConfig();

        var clone = original.Clone();

        Assert.Null(clone.AvailableTools);
        Assert.Null(clone.ExcludedTools);
        Assert.Null(clone.McpServers);
        Assert.Null(clone.CustomAgents);
        Assert.Null(clone.SkillDirectories);
        Assert.Null(clone.InstructionDirectories);
        Assert.Null(clone.DisabledSkills);
        Assert.Null(clone.Tools);
        Assert.Null(clone.DefaultAgent);
        Assert.True(clone.IncludeSubAgentStreamingEvents);
    }

    [Fact]
    public void SessionConfig_Clone_CopiesAgentProperty()
    {
        var original = new SessionConfig
        {
            Agent = "test-agent",
            CustomAgents = [new CustomAgentConfig { Name = "test-agent", Prompt = "You are a test agent." }],
        };

        var clone = original.Clone();

        Assert.Equal("test-agent", clone.Agent);
    }

    [Fact]
    public void ResumeSessionConfig_Clone_CopiesAgentProperty()
    {
        var original = new ResumeSessionConfig
        {
            Agent = "test-agent",
            CustomAgents = [new CustomAgentConfig { Name = "test-agent", Prompt = "You are a test agent." }],
        };

        var clone = original.Clone();

        Assert.Equal("test-agent", clone.Agent);
    }

    [Fact]
    public void ResumeSessionConfig_Clone_CopiesModeSwitchHandlers()
    {
        var original = new ResumeSessionConfig
        {
            OnExitPlanModeRequest = static (_, _) => Task.FromResult(new ExitPlanModeResult()),
            OnAutoModeSwitchRequest = static (_, _) => Task.FromResult(AutoModeSwitchResponse.No),
        };

        var clone = original.Clone();

        Assert.Same(original.OnExitPlanModeRequest, clone.OnExitPlanModeRequest);
        Assert.Same(original.OnAutoModeSwitchRequest, clone.OnAutoModeSwitchRequest);
    }

    [Fact]
    public void ResumeSessionConfig_Clone_CopiesIncludeSubAgentStreamingEvents()
    {
        var original = new ResumeSessionConfig
        {
            IncludeSubAgentStreamingEvents = false,
        };

        var clone = original.Clone();

        Assert.False(clone.IncludeSubAgentStreamingEvents);
    }

    [Fact]
    public void ResumeSessionConfig_Clone_PreservesIncludeSubAgentStreamingEventsDefault()
    {
        var original = new ResumeSessionConfig();

        var clone = original.Clone();

        Assert.True(clone.IncludeSubAgentStreamingEvents);
    }

    [Fact]
    public void ResumeSessionConfig_Clone_CopiesEnableSessionTelemetry()
    {
        var original = new ResumeSessionConfig
        {
            EnableSessionTelemetry = false,
        };

        var clone = original.Clone();

        Assert.False(clone.EnableSessionTelemetry);
    }

    [Fact]
    public void ResumeSessionConfig_Clone_CopiesIsExperimentalMode()
    {
        var original = new ResumeSessionConfig
        {
            IsExperimentalMode = true,
        };

        var clone = original.Clone();

        Assert.True(clone.IsExperimentalMode);
    }

    [Fact]
    public void ResumeSessionConfig_Clone_CopiesContinuePendingWork()
    {
        var original = new ResumeSessionConfig
        {
            ContinuePendingWork = true,
        };

        var clone = original.Clone();

        Assert.True(clone.ContinuePendingWork);
    }

    [Fact]
    public void ResumeSessionConfig_Clone_CopiesReasoningSummary()
    {
        var original = new ResumeSessionConfig
        {
            ReasoningSummary = ReasoningSummary.None,
        };

        var clone = original.Clone();

        Assert.Equal(original.ReasoningSummary, clone.ReasoningSummary);
    }

    [Fact]
    public void ResumeSessionConfig_Clone_CopiesContextTier()
    {
        var original = new ResumeSessionConfig
        {
            ContextTier = ContextTier.LongContext,
        };

        var clone = original.Clone();

        Assert.Equal(original.ContextTier, clone.ContextTier);
    }

    [Fact]
    public void ResumeSessionConfig_Clone_CopiesPluginDirectoriesAndLargeOutput()
    {
        var largeOutput = new LargeToolOutputConfig { Enabled = false, MaxSizeBytes = 4096, OutputDirectory = "/tmp/resume" };
        var original = new ResumeSessionConfig
        {
            PluginDirectories = ["/resume/plugins"],
            LargeOutput = largeOutput,
        };

        var clone = original.Clone();

        Assert.Equal(original.PluginDirectories, clone.PluginDirectories);
        Assert.Same(original.LargeOutput, clone.LargeOutput);
    }

    [Fact]
    public void ResumeSessionConfig_Clone_PreservesContinuePendingWorkDefault()
    {
        var original = new ResumeSessionConfig();

        var clone = original.Clone();

        Assert.Null(clone.ContinuePendingWork);
    }

    [Fact]
    public void SessionConfig_Clone_PreservesEnableSessionTelemetryDefault()
    {
        var original = new SessionConfig();

        var clone = original.Clone();

        Assert.Null(clone.EnableSessionTelemetry);
    }

    [Fact]
    public void ResumeSessionConfig_Clone_PreservesEnableSessionTelemetryDefault()
    {
        var original = new ResumeSessionConfig();

        var clone = original.Clone();

        Assert.Null(clone.EnableSessionTelemetry);
    }

    [Fact]
    public void SessionConfig_Clone_PreservesIsExperimentalModeDefault()
    {
        var original = new SessionConfig();

        var clone = original.Clone();

        Assert.Null(clone.IsExperimentalMode);
    }

    [Fact]
    public void ResumeSessionConfig_Clone_PreservesIsExperimentalModeDefault()
    {
        var original = new ResumeSessionConfig();

        var clone = original.Clone();

        Assert.Null(clone.IsExperimentalMode);
    }

    [Fact]
    public void SessionConfig_Clone_CopiesEnableOnDemandInstructionDiscovery()
    {
        var original = new SessionConfig
        {
            EnableOnDemandInstructionDiscovery = false,
        };

        var clone = original.Clone();

        Assert.False(clone.EnableOnDemandInstructionDiscovery);
    }

    [Fact]
    public void ResumeSessionConfig_Clone_CopiesEnableOnDemandInstructionDiscovery()
    {
        var original = new ResumeSessionConfig
        {
            EnableOnDemandInstructionDiscovery = true,
        };

        var clone = original.Clone();

        Assert.True(clone.EnableOnDemandInstructionDiscovery);
    }

    [Fact]
    public void SessionConfig_Clone_PreservesEnableOnDemandInstructionDiscoveryDefault()
    {
        var original = new SessionConfig();

        var clone = original.Clone();

        Assert.Null(clone.EnableOnDemandInstructionDiscovery);
    }

    [Fact]
    public void ResumeSessionConfig_Clone_PreservesEnableOnDemandInstructionDiscoveryDefault()
    {
        var original = new ResumeSessionConfig();

        var clone = original.Clone();

        Assert.Null(clone.EnableOnDemandInstructionDiscovery);
    }

    [Fact]
    public void SessionConfig_Clone_CopiesMcpOAuthTokenStorage()
    {
        var original = new SessionConfig
        {
            McpOAuthTokenStorage = McpOAuthTokenStorageMode.Persistent,
        };

        var clone = original.Clone();

        Assert.Equal(McpOAuthTokenStorageMode.Persistent, clone.McpOAuthTokenStorage);
    }

    [Fact]
    public void ResumeSessionConfig_Clone_CopiesMcpOAuthTokenStorage()
    {
        var original = new ResumeSessionConfig
        {
            McpOAuthTokenStorage = McpOAuthTokenStorageMode.Persistent,
        };

        var clone = original.Clone();

        Assert.Equal(McpOAuthTokenStorageMode.Persistent, clone.McpOAuthTokenStorage);
    }
}

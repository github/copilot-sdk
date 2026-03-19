/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.SDK.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test;

public class SystemMessageTransformTests(E2ETestFixture fixture, ITestOutputHelper output) : E2ETestBase(fixture, "system_message_transform", output)
{
    [Fact]
    public async Task Should_Invoke_Transform_Callbacks_With_Section_Content()
    {
        var identityCallbackInvoked = false;
        var toneCallbackInvoked = false;

        var session = await CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            SystemMessage = new SystemMessageConfig
            {
                Mode = SystemMessageMode.Customize,
                Sections = new Dictionary<string, SectionOverride>
                {
                    ["identity"] = new SectionOverride
                    {
                        Transform = async (content) =>
                        {
                            Assert.False(string.IsNullOrEmpty(content));
                            identityCallbackInvoked = true;
                            return content;
                        }
                    },
                    ["tone"] = new SectionOverride
                    {
                        Transform = async (content) =>
                        {
                            Assert.False(string.IsNullOrEmpty(content));
                            toneCallbackInvoked = true;
                            return content;
                        }
                    }
                }
            }
        });

        await File.WriteAllTextAsync(Path.Combine(Ctx.WorkDir, "test.txt"), "Hello transform!");

        await session.SendAsync(new MessageOptions
        {
            Prompt = "Read the contents of test.txt and tell me what it says"
        });

        await TestHelper.GetFinalAssistantMessageAsync(session);

        Assert.True(identityCallbackInvoked, "Expected identity transform callback to be invoked");
        Assert.True(toneCallbackInvoked, "Expected tone transform callback to be invoked");
    }

    [Fact]
    public async Task Should_Apply_Transform_Modifications_To_Section_Content()
    {
        var session = await CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            SystemMessage = new SystemMessageConfig
            {
                Mode = SystemMessageMode.Customize,
                Sections = new Dictionary<string, SectionOverride>
                {
                    ["identity"] = new SectionOverride
                    {
                        Transform = async (content) =>
                        {
                            return content + "\nAlways end your reply with TRANSFORM_MARKER";
                        }
                    }
                }
            }
        });

        await File.WriteAllTextAsync(Path.Combine(Ctx.WorkDir, "hello.txt"), "Hello!");

        await session.SendAsync(new MessageOptions
        {
            Prompt = "Read the contents of hello.txt"
        });

        await TestHelper.GetFinalAssistantMessageAsync(session);

        // Verify the transform result was actually applied to the system message
        var traffic = await Ctx.GetExchangesAsync();
        Assert.NotEmpty(traffic);
        var systemMessage = GetSystemMessage(traffic[0]);
        Assert.Contains("TRANSFORM_MARKER", systemMessage);
    }

    [Fact]
    public async Task Should_Work_With_Static_Overrides_And_Transforms_Together()
    {
        var transformCallbackInvoked = false;

        var session = await CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            SystemMessage = new SystemMessageConfig
            {
                Mode = SystemMessageMode.Customize,
                Sections = new Dictionary<string, SectionOverride>
                {
                    ["safety"] = new SectionOverride
                    {
                        Action = SectionOverrideAction.Remove
                    },
                    ["identity"] = new SectionOverride
                    {
                        Transform = async (content) =>
                        {
                            transformCallbackInvoked = true;
                            return content;
                        }
                    }
                }
            }
        });

        await File.WriteAllTextAsync(Path.Combine(Ctx.WorkDir, "combo.txt"), "Combo test!");

        await session.SendAsync(new MessageOptions
        {
            Prompt = "Read the contents of combo.txt and tell me what it says"
        });

        await TestHelper.GetFinalAssistantMessageAsync(session);

        Assert.True(transformCallbackInvoked, "Expected identity transform callback to be invoked");
    }
}

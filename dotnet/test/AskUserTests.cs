/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.SDK.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test;

public class AskUserTests(E2ETestFixture fixture, ITestOutputHelper output) : E2ETestBase(fixture, "ask-user", output)
{
    [Fact]
    public async Task Should_Invoke_User_Input_Handler_When_Model_Uses_Ask_User_Tool()
    {
        var userInputRequests = new List<UserInputRequest>();
        CopilotSession? session = null;
        session = await Client.CreateSessionAsync(new SessionConfig
        {
            OnUserInputRequest = (request, invocation) =>
            {
                userInputRequests.Add(request);
                Assert.Equal(session!.SessionId, invocation.SessionId);

                // Return the first choice if available, otherwise a freeform answer
                var answer = request.Choices?.FirstOrDefault() ?? "freeform answer";
                var wasFreeform = request.Choices == null || request.Choices.Count == 0;

                return Task.FromResult(new UserInputResponse { Answer = answer, WasFreeform = wasFreeform });
            }
        });

        await session.SendAsync(new MessageOptions
        {
            Prompt = "Ask me to choose between 'Option A' and 'Option B' using the ask_user tool. Wait for my response before continuing."
        });

        await TestHelper.GetFinalAssistantMessageAsync(session);

        // Should have received at least one user input request
        Assert.NotEmpty(userInputRequests);

        // The request should have a question
        Assert.Contains(userInputRequests, r => !string.IsNullOrEmpty(r.Question));
    }

    [Fact]
    public async Task Should_Receive_Choices_In_User_Input_Request()
    {
        var userInputRequests = new List<UserInputRequest>();

        var session = await Client.CreateSessionAsync(new SessionConfig
        {
            OnUserInputRequest = (request, invocation) =>
            {
                userInputRequests.Add(request);

                // Pick the first choice
                var answer = request.Choices?.FirstOrDefault() ?? "default";

                return Task.FromResult(new UserInputResponse { Answer = answer, WasFreeform = false });
            }
        });

        await session.SendAsync(new MessageOptions
        {
            Prompt = "Use the ask_user tool to ask me to pick between exactly two options: 'Red' and 'Blue'. These should be provided as choices. Wait for my answer."
        });

        await TestHelper.GetFinalAssistantMessageAsync(session);

        // Should have received a request
        Assert.NotEmpty(userInputRequests);

        // At least one request should have choices
        Assert.Contains(userInputRequests, r => r.Choices != null && r.Choices.Count > 0);
    }

    [Fact]
    public async Task Should_Handle_Freeform_User_Input_Response()
    {
        var userInputRequests = new List<UserInputRequest>();
        var freeformAnswer = "This is my custom freeform answer that was not in the choices";

        var session = await Client.CreateSessionAsync(new SessionConfig
        {
            OnUserInputRequest = (request, invocation) =>
            {
                userInputRequests.Add(request);

                // Return a freeform answer (not from choices)
                return Task.FromResult(new UserInputResponse { Answer = freeformAnswer, WasFreeform = true });
            }
        });

        await session.SendAsync(new MessageOptions
        {
            Prompt = "Ask me a question using ask_user and then include my answer in your response. The question should be 'What is your favorite color?'"
        });

        var response = await TestHelper.GetFinalAssistantMessageAsync(session);

        // Should have received a request
        Assert.NotEmpty(userInputRequests);

        // The model's response should be defined
        Assert.NotNull(response);
    }
}

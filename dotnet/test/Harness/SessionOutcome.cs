/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

namespace GitHub.Copilot.SDK.Test.Harness;

public enum SessionOutcomeKind
{
    Message,
    Abstention,
}

public sealed record SessionOutcome(SessionOutcomeKind Kind, AssistantMessageEvent? AssistantMessage)
{
    public static SessionOutcome Message(AssistantMessageEvent message) =>
        new(SessionOutcomeKind.Message, message);

    public static SessionOutcome Abstention() =>
        new(SessionOutcomeKind.Abstention, null);
}

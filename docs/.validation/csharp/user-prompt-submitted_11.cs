// Source: hooks/user-prompt-submitted.md:52
public delegate Task<UserPromptSubmittedHookOutput?> UserPromptSubmittedHandler(
    UserPromptSubmittedHookInput input,
    HookInvocation invocation);
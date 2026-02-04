# Source: hooks/user-prompt-submitted.md:27
UserPromptSubmittedHandler = Callable[
    [UserPromptSubmittedHookInput, HookInvocation],
    Awaitable[UserPromptSubmittedHookOutput | None]
]
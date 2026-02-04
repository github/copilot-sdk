# Source: hooks/error-handling.md:27
ErrorOccurredHandler = Callable[
    [ErrorOccurredHookInput, HookInvocation],
    Awaitable[ErrorOccurredHookOutput | None]
]
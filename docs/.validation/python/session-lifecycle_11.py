# Source: hooks/session-lifecycle.md:31
SessionStartHandler = Callable[
    [SessionStartHookInput, HookInvocation],
    Awaitable[SessionStartHookOutput | None]
]
# Source: hooks/session-lifecycle.md:208
SessionEndHandler = Callable[
    [SessionEndHookInput, HookInvocation],
    Awaitable[SessionEndHookOutput | None]
]
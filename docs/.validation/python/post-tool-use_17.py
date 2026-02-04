# Source: hooks/post-tool-use.md:27
PostToolUseHandler = Callable[
    [PostToolUseHookInput, HookInvocation],
    Awaitable[PostToolUseHookOutput | None]
]
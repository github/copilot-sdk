# Source: hooks/pre-tool-use.md:27
PreToolUseHandler = Callable[
    [PreToolUseHookInput, HookInvocation],
    Awaitable[PreToolUseHookOutput | None]
]
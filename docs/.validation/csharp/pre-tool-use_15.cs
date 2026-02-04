// Source: hooks/pre-tool-use.md:52
public delegate Task<PreToolUseHookOutput?> PreToolUseHandler(
    PreToolUseHookInput input,
    HookInvocation invocation);
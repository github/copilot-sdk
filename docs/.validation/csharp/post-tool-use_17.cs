// Source: hooks/post-tool-use.md:52
public delegate Task<PostToolUseHookOutput?> PostToolUseHandler(
    PostToolUseHookInput input,
    HookInvocation invocation);
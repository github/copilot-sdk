// Source: hooks/session-lifecycle.md:56
public delegate Task<SessionStartHookOutput?> SessionStartHandler(
    SessionStartHookInput input,
    HookInvocation invocation);
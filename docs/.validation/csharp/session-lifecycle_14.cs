// Source: hooks/session-lifecycle.md:233
public delegate Task<SessionEndHookOutput?> SessionEndHandler(
    SessionEndHookInput input,
    HookInvocation invocation);
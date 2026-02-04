// Source: hooks/error-handling.md:52
public delegate Task<ErrorOccurredHookOutput?> ErrorOccurredHandler(
    ErrorOccurredHookInput input,
    HookInvocation invocation);
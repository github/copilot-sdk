// Source: hooks/error-handling.md:15
type ErrorOccurredHandler = (
  input: ErrorOccurredHookInput,
  invocation: HookInvocation
) => Promise<ErrorOccurredHookOutput | null | undefined>;
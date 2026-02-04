// Source: hooks/session-lifecycle.md:196
type SessionEndHandler = (
  input: SessionEndHookInput,
  invocation: HookInvocation
) => Promise<SessionEndHookOutput | null | undefined>;
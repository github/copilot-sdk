// Source: hooks/session-lifecycle.md:19
type SessionStartHandler = (
  input: SessionStartHookInput,
  invocation: HookInvocation
) => Promise<SessionStartHookOutput | null | undefined>;
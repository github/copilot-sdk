// Source: hooks/user-prompt-submitted.md:15
type UserPromptSubmittedHandler = (
  input: UserPromptSubmittedHookInput,
  invocation: HookInvocation
) => Promise<UserPromptSubmittedHookOutput | null | undefined>;